(ns tellme.core
    (:require [reagent.core :as reagent :refer [atom]]
              [cljsjs.react :as react]
              [tellme.routes :as routes]
              [tellme.handlers]
              [tellme.subs]
              [tellme.views :as views]
              [re-frame.core :refer [dispatch-sync]]))

;; TODO Need to fix SSL
(defn https? []
  (= "https:" (.-protocol js/location)))

;; -------------------------
;; Initialize app
(defn init! []
  (dispatch-sync [:initialise-db])
  (routes/hook-browser-navigation!)
  (reagent/render-component [views/tellme-app] (.getElementById js/document "app")))
