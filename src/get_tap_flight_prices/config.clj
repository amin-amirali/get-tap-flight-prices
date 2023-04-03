(ns get-tap-flight-prices.config
  (:require [cprop.core :refer [load-config]]
            [mount.core :refer [defstate] :as mount]))

(defstate configs
          :start
          (load-config :resource "config/config.edn"))
