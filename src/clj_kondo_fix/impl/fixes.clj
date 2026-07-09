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
            idx  (find-ns-on-line line ns-name col-start)]
        (if (nil? idx)
          (do (swap! log conj (str "  " file-url ":" (:line finding) "  skip: can't find [" ns-name))
              [lines nil])
          ;; Try single-line bracket match first; fall back to across-lines for
          ;; multi-line entries like [ns.name\n :as alias].
          (let [single-end (find-matching-bracket line idx)
                [end-line end-col]
                (if single-end
                  [line-idx single-end]
                  (find-matching-bracket-across-lines lines line-idx idx))]
            (if (nil? end-line)
              (do (swap! log conj (str "  " file-url ":" (:line finding) "  skip: unmatched bracket for " ns-name))
                  [lines nil])
              (let [before     (subs line 0 idx)
                    after      (subs (nth lines end-line) (inc end-col))
                    ;; Remove the entry span (line-idx..end-line) entirely.
                    remove-span  (fn [ls]
                                   (vec (concat (take line-idx ls)
                                                (drop (inc end-line) ls))))
                    ;; Replace the entry span with a single new line.
                    replace-span (fn [ls new-line]
                                   (vec (concat (take line-idx ls)
                                                [new-line]
                                                (drop (inc end-line) ls))))]
                (cond
                  ;; Entry is alone on its line(s) — just remove the span.
                  (and (re-find #"^\s*$" before)
                       (re-find #"^\s*,?\s*$" after))
                  (do (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name))
                      [(remove-span lines) true])

                  ;; Last entry — closing paren follows immediately after ].
                  (re-find #"^\s*\)" after)
                  (let [prev-idx (dec line-idx)]
                    (if (and (>= prev-idx 0)
                             (re-find #"^\s*$" before)
                             (re-find #"^\s*\[" (nth lines prev-idx)))
                      ;; Sub-branch A: prev line starts with [ — merge ) there.
                      (let [prev-line (nth lines prev-idx)]
                        (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                        [(-> (remove-span lines)
                             (assoc prev-idx (str prev-line after)))
                         true])
                      ;; Sub-branch B: scan backward for nearest ]-ending line
                      ;; (handles (:require on prev line or multi-line prev entry).
                      (if (and (re-find #"^\s*$" before)
                               (re-find #"^\s*\)+$" (str/trim after)))
                        (let [close-str  (str/trim after)
                              attach-idx (loop [j (dec line-idx)]
                                           (when (>= j 0)
                                             (if (str/ends-with? (str/trim (nth lines j)) "]")
                                               j
                                               (recur (dec j)))))]
                          (if attach-idx
                            (let [new-attach (str (nth lines attach-idx) close-str)]
                              (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                              [(vec (concat (take attach-idx lines)
                                            [new-attach]
                                            (subvec lines (inc attach-idx) line-idx)
                                            (drop (inc end-line) lines)))
                               true])
                            (let [new-line (str before after)]
                              (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                              [(replace-span lines new-line) true])))
                        (let [new-line (str before after)]
                          (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                          [(replace-span lines new-line) true]))))

                  :else
                  (let [new-line (str before after)]
                    (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name))
                    ;; If removing the entry left an empty (:require, absorb the
                    ;; next sibling entry onto this line.  Use end-line+1 so that
                    ;; multi-line entries don't accidentally grab a continuation line.
                    (if (re-find #"^\s*\(\s*:\w+\s*$" new-line)
                      (let [next-idx (inc end-line)]
                        (if (and (< next-idx (count lines))
                                 (re-find #"^\s*\[" (nth lines next-idx)))
                          (let [indent (re-find #"^\s*" new-line)
                                entry  (str/trim (nth lines next-idx))]
                            [(vec (concat (take line-idx lines)
                                          [(str indent "(:require " entry)]
                                          (drop (inc next-idx) lines)))
                             true])
                          [(replace-span lines new-line) true]))
                      [(replace-span lines new-line) true])))))))))))



(defn cleanup-empty-clauses [lines]
  (loop [i 0, lines lines]
    (if (>= i (count lines))
      lines
      (let [line (nth lines i)]
        (cond
          ;; Empty ns-form clause on one line: (:require ) or (:require )) etc.
          ;; Restrict to known ns clause keywords to avoid matching keyword lookups
          ;; like (:count))) in threading macros.
          ;; The first ) closes the clause; any extra )s close outer forms
          ;; and must be attached to the preceding line.
          (re-find #"^\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*\)+\s*$" line)
          (let [extras (let [[_ ps] (re-find #"^\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*(\)+)\s*$" line)]
                         (subs ps 1))]  ; parens beyond the clause's own )
            (if (and (pos? i) (not (str/blank? extras)))
              (let [prev (nth lines (dec i))]
                (recur (dec i) (vec (concat (take (dec i) lines)
                                            [(str prev extras)]
                                            (drop (inc i) lines)))))
              (recur i (vec (concat (take i lines) (drop (inc i) lines))))))

          (and (re-find #"^\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*$" line)
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
;; Fix: duplicate-require — keep one alias, rename usages if needed
;; ------------------------------------------------------------

(defn find-require-aliases [lines ns-name]
  "Returns [{:alias string :line-idx int}] for every [ns-name :as alias] entry found.
   Uses re-seq so multiple entries on the same line are all captured."
  (let [pattern (re-pattern (str "\\[" (java.util.regex.Pattern/quote ns-name)
                                 "\\s+:as\\s+([^\\s\\]]+)\\]"))]
    (into []
          (mapcat (fn [[i line]]
                    (map (fn [[_ alias]] {:alias alias :line-idx i})
                         (re-seq pattern line)))
                  (map-indexed vector lines)))))

(defn find-alias-col [line ns-name alias]
  "Returns the 1-indexed column of [ns-name :as alias] on the line, or 1 as fallback."
  (let [pattern (re-pattern (str "\\[" (java.util.regex.Pattern/quote ns-name)
                                 "\\s+:as\\s+" (java.util.regex.Pattern/quote alias) "\\]"))
        m       (re-matcher pattern line)]
    (if (.find m) (inc (.start m)) 1)))

(defn alias-used-in-file? [lines alias]
  "Returns true if `alias/` appears in any line (word-boundary anchored)."
  (let [pattern (re-pattern (str "\\b" (java.util.regex.Pattern/quote alias) "/"))]
    (boolean (some #(re-find pattern %) lines))))

(defn fix-duplicate-require-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted   (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted
           current-lines lines
           fixed 0]
      (if (nil? f)
        (let [cleaned (cleanup-empty-clauses current-lines)]
          {:fixed fixed :lines cleaned :changed? (or (pos? fixed) (not= cleaned lines))})
        (let [ns-name     (some-> (re-find #"^duplicate require of (.+)$" (:message f)) second)
              all-aliases (when ns-name (find-require-aliases current-lines ns-name))]
          (if (not= (count all-aliases) 2)
            ;; Not exactly 2 entries (already fixed, or 3+ duplicates) — fall back
            (let [[new-lines changed] (remove-require-finding current-lines f file-url log)]
              (recur more new-lines (if changed (inc fixed) fixed)))
            (let [first-entry   (first all-aliases)
                  second-entry  (second all-aliases)
                  first-alias   (:alias first-entry)
                  second-alias  (:alias second-entry)
                  first-used?   (alias-used-in-file? current-lines first-alias)
                  second-used?  (alias-used-in-file? current-lines second-alias)
                  ;; Loser = entry to remove.
                  ;; - Only first used or neither used → remove second (reported duplicate).
                  ;; - Only second used                → remove first.
                  ;; - Both used                       → keep longer alias; first wins tie.
                  [loser keeper]
                  (cond
                    (not second-used?)
                    [second-entry first-entry]

                    (not first-used?)
                    [first-entry second-entry]

                    :else
                    (if (>= (count first-alias) (count second-alias))
                      [second-entry first-entry]
                      [first-entry  second-entry]))
                  ;; Rename loser/  →  keeper/ throughout the file when both were in use
                  renamed-lines
                  (if (and first-used? second-used?)
                    (do (swap! log conj (str "  " file-url "  rename "
                                             (:alias loser) "/ -> " (:alias keeper) "/"))
                        (mapv #(str/replace %
                                            (re-pattern (str "\\b"
                                                             (java.util.regex.Pattern/quote (:alias loser))
                                                             "/"))
                                            (str (:alias keeper) "/"))
                              current-lines))
                    current-lines)
                  ;; Synthetic finding pointing at the loser's exact position
                  loser-col     (find-alias-col (nth renamed-lines (:line-idx loser))
                                                ns-name (:alias loser))
                  loser-finding {:line    (inc (:line-idx loser))
                                 :col     loser-col
                                 :message (str "duplicate require of " ns-name)}
                  [new-lines changed] (remove-require-finding renamed-lines loser-finding file-url log)]
              (recur more new-lines (if changed (inc fixed) fixed)))))))))


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
                  (< col-idx 0) (> (+ col-idx 3) (count (nth current-lines line-idx))))
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
                    (let [prefix (str/trimr (subs def-line 0 bracket-idx))]
                      ;; if prefix is blank the defn name is on a different line
                      ;; from the param vector — skip rather than produce invalid code
                      (if (str/blank? prefix)
                        (recur more current-lines fixed)
                        (let [params (subs def-line bracket-idx (inc end-idx))
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
                                 (inc fixed)))))))))))))))

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
                  (< col-idx 0) (> (+ col-idx 3) (count (nth current-lines line-idx))))
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

;; ------------------------------------------------------------
;; Fix: redundant-let — merge nested lets into one
;; ------------------------------------------------------------

(defn reindent-line [line old-leading new-leading]
  "Strip old-leading spaces and prepend new-leading spaces.
   Returns line unchanged if it does not have exactly old-leading leading spaces."
  (if (str/blank? line)
    line
    (let [actual (count (re-find #"^ *" line))]
      (if (= actual old-leading)
        (str (apply str (repeat (max 0 new-leading) " ")) (subs line old-leading))
        line))))

(defn find-outer-let [lines inner-line-idx inner-col-idx]
  "Scan backward from inner-line-idx to find the nearest enclosing (let.
   Returns {:line :col :close-line :close-col} or nil."
  (loop [i inner-line-idx]
    (when (>= i 0)
      (let [line      (nth lines i)
            max-col   (if (= i inner-line-idx) inner-col-idx (count line))
            portion   (subs line 0 max-col)
            ;; collect all valid (let positions on this line before max-col
            candidates
            (loop [from 0 acc []]
              (let [idx (.indexOf portion "(let" from)]
                (if (neg? idx)
                  acc
                  (let [after (+ idx 4)
                        nch   (when (< after (count line)) (nth line after))]
                    (if (or (nil? nch) (= nch \space) (= nch \[))
                      (if-let [[ml mc] (find-matching-bracket-across-lines lines i idx)]
                        ;; match must be at or after the inner let position
                        (if (or (> ml inner-line-idx)
                                (and (= ml inner-line-idx) (>= mc inner-col-idx)))
                          (recur (inc idx) (conj acc {:line i :col idx :close-line ml :close-col mc}))
                          (recur (inc idx) acc))
                        (recur (inc idx) acc))
                      (recur (inc idx) acc))))))]
        (if (seq candidates)
          (last candidates) ; rightmost = closest parent
          (recur (dec i)))))))

(defn- find-bracket-open [line start-col]
  "Scan right from start-col to find the first [ on line. Returns col or nil."
  (loop [j start-col]
    (when (< j (count line))
      (if (= \[ (nth line j)) j (recur (inc j))))))

(defn- spaces [n] (apply str (repeat (max 0 n) " ")))

(defn merge-lets [lines inner-line-idx inner-col-idx outer]
  "Merge the outer let (described by outer map) with the inner let at
   (inner-line-idx, inner-col-idx).  Returns the new line vector, or nil
   to signal that this case should be skipped."
  (let [{OL  :line  OC  :col
         OCL :close-line  OCC :close-col} outer
        outer-line    (nth lines OL)
        outer-bv-open (find-bracket-open outer-line (+ OC 4))
        [OBL OBC]     (when outer-bv-open
                        (find-matching-bracket-across-lines lines OL outer-bv-open))]
    ;; precondition: outer binding vector must be single-line
    (when (and OBL (= OBL OL))
      (let [IL            inner-line-idx
            IC            inner-col-idx
            inner-line    (nth lines IL)
            inner-bv-open (find-bracket-open inner-line (+ IC 4))
            [IBL IBC]     (when inner-bv-open
                            (find-matching-bracket-across-lines lines IL inner-bv-open))
            [ICL ICC]     (find-matching-bracket-across-lines lines IL IC)]
        (when (and IBL ICL)
          (let [outer-bind-col (+ OC 6)  ; column where outer bindings start (after "(let [")
                single-line?  (= OL IL)] ; both lets on the same line

            (if single-line?
              ;; ---- Single-line: pure string surgery on one line ----
              (let [line        outer-line
                    outer-binds (str/trim (subs line (inc outer-bv-open) OBC))
                    inner-binds (str/trim (subs line (inc inner-bv-open) IBC))
                    body-text   (subs line (inc IBC) ICC)
                    after-outer (subs line (inc OCC))
                    merged      (str "(let [" outer-binds " " inner-binds "]" body-text ")" after-outer)]
                (assoc lines OL (str (subs line 0 OC) merged)))

              ;; ---- Multi-line merge ----
              (let [;; outer line: strip the closing ] of its binding vector
                    new-outer-line (subs outer-line 0 OBC)

                    ;; intermediate lines (between outer binding close and inner let)
                    ;; moved before the merged let, un-indented by 2
                    intermediate (subvec lines (inc OL) IL)
                    moved-lines  (mapv #(reindent-line % (+ OC 2) OC) intermediate)

                    ;; inner binding lines, re-indented to outer-bind-col
                    inner-bind-lines
                    (if (= IBL IL)
                      ;; single-line inner binding vector: subs between [ and ]
                      [(str (spaces outer-bind-col)
                            (subs inner-line (inc inner-bv-open) IBC)
                            "]")]
                      ;; multi-line inner binding vector
                      (vec
                       (concat
                        ;; first line: everything after [ on the inner-let line
                        [(str (spaces outer-bind-col)
                              (str/trimr (subs inner-line (inc inner-bv-open))))]
                        ;; continuation lines
                        (for [i (range (inc IL) IBL)]
                          (reindent-line (nth lines i) (+ IC 6) outer-bind-col))
                        ;; closing ] line
                        [(reindent-line (nth lines IBL) (+ IC 6) outer-bind-col)])))

                    ;; body + close lines
                    body-close-lines
                    (if (= ICL IL)
                      ;; inner close is on the same line as the inner let —
                      ;; body (if any) is between ] and )
                      (let [body-inline  (str/trim (subs inner-line (inc IBC) ICC))
                            after-outer  (subs inner-line (inc OCC))]
                        (if (str/blank? body-inline)
                          :no-body
                          [(str (spaces (+ OC 2)) body-inline ")" after-outer)]))
                      ;; body is on lines after the inner binding vector
                      (let [body-lines  (mapv #(reindent-line (nth lines %) (+ IC 2) (+ OC 2))
                                              (range (inc IBL) ICL))
                            close-line  (nth lines ICL)
                            ;; remove the inner ) from the close line
                            close-mod   (str (subs close-line 0 ICC)
                                             (subs close-line (inc ICC)))
                            close-rein  (reindent-line close-mod (+ IC 2) (+ OC 2))]
                        (conj body-lines close-rein)))

                    ;; no-body: append ) to the last binding line
                    [inner-bind-lines body-close-lines]
                    (if (= body-close-lines :no-body)
                      (let [after-outer (subs inner-line (inc OCC))]
                        [(conj (vec (butlast inner-bind-lines))
                               (str (last inner-bind-lines) ")" after-outer))
                         []])
                      [inner-bind-lines body-close-lines])]

                (vec (concat
                      (take OL lines)
                      moved-lines
                      [new-outer-line]
                      inner-bind-lines
                      body-close-lines
                      (drop (inc OCL) lines)))))))))))

(defn fix-redundant-let-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted   (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted
           current-lines lines
           fixed 0]
      (if (nil? f)
        {:fixed fixed :lines current-lines :changed? (pos? fixed)}
        (let [IL    (dec (:line f))
              IC    (dec (:col f))
              outer (find-outer-let current-lines IL IC)]
          (if (nil? outer)
            (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: could not find outer let"))
                (recur more current-lines fixed))
            (let [new-lines (merge-lets current-lines IL IC outer)]
              (if (nil? new-lines)
                (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: unsupported let structure"))
                    (recur more current-lines fixed))
                (do (swap! log conj (str "  " file-url ":" (:line f) "  merge redundant let"))
                    (recur more new-lines (inc fixed)))))))))))

