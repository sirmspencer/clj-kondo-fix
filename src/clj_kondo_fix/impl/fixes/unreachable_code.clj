(ns clj-kondo-fix.impl.fixes.unreachable-code
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- find-enclosing-close
  "Scan forward from (line-idx, col-idx) tracking bracket depth.
   Returns [end-line-idx end-col-idx] of the ) that closes the enclosing
   form, or nil if not found."
  [lines start-line-idx start-col-idx]
  (loop [i start-line-idx
         j start-col-idx
         depth 0]
    (when (< i (count lines))
      (let [line (nth lines i)]
        (if (>= j (count line))
          (recur (inc i) 0 depth)
          (let [ch (nth line j)]
            (case ch
              \( (recur i (inc j) (inc depth))
              \) (if (zero? depth)
                  [i j]
                  (recur i (inc j) (dec depth)))
              (recur i (inc j) depth))))))))

(defn- trim-trailing-non-empty-lines
  [lines]
  (if (every? str/blank? lines)
    (vec (take-last 1 lines))
    (let [last-non-blank (dec (count lines))]
      (loop [i (dec (count lines))]
        (if (and (>= i 0) (str/blank? (nth lines i)))
          (recur (dec i))
          (subvec lines 0 (inc i)))))))

(defn fix-unreachable-code-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [[end-line-idx end-col-idx] (find-enclosing-close current-lines line-idx col-idx)]
                             (if (nil? end-line-idx)
                               [current-lines nil]
                               (let [changed? (volatile! false)
                                     new-lines (if (= line-idx end-line-idx)
                                                 ;; same line
                                                 (let [line  (nth current-lines line-idx)
                                                       new-line (str (subs line 0 col-idx) (subs line end-col-idx))]
                                                   (when (not= line new-line) (vreset! changed? true))
                                                   (assoc current-lines line-idx new-line))
                                                 ;; different lines
                                                 (let [first-line (nth current-lines line-idx)
                                                       last-line  (nth current-lines end-line-idx)
                                                       new-first  (str/trimr (subs first-line 0 col-idx))
                                                       new-last   (subs last-line end-col-idx)
                                                       head       (subvec current-lines 0 line-idx)
                                                       tail       (subvec current-lines (inc end-line-idx))
                                                       middle     (if (str/blank? new-last)
                                                                    [new-first]
                                                                    [new-first new-last])]
                                                   (vreset! changed? true)
                                                   (into head (into middle tail))))]
                                 (if @changed?
                                   (let [cleaned (trim-trailing-non-empty-lines new-lines)]
                                     (swap! log conj (str "  " fu ":" (:line f) "  removed unreachable code"))
                                     [cleaned true])
                                   [current-lines nil]))))))))))
