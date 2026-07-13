(ns clj-kondo-fix.impl.fixes.docstring-leading-trailing-whitespace
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- find-close-quote
  [lines line-idx col-idx]
  (loop [i line-idx, j (inc col-idx), escaped? false]
    (if (>= i (count lines))
      nil
      (let [line (nth lines i)
            ch   (when (< j (count line)) (nth line j))]
        (cond
          (nil? ch)   (recur (inc i) 0 false)
          escaped?    (recur i (inc j) false)
          (= \\ ch)   (recur i (inc j) true)
          (= \" ch)   [i j]
          :else       (recur i (inc j) false))))))

(defn fix-docstring-leading-trailing-whitespace-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \" (nth line col-idx)))
            [current-lines nil]
            (let [close-pos (find-close-quote current-lines line-idx col-idx)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [inner  (subs line (inc col-idx) close-col)
                          trimmed (str/trim inner)]
                      (if (= inner trimmed)
                        [current-lines nil]
                        (let [new-line (str (subs line 0 (inc col-idx))
                                            trimmed
                                            "\""
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  trimmed docstring whitespace"))
                          [(assoc current-lines line-idx new-line) true])))))))))))))
