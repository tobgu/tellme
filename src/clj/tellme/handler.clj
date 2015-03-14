(ns tellme.handler
  (:require [compojure.core :refer [GET POST defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.anti-forgery :refer :all]
            [ring.middleware.session.cookie :refer [cookie-store]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
            [cheshire.core :refer :all]
            [buddy.auth.backends.token :refer [token-backend]]
            [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
            [buddy.auth :refer [authenticated? throw-unauthorized]]
            [buddy.hashers :as hashers]
            [yesql.core :refer [defqueries]]))


(def authdata {:a "1"
               :b "2"})


(def roles {:a [:admin :user],
            :b [:user]})


; TODO start using SQL backend
; TODO Remove session dependency (is is possible to remove the session altogether?)
; TODO buddy hash
; TODO Use fairly high iteration count (12, default => 0.5 s) for passwords but a much lower (4? or even lower => 2 ms) for the token.

(defn login-authenticate
  [request]
  (let [user (get-in request [:params :user])
        pass (get-in request [:params :pass])
        session (:session request)]
    (if-let [found-password (get authdata (keyword user))]
      (if (= found-password pass)
        (let [nexturl (get-in request [:query-params :next] "/")
              session (assoc session :identity (keyword user))]
          {:status 201 :session session :body {:user user :roles (get roles (keyword user)) :token :2f904e245c1f5}})
        {:status 401 :body {:user user :pass pass}})
      {:status 401 :body {:user user :pass pass}})))


; "Functions" use kebab case and "data" use snake case for SQL compatibility etc?

; TODO: Move connection info away from here
(def db-connection
  {:user "tellme"
   :password "letmein"
   :subname "//localhost:5434/tellme"
   :subprotocol "postgresql"})

(defqueries "db/queries.sql")


(defn create-user [request org_short_name roles]
  (let [user_name (str org_short_name "_" (get-in request [:params :user_name]))
        hashed_password (hashers/encrypt (get-in request [:params :password]))
        email (get-in request [:params :email])
        first_name (get-in request [:params :first_name])
        last_name (get-in request [:params :last_name])]
    (create-user<! db-connection user_name hashed_password email first_name last_name org_short_name)
  ))

(defn create-account [request]
  (let [org_short_name (get-in request [:params :org_short_name])
        org_long_name (get-in request [:params :org_long_name])]
    (do
      (create-organization<! db-connection org_short_name org_long_name)
      (create-user request org_short_name [:admin])
      {:status 201}
    )))


;; (create-account {:params {:org_long_name "foo bar"
;;                           :org_short_name "foo"
;;                           :user_name "the_user"
;;                           :password "the_pass"
;;                           :email "a@b.com"
;;                           :first_name "Tobias"
;;                           :last_name "G"}})

;; Define a in-memory relation between tokens and users:
(def tokens {:2f904e245c1f5 :a
             :45c1f5e3f05d0 :b})

;; Define a authfn, function with the responsibility
;; to authenticate the incoming token and return an
;; identity instance

(defn my-authfn
  [request token]
  (let [token (keyword token)]
    (get tokens token nil)))

;; Create a instance
(def backend (token-backend {:authfn my-authfn}))


(defn logout
  [request]
  (if (authenticated? request)
    (let [session (:session request)]
      {:status 200 :session (dissoc session :identity)})
    (throw-unauthorized)))


(defroutes routes
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?) :csrf-token *anti-forgery-token*}))
  (GET "/api/text" [] "Text from the API")
  (POST "/login" [] login-authenticate)
  (POST "/logout" [] logout)
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


(def app
  (let [handler (-> routes
                    (wrap-transit-response {:encoding :json :opts {}})
                    (wrap-defaults (-> site-defaults
                                       (assoc-in [:session :store] (cookie-store {:key "abcdefghijklmnop"}))
                                       (assoc-in [:security :anti-forgery] false)))   ; TODO: Change key and move out of repo
                    (wrap-transit-params {:keywords? true :opts {}})
                    (wrap-authentication backend)
                    (wrap-authorization backend))]
    (if (env :dev?) (wrap-reload(wrap-exceptions handler) handler))))
