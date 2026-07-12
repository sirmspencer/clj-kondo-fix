(ns clj-kondo-fix.impl.fixes.refer-all
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket]]))

(defn- find-require-entry-start [line col-idx]
  (loop [i (dec col-idx) depth 0]
    (if (< i 0)
      nil
      (let [ch (nth line i)]
        (case ch
          \[ (if (zero? depth) i (recur (dec i) (dec depth)))
          \] (recur (dec i) (inc depth))
          (recur (dec i) depth))))))

(defn fix-refer-all-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx    (dec (:line f))
                             col-idx     (dec (:col f))
                             line        (nth current-lines line-idx)
                             entry-start (find-require-entry-start line col-idx)]
                         (if (nil? entry-start)
                           [current-lines nil]
                           (let [end-idx (find-matching-bracket line entry-start)]
                             (if (nil? end-idx)
                               [current-lines nil]
                               (let [entry   (subs line entry-start (inc end-idx))
                                     cleaned (str/replace entry #"\s*:refer\s+:all" "")]
                                 (if (= cleaned entry)
                                   [current-lines nil]
                                   (let [new-line (str (subs line 0 entry-start) cleaned (subs line (inc end-idx)))]
                                     (swap! log conj (str "  " fu ":" (:line f) "  remove :refer :all from " entry))
                                     [(assoc current-lines line-idx new-line) true])))))))))))
