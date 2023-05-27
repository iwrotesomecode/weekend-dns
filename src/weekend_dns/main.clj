(ns weekend-dns.main
  (:require
   [clojure.string :as str]
   [clojure.pprint :refer [pprint]]
   [clojure.tools.cli :refer [parse-opts]]
   [weekend-dns.part3 :refer [resolve-ip respond]])
  (:gen-class))

(set! *warn-on-reflection* true)

(def record-map {"TYPE-A" 1
                 "A" 1
                 "NS" 2
                 "CNAME" 5
                 "TXT" 16
                 "AAAA" 28})

(def cli-options
  [["-t" "--type TYPE" "Record Type"
    :default 1
    :parse-fn #(or (get record-map %) (Integer/parseInt %))
    :validate [#(or (contains? record-map %)
                    (some (fn [v] (= v %)) (vals record-map)))]]
   ["-n" "--nameserver IP" "Nameserver IP"
    :default "198.41.0.4"
    :validate [#(let [reducer (comp
                               (partial reduce +)
                               (partial map count))]
                  (= (->> (str/split % #"\.") (reducer))
                     (->> (re-seq #"\d+" %) (reducer))))]]
   ["-r" "--response" "Print DNS response"]
   ["-v" "--verbose"]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["Weekend DNS Resolver"
        ""
        "Usage (native image): ./dns url [options]"
        "Usage (clj):          clj -M:run url [options]"
        ""
        "URL:"
        "e.g. www.example.com"
        ""
        "Options:"
        options-summary
        ""
        "Examples:"
        ""
        "./dns www.example.com"
        "./dns www.example.com -v -t TYPE-A"
        "clj -M:run www.example.com -r"
        "clj -M:run www.example.com -rn 192.5.6.30"]
       (str/join \newline)))

(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with an error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options)]
    (cond
      (:help options) ; help => exit OK with usage summary
      {:exit-message (usage summary) :ok? true}

      errors ; errors => exit with description of errors
      {:exit-message (error-msg errors)}

      ;; custom validation on arguments
      (= 1 (count arguments))
      {:url (first arguments) :options options}

      :else ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))

(defn exit [status msg]
  (println msg)
  (System/exit status))

(defn -main
  [& args]
  (let [{:keys [url options exit-message ok?]} (validate-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (cond
        (:response options)
        (do (pprint (respond url options))
            (shutdown-agents))

        :else
        (do (pprint (resolve-ip url options))
            (shutdown-agents))))))
