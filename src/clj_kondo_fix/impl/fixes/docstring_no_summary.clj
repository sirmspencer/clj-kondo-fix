(ns clj-kondo-fix.impl.fixes.docstring-no-summary
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- closing-quote [line col-idx]
  (loop [i (inc col-idx)]
    (if (>= i (count line))
      nil
      (if (= \" (nth line i))
        i
        (recur (inc i))))))

(defn- capitalize-first [s]
  (if (empty? s)
    s
    (str (str/upper-case (subs s 0 1)) (subs s 1))))

(defn- needs-period? [s]
  (let [trimmed (str/trimr s)]
    (and (pos? (count trimmed))
         (not (some #(= % (last trimmed)) [\. \! \?])))))

(defn fix-docstring-no-summary-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \" (nth line col-idx)))
            [current-lines nil]
            (let [close-idx (closing-quote line col-idx)]
              (if (nil? close-idx)
                [current-lines nil]
                (let [content   (subs line (inc col-idx) close-idx)
                      fixed     (cond-> content
                                  true capitalize-first
                                  (needs-period? content) (str "."))
                      new-line  (str (subs line 0 col-idx)
                                     "\"" fixed "\""
                                     (subs line (inc close-idx)))]
                  (swap! log conj (str "  " fu ":" (:line f)
                                       "  added capitalization/period to docstring"))
                  [(assoc current-lines line-idx new-line) true])))))))))
