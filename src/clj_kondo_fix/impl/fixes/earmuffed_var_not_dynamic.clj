(ns clj-kondo-fix.impl.fixes.earmuffed-var-not-dynamic
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-earmuffed-var-not-dynamic-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line)))
            [current-lines nil]
            (let [new-line (str (subs line 0 col-idx)
                                "^:dynamic "
                                (subs line col-idx))]
              (swap! log conj (str "  " fu ":" (:line f)
                                   "  added ^:dynamic to earmuffed var"))
              [(assoc current-lines line-idx new-line) true])))))))