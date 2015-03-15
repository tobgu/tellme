(ns tellme.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
  :in-progress
  (fn [db _]
    (reaction (:in-progress @db))))

(register-sub
  :user
  (fn [db _]
    (reaction (:user @db))))

(register-sub
  :active-panel
  (fn [db _]
    (reaction (:active-panel @db))))
