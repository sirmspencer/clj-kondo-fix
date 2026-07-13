;;-; :as config unused but :keys binding is used; :as clause removed entirely ;-;;
(defn f [{:keys [a] :as config}] a)
