(ns foo
  (:require [clojure.test :refer [is]]))

(is (= 1 1) "not-a-string")
