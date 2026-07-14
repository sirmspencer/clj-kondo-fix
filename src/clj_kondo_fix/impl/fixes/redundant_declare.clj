(ns clj-kondo-fix.impl.fixes.redundant-declare
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn fix-redundant-declare-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line))
                  (not= \( (nth line col-idx)))
            [current-lines nil]
            (let [close-pos (find-matching-bracket-across-lines
                              current-lines line-idx col-idx)]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[close-line close-col] close-pos]
                  (if (not= close-line line-idx)
                    [current-lines nil]
                    (let [form-str   (subs line col-idx (inc close-col))
                          var-name   (second (re-find #"^Redundant declare: (.+)$" (:message f)))
                          content    (subs form-str 1 (dec (count form-str)))
                          parts      (str/split (str/trim content) #"\s+")
                          vars       (remove #{"declare"} parts)
                          remaining  (remove #(= % var-name) vars)]
                      (if (empty? remaining)
                        (let [new-line (str (subs line 0 col-idx)
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  removed redundant declare"))
                          [(assoc current-lines line-idx new-line) true])
                        (let [new-form (str "(declare " (str/join " " remaining) ")")
                              new-line (str (subs line 0 col-idx)
                                            new-form
                                            (subs line (inc close-col)))]
                          (swap! log conj (str "  " fu ":" (:line f)
                                               "  removed " var-name " from declare"))
                          [(assoc current-lines line-idx new-line) true])))))))))))))
