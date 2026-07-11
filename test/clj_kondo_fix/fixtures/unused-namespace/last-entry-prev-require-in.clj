;;-; last entry removed when :require is on the preceding line; closing )) merged onto surviving ] ;-;;
(ns foo
  (:require [clojure.set :as set]
            [clojure.tools.logging :as log]))

(set/difference #{1} #{2})
