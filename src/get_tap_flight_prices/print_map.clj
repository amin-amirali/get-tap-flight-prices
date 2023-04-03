(ns get-tap-flight-prices.print-map)

(defn list-of-maps-as-tsv-with-current-timestamp
  "Receives a list of maps, outputs these maps as tsv, following the ordered keys. Current time is also
  included at the end of each of the row."
  [input-list ordered-keys]
  (let [startdttm (.format (java.text.SimpleDateFormat. "yyyy/MM/dd HH:mm") (new java.util.Date))]
    (->> input-list
         (map #(assoc % :extraction_date startdttm))
         (map #(select-keys % (merge ordered-keys :extraction_date)))
         (map vals)
         (map #(println (apply str (interpose "\t" %)))))))
