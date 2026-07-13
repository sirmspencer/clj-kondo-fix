;;-; multi-line :keys vector, unused key shares a line with other keys; unused key removed, others kept ;-;;
(defn f [{:keys [x y
                 z]}] (+ y z))
