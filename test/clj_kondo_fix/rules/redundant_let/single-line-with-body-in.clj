;;-; nested lets on one line with a body; inner bindings merged into outer, body preserved ;-;;
(let [x 2] (let [y 1] (+ x y)))
