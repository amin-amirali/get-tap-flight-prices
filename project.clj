(defproject get-tap-flight-prices "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.7.0"]
                 [cheshire "5.11.0"]
                 [org.clojure/data.json "2.4.0"]
                 [clj-time "0.15.2"]]
  :main ^:skip-aot get-tap-flight-prices.core
  :aot [get-tap-flight-prices.core]
  :profiles {:uberjar {:uberjar-name "get-tap-flight-prices.jar"}})
