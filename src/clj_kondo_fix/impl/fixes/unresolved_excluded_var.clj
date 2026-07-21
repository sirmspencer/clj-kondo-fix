(ns clj-kondo-fix.impl.fixes.unresolved-excluded-var
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [cleanup-empty-clauses]]
            [clj-kondo-fix.impl.utils :refer [remove-referred-var-from-line]]))

(defn- extract-var-name [msg]
  (second (re-find #"^Unresolved excluded var: (.+)$" msg)))

(defn fix-unresolved-excluded-var-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [var-name (extract-var-name (:message f))
                             line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (nil? var-name) (< line-idx 0) (>= line-idx (count current-lines)))
                           [current-lines nil]
                           (let [line     (nth current-lines line-idx)
                                 new-line (remove-referred-var-from-line line var-name col-idx)]
                             (if (= new-line line)
                               [current-lines nil]
                               (let [new-lines
                                     (if (re-find #"^\s*[\]\)]+\s*$" new-line)
                                       (if (pos? line-idx)
                                         (let [prev     (nth current-lines (dec line-idx))
                                               brackets (str/trim new-line)]
                                           (vec (concat (take (dec line-idx) current-lines)
                                                        [(str prev brackets)]
                                                        (drop (inc line-idx) current-lines))))
                                         (assoc current-lines line-idx new-line))
                                       (assoc current-lines line-idx new-line))]
                                 (swap! log conj (str "  " fu ":" (:line f)
                                                      "  remove unresolved excluded var: " var-name))
                                 [new-lines true]))))))
                     cleanup-empty-clauses)))
