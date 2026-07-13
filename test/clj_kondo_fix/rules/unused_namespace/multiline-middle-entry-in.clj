;;-; multi-line entry in the middle of three; removed, both single-line siblings preserved ;-;;
(ns foo
  (:require [clojure.set :as cs]
            [my.app.some.long-unused-ns
             :as unused]
            [clojure.string :as str]))

(cs/difference #{1} #{2})
(str/join [""] "")
