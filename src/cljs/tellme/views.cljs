(ns tellme.views
  (:require [re-frame.core :refer [subscribe dispatch]]
            [reagent-modals.modals :as reagent-modals]
            [reagent.core :as reagent]
            [tellme.routes :as routes]))

(defn input-value [input]
  (-> input .-target .-value))

(defn set-value! [target]
  (fn [source] (reset! target (input-value source))))

; TODO: The notion of an error? true/false is probably to weak. Some more information should
;       be possible to supply.
(defn login-form []
  (let [user (atom nil)
        pass (atom nil)
        error? (subscribe [:error?])
        in-progress (subscribe [:in-progress])]
      (fn []
        [:div.tellme-modal
         [:h2 "Login"]
         (if @error?
           [:div.error-feedback (str "Invalid credentials, please try again")])
         [:form.login-form
          [:div.formgroup
           [:input.tellme-input {:on-change (set-value! user) :type "text" :placeholder "Username"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change (set-value! pass) :type "password" :placeholder "Password"}]]
          [:div.btn.btn-lg.btn-primary {:on-click #(dispatch [:login @user @pass])} "Login"]]])))


;; (defn input-valid?
;;   "Valid if input is less than 10 characters"
;;   [x]
;;   (> 10 (count x)))

;; (defn color [input]
;;   (let [valid-color "green"
;;         invalid-color "red"]
;;     (if (input-valid? input)
;;       valid-color invalid-color)))

;; (defn home []
;;   (let [state (reagent/atom {:user-input "some value"})]
;;     (fn []
;;       [:div [:h1 "Welcome to Reagent Cookbook!"]
;;        [:span {:style {:padding "20px"
;;                        :background-color (color (@state :user-input))}}
;;         [:input {:value (@state :user-input)
;;                  :on-change #(swap! state assoc :user-input (-> % .-target .-value))
;;                  }]]])))


(defn sign-up-form []
  (let [account-info (reagent/atom {:user "", :password "", :first-name "", :last-name "", :e-mail ""
                                    :org-name "", :org-short-name ""})
        error? (subscribe [:error?])
        in-progress (subscribe [:in-progress])]
      (fn []
        [:div.tellme-modal
         [:h2 "Sign up"]
         (if @error?
           [:div.error-feedback (str "Invalid input, please try again")])
         [:form.sign-up-form
          [:div.formgroup
           [:span (str (:org-short-name @account-info) "_")]
           [:input.tellme-input {:on-change #(swap! account-info assoc :user (-> % .-target .-value)) :type "text" :placeholder "Username"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change #(swap! account-info assoc :password (-> % .-target .-value)) :type "password" :placeholder "Password"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change #(swap! account-info assoc :first-name (-> % .-target .-value)) :type "text" :placeholder "First name"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change #(swap! account-info assoc :last-name (-> % .-target .-value)) :type "text" :placeholder "Last name"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change #(swap! account-info assoc :e-mail (-> % .-target .-value)) :type "email" :placeholder "E-mail"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change #(swap! account-info assoc :org-name (-> % .-target .-value)) :type "text" :placeholder "Organization name"}]]
          [:div.formgroup
           [:input.tellme-input {:on-change #(swap! account-info assoc :org-short-name (-> % .-target .-value)) :type "text" :placeholder "Organization short name"}]]
          [:div.btn.btn-lg.btn-primary
           {:on-click #(dispatch [:sign-up @account-info])} "Sign up"]]]))
  )

(defn start-page []
  [:div.container
   [reagent-modals/modal-window]
   [:div.jumbotron
    [:h1 "Welcome to TellMe"]
    [:p "Dedicated to simplifying your report workflow!"]
    [:div.btn.btn-lg.btn-primary {:on-click #(reagent-modals/modal! [login-form])} "Login"]
    [:div.btn.btn-lg.btn-default {:on-click #(reagent-modals/modal! [sign-up-form])} "Sign up"]   ;; TODO Fix sign up
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
