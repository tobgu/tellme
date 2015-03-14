(ns tellme.db)


;; -- Default Value  ----------------------------------------------------------

(def default-value
  {:user {}
   :in-progress? false
   :error? false})


;; -- Session Storage  ----------------------------------------------------------

(def session-storage-key "tellme")

(defn ss->user       ;; read in security token from session storage
  []
  (some->> (.getItem js/sessionStorage session-storage-key)
           (js/JSON.parse)
           (#(js->clj %1 :keywordize-keys true))
           (hash-map :user)))

(defn user->ss!
  [db]
    (->> (clj->js (:user db))
         (js/JSON.stringify)
         (.setItem js/sessionStorage session-storage-key)))
