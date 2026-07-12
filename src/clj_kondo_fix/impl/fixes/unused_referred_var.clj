(ns clj-kondo-fix.impl.fixes.unused-referred-var
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [cleanup-empty-clauses
                                                      remove-bare-requires]]
            [clj-kondo-fix.impl.utils :refer [remove-referred-var-from-line]]))

(defn- extract-ns-from-referred-var-msg [msg]
  (some-> (re-find #"^#'(.+)/[^/]+ is referred but never used$" msg) second))

(defn fix-unused-referred-var-in-file [file-path lines findings log]
  (let [fu       (->display-path file-path)
        ns-names (->> (distinct findings)
                      (map #(extract-ns-from-referred-var-msg (:message %)))
                      (filter some?)
                      distinct)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [var-name (some-> (re-find #"^#'(.+) is referred but never used$" (:message f)) second)
                             line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (nil? var-name) (< line-idx 0) (>= line-idx (count current-lines)))
                           [current-lines nil]
                           (let [line     (nth current-lines line-idx)
                                 new-line (remove-referred-var-from-line line var-name col-idx)]
                             (if (= new-line line)
                               [current-lines nil]
                               ;; If only closing brackets/parens remain on the line after removal
                               ;; (e.g. ]]) from a multi-line :refer vector), pull them up onto the
                               ;; preceding line instead of leaving a straggler.
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
                                 (swap! log conj (str "  " fu ":" (:line f) "  remove referred var: " var-name))
                                 [new-lines true]))))))
                     ;; Post-pass: cleanup empty clauses, then remove bare [ns] entries left
                     ;; after :refer cleanup, then run cleanup again.
                     (fn [current-lines]
                       (-> current-lines
                           cleanup-empty-clauses
                           (remove-bare-requires ns-names fu log)
                           cleanup-empty-clauses)))))
