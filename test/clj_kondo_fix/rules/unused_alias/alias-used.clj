(ns foo.alias-used
  (:require [clojure.string :as str]))

(def x (str/join ", " ["a" "b"]))
