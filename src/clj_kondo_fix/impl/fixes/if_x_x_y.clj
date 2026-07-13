(ns clj-kondo-fix.impl.fixes.if-x-x-y
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- expr-end
  "Return the end index (exclusive) of the first complete expression
   in s starting from start-idx. Handles () [] {} nesting."
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

(defn fix-if-x-x-y-in-file [file-path lines findings log]
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
                          inner    (subs line (inc open-col) close-col)
                          args-str (str/trim (subs inner 3))]
                      (if (.startsWith inner "if ")
                        (let [a-end  (expr-end args-str 0)
                              a-str  (subs args-str 0 a-end)
                              after-a (skip-ws args-str a-end)
                              b-start (skip-ws args-str (+ after-a (count a-str)))
                              b-str  (str/trim (subs args-str b-start))
                              new-line (str (subs line 0 open-col)
                                            "(or " a-str " " b-str ")"
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  (if A A B) → (or A B)"))
                          [(assoc current-lines line-idx new-line) true])
                        [current-lines nil]))))))))))))
