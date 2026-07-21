(ns clj-kondo-fix.impl.fixes.def-fn
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- def-open-col
  "Scan backward from just before the fn opening paren to find the def
  form's opening paren. Returns column or nil."
  [line fn-col-idx]
  (loop [i (dec fn-col-idx)]
    (when (>= i 0)
      (if (and (= \( (get line i))
               (.startsWith (subs line i) "(def"))
        i
        (recur (dec i))))))

(defn- name-range
  "Return [start end) of the def name in line, given def-open and fn-open."
  [line def-open fn-col-idx]
  (let [after-def (+ def-open 5)
        s (loop [i after-def]
            (if (or (>= i fn-col-idx) (not (Character/isWhitespace (get line i))))
              i
              (recur (inc i))))
        e (loop [i (dec fn-col-idx)]
            (if (or (<= i s) (not (Character/isWhitespace (get line i))))
              (inc i)
              (recur (dec i))))]
    (when (< s e)
      [s e])))

(defn- fn-content-start
  "Return column after 'fn ' (skipping whitespace) inside the fn form.
  fn-open-col is the column of '(' opening the fn form."
  [line fn-open-col]
  (let [after-fn (+ fn-open-col 3)]
    (loop [i (min after-fn (dec (count line)))]
      (if (or (>= i (count line)) (not (Character/isWhitespace (get line i))))
        i
        (recur (inc i))))))

(defn fix-def-fn-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             fn-open  (dec (:col f))
                             line     (nth current-lines line-idx)]
                         (when (nil? line)
                           [current-lines nil])
                         (let [def-open (def-open-col line fn-open)]
                           (if (nil? def-open)
                             [current-lines nil]
                             (let [fn-close (find-matching-bracket-across-lines
                                             current-lines line-idx fn-open)]
                               (if (nil? fn-close)
                                 [current-lines nil]
                                 (let [[fn-close-line fn-close-col] fn-close]
                                   (if (not= fn-close-line line-idx)
                                     [current-lines nil]
                                     (let [[name-s name-e] (name-range line def-open fn-open)]
                                       (if (nil? name-s)
                                         [current-lines nil]
                                         (let [name         (subs line name-s name-e)
                                               fcs          (fn-content-start line fn-open)
                                               inner        (subs line fcs fn-close-col)
                                               def-close    (inc fn-close-col)
                                               pre          (subs line 0 def-open)
                                               post         (subs line (inc def-close))
                                               insert       (str "defn " name " " inner)
                                               new-line     (str pre "(" insert ")" post)]
                                           (swap! log conj (str "  " fu ":" (:line f)
                                                                "  def+fn → defn"))
                                           [(assoc current-lines line-idx new-line) true]))))))))))))))
