(ns clj-kondo-fix.impl.fixes.keyword-binding
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-keyword-binding-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \: (nth line col-idx)))
            [current-lines nil]
            (let [new-line (str (subs line 0 col-idx)
                                (subs line (inc col-idx)))]
              (swap! log conj (str "  " fu ":" (:line f)
                                   "  keyword binding :x → x"))
              [(assoc current-lines line-idx new-line) true])))))))
