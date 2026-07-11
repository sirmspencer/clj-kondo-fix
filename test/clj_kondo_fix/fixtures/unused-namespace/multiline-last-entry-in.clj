;;-; multi-line last entry; removed and closing ) merged onto previous entry line ;-;;
(ns foo
  (:require [clojure.set :as cs]
            [my.app.some.long-unused-ns
             :as unused]))

(cs/difference #{1} #{2})
