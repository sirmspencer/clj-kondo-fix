;;-; #_ discard form between outer and inner let; moved before the merged let ;-;;
(let [x 1]
  #_(println "hello")
  (let [y 2]
    body))
