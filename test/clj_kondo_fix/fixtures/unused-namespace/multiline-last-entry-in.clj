(ns foo
  (:require [clojure.set :as cs]
            [my.app.some.long-unused-ns
             :as unused]))

(cs/difference #{1} #{2})
