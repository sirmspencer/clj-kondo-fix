(ns foo.removes-alias-keeps-refer
  (:require [clojure.string :refer [join]]))

(def x (join ", " ["a" "b"]))
