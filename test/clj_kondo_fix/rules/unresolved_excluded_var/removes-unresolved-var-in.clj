(ns foo.removes-unresolved-var
  (:refer-clojure :exclude [nonexistent]))

(def x 1)
