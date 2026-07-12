(ns clj-kondo-fix.impl.fixes.redundant-do
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-redundant-do-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (> (+ col-idx 3) (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line (nth current-lines line-idx)]
                             (if (= "(do" (subs line col-idx (+ col-idx 3)))
                               (if-let [[match-line match-col] (find-matching-bracket-across-lines current-lines line-idx col-idx)]
                                 (let [match-line-str (nth current-lines match-line)
                                       new-match      (str (subs match-line-str 0 match-col)
                                                           (subs match-line-str (inc match-col)))
                                       dedent         (fn [l] (if (str/starts-with? l "  ") (subs l 2) l))
                                       new-lines
                                       (if (= line-idx match-line)
                                         ;; single-line: (parent (do a b)) → (parent a b)
                                         (assoc current-lines line-idx
                                                (str (str/trimr (subs line 0 col-idx))
                                                     " "
                                                     (str/triml (subs line (+ col-idx 3) match-col))
                                                     (subs line (inc match-col))))
                                         ;; multi-line
                                         (let [start-line (str (subs line 0 col-idx) (subs line (+ col-idx 3)))]
                                           (if (str/blank? start-line)
                                             ;; (do occupies its own line — remove it and dedent body
                                             (vec (concat
                                                   (subvec current-lines 0 line-idx)
                                                   (mapv dedent (subvec current-lines (inc line-idx) match-line))
                                                   [(dedent new-match)]
                                                   (subvec current-lines (inc match-line))))
                                             ;; (do is inline on a content line
                                             (-> current-lines
                                                 (assoc line-idx start-line)
                                                 (assoc match-line new-match)))))]
                                   (swap! log conj (str "  " fu ":" (:line f) "  remove redundant do"))
                                   [new-lines true])
                                 [current-lines nil])
                               [current-lines nil]))))))))
