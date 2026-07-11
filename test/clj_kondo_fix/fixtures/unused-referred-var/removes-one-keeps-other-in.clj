;;-; one referred var unused, other is used; unused var removed, used one stays ;-;;
(ns foo (:require [clojure.string :refer [join ends-with?]]))

(join [""] "")
