;;-; :keys destructuring in a let binding; unused key removed (safe — no side effects on deref) ;-;;
(let [{:keys [x y]} some-map] (foo some-map y))
