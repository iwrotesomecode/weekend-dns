(ns weekend-dns.part2
  (:require [weekend-dns.part1 :refer [build-query make-DNS-header make-DNS-question]]
            [weekend-dns.network :refer [socket send-bytes receive-loop]]
            [weekend-dns.utils :refer [concat-byte-arrays hexify]]
            [clojure.string :as str])
  (:import [java.io ByteArrayInputStream DataInputStream]
           [java.nio ByteBuffer]
           [java.lang String]))

(set! *warn-on-reflection* true)

(def TYPE-A 1)
(def TYPE-NS 2)
(def CNAME 5)
(def TXT 16)
(def AAAA 28)

;; Prevent compression pointer infinite loop
;; https://github.com/miekg/dns/blob/b3dfea07155dbe4baafd90792c67b85a3bf5be23/msg.go#L26-L36
(def max-octets 255) ;; RFC 1035 section 2.3.4
(def max-compression-pointers  (-> (inc max-octets) (/ 2) (- 2)))
(def pointer-depth (atom 0))

;; https://clojureverse.org/t/what-is-2021-recommendation-for-specs/7508/19
(defn make-DNS-record
  [& args]
  (let [keys [:name :type :class :ttl :data]]
    (zipmap keys args)))

(defn make-DNS-packet
  [& args]
  (let [keys [:header :questions :answers :authorities :additionals]]
    (zipmap keys args)))

(defn make-reader
  "Creates DataInputStream from byte response. Optionally takes an argument for
  offset used to generate a second reader for dealing with compression since
  seek is not supported."
  ([response] (DataInputStream. (ByteArrayInputStream. response)))
  ([response offset] (DataInputStream. (ByteArrayInputStream. response offset (- (count response) offset)))))

(defn parse-header [^DataInputStream reader]
  (let [keys [:id :flags :num-questions :num-answers :num-authorities :num-additionals]]
    (->> (for [_ keys]
           (.readUnsignedShort reader))
         (apply make-DNS-header))))

(declare decode-name)
(defn decode-compressed-name
  "Handle DNS compression. If first 2 bits are set (1100 0000), it is compressed.
  This is a pointer, since all labels must begin with two 0 bits and have a max
  length 63 (0011 1111). For pointers the least 6 bits indicate the offset field
  from the start of the message.
  https://datatracker.ietf.org/doc/html/rfc1035#section-4.1.4
  "
  [length ^DataInputStream reader response]
  ;; default byte order is BIG_ENDIAN, most significant byte stored first
  (let [bb (ByteBuffer/allocate 2)
        byte1 (byte (bit-and length 2r00111111)) ;; bit operations promoted to Long, recast to byte
        pointer (do
                  ;;(.order bb java.nio.ByteOrder/LITTLE_ENDIAN)
                  (.put bb byte1)
                  (.put bb (unchecked-byte (.readByte reader))) ;; recast promoted byte
                  (.flip bb) ;; convert writing to reading
                  (Short/toUnsignedInt (.getShort bb)))
        ;; Since Input Streams aren't seekable, make a new stream offset by the pointer
        result (decode-name (make-reader response pointer) response)]
    (swap! pointer-depth inc)
    (when (>= @pointer-depth max-compression-pointers)
      (throw (Exception. (str "Too many compression pointers. " @pointer-depth))))
    result))

(defn decode-name
  [^DataInputStream reader response]
  (loop [parts []
         length (.readUnsignedByte reader)]
    (if (not= 0 length)
      (if (< length 2r11000000) ;; >= 192
        (recur (let [ba (doto (byte-array length)
                          (#(.read reader %)))]
                 (conj parts ba))
               (.readUnsignedByte reader))
        ;; a compressed name is never followed by another label, exit
        (recur (conj parts (decode-compressed-name length reader response)) 0))
      (->> (interpose (byte-array [(int \.)]) parts)
           (apply concat-byte-arrays)))))

(defn ip->string [ip-bytes]
  (str/join "." (map #(Byte/toUnsignedInt %) ip-bytes)))

(defn ipv6->string [ip-bytes]
  (->> (hexify ip-bytes)
       (partition 4)
       (map #(apply str %))
       (str/join ":")))

(defn byte->string [^"[B" name]
  (String. name "UTF-8"))

(defn parse-record [^DataInputStream reader response]
  (reset! pointer-depth 0)
  (let [name (-> (decode-name reader response)
                 (byte->string))
        type (.readShort reader)
        class (.readShort reader)
        ttl (.readInt reader)
        data-len (Short/toUnsignedInt (.readShort reader))]
    (cond
      (= type TYPE-NS) (let [data (decode-name reader response)]
                         (make-DNS-record name type class ttl (byte->string data)))
      (= type TYPE-A) (let [data (byte-array data-len)]
                        (.read reader data)
                        (make-DNS-record name type class ttl (ip->string data)))
      (= type CNAME) (let [data (decode-name reader response)]
                       (make-DNS-record name type class ttl (byte->string data)))
      (= type AAAA) (let [data (byte-array data-len)]
                      (.read reader data)
                      (make-DNS-record name type class ttl (ipv6->string data)))
      :else (let [data (byte-array data-len)]
              (.read reader data)
              (make-DNS-record name type class ttl data)))))

(defn parse-question [^DataInputStream reader response]
  (reset! pointer-depth 0)
  (let [name (-> (decode-name reader response) (byte->string))
        type (.readShort reader)
        class (.readShort reader)]
    (make-DNS-question name type class)))

;; without doall, it will parse everything when run
;; (-> response parse-dns-packet)
;; BUT
;; (-> response parse-dns-packet :answers)
;; would then only execute the answers list comprehension and be wrong
;; 'for' eagerly returns a lazy sequence
(defn parse-dns-packet [response]
  (let [^DataInputStream reader (make-reader response)
        header (parse-header reader)
        questions (doall (for [_ (range (:num-questions header))]
                           (parse-question reader response)))
        answers (doall (for [_ (range (:num-answers header))]
                         (parse-record reader response)))
        authorities (doall (for [_ (range (:num-authorities header))]
                             (parse-record reader response)))
        additionals (doall (for [_ (range (:num-additionals header))]
                             (parse-record reader response)))]
    (make-DNS-packet header questions answers authorities additionals)))

(defn lookup-domain
  [domain-name]
  (let [socket socket
        query (build-query domain-name TYPE-A)
        response (receive-loop socket :data)]
    (send-bytes socket query "8.8.8.8" 53)
    (-> @response
        parse-dns-packet
        :answers
        first
        :data
        ip->string
        prn)))

(comment
  (let [bb (ByteBuffer/allocate 2)
        byte1 (unchecked-byte 2r10000000)
        byte2 (unchecked-byte 2r00000001)]
    (.put bb byte1)
    (.put bb byte2)
    (.flip bb)
    (unchecked-short (.getShort bb))))

(comment
  (lookup-domain "www.example.com"))
