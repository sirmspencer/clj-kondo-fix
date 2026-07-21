(ns clj-kondo-fix.impl.fixes.duplicate-set-key
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- find-element-end
  "Given a line string and a zero-indexed start position of a Clojure form,
  return the exclusive end position (character after the form)."
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx
           depth 0
           in-str false
           esc   false]
      (if (>= i len)
        len
        (let [c (get line i)]
          (cond
            esc          (recur (inc i) depth in-str false)
            (= c \\)     (recur (inc i) depth in-str true)
            (= c \")     (recur (inc i) depth (not in-str) false)
            (and (not in-str) (#{\( \[ \{} c))
            (recur (inc i) (inc depth) false false)
            (and (not in-str) (zero? depth) (#{\) \] \}} c))
            i
            (and (not in-str) (#{\) \] \}} c))
            (recur (inc i) (dec depth) false false)
            (and (not in-str) (zero? depth) (#{\space \, \}} c))
            i
            :else        (recur (inc i) depth in-str false)))))))

(defn- backward-separator
  "Scan backward from pos (exclusive) to find where the separator before
  a duplicate element begins.  Returns the start index (inclusive) to remove
  from, or col-idx if there is no separator."
  [line col-idx]
  (loop [pos (dec col-idx)]
    (cond
      (neg? pos) col-idx
      (#{\{ \#} (get line pos)) col-idx
      (#{\space \,} (get line pos)) (recur (dec pos))
      :else (inc pos))))

(defn fix-duplicate-set-key-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx  (dec (:line f))
                             col-idx   (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line         (nth current-lines line-idx)
                                 element-end  (find-element-end line col-idx)
                                 remove-start (backward-separator line col-idx)]
                             (if (or (>= remove-start element-end) (>= remove-start col-idx))
                               [current-lines nil]
                               (let [new-line (str (subs line 0 remove-start)
                                                   (subs line element-end))]
                                 (swap! log conj (str "  " fu ":" (:line f)
                                                      "  remove duplicate set element"))
                                 [(assoc current-lines line-idx new-line) true])))))))))
