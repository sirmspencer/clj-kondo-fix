;;-; both requires unused; entries and the entire :require block removed, ns closes cleanly ;-;;
(ns foo
  (:require [clojure.string :as s]
            [clojure.set :as cs]))
