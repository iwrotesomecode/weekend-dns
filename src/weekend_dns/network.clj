(ns weekend-dns.network
  (:import [java.net InetSocketAddress DatagramPacket DatagramSocket]))
(set! *warn-on-reflection* true)
;; https://github.com/clojure-cookbook/clojure-cookbook/blob/master/05_network-io/5-11_udp.asciidoc

(def socket (delay (DatagramSocket.))) ;; instantiate at runtime
(defn send-bytes
  "Send a byte-array over a DatagramSocket to the specified
  host and port."
  [^DatagramSocket socket ^"[B" byte-array ^String host ^int port]
  (let [payload byte-array
        length (count payload)
        address (InetSocketAddress. host port)
        packet (DatagramPacket. payload length address)]
    (.send socket packet)))

(defn receive
  "Block until a UDP message is received on the given DatagramSocket, and
  return the payload as a map of data and length."
  [^DatagramSocket socket]
  (let [buffer (byte-array 512)
        packet (DatagramPacket. buffer 512)]
    (.receive socket packet)
    {:data (.getData packet) :length (.getLength packet)}))

(defn receive-loop
  "Given a function and DatagramSocket, will (in another thread) wait
  for the socket to receive a message, and whenever it does, will call
  the provided function on the incoming message."
  [socket f]
  ;;(future (while true (f (receive socket))))
  ;; don't need to keep this open, just return.
  (future (f (receive socket))))
