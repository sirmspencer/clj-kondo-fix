(ns clj-kondo-fix.impl.fixes.if-nil-return
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- expr-end
  "Return the end index (exclusive) of the first complete expression
   in s starting from start-idx."
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

(defn- parse-if-args [s]
  (let [a1-end (expr-end s 0)
        a1     (subs s 0 a1-end)
        a2-start (skip-ws s a1-end)
        a2-end   (expr-end s a2-start)
        a2       (subs s a2-start a2-end)
        a3-start (skip-ws s a2-end)]
    [a1 a2 (str/trim (subs s a3-start))]))

(defn fix-if-nil-return-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))]
          (if (or (< line-idx 0) (>= line-idx (count current-lines))
                  (< col-idx 0)
                  (not= \( (get (nth current-lines line-idx) col-idx)))
            [current-lines nil]
            (let [open-col  col-idx
                  close-pos (find-matching-bracket-across-lines
                              current-lines line-idx open-col)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [line     (nth current-lines line-idx)
                          inner    (subs line (inc open-col) close-col)]
                      (if (.startsWith inner "if ")
                        (let [[cond-expr then-branch else-branch]
                              (parse-if-args (str/trim (subs inner 3)))
                              [new-form log-msg]
                              (clojure.core/cond
                                (= "nil" then-branch)
                                [(str "(when-not " cond-expr " " else-branch ")")
                                 (str "  (if ... nil ...) → (when-not ...)")]
                                (= "nil" else-branch)
                                [(str "(when " cond-expr " " then-branch ")")
                                 (str "  (if ... ... nil) → (when ...)")]
                                :else [nil nil])]
                          (if new-form
                            (let [new-line (str (subs line 0 open-col)
                                                new-form
                                                (subs line (inc close-col)))]
                              (swap! log conj (str "  " fu ":" (:line f) log-msg))
                              [(assoc current-lines line-idx new-line) true])
                            [current-lines nil]))
                        [current-lines nil]))))))))))))
