(ns get-tap-flight-prices.date-helper
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def ^:private custom-formatter (f/formatter "ddMMyyyy"))

(defn date-to-str [dt]
  (f/unparse custom-formatter dt))

(defn str-to-date [str]
  (f/parse str))

(defn plus-x-days-or-end-date
  "Returns a date that is the earliest between start+x days or end"
  [start-dt end-dt x-days]
  (let [start-plus-x (t/plus start-dt (t/days x-days))]
    (if (t/after? start-plus-x end-dt)
      end-dt
      start-plus-x)))

(defn date-interval
  "Returns a seq of dates (as strings) between `start` and `end`, separated by 1 day"
  ([start end] (date-interval start end []))
  ([start end interval]
   (if (t/after? start end)
     interval
     (recur (t/plus start (t/days 1))
            end
            (concat interval [(date-to-str start)])))))

(defn date-interval-sequence
  "Returns an interval of dates (as strings) between `start` and `end`, separated by 1 day. Each
  element includes a start-date and the start-date plus a date interval. If that second date is bigger
  than the end date, then end date is used instead."
  ([start end x-days] (date-interval-sequence start end x-days []))
  ([start end x-days interval]
   (if (t/after? start end)
     interval
     (recur (t/plus start (t/days 1))
            end
            x-days
            (concat interval [[(date-to-str start)
                               (date-to-str (plus-x-days-or-end-date start end x-days))]])))))
