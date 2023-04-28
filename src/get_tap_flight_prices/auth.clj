(ns get-tap-flight-prices.auth
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [get-tap-flight-prices.config :as config]
            [clojure.tools.logging :as log]))

(defn get-token []
  (let [url "https://booking.flytap.com/bfm/rest/session/create"
        request-options {:content-type :json
                         :cookie-policy :none}
        form-params {:clientId (:clientId config/configs)
                     :clientSecret (:clientSecret config/configs)
                     :referralId (:referralId config/configs)
                     :market "NO"
                     :language "en"
                     :userProfile nil
                     :appModule "0"}
        response (client/post url
                              (merge request-options {:form-params form-params
                                                      :content-type :json}))
        parsed-response (json/read-str (:body response) :key-fn keyword)]
    (if (= (:status parsed-response) "200")
      (:id parsed-response)
      (log/warn (str "Error: " (get-in parsed-response [:errors 0 :desc]))))))