(ns clj-kondo-fix.impl.require-entry
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket
                                              find-matching-bracket-across-lines]]))

;; ------------------------------------------------------------
;; Message parsing (require/namespace family)
;; ------------------------------------------------------------

(def unused-ns-re #"^namespace (.+) is required but never used$")
(def duplicate-require-re #"^duplicate require of (.+)$")

(defn extract-ns-name [msg]
  (or (some-> (re-find unused-ns-re msg) second)
      (some-> (re-find duplicate-require-re msg) second)))

;; ------------------------------------------------------------
;; Namespace-entry locators
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

(defn- find-prev-bracket-close
  "Scan backward from from-idx to find the nearest preceding line ending in ].
   Returns that line index or nil."
  [lines from-idx]
  (loop [j (dec from-idx)]
    (when (>= j 0)
      (if (str/ends-with? (str/trim (nth lines j)) "]")
        j
        (recur (dec j))))))

;; ------------------------------------------------------------
;; Core entry removal
;; ------------------------------------------------------------

(defn remove-require-entry
  "Core require-entry removal. Removes the entry for ns-name that starts at
   (line-idx, col-start). finding-line-num is 1-indexed and used only for log
   messages. Returns [new-lines changed?]."
  [lines line-idx col-start ns-name log file-url finding-line-num]
  (let [line (nth lines line-idx)
        idx  (find-ns-on-line line ns-name col-start)]
    (if (nil? idx)
      (do (swap! log conj (str "  " file-url ":" finding-line-num "  skip: can't find [" ns-name))
          [lines nil])
      (let [single-end (find-matching-bracket line idx)
            [end-line end-col]
            (if single-end
              [line-idx single-end]
              (find-matching-bracket-across-lines lines line-idx idx))]
        (if (nil? end-line)
          (do (swap! log conj (str "  " file-url ":" finding-line-num "  skip: unmatched bracket for " ns-name))
              [lines nil])
          (let [before       (subs line 0 idx)
                after        (subs (nth lines end-line) (inc end-col))
                remove-span  (fn [ls]
                               (vec (concat (take line-idx ls)
                                            (drop (inc end-line) ls))))
                replace-span (fn [ls new-line]
                               (vec (concat (take line-idx ls)
                                            [new-line]
                                            (drop (inc end-line) ls))))]
            (cond
              ;; Entry is alone on its line(s) — remove the span.
              ;; A trailing inline comment counts as nothing and goes with the entry.
              ;; After removal, straggling close-parens on the new line-idx position
              ;; are pulled up to the nearest preceding ]-ending line.
              (and (re-find #"^\s*$" before)
                   (re-find #"^\s*,?\s*(?:;.*)?$" after))
              (let [removed   (remove-span lines)
                    straggler (when (< line-idx (count removed)) (nth removed line-idx))
                    cleaned
                    (if (and straggler (re-find #"^\s*\)+\s*$" straggler))
                      (let [close-str  (str/trim straggler)
                            attach-idx (find-prev-bracket-close removed line-idx)]
                        (if attach-idx
                          (vec (concat (take attach-idx removed)
                                       [(str (nth removed attach-idx) close-str)]
                                       (subvec removed (inc attach-idx) line-idx)
                                       (drop (inc line-idx) removed)))
                          removed))
                      removed)]
                (swap! log conj (str "  " file-url ":" finding-line-num "  remove require: " ns-name))
                [cleaned true])

              ;; Last entry — closing paren follows immediately after ].
              (re-find #"^\s*\)" after)
              (let [prev-idx (dec line-idx)
                    strip-orphan-comments
                    (fn [ls from to]
                      (mapv (fn [l] (if (re-find #"^\s+;" l) (str/triml l) l))
                            (subvec ls from to)))]
                (if (and (>= prev-idx 0)
                         (re-find #"^\s*$" before)
                         (re-find #"^\s*\[" (nth lines prev-idx)))
                  ;; Sub-branch A: prev line starts with [ — merge ) there.
                  (let [prev-line (nth lines prev-idx)]
                    (swap! log conj (str "  " file-url ":" finding-line-num "  remove require: " ns-name " (last entry)"))
                    [(-> (remove-span lines)
                         (assoc prev-idx (str prev-line after)))
                     true])
                  ;; Sub-branch B: scan backward for nearest ]-ending line.
                  (if (and (re-find #"^\s*$" before)
                           (re-find #"^\s*\)+$" (str/trim after)))
                    (let [close-str  (str/trim after)
                          attach-idx (find-prev-bracket-close lines line-idx)]
                      (if attach-idx
                        (let [new-attach   (str (nth lines attach-idx) close-str)
                              orphan-lines (strip-orphan-comments lines (inc attach-idx) line-idx)]
                          (swap! log conj (str "  " file-url ":" finding-line-num "  remove require: " ns-name " (last entry)"))
                          [(vec (concat (take attach-idx lines)
                                        [new-attach]
                                        orphan-lines
                                        (drop (inc end-line) lines)))
                           true])
                        (let [new-line (str before after)]
                          (swap! log conj (str "  " file-url ":" finding-line-num "  remove require: " ns-name " (last entry)"))
                          [(replace-span lines new-line) true])))
                    (let [new-line (str before after)]
                      (swap! log conj (str "  " file-url ":" finding-line-num "  remove require: " ns-name " (last entry)"))
                      [(replace-span lines new-line) true]))))

              :else
              (let [new-line (str before after)]
                (swap! log conj (str "  " file-url ":" finding-line-num "  remove require: " ns-name))
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

                  ;; Inline empty clause on (ns ...) line — pull the next entry up.
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
                  [(replace-span lines new-line) true])))))))))

(defn remove-require-finding
  "Adapter: extract ns-name and position from a finding, then delegate to
   remove-require-entry. Returns [new-lines changed?]."
  [lines finding file-url log]
  (let [line-idx  (dec (:line finding))
        ;; :col is 1-indexed and points to the first char of the ns name.
        ;; The [ preceding it is at col-2 (0-indexed), so start the bracket
        ;; search from there. This ensures that for duplicate-require we skip
        ;; the first (non-duplicate) occurrence and land on the right entry.
        col-start (max 0 (- (:col finding) 2))
        ns-name   (extract-ns-name (:message finding))]
    (if (or (nil? ns-name) (< line-idx 0) (>= line-idx (count lines)))
      [lines nil]
      (remove-require-entry lines line-idx col-start ns-name log file-url (:line finding)))))

(defn cleanup-empty-clauses [lines]
  (loop [i 0, lines lines]
    (if (>= i (count lines))
      lines
      (let [line (nth lines i)]
        (cond
          ;; Inline empty clause on the (ns ...) line: (ns foo (:require )) → (ns foo)
          (re-find #"^\(ns\b.*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*\)" line)
          (recur i (assoc lines i
                          (str/replace line
                                       #"\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*\)"
                                       "")))

          ;; Dangling close-paren after (ns ...) on its own line: (ns foo\n   ) → (ns foo)
          (and (re-find #"^\(ns\b" line)
               (not (str/includes? line ")"))
               (< (inc i) (count lines))
               (re-find #"^\s*\)+\s*$" (nth lines (inc i))))
          (let [parens (re-find #"\)+" (nth lines (inc i)))]
            (recur i (vec (concat (take i lines)
                                  [(str line parens)]
                                  (drop (+ i 2) lines)))))

          ;; Empty ns-form clause on one line: (:require ) or (:require )) etc.
          (re-find #"^\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*\)+\s*$" line)
          (let [extras (let [[_ ps] (re-find #"^\s*\(\s*:(?:require|import|use|refer|refer-clojure|load|gen-class)\s*(\)+)\s*$" line)]
                         (subs ps 1))]
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
                reduced   (str/replace-first next-line #"\)" "")]
            (recur (max 0 (dec i)) (vec (concat (take i lines)
                                                [reduced]
                                                (drop (+ i 2) lines)))))

          (re-find #":refer\s*\[\s*\]" line)
          (let [cleaned (str/replace line #"\s*:refer\s*\[\s*\]" "")]
            (recur (inc i) (assoc lines i cleaned)))

          (re-find #":exclude\s*\[\s*\]" line)
          (let [cleaned (str/replace line #"\s*:exclude\s*\[\s*\]" "")]
            (recur i (assoc lines i cleaned)))

          :else
          (recur (inc i) lines))))))

(defn remove-bare-requires
  "Remove bare [ns-name] require entries (no :as, no :refer) for each ns-name.
   Delegates to remove-require-entry so all edge-case handling is shared."
  [lines ns-names file-url log]
  (reduce (fn [current-lines ns-name]
            (let [pat (re-pattern (str "\\[" (java.util.regex.Pattern/quote ns-name) "\\s*\\]"))
                  hit (first (keep-indexed #(when (re-find pat %2) %1) current-lines))]
              (if (nil? hit)
                current-lines
                (let [[new-lines changed] (remove-require-entry current-lines hit 0 ns-name log file-url (inc hit))]
                  (if changed new-lines current-lines)))))
          lines
          ns-names))
