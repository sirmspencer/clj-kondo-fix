;;-; first var removed from :refer vector; remaining vars shift left with correct spacing ;-;;
(ns foo (:require [clojure.string :refer [join split starts-with?]]))

(split "" #",") (starts-with? "" "")
