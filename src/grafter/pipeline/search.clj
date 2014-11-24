(ns grafter.pipeline.search
  (:refer-clojure :exclude [ns-name])
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as string])
  (:import [net.sf.corn.cps CPScanner ResourceFilter]
           [java.io InputStreamReader PushbackReader]))

(defn find-clj-classpath-resources []
  (let [resource-filter (doto (ResourceFilter.) (.resourceName "*.clj"))
        args (into-array ResourceFilter [resource-filter])]
    (CPScanner/scanResources args)))

(defn try-read [reader]
  (binding [*read-eval* false]
    (try
      (read reader false ::eof)
      (catch Exception e
        e))))

(defn forms
  "Given a Reader return a lazy sequence of its forms."
  [reader]
  (let [form (try-read reader)]
    (if-not (= ::eof form)
      (lazy-cat
       [form]
       (forms reader))
      (.close reader))))

(defn ns? [form]
  (when (seq? form)
    (= 'ns (first form))))

(defn pipeline? [form]
  (when (seq? form)
    (= 'defpipeline (first form))))

(defn ns-name [nsform]
  (second nsform))

(defn pipeline-name [pipeform]
  (second pipeform))

(defn pipeline-meta [pipeform]
  (nth pipeform 3))

(defn pipeline-args [pipeform]
  (nth pipeform 4))

(defn pipeline-doc [pipeform]
  (nth pipeform 2))

(defn pipeline-body [pipeform]
  (drop 5 pipeform))

(defrecord Pipeline [namespace name args doc meta body])

(defn fully-qualified-name [pipeline]
  (symbol (str (:namespace pipeline)) (str (:name pipeline))))

(defn form->Pipeline [ns form]
  (->Pipeline ns
              (pipeline-name form)
              (pipeline-args form)
              (pipeline-doc form)
              (pipeline-meta form)
              (pipeline-body form)))

(defn find-pipelines
  ([forms]
   (find-pipelines forms nil))

  ([[form & forms] ns]
   (when form
     (cond
      (ns? form) (find-pipelines forms (ns-name form))
      (pipeline? form) (let [pipe (form->Pipeline ns form)]
                         (lazy-seq (cons pipe
                                         (find-pipelines forms ns))))
      :else (find-pipelines forms ns)))))

(defn get-clj-resource-reader [url]
  (let [rs (io/input-stream url)]
    (PushbackReader. (InputStreamReader. rs))))

(defn find-resource-pipelines [url]
  (with-open [reader (get-clj-resource-reader url)]
    (find-pipelines (forms reader))))

(defmacro defpipeline [name doc meta args & body]
  (let [m (merge {:pipeline true} (or meta {}))]
    `(defn ~name ~doc ~m ~args ~@body)))

(defpipeline my-pipeline "Doc string" {:meta 1} [dataset]
  (println "Hello world!"))
