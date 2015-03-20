(ns tellme.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent-modals.modals :as reagent-modals]
            [tellme.routes :as routes]))

(defn input-value [input]
  (-> input .-target .-value))

(defn set-value! [target]
  (fn [source] (reset! target (input-value source))))

(defn login-form []
  (let [user (atom nil)
        pass (atom nil)
        error? (subscribe [:error?])
        in-progress (subscribe [:in-progress])]
      (fn []
        [:div
        (if @error?
          [:div (str "Invalid credentials, please try again")])
          [:div.login-form
              [:input {:on-change (set-value! user) :type "text" :placeholder "Username"}]
              [:input {:on-change (set-value! pass) :type "password" :placeholder "Password"}]
              [:span.button.login-button {:on-click #(dispatch [:login @user @pass])} "Login"]]])))


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

(defn report-panel []
  [:span "Do some reports!"])

(defn analyze-panel []
  [:span "Analyze this!"])

; Potentially separate into report and user administration
(defn admin-panel []
  [:span "Administrate!"])

(def panels (sorted-map :admin   {:label "Admin"   :panel admin-panel}
                        :analyze {:label "Analyze" :panel analyze-panel}
                        :report  {:label "Report"  :panel report-panel}))

(defn panel-for-user [panel user]
  ; TODO Make the start panel user dependent
  (case panel
        :start :report
        panel))


(defn sidebar [user active-panel]
  [:div#sidebar-wrapper
   (into [:ul.sidebar-nav]
         (concat
          [
           [:li.sidebar-logo [:a {:href "#"} [:div.circle "TM"]]]
           [:li.sidebar-brand [:a {:href "#"} (:user user)]]
           ]
          (for [[k p] (seq panels)]
            [:li [:a {:class (if (= k active-panel) "active" "inactive") :href (routes/panel-route {:panel (name k)})} (:label p)]])
          [[:div.btn.btn-xs.btn-default.logout-button {:on-click #(dispatch [:logout])} "Logout"]]
          ))
   ])

(defn logged-in-page [user active-panel]
  (hide-modal!)
  (let [panel (panel-for-user active-panel user)]
    [:div#wrapper
     (sidebar user panel)
     (if active-panel  ; Initially the panel may be empty so we need to guard against that case
       [(get-in panels [panel :panel])])]))

(defn tellme-app []
  (let [active-panel (subscribe [:active-panel])
        user (subscribe [:user])]
    (fn []
      [:div
       (if (empty? @user)
         [start-page]
         (logged-in-page @user @active-panel))])))

; reaction vs subscribe vs subscription???
