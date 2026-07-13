;;-; multi-line :keys vector, last key on its own line unused; closing brackets merged onto preceding line ;-;;
(defn f [{:keys [x
                 y
                 z]}] (+ x y))
