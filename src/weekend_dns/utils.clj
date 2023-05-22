(ns weekend-dns.utils
  (:import [java.nio ByteBuffer]
           [java.util HexFormat]))

(set! *warn-on-reflection* true)

;; https://stackoverflow.com/a/26670298
(defn concat-byte-arrays [& byte-arrays]
  (when (not-empty byte-arrays)
    (let [total-size (reduce + (map count byte-arrays))
          result     (byte-array total-size)
          bb         (ByteBuffer/wrap result)]
      (doseq [^"[B" ba byte-arrays]
        (.put bb ba))
      result)))

(defn num->byte-array [n]
  (let [num (unchecked-short n)
        result (byte-array 2)
        bb (ByteBuffer/wrap result)]
    (.putShort bb num)
    result))

(defn hexify [byte-array]
  (.formatHex (HexFormat/of) byte-array))

(defn unhexify
  "Returns byte array from a hex string"
  [str]
  (.parseHex (HexFormat/of) str))

(defn seek
  "Returns first item from coll for which (pred item) returns true.
   Returns nil if no such item is present, or the not-found value if supplied."
  ([pred coll] (seek pred coll nil))
  ([pred coll not-found]
   (reduce (fn [_ x]
             (if (pred x)
               (reduced x)
               not-found))
           not-found coll)))
