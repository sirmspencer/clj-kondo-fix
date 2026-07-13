(ns clj-kondo-fix.impl.fixes.condition-always-true
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- open-paren-left
  "Scan left from col-idx on line to find the ( that starts the enclosing form."
  [line col-idx]
  (loop [i (dec col-idx) depth 0]
    (if (< i 0)
      nil
      (let [ch (nth line i)]
        (cond
          (#{\) \] \}} ch) (recur (dec i) (inc depth))
          (#{\( \[ \{} ch) (if (zero? depth) i (recur (dec i) (dec depth)))
          :else (recur (dec i) depth))))))

(defn- expr-end
  "Return the end index (exclusive) of the first complete expression in s."
  [s start-idx]
  (loop [i start-idx depth 0]
    (if (>= i (count s))
      i
      (let [ch (nth s i)]
        (cond
          (#{\( \[ \{} ch) (recur (inc i) (inc depth))
          (#{\) \] \}} ch) (if (zero? depth) i (recur (inc i) (dec depth)))
          (and (zero? depth) (= \space ch)) i
          :else (recur (inc i) depth))))))

(defn- skip-ws [s start-idx]
  (loop [i start-idx]
    (if (and (< i (count s)) (= \space (nth s i)))
      (recur (inc i))
      i)))

(defn- read-expr [s]
  (when (seq s)
    (let [end-idx (expr-end s 0)]
      [(subs s 0 end-idx) (str/trim (subs s (skip-ws s end-idx)))])))

(defn fix-condition-always-true-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)
              open-col (open-paren-left line col-idx)]
          (if (nil? open-col)
            [current-lines nil]
            (let [close-pos (find-matching-bracket-across-lines
                              current-lines line-idx open-col)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [inner (subs line (inc open-col) close-col)
                          op    (re-find #"^(?:if|when)\s" inner)]
                      (when op
                        (let [args-str  (str/trim (subs inner (count op)))
                              [_ cond-rest] (read-expr args-str)
                              [then-branch else-rest] (read-expr cond-rest)
                              new-form    then-branch
                              new-line    (str (subs line 0 open-col)
                                               new-form
                                               (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  condition always true → ..."))
                          [(assoc current-lines line-idx new-line) true])))))))))))))
