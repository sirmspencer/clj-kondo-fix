(ns test-foo)

(defn foo [x]
  (cond
    (odd? x) 1
    :else 2
    ))
