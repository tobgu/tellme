(ns tellme.views
  (:require [re-frame.core :refer [subscribe dispatch]]))

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
