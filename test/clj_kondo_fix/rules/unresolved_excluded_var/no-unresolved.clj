(ns foo.no-unresolved
  (:refer-clojure :exclude [str]))

(defn str [x] x)
