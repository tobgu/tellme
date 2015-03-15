(ns tellme.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [tellme.handlers]
              [tellme.subs]
              [tellme.views :as views]
              [re-frame.core :refer [dispatch dispatch-sync]])
    (:import goog.History))

;; TODO Need to fix SSL
(defn https? []
  (= "https:" (.-protocol js/location)))

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (dispatch [:active-panel :foo]))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn init! []
  (hook-browser-navigation!)
  (dispatch-sync [:initialise-db])
  (reagent/render-component [views/tellme-app] (.getElementById js/document "app")))
