(ns foo)
(let [{:keys [a b] :or {b 1}} {:a 2}]
  [a b])
