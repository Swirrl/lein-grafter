(ns leiningen.grafter
  (:refer-clojure :exclude [list])
  (:require
   [grafter.pipeline.search :refer [find-clj-classpath-resources find-resource-pipelines
                                    fully-qualified-name]]
   [leiningen.core.main :refer [info warn debug]]
   [clojure.string :as string])
  (:import [net.sf.corn.cps CPScanner ResourceFilter]
           [java.io InputStreamReader PushbackReader]))

(defn list
  "Lists all the grafter pipelines in the current project"
  []
  (doseq [url (find-clj-classpath-resources)
          pipeline (find-resource-pipelines url)]
    (info (fully-qualified-name pipeline) "\t\t" (string/join ", " (:args pipeline)) "\t\t" (:doc pipeline))))

(defn server
  "Starts a HTTP server hosting an import service for the pipelines in this project."
  [& args]
  nil)

(defn ^{:subtasks [#'list #'server]}
  grafter
  "Plugin for managing and executing grafter pipelines."
  [project command & args]
  (case command
    "list" (list)
    "server" (server args)
    (warn "Unknown command:" command)))
