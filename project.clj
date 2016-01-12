(defproject lein-grafter "0.6.0-SNAPSHOT"
  :description "A leiningen plugin for finding and running grafter pipelines from the commandline."
  :url "https://github.com/Swirrl/lein-grafter"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [^{:voom {:repo "https://github.com/swirrl/grafter" :branch "master"}}
                 [grafter "0.6.0-SNAPSHOT"]]
  :eval-in-leiningen true)
