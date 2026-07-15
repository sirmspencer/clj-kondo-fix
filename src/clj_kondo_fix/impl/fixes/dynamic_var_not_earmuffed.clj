(ns clj-kondo-fix.impl.fixes.dynamic-var-not-earmuffed
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-dynamic-var-not-earmuffed-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line)))
            [current-lines nil]
            (let [line-end (count line)
                  ;; find the end of the var name (symbol chars)
                  var-end (loop [i col-idx]
                            (if (>= i line-end)
                              i
                              (let [c (nth line i)]
                                (if (or (Character/isLetter c)
                                        (= c \-)
                                        (= c \_)
                                        (= c \*)
                                        (= c \/)
                                        (= c \.)
                                        (Character/isDigit c))
                                  (recur (inc i))
                                  i))))
                  new-line (str (subs line 0 col-idx)
                               "*"
                               (subs line col-idx var-end)
                               "*"
                               (subs line var-end))]
              (swap! log conj (str "  " fu ":" (:line f)
                                   "  added earmuffs to dynamic var"))
              [(assoc current-lines line-idx new-line) true])))))))