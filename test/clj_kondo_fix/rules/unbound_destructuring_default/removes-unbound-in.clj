(ns foo)
(let [{:keys [a] :or {b 1}} {:a 2}]
  a)
