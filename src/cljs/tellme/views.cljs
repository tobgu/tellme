(ns tellme.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent-modals.modals :as reagent-modals]))

(defn input-value [input]
  (-> input .-target .-value))

(defn set-value! [target]
  (fn [source] (reset! target (input-value source))))

(defn login-form []
  (let [user (atom nil)
        pass (atom nil)
;        error (atom nil)
;        result (subscribe [:login-result])
        in-progress (subscribe [:in-progress])]
      (fn []
        [:div.login-form
            [:input {:on-change (set-value! user) :type "text" :placeholder "Username"}]
            [:input {:on-change (set-value! pass) :type "password" :placeholder "Password"}]
            [:span.button.login-button {:on-click #(dispatch [:login @user @pass])} "Login"]])))

(defn sidebar []
  [:div#sidebar-wrapper
   [:ul.sidebar-nav
    [:li.sidebar-brand [:a {:href "#"} "User Foo"]]
    [:li [:a {:href "#/report"} "Reports"]]
    [:li [:a {:href "#/stat"} "Statistics"]]
    [:li [:a {:href "#/admin"} "Admin"]]
    ]])


(defn start-page []
  [:div.container
   [reagent-modals/modal-window]
   [:div.jumbotron
    [:h1 "Welcome to TellMe"]
    [:p "Dedicated to simplifying your report workflow!"]
    [:div.btn.btn-lg.btn-primary {:on-click #(reagent-modals/modal! [login-form])} "Login"]
    [:div.btn.btn-lg.btn-default {:on-click #(dispatch [:login-success])} "Sign up"]   ;; TODO Fix sign up
    ]])

;; This hack to hide the modal once on the start page works for now
;; but something better is probably needed in the future
(defn hide-modal! []
  (let [m (js/jQuery (reagent-modals/get-modal))]
    (.call (aget m "modal") m "hide")
    m))

(defn logged-in-page []
  (hide-modal!)
  [:div#wrapper
   (sidebar)
   [:div.login-form
    [:span (str "Logged in")]
    [:span.button.login-button.out {:on-click #(dispatch [:logout])} "Logout"]]])


(defn tellme-app []
  (let [active-panel (subscribe [:active-panel])
        user (subscribe [:user])]
    (fn []
      (.warn js/console (str "Rendering app for user " @user ", active panel " @active-panel))
      [:div
       (if (empty? @user)
         [start-page]
         [logged-in-page])])))

; reaction vs subscribe vs subscription???
