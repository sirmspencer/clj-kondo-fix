;;-; (get-in {:a [1 2]} [:a]) → (get {:a [1 2]} :a) ;-;;
(defn f [] (get-in {:a [1 2]} [:a]))
