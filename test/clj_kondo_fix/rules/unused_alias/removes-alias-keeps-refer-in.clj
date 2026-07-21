(ns foo.removes-alias-keeps-refer
  (:require [clojure.string :as str :refer [join]]))

(def x (join ", " ["a" "b"]))
