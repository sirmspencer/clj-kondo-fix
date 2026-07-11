;;-; multi-line :refer vector, unused var on its own line; line removed, closing bracket pulled up ;-;;
(ns foo
  (:require
   [clojure.string :refer [join
                           ends-with?]]))

(join [""] "")
