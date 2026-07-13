;;-; multiple if-family forms on one line all lacking else branches; all converted to when-family ;-;;
(if true 1) (if-not true 1) (if-let [x 1] x) (if-some [x 1] x)
