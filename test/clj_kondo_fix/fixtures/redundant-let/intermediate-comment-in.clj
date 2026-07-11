;;-; comment line between outer and inner let; moved before the merged let ;-;;
(let [x 1]
  ;; important note
  (let [y 2]
    body))
