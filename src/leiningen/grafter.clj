(ns leiningen.grafter
  (:refer-clojure :exclude [list])
  (:require [leiningen.core.main :refer [info warn debug]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as string])
  (:import [net.sf.corn.cps CPScanner ResourceFilter]
           [java.io InputStreamReader PushbackReader]))

(defn find-clj-classpath-resources []
  (let [resource-filter (doto (ResourceFilter.) (.resourceName "*.clj"))
        args (into-array ResourceFilter [resource-filter])]
    (CPScanner/scanResources args)))

(defn find-pipeline-opts [opts]
  (let [c (count opts)
        doc (first opts)]
    (cond (= 0 c) {}
          (> c 2) nil
          (string? doc) {:doc doc}
          :else nil)))

(defn find-args [pipeline-form]
  (let [[opts args-rest] (split-with (complement vector?) pipeline-form)]
    (when-let [arg-map (find-pipeline-opts opts)]
      (when-let [arg-vec (first args-rest)]
        (merge arg-map {:args arg-vec})))))

(defn parse-pipeline-definition [pipeline-form]
  (when-let [n (first pipeline-form)]
    (when-let [arg-map (find-args (rest pipeline-form))]
      (merge {:name n} arg-map))))

(defn match-pipeline [form]
  (if (= 'defpipeline (first form))
    (parse-pipeline-definition (rest form))))

(defn try-read [reader]
  (try
    (read reader false ::eof)
    (catch Exception e (debug "Failed to read form: " e))))

(defn get-clj-resource-reader [url]
  (let [rs (io/input-stream url)]
    (PushbackReader. (InputStreamReader. rs))))

(defn get-form [l]
  (if (seq? l)
    (first l)
    nil))

(defn find-pipelines [reader]
  (loop [namespace nil
         found []]
    (let [f (try-read reader)]
      (if (= ::eof f)
        found
        (condp = (get-form f)
          'ns (recur (second f) found)
          'defpipeline (let [match (parse-pipeline-definition (rest f))]
                         (if match
                           (recur namespace (conj found (merge match {:ns namespace})))
                           (recur namespace found)))
          (recur namespace found))))))

(defn find-resource-pipelines [url]
  (with-open [reader (get-clj-resource-reader url)]
    (find-pipelines reader)))

(defmacro defpipeline [name doc meta args & body]
  (let [m (merge {:pipeline true} (or meta {}))]
    `(defn ~name ~doc ~m ~args ~@body)))

(defpipeline my-pipeline "Doc string" {:meta 1} [dataset]
  (println "Hello world!"))

(defn list-pipelines [resource-urls]
  (binding [*read-eval* false]
    (doseq [url resource-urls
            {:keys [name doc args ns]} (find-resource-pipelines url)]
      (info (str ns "/" name) "\t\t" (string/join ", " args) "\t\t" doc))))

(defn list
  "Lists all the grafter pipelines in the current project"
  []
  (list-pipelines (find-clj-classpath-resources)))

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
