(ns clj-kondo-fix.impl.fixes.unused-binding
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines
                                              find-opening-bracket
                                              enclosing-bracket-type
                                              word-end-pos
                                              find-binding-on-line
                                              remove-token-span
                                              remove-referred-var-from-line]]))

;; ------------------------------------------------------------
;; Message parsing
;; ------------------------------------------------------------

(def ^:private unused-binding-re #"^unused binding (.+)$")

(defn- extract-binding-name [msg]
  (some-> (re-find unused-binding-re msg) second))

;; ------------------------------------------------------------
;; Binding context detection
;; ------------------------------------------------------------

(def ^:private let-like-forms
  #{"let" "loop" "for" "doseq" "binding" "with-open"
    "if-let" "when-let" "if-some" "when-some"
    "with-local-vars" "letfn" "when-first" "dotimes"
    "with-bindings"})

(def ^:private fn-like-forms
  #{"defn" "defn-" "fn" "fn*" "defmethod" "defmacro"
    "defmulti" "reify" "proxy"})

(defn- classify-bracket-context [text-before-bracket]
  (let [words  (re-seq #"[a-zA-Z*!?][a-zA-Z0-9*!?-]*" text-before-bracket)
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
              context-text (if (str/blank? brk-text)
                             (loop [i (dec brk-line)]
                               (if (< i 0) ""
                                   (let [lt (str/trim (nth lines i))]
                                     (if (str/blank? lt) (recur (dec i)) lt))))
                             brk-text)
              inner-ctx    (classify-bracket-context context-text)]
          (if (= inner-ctx :keys-destr)
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
              :keys-destr-fn)
            inner-ctx))
        :let-binding))))

;; ------------------------------------------------------------
;; Destructuring map collapse helpers
;; ------------------------------------------------------------

