;;-; :as and the concrete binding both unused; :as removed and map collapses to _ ;-;;
(defn f [{conn :db/conn :as req}] {:status 501})
