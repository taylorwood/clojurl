(ns clojurl
  (:require [clj-http.lite.client :as http]
            [clojure.spec.alpha :as s]
            [clojure.string :as cs]
            [clojure.tools.cli :as cli]
            [clojure.pprint :refer [pprint]]
            [expound.alpha :as expound]
            [hickory.core :as hick])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-options
  [["-u" "--uri URI" "URI of request"
    :id :url
    :validate [(comp not cs/blank?) "Must be a non-empty string"]]
   ["-H" "--header HEADER" "Request header(s)"
    :parse-fn #(cs/split % #"=")
    :id :headers, :default {}, :assoc-fn (fn [m k [l r]] (update m k assoc l r))
    :default-desc ""]
   ["-d" "--data DATA" "Request data"
    :id :body]
   ["-m" "--method METHOD" "Request method e.g. GET, POST, etc."
    :default :get, :parse-fn (comp keyword cs/lower-case)
    :default-desc "GET"]
   ["-o" "--output FORMAT" "Output format e.g. edn, hickory"
    :id :output-fn
    :parse-fn {"edn"     pprint
               "hickory" (comp prn hick/as-hickory hick/parse :body)}
    :default pprint :default-desc "edn"]
   ["-v" "--verbose" "Print verbose info" :id :verbose?]
   ["-h" "--help" "Print this message" :id :help?]])

(s/def ::non-empty-string (s/and string? (comp not cs/blank?)))
(s/def ::url ::non-empty-string)
(s/def ::output-fn ifn?)
(s/def ::method keyword?)
(s/def ::headers (s/coll-of (s/tuple ::non-empty-string ::non-empty-string)))
(s/def ::body string?)
(s/def ::options
  (s/keys :req-un [::url ::output-fn]
          :opt-un [::method ::headers ::body]))

(defn -main [& args]
  (let [{{:keys [help? verbose? output-fn] :as opts} :options, help :summary}
        (cli/parse-opts args cli-options)]
    (cond
      help?
      (println help)

      (s/valid? ::options opts)
      (-> opts
          (select-keys [:url :headers :method :body])
          (http/request)
          (output-fn))

      :else
      (do (println "Invalid option(s)")
          (if verbose?
            (expound/expound ::options opts)
            (println "Use --verbose for more detail"))
          (println "Usage:")
          (println help)
          (flush)
          (System/exit 1)))))
