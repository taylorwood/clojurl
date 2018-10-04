(ns script
  (:require [clj-http.lite.client :as http]
            [clojure.string :as cs]
            [clojure.tools.cli :as cli])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-options
  [["-u" "--uri URI" "URI of request"
    :validate [(comp not cs/blank?) "Must be a non-empty string"]]
   ["-m" "--method METHOD" "Request method e.g. GET, POST, etc."
    :parse-fn (comp keyword cs/lower-case)
    :default :get]
   ["-h" "--help"]])

(defn -main [& args]
  ;; for sunec native lib loading at native-image runtime
  (System/setProperty "java.library.path"
                      (str (System/getenv "GRAALVM_HOME") "/jre/lib"))
  (let [opts (cli/parse-opts args cli-options)]
    (when (get-in opts [:options :help]) (println (:summary opts)))
    (if-let [url (get-in opts [:options :uri])]
      (clojure.pprint/pprint
       (http/request {:method (keyword (get-in opts [:options :method]))
                      :url    url}))
      (println (:summary opts)))))
