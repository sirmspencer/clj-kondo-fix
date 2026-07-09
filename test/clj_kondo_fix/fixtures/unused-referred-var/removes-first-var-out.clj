(ns foo (:require [clojure.string :refer [split starts-with?]]))
(split "" #",") (starts-with? "" "")
