(ns foo.alias-differs
  (:require [clojure.string :as str]))

(def x (str/join ", " ["a" "b"]))
