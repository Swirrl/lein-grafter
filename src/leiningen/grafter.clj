(ns leiningen.grafter
  (:refer-clojure :exclude [list])
  (:require
   [grafter.pipeline :refer [find-clj-classpath-resources find-resource-pipelines
                             fully-qualified-name apply-pipeline]]
   [leiningen.core.main :refer [info warn debug abort]]
   [leiningen.core.eval :refer [eval-in-project]]
   [leiningen.core.project :as project]
   [clojure.string :as string])
  (:import [net.sf.corn.cps CPScanner ResourceFilter]
           [java.io InputStreamReader PushbackReader FileNotFoundException]))

(def grafter-profile {:dependencies '[[lein-grafter "0.3.0-SNAPSHOT"]
                                      [leiningen "2.5.0"]]})

(def grafter-requires '(do (require 'grafter.pipeline)
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
  [project]
  (eval-in-grafter-project project `(doseq [url# (find-clj-classpath-resources)
                                            pipeline# (->> (find-resource-pipelines url#)
                                                           (remove #(instance? Exception %)))]
                                      (let [pattern# "%1$-60s %2$-30s %3$s"
                                            data# (into-array Object
                                                              [(fully-qualified-name pipeline#)
                                                               (string/join ", " (:args pipeline#))
                                                               (if-let [doc# (:doc pipeline#)]
                                                                 (str ";; " doc#)
                                                                 ";; No doc string")])]
                                        (leiningen.core.main/info (String/format pattern# data#))))
                           grafter-requires))

(defn run
  "Run the specified grafter pipeline"
  [project pipeline inputs output]

  (eval-in-grafter-project project
                           `(do (try
                                  (let [results# (grafter.pipeline/apply-pipeline ~pipeline '~inputs)]
                                    (if (re-find #"\.(xls|xlsx|csv)$" ~output)
                                      (grafter.tabular/write-dataset ~output results#)
                                      (grafter.rdf/add (grafter.rdf.io/rdf-serializer ~output) results#)))
                                  (catch FileNotFoundException ex#
                                    (leiningen.core.main/abort (str "No such pipeline " ~pipeline " pipelines must be defined with defpipeline to be found by this plugin")))))

                           (concat grafter-requires '((require 'grafter.tabular)
                                                      (require 'grafter.tabular.csv)
                                                      (require 'grafter.tabular.excel)))))

(defn ^{:subtasks [#'list #'run]} grafter
  "Plugin for managing and executing grafter pipelines."
  [project command & args]
  (case command
    "list" (list project)
    "run" (let [pipeline (first args)
                arguments (rest args)
                inputs (butlast arguments)
                output (last args)]
            (if (or (nil? pipeline) (empty? inputs) (nil? output))
              (abort "Invalid arguments.  Usage: lein grafter run pipeline-name input ... output")
              (run project pipeline inputs output)))
    (abort (str "Unknown command: " command))))
