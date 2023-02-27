(ns get-tap-flight-prices.core
  (:gen-class)
  (:require [clj-http.client :as client]
            [cheshire.core :refer :all]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.walk :as w]))

(def custom-formatter (f/formatter "ddMMyyyy"))

(def data {:adt 2
           :chd 2
           :passengers {:ADT 2 :CHD 2}
           :paxSearch {:ADT 2 :CHD 2}
           :numSeat 4
           :numSeats 4
           :departureDate ["04062023"]
           :returnDate "04062023"
           :origin ["OSL"]
           :destination ["LIS"]
           :cabinClass "E"
           :airlineId "TP"
           :bfmModule "BFM_BOOKING"
           :c14 0
           :language "en"
           :market "NO"
           :oneWay true
           :roundTripType false
           :searchPoint true
           :session "string"
           :tripType "O"
           :validTripType true
           :cmsId "string"
           :channelDetectionName ""
           })

(defn get-data [data-updated]
  (let [url "https://booking.flytap.com/bfm/rest/booking/availability/search/"
        token "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiItYnFCaW5CaUh6NFlnKzg3Qk4rUFUzVGFYVVd5UnJuMVQvaVYvTGp4Z2VTQT0iLCJzY29wZXMiOlsiUk9MRV9BTk9OWU1PVVNfVVNFUiJdLCJob3N0IjoidGFwbHBhYjA0MDAwMDAxLmludGVybmFsLmNsb3VkYXBwLm5ldCIsInJhbmRvbSI6Ijk1WEhRIiwiaWF0IjoxNjc3NDQ4OTIzLCJleHAiOjE2Nzc0NTI1MjN9.ur3im70WlCeU7QbC4GD53aVQlGUDzgMsnvYPbBAEqbY"
        request-options {:headers {"Authorization" (str "Bearer " token)}
                         :cookie-policy :none}
        response (client/post url
                              (merge request-options {:form-params data-updated
                                                      :content-type :json}))
        parsed-response (json/read-str (:body response) :key-fn keyword)]
    (if (not= (:status parsed-response) "200")
      (println "ERROR!" (:status parsed-response) parsed-response)
      parsed-response)))

(defn get-flight-prices [data-updated from to]
  (let [res (get-data data-updated)
        dates-list (->> res
                        :data
                        :listOutbound
                        (map #(get-in % [:listSegment]))
                        (map #(nth % 0))
                        (map #(select-keys % [:departureDate :arrivalDate])))
        price-list (->> res
                        :data
                        :offers
                        :listOffers
                        (filter #(= "CLANEW" (:outFareFamily %)))
                        (map #(merge (select-keys % [:outFareFamily])
                                     (-> (get-in % [:totalPrice])
                                         (select-keys [:price])))))
        source-list {:from from}
        destiny-list {:to to}]
    (map #(merge %1 %2 %3 %4) dates-list price-list (repeat source-list) (repeat destiny-list))))

(defn date-interval
  "Returns a seq of dates (as strings) between `start` and `end`, separated by 1 day"
  ([start end] (date-interval start end []))
  ([start end interval]
   (if (t/after? start end)
     interval
     (recur (t/plus start (t/days 1))
            end
            (concat interval [(f/unparse custom-formatter start)])))))

(defn get-prices-between-dates [from to start-dt end-dt]
  (map
    #(let [sorted-map (into (sorted-map) data)
           data-updated (assoc sorted-map :returnDate %
                                          :origin [from]
                                          :destination [to]
                                          :departureDate [%])]
       (get-flight-prices data-updated from to))
    (date-interval start-dt end-dt)))

(defn print-rows-as-tsv [prepared-rows]
  (let [startdttm (.format (java.text.SimpleDateFormat. "yyyy/MM/dd HH:MM") (new java.util.Date))]
    (->> prepared-rows
         (map #(assoc % :extraction_date startdttm))
         (map #(select-keys % [:departureDate :arrivalDate :outFareFamily :price :from :to :extraction_date]))
         (map vals)
         (map #(println (apply str (interpose "\t" %)))))))

(defn -main
  [& args]
  (let [[from-airport to-airport from-date to-date] args
        start-dt (f/parse from-date)
        end-dt (f/parse to-date)
        l1 (get-prices-between-dates from-airport to-airport start-dt end-dt)
        l2 (get-prices-between-dates to-airport from-airport start-dt end-dt)
        final-list (flatten (merge l1 l2))]
    (print-rows-as-tsv final-list)))
