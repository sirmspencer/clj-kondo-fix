;;-; :strs destructuring in a let binding; unused key removed (same behaviour as :keys) ;-;;
(let [{:strs [x y]} some-map] (foo some-map y))
