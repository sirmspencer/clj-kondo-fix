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
                  ;; A trailing inline comment (;; ...) counts as nothing —
                  ;; it belongs to the removed entry and goes with it.
                  ;; After removal, if straggling close-parens remain on the
                  ;; new line-idx position (e.g. from a )) that closed the
                  ;; require on its own line), pull them up to the nearest
                  ;; preceding ]-ending line.
                  (and (re-find #"^\s*$" before)
                       (re-find #"^\s*,?\s*(?:;.*)?$" after))
                  (let [removed   (remove-span lines)
                        straggler (when (< line-idx (count removed)) (nth removed line-idx))
                        cleaned
                        (if (and straggler (re-find #"^\s*\)+\s*$" straggler))
                          (let [close-str  (str/trim straggler)
                                attach-idx (loop [j (dec line-idx)]
                                             (when (>= j 0)
                                               (if (str/ends-with? (str/trim (nth removed j)) "]")
                                                 j
                                                 (recur (dec j)))))]
                            (if attach-idx
                              (vec (concat (take attach-idx removed)
                                           [(str (nth removed attach-idx) close-str)]
                                           (subvec removed (inc attach-idx) line-idx)
                                           (drop (inc line-idx) removed)))
                              removed))
                          removed)]
                    (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name))
                    [cleaned true])

                  ;; Last entry — closing paren follows immediately after ].
                  (re-find #"^\s*\)" after)
                  (let [prev-idx (dec line-idx)
                        ;; Helper: strip indentation from orphan comment-only lines
                        ;; (lines between the surviving entry and the removed entry).
                        strip-orphan-comments
                        (fn [ls from to]
                          (mapv (fn [l] (if (re-find #"^\s+;" l) (str/triml l) l))
                                (subvec ls from to)))]
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
                            (let [new-attach     (str (nth lines attach-idx) close-str)
                                  orphan-lines   (strip-orphan-comments lines (inc attach-idx) line-idx)]
                              (swap! log conj (str "  " file-url ":" (:line finding) "  remove require: " ns-name " (last entry)"))
                              [(vec (concat (take attach-idx lines)
                                            [new-attach]
                                            orphan-lines
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
                    (cond
                      ;; Dedicated-line empty clause: (:require\n   [next-entry])
                      ;; Rebuild the clause line with the next entry pulled up.
                      (re-find #"^\s*\(\s*:\w+\s*$" new-line)
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

                      ;; Inline empty clause on (ns ...) line: (ns foo (:require [next-entry])
                      ;; Pull the next line's entry up onto the same line.
                      (re-find #"^\(ns\b.*\(\s*:\w+\s*$" new-line)
                      (let [next-idx (inc end-line)]
                        (if (and (< next-idx (count lines))
                                 (re-find #"^\s*\[" (nth lines next-idx)))
                          (let [entry (str/trim (nth lines next-idx))]
                            [(vec (concat (take line-idx lines)
                                          [(str new-line " " entry)]
                                          (drop (inc next-idx) lines)))
                             true])
                          [(replace-span lines new-line) true]))

                      :else
                      [(replace-span lines new-line) true])))))))))))



(defn cleanup-empty-clauses [lines]
  (loop [i 0, lines lines]
    (if (>= i (count lines))
      lines
      (let [line (nth lines i)]
        (cond
          ;; Inline empty clause on the (ns ...) line: (ns foo (:require )) → (ns foo)
          ;; Only fires when the clause has no entries — detected by no content between
          ;; the clause keyword and its closing paren.
          (re-find #"^\(ns\b.*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*\)" line)
          (recur i (assoc lines i
                          (str/replace line
                                       #"\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*\)"
                                       "")))

          ;; Dangling close-paren after (ns ...) on its own line: (ns foo\n   ) → (ns foo)
          ;; Arises when all clauses in a multi-line ns form have been cleaned out and
          ;; only the ns form's closing ) remains on the next line.
          (and (re-find #"^\(ns\b" line)
               (not (str/includes? line ")"))
               (< (inc i) (count lines))
               (re-find #"^\s*\)+\s*$" (nth lines (inc i))))
          (let [parens (re-find #"\)+" (nth lines (inc i)))]
            (recur i (vec (concat (take i lines)
                                  [(str line parens)]
                                  (drop (+ i 2) lines)))))

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
            ;; Back up one position so the preceding (ns …) line can be
            ;; re-checked by the dangling-close case if it now qualifies.
            (recur (max 0 (dec i)) (vec (concat (take i lines)
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


;; ------------------------------------------------------------
;; Fix: unused-binding — context detection helpers
;; ------------------------------------------------------------

(def ^:private let-like-forms
  #{"let" "loop" "for" "doseq" "binding" "with-open"
    "if-let" "when-let" "if-some" "when-some"
    "with-local-vars" "letfn" "when-first" "dotimes"
    "with-bindings"})

(def ^:private fn-like-forms
  #{"defn" "defn-" "fn" "fn*" "defmethod" "defmacro"
    "defmulti" "reify" "proxy"})

(defn- find-opening-bracket [lines line-idx col-idx]
  "Scan left from (line-idx, col-idx-1) to find the [ that directly contains
   the position.  Returns {:line l :col c} or nil."
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

(defn- classify-bracket-context [text-before-bracket]
  "Given text before a [, classify as :keys-destr, :fn-param, or :let-binding."
  (let [words (re-seq #"[a-zA-Z*!?][a-zA-Z0-9*!?-]*" text-before-bracket)
        rwords (vec (reverse words))]
    (cond
      (some #{"keys" "strs" "syms" "keys!" "strs!" "syms!"} (take 2 rwords)) :keys-destr
      (some let-like-forms (take 3 rwords)) :let-binding
      (some fn-like-forms  (take 3 rwords)) :fn-param
      :else :let-binding)))

(defn detect-binding-context
  "Detect the context of the binding at (line-idx, col-idx).
   Returns :as-clause, :fn-param, :let-binding, :keys-destr-fn, or :keys-destr-let."
  [lines line-idx col-idx]
  (let [line       (nth lines line-idx)
        before-col (subs line 0 col-idx)]
    (if (re-find #":as\s+$" before-col)
      :as-clause
      (if-let [{brk-line :line brk-col :col}
               (find-opening-bracket lines line-idx col-idx)]
        (let [brk-text     (subs (nth lines brk-line) 0 brk-col)
              ;; If [ is at start of line, scan previous lines for context
              context-text (if (str/blank? brk-text)
                             (loop [i (dec brk-line)]
                               (if (< i 0) ""
                                 (let [lt (str/trim (nth lines i))]
                                   (if (str/blank? lt) (recur (dec i)) lt))))
                             brk-text)
              inner-ctx    (classify-bracket-context context-text)]
          (if (= inner-ctx :keys-destr)
            ;; Find the { that contains this [ then find the [ containing that {
            (if-let [{outer-line :line outer-col :col}
                     (find-opening-bracket lines brk-line brk-col)]
              (let [outer-text    (subs (nth lines outer-line) 0 outer-col)
                    outer-context (if (str/blank? outer-text)
                                    (loop [i (dec outer-line)]
                                      (if (< i 0) ""
                                        (let [lt (str/trim (nth lines i))]
                                          (if (str/blank? lt) (recur (dec i)) lt))))
                                    outer-text)
                    outer-ctx     (classify-bracket-context outer-context)]
                (if (= outer-ctx :fn-param) :keys-destr-fn :keys-destr-let))
              :keys-destr-fn)  ; can't find outer → default to fn-param behaviour
             inner-ctx))
        :let-binding))))

;; ------------------------------------------------------------
;; Fix: unused-binding — rename to underscore prefix / remove
;; ------------------------------------------------------------

(defn remove-as-clause-from-line [line binding-name idx word-end]
  (if-let [m (re-find #"[\s,]:as\s+[\w-]+$" (subs line 0 word-end))]
    (let [match-str (if (string? m) m (first m))
          clause-start (- word-end (count match-str))]
      (str (subs line 0 clause-start) (subs line word-end)))
    line))

;; forward declaration — remove-referred-var-from-line is defined in the
;; referred-var section below but is also used for :keys-destr removal here.
(declare remove-referred-var-from-line)

;; ------------------------------------------------------------
;; Destructuring map collapse helpers
;; ------------------------------------------------------------

(defn- map-collapses-to [content]
  "Given the string content between { and } (inclusive whitespace), decide if
   the map can be collapsed to a simpler form.  Returns the target string or nil.
   Rules:
   - All 'concrete' bindings (non-`:as`, non-`:keys/strs/syms []`) must be
     _-prefixed (unused).
   - If an :as binding is present, collapse to that name (drop the `_` if the
     binding itself doesn't start with it, i.e. `state` not `_state`).
   - If no :as binding, collapse to `_`."
  (let [;; extract :as binding name (with or without _ prefix)
        as-name   (second (re-find #":as\s+([a-zA-Z_][a-zA-Z0-9*!?_-]*)" content))
        ;; strip :as clause from analysis
        no-as     (if as-name
                    (str/replace content
                                 (re-pattern (str ":as\\s+" (java.util.regex.Pattern/quote as-name)))
                                 "")
                    content)
        ;; strip empty :foo/keys [], :keys [], etc.
        no-empty  (str/replace no-as #":[\w./]+\s*\[\s*\]" "")
        ;; strip :or {} and similar empty sub-maps
        no-empty2 (str/replace no-empty #":[\w./]+\s*\{\s*\}" "")
        remaining (str/trim no-empty2)]
    (cond
      ;; nothing meaningful left — all was empty vectors / :as
      ;; BUT only if the original content had actual destructuring patterns.
      ;; A bare {} (empty map used as a value) must NOT be collapsed.
      (and (str/blank? remaining)
           (or as-name
               (re-find #":(?:keys|strs|syms)!?\s*\[" content)
               (re-find #"_[a-zA-Z]" content)))
      (or as-name "_")

      ;; only _-prefixed concrete bindings remain: `_x :keyword` pairs
      (re-matches #"(?:\s*_[a-zA-Z][a-zA-Z0-9*!?-]*\s+:[\w./]+\s*)+" remaining)
      (or as-name "_")

      :else nil)))

(defn- enclosing-bracket-type [lines line-idx col-idx]
  "Returns the character ([, (, or {) of the bracket that directly contains
   (line-idx, col-idx-1), tracking all three bracket pairs so function-call
   parens act as barriers.  Returns nil at top level."
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

(defn- binding-bracket? [lines bracket-line bracket-col]
  "Returns true if the [ at (bracket-line, bracket-col) belongs to a known
   binding form (let/defn/fn/for/etc. or :keys/:strs/:syms).
   Used by collapse-destr-maps to reject function-call argument vectors."
  (let [brk-text (subs (nth lines bracket-line) 0 bracket-col)
        ctx-text  (if (str/blank? brk-text)
                    (loop [i (dec bracket-line)]
                      (if (< i 0) ""
                        (let [lt (str/trim (nth lines i))]
                          (if (str/blank? lt) (recur (dec i)) lt))))
                    brk-text)
        words  (re-seq #"[a-zA-Z*!?][a-zA-Z0-9*!?-]*" ctx-text)
        rwords (vec (reverse words))]
    (or (some let-like-forms (take 3 rwords))
        (some fn-like-forms  (take 3 rwords))
        (some #{"keys" "strs" "syms" "keys!" "strs!" "syms!"} (take 2 rwords)))))

(defn collapse-destr-maps [lines]
  "Post-pass: replace destructuring maps whose bindings are all effectively
   unused (_-prefixed or empty) with either their :as name or plain `_`.
   Only collapses maps that are directly inside a [...] vector (destructuring
   position) — never maps that are arguments to function calls (inside (...))."
  (loop [i 0, current-lines (vec lines)]
    (if (>= i (count current-lines))
      current-lines
      (let [line (nth current-lines i)]
        ;; scan the line for { chars
        (if-let [j (some (fn [j] (when (= \{ (nth line j)) j))
                         (range (count line)))]
          (if-let [[cl cc] (find-matching-bracket-across-lines current-lines i j)]
            (let [                  ;; Only collapse maps in destructuring (vector) position.
                  ;; Two-part check:
                  ;; 1. enclosing-bracket-type: must be [ (not ( or {)
                  ;; 2. binding-bracket?: the [ must belong to a known binding form
                  ;;    (let/defn/fn/for/etc.), not a function-call argument vector
                  ;;    like (side-effect-fn [{:foo [x]} y]).
                  enc-ch        (enclosing-bracket-type current-lines i j)
                  in-destr-pos? (and (= enc-ch \[)
                                     (if-let [{bl :line bc :col}
                                              (find-opening-bracket current-lines i j)]
                                       (binding-bracket? current-lines bl bc)
                                       false))
                  ;; extract full content between { and }
                  content (if (= i cl)
                            (subs line (inc j) cc)
                            (str (subs line (inc j))
                                 (str/join "\n" (map #(nth current-lines %) (range (inc i) cl)))
                                 (subs (nth current-lines cl) 0 cc)))
                  target (when in-destr-pos? (map-collapses-to content))]
              (if target
                ;; replace the {…} span with target
                (let [before    (subs line 0 j)
                      after-cc  (subs (nth current-lines cl) (inc cc))
                      new-line  (str before target after-cc)
                      ;; remove lines i+1..cl, replace line i with new-line
                      new-lines (vec (concat (take i current-lines)
                                             [new-line]
                                             (drop (inc cl) current-lines)))]
                  (recur i new-lines))          ; retry same line (more { may remain)
                (recur (inc i) current-lines))) ; no collapse — move on
            (recur (inc i) current-lines))
          (recur (inc i) current-lines))))))

(defn fix-unused-binding-in-file
  "Fix unused bindings.  fix-contexts controls which binding types to handle:
     :as-clause    — remove the :as clause when unused
     :fn-param     — prefix unused function params with _
     :keys-destr-fn — remove from {:keys/strs/syms []} in function params
     :keys-destr-let — remove from {:keys/strs/syms []} in let bindings
     :let-binding  — prefix unused let/loop/for/doseq bindings with _
   Default: #{:as-clause :fn-param :keys-destr-fn :keys-destr-let} (let scalar bindings skipped)"
  ([file-path lines findings log]
   (fix-unused-binding-in-file file-path lines findings log
                               #{:as-clause :fn-param :keys-destr-fn :keys-destr-let}))
    ([file-path lines findings log fix-contexts]
     (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
           sorted   (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
       (loop [[f & more] sorted
              current-lines lines
              fixed 0]
         (if (nil? f)
            ;; Post-pass: collapse destructuring maps (position-checked inside)
            (let [collapsed (collapse-destr-maps current-lines)]
              {:fixed fixed :lines collapsed :changed? (or (pos? fixed) (not= collapsed lines))})
         (let [binding-name (extract-binding-name (:message f))
               line-idx     (dec (:line f))
               col-idx      (dec (:col f))]
           (if (or (nil? binding-name) (< line-idx 0) (>= line-idx (count current-lines)))
             (recur more current-lines fixed)
             (let [line (nth current-lines line-idx)
                   idx  (find-binding-on-line line binding-name col-idx)]
               (if (nil? idx)
                 (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: can't find binding " binding-name " on line"))
                     (recur more current-lines fixed))
                 (let [word-end (word-end-pos line idx)]
                   (if (not= (subs line idx word-end) binding-name)
                     (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: binding " binding-name " not found at column " (:col f)))
                         (recur more current-lines fixed))
                     ;; skip namespaced keys e.g. {:keys [ns/name]}
                     (if (and (pos? idx) (= \/ (nth line (dec idx))))
                       (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: binding " binding-name " is part of a namespaced key"))
                           (recur more current-lines fixed))
                        (let [ctx (detect-binding-context current-lines line-idx idx)]
                          (cond
                            ;; :as clause — rename to _name (keep in place so post-pass can
                            ;; :as clause — remove the whole clause
                             (and (= ctx :as-clause) (fix-contexts :as-clause))
                             (let [new-line (remove-as-clause-from-line line binding-name idx word-end)]
                               (swap! log conj (str "  " file-url ":" (:line f) "  remove unused :as binding: " binding-name))
                               (recur more (assoc current-lines line-idx new-line) (inc fixed)))

                           ;; :keys/:strs/:syms destructuring in fn param — remove from vector
                           (and (= ctx :keys-destr-fn) (fix-contexts :keys-destr-fn))
                           (let [new-line  (remove-referred-var-from-line line binding-name idx)
                                 new-lines (cond
                                             ;; key was the only item on its line — remove the line
                                             (str/blank? new-line)
                                             (vec (concat (take line-idx current-lines)
                                                          (drop (inc line-idx) current-lines)))
                                             ;; key was first on a {:keys [ line — pull next line's content up
                                             (and (re-find #"\[\s*$" new-line)
                                                  (< (inc line-idx) (count current-lines)))
                                             (let [next-idx  (inc line-idx)
                                                   next-trim (str/triml (nth current-lines next-idx))]
                                               (if (str/blank? next-trim)
                                                 (vec (concat (take line-idx current-lines)
                                                              [new-line]
                                                              (drop (inc next-idx) current-lines)))
                                                 (vec (concat (take line-idx current-lines)
                                                              [(str (str/trimr new-line) next-trim)]
                                                              (drop (inc next-idx) current-lines)))))
                                             :else
                                             (assoc current-lines line-idx new-line))]
                             (if (= new-lines current-lines)
                               (recur more current-lines fixed)
                               (do (swap! log conj (str "  " file-url ":" (:line f) "  remove from keys vector: " binding-name))
                                   (recur more new-lines (inc fixed)))))

                           ;; :keys/:strs/:syms destructuring in let — also safe (just a deref, no side effects)
                           (and (= ctx :keys-destr-let) (fix-contexts :keys-destr-let))
                           (let [new-line  (remove-referred-var-from-line line binding-name idx)
                                 new-lines (cond
                                             (str/blank? new-line)
                                             (vec (concat (take line-idx current-lines)
                                                          (drop (inc line-idx) current-lines)))
                                             (and (re-find #"\[\s*$" new-line)
                                                  (< (inc line-idx) (count current-lines)))
                                             (let [next-idx  (inc line-idx)
                                                   next-trim (str/triml (nth current-lines next-idx))]
                                               (if (str/blank? next-trim)
                                                 (vec (concat (take line-idx current-lines)
                                                              [new-line]
                                                              (drop (inc next-idx) current-lines)))
                                                 (vec (concat (take line-idx current-lines)
                                                              [(str (str/trimr new-line) next-trim)]
                                                              (drop (inc next-idx) current-lines)))))
                                             :else
                                             (assoc current-lines line-idx new-line))]
                             (if (= new-lines current-lines)
                               (recur more current-lines fixed)
                               (do (swap! log conj (str "  " file-url ":" (:line f) "  remove from keys vector: " binding-name))
                                   (recur more new-lines (inc fixed)))))

                           ;; fn param — prefix with _
                           (and (= ctx :fn-param) (fix-contexts :fn-param))
                           (let [new-line (str (subs line 0 idx) "_" (subs line idx))]
                             (swap! log conj (str "  " file-url ":" (:line f) "  rename unused binding: " binding-name " -> _" binding-name))
                             (recur more (assoc current-lines line-idx new-line) (inc fixed)))

                           ;; let-binding — prefix with _ only if explicitly enabled
                           (and (= ctx :let-binding) (fix-contexts :let-binding))
                           (let [new-line (str (subs line 0 idx) "_" (subs line idx))]
                             (swap! log conj (str "  " file-url ":" (:line f) "  rename unused binding: " binding-name " -> _" binding-name))
                             (recur more (assoc current-lines line-idx new-line) (inc fixed)))

                           ;; anything else or context not in fix-contexts — skip
                           :else
                           (do (swap! log conj (str "  " file-url ":" (:line f) "  skip: binding " binding-name " in context " ctx " not enabled"))
                               (recur more current-lines fixed))))))))))))))))

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
              ;; Strip whitespace from only one side to preserve the separator
              ;; between remaining vars.  Prefer stripping trailing space from
              ;; before; only touch after's leading space when before has none.
              cleaned (if (re-find #"[\s,]$" before)
                        (str (subs before 0 (dec (count before))) after)
                        (str before (if (re-find #"^[\s,]" after)
                                      (subs after 1)
                                      after)))]
          cleaned)
        line))))

(defn fix-unused-import-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        (let [cleaned (cleanup-empty-clauses current-lines)]
          {:fixed fixed :lines cleaned :changed? (or (pos? fixed) (not= cleaned lines))})
        (let [msg (:message f)
              var-name (some-> (re-find #"^Unused import (.+)$" msg) second)
              line-idx (dec (:line f))
              col-idx (dec (:col f))]
          (if (or (nil? var-name) (< line-idx 0) (>= line-idx (count current-lines)))
            (recur more current-lines fixed)
            (let [line     (nth current-lines line-idx)
                  new-line (remove-referred-var-from-line line var-name col-idx)
                  ;; If removing the class left a bare [package] vector (no classes
                  ;; remaining), strip it so cleanup-empty-clauses can tidy the clause.
                  final-line (str/replace new-line #"\s*\[[a-z][a-zA-Z0-9.]*\s*\]" "")]
              (if (= final-line line)
                (recur more current-lines fixed)
                (do (swap! log conj (str "  " file-url ":" (:line f) "  remove unused import: " var-name))
                    (recur more
                           (assoc current-lines line-idx final-line)
                           (inc fixed)))))))))))

(defn- extract-ns-from-referred-var-msg [msg]
  "Extract namespace from '#'clojure.set/rename-keys is referred but never used'."
  (some-> (re-find #"^#'(.+)/[^/]+ is referred but never used$" msg) second))

(defn remove-bare-requires [lines ns-names file-url log]
  "Remove bare [ns-name] require entries (no :as, no :refer) for each ns-name.
   Reuses remove-require-finding so all edge-case handling is shared."
  (reduce (fn [current-lines ns-name]
            (let [pat (re-pattern (str "\\[" (java.util.regex.Pattern/quote ns-name) "\\s*\\]"))
                  hit (first (keep-indexed #(when (re-find pat %2) %1) current-lines))]
              (if (nil? hit)
                current-lines
                (let [synthetic {:line    (inc hit)
                                 :col     1
                                 :message (str "namespace " ns-name
                                               " is required but never used")}
                      [new-lines changed] (remove-require-finding current-lines synthetic file-url log)]
                  (if changed new-lines current-lines)))))
          lines
          ns-names))

(defn fix-unused-referred-var-in-file [file-path lines findings log]
  (let [file-url (str/replace file-path (str (System/getProperty "user.home")) "~")
        sorted (sort-by (juxt :line :col) #(compare %2 %1) (distinct findings))]
    (loop [[f & more] sorted current-lines lines fixed 0]
      (if (nil? f)
        (let [cleaned  (cleanup-empty-clauses current-lines)
              ;; remove any bare [ns-name] entries left after :refer cleanup
              ns-names (->> (distinct findings)
                            (map #(extract-ns-from-referred-var-msg (:message %)))
                            (filter some?)
                            distinct)
              after-bare (remove-bare-requires cleaned ns-names file-url log)
              ;; second cleanup pass: bare-require removal may have left empty clauses
              final      (cleanup-empty-clauses after-bare)]
          {:fixed fixed :lines final :changed? (or (pos? fixed) (not= final lines))})
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
                 ;; If only closing brackets/parens remain on the line after removal
                 ;; (e.g. ]]) from a multi-line :refer vector), pull them up onto the
                 ;; preceding line instead of leaving a straggler.
                 (let [new-lines
                       (if (re-find #"^\s*[\]\)]+\s*$" new-line)
                         (if (pos? line-idx)
                           (let [prev     (nth current-lines (dec line-idx))
                                 brackets (str/trim new-line)]
                             (vec (concat (take (dec line-idx) current-lines)
                                          [(str prev brackets)]
                                          (drop (inc line-idx) current-lines))))
                           (assoc current-lines line-idx new-line))
                         (assoc current-lines line-idx new-line))]
                   (swap! log conj (str "  " file-url ":" (:line f) "  remove referred var: " var-name))
                   (recur more new-lines (inc fixed)))))))))))

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
              line-idx (dec (:line f))]
          (if (nil? var-name)
            (recur more current-lines fixed)
            ;; Scan backwards from the finding line to locate the opening
            ;; top-level (def...) or (defn-...) form.
            (let [form-start (loop [i line-idx]
                               (cond
                                 (< i 0) nil
                                 (re-find #"^\s*\(def" (nth current-lines i)) i
                                 :else (recur (dec i))))
                  form-col   (when form-start
                               (.indexOf (nth current-lines form-start) "("))]
              (if (or (nil? form-start) (neg? form-col))
                (recur more current-lines fixed)
                (if-let [[end-line _] (find-matching-bracket-across-lines current-lines form-start form-col)]
                  (let [;; Include a preceding blank line in the removal if present
                        remove-start (if (and (> form-start 0)
                                              (str/blank? (nth current-lines (dec form-start))))
                                       (dec form-start)
                                       form-start)
                        new-lines (vec (concat (subvec current-lines 0 remove-start)
                                               (subvec current-lines (inc end-line))))]
                    (swap! log conj (str "  " file-url ":" (:line f) "  remove unused private var: " var-name))
                    (recur more new-lines (inc fixed)))
                  (recur more current-lines fixed))))))))))

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
                  (let [match-line-str (nth current-lines match-line)
                        new-match      (str (subs match-line-str 0 match-col) (subs match-line-str (inc match-col)))
                        dedent         (fn [l] (if (str/starts-with? l "  ") (subs l 2) l))
                        new-lines
                        (if (= line-idx match-line)
                          ;; single-line: (parent (do a b)) → (parent a b)
                          ;; trimr/triml to collapse spaces left by removing (do
                          (assoc current-lines line-idx
                                 (str (str/trimr (subs line 0 col-idx))
                                      " "
                                      (str/triml (subs line (+ col-idx 3) match-col))
                                      (subs line (inc match-col))))
                          ;; multi-line
                          (let [start-line (str (subs line 0 col-idx) (subs line (+ col-idx 3)))]
                            (if (str/blank? start-line)
                              ;; (do occupies its own line — remove it and dedent body
                              (vec (concat
                                     (subvec current-lines 0 line-idx)
                                     (mapv dedent (subvec current-lines (inc line-idx) match-line))
                                     [(dedent new-match)]
                                     (subvec current-lines (inc match-line))))
                              ;; (do is inline on a content line — keep existing behaviour
                              (-> current-lines
                                  (assoc line-idx start-line)
                                  (assoc match-line new-match)))))]
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

