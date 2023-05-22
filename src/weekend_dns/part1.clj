(ns weekend-dns.part1
  (:require [clojure.string :as str]
            [weekend-dns.utils :refer [concat-byte-arrays num->byte-array hexify]]
            [weekend-dns.network :refer [socket receive-loop send-bytes]]))

(set! *warn-on-reflection* true)

(defn make-DNS-header
  ([id flags questions]
   (let [keys [:id :flags :num-questions]]
     (-> (zipmap keys [id flags questions])
         (assoc :num-answers 0 :num-authorities 0 :num-additionals 0))))
  ([id flags num-questions num-answers num-authorities num-additionals]
   {:id id
    :flags flags
    :num-questions num-questions
    :num-answers num-answers
    :num-authorities num-authorities
    :num-additionals num-additionals}))

(defn make-DNS-question
  ([name]
   (-> {:name name}
       (assoc :type 1 :class 1)))
  ([name type class]
   {:name name
    :type type
    :class class}))

(defn header->bytes
  [header]
  (->> (map num->byte-array (vals header))
       (apply concat-byte-arrays)))

(defn question->bytes
  [question]
  (let [name (:name question)
        type (-> (:type question)
                 (num->byte-array))
        class (-> (:class question)
                  (num->byte-array))]
    (concat-byte-arrays name type class)))

(defn encode-dns-name
  [domain-name]
  (let [parts (str/split domain-name #"\.")]
    (-> (reduce (fn [acc el]
                  (concat acc (vector (count el))
                          (map int el)))
                []
                parts)
        (concat [0])
        (byte-array))))

(def TYPE-A 1)
(def CLASS-IN 1)

(defn build-query
  [domain-name record-type]
  (let [name (encode-dns-name domain-name)
        id (rand-int 65535) ;; 33432
        ;; recursion-desired (bit-shift-left 1 8) ;; 256
        ;; asking authoritative nameserver now, don't ask for recursion {:flags 0}
        header (make-DNS-header id 0 1)
        question (make-DNS-question name record-type CLASS-IN)]
    (concat-byte-arrays (header->bytes header) (question->bytes question))))

(comment
  ;; Testing these hex strings match expected output
  (-> (build-query "example.com" TYPE-A) hexify)
  ;; => "395901000001000000000000076578616d706c6503636f6d0000010001"
  (-> (build-query "www.example.com" TYPE-A) hexify)
  ;; => "82980100000100000000000003777777076578616d706c6503636f6d0000010001"
  )

(defn test-dns
  [{:keys [domain] :or {domain "www.example.com"}}]
  (receive-loop socket println)
  (send-bytes socket (build-query domain TYPE-A) "8.8.8.8" 53))

