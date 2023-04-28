(ns get-tap-flight-prices.print-map)

(def all-keys
  [:departureDate :arrivalDate :outFareFamily :price :from :to])

(defn as-tsv-with-current-timestamp
  "Receives a list of maps, outputs these maps as tsv, following the ordered keys. Current time is also
  included at the end of each of the row."
  [input-list]
  (let [startdttm (.format (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm") (new java.util.Date))]
    (->> input-list
         (map #(assoc % :extraction_date startdttm))
         (map #(select-keys % (merge all-keys :extraction_date)))
         (map vals)
         (map #(println (apply str (interpose "\t" %)))))))
