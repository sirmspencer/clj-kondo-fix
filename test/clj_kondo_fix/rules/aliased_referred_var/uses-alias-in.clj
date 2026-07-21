(ns test-foo
  (:require [clojure.set :as set :refer [union]]))

(set/union #{1} #{2})
(union #{3} #{4})
