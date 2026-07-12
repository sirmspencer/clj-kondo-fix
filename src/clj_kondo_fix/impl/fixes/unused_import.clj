(ns clj-kondo-fix.impl.fixes.unused-import
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [cleanup-empty-clauses]]
            [clj-kondo-fix.impl.utils :refer [remove-referred-var-from-line]]))

(defn fix-unused-import-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [var-name (some-> (re-find #"^Unused import (.+)$" (:message f)) second)
                             line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (nil? var-name) (< line-idx 0) (>= line-idx (count current-lines)))
                           [current-lines nil]
                           (let [line       (nth current-lines line-idx)
                                 new-line   (remove-referred-var-from-line line var-name col-idx)
                                 ;; If removing the class left a bare [package] vector (no classes
                                 ;; remaining), strip it so cleanup-empty-clauses can tidy the clause.
                                 final-line (str/replace new-line #"\s*\[[a-z][a-zA-Z0-9.]*\s*\]" "")]
                             (if (= final-line line)
                               [current-lines nil]
                               (do (swap! log conj (str "  " fu ":" (:line f) "  remove unused import: " var-name))
                                   [(assoc current-lines line-idx final-line) true]))))))
                     cleanup-empty-clauses)))
