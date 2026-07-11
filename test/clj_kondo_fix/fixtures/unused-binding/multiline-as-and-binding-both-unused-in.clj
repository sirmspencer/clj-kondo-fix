;;-; multi-line form where :as and binding both unused; same collapse behaviour across lines ;-;;
(defn f [{conn :db/conn
          :as req}] {:status 501})
