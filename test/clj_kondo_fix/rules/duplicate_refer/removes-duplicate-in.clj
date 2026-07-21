(ns foo.removes-duplicate
  (:require [clojure.string :refer [join join]]))

(def x (join "," ["a" "b"]))
