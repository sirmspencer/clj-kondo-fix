;;-; comment-only line precedes the removed entry; comment de-indented and preserved outside require ;-;;
(ns foo
  (:require [clojure.string :as s]
            ;; this one is unused
            [clojure.set :as cs]))

(s/join [""] "")
