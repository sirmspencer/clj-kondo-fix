;;-; map inside a function-call let rhs is not a destructuring position; only the :keys key is removed ;-;;
(defn f [{:keys [query]} vals]
  (let [sql (some-fn/call {:results {:as vals}})]
    sql))
