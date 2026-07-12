(ns clj-kondo-fix.impl.fixes.misplaced-docstring
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket
                                              find-docstring-end]]))

(defn fix-misplaced-docstring-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [docstring-line-idx (dec (:line f))
                             def-line-idx       (dec docstring-line-idx)]
                         (if (or (< def-line-idx 0) (>= docstring-line-idx (count current-lines)))
                           [current-lines nil]
                           (let [def-line       (nth current-lines def-line-idx)
                                 docstring-line (nth current-lines docstring-line-idx)
                                 bracket-idx    (.indexOf def-line "[")]
                             (if (neg? bracket-idx)
                               [current-lines nil]
                               (let [end-idx (find-matching-bracket def-line bracket-idx)]
                                 (if (nil? end-idx)
                                   [current-lines nil]
                                   (let [prefix (str/trimr (subs def-line 0 bracket-idx))]
                                     ;; if prefix is blank the defn name is on a different line
                                     ;; from the param vector — skip rather than produce invalid code
                                     (if (str/blank? prefix)
                                       [current-lines nil]
                                       (let [params                 (subs def-line bracket-idx (inc end-idx))
                                             after-params           (str/trim (subs def-line (inc end-idx)))
                                             docstring-end-line-idx (find-docstring-end current-lines docstring-line-idx)
                                             indent                 (re-find #"^\s*" docstring-line)]
                                         (swap! log conj (str "  " fu ":" (:line f) "  move docstring before params"))
                                         [(vec (concat (take def-line-idx current-lines)
                                                       [prefix]
                                                       (subvec (vec current-lines)
                                                               docstring-line-idx
                                                               (inc docstring-end-line-idx))
                                                       [(str indent params
                                                             (when-not (empty? after-params)
                                                               (str " " after-params)))]
                                                       (drop (inc docstring-end-line-idx) current-lines)))
                                          true])))))))))))))
