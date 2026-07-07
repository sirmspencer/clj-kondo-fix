(ns clj-kondo-fix.impl.utils
  (:require [clojure.string :as str]))

(defn read-lines [f]
  (str/split-lines (slurp f)))

(defn write-lines! [f lines]
  (spit f (str (str/join \newline lines) \newline)))

(defn find-matching-bracket [s ^long start-idx]
  (loop [i (inc start-idx) depth 1]
    (cond (>= i (count s)) nil
          (= \[ (nth s i)) (recur (inc i) (inc depth))
          (= \] (nth s i)) (if (= depth 1) i (recur (inc i) (dec depth)))
          :else (recur (inc i) depth))))

(defn word-end-pos [line col-idx]
  ;; Clojure identifiers may contain letters, digits, and: - _ ? ! * + < > = ' .
  (loop [i col-idx]
    (if (or (>= i (count line))
            (not (re-find #"[a-zA-Z0-9_\-?!*+<>='.']" (str (nth line i)))))
      i
      (recur (inc i)))))

(defn find-binding-on-line [line binding-name approx-col]
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

(defn in-keys-destructuring? [line col-idx]
  (let [before (subs line 0 col-idx)]
    (re-find #":(?:keys|strs|syms)\s*\[[^\]]*$" before)))

(defn find-docstring-end [lines start-line-idx]
  (let [docstring-line (nth lines start-line-idx)
        quote-col (.indexOf docstring-line "\"")]
    (if (neg? quote-col)
      start-line-idx
      (loop [line-idx start-line-idx
             col-idx (inc quote-col)
             escaped false]
        (if (>= line-idx (count lines))
          start-line-idx
          (let [line (nth lines line-idx)
                ch (when (< col-idx (count line)) (nth line col-idx))]
            (cond
              (nil? ch) (recur (inc line-idx) 0 false)
              escaped (recur line-idx (inc col-idx) false)
              (= \\ ch) (recur line-idx (inc col-idx) true)
              (= \" ch) line-idx
              :else (recur line-idx (inc col-idx) false))))))))

(defn find-matching-bracket-across-lines [lines start-line-idx start-col-idx]
  (let [start-ch (get-in lines [start-line-idx start-col-idx])
        open-set #{\[ \( \{}
        close-map {\[ \] \( \) \{ \}}]
    (if-not (open-set start-ch)
      nil
      (let [close-ch (get close-map start-ch)]
        (loop [i start-line-idx
               j (inc start-col-idx)
               depth 1
               in-str? false
               escaped? false]
          (if (>= i (count lines))
            nil
            (let [line (nth lines i)
                  ch (when (< j (count line)) (nth line j))]
              (cond
                (nil? ch) (recur (inc i) 0 depth in-str? false)
                escaped? (recur i (inc j) depth in-str? false)
                (= \\ ch) (recur i (inc j) depth in-str? true)
                (= \" ch) (recur i (inc j) depth (not in-str?) false)
                in-str? (recur i (inc j) depth in-str? false)
                (= start-ch ch) (recur i (inc j) (inc depth) false false)
                (= close-ch ch) (if (= depth 1) [i j] (recur i (inc j) (dec depth) false false))
                :else (recur i (inc j) depth false false)))))))))
