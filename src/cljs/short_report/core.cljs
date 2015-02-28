(ns short-report.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [ajax.core :as ajx]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react])
    (:import goog.History))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to short-report!"]
   [:div [:a {:href "#/about"} "go to about page"]]
   [:div (:text state)]
   [:div [:a {:on-click (fn [_] (fetch-text!))} "Fetch some home text"]]])

(defn about-page []
  [:div [:h2 "About short-report"]
   [:div [:a {:href "#/"} "go to the home page"]]
   [:div (:text @state)]
   [:div [:a {:on-click (fn [_] (fetch-text!))} "Fetch some about text"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

(defn server-message []
  [:div [(session/get :server-message)]])

;; -------------------------
;; Ajax

(defn fetch-text! []
  (do
    (.warn js/console "About to fetch text")
    (ajx/GET "/api/text"
             {:handler (fn [text] (do
                                    (.warn js/console (str "Received" text))
                                    (swap! state assoc :text text)))
              :error-handler (fn [details] (.warn js/console (str "Failed to fetch text" details)))})))

(defn login []
  (ajx/GET "/api/text"
       {:handler (fn [data]
                   (reset! user (into @user data)))
        :error-handler (fn [response]
                         (println "ERROR: " (str response)))}))


;; -------------------------
;; State
(def state
  (reagent/atom {:user []
                 :text "Some text"}))


;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

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
  (reagent/render-component [current-page] (.getElementById js/document "app")))
