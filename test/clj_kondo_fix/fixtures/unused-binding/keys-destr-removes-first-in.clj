;;-; first key in :keys vector unused; key removed, remaining keys preserved ;-;;
(defn f [{:keys [x y z]}] (+ y z))
