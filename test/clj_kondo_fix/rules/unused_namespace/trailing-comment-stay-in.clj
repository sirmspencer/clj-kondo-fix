;;-; trailing comment belongs to the kept entry, not the removed one; comment stays in place ;-;;
(ns foo
  (:require [clojure.string :as s] ;; for set ops
            [clojure.set :as cs]))

(s/join [""] "")
