(ns clj-kondo-fix.impl.fixes
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.utils :refer [read-lines write-lines!
                                              find-matching-bracket
                                              find-matching-bracket-across-lines
                                              word-end-pos
                                              find-binding-on-line
                                              find-docstring-end]]))

;; ------------------------------------------------------------
;; Message parsing
;; ------------------------------------------------------------

(def unused-ns-re #"^namespace (.+) is required but never used$")
(def duplicate-require-re #"^duplicate require of (.+)$")
(def unused-binding-re #"^unused binding (.+)$")

(defn extract-ns-name [msg]
  (or (some-> (re-find unused-ns-re msg) second)
      (some-> (re-find duplicate-require-re msg) second)))

(defn extract-binding-name [msg]
  (some-> (re-find unused-binding-re msg) second))

;; ------------------------------------------------------------
;; Helpers for require-entry removal
;; ------------------------------------------------------------

(defn ns-name-matches-at [line ^long idx ns-name]
  (let [pattern (re-pattern (str (java.util.regex.Pattern/quote ns-name) "(?:\\s|\\]|$)"))]
    (re-find pattern (subs line idx))))

;; start-from: 0-indexed minimum position to begin searching (used to skip the
;; first occurrence when the finding's :col points to a later duplicate).
(defn find-ns-on-line
  ([line ns-name] (find-ns-on-line line ns-name 0))
  ([line ns-name start-from]
   (let [ns-str (str "[" ns-name)]
     (loop [start (max 0 start-from)]
       (let [idx (.indexOf line ns-str start)]
         (if (neg? idx)
           nil
           (if (ns-name-matches-at line (inc idx) ns-name)
             idx
             (recur (inc idx)))))))))

(defn remove-entry-from-line [line ns-name]
  (let [idx (find-ns-on-line line ns-name)]
    (if (nil? idx)
      line
      (let [end-idx (find-matching-bracket line idx)]
        (if (nil? end-idx)
          line
          (str (subs line 0 idx) (subs line (inc end-idx))))))))

(defn entry-is-only-on-line? [line ns-name]
  (let [idx (find-ns-on-line line ns-name)]
    (if (nil? idx)
      false
      (let [end-idx (find-matching-bracket line idx)]
        (if (nil? end-idx)
          false
          (let [before (subs line 0 idx)
                after (subs line (inc end-idx))]
            (and (re-find #"^\s*$" before)
                 (re-find #"^\s*\)?\s*,?\s*$" after))))))))

(defn entry-has-closing-paren? [line ns-name]
  (let [idx (find-ns-on-line line ns-name)]
    (if (nil? idx)
      false
      (let [end-idx (find-matching-bracket line idx)]
        (if (nil? end-idx)
          false
          (let [after (subs line (inc end-idx))]
            (re-find #"^\s*\)" after)))))))

(defn entry-has-opening-require? [line ns-name]
  (let [idx (find-ns-on-line line ns-name)]
    (if (nil? idx)
      false
      (let [before (subs line 0 idx)]
        (re-find #"\(\s*:require" before)))))

;; ------------------------------------------------------------
;; Fix: unused-ns / duplicate-require
;; ------------------------------------------------------------

(defn remove-require-finding [lines finding file-url log]
  (let [line-idx  (dec (:line finding))
        ;; :col is 1-indexed and points to the first char of the ns name.
        ;; The [ preceding it is at col-2 (0-indexed), so start the bracket
        ;; search from there.  This ensures that for duplicate-require we skip
        ;; the first (non-duplicate) occurrence and land on the right entry.
        col-start (max 0 (- (:col finding) 2))
        ns-name   (extract-ns-name (:message finding))]
    (if (or (nil? ns-name) (< line-idx 0) (>= line-idx (count lines)))
      [lines nil]
      (let [line (nth lines line-idx)
            idx (find-ns-on-line line ns-name col-start)]
        (if (nil? idx)
          (do (swap! log conj (str "  " file-url ":" (:line finding) "  skip: can't find [" ns-name))
              [lines nil])
          (let [end-idx (find-matching-bracket line idx)]
            (if (nil? end-idx)
              (do (swap! log conj (str "  " file-url ":" (:line finding) "  skip: unmatched bracket for " ns-name))
                  [lines nil])
              (let [before (subs line 0 idx)
                    after (subs line (inc end-idx))]
                (cond
                  (and (re-find #"^\s*$" before)
                       (re-find #"^\s*,?\s*$" after))
                  (do (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name))
                      [(vec (concat (take line-idx lines)
                                    (drop (inc line-idx) lines)))
                       true])

                  (re-find #"^\s*\)" after)
                  (let [prev-idx (dec line-idx)]
                    (if (and (>= prev-idx 0)
                             (re-find #"^\s*$" before)
                             (re-find #"^\s*\[" (nth lines prev-idx)))
                      (let [prev-line (nth lines prev-idx)]
                        (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                        [(-> (vec (concat (take line-idx lines)
                                          (drop (inc line-idx) lines)))
                             (assoc prev-idx (str prev-line after)))
                         true])
                      (let [new-line (str before after)]
                        (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                        [(assoc lines line-idx new-line) true])))

                  :else
                  (let [new-line (str before after)]
                    (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name))
                    (if (re-find #"^\s*\(\s*:\w+\s*$" new-line)
                      (let [next-idx (inc line-idx)]
                        (if (and (< next-idx (count lines))
                                 (re-find #"^\s*\[" (nth lines next-idx)))
                          (let [indent (re-find #"^\s*" new-line)
                                entry (str/trim (nth lines next-idx))]
                            [(-> (vec (concat (take next-idx lines)
                                              (drop (inc next-idx) lines)))
                                 (assoc line-idx (str indent "(:require " entry)))
                             true])
                          [(assoc lines line-idx new-line) true]))
                      [(assoc lines line-idx new-line) true])))))))))))

(defn cleanup-empty-clauses [lines]
  (loop [i 0, lines lines]
    (if (>= i (count lines))
      lines
      (let [line (nth lines i)]
        (cond
          (re-find #"^\s*\(\s*:\w+\s*\)\s*$" line)
          (recur i (vec (concat (take i lines) (drop (inc i) lines))))

          (and (re-find #"^\s*\(\s*:\w+\s*$" line)
               (< (inc i) (count lines))
               (re-find #"^\s*\)+" (nth lines (inc i))))
          (let [next-line (nth lines (inc i))
                reduced (str/replace-first next-line #"\)" "")]
            (recur i (vec (concat (take i lines)
                                  [reduced]
                                  (drop (+ i 2) lines)))))

          (re-find #":refer\s*\[\s*\]" line)
          (let [cleaned (str/replace line #"\s*:refer\s*\[\s*\]" "")]
            (recur (inc i) (assoc lines i cleaned)))

          :else
          (recur (inc i) lines))))))

(defn fix-unused-ns-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted
           current-lines lines
           fixed 0]
      (if (nil? f)
        (let [cleaned (cleanup-empty-clauses current-lines)]
          {:fixed fixed :lines cleaned :changed? (or (pos? fixed) (not= cleaned lines))})
        (let [[new-lines changed] (remove-require-finding current-lines f file-url log)]
          (recur more new-lines (if changed (inc fixed) fixed)))))))

;; ------------------------------------------------------------
;; Fix: unused-binding — rename to underscore prefix
;; ------------------------------------------------------------

(defn remove-as-clause-from-line [line binding-name idx word-end]
  (if-let [m (re-find #"[\s,]:as\s+[\w-]+$" (subs line 0 word-end))]
    (let [match-str (if (string? m) m (first m))
          clause-start (- word-end (count match-str))]
      (str (subs line 0 clause-start) (subs line word-end)))
    line))

(defn fix-unused-binding-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted
           current-lines lines
           fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [binding-name (extract-binding-name (:message f))
              line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (nil? binding-name) (< line-idx 0) (>= line-idx (count current-lines)))
            (recur more current-lines fixed)
            (let [line (nth current-lines line-idx)
                  idx (find-binding-on-line line binding-name col-idx)]
              (if (nil? idx)
                (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: can't find binding " binding-name " on line"))
                    (recur more current-lines fixed))
                (let [word-end (word-end-pos line idx)]
                  (if (not= (subs line idx word-end) binding-name)
                    (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: binding " binding-name " not found at column " (:col f)))
                        (recur more current-lines fixed))
                    ;; skip namespaced keys e.g. {:keys [ns/name]} — the :col
                    ;; lands on "name" but inserting _ there produces ns/_name
                    (if (and (pos? idx) (= \/ (nth line (dec idx))))
                      (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: binding " binding-name " is part of a namespaced key"))
                          (recur more current-lines fixed))
                    (let [text-before (subs line 0 idx)]
                      (if (re-find #":as\s+$" text-before)
                        (let [new-line (remove-as-clause-from-line line binding-name idx word-end)]
                          (swap! log conj (str "  " file-url ":" (:line f) "  remove unused :as binding: " binding-name))
                          (recur more
                                 (assoc current-lines line-idx new-line)
                                 (inc fixed)))
                        (let [new-line (str (subs line 0 idx) "_" (subs line idx))]
                          (swap! log conj (str "  " file-url ":" (:line f) "  rename unused binding: " binding-name " -> _" binding-name))
                           (recur more
                                  (assoc current-lines line-idx new-line)
                                  (inc fixed))))))))))))))))


;; ------------------------------------------------------------
;; Fix: referred var, refer-all, import
;; ------------------------------------------------------------

(defn remove-referred-var-from-line [line var-name col-idx]
  (if (>= col-idx (count line))
    line
    (let [simple-name (or (second (re-find #"([^/]+)$" var-name)) var-name)
          end (word-end-pos line col-idx)
          actual (subs line col-idx end)]
      (if (.startsWith actual simple-name)
        (let [match-end (+ col-idx (count simple-name))
              before (subs line 0 col-idx)
              after (subs line match-end)
              cleaned-before (if (re-find #"[\s,]$" before)
                               (subs before 0 (dec (count before)))
                               before)
              cleaned-after (if (re-find #"^[\s,]" after)
                              (subs after 1)
                              after)]
          (str cleaned-before cleaned-after))
        line))))

(defn fix-unused-import-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [msg (:message f)
              var-name (some-> (re-find #"^Unused import (.+)$" msg) second)
              line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (nil? var-name) (< line-idx 0) (>= line-idx (count current-lines)))
            (recur more current-lines fixed)
            (let [line (nth current-lines line-idx)
                  new-line (remove-referred-var-from-line line var-name col-idx)]
              (if (= new-line line)
                (recur more current-lines fixed)
                (do (swap! log conj (str "  " file-url ":" (:line f) "  remove unused import: " var-name))
                    (recur more
                           (assoc current-lines line-idx new-line)
                           (inc fixed)))))))))))

(defn fix-unused-referred-var-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        (let [cleaned (cleanup-empty-clauses current-lines)]
          {:fixed fixed :lines cleaned :changed? (or (pos? fixed) (not= cleaned lines))})
        (let [msg (:message f)
              var-name (some-> (re-find #"^#'(.+) is referred but never used$" msg) second)
              line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (nil? var-name) (< line-idx 0) (>= line-idx (count current-lines)))
            (recur more current-lines fixed)
            (let [line (nth current-lines line-idx)
                  new-line (remove-referred-var-from-line line var-name col-idx)]
              (if (= new-line line)
                (recur more current-lines fixed)
                (do (swap! log conj (str "  " file-url ":" (:line f) "  remove referred var: " var-name))
                    (recur more
                           (assoc current-lines line-idx new-line)
                           (inc fixed)))))))))))

(defn find-require-entry-start [line col-idx]
  (loop [i (dec col-idx) depth 0]
    (if (< i 0)
      nil
      (let [ch (nth line i)]
        (case ch
          \[ (if (zero? depth) i (recur (dec i) (dec depth)))
          \] (recur (dec i) (inc depth))
          (recur (dec i) depth))))))

(defn fix-refer-all-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [line-idx (dec (:line f))
              col-idx (dec (:col f))
              line (nth current-lines line-idx)
              entry-start (find-require-entry-start line col-idx)]
          (if (nil? entry-start)
            (recur more current-lines fixed)
            (let [end-idx (find-matching-bracket line entry-start)]
              (if (nil? end-idx)
                (recur more current-lines fixed)
                (let [entry (subs line entry-start (inc end-idx))
                      cleaned (str/replace entry #"\s*:refer\s+:all" "")]
                  (if (= cleaned entry)
                    (recur more current-lines fixed)
                    (let [new-line (str (subs line 0 entry-start) cleaned (subs line (inc end-idx)))]
                      (swap! log conj (str "  " file-url ":" (:line f) "  remove :refer :all from " entry))
                      (recur more
                             (assoc current-lines line-idx new-line)
                             (inc fixed)))))))))))))

;; ------------------------------------------------------------
;; Fix: missing-else-branch — if/if-not/if-let/if-some → when variant
;; ------------------------------------------------------------

(defn fix-missing-else-branch-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted
           current-lines lines
           fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (< line-idx 0) (>= line-idx (count current-lines))
                  (< col-idx 0) (>= (+ col-idx 3) (count (nth current-lines line-idx))))
            (recur more current-lines fixed)
            (let [line (nth current-lines line-idx)]
              ;; Replace "(if" with "(when" — works for (if, (if-not, (if-let, (if-some
              ;; because we only replace the first 3 chars "(if", leaving "-not"/"-let"/"-some" intact
              (if (= "(if" (subs line col-idx (+ col-idx 3)))
                (let [new-line (str (subs line 0 col-idx) "(when" (subs line (+ col-idx 3)))]
                  (swap! log conj (str "  " file-url ":" (:line f) "  replace (if with (when"))
                  (recur more (assoc current-lines line-idx new-line) (inc fixed)))
                (recur more current-lines fixed)))))))))

;; ------------------------------------------------------------
;; Fix: misplaced-docstring
;; ------------------------------------------------------------

(defn fix-misplaced-docstring-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted
           current-lines lines
           fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [docstring-line-idx (dec (:line f))
              def-line-idx (dec docstring-line-idx)]
          (if (or (< def-line-idx 0) (>= docstring-line-idx (count current-lines)))
            (recur more current-lines fixed)
            (let [def-line (nth current-lines def-line-idx)
                  docstring-line (nth current-lines docstring-line-idx)
                  bracket-idx (.indexOf def-line "[")]
              (if (neg? bracket-idx)
                (recur more current-lines fixed)
                (let [end-idx (find-matching-bracket def-line bracket-idx)]
                  (if (nil? end-idx)
                    (recur more current-lines fixed)
                    (let [prefix (str/trimr (subs def-line 0 bracket-idx))
                          params (subs def-line bracket-idx (inc end-idx))
                          after-params (str/trim (subs def-line (inc end-idx)))
                          docstring-end-line-idx (find-docstring-end current-lines docstring-line-idx)
                          indent (re-find #"^\s*" docstring-line)]
                      (swap! log conj (str "  " file-url ":" (:line f) "  move docstring before params"))
                      (recur more
                             (vec (concat (take def-line-idx current-lines)
                                          [prefix]
                                          (subvec (vec current-lines) docstring-line-idx (inc docstring-end-line-idx))
                                          [(str indent params
                                                (when-not (empty? after-params)
                                                  (str " " after-params)))]
                                          (drop (inc docstring-end-line-idx) current-lines)))
                             (inc fixed)))))))))))))

;; ------------------------------------------------------------
;; Fix: unused-private-var
;; ------------------------------------------------------------

(defn fix-unused-private-var-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [msg      (:message f)
              var-name (some-> (re-find #"^Unused private var .+/(.+)$" msg) second)
              line-idx (dec (:line f))
              ;; :col is 1-indexed and points to the first char of the var name.
              ;; Start .indexOf from there so we skip any earlier occurrences of
              ;; the same substring (e.g. "f" appearing in "foo" before the def).
              col-start (max 0 (- (:col f) 2))]
          (if (nil? var-name)
            (recur more current-lines fixed)
            (let [line (nth current-lines line-idx)
                  idx  (.indexOf line var-name col-start)]
              (if (neg? idx)
                (recur more current-lines fixed)
                (let [new-line (str (subs line 0 idx) "_" (subs line idx))]
                  (swap! log conj (str "  " file-url ":" (:line f) "  prefix unused private var: " var-name))
                  (recur more (assoc current-lines line-idx new-line) (inc fixed)))))))))))

;; ------------------------------------------------------------
;; Fix: redundant-do
;; ------------------------------------------------------------

(defn fix-redundant-do-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (< line-idx 0) (>= line-idx (count current-lines))
                  (< col-idx 0) (>= (+ col-idx 3) (count (nth current-lines line-idx))))
            (recur more current-lines fixed)
            (let [line (nth current-lines line-idx)]
              (if (= "(do" (subs line col-idx (+ col-idx 3)))
                (if-let [[match-line match-col] (find-matching-bracket-across-lines current-lines line-idx col-idx)]
                  (let [start-line (str (subs line 0 col-idx) " " (subs line (+ col-idx 3)))
                        match-line-str (nth current-lines match-line)
                        new-match (str (subs match-line-str 0 match-col) (subs match-line-str (inc match-col)))
                        new-lines (if (= line-idx match-line)
                                    (assoc current-lines line-idx
                                           (str (subs line 0 col-idx) " " (subs line (+ col-idx 3) match-col) (subs line (inc match-col))))
                                    (-> current-lines
                                        (assoc line-idx start-line)
                                        (assoc match-line new-match)))]
                    (swap! log conj (str "  " file-url ":" (:line f) "  remove redundant do"))
                     (recur more new-lines (inc fixed)))
                   (recur more current-lines fixed))
                 (recur more current-lines fixed)))))))))

