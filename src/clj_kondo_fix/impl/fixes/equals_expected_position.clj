(ns clj-kondo-fix.impl.fixes.equals-expected-position
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-equals-expected-position-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line)))
            [current-lines nil]
            ;; Find the opening paren of the = form by scanning backward
            (let [open-col (loop [i col-idx]
                             (if (or (< i 0) (= \( (nth line i)))
                               i
                               (recur (dec i))))
                  close-pos (if (and (>= open-col 0) (= \( (nth line open-col)))
                              (find-matching-bracket-across-lines
                                current-lines line-idx open-col)
                              nil)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [line   (nth current-lines line-idx)
                          inner  (subs line (inc open-col) close-col)
                          ;; inner = "= ARG1 ARG2" where ARG2 is at col-idx
                          ;; Find ARG1 (between "= " and ARG2)
                          arg1-start (+ open-col 2)  ;; after "= "
                          arg1-end   (dec col-idx)     ;; before the space before ARG2
                          arg1       (str/trim (subs line arg1-start arg1-end))
                          arg2-start col-idx
                          arg2-end   close-col
                          arg2       (str/trim (subs line arg2-start arg2-end))
                          new-line   (str (subs line 0 (inc open-col))
                                          "= " arg2 " " arg1
                                          (subs line close-col))]
                      (swap! log conj (str "  " fu ":" (:line f)
                                           "  swapped expected value to first position"))
                      [(assoc current-lines line-idx new-line) true])))))))))))
