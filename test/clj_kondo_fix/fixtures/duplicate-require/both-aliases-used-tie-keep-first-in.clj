;;-; both aliases same length; first alias wins — second alias usages renamed and its entry removed ;-;;
(ns foo
  (:require [clojure.string :as aa]
            [clojure.string :as bb]))

(aa/join [""] "")
(bb/upper-case "x")
