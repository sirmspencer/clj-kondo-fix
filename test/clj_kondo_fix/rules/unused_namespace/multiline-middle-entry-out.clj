(ns foo
  (:require [clojure.set :as cs]
            [clojure.string :as str]))

(cs/difference #{1} #{2})
(str/join [""] "")
