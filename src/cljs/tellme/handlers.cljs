(ns tellme.handlers
  (:require
   [tellme.db     :refer [default-value ss->user user->ss!]]
   [re-frame.core :refer [register-handler path trim-v after dispatch]]
   [ajax.core :as ajx]))


(def user-ware [(after user->ss!) trim-v])

(register-handler
  :initialise-db
  (fn  [_ _]
     (merge default-value (ss->user))))


(register-handler
  :login-success
  user-ware
  (fn [db [user-data]]
    (.warn js/console (str "Login success, response: " user-data ", db: " db))
    (assoc db :user user-data :in-progress? false :error? false)))


(register-handler
  :login-failure
  user-ware
 (fn [db [args]]
    (.warn js/console (str "Login error, response: " args ", db " db))
    (assoc db :in-progress? false :error? true)))


(register-handler
 :login
 [trim-v]
 (fn  [db [user pass]]
   (ajx/POST "/login" {:params {:user user :pass pass}
                       :handler #(dispatch [:login-success %])
                       :error-handler #(dispatch [:login-failure %])})
   (assoc db :in-progress? true :error? false)))

