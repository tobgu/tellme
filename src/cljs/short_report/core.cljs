(ns short-report.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [reagent.cookies :as cookies]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [ajax.core :as ajx]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [goog.crypt.base64 :as b64])
    (:import goog.History))

(defn https? []
  (= "https:" (.-protocol js/location)))

(defn auth-hash [user pass]
  (->> (str user ":" pass) (b64/encodeString) (str "Basic ")))

(defn login! [user pass error]
  (do
    (.warn js/console (str "About to login"))
    (cond
      (empty? user) (reset! error "User name required")
      (empty? pass) (reset! error "Password required")
      :else (ajx/POST "/login" {:params {:user user :pass pass}
                                :handler #(do
                                            (.warn js/console (str "Logged in " (:tellme-token %)))
                                            (cookies/set! :tellme-roles (:roles %))
                                            (cookies/set! :tellme-user user)
                                            (cookies/set! :tellme-token (name (:token %)))
                                            (session/put! :tellme-user user))
                                ; TODO: something better
                                :error-handler #(.warn js/console (str "Login error, response: " %))}))))

(defn logout! [error]
  (if (cookies/get :tellme-user)
    (ajx/POST "/logout" {:headers {"Authorization" (str "Token " (cookies/get :tellme-token))}
                         :handler #(do
                                    (cookies/remove! :tellme-user)
                                    (session/remove! :tellme-user user)
                                    (cookies/remove! :tellme-token)
                                    (cookies/remove! :tellme-roles))
                         ; TODO: something better
                         :error-handler #(.warn js/console (str "Logout error, response: " %))})
    (.warn js/console (str "Not logged in?"))))

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

;; TODO Need to fix SSL

(defn input-value [input]
  (-> input .-target .-value))

(defn set-value! [target]
  (fn [source] (reset! target (input-value source))))


:on-change #(reset! cursor (-> % .-target .-value))

(defn login-page []
  (let [user (atom nil)
        pass (atom nil)
        error (atom nil)]
      (fn []
        (if-let [user (session/get :tellme-user)]
          [:div.login-form
           [:span (str "Logged in as " user)]
           [:span.button.login-button.out {:on-click #(logout! error)} "Logout"]]
          [:div.login-form
            [:input {:on-change (set-value! user) :value @user :type "text" :placeholder "User name"}]
            [:input {:on-change (set-value! pass) :value @pass :type "password" :placeholder "Password"}]
            [:span.button.login-button {:on-click #(login! @user @pass error)} "Login"]
            (if-let [error @error]
              [:div.error error])]))))


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
                         (.warn js/console (str "Response " response)))}))


;; -------------------------
;; State
(def state
  (reagent/atom {:user []
                 :text "Some text"}))


;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (if (session/get :user)
    (session/put! :current-page #'home-page)
    (session/put! :current-page #'login-page)))

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
  (session/put! :tellme-user (cookies/get :tellme-user))   ; The mix of sessions and cookies here feels hackish...
  (reagent/render-component [current-page] (.getElementById js/document "app")))
