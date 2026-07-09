(ns foo
  (:require [clojure.string :as s]
            ;; this one is unused
            [clojure.set :as cs]))
(s/join [""] "")
