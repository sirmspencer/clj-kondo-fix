(defn f [_ vals]
  (let [z (g [{:bar vals} "data"])]
    z))
