(defproject get-tap-flight-prices "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"] ; running curl and other http calls
                 [cheshire "5.11.0"] ; working with JSONs
                 [org.clojure/data.json "2.4.0"] ; also for dealing with jsons
                 [clj-time "0.15.2"] ; working with time
                 [cprop "0.1.19"] ; env variables and config files
                 [mount "0.1.17"] ; mounting state
                 ]
  :main ^:skip-aot get-tap-flight-prices.core
  :aot [get-tap-flight-prices.core]
  :profiles {:uberjar {:uberjar-name "get-tap-flight-prices.jar"}})
