(defproject nginx-clojure/nginx-clojure-embed "0.4.2"
  :description "Embeding Nginx-Clojure into a standard clojure/java/groovy app without additional Nginx process"
  :url "https://github.com/nginx-clojure/nginx-clojure/tree/master/nginx-clojure-embed"
  :license {:name "BSD 3-Clause license"
            :url "http://opensource.org/licenses/BSD-3-Clause"}
  :plugins []
  :dependencies [
                 [nginx-clojure/nginx-clojure "0.4.2"]
                 ]
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :resource-paths ["res" "src/java"]
  :target-path "target/"
  :jar-exclusions [#"^test" #"Test.*class$" #".*for_test.clj$"]
  :javac-options ["-target" "1.6" "-source" "1.6" "-g" "-nowarn"]
  :test-paths ["test/clojure"]
  :profiles {
             :provided {
                        :dependencies [
                                  [org.clojure/clojure "1.5.1"]]
                        }
             :dev {:dependencies [;only for test / compile usage
                                  [ring/ring-core "1.2.1"]
                                  [compojure "1.1.6"]
                                  [clj-http "0.7.8"]
                                  [stylefruits/gniazdo "0.4.0"]
                                  ]}} 
  )
