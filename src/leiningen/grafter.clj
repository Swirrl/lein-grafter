(ns leiningen.grafter
  (:refer-clojure :exclude [list])
  (:require
   ;[grafter.pipeline :refer [apply-pipeline]]
   [leiningen.core.main :refer [info warn debug abort]]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project :as project]
   [clojure.string :as string])
  (:import
           [java.io InputStreamReader PushbackReader FileNotFoundException]))

(def grafter-profile {:dependencies '[[lein-grafter "0.5.0"]
                                      [leiningen "2.5.0"]]})

(def grafter-requires '(do (require 'clojure.java.io)
                           (require 'grafter.pipeline)
                           (require 'clojure.edn)
                           (require 'grafter.pipeline.plugin)
                           (require 'grafter.rdf)
                           (require 'grafter.rdf.io)
                           (require 'leiningen.core.main)))

(defn eval-in-grafter-project [project code-to-eval require]
  (let [profile (or (:grafter (:profiles project)) grafter-profile)
        project (project/merge-profiles project [profile])]
    (eval-in-project project
                     code-to-eval
                     require)))
(defn list
  "Lists all the grafter pipelines in the current project"
  ([project type]
   (let [function-call-form (if (or (nil? type) (#{"pipe" "graft"} type))
                              `(grafter.pipeline.plugin/list-pipelines ~type)
                              (leiningen.core.main/abort "Usage: lein grafter list [pipe|graft]"))]
     (eval-in-grafter-project project `(do
                                         (let [config# (clojure.edn/read-string (slurp "grafter-config.edn"))]
                                           (apply require (:pipeline-namespaces config#))
                                           (doseq [pipeline-desc# ~function-call-form]
                                             (leiningen.core.main/info pipeline-desc#))))
                              grafter-requires))))




(defn run
  "Run the specified grafter pipeline"
  [project pipeline inputs output]

  (let [syntree `(do
                   (let [config# (clojure.edn/read-string (slurp "grafter-config.edn"))]
                     (apply require (:pipeline-namespaces config#))

                     (try
                       ;; TODO need to interpret parameters against their types
                       ;; and call e.g. read-dataset if it's a dataset this
                       ;; should use code in grafter.pipeline.types
                       (let [results# ((symbol ~pipeline) ~@inputs)]
                         (leiningen.core.main/info (str ~@inputs " --[" ~pipeline "]--> " ~output " results " results#))
                         (if (re-find #"\.(xls|xlsx|csv)$" ~output)
                           (if (grafter.tabular/dataset? results#)
                             (grafter.tabular/write-dataset ~output results#)

                             (grafter.rdf/add (grafter.rdf.io/rdf-serializer ~output) results#))))

                       (catch FileNotFoundException ex#
                         (leiningen.core.main/abort (str "No such pipeline " ~pipeline " pipelines must be defined with defpipeline to be found by this plugin"))))))]
    (prn syntree)
    (eval-in-grafter-project project
                             syntree

                             (concat grafter-requires '((require 'grafter.tabular)
                                                        (require 'grafter.tabular.csv)
                                                        (require 'grafter.tabular.excel))))))

(defn ^{:subtasks [#'list #'run]} grafter
  "Plugin for managing and executing grafter pipelines."
  [project command & args]
  (case command
    "list" (list project (first args))
    "run" (let [pipeline (first args)
                arguments (rest args)
                inputs (butlast arguments)
                output (last args)]
            (if (or (nil? pipeline) (empty? inputs) (nil? output))
              (abort "Invalid arguments.  Usage: lein grafter run pipeline-name input ... output")
              (run project pipeline inputs output)))
    (abort (str "Unknown command: " command))))
