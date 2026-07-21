(ns clj-kondo-fix.impl.fixes.non-arg-vec-return-type-hint
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clojure.string :as str]))

(defn- scan-to-whitespace
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx]
      (if (or (>= i len) (Character/isWhitespace (get line i)))
        i
        (recur (inc i))))))

(defn- skip-whitespace
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx]
      (if (or (>= i len) (not (Character/isWhitespace (get line i))))
        i
        (recur (inc i))))))

(defn fix-non-arg-vec-return-type-hint-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line       (nth current-lines line-idx)
                                 hint-start (loop [i (dec col-idx)]
                                              (if (or (neg? i) (= \^ (get line i)))
                                                i
                                                (recur (dec i))))
                                 type-end   (scan-to-whitespace line col-idx)
                                 after-hint (skip-whitespace line type-end)
                                 arg-col    (loop [i after-hint]
                                              (if (or (>= i (count line)) (= \[ (get line i)))
                                                i
                                                (recur (inc i))))]
                             (if (or (neg? hint-start) (>= type-end arg-col)
                                     (>= after-hint arg-col))
                               [current-lines nil]
                               (let [hint-trim (subs line hint-start type-end)
                                     name     (str/trim (subs line after-hint arg-col))
                                     prefix   (subs line 0 hint-start)
                                     rest     (subs line arg-col)
                                     new-line (str prefix name " " hint-trim " " rest)]
                                 (swap! log conj (str "  " fu ":" (:line f)
                                                      "  move " hint-trim " before arg vector"))
                                 [(assoc current-lines line-idx new-line) true])))))))))
