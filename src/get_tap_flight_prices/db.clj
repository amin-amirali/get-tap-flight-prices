(ns get-tap-flight-prices.db
  (:require [get-tap-flight-prices.config :as config]
            [mount.core :refer [defstate] :as mount]
            [clojure.java.jdbc :as j]
            [clj-time.coerce :refer [to-sql-time]]))

(def target-table :tap_flight_prices)

(def flight-info->target-table-schema
  {:departureDate :departure_ts
   :arrivalDate :arrival_ts
   :outFareFamily :flight_class
   :price :price
   :from :departure_airport
   :to :arrival_airport
   :extraction_date :extraction_datetime})

(defstate db-spec
  :start {:dbtype "mysql"
          :dbname (:db-database config/configs)
          :user (:db-username config/configs)
          :password (:db-password config/configs)
          :host (:db-host config/configs)
          :port (:db-port config/configs)})

(defn insert-rows! [list-of-maps]
  (let [startdttm (new java.util.Date)]
    (j/insert-multi! db-spec
                     target-table
                     (map ;#(clojure.set/rename-keys % flight-info->target-table-schema)
                       #(-> {:departure_ts (to-sql-time (:departureDate %))
                             :arrival_ts (to-sql-time (:arrivalDate %))
                             :flight_class (:outFareFamily %)
                             :price (:price %)
                             :departure_airport (:from %)
                             :arrival_airport (:to %)}
                            (assoc :extraction_datetime (to-sql-time startdttm)))
                       list-of-maps))))
