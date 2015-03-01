(ns short-report.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.anti-forgery :refer :all]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.auth.backends.session :refer [session-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [environ.core :refer [env]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [cheshire.core :refer :all]))


(def authdata {:a "1"
               :b "2"})


(def roles {:a [:admin :user],
            :b [:user]})


(defn login-authenticate
  [request]
  (let [user (get-in request [:params :user])
        pass (get-in request [:params :pass])
        session (:session request)]
    (if-let [found-password (get authdata (keyword user))]
      (if (= found-password pass)
        (let [nexturl (get-in request [:query-params :next] "/")
              session (assoc session :identity (keyword user))]
          {:status 201 :session session :body {:roles (get roles (keyword user))}})
        {:status 401 :body {:user user :pass pass}})
      {:status 401 :body {:user user :pass pass}})))



(defroutes routes
  (GET "/csrf" [] (generate-string {:csrf-token *anti-forgery-token*}))
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (GET "/api/text" [] "Text from the API")
  (POST "/login" [] login-authenticate)
  (GET "/login" [] "Baah!") ; login-authenticate
  (resources "/")
  (not-found "Not Found"))

; TODO
; ------------
; - SSL
; - Log out
; - Session timeout
; - DB (SQL postgres with yesql?)
; - Store user and session data in DB. Create model of organizations.
; - Check roles when accessing different URLs. Both front end for how to render and backend for what is actually
;   OK to access for a user belonging to a specific organization and with a specific set of roles.

(defn unauthenticated-handler
  [request metadata]
  (do
    (.println System/out "!!!! Fooo !!!!")
    (cond
      (authenticated? request) (println "!!! Authenticated !!!")
      :else {:status 401})))


;; Create an instance of auth backend.

(def auth-backend
  (session-backend {:unauthorized-handler unauthenticated-handler}))



(def app
  (let [handler (-> routes
                    (wrap-transit-response {:encoding :json :opts {}})
                    (wrap-authentication auth-backend)
                    (wrap-defaults site-defaults)
                    (wrap-transit-params {:keywords? true :opts {}}))]
    (if (env :dev?) (wrap-reload(wrap-exceptions handler) handler))))
