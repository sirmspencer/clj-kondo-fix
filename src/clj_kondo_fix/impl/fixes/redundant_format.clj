(ns clj-kondo-fix.impl.fixes.redundant-format
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- open-paren-left
  [line col-idx]
  (loop [i (dec col-idx) depth 0]
    (if (< i 0)
      nil
      (let [ch (nth line i)]
        (cond
          (#{\) \] \}} ch) (recur (dec i) (inc depth))
          (#{\( \[ \{} ch) (if (zero? depth) i (recur (dec i) (dec depth)))
          :else (recur (dec i) depth))))))

(defn fix-redundant-format-in-file [file-path lines findings log]
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
                    (let [inner (subs line (inc open-col) close-col)]
                      (if (.startsWith inner "format ")
                        (let [arg-start (loop [i (+ open-col 7)]
                                          (if (and (< i (count line))
                                                   (= \space (nth line i)))
                                            (recur (inc i))
                                            i))
                              arg      (subs line arg-start close-col)
                              new-line (str (subs line 0 open-col)
                                            arg
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  (format ...) → ..."))
                          [(assoc current-lines line-idx new-line) true])
                        [current-lines nil]))))))))))))
