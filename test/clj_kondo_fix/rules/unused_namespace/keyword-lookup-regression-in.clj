;;-; (:count) keyword lookup in a threading macro; must not be matched as an ns clause to clean up ;-;;
(ns foo
  (:require [clojure.string :as s]))

(defn f [m]
  (-> m
      first
      (:count)))
