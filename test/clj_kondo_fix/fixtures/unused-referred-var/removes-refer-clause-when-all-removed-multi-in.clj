;;-; all referred vars removed from a multi-require ns; bare entry removed, sibling require preserved ;-;;
(ns foo (:require [test :as t]
                  [clojure.string :refer [join]]))
