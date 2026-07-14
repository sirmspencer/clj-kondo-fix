(ns clj-kondo-fix.impl.fixes.docstring-blank
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-docstring-blank-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \" (nth line col-idx)))
            [current-lines nil]
            (let [close-quote (loop [i (inc col-idx)]
                                (if (>= i (count line))
                                  nil
                                  (if (= \" (nth line i))
                                    i
                                    (recur (inc i)))))
                  new-line (if close-quote
                             (str (subs line 0 (dec col-idx))
                                  (subs line (inc close-quote)))
                             line)]
              (swap! log conj (str "  " fu ":" (:line f)
                                   "  removed blank docstring"))
              [(assoc current-lines line-idx new-line) true])))))))
