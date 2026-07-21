(ns foo.removes-excluded-var
  (:refer-clojure :exclude [str]))

(def x 1)
