(ns get-tap-flight-prices.date-helper
  (:require [clj-time.core :as t]
            [clj-time.format :as f]))

(def ^:private custom-formatter (f/formatter "ddMMyyyy"))

(defn date-interval
  "Returns a seq of dates (as strings) between `start` and `end`, separated by 1 day"
  ([start end] (date-interval start end []))
  ([start end interval]
   (if (t/after? start end)
     interval
     (recur (t/plus start (t/days 1))
            end
            (concat interval [(f/unparse custom-formatter start)])))))

(defn str-to-date [str]
  (f/parse str))
