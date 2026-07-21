(ns clj-kondo-fix.impl.fixes.is-message-not-string
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn- scan-end
  "Scan forward from start-idx to find where a keyword/expression ends."
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx]
      (if (or (>= i len)
              (Character/isWhitespace (get line i))
              (#{\( \) \[ \] \{ \} \" \, \; \@ \^ \~} (get line i)))
        i
        (recur (inc i))))))

(defn fix-is-message-not-string-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx  (dec (:line f))
                             col-idx   (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0) (>= col-idx (count (nth current-lines line-idx))))
                           [current-lines nil]
                           (let [line (nth current-lines line-idx)
                                 end  (scan-end line col-idx)]
                             (if (<= end col-idx)
                               [current-lines nil]
                               (let [text    (subs line col-idx end)
                                     new-val (if (.startsWith text ":")
                                               (str "\"" (subs text 1) "\"")
                                               (str "(str " text ")"))]
                                 (swap! log conj (str "  " fu ":" (:line f)
                                                      "  " text " → " new-val))
                                 (let [new-line (str (subs line 0 col-idx)
                                                     new-val
                                                     (subs line end))]
                                   [(assoc current-lines line-idx new-line) true]))))))))))
