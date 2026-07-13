(ns clj-kondo-fix.impl.fixes.equals-false
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-equals-false-in-file [file-path lines findings log]
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
                      (cond
                        ;; Case 1: (= false ARG) → (false? ARG)
                        (.startsWith inner "= false ")
                        (let [arg      (subs line (+ open-col 9) close-col)
                              new-line (str (subs line 0 open-col)
                                            "(false? " arg ")"
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  (= false ...) → (false? ...)"))
                          [(assoc current-lines line-idx new-line) true])

                        ;; Case 2: (= ARG false) → (false? ARG)
                        (.endsWith inner " false")
                        (let [arg      (subs line (+ open-col 3) (- close-col 6))
                              new-line (str (subs line 0 open-col)
                                            "(false? " arg ")"
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  (= ... false) → (false? ...)"))
                          [(assoc current-lines line-idx new-line) true])

                        :else [current-lines nil]))))))))))))
