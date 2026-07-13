;;-; (str x) with single untyped var — kondo can't determine it's a string, no finding ;-;;
(defn greet [x] (str x))
