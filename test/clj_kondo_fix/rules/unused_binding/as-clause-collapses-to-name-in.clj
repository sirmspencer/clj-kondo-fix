;;-; unused :keys binding but :as state is used; map collapses to the :as name ;-;;
(defn f [{:keys [db] :as state} arg] (foo state arg))
