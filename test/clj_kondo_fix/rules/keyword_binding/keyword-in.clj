;;-; {:keys [:a]} → {:keys [a]} ;-;;
(let [{:keys [:a]} {:a 1}] a)
