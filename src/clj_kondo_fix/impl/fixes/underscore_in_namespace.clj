(ns clj-kondo-fix.impl.fixes.underscore-in-namespace
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-underscore-in-namespace-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines)))
                           [current-lines nil]
                           (when-let [old-ns (second (re-find #"^Avoid underscore in namespace name: (.+)$"
                                                              (:message f)))]
                             (let [new-ns   (str/replace old-ns "_" "-")
                                   line     (nth current-lines line-idx)
                                   new-line (str/replace line old-ns new-ns)]
                               (if (= new-line line)
                                 [current-lines nil]
                                 (do (swap! log conj (str "  " fu ":" (:line f)
                                                          "  " old-ns " \u2192 " new-ns))
                                     [(assoc current-lines line-idx new-line) true]))))))))))
