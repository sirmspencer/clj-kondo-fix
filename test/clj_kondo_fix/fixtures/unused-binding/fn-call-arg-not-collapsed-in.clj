(defn f [{:keys [query]} vals]
  (let [sql (some-fn/call {:results {:as vals}})]
    sql))
