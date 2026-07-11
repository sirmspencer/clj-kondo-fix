;;-; entry has no :as alias and all :refer vars removed; entire require entry removed ;-;;
(ns foo
  (:require [clojure.string :as s]
            [clojure.set :refer [rename-keys]]))

(s/join [""] "")
