(ns weekend-dns.part3
  (:require [weekend-dns.utils :refer [seek]]
            [weekend-dns.part1 :refer [build-query]]
            [weekend-dns.part2 :refer [parse-dns-packet]]
            [weekend-dns.network :refer [socket send-bytes receive-loop]])
  (:import [java.lang String]))

(set! *warn-on-reflection* true)

(def TYPE-A 1)
(def TYPE-NS 2)
(def CNAME 5)
(def TXT 16)

(defn send-query
  [ip-addr domain-name record-type]
  (let [socket @socket
        query (build-query domain-name record-type)
        response (receive-loop socket :data)]
    (send-bytes socket query ip-addr 53)
    (-> @response
        parse-dns-packet)))

(defn get-answer
  "Return the first A record in the Answer section"
  [packet]
  (if-let [record (seek #(= TYPE-A (:type %)) (:answers packet))]
    (:data record)
    nil))

(defn get-nameserver-ip
  "Return first A record in the Additional section"
  [packet]
  (if-let [record (seek #(= TYPE-A (:type %)) (:additionals packet))]
    (:data record)
    nil))

(defn get-nameserver
  "Return first NS record in the Authority section"
  [packet]
  (if-let [record (seek #(= TYPE-NS (:type %)) (:authorities packet))]
    (-> ^"[B" (:data record) (String. "UTF-8"))
    nil))

(defn get-canonical-name
  "Return first CNAME record in Answer section"
  [packet]
  (if-let [record (seek #(= CNAME (:type %)) (:answers packet))]
    (:data record)
    nil))

(defn resolve-ip
  [domain-name record-type]
  (reduce (fn [nameserver _]
            (let [response (send-query (:ip nameserver) domain-name record-type)]
              (prn (str "Querying " (:ip nameserver) " for " domain-name))

              (or
               ;; best case, get answer to query and we're done
               (when-let [ip (get-answer response)]
                 (reduced ip))

               ;; second best, get "additionals" record ip addr of another nameserver to query
               (when-let [ns-ip (get-nameserver-ip response)]
                 (assoc nameserver :ip ns-ip))

               ;; or, get the domain name of another nameserver for which we can look up ip addr
               (when-let [ns-domain (get-nameserver response)]
                 (let [ns (resolve-ip ns-domain TYPE-A)]
                   (prn (str "NS-domain " ns-domain " found at " ns))
                   (assoc nameserver :ip ns)))

               ;; or, get the canonical name or alias domain for which we can look up ip addr
               (when-let [cname-domain (get-canonical-name response)]
                 (let [cname (resolve-ip cname-domain TYPE-A)]
                   (prn (str "CNAME " cname-domain " resolved"))
                   (reduced cname)))

               (throw (Exception. "Something went wrong.")))))

          {:ip "198.41.0.4"} ;; Root nameserver a.root-servers.net
          (repeat nil)))

