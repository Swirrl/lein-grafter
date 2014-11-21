(ns leiningen.grafter
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
    (if-let [arg-map (find-pipeline-opts opts)]
      (if-let [arg-vec (first args-rest)]
        (merge arg-map {:args arg-vec})))))

(defn parse-pipeline-definition [pipeline-form]
  (if-let [n (first pipeline-form)]
    (if-let [arg-map (find-args (rest pipeline-form))]
      (merge {:name n} arg-map))))

(defn match-pipeline [form]
  (if (= 'defpipeline (first form))
    (parse-pipeline-definition (rest form))))

(defn try-read [reader]
  (try
    (read reader false nil)
    (catch Exception e (debug "Failed to read form: " e))))

(defn form-seq [reader]
  (if-let [f (try-read reader)]
    (cons f (form-seq reader))))

(defn get-clj-resource-reader [url]
  (let [rs (io/input-stream url)]
    (PushbackReader. (InputStreamReader. rs))))

(defn match-clj-resource-forms [url match-fn]
  (with-open [reader (get-clj-resource-reader url)]
    (apply vector (remove nil? (map match-fn (form-seq reader))))))

(defn dump-forms [url]
  (doseq [f (match-clj-resource-forms url identity)]
    (info f)))

(defmacro defpipeline [name doc meta args & body]
  (let [m (merge {:pipeline true} (or meta {}))]
    `(defn ~name ~doc ~m ~args ~@body)))

(defpipeline my-pipeline "Doc string" {:meta 1} [dataset]
  (println "Hello world!"))

(defn list-pipelines [resource-urls]
  (binding [*read-eval* false]
    (doseq [url resource-urls]
      (doseq [{:keys [name doc args]} (match-clj-resource-forms url match-pipeline)]
        (info name "\t\t" (string/join ", " args) "\t\t" doc)))))

(defn grafter
  "List the grafter pipelines in the project"
  [project & args]
  (list-pipelines (find-clj-classpath-resources)))
