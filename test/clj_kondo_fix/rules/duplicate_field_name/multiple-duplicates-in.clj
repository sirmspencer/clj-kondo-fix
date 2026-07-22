;;-; multiple duplicate fields ;-;;
(ns foo)

(deftype T [a b a b])
