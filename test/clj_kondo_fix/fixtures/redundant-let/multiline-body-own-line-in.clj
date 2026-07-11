;;-; nested lets with body on its own line; merged into one let with body preserved ;-;;
(let [x 1]
  (let [y 2]
    (+ x y)))
