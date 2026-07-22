(ns clj-kondo-fix.impl.fixes.unused-value
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- atom-end
  "Return the exclusive end index of a non-bracket form (atom, keyword,
   number, string, symbol) starting at col-idx on line.
   Stops at whitespace, comma, or a closing bracket."
  [line col-idx]
  (let [len (count line)]
    (loop [i col-idx in-str false esc false]
      (if (>= i len)
        len
        (let [c (get line i)]
          (cond
            esc                                               (recur (inc i) in-str false)
            (= c \\)                                         (recur (inc i) in-str true)
            (and in-str (= c \"))                            (inc i)
            (and (not in-str) (= c \"))                      (recur (inc i) true false)
            (and (not in-str) (#{\space \, \tab \) \] \}} c)) i
            :else                                            (recur (inc i) in-str false)))))))

(defn- find-form-end
  "Return [end-line-idx end-col-idx] (exclusive) of the form at col-idx.
   Returns nil if form end cannot be determined."
  [lines line-idx col-idx]
  (let [line (nth lines line-idx)
        ch   (get line col-idx)]
    (if (#{\( \[ \{} ch)
      (when-let [[cl cc] (find-matching-bracket-across-lines lines line-idx col-idx)]
        [cl (inc cc)])
      [line-idx (atom-end line col-idx)])))

(defn- eat-backward
  "Eat preceding whitespace/commas before col-idx. Returns removal start index."
  [line col-idx]
  (loop [i (dec col-idx)]
    (cond
      (neg? i)                          0
      (#{\space \, \tab} (get line i))  (recur (dec i))
      :else                             (inc i))))

(defn fix-unused-value-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0))
                           [current-lines nil]
                           (let [[end-line end-col] (or (find-form-end current-lines line-idx col-idx)
                                                        [nil nil])]
                             (if (or (nil? end-line) (not= end-line line-idx))
                               [current-lines nil]  ; multi-line form — skip
                               (let [line     (nth current-lines line-idx)
                                     rs       (eat-backward line col-idx)
                                     new-line (str (subs line 0 rs) (subs line end-col))]
                                 (if (= new-line line)
                                   [current-lines nil]
                                   (do (swap! log conj (str "  " fu ":" (:line f) "  remove unused value"))
                                       (if (str/blank? new-line)
                                         [(vec (concat (subvec current-lines 0 line-idx)
                                                       (subvec current-lines (inc line-idx)))) true]
                                         [(assoc current-lines line-idx new-line) true]))))))))))))
