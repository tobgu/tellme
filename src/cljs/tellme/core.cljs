(ns tellme.core
    (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [reagent.cookies :as cookies]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [ajax.core :as ajx]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [goog.crypt.base64 :as b64]
              [reagent-modals.modals :as reagent-modals]
              [tellme.handlers]
              [tellme.subs]
              [tellme.views :as views]
              [re-frame.core :refer [dispatch dispatch-sync]])
    (:import goog.History))

(defn https? []
  (= "https:" (.-protocol js/location)))

(defn auth-hash [user pass]
  (->> (str user ":" pass) (b64/encodeString) (str "Basic ")))

; TODO Stop using cookies to store stuff, use local storage / session storage instead.
(defn login! [user pass error]
  (do
    (.warn js/console (str "About to login"))
    (cond
      (empty? user) (reset! error "User name required")
      (empty? pass) (reset! error "Password required")
      :else (ajx/POST "/login" {:params {:user user :pass pass}
                                :handler #(do
                                            (.warn js/console (str "Logged in " (:token %)))
                                            (cookies/set! :tellme-roles (:roles %))
                                            (cookies/set! :tellme-user user)
                                            (cookies/set! :tellme-token (name (:token %)))
                                            (secretary/dispatch! "/"))
                                ; TODO: something better
                                :error-handler #(.warn js/console (str "Login error, response: " %))}))))

(defn logout! [error]
  (if (cookies/get :tellme-user)
    (ajx/POST "/logout" {:headers {"Authorization" (str "Token " (cookies/get :tellme-token))}
                         :handler #(do
                                    (cookies/remove! :tellme-user)
                                    (cookies/remove! :tellme-token)
                                    (cookies/remove! :tellme-roles)
                                    (secretary/dispatch! "/"))
                         ; TODO: something better
                         :error-handler #(.warn js/console (str "Logout error, response: " %))})
    (.warn js/console (str "Not logged in?"))))

;; -------------------------
;; Views

;; TODO Need to fix SSL

(defn input-value [input]
  (-> input .-target .-value))

(defn set-value! [target]
  (fn [source] (reset! target (input-value source))))


(defn sidebar []
  [:div#sidebar-wrapper
   [:ul.sidebar-nav
    [:li.sidebar-brand [:a {:href "#"} (cookies/get :tellme-user)]]
    [:li [:a {:href "#/report"} "Reports"]]
    [:li [:a {:href "#/stat"} "Statistics"]]
    [:li [:a {:href "#/admin"} "Admin"]]
    ]])

(defn logged-in-page []
  [:div#wrapper
   (sidebar)
   [:div.login-form
    [:span (str "Logged in")]
    [:span.button.login-button.out {:on-click #(logout! error)} "Logout"]]])

(defn login-page []
  (let [user (atom nil)
        pass (atom nil)
        error (atom nil)]
      (fn []
        [:div.login-form
            [:input {:on-change (set-value! user) :value @user :type "text" :placeholder "User name"}]
            [:input {:on-change (set-value! pass) :value @pass :type "password" :placeholder "Password"}]
            [:span.button.login-button {:on-click #(login! @user @pass error)} "Login"]
            (if-let [error @error]
              [:div.error error])])))


(defn start-page []
  [:div.container
    [reagent-modals/modal-window]
    [:div.jumbotron
      [:h1 "Welcome to TellMe"]
      [:p "Dedicated to simplifying your report workflow!"]
      [:div.btn.btn-lg.btn-primary {:on-click #(reagent-modals/modal! [views/login-form])} "Login"]
      [:div.btn.btn-lg.btn-default {:on-click #(dispatch [:login-success])} "Sign up"]
   ]])

(defn current-page []
  [:div [(session/get :current-page)]])

(defn server-message []
  [:div [(session/get :server-message)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (if (cookies/get :tellme-user)
    (session/put! :current-page #'logged-in-page)
    (session/put! :current-page #'start-page)))

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
  (session/put! :tellme-user (cookies/get :tellme-user))   ; The mix of sessions and cookies here feels hackish...
  (reagent/render-component [current-page] (.getElementById js/document "app")))
