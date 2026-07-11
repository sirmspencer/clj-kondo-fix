;;-; middle key in :keys vector unused; key removed, flanking keys and spacing preserved ;-;;
(defn f [{:keys [x y z]}] (+ x z))
