(ns clj-kondo-fix.core-test
  (:require [clojure.test :as t :refer [deftest is testing run-tests]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clj-kondo.core :as kondo]
            [clj-kondo-fix.core :as fix]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.impl.utils :refer [read-lines find-matching-bracket-across-lines]]))

;; ============================================================
;; Helpers
;; ============================================================

(defn fixture-path
  "Returns the absolute path for a fixture file.
   rule  — clj-kondo rule name (e.g. \"unused-namespace\")
   slug  — file slug without extension (e.g. \"removes-single-in\")"
  [rule slug]
  (str (System/getProperty "user.dir")
       "/test/clj_kondo_fix/fixtures/" rule "/" slug ".clj"))

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
                            :invalid-arity              {:level :off}
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
                            :unused-value               {:level :off}
                            :not-a-function             {:level :off}
                            :type-mismatch              {:level :off}
                            :shadowed-var               {:level :off}
                            :shadowed-fn-param          {:level :off}
                            :loop-without-recur         {:level :off}
                            :uninitialized-var          {:level :off}
                            :inline-def                 {:level :off}
                            :cond-else                  {:level :off}
                            :condition-always-true      {:level :off}
                            :earmuffed-var-not-dynamic  {:level :off}
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

(defn apply-fix
  "Apply fix-fn to file-path purely in memory.
   Returns {:fixed N :content string}.  Never writes to disk."
  [fix-fn file-path findings]
  (let [lines  (read-lines file-path)
        log    (atom [])
        result (fix-fn file-path lines findings log)]
    {:fixed (:fixed result) :content (str (str/join "\n" (:lines result)) "\n")}))

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

;; ============================================================
;; :unused-namespace
;; ============================================================

(deftest test-unused-namespace
  (testing "removes unused namespace from require"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "removes-single-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "removes-single-out")) (:content result)))))

  (testing "all requires removed — (:require) block is removed and ns closes cleanly"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "all-requires-removed-in")
                             [:unused-namespace] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "all-requires-removed-out")) (:content result)))))

  (testing "leaves used namespace untouched"
    (let [result (assert-no-finding fixes/fix-unused-ns-in-file
                                    (fixture-path "unused-namespace" "used-ns")
                                    [:unused-namespace])]
      (is (zero? (:fixed result)))))

  (testing "removes both of two unused namespaces on same line"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "removes-two-same-line-in")
                             [:unused-namespace] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "removes-two-same-line-out")) (:content result)))))

  (testing "keyword lookups like (:count) in threading macros are not touched"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "keyword-lookup-regression-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "keyword-lookup-regression-out")) (:content result)))))

  (testing "trailing comment on the removed entry line — no corruption"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "trailing-comment-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "trailing-comment-out")) (:content result)))))

  (testing "comment-only line before removed entry stays as orphan — no corruption"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "orphan-comment-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "orphan-comment-out")) (:content result)))))

  (testing "trailing comment on kept entry line is preserved"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "trailing-comment-stay-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "trailing-comment-stay-out")) (:content result)))))

  (testing "inline single-line ns: entry removed, (:require ) straggler stays — no corruption"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "inline-ns-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "inline-ns-out")) (:content result)))))

  (testing "last entry removed when prev line is (:require — closing )) merged onto preceding ]"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "last-entry-prev-require-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "last-entry-prev-require-out")) (:content result)))))

  (testing "last entry removed when prev entry is multi-line — closing ) merged onto :as line"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "last-entry-prev-multiline-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "last-entry-prev-multiline-out")) (:content result)))))

  (testing "multi-line entry removed: middle entry between two single-line entries"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "multiline-middle-entry-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "multiline-middle-entry-out")) (:content result)))))

  (testing "multi-line entry removed: last entry — closing ) merged onto previous ]"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "multiline-last-entry-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "multiline-last-entry-out")) (:content result)))))

  (testing "multi-line entry removed: only entry in require clause"
    (let [result (assert-fix fixes/fix-unused-ns-in-file
                             (fixture-path "unused-namespace" "multiline-only-entry-in")
                             [:unused-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-namespace" "multiline-only-entry-out")) (:content result))))))

;; ============================================================
;; :duplicate-require
;; ============================================================

(deftest test-duplicate-require
  (testing "case 1: only first alias used — remove reported duplicate, no renames"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "first-alias-used-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "first-alias-used-out")) (:content result)))))

  (testing "case 2: only duplicate alias used — remove first entry, no renames"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "second-alias-used-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "second-alias-used-out")) (:content result)))))

  (testing "case 3: both aliases used — keep longer, rename shorter usages, remove shorter entry"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "both-aliases-used-keep-longer-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "both-aliases-used-keep-longer-out")) (:content result)))))

  (testing "case 3 tie: both aliases same length — keep first (shorter or equal wins)"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "both-aliases-used-tie-keep-first-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "both-aliases-used-tie-keep-first-out")) (:content result)))))

  (testing "case 4: neither alias used — remove reported duplicate, first entry remains"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "neither-alias-used-in")
                             [:duplicate-require] 1)]
      (is (pos? (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "neither-alias-used-out")) (:content result))))))

;; ============================================================
;; :unused-binding
;; ============================================================

(deftest test-unused-binding
  (testing "prefixes simple unused fn param with underscore"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "simple-fn-param-in")
                             [:unused-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "simple-fn-param-out")) (:content result)))))

  (testing "let binding is skipped by default — too risky (may be side-effectful)"
    (assert-skip fixes/fix-unused-binding-in-file
                 (fixture-path "unused-binding" "let-binding-skip")
                 [:unused-binding]))

  (testing "no change when binding is used"
    (let [result (assert-no-finding fixes/fix-unused-binding-in-file
                                    (fixture-path "unused-binding" "binding-used")
                                    [:unused-binding])]
      (is (zero? (:fixed result)))))

  (testing "removes unused namespaced key from :keys vector"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "namespaced-key-in")
                             [:unused-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "namespaced-key-out")) (:content result)))))

  (testing ":as bindings are not reported by :unused-binding"
    (let [result (assert-no-finding fixes/fix-unused-binding-in-file
                                    (fixture-path "unused-binding" "as-alias-no-finding")
                                    [:unused-binding])]
      (is (zero? (:fixed result)))))

  (testing "loop/for bindings are skipped by default"
    (assert-skip fixes/fix-unused-binding-in-file
                 (fixture-path "unused-binding" "loop-binding-skip")
                 [:unused-binding]))

  (testing ":as clause in destructuring: removed when unused"
    (let [pred   #(str/includes? (:message %) "config")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "as-clause-removed-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "as-clause-removed-out")) (:content result)))))

  (testing ":as clause: all concrete bindings unused → map collapses to _as-name"
    (let [pred   #(str/includes? (:message %) " db")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "as-clause-collapses-to-name-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "as-clause-collapses-to-name-out")) (:content result)))))

  (testing "map inside function call (let rhs) is NOT collapsed — not in destructuring position"
    (let [pred   #(str/includes? (:message %) " query")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "fn-call-arg-not-collapsed-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "fn-call-arg-not-collapsed-out")) (:content result)))))

  (testing ":as and concrete binding both unused → :as removed, map collapses to _"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "as-and-binding-both-unused-in")
                             [:unused-binding] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "as-and-binding-both-unused-out")) (:content result)))))

  (testing "multi-line: :as removed, map collapses to _"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "multiline-as-and-binding-both-unused-in")
                             [:unused-binding] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "multiline-as-and-binding-both-unused-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes unused key from :keys vector"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-first-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-first-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes first key, rest preserved"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-first-in")
                             [:unused-binding] 1 pred)]
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-first-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes middle key, space preserved"
    (let [pred   #(str/includes? (:message %) " y")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-middle-in")
                             [:unused-binding] 1 pred)]
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-middle-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes last key, preceding preserved"
    (let [pred   #(str/includes? (:message %) " z")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-last-in")
                             [:unused-binding] 1 pred)]
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-last-out")) (:content result)))))

  (testing "keys-destr in fn-param: only key removed, entire map collapses to _"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-only-key-collapses-in")
                             [:unused-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-only-key-collapses-out")) (:content result)))))

  (testing "keys-destr in let: unused key removed — safe, just a deref on existing var"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-let-safe-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-let-safe-out")) (:content result)))))

  (testing "let scalar binding is still skipped — may be side-effectful"
    (assert-skip fixes/fix-unused-binding-in-file
                 (fixture-path "unused-binding" "let-binding-skip")
                 [:unused-binding]))

  (testing "keys-destr multi-line: unused key is only item on its line — line removed"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-first-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-first-key-out")) (:content result)))))

  (testing "keys-destr multi-line: unused key is middle item on its own line — line removed"
    (let [pred   #(str/includes? (:message %) " y")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-middle-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-middle-key-out")) (:content result)))))

  (testing "keys-destr multi-line: unused key is last item on its own line — line removed"
    (let [pred   #(str/includes? (:message %) " z")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-last-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-last-key-out")) (:content result)))))

  (testing "keys-destr multi-line: unused key shares line with other keys — others preserved"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-shared-line-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-shared-line-out")) (:content result)))))

  (testing "keys-destr multi-line: first key was only thing after {:keys [ — pull next key up"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-first-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-first-key-out")) (:content result))))))

;; ============================================================
;; :unused-import
;; ============================================================

(deftest test-unused-import
  (testing "removes one unused import from group, leaves the other"
    (let [pred   #(str/ends-with? (:message %) "List")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-one-from-group-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-one-from-group-out")) (:content result)))))

  (testing "removes all unused imports from group"
    (let [result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-all-from-group-in")
                             [:unused-import] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-all-from-group-out")) (:content result)))))

  (testing "removes unused import from vector-style standalone import"
    (let [pred   #(str/ends-with? (:message %) "Foo")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-from-standalone-vector-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-from-standalone-vector-out")) (:content result)))))

  (testing "removes entire import group when last class removed — no bare [package] left"
    (let [result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-last-class-removes-group-in")
                             [:unused-import] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-last-class-removes-group-out")) (:content result)))))

  (testing "removes middle unused import — first and last preserved with correct spacing"
    (let [pred   #(str/ends-with? (:message %) "Instant")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-middle-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-middle-out")) (:content result)))))

  (testing "removes first unused import from group — rest preserved"
    (let [pred   #(str/ends-with? (:message %) "Date")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-first-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-first-out")) (:content result)))))

  (testing "removes last unused import from group — preceding preserved"
    (let [pred   #(str/ends-with? (:message %) "List")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-last-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-last-out")) (:content result))))))

;; ============================================================
;; :unused-referred-var
;; ============================================================

(deftest test-unused-referred-var
  (testing "removes single unused referred var, keeps used one"
    (let [pred   #(str/includes? (:message %) "ends-with?")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-one-keeps-other-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-one-keeps-other-out")) (:content result)))))

  (testing "works with vars whose names end in ? (word boundary)"
    (let [pred   #(str/includes? (:message %) "ends-with?")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "predicate-var-name-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "predicate-var-name-out")) (:content result)))))

  (testing "removes :refer clause when all vars removed"
    (let [result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-in")
                             [:unused-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-out")) (:content result)))))

  (testing "removes :refer clause when all vars removed — multi-require ns"
    (let [result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-multi-in")
                             [:unused-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-multi-out")) (:content result)))))

  (testing "removes entire require entry when only referred var is removed"
    (let [result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-entire-entry-when-only-referred-in")
                             [:unused-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-entire-entry-when-only-referred-out")) (:content result)))))

  (testing "space preserved when removing middle var from :refer vector"
    (let [pred   #(str/includes? (:message %) "run-cucumber")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-middle-var-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-middle-var-out")) (:content result)))))

  (testing "space preserved when removing first var from :refer vector"
    (let [pred   #(str/includes? (:message %) "clojure.string/join")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-first-var-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-first-var-out")) (:content result)))))

  (testing "space preserved when removing last var from :refer vector"
    (let [pred   #(str/includes? (:message %) "clojure.string/split")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-last-var-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-last-var-out")) (:content result)))))

  (testing "multi-line :refer vector: removes var from its own line"
    (let [pred   #(str/includes? (:message %) "ends-with?")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "multiline-refer-vector-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "multiline-refer-vector-out")) (:content result))))))

;; ============================================================
;; :misplaced-docstring
;; ============================================================

(deftest test-misplaced-docstring
  (testing "moves docstring before param vector (multi-line form)"
    (let [result (assert-fix fixes/fix-misplaced-docstring-in-file
                             (fixture-path "misplaced-docstring" "moves-before-params-in")
                             [:misplaced-docstring] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "misplaced-docstring" "moves-before-params-out")) (:content result)))))

  (testing "single-line form is skipped — finding is on row 1, def-line-idx = -1"
    (let [result (assert-skip fixes/fix-misplaced-docstring-in-file
                              (fixture-path "misplaced-docstring" "single-line-skip")
                              [:misplaced-docstring])]
      (is (str/includes? (:content result) "(defn f [x] \"dude\" x)"))))

  (testing "correctly placed docstring is unchanged"
    (let [result (assert-no-finding fixes/fix-misplaced-docstring-in-file
                                    (fixture-path "misplaced-docstring" "correctly-placed")
                                    [:misplaced-docstring])]
      (is (zero? (:fixed result)))))

  (testing "comment between params and docstring — safe skip"
    (let [result (assert-skip fixes/fix-misplaced-docstring-in-file
                              (fixture-path "misplaced-docstring" "comment-between-skip")
                              [:misplaced-docstring])]
      (is (str/includes? (:content result) ";; explains x"))
      (is (str/includes? (:content result) "\"doc\""))))

  (testing "multi-line defn signature: params on separate line — safe skip"
    (let [result (assert-skip fixes/fix-misplaced-docstring-in-file
                              (fixture-path "misplaced-docstring" "multiline-sig-skip")
                              [:misplaced-docstring])]
      (is (str/includes? (:content result) "(defn f"))
      (is (str/includes? (:content result) "  [x]"))
      (is (str/includes? (:content result) "\"doc\"")))))

;; ============================================================
;; :missing-else-branch
;; ============================================================

(deftest test-missing-else-branch
  (testing "converts (if ...) to (when ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-out")) (:content result)))))

  (testing "converts (if-not ...) to (when-not ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-not-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-not-out")) (:content result)))))

  (testing "converts (if-let ...) to (when-let ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-let-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-let-out")) (:content result)))))

  (testing "converts (if-some ...) to (when-some ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-some-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-some-out")) (:content result)))))

  (testing "multiple if variants on same line all converted"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-multiple-variants-in")
                             [:missing-else-branch] 4)]
      (is (= 4 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-multiple-variants-out")) (:content result)))))

  (testing "no change when else branch is present"
    (let [result (assert-no-finding fixes/fix-missing-else-branch-in-file
                                    (fixture-path "missing-else-branch" "else-branch-present")
                                    [:missing-else-branch])]
      (is (zero? (:fixed result))))))

;; ============================================================
;; :unused-private-var
;; ============================================================

(deftest test-unused-private-var
  (testing "removes defn- form entirely"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-defn-form-in")
                             [:unused-private-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-defn-form-out")) (:content result)))))

  (testing "removes def ^:private form entirely"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-def-private-form-in")
                             [:unused-private-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-def-private-form-out")) (:content result)))))

  (testing "removes multi-line def ^:private form"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-multiline-def-private-in")
                             [:unused-private-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-multiline-def-private-out")) (:content result)))))

  (testing "removes two independent private vars"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-two-private-vars-in")
                             [:unused-private-var] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-two-private-vars-out")) (:content result))))))

;; ============================================================
;; :redundant-do
;; ============================================================

(deftest test-redundant-do
  (testing "removes redundant do wrapper (single-line)"
    (let [result (assert-fix fixes/fix-redundant-do-in-file
                             (fixture-path "redundant-do" "single-line-in")
                             [:redundant-do] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-do" "single-line-out")) (:content result)))))

  (testing "removes redundant do wrapper (multi-line)"
    (let [result (assert-fix fixes/fix-redundant-do-in-file
                             (fixture-path "redundant-do" "multi-line-in")
                             [:redundant-do] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-do" "multi-line-out")) (:content result))))))

;; ============================================================
;; :redundant-let
;; ============================================================

(deftest test-redundant-let
  (testing "single-line: no body"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "single-line-no-body-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "single-line-no-body-out")) (:content result)))))

  (testing "single-line: with body"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "single-line-with-body-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "single-line-with-body-out")) (:content result)))))

  (testing "multi-line: no body"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-no-body-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-no-body-out")) (:content result)))))

  (testing "multi-line: with body on its own line"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-body-own-line-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-body-own-line-out")) (:content result)))))

  (testing "multi-line: body inline with inner binding close"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-body-inline-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-body-inline-out")) (:content result)))))

  (testing "multi-line: multiple inner bindings"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-multiple-inner-bindings-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-multiple-inner-bindings-out")) (:content result)))))

  (testing "intermediate #_ discard form: moved before merged let"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "intermediate-discard-form-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (let [lines       (str/split-lines (:content result))
            discard-idx (first (keep-indexed #(when (str/includes? %2 "#_") %1) lines))
            let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
        (is (some? discard-idx))
        (is (some? let-idx))
        (is (< discard-idx let-idx)))))

  (testing "intermediate comment line: moved before merged let"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "intermediate-comment-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (let [lines       (str/split-lines (:content result))
            comment-idx (first (keep-indexed #(when (str/includes? %2 ";;") %1) lines))
            let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
        (is (some? comment-idx))
        (is (some? let-idx))
        (is (< comment-idx let-idx)))
      (is (str/includes? (:content result) "body"))))

  (testing "skip: outer let with multi-line binding vector"
    (let [result (assert-skip fixes/fix-redundant-let-in-file
                              (fixture-path "redundant-let" "skip-multiline-outer-binding")
                              [:redundant-let])]
      (is (zero? (:fixed result))))))

;; ============================================================
;; Integration tests — full pipeline
;; ============================================================

(deftest test-full-pipeline-dry-run
  (testing "dry-run does not write files"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [original (slurp f)
              result   (fix/fix! {:lint [f] :dry-run true})]
          (is (pos? (-> result :summary :total-fixed)))
          (is (= original (slurp f))))))))

(deftest test-full-pipeline-fix
  (testing "fix mode writes changes to disk"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [result (fix/fix! {:lint [f] :dry-run false})]
          (is (pos? (-> result :summary :total-fixed)))
          (is (not (str/includes? (slurp f) "clojure.string"))))))))

;; ============================================================
;; Utility tests
;; ============================================================

(deftest test-find-matching-bracket
  (testing "finds matching paren across lines"
    (is (= [3 0] (find-matching-bracket-across-lines ["(foo" "  (bar" "    baz)" ")"] 0 0))))
  (testing "handles paren depth correctly"
    (is (= [1 3] (find-matching-bracket-across-lines ["(let [x (foo bar)]" "  x)"] 0 0))))
  (testing "skips brackets inside strings"
    (is (= [1 3] (find-matching-bracket-across-lines ["(let [x \"hello [world]\"]" "  x)"] 0 0))))
  (testing "returns nil for non-bracket start"
    (is (nil? (find-matching-bracket-across-lines ["foo bar"] 0 0)))))

(defn -main [& _args]
  (let [result (run-tests 'clj-kondo-fix.core-test)]
    (System/exit (if (t/successful? result) 0 1))))