(defn- map-collapses-to
  "Given the content between { and }, decide if the map can collapse to a
   simpler form. Returns the target string or nil."
  [content]
  (let [as-name  (second (re-find #":as\s+([a-zA-Z_][a-zA-Z0-9*!?_-]*)" content))
        no-as    (if as-name
                   (str/replace content
                                (re-pattern (str ":as\\s+" (java.util.regex.Pattern/quote as-name)))
                                "")
                   content)
        no-empty  (str/replace no-as #":[\w./]+\s*\[\s*\]" "")
        no-empty2 (str/replace no-empty #":[\w./]+\s*\{\s*\}" "")
        remaining (str/trim no-empty2)]
    (cond
      (and (str/blank? remaining)
           (or as-name
               (re-find #":(?:keys|strs|syms)!?\s*\[" content)
               (re-find #"_[a-zA-Z]" content)))
      (or as-name "_")

      (re-matches #"(?:\s*_[a-zA-Z][a-zA-Z0-9*!?-]*\s+:[\w./]+\s*)+" remaining)
      (or as-name "_")

      :else nil)))

(defn- binding-bracket?
  "Returns true if the [ at (bracket-line, bracket-col) belongs to a known
   binding form (let/defn/fn/for/etc. or :keys/:strs/:syms)."
  [lines bracket-line bracket-col]
  (let [brk-text  (subs (nth lines bracket-line) 0 bracket-col)
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

(defn collapse-destr-maps
  "Post-pass: replace destructuring maps whose bindings are all effectively
   unused with either their :as name or plain `_`. Only collapses maps in
   destructuring position — never maps that are arguments to function calls."
  [lines]
  (loop [i 0, current-lines (vec lines)]
    (if (>= i (count current-lines))
      current-lines
      (let [line (nth current-lines i)]
        (if-let [j (some (fn [j] (when (= \{ (nth line j)) j))
                         (range (count line)))]
          (if-let [[cl cc] (find-matching-bracket-across-lines current-lines i j)]
            (let [enc-ch        (enclosing-bracket-type current-lines i j)
                  in-destr-pos? (and (= enc-ch \[)
                                     (if-let [{bl :line bc :col}
                                              (find-opening-bracket current-lines i j)]
                                       (binding-bracket? current-lines bl bc)
                                       false))
                  content (if (= i cl)
                            (subs line (inc j) cc)
                            (str (subs line (inc j))
                                 (str/join "\n" (map #(nth current-lines %) (range (inc i) cl)))
                                 (subs (nth current-lines cl) 0 cc)))
                  target (when in-destr-pos? (map-collapses-to content))]
              (if target
                (let [before    (subs line 0 j)
                      after-cc  (subs (nth current-lines cl) (inc cc))
                      new-line  (str before target after-cc)
                      new-lines (vec (concat (take i current-lines)
                                             [new-line]
                                             (drop (inc cl) current-lines)))]
                  (recur i new-lines))
                (recur (inc i) current-lines)))
            (recur (inc i) current-lines))
          (recur (inc i) current-lines))))))

;; ------------------------------------------------------------
;; Remove / rename helpers
;; ------------------------------------------------------------

(defn remove-as-clause-from-line [line word-end]
  (if-let [m (re-find #"[\s,]:as\s+[\w-]+$" (subs line 0 word-end))]
    (let [match-str    (if (string? m) m (first m))
          clause-start (- word-end (count match-str))]
      (str (subs line 0 clause-start) (subs line word-end)))
    line))

(defn- remove-key-from-destr
  "Remove a key from a :keys/:strs/:syms destructuring vector.
   Returns [new-lines changed?]."
  [current-lines line-idx line effective-idx idx binding-name word-end log fu finding-line-num]
  (let [new-line  (if (= effective-idx idx)
                    (remove-referred-var-from-line line binding-name idx)
                    (remove-token-span line effective-idx word-end))
        new-lines (cond
                    (str/blank? new-line)
                    (vec (concat (take line-idx current-lines)
                                 (drop (inc line-idx) current-lines)))
                    (and (re-find #"^\s*[\]\)}]" new-line)
                         (pos? line-idx))
                    (let [prev (str/trimr (nth current-lines (dec line-idx)))]
                      (vec (concat (take (dec line-idx) current-lines)
                                   [(str prev (str/triml new-line))]
                                   (drop (inc line-idx) current-lines))))
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
      [current-lines nil]
      (do (swap! log conj (str "  " fu ":" finding-line-num "  remove from keys vector: " binding-name))
          [new-lines true]))))

;; ------------------------------------------------------------
;; Public fix function
;; ------------------------------------------------------------

(defn fix-unused-binding-in-file
  "Fix unused bindings. fix-contexts controls which binding types to handle:
     :as-clause      — remove the :as clause when unused
     :fn-param       — prefix unused function params with _
     :keys-destr-fn  — remove from {:keys/strs/syms []} in function params
     :keys-destr-let — remove from {:keys/strs/syms []} in let bindings
     :let-binding    — prefix unused let/loop/for/doseq bindings with _
   Default: #{:as-clause :fn-param :keys-destr-fn :keys-destr-let} (let scalar bindings skipped)"
  ([file-path lines findings log]
   (fix-unused-binding-in-file file-path lines findings log
                               #{:as-clause :fn-param :keys-destr-fn :keys-destr-let}))
  ([file-path lines findings log fix-contexts]
   (let [fu (->display-path file-path)]
     (reduce-findings lines findings
                      (fn [current-lines f]
                        (let [binding-name (extract-binding-name (:message f))
                              line-idx     (dec (:line f))
                              col-idx      (dec (:col f))]
                          (if (or (nil? binding-name) (< line-idx 0) (>= line-idx (count current-lines)))
                            [current-lines nil]
                            (let [line (nth current-lines line-idx)
                                  idx  (find-binding-on-line line binding-name col-idx)]
                              (if (nil? idx)
                                (do (swap! log conj (str "  " fu ":" (:line f) "  skip: can't find binding " binding-name " on line"))
                                    [current-lines nil])
                                (let [word-end (word-end-pos line idx)]
                                  (if (not= (subs line idx word-end) binding-name)
                                    (do (swap! log conj (str "  " fu ":" (:line f) "  skip: binding " binding-name " not found at column " (:col f)))
                                        [current-lines nil])
                                    ;; For namespaced keys e.g. {:keys [patient/id]}, kondo's col
                                    ;; points at the local name (id). Walk backward to include the
                                    ;; full namespace/name token so removal doesn't leave "patient/" stranded.
                                    (let [effective-idx
                                          (if (and (pos? idx) (= \/ (nth line (dec idx))))
                                            (loop [j (- idx 2)]
                                              (if (or (< j 0)
                                                      (not (re-find #"[a-zA-Z0-9_\-.*+?!]" (str (nth line j)))))
                                                (inc j)
                                                (recur (dec j))))
                                            idx)
                                          ctx (detect-binding-context current-lines line-idx effective-idx)]
                                      (cond
                                        (and (= ctx :as-clause) (fix-contexts :as-clause))
                                        (let [new-line (remove-as-clause-from-line line word-end)]
                                          (swap! log conj (str "  " fu ":" (:line f) "  remove unused :as binding: " binding-name))
                                          [(assoc current-lines line-idx new-line) true])

                                        ;; Both :keys-destr-fn and :keys-destr-let use the same removal logic.
                                        (and (#{:keys-destr-fn :keys-destr-let} ctx) (fix-contexts ctx))
                                        (remove-key-from-destr current-lines line-idx line effective-idx idx
                                                               binding-name word-end log fu (:line f))

                                        (and (= ctx :fn-param) (fix-contexts :fn-param))
                                        (let [new-line (str (subs line 0 idx) "_" (subs line idx))]
                                          (swap! log conj (str "  " fu ":" (:line f) "  rename unused binding: " binding-name " -> _" binding-name))
                                          [(assoc current-lines line-idx new-line) true])

                                        (and (= ctx :let-binding) (fix-contexts :let-binding))
                                        (let [new-line (str (subs line 0 idx) "_" (subs line idx))]
                                          (swap! log conj (str "  " fu ":" (:line f) "  rename unused binding: " binding-name " -> _" binding-name))
                                          [(assoc current-lines line-idx new-line) true])

                                        :else
                                        (do (swap! log conj (str "  " fu ":" (:line f) "  skip: binding " binding-name " in context " ctx " not enabled"))
                                            [current-lines nil]))))))))))
                      collapse-destr-maps))))
