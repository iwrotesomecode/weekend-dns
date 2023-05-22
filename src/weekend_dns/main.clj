(ns weekend-dns.main
  (:require [weekend-dns.part3 :refer [resolve-ip]])
  (:gen-class))

(set! *warn-on-reflection* true)
(def record-map {"TYPE-A" 1})

(defn -main
  [& args]
  (let [[domain t] args
        type (or (get record-map t) (Integer/parseInt t))]
    (prn (resolve-ip domain type))
    (shutdown-agents)))
