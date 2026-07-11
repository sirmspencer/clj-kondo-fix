(ns foo
  (:require [clojure.string :as s]))

(defn f [m]
  (-> m
      first
      (:count)))
