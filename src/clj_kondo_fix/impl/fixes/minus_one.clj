(ns clj-kondo-fix.impl.fixes.minus-one
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-minus-one-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))]
          (if (or (< line-idx 0) (>= line-idx (count current-lines))
                  (< col-idx 0)
                  (not= \( (get (nth current-lines line-idx) col-idx)))
            [current-lines nil]
            (let [open-col  col-idx
                  close-pos (find-matching-bracket-across-lines
                              current-lines line-idx open-col)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [line  (nth current-lines line-idx)
                          inner (subs line (inc open-col) close-col)]
                      ;; (- x 1) → (dec x)
                      (if (.endsWith inner " 1")
                        (let [arg      (subs line (+ open-col 3) (- close-col 2))
                              new-line (str (subs line 0 open-col)
                                            "(dec " arg ")"
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  (- ... 1) → (dec ...)"))
                          [(assoc current-lines line-idx new-line) true])
                        [current-lines nil]))))))))))))
