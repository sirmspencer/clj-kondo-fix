;;-; only key in :keys vector unused; entire map collapses to plain _ ;-;;
(defn f [{:keys [x]}])
