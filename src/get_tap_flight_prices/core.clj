(ns get-tap-flight-prices.core
  (:gen-class)
  (:require [get-tap-flight-prices.config :as config]
            [get-tap-flight-prices.date-helper :as date-helper]
            [get-tap-flight-prices.flights-info :as fi]
            [get-tap-flight-prices.db :as db]
            [get-tap-flight-prices.print-map :as print-map]
            [get-tap-flight-prices.auth :as auth]
            [cheshire.core :refer :all]
            [mount.core :as mount]))

(defn get-prices-between-dates [from-airport to-airport start-dt end-dt token]
  (pmap
    #(let [sorted-map (into (sorted-map) (:data config/configs))
           data-updated (assoc sorted-map :returnDate (second %)
                                          :origin [from-airport]
                                          :destination [to-airport]
                                          :departureDate [(first %)])]
       (fi/get-best-flights data-updated token))
    (date-helper/date-interval-sequence start-dt end-dt 5)))

(defn -main
  [& args]
  (mount/start)
  (let [[from-airport to-airport from-date to-date] args
        start-dt (date-helper/str-to-date from-date)
        end-dt (date-helper/str-to-date to-date)
        token (auth/get-token)
        l1 (get-prices-between-dates from-airport to-airport start-dt end-dt token)
        l2 (get-prices-between-dates to-airport from-airport start-dt end-dt token)
        final-list (flatten (merge l1 l2))]
    (doall
      (db/insert-rows! final-list)
      ;(print-map/as-tsv-with-current-timestamp final-list)
      )))
