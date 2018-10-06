(ns clojurl
  (:require [clj-http.lite.client :as http]
            [clojure.string :as cs]
            [clojure.tools.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [hickory.core :as hick])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-options
  [["-u" "--uri URI" "URI of request"
    :validate [(comp not cs/blank?) "Must be a non-empty string"]]
   ["-H" "--header HEADER" "Request header(s)"
    :parse-fn #(cs/split % #"=")
    :id :headers, :default {}, :assoc-fn (fn [m k [l r]] (update m k assoc l r))
    :default-desc ""]
   ["-d" "--data DATA" "Request data"]
   ["-m" "--method METHOD" "Request method e.g. GET, POST, etc."
    :default :get, :parse-fn (comp keyword cs/lower-case)
    :default-desc "GET"]
   ["-o" "--output FORMAT" "Output format e.g. edn, hickory"
    :id :output
    :parse-fn {"edn"     pprint
               "hickory" (comp prn hick/as-hickory hick/parse :body)}
    :default pprint :default-desc "edn"]
   ["-h" "--help" :id :help?]])

(defn -main [& args]
  ;; for sunec native lib loading at native-image runtime
  (System/setProperty "java.library.path"
                      (str (System/getenv "GRAALVM_HOME") "/jre/lib"))
  (let [{:keys [options summary]} (cli/parse-opts args cli-options)
        {:keys [help? uri method headers data output]} options]
    (when help? (println summary))
    (when uri
      (output (http/request {:url     uri
                             :method  (keyword method)
                             :headers headers
                             :body    data})))))
