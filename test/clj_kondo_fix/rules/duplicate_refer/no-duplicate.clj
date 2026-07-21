(ns foo.no-duplicate
  (:require [clojure.string :refer [join]]))

(def x (join "," ["a" "b"]))
