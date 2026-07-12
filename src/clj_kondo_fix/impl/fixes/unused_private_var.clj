(ns clj-kondo-fix.impl.fixes.unused-private-var
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-unused-private-var-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [var-name (some-> (re-find #"^Unused private var .+/(.+)$" (:message f)) second)
                             line-idx (dec (:line f))]
                         (if (nil? var-name)
                           [current-lines nil]
                           ;; Scan backwards from the finding line to locate the opening
                           ;; top-level (def...) or (defn-...) form.
                           (let [form-start (loop [i line-idx]
                                              (cond
                                                (< i 0) nil
                                                (re-find #"^\s*\(def" (nth current-lines i)) i
                                                :else (recur (dec i))))
                                 form-col   (when form-start
                                              (.indexOf (nth current-lines form-start) "("))]
                             (if (or (nil? form-start) (neg? form-col))
                               [current-lines nil]
                               (if-let [[end-line _] (find-matching-bracket-across-lines current-lines form-start form-col)]
                                 (let [;; Include a preceding blank line in the removal if present.
                                       remove-start (if (and (> form-start 0)
                                                             (str/blank? (nth current-lines (dec form-start))))
                                                      (dec form-start)
                                                      form-start)
                                       new-lines    (vec (concat (subvec current-lines 0 remove-start)
                                                                 (subvec current-lines (inc end-line))))]
                                   (swap! log conj (str "  " fu ":" (:line f) "  remove unused private var: " var-name))
                                   [new-lines true])
                                 [current-lines nil])))))))))
