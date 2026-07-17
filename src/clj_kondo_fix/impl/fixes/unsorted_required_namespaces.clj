(ns clj-kondo-fix.impl.fixes.unsorted-required-namespaces
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(defn- top-level-vectors [s]
  (loop [idx 0
         depth 0
         current (StringBuilder.)
         result []]
    (if (>= idx (count s))
      result
      (let [ch (nth s idx)]
        (cond
          (= \[ ch)
          (if (zero? depth)
            (recur (inc idx) 1 (StringBuilder. "[") result)
            (do (.append current ch)
                (recur (inc idx) (inc depth) current result)))
          (= \] ch)
          (let [new-depth (dec depth)]
            (if (zero? new-depth)
              (recur (inc idx) 0 (StringBuilder.) (conj result (str current "]")))
              (do (.append current ch)
                  (recur (inc idx) new-depth current result))))
          (pos? depth)
          (do (.append current ch)
              (recur (inc idx) depth current result))
          :else
          (recur (inc idx) depth current result))))))

(defn- ns-name-from [libspec]
  (let [m (re-find #"^\[([^\s\]]+)" libspec)]
    (if m (second m) "")))

(defn- sort-libspecs [libspecs]
  (sort-by ns-name-from libspecs))

(defn- find-open-paren [lines line-idx col-idx]
  (loop [l line-idx j col-idx]
    (if (< l 0)
      nil
      (if (< j 0)
        (recur (dec l) (dec (count (nth lines (dec l)))))
        (let [line (nth lines l)]
          (if (= \( (nth line j))
            [l j]
            (recur l (dec j))))))))

(defn- content-lines-between [lines open-pos close-pos]
  (let [[open-line open-col] open-pos
        [close-line close-col] close-pos]
    (if (= open-line close-line)
      [(subs (nth lines open-line) (inc open-col) close-col)]
      (let [first-part (subs (nth lines open-line) (inc open-col))
            last-part (subs (nth lines close-line) 0 close-col)
            middle (mapv #(nth lines %) (range (inc open-line) close-line))]
        (vec (cons first-part (concat middle [last-part])))))))

(defn- libspec-entries [content-lines]
  (loop [i 0
         entries []]
    (if (>= i (count content-lines))
      entries
      (let [line (nth content-lines i)
            bracket-idx (str/index-of line "[")]
        (if (nil? bracket-idx)
          (recur (inc i) entries)
          (let [prefix (subs line 0 bracket-idx)
                vectors (top-level-vectors (subs line bracket-idx))
                last-vector-end (inc (str/last-index-of line "]"))
                suffix (subs line last-vector-end)]
            (if (= 1 (count vectors))
              (recur (inc i) (conj entries {:idx i :prefix prefix :libspec (first vectors) :suffix suffix}))
              (recur (inc i) (conj entries {:idx i :prefix prefix :libspecs vectors :suffix suffix})))))))))

(defn- rebuild-content [content-lines entries sorted-libspecs]
  (let [single-line? (= 1 (count content-lines))
        all-sorted (vec sorted-libspecs)]
    (if single-line?
      (let [first-entry (first entries)]
        (if first-entry
          [(str (:prefix first-entry) (str/join " " all-sorted) (:suffix first-entry))]
          content-lines))
      (let [;; Build a map: content-line-idx -> entry
            entry-map (reduce (fn [m e] (assoc m (:idx e) e)) {} entries)
            ;; Assign sorted libspecs to entry slots in order
            entry-idxs (vec (sort (keys entry-map)))
            _ (assert (= (count entry-idxs) (count all-sorted))
                      (str "Mismatch: " (count entry-idxs) " entries vs " (count all-sorted) " sorted"))
            assigned (zipmap entry-idxs all-sorted)]
        (mapv (fn [i]
                (if-let [entry (get entry-map i)]
                  (str (:prefix entry) (get assigned i) (:suffix entry))
                  (nth content-lines i)))
              (range (count content-lines)))))))

(defn fix-unsorted-required-namespaces-in-file
  [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        (let [line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (< line-idx 0)
                  (>= line-idx (count current-lines))
                  (< col-idx 0))
            [current-lines nil]
            (let [open-pos (find-open-paren current-lines line-idx col-idx)
                  close-pos (when open-pos
                              (find-matching-bracket-across-lines
                                current-lines
                                (first open-pos)
                                (second open-pos)))]
              (if (nil? close-pos)
                [current-lines nil]
                (let [[open-line open-col] open-pos
                      [close-line close-col] close-pos
                      c-lines (content-lines-between current-lines [open-line open-col] [close-line close-col])
                      entries (libspec-entries c-lines)
                      all-libspecs (if (and (= 1 (count entries)) (:libspecs (first entries)))
                                     (:libspecs (first entries))
                                     (mapv :libspec entries))
                      sorted (sort-libspecs all-libspecs)]
                  (if (= all-libspecs sorted)
                    [current-lines nil]
                    (let [rebuilt (rebuild-content c-lines entries sorted)
                      open-line-str (nth current-lines open-line)
                      prefix (subs open-line-str 0 open-col)
                      new-open-str (str prefix "(" (first rebuilt))
                                cleared-lines (if (= open-line close-line)
                                                (assoc current-lines open-line (str new-open-str (subs open-line-str close-col)))
                                                (let [close-line-str (nth current-lines close-line)
                                                      rest-lines (subvec rebuilt 1)
                                                      last-idx (dec (count rest-lines))
                                                      rest-with-close (mapv (fn [i s]
                                                                              (if (= i last-idx)
                                                                                (str s (subs close-line-str close-col))
                                                                                s))
                                                                            (range)
                                                                            rest-lines)
                                                 lines-with-first (assoc current-lines open-line new-open-str)
                                                 cleared (reduce (fn [ls [l s]]
                                                                   (assoc ls l s))
                                                                 lines-with-first
                                                                 (map vector
                                                                      (range (inc open-line) (inc close-line))
                                                                      rest-with-close))]
                                             cleared))]
                      (swap! log conj (str "  " fu ":" (:line f)
                                           "  sorted required namespaces"))
                        [cleared-lines true])))))))))))

