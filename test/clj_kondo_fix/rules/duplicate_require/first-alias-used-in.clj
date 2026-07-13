;;-; only the first alias is used; duplicate (second) entry removed, no renames needed ;-;;
(ns foo (:require [clojure.string :as s]
                  [clojure.string :as str]))

(s/join [""] "")
