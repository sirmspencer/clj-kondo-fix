(ns clj-kondo-fix.impl.fixes.equals-float
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-equals-float-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \( (nth line col-idx)))
            [current-lines nil]
            (let [close-pos (find-matching-bracket-across-lines
                              current-lines line-idx col-idx)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [inner   (subs line (inc col-idx) close-col)
                          new-inner (if (.startsWith inner "= ")
                                      (str "== " (subs inner 2))
                                      inner)]
                      (if (= inner new-inner)
                        [current-lines nil]
                        (let [new-line (str (subs line 0 col-idx)
                                            "(" new-inner ")"
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  = → =="))
                          [(assoc current-lines line-idx new-line) true])))))))))))))
