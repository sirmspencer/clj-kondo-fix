;;-; nested lets across lines with no body; inner bindings merged into outer ;-;;
(let [x 1]
  (let [y 2]))
