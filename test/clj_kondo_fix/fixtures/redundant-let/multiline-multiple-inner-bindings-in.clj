;;-; inner let has multiple bindings; all merged into outer binding vector ;-;;
(let [a 1]
  (let [b 2
        c 3]
    (+ a b c)))
