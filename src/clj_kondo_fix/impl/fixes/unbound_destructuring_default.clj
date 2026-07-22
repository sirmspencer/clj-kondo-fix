(ns clj-kondo-fix.impl.fixes.unbound-destructuring-default
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [word-end-pos]]))

(defn- form-end
  "Return the exclusive end index of the form starting at start-idx.
   Handles nested brackets and strings; stops at whitespace, comma, or }
   when at depth 0."
  [line start-idx]
  (let [len (count line)]
    (loop [i start-idx depth 0 in-str false esc false]
      (if (>= i len)
        len
        (let [c (get line i)]
          (cond
            esc                                               (recur (inc i) depth in-str false)
            (= c \\)                                         (recur (inc i) depth in-str true)
            (= c \")                                         (recur (inc i) depth (not in-str) false)
            (and (not in-str) (#{\( \[ \{} c))               (recur (inc i) (inc depth) false false)
            (and (not in-str) (zero? depth) (#{\) \] \}} c)) i
            (and (not in-str) (#{\) \] \}} c))               (recur (inc i) (dec depth) false false)
            (and (not in-str) (zero? depth) (#{\space \,} c)) i
            :else                                            (recur (inc i) depth in-str false)))))))

(defn- remove-or-pair
  "Remove the key-value pair whose key starts at col-idx from line.
   Eats a preceding separator if one exists, otherwise eats a trailing one."
  [line col-idx]
  (let [key-end   (word-end-pos line col-idx)
        val-start (loop [i key-end]
                    (if (or (>= i (count line))
                            (not (#{\space \tab \,} (get line i))))
                      i
                      (recur (inc i))))
        val-end   (form-end line val-start)
        ;; try eating backward: whitespace/comma before the key
        rm-start  (loop [i (dec col-idx)]
                    (cond
                      (neg? i)                          0
                      (= \{ (get line i))              (inc i)   ; stop at {
                      (#{\space \, \tab} (get line i)) (recur (dec i))
                      :else                            (inc i)))
        ;; if we ate nothing backward, eat a trailing separator after val-end
        [rs re]   (if (< rm-start col-idx)
                    [rm-start val-end]
                    (let [re' (loop [i val-end]
                                (if (or (>= i (count line))
                                        (not (#{\space \, \tab} (get line i))))
                                  i
                                  (recur (inc i))))]
                      [col-idx re']))]
    (str (subs line 0 rs) (subs line re))))

(defn- cleanup-empty-or
  "Remove :or {} occurrences (including preceding whitespace) from all lines."
  [lines]
  (mapv #(str/replace % #"\s*:or\s*\{\}" "") lines))

(defn fix-unbound-destructuring-default-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [current-lines f]
                       (let [line-idx (dec (:line f))
                             col-idx  (dec (:col f))]
                         (if (or (< line-idx 0) (>= line-idx (count current-lines))
                                 (< col-idx 0))
                           [current-lines nil]
                           (let [line     (nth current-lines line-idx)
                                 new-line (remove-or-pair line col-idx)]
                             (if (= new-line line)
                               [current-lines nil]
                               (do (swap! log conj (str "  " fu ":" (:line f)
                                                        "  remove unbound :or default"))
                                   [(assoc current-lines line-idx new-line) true]))))))
                     cleanup-empty-or)))
