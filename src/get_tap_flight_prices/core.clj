(ns get-tap-flight-prices.core
  (:gen-class)
  (:require [get-tap-flight-prices.config :as config]
            [clj-http.client :as client]
            [cheshire.core :refer :all]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [mount.core :as mount]))

(def custom-formatter (f/formatter "ddMMyyyy"))

(defn getIdOutBoundFromList
  "From this: [{:idOutBound 1,(...)}{:idOutBound 2,(...)}]
   To this:   [1 2]"
  [l]
  (let [id-outbound-vector (into [] (map :idOutBound l))]
    id-outbound-vector))

(defn parse-flight-dates [l]
  (into [] (for [outbound-info (get-in l [:data :listOutbound])]
             (let [segment (first (get-in outbound-info [:listSegment]))
                   dates (select-keys segment [:departureDate :arrivalDate])]
               dates))))
(defn parse-flight-info-list [l]
  (flatten (for [flight-info (get-in l [:data :offers :listOffers])]
             (let [group-flights (:groupFlights flight-info)
                   group-flights-list (getIdOutBoundFromList group-flights)
                   outfare-family (select-keys flight-info [:outFareFamily])
                   total-price {:price (get-in flight-info [:totalPrice :price])}]
               (for [outbound-id group-flights-list]
                 (merge {:outbound_id outbound-id} outfare-family total-price))))))

(defn find-cheapest-flight-for-all-outbounds [res]
  (let [all-flights (parse-flight-info-list res)]
    (for [vals (vals (group-by #(:outbound_id %) all-flights))]
      (apply min-key :price vals))))

(defn all-flights-info [res]
  (let [flight-dttm (parse-flight-dates res)]
    (for [flight-info (find-cheapest-flight-for-all-outbounds res)]
      (merge (dissoc flight-info :outbound_id) (nth flight-dttm (- (:outbound_id flight-info) 1))))))

(defn get-token []
  (let [url "https://booking.flytap.com/bfm/rest/session/create"
        request-options {:content-type :json
                         :cookie-policy :none}
        form-params {:clientId nil
                     :clientSecret nil
                     :referralId nil
                     :market "NO"
                     :language "en"
                     :userProfile nil
                     :appModule "0"}
        response (client/post url
                              (merge request-options {:form-params form-params
                                                      :content-type :json}))
        parsed-response (json/read-str (:body response) :key-fn keyword)]
    (if (= (:status parsed-response) "200")
      (:id parsed-response))))

(defn get-data [data-updated token]
  (let [url "https://booking.flytap.com/bfm/rest/booking/availability/search/"
        request-options {:headers {"Authorization" (str "Bearer " token)}
                         :cookie-policy :none}
        response (client/post url
                              (merge request-options {:form-params data-updated
                                                      :content-type :json}))
        parsed-response (json/read-str (:body response) :key-fn keyword)]
    (if (= (:status parsed-response) "200")
      parsed-response)))

(defn get-flight-prices [data-updated from to token]
  (let [res (get-data data-updated token)
        cheapest-flights (all-flights-info res)
        source-list {:from from}
        destiny-list {:to to}]
    (map #(merge %1 %2 %3) cheapest-flights (repeat source-list) (repeat destiny-list))))

(defn date-interval
  "Returns a seq of dates (as strings) between `start` and `end`, separated by 1 day"
  ([start end] (date-interval start end []))
  ([start end interval]
   (if (t/after? start end)
     interval
     (recur (t/plus start (t/days 1))
            end
            (concat interval [(f/unparse custom-formatter start)])))))

(defn get-prices-between-dates [from to start-dt end-dt token]
  (pmap
    #(let [sorted-map (into (sorted-map) (:data config/configs))
           data-updated (assoc sorted-map :returnDate %
                                          :origin [from]
                                          :destination [to]
                                          :departureDate [%])]
       (get-flight-prices data-updated from to token))
    (date-interval start-dt end-dt)))

(defn print-rows-as-tsv [prepared-rows]
  (let [startdttm (.format (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm") (new java.util.Date))]
    (->> prepared-rows
         (map #(assoc % :extraction_date startdttm))
         (map #(select-keys % [:departureDate :arrivalDate :outFareFamily :price :from :to :extraction_date]))
         (map vals)
         (map #(println (apply str (interpose "\t" %)))))))

(defn -main
  [& args]
  (mount/start)
  (let [[from-airport to-airport from-date to-date] args
        start-dt (f/parse from-date)
        end-dt (f/parse to-date)
        token (get-token)
        l1 (get-prices-between-dates from-airport to-airport start-dt end-dt token)
        l2 (get-prices-between-dates to-airport from-airport start-dt end-dt token)
        final-list (flatten (merge l1 l2))]
    (doall (print-rows-as-tsv final-list))))
