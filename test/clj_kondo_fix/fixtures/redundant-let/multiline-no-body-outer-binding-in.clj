;;-; outer let has a multi-line binding vector; inner bindings appended with matching indentation ;-;;
(let [x 1
      y 2]
  (let [z 3]))
