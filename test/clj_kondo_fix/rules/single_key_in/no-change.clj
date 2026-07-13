;;-; (get-in m [:a :b]) has two keys — no finding ;-;;
(defn f [m] (get-in m [:a :b]))
