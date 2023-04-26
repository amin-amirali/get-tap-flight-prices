(defproject get-tap-flight-prices "0.1.0"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-http "3.12.3"] ; running curl and other http calls
                 [cheshire "5.11.0"] ; working with JSONs
                 [org.clojure/data.json "2.4.0"] ; also for dealing with jsons
                 [clj-time "0.15.2"] ; working with time
                 [cprop "0.1.19"] ; env variables and config files
                 [mount "0.1.17"] ; mounting state
                 [org.clojure/tools.logging "1.2.4"] ; logging
                 [org.clojure/java.jdbc "0.7.12"] ; interfacing with DBs
                 [mysql/mysql-connector-java "8.0.32"] ; specifically with mysql
                 ]
  :main ^:skip-aot get-tap-flight-prices.core
  :aot [get-tap-flight-prices.core]
  :profiles {:uberjar {:uberjar-name "get-tap-flight-prices.jar"}}
  :jvm-opts ["-Duser.timezone=UTC"])
