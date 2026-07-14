(ns clj-kondo-fix.impl.fixes.cond-else
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-cond-else-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \: (nth line col-idx)))
            [current-lines nil]
            (let [kw-end (loop [i (inc col-idx)]
                           (if (>= i (count line))
                             i
                             (let [c (nth line i)]
                               (if (or (Character/isLetter c)
                                       (Character/isDigit c)
                                       (contains? #{\- \_ \* \+ \! \? \. \# \/ \:} c))
                                 (recur (inc i))
                                 i))))
                  new-line (str (subs line 0 col-idx)
                                ":else"
                                (subs line kw-end))]
              (swap! log conj (str "  " fu ":" (:line f)
                                   "  :default → :else"))
               [(assoc current-lines line-idx new-line) true])))))))
