(ns foo.no-unused-exclude
  (:refer-clojure :exclude [str]))

(defn str [x] x)
