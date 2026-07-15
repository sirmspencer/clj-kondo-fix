(ns clj-kondo-fix.impl.fixes.redundant-let-binding
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]))

(defn fix-redundant-let-binding-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))
              line     (nth current-lines line-idx)]
          (if (or (< col-idx 0) (>= col-idx (count line)))
            [current-lines nil]
            (let [line-len (count line)
                  ;; The finding col points to the binding name.
                  ;; Find the binding value: scan forward from finding col to
                  ;; find the space after the name, then find the value end.
                  name-start col-idx
                  ;; Find the space after the binding name
                  space-pos (loop [i col-idx]
                              (if (>= i line-len)
                                nil
                                (if (Character/isWhitespace (nth line i))
                                  i
                                  (recur (inc i)))))
                  val-end (if space-pos
                            (loop [i (inc space-pos)]
                              (if (>= i line-len)
                                i
                                (let [c (nth line i)]
                                  (if (or (Character/isWhitespace c)
                                          (= \] c))
                                    i
                                    (recur (inc i))))))
                            nil)]
              (if (nil? val-end)
                [current-lines nil]
                (let [new-line (str (subs line 0 name-start)
                                    (subs line val-end))
                      new-line (-> new-line
                                   (str/replace #"\[\s+" "[")
                                   (str/replace #"\s+\]" "]"))]
                  (swap! log conj (str "  " fu ":" (:line f)
                                       "  removed redundant let binding"))
                  [(assoc current-lines line-idx new-line) true])))))))))
