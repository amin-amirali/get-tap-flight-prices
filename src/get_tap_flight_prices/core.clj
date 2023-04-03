(ns get-tap-flight-prices.core
  (:gen-class)
  (:require [get-tap-flight-prices.config :as config]
            [get-tap-flight-prices.print-map :as print-map]
            [get-tap-flight-prices.date-helper :as date-helper]
            [get-tap-flight-prices.flights-info :as fi]
            [cheshire.core :refer :all]
            [mount.core :as mount]))

(defn get-prices-between-dates [from-airport to-airport start-dt end-dt token]
  (pmap
    #(let [sorted-map (into (sorted-map) (:data config/configs))
           data-updated (assoc sorted-map :returnDate %
                                          :origin [from-airport]
                                          :destination [to-airport]
                                          :departureDate [%])]
       (fi/get-flight-prices data-updated from-airport to-airport token))
    (date-helper/date-interval start-dt end-dt)))

(defn -main
  [& args]
  (mount/start)
  (let [[from-airport to-airport from-date to-date] args
        start-dt (date-helper/str-to-date from-date)
        end-dt (date-helper/str-to-date to-date)
        token (fi/get-token)
        l1 (get-prices-between-dates from-airport to-airport start-dt end-dt token)
        l2 (get-prices-between-dates to-airport from-airport start-dt end-dt token)
        final-list (flatten (merge l1 l2))]
    (doall
      (print-map/list-of-maps-as-tsv-with-current-timestamp
        final-list
        [:departureDate :arrivalDate :outFareFamily :price :from :to]))))
