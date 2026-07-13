;;-; last key in :keys vector unused; key removed, preceding keys preserved ;-;;
(defn f [{:keys [x y z]}] (+ x y))
