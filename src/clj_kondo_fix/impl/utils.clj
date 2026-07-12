(ns clj-kondo-fix.impl.utils
  (:require [clojure.string :as str]))

;; ------------------------------------------------------------
;; I/O
;; ------------------------------------------------------------

(defn read-lines [f]
  (str/split-lines (slurp f)))

(defn write-lines! [f lines]
  (spit f (str (str/join \newline lines) \newline)))

;; ------------------------------------------------------------
;; Bracket navigation
;; ------------------------------------------------------------

(defn find-matching-bracket
  "Find the matching ] for the [ at start-idx on a single line.
   Returns the index of ] or nil."
  [s ^long start-idx]
  (loop [i (inc start-idx) depth 1]
    (cond (>= i (count s)) nil
          (= \[ (nth s i)) (recur (inc i) (inc depth))
          (= \] (nth s i)) (if (= depth 1) i (recur (inc i) (dec depth)))
          :else (recur (inc i) depth))))

(defn find-matching-bracket-across-lines
  "Find the matching close bracket for the open bracket at (start-line-idx,
   start-col-idx). Handles [, (, and {. String-aware (skips brackets in
   string literals). Returns [line col] or nil."
  [lines start-line-idx start-col-idx]
  (let [start-ch  (get-in lines [start-line-idx start-col-idx])
        open-set  #{\[ \( \{}
        close-map {\[ \] \( \) \{ \}}]
    (if-not (open-set start-ch)
      nil
      (let [close-ch (get close-map start-ch)]
        (loop [i        start-line-idx
               j        (inc start-col-idx)
               depth    1
               in-str?  false
               escaped? false]
          (if (>= i (count lines))
            nil
            (let [line (nth lines i)
                  ch   (when (< j (count line)) (nth line j))]
              (cond
                (nil? ch)   (recur (inc i) 0 depth in-str? false)
                escaped?    (recur i (inc j) depth in-str? false)
                (= \\ ch)   (recur i (inc j) depth in-str? true)
                (= \" ch)   (recur i (inc j) depth (not in-str?) false)
                in-str?     (recur i (inc j) depth in-str? false)
                (= start-ch ch) (recur i (inc j) (inc depth) false false)
                (= close-ch ch) (if (= depth 1)
                                  [i j]
                                  (recur i (inc j) (dec depth) false false))
                :else       (recur i (inc j) depth false false)))))))))

(defn find-opening-bracket
  "Scan left from (line-idx, col-idx-1) to find the [ that directly contains
   the position. Tracks [] nesting only. Returns {:line l :col c} or nil."
  [lines line-idx col-idx]
  (loop [i line-idx, j (dec col-idx), depth 0]
    (when (>= i 0)
      (if (< j 0)
        (when (> i 0)
          (recur (dec i) (dec (count (nth lines (dec i)))) depth))
        (let [ch (nth (nth lines i) j)]
          (case ch
            \] (recur i (dec j) (inc depth))
            \[ (if (zero? depth)
                 {:line i :col j}
                 (recur i (dec j) (dec depth)))
            (recur i (dec j) depth)))))))

(defn enclosing-bracket-type
  "Returns the character ([, (, or {) of the innermost bracket that directly
   contains (line-idx, col-idx-1), tracking all three bracket pairs so
   function-call parens act as barriers. Returns nil at top level."
  [lines line-idx col-idx]
  (loop [i line-idx, j (dec col-idx), depth 0]
    (when (>= i 0)
      (if (< j 0)
        (when (> i 0)
          (recur (dec i) (dec (count (nth lines (dec i)))) depth))
        (let [ch (nth (nth lines i) j)]
          (case ch
            (\] \) \}) (recur i (dec j) (inc depth))
            (\[ \( \{) (if (zero? depth)
                         ch
                         (recur i (dec j) (dec depth)))
            (recur i (dec j) depth)))))))

;; ------------------------------------------------------------
;; Token / identifier helpers
;; ------------------------------------------------------------

(defn word-end-pos
  "Return the end position (exclusive) of the identifier starting at col-idx.
   Clojure identifiers may contain letters, digits, and: - _ ? ! * + < > = ' ."
  [line col-idx]
  (loop [i col-idx]
    (if (or (>= i (count line))
            (not (re-find #"[a-zA-Z0-9_\-?!*+<>='.']" (str (nth line i)))))
      i
      (recur (inc i)))))

(defn find-binding-on-line
  "Find binding-name as a whole word starting at or near approx-col.
   Returns the start index or nil."
  [line binding-name approx-col]
  (let [search-from (max 0 (dec approx-col))]
    (loop [search-idx search-from]
      (let [idx (.indexOf line binding-name search-idx)]
        (cond
          (neg? idx) nil
          (and (or (zero? idx)
                   (not (re-find #"[a-zA-Z0-9_-]" (str (nth line (dec idx))))))
               (or (>= (+ idx (count binding-name)) (count line))
                   (not (re-find #"[a-zA-Z0-9_-]" (str (nth line (+ idx (count binding-name))))))))
          idx
          :else (recur (inc idx)))))))

(defn find-docstring-end
  "Return the line index of the closing \" of the docstring that begins on
   start-line-idx. Returns start-line-idx if no opening quote is found."
  [lines start-line-idx]
  (let [docstring-line (nth lines start-line-idx)
        quote-col      (.indexOf docstring-line "\"")]
    (if (neg? quote-col)
      start-line-idx
      (loop [line-idx start-line-idx
             col-idx  (inc quote-col)
             escaped  false]
        (if (>= line-idx (count lines))
          start-line-idx
          (let [line (nth lines line-idx)
                ch   (when (< col-idx (count line)) (nth line col-idx))]
            (cond
              (nil? ch) (recur (inc line-idx) 0 false)
              escaped   (recur line-idx (inc col-idx) false)
              (= \\ ch) (recur line-idx (inc col-idx) true)
              (= \" ch) line-idx
              :else     (recur line-idx (inc col-idx) false))))))))

(defn remove-token-span
  "Remove the token at [start, end) from line, adjusting surrounding
   whitespace so no double-space or leading/trailing comma remains."
  [line start end]
  (let [before (subs line 0 start)
        after  (subs line end)]
    (if (re-find #"[\s,]$" before)
      (str (subs before 0 (dec (count before))) after)
      (str before (if (re-find #"^[\s,]" after)
                    (subs after 1)
                    after)))))

(defn remove-referred-var-from-line
  "Remove var-name at col-idx from line, adjusting surrounding whitespace.
   Handles both fully-qualified tokens (clojure.string/join) and simple
   names, delegating span removal to remove-token-span."
  [line var-name col-idx]
  (if (>= col-idx (count line))
    line
    (let [simple-name (or (second (re-find #"([^/]+)$" var-name)) var-name)
          end         (word-end-pos line col-idx)
          actual      (subs line col-idx end)
          match-name  (if (= actual var-name) var-name simple-name)]
      (if (.startsWith actual match-name)
        (remove-token-span line col-idx (+ col-idx (count match-name)))
        line))))
