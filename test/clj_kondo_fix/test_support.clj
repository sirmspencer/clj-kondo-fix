(ns clj-kondo-fix.test-support
  (:require [clojure.test :refer [is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-kondo.core :as kondo]
            [clj-kondo-fix.impl.utils :refer [read-lines find-matching-bracket-across-lines]]))

;; ============================================================
;; Helpers
;; ============================================================

(defn fixture-path
  "Returns the absolute path for a fixture file.
   rule  — clj-kondo rule name, hyphenated (e.g. \"unused-namespace\")
   slug  — file slug without extension (e.g. \"removes-single-in\")
   Fixtures live colocated with their test file under
   test/clj_kondo_fix/rules/<rule_underscored>/<slug>.clj"
  [rule slug]
  (str (System/getProperty "user.dir")
       "/test/clj_kondo_fix/rules/" (str/replace rule "-" "_") "/" slug ".clj"))

(defn with-temp-file [content f]
  (let [fpath (str (System/getProperty "java.io.tmpdir")
                   "/clj-kondo-fix-test-" (java.util.UUID/randomUUID) ".clj")]
    (spit fpath content)
    (try (f fpath) (finally (io/delete-file fpath true)))))

(defn lint-file
  "Run clj-kondo on file-path with the given linters enabled (all others off).
   Returns findings normalized to {:line :col :message}."
  [file-path & {:keys [linters]}]
  (let [all-off {:linters {:namespace-name-mismatch    {:level :off}
                            :syntax                     {:level :off}
                            :datalog-syntax             {:level :off}
                             :unresolved-symbol          {:level :off}
                            :unresolved-var             {:level :off}
                            :unresolved-namespace       {:level :off}
                            :unresolved-excluded-var    {:level :off}
                            :unresolved-protocol-method {:level :off}
                            :unused-binding             {:level :off}
                            :unused-referred-var        {:level :off}
                            :unused-namespace           {:level :off}
                            :unused-private-var         {:level :off}
                            :unused-import              {:level :off}
                            :duplicate-require          {:level :off}
                            :redundant-do               {:level :off}
                            :redundant-let              {:level :off}
                            :redundant-let-binding      {:level :off}
                            :redundant-expression       {:level :off}
                            :redundant-call             {:level :off}
                            :redundant-declare          {:level :off}
                            :redundant-fn-wrapper       {:level :off}
                            :redundant-nested-call      {:level :off}
                            :refer-all                  {:level :off}
                            :misplaced-docstring        {:level :off}
                            :missing-docstring          {:level :off}
                            :missing-else-branch        {:level :off}
                             :missing-body-in-when       {:level :off}
                             :missing-test-assertion     {:level :off}
                             :redundant-str-call         {:level :off}
                             :redundant-format           {:level :off}
                             :unused-value               {:level :off}
                            :not-a-function             {:level :off}
                             :shadowed-var               {:level :off}
                            :shadowed-fn-param          {:level :off}
                            :loop-without-recur         {:level :off}
                            :uninitialized-var          {:level :off}
                            :inline-def                 {:level :off}
                            :cond-else                  {:level :off}
                             :condition-always-true      {:level :off}
                             :docstring-leading-trailing-whitespace {:level :off}
                             :earmuffed-var-not-dynamic  {:level :off}
                             :keyword-binding            {:level :off}
                             :not-nil?                   {:level :off}
                             :redundant-primitive-coercion {:level :off}
                            :unknown-require-option     {:level :off}
                            :invalid-ref                {:level :off}}}
        enabled {:linters (into {:namespace-name-mismatch {:level :off}}
                                (map (fn [k] [k {:level :warning}]) linters))}
        config  {:linters (merge (:linters all-off) (:linters enabled))}
        result  (kondo/run! {:lint [file-path] :config config})]
    (vec (for [f (:findings result)]
           {:line    (:row f)
            :col     (:col f)
            :message (:message f)}))))

(defn strip-doc-tag
  "Remove a leading ;;-; ... ;-;; documentation line from content if present.
   These lines appear only in -in.clj fixture files and are not present in
   -out.clj files, so they must be stripped before exact-match comparison."
  [content]
  (str/replace content #"\A;;-;.*?;-;;\n" ""))

(defn apply-fix
  "Apply fix-fn to file-path purely in memory.
   Returns {:fixed N :content string}.  Never writes to disk.
   Strips any leading ;;-; ... ;-;; doc-tag block from the output content
   so that exact comparison against -out.clj files (which have no tag) works."
  [fix-fn file-path findings]
  (let [lines  (read-lines file-path)
        log    (atom [])
        result (fix-fn file-path lines findings log)]
    {:fixed (:fixed result) :content (strip-doc-tag (str (str/join "\n" (:lines result)) "\n"))}))

(defn assert-fix
  "Assert: expected-count matching findings exist before fix; none after.
   Writes fixed content to a temp file internally for the after-lint check —
   the input fixture file is never modified.
   Optional filter-fn narrows which findings count (for partial-fix cases).
   Returns the apply-fix result map for additional content assertions."
  ([fix-fn file-path linters expected-count]
   (assert-fix fix-fn file-path linters expected-count nil))
  ([fix-fn file-path linters expected-count filter-fn]
   (let [all-before (lint-file file-path :linters linters)
         before     (if filter-fn (filter filter-fn all-before) all-before)]
     (is (= expected-count (count before))
         (str "expected " expected-count " finding(s) before fix, got " (count before)))
     (let [result   (apply-fix fix-fn file-path before)
           tmp-path (str (System/getProperty "java.io.tmpdir")
                         "/clj-kondo-fix-verify-" (java.util.UUID/randomUUID) ".clj")]
       (spit tmp-path (:content result))
       (try
         (let [all-after (lint-file tmp-path :linters linters)
               after     (if filter-fn (filter filter-fn all-after) all-after)]
           (is (empty? after) "expected no matching findings after fix")
           result)
         (finally (io/delete-file tmp-path true)))))))

(defn assert-skip
  "Assert: linter fires before; fix makes no changes; linter still fires after.
   Returns the apply-fix result map."
  [fix-fn file-path linters]
  (let [before (lint-file file-path :linters linters)]
    (is (pos? (count before)) "expected linter to fire before skip")
    (let [result (apply-fix fix-fn file-path before)]
      (is (zero? (:fixed result)) "expected no changes (deliberate skip)")
      (is (pos? (count (lint-file file-path :linters linters)))
          "expected linter to still fire after skip")
      result)))

(defn assert-no-finding
  "Assert: linter does not fire — input is already correct.
   Returns the apply-fix result map."
  [fix-fn file-path linters]
  (let [before (lint-file file-path :linters linters)]
    (is (empty? before) "expected no findings for already-correct code")
    (apply-fix fix-fn file-path before)))
