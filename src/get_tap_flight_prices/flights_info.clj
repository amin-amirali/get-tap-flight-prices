(ns get-tap-flight-prices.flights-info
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [clojure.set :as set]))

(defn quickest-flights
  "Given a list of outbound flight schedules, returns a sub-list consisting of the fastest
  flights (i.e. the ones with the shortest travel time)."
  [all-flight-schedules]
  (let [min-duration (apply min (map :duration all-flight-schedules))]
    (filter #(= (:duration %) min-duration) all-flight-schedules)))

(defn best-flight-by-duration
  "Given a list of all outbound flights and another list of all available rates for each of these outbounds,
  returns the best flight (and respective rate) by duration.
  Price will be the tie-breaker. Otherwise, it doesnt matter."
  [all-flight-schedules all-flights-all-rates]
  (let [quickest-flights (quickest-flights all-flight-schedules)
        quickest-flights-with-rates (set/join quickest-flights all-flights-all-rates {:flight-id :outbound-id})]
    (->> quickest-flights-with-rates
         (sort-by :price)
         (take 1))))

(defn best-rate-grouped-by-outbound [all-outbounds-all-rates]
  (for [vals (vals (group-by #(:outbound-id %) all-outbounds-all-rates))]
    (apply min-key :price vals)))

(defn best-flight-by-price
  "Given a list of all outbound flights and another list of all available rates for each of these outbounds,
  returns the best flight (and respective rate) by price.
  Duration will be the tie-breaker. Otherwise, it doesnt matter."
  [all-flight-schedules all-flights-all-rates]
  (let [best-rate-per-flight (best-rate-grouped-by-outbound all-flights-all-rates)
        best-rate-incl-schedule (set/join all-flight-schedules best-rate-per-flight {:flight-id :outbound-id})]
    (->> best-rate-incl-schedule
         (sort-by :duration)
         (sort-by :price)
         (take 1))))

(defn getIdOutBoundFromList
  "From this: [{:idOutBound 1,(...)}{:idOutBound 2,(...)}]
   To this:   [1 2]"
  [l]
  (let [id-outbound-vector (into [] (map :idOutBound l))]
    id-outbound-vector))

(defn all-flights-all-rates [response]
  (flatten (for [flight-info (get-in response [:data :offers :listOffers])]
             (let [group-flights (:groupFlights flight-info)
                   group-flights-list (getIdOutBoundFromList group-flights)
                   outfare-family (select-keys flight-info [:outFareFamily])
                   total-price {:price (get-in flight-info [:totalPrice :price])}]
               (for [outbound-id group-flights-list]
                 (merge {:outbound-id outbound-id} outfare-family total-price))))))

(defn all-flight-schedules [res]
  (into [] (for [outbound-flight (get-in res [:data :listOutbound])]
             (let [id-flight (:idFlight outbound-flight)
                   duration (:duration outbound-flight)
                   num-stops (:numberOfStops outbound-flight)
                   all-segments (get-in outbound-flight [:listSegment])
                   all-leg-departure-and-arrivals (map #(select-keys % [:departureDate :arrivalDate]) all-segments)
                   dates {:departureDate (:departureDate (first all-leg-departure-and-arrivals))
                          :arrivalDate (:arrivalDate (last all-leg-departure-and-arrivals))
                          :flight-id id-flight
                          :duration duration
                          :num-stops num-stops}]
               dates))))

(defn best-flights [parsed-response]
  (let [all-flight-schedules (all-flight-schedules parsed-response)
        all-flights-all-rates (all-flights-all-rates parsed-response)
        best-flight-by-price (best-flight-by-price all-flight-schedules all-flights-all-rates)
        best-flight-by-duration (best-flight-by-duration all-flight-schedules all-flights-all-rates)
        unique-outbounts (distinct (concat best-flight-by-price best-flight-by-duration))]
    (map #(dissoc % :flight-id :outbound-id) unique-outbounts)))

(defn get-data [data-updated token]
  (let [url "https://booking.flytap.com/bfm/rest/booking/availability/search/"
        request-options {:headers {"Authorization" (str "Bearer " token)}
                         :cookie-policy :none}
        _ (log/info (str "Processing day: " (:departureDate data-updated) ", departing from "
                         (:origin data-updated) " towards " (:destination data-updated)))
        response (client/post url
                              (merge request-options {:form-params data-updated
                                                      :content-type :json}))
        parsed-response (json/read-str (:body response) :key-fn keyword)]
    (if (= (:status parsed-response) "200")
      parsed-response
      (log/warn (str "Error: " (get-in parsed-response [:errors 0 :desc]))))))

(defn get-best-flights [data-updated token]
  (let [from-airport (first (:origin data-updated))
        to-airport (first (:destination data-updated))
        res (get-data data-updated token)
        best-flights (best-flights res)
        source-list {:from from-airport}
        destiny-list {:to to-airport}]
    (map #(merge %1 %2 %3) best-flights (repeat source-list) (repeat destiny-list))))