;;-; multi-line entry is the only require; entry and entire :require block removed, (ns foo) left clean ;-;;
(ns foo
  (:require
   [my.app.some.long-unused-ns
    :as unused]))
