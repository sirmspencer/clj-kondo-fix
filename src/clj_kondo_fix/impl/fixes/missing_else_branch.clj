(ns clj-kondo-fix.impl.fixes.missing-else-branch
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-missing-else-branch-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (> (+ col-idx 3) (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line (nth current-lines line-idx)]
                             ;; Replace "(if" with "(when" — works for (if, (if-not, (if-let, (if-some
                             ;; because we only replace the first 3 chars "(if", leaving "-not"/"-let"/"-some" intact.
                             (if (= "(if" (subs line col-idx (+ col-idx 3)))
                               (let [new-line (str (subs line 0 col-idx) "(when" (subs line (+ col-idx 3)))]
                                 (swap! log conj (str "  " fu ":" (:line f) "  replace (if with (when"))
                                 [(assoc current-lines line-idx new-line) true])
                               [current-lines nil]))))))))
