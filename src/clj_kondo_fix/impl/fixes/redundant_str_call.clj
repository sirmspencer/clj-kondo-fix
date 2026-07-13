(ns clj-kondo-fix.impl.fixes.redundant-str-call
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-redundant-str-call-in-file [file-path lines findings log]
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
                      ;; (str ARG) → ARG
                      (if (.startsWith inner "str ")
                        (let [arg      (subs line (+ open-col 5) close-col)
                              new-line (str (subs line 0 open-col)
                                            arg
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  (str ...) → ..."))
                          [(assoc current-lines line-idx new-line) true])
                        [current-lines nil]))))))))))))
