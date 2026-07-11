;;-; required namespace is actually used via its alias; kondo does not fire, no change made ;-;;
(ns foo (:require [clojure.string :as s])) (s/join [""] "")
