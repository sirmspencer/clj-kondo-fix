(ns clj-kondo-fix.impl.fixes.shadowed-var
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-opening-bracket
                                              find-matching-bracket-across-lines]]))

(defn- extract-binding-name
  "Extract the simple binding name from a 'Shadowed var: ns/name' message."
  [message]
  (when-let [[_ full] (re-find #"^Shadowed var: (.+?)\.?$" message)]
    (last (str/split full #"/"))))

(defn- find-enclosing-open-paren
  "Scan backward from (bracket-line, bracket-col - 1) to find the ( that
   directly encloses the binding vector. Returns [line col] or nil."
  [lines bracket-line bracket-col]
  (loop [i bracket-line
         j (dec bracket-col)
         depth 0]
    (when (>= i 0)
      (if (< j 0)
        (when (pos? i)
          (recur (dec i) (dec (count (nth lines (dec i)))) depth))
        (let [ch (get (nth lines i) j)]
          (case ch
            (\] \) \}) (recur i (dec j) (inc depth))
            (\[ \{)    (recur i (dec j) (inc depth))
            \(         (if (zero? depth)
                         [i j]
                         (recur i (dec j) (dec depth)))
            (recur i (dec j) depth)))))))

(defn- replace-symbol-in-span
  "Replace old-name with new-name as a whole word within
   lines[open-line..close-line], bounded by [open-col..close-col].
   Returns updated lines vector."
  [lines old-name new-name open-line open-col close-line close-col]
  (let [pat (re-pattern (str "(?<![a-zA-Z0-9_\\-?!*+<>='./])"
                             (java.util.regex.Pattern/quote old-name)
                             "(?![a-zA-Z0-9_\\-?!*+<>='./])"))]
    (reduce
     (fn [ls line-idx]
       (let [line      (nth ls line-idx)
             start-col (if (= line-idx open-line) open-col 0)
             end-col   (if (= line-idx close-line) (inc close-col) (count line))
             prefix    (subs line 0 start-col)
             segment   (subs line start-col end-col)
             suffix    (subs line end-col)
             new-seg   (str/replace segment pat new-name)]
         (assoc ls line-idx (str prefix new-seg suffix))))
     lines
     (range open-line (inc close-line)))))

(defn fix-shadowed-var-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [binding-name (extract-binding-name (:message f))]
                         (if (nil? binding-name)
                           [current-lines nil]
                           (let [line-idx (dec (:line f))
                                 col-idx  (dec (:col f))
                                 bracket  (find-opening-bracket current-lines line-idx (inc col-idx))]
                             (if (nil? bracket)
                               [current-lines nil]
                               (let [b-line (:line bracket)
                                     b-col  (:col bracket)
                                     paren  (find-enclosing-open-paren current-lines b-line b-col)]
                                 (if (nil? paren)
                                   [current-lines nil]
                                   (let [[p-line p-col] paren
                                         [c-line c-col] (or (find-matching-bracket-across-lines current-lines p-line p-col)
                                                            [nil nil])]
                                     (if (nil? c-line)
                                       [current-lines nil]
                                       (let [new-name  (str "LOCAL-" binding-name)
                                             new-lines (replace-symbol-in-span current-lines
                                                                               binding-name new-name
                                                                               p-line p-col
                                                                               c-line c-col)]
                                         (if (= new-lines current-lines)
                                           [current-lines nil]
                                           (do (swap! log conj (str "  " fu ":" (:line f)
                                                                    "  " binding-name " \u2192 " new-name))
                                               [new-lines true])))))))))))))))
