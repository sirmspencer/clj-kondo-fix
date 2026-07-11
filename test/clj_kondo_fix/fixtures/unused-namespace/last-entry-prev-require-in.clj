(ns foo
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]))

(set/difference #{1} #{2})
