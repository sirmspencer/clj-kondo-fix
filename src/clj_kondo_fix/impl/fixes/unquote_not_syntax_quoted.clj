(ns clj-kondo-fix.impl.fixes.unquote-not-syntax-quoted
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-unquote-not-syntax-quoted-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0))
                           [current-lines nil]
                           (let [line     (nth current-lines line-idx)
                                 splicing? (= "~@" (subs line col-idx (min (count line) (+ col-idx 2))))
                                 tilde?    (= \~ (get line col-idx))
                                 remove-n  (cond splicing? 2 tilde? 1 :else 0)]
                             (if (zero? remove-n)
                               [current-lines nil]
                               (let [new-line (str (subs line 0 col-idx) (subs line (+ col-idx remove-n)))]
                                 (swap! log conj (str "  " fu ":" (:line f) "  remove unquote outside syntax-quote"))
                                 [(assoc current-lines line-idx new-line) true])))))))))

