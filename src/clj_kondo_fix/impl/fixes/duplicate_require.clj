(ns clj-kondo-fix.impl.fixes.duplicate-require
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [remove-require-finding
                                                      cleanup-empty-clauses]]))

(defn find-require-aliases
  "Returns [{:alias string :line-idx int}] for every [ns-name :as alias] entry
   found across all lines. Uses re-seq so multiple entries on one line are all
   captured."
  [lines ns-name]
  (let [pattern (re-pattern (str "\\[" (java.util.regex.Pattern/quote ns-name)
                                 "\\s+:as\\s+([^\\s\\]]+)\\]"))]
    (into []
          (mapcat (fn [[i line]]
                    (map (fn [[_ alias]] {:alias alias :line-idx i})
                         (re-seq pattern line)))
                  (map-indexed vector lines)))))

(defn find-alias-col
  "Returns the 1-indexed column of [ns-name :as alias] on the line, or 1 as fallback."
  [line ns-name alias]
  (let [pattern (re-pattern (str "\\[" (java.util.regex.Pattern/quote ns-name)
                                 "\\s+:as\\s+" (java.util.regex.Pattern/quote alias) "\\]"))
        m       (re-matcher pattern line)]
    (if (.find m) (inc (.start m)) 1)))

(defn alias-used-in-file?
  "Returns true if alias/ appears in any line (word-boundary anchored)."
  [lines alias]
  (let [pattern (re-pattern (str "\\b" (java.util.regex.Pattern/quote alias) "/"))]
    (boolean (some #(re-find pattern %) lines))))

(defn- fix-duplicate-require-finding
  "Per-finding handler for duplicate-require. Determines which alias to keep,
   renames usages when both aliases were in use, then delegates entry removal
   to remove-require-finding."
  [current-lines f fu log]
  (let [ns-name     (some-> (re-find #"^duplicate require of (.+)$" (:message f)) second)
        all-aliases (when ns-name (find-require-aliases current-lines ns-name))]
    (if (not= (count all-aliases) 2)
      ;; Not exactly 2 entries (already fixed, or 3+ duplicates) — fall back.
      (remove-require-finding current-lines f fu log)
      (let [first-entry  (first all-aliases)
            second-entry (second all-aliases)
            first-alias  (:alias first-entry)
            second-alias (:alias second-entry)
            first-used?  (alias-used-in-file? current-lines first-alias)
            second-used? (alias-used-in-file? current-lines second-alias)
            ;; Loser = entry to remove.
            ;; - Only first used or neither used → remove second (reported duplicate).
            ;; - Only second used                → remove first.
            ;; - Both used                       → keep longer alias; first wins tie.
            [loser keeper]
            (cond
              (not second-used?) [second-entry first-entry]
              (not first-used?)  [first-entry second-entry]
              :else (if (>= (count first-alias) (count second-alias))
                      [second-entry first-entry]
                      [first-entry second-entry]))
            ;; Rename loser/ → keeper/ throughout the file when both were in use.
            renamed-lines
            (if (and first-used? second-used?)
              (do (swap! log conj (str "  " fu "  rename "
                                       (:alias loser) "/ -> " (:alias keeper) "/"))
                  (mapv #(str/replace %
                                      (re-pattern (str "\\b"
                                                       (java.util.regex.Pattern/quote (:alias loser))
                                                       "/"))
                                      (str (:alias keeper) "/"))
                        current-lines))
              current-lines)
            ;; Synthetic finding pointing at the loser's exact position.
            loser-col     (find-alias-col (nth renamed-lines (:line-idx loser))
                                          ns-name (:alias loser))
            loser-finding {:line    (inc (:line-idx loser))
                           :col     loser-col
                           :message (str "duplicate require of " ns-name)}]
        (remove-require-finding renamed-lines loser-finding fu log)))))

(defn fix-duplicate-require-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
                     (fn [ls f] (fix-duplicate-require-finding ls f fu log))
                     cleanup-empty-clauses)))
