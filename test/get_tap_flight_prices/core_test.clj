(ns get-tap-flight-prices.core-test
  (:require [clojure.test :refer :all]
            [get-tap-flight-prices.flights-info :as fi]
            [get-tap-flight-prices.auth :as auth]
            [cheshire.core :refer :all]
            [mount.core :as mount]
            ))

(def test-data-return
  {:cabinClass "E", :adt 1, :passengers {:ADT 1}, :paxSearch {:ADT 1}, :numSeat 1, :numSeats 1, :airlineId "TP", :bfmModule "BFM_BOOKING", :c14 0, :language "en", :market "NO",:validTripType true, :cmsId "string", :channelDetectionName "", :changeReturn false, :multiCityTripType false,:searchPoint true, :session "string",
   :origin ["OSL"],
   :destination ["LIS"],
   :departureDate ["15032024"],
   :returnDate "15042024",
   :oneWay true,
   :roundTripType false,
   :tripType "R"
   })

(def test-data-single
  {:cabinClass "E", :adt 1, :passengers {:ADT 1}, :paxSearch {:ADT 1}, :numSeat 1, :numSeats 1, :airlineId "TP", :bfmModule "BFM_BOOKING", :c14 0, :language "en", :market "NO",:validTripType true, :cmsId "string", :channelDetectionName "", :changeReturn false, :multiCityTripType false,:searchPoint true, :session "string",
   :origin ["OSL"],
   :destination ["LIS"],
   :departureDate ["15032024"],
   :returnDate "15032024",
   :oneWay true,
   :roundTripType false,
   :tripType "O"
   })

(defn test-flow []
  (mount/start)
  (let [token (auth/get-token)
        res (fi/get-data test-data-return token)]
    (->>
      (get-in res [:data :offers :listOffers])
      (map #(get-in % [:outbound]))
      (map #(get-in % [:listPaxPrice]))
      (map #(first %))
      (map #(get-in % [:price]))
      (reduce min))))
