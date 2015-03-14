(ns tellme.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [register-sub]]))

(register-sub
  :in-progress
  (fn [db _]
    (reaction (vals (:in-progress @db)))))
