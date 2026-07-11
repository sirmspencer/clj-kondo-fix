;;-; nested lets with body inline after inner binding close; merged correctly ;-;;
(let [x 1]
  (let [y 2] (+ x y)))
