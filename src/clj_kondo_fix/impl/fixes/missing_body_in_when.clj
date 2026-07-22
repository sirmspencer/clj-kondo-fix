(ns clj-kondo-fix.impl.fixes.missing-body-in-when
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- condition-start-idx
  "Return the index of the first char of the condition in a (when...) form,
   or nil if the form cannot be parsed.
   Handles (when ...), (when-not ...), and any other (when-*) variants."
  [line col-idx]
  (when (and (< col-idx (count line))
             (.startsWith (subs line col-idx) "(when"))
    (let [rest-str  (subs line (+ col-idx 5))   ; chars after "(when"
          space-idx (.indexOf rest-str " ")]
      (when (>= space-idx 0)
        (let [after-space (+ col-idx 5 space-idx 1)]
          (loop [i after-space]
            (when (< i (count line))
              (if (#{\space \tab} (get line i))
                (recur (inc i))
                i))))))))

(defn- remove-start
  "Scan backward from col-idx eating preceding whitespace/commas.
   Returns the index to begin the removal from."
  [line col-idx]
  (if (zero? col-idx)
    0
    (loop [i (dec col-idx)]
      (cond
        (neg? i)                    0
        (#{\space \, \tab} (get line i)) (recur (dec i))
        :else                       (inc i)))))

(defn fix-missing-body-in-when-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0))
                           [current-lines nil]
                           (let [line     (nth current-lines line-idx)
                                 cond-idx (condition-start-idx line col-idx)]
                             (cond
                               (nil? cond-idx)
                               [current-lines nil]

                               (= \( (get line cond-idx))
                               [current-lines nil]  ; condition is a call — skip for side-effect safety

                               :else
                               (let [[close-line close-col]
                                     (or (find-matching-bracket-across-lines current-lines line-idx col-idx)
                                         [nil nil])]
                                 (if (or (nil? close-line) (not= close-line line-idx))
                                   [current-lines nil]  ; multi-line form — skip
                                   (let [rs       (remove-start line col-idx)
                                         new-line (str (subs line 0 rs) (subs line (inc close-col)))]
                                     (swap! log conj (str "  " fu ":" (:line f) "  remove empty (when) form"))
                                     (if (str/blank? new-line)
                                       [(vec (concat (subvec current-lines 0 line-idx)
                                                     (subvec current-lines (inc line-idx)))) true]
                                       [(assoc current-lines line-idx new-line) true]))))))))))))
