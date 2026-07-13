(ns clj-kondo-fix.impl.fixes.single-key-in
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- find-enclosing-paren
  "Find the ( that encloses the given position on the same line.
   Returns column index or nil."
  [line col-idx]
  (loop [j (dec col-idx) depth 0]
    (if (< j 0)
      nil
      (let [ch (nth line j)]
        (cond
          (#{\) \] \}} ch) (recur (dec j) (inc depth))
          (#{\( \[ \{} ch) (if (zero? depth) j (recur (dec j) (dec depth)))
          :else            (recur (dec j) depth))))))

(defn fix-single-key-in-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))]
          (if (or (< line-idx 0)
                  (>= line-idx (count current-lines))
                  (< col-idx 0)
                  (not= \[ (get (nth current-lines line-idx) col-idx)))
            [current-lines nil]
            (let [key-close-pos (find-matching-bracket-across-lines
                                  current-lines line-idx col-idx)]
              (if (nil? key-close-pos)
                [current-lines nil]
                (let [[key-close-line key-close-col] key-close-pos]
                  (if (not= key-close-line line-idx)
                    [current-lines nil]
                    (let [line     (nth current-lines line-idx)
                          key      (subs line (inc col-idx) key-close-col)
                          open-col (find-enclosing-paren line col-idx)]
                      (if (nil? open-col)
                        [current-lines nil]
                        (let [get-in-close-pos (find-matching-bracket-across-lines
                                                 current-lines line-idx open-col)]
                          (if (nil? get-in-close-pos)
                            [current-lines nil]
                            (let [[gi-close-line gi-close-col] get-in-close-pos]
                              (if (not= gi-close-line line-idx)
                                [current-lines nil]
                                (let [map-expr (str/trim (subs line (+ open-col 8) col-idx))
                                      new-form (str "(get " map-expr " " key ")")
                                      new-line (str (subs line 0 open-col)
                                                    new-form
                                                    (subs line (inc gi-close-col)))]
                                  (swap! log conj (str "  " fu ":" (:line f)
                                                       "  (get-in ... [key]) \u2192 (get ... key)"))
                                  [(assoc current-lines line-idx new-line) true])))))))))))))))))
