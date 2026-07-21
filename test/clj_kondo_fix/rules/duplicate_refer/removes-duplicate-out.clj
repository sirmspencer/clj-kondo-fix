(ns foo.removes-duplicate
  (:require [clojure.string :refer [join]]))

(def x (join "," ["a" "b"]))
