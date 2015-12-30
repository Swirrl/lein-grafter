(ns leiningen.grafter
  (:refer-clojure :exclude [list])
  (:require
   [leiningen.core.main :refer [info warn debug abort]]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project :as project]
   [clojure.edn]
   [clojure.string :as string])
  (:import
           [java.io InputStreamReader PushbackReader FileNotFoundException]))

(def grafter-profile {:dependencies '[[lein-grafter "0.6.0-SNAPSHOT"]
                                      [leiningen "2.5.0"]]})

(def grafter-requires ['clojure.java.io
                       'clojure.edn
                       'grafter.pipeline.plugin
                       'leiningen.core.main])

(defn eval-in-grafter-project [project code-to-eval more-requires]
  (let [profile (or (:grafter (:profiles project)) grafter-profile)
        project (project/merge-profiles project [profile])
        namespaces (:pipeline-namespaces (clojure.edn/read-string (slurp "grafter-config.edn")))
        requires `(require ~@(map #(clojure.core/list 'quote %1)
                                  (concat namespaces  more-requires)))]
    (eval-in-project project
                     code-to-eval
                     requires)))
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
                   (try
                     ;; TODO need to interpret parameters against their types
                     ;; and call e.g. read-dataset if it's a dataset this
                     ;; should use code in grafter.pipeline.types
                     (let [results# (~(symbol pipeline) ~@inputs)]
                       (leiningen.core.main/info ~(str (string/join inputs " ") " --[" pipeline "]--> " output))
                       (if (re-find #"\.(xls|xlsx|csv)$" ~output)
                         (if (grafter.tabular/dataset? results#)
                           (grafter.tabular/write-dataset ~output results#)
                           (throw (IllegalArgumentException. "You tried to generate tabular results from a non-tabular pipeline.")))
                         (grafter.rdf/add (grafter.rdf.io/rdf-serializer ~output) results#)))

                     (catch FileNotFoundException ex#
                       (leiningen.core.main/abort (str "No such pipeline " ~pipeline " pipelines must be exported with declare-pipeline to be found by this plugin")))))]

    (eval-in-grafter-project project
                             syntree
                             '[grafter.tabular
                               grafter.rdf
                               grafter.rdf.io
                               leiningen.core.main])))

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
