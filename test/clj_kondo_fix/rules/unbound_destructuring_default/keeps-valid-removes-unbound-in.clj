(ns foo)
(let [{:keys [a b] :or {b 1 c 2}} {:a 2}]
  [a b])
