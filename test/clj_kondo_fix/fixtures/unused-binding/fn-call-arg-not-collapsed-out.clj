(defn f [_ vals]
  (let [sql (some-fn/call {:results {:as vals}})]
    sql))
