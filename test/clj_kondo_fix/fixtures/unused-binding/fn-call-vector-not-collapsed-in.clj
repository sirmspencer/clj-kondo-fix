(defn f [{:keys [x]} vals]
  (let [z (g [{:bar vals} "data"])]
    z))
