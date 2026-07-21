(ns clj-kondo-fix.impl.fixes.single-operand-comparison
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-single-operand-comparison-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0)
                                 (not= \( (get (nth current-lines line-idx) col-idx)))
                           [current-lines nil]
                           (let [close-pos (find-matching-bracket-across-lines
                                            current-lines line-idx col-idx)]
                             (if (nil? close-pos)
                               [current-lines nil]
                               (let [[close-line close-col] close-pos]
                                 (if (not= close-line line-idx)
                                   [current-lines nil]
                                   (let [line     (nth current-lines line-idx)
                                         new-line (str (subs line 0 col-idx)
                                                       "true"
                                                       (subs line (inc close-col)))]
                                     (swap! log conj (str "  " fu ":" (:line f)
                                                          "  (< op arg> → true"))
                                     [(assoc current-lines line-idx new-line) true])))))))))))
