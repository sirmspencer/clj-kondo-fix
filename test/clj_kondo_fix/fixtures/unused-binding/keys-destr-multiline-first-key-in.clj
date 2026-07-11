;;-; multi-line :keys vector, first key on its own line unused; line removed, next key pulled up ;-;;
(defn f [{:keys [x
                 y
                 z]}] (+ y z))
