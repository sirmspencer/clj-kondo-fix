(ns foo)

(defn f [m]
  (-> m
      first
      (:count)))
