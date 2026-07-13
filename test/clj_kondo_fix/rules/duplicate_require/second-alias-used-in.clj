;;-; only the second alias is used; first entry removed and second entry pulled up inline ;-;;
(ns foo (:require [clojure.string :as s]
                  [clojure.string :as str]))

(str/join [""] "")
