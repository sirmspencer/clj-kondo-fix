(ns foo.removes-redundant-as
  (:require [clojure.string :as clojure.string :refer [join]]))

(def x (join ", " ["a" "b"]))
