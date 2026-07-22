;;-; defrecord with duplicate field ;-;;
(ns foo)

(defrecord R [field another-field field])
