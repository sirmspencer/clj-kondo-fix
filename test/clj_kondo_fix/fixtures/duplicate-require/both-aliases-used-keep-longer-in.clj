;;-; both aliases used; longer alias wins — shorter alias usages renamed and its entry removed ;-;;
(ns foo
  (:require [my.tools :as pt]
            [my.tools :as toolz]))

(pt/make-endpoint :x)
(toolz/make-exception {})
