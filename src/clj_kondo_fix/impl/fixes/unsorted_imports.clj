(ns clj-kondo-fix.impl.fixes.unsorted-imports
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- extract-import-forms [lines open-pos close-pos]
  (let [[open-line open-col] open-pos
        [close-line close-col] close-pos]
    (if (= open-line close-line)
      (let [line (nth lines open-line)
            content (subs line (inc open-col) close-col)]
        (vec (re-seq #"\[[^\]]+\]" content)))
      (let [first-line (nth lines open-line)
            last-line (nth lines close-line)
            first-part (subs first-line (inc open-col))
            last-part (subs last-line 0 close-col)
            middle-lines (map #(nth lines %) (range (inc open-line) close-line))
            full-text (str/join "\n" (cons first-part (concat middle-lines [last-part])))]
        (vec (re-seq #"\[[^\]]+\]" full-text))))))

(defn- sort-import-forms [import-forms]
  (vec (sort (fn [a b]
               (let [a-pkg (second (re-find #"\[([^\s\]]+)" a))
                     b-pkg (second (re-find #"\[([^\s\]]+)" b))]
                 (compare (or a-pkg "") (or b-pkg ""))))
             import-forms)))

(defn- find-open-paren [lines line-idx col-idx]
  (loop [l line-idx]
    (if (< l 0)
      nil
      (let [line (nth lines l)
            start-col (if (= l line-idx) col-idx (dec (count line)))]
        (loop [j start-col]
          (if (< j 0)
            (recur (dec l))
            (if (= \( (nth line j))
              [l j]
              (recur (dec j)))))))))

(defn fix-unsorted-imports-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx  (dec (:col f))]
          (if (or (< line-idx 0) (>= line-idx (count current-lines))
                  (< col-idx 0))
            [current-lines nil]
            (let [open-pos (find-open-paren current-lines line-idx col-idx)
                  close-pos (when open-pos
                              (find-matching-bracket-across-lines
                                current-lines (first open-pos) (second open-pos)))]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[open-line open-col] open-pos
                      [close-line close-col] close-pos
                      import-forms (extract-import-forms current-lines [open-line open-col] [close-line close-col])
                      sorted-forms (sort-import-forms import-forms)]
                  (if (= import-forms sorted-forms)
                    [current-lines nil]
                    (let [full-form (str "(" (str/join " " (cons ":import" sorted-forms)) ")")]
                      (swap! log conj (str "  " fu ":" (:line f)
                                           "  sorted imports"))
                      (if (= open-line close-line)
                        (let [line (nth current-lines open-line)
                              new-line (str (subs line 0 open-col)
                                            full-form
                                            (subs line (inc close-col)))]
                          [(assoc current-lines open-line new-line) true])
                        (let [first-line (nth current-lines open-line)
                              prefix (subs first-line 0 open-col)
                              lines-with-first (assoc current-lines open-line (str prefix full-form))
                              cleared-lines (reduce (fn [lines l]
                                                      (assoc lines l ""))
                                                    lines-with-first
                                                    (range (inc open-line) (inc close-line)))]
                          [cleared-lines true])))))))))))))
