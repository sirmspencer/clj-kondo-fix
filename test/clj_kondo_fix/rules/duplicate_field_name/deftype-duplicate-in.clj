;;-; deftype with duplicate field ;-;;
(ns foo)

(deftype T [field another-field field])
