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

(defn with-temp-file [content f]
  (let [fpath (str (System/getProperty "java.io.tmpdir")
                   "/clj-kondo-fix-test-" (java.util.UUID/randomUUID) ".clj")]
    (spit fpath content)
    (try (f fpath) (finally (io/delete-file fpath true)))))

(defn lint-file
  "Run clj-kondo on file-path with the given linters enabled (all others off).
   Returns findings normalized to {:line :col :message}."
  [file-path & {:keys [linters]}]
  (let [all-off {:linters {:namespace-name-mismatch {:level :off}
                           :unused-binding {:level :off}
                           :unused-referred-var {:level :off}
                           :refer-all {:level :off}
                           :redundant-do {:level :off}
                           :redundant-let {:level :off}
                           :unused-import {:level :off}
                           :misplaced-docstring {:level :off}
                           :missing-else-branch {:level :off}
                           :unused-private-var {:level :off}
                           :duplicate-require {:level :off}
                           :unused-namespace {:level :off}}}
        enabled {:linters (into {:namespace-name-mismatch {:level :off}}
                                (map (fn [k] [k {:level :warning}]) linters))}
        config  {:linters (merge (:linters all-off) (:linters enabled))}
        result  (kondo/run! {:lint [file-path] :config config})]
    (for [f (:findings result)]
      {:line    (:row f)
       :col     (:col f)
       :message (:message f)})))

(defn apply-fix
  "Apply fix-fn to file-path and return {:fixed N :content string}.
   Writes the result back to the file if changed."
  [fix-fn file-path findings]
  (let [lines  (read-lines file-path)
        log    (atom [])
        result (fix-fn file-path lines findings log)]
    (when (:changed? result)
      (spit file-path (str/join "\n" (:lines result))))
    {:fixed (:fixed result) :content (slurp file-path)}))

;; ============================================================
;; :unused-namespace
;; ============================================================

(deftest test-unused-namespace
  (testing "removes unused namespace from require"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-ns-in-file f
                                (lint-file f :linters [:unused-namespace]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.string")))))))

  (testing "leaves used namespace untouched"
    (with-temp-file "(ns foo (:require [clojure.string :as s])) (s/join [\"\"] \"\")"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-ns-in-file f
                                (lint-file f :linters [:unused-namespace]))]
          (is (zero? (:fixed result)))))))

  (testing "removes one of two unused namespaces, same line"
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.set :as cs]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-ns-in-file f
                                (lint-file f :linters [:unused-namespace]))]
          (is (= 2 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.string")))
          (is (not (str/includes? (:content result) "clojure.set"))))))))

;; ============================================================
;; :duplicate-require
;; ============================================================

(deftest test-duplicate-require
  (testing "removes the duplicate (second) entry, keeps the first"
    ;; Both entries on same line — col points to the second [clojure.string.
    ;; The first entry is actively used via s/join; only the duplicate :as str
    ;; should be removed.
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.string :as str])) (s/join [\"\"] \"\")"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-ns-in-file f
                                (lint-file f :linters [:duplicate-require]))]
          (is (= 1 (:fixed result)))
          ;; first entry must survive
          (is (str/includes? (:content result) "clojure.string :as s"))
          ;; duplicate must be gone
          (is (not (str/includes? (:content result) ":as str")))))))

  (testing "removes duplicate when both are on separate lines"
    (with-temp-file "(ns foo\n  (:require [clojure.string :as s]\n            [clojure.string :as str])) (s/join [\"\"] \"\")"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-ns-in-file f
                                (lint-file f :linters [:duplicate-require]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "clojure.string :as s"))
          (is (not (str/includes? (:content result) ":as str")))))))

  (testing "single clojure.string occurrence — removes it when unused"
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.string :as str]))"
      (fn [f]
        ;; Both are unused here; kondo reports the duplicate entry.
        ;; We should still remove the right one (the duplicate).
        (let [findings (lint-file f :linters [:duplicate-require])
              result   (apply-fix fixes/fix-unused-ns-in-file f findings)]
          (is (pos? (:fixed result)))
          ;; only one [clojure.string entry should remain
          (is (= 1 (count (re-seq #"\[clojure\.string" (:content result))))))))))

;; ============================================================
;; :unused-binding
;; ============================================================

(deftest test-unused-binding
  (testing "prefixes simple unused binding with underscore"
    (with-temp-file "(defn foo [x])"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-binding-in-file f
                                (lint-file f :linters [:unused-binding]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_x"))))))

  (testing "prefixes unused let binding"
    (with-temp-file "(let [x 1])"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-binding-in-file f
                                (lint-file f :linters [:unused-binding]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_x"))))))

  (testing "no change when binding is used"
    (with-temp-file "(defn foo [x] x)"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-binding-in-file f
                                (lint-file f :linters [:unused-binding]))]
          (is (zero? (:fixed result)))))))

  (testing "skips namespaced key destructuring (patient/id) — inserting _ would corrupt source"
    ;; clj-kondo reports col pointing into 'id' inside 'patient/id'.
    ;; We must NOT produce {:keys [patient/_id]}.
    (with-temp-file "(let [{:keys [patient/id order/id]} {}] id)"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-binding-in-file f
                                (lint-file f :linters [:unused-binding]))]
          (is (zero? (:fixed result)))
          ;; source must be unmodified
          (is (not (str/includes? (:content result) "_id")))))))

  (testing "skips :as binding — removes the whole :as clause instead"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        ;; :as bindings are reported by :unused-namespace, not :unused-binding.
        ;; This is just confirming we don't accidentally touch them here.
        (let [result (apply-fix fixes/fix-unused-binding-in-file f
                                (lint-file f :linters [:unused-binding]))]
          (is (zero? (:fixed result)))))))

  (testing "prefixes multiple unused bindings in same let"
    (with-temp-file "(loop [x 1 y 2])"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-binding-in-file f
                                (lint-file f :linters [:unused-binding]))]
          (is (= 2 (:fixed result)))
          (is (str/includes? (:content result) "_x"))
          (is (str/includes? (:content result) "_y")))))))

;; ============================================================
;; :unused-import
;; ============================================================

(deftest test-unused-import
  (testing "removes one unused import from group, leaves the other"
    (with-temp-file "(ns foo (:import [java.util Date List]))"
      (fn [f]
        (let [findings (filter #(str/ends-with? (:message %) "List")
                               (lint-file f :linters [:unused-import]))
              result   (apply-fix fixes/fix-unused-import-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "List")))
          (is (str/includes? (:content result) "Date"))))))

  (testing "removes all unused imports from group"
    (with-temp-file "(ns foo (:import [java.util Date List]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-import-in-file f
                                (lint-file f :linters [:unused-import]))]
          (is (= 2 (:fixed result)))
          (is (not (str/includes? (:content result) "Date")))
          (is (not (str/includes? (:content result) "List")))))))

  (testing "removes unused import from vector-style standalone import"
    (with-temp-file "(import '[java.util Foo Bar])"
      (fn [f]
        (let [findings (filter #(str/ends-with? (:message %) "Foo")
                               (lint-file f :linters [:unused-import]))
              result   (apply-fix fixes/fix-unused-import-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "Foo")))
          (is (str/includes? (:content result) "Bar")))))))

;; ============================================================
;; :unused-referred-var
;; ============================================================

(deftest test-unused-referred-var
  (testing "removes single unused referred var, keeps used one"
    (with-temp-file "(ns foo (:require [clojure.string :refer [join ends-with?]]))\n(join [\"\"] \"\")"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-referred-var-in-file f
                                (lint-file f :linters [:unused-referred-var]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "ends-with?")))
          (is (str/includes? (:content result) "join"))))))

  (testing "works with vars whose names end in ? (word boundary)"
    (with-temp-file "(ns foo (:require [clojure.string :refer [starts-with? ends-with?]]))"
      (fn [f]
        (let [findings (filter #(str/includes? (:message %) "ends-with?")
                               (lint-file f :linters [:unused-referred-var]))
              result   (apply-fix fixes/fix-unused-referred-var-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "ends-with?")))
          ;; starts-with? must survive
          (is (str/includes? (:content result) "starts-with?"))))))

  (testing "removes :refer clause when all vars removed"
    (with-temp-file "(ns foo (:require [clojure.string :refer [join]]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-referred-var-in-file f
                                (lint-file f :linters [:unused-referred-var]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer"))))))))

;; ============================================================
;; :refer-all
;; ============================================================

(deftest test-refer-all
  (testing "removes :refer :all leaving bare require"
    (with-temp-file "(ns foo (:require [clojure.string :refer :all]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-refer-all-in-file f
                                (lint-file f :linters [:refer-all]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer :all")))
          (is (str/includes? (:content result) "clojure.string"))))))

  (testing "removes :refer :all when :as alias also present"
    (with-temp-file "(ns foo (:require [clojure.string :as s :refer :all]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-refer-all-in-file f
                                (lint-file f :linters [:refer-all]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer :all")))
          (is (str/includes? (:content result) ":as s")))))))

;; ============================================================
;; :misplaced-docstring
;; ============================================================

(deftest test-misplaced-docstring
  (testing "moves docstring before param vector (multi-line form)"
    (with-temp-file "(defn my-fn [x y]\n  \"does something\"\n  (+ x y))"
      (fn [f]
        (let [result (apply-fix fixes/fix-misplaced-docstring-in-file f
                                (lint-file f :linters [:misplaced-docstring]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "\"does something\""))
          (is (str/includes? (:content result) "[x y]"))))))

  (testing "single-line form is skipped (safe no-op — defn and docstring on same line)"
    ;; (defn f [x] "doc" x) — finding on row 1, def-line-idx = -1 → skip
    (with-temp-file "(defn f [x] \"dude\" x)"
      (fn [f]
        (let [result (apply-fix fixes/fix-misplaced-docstring-in-file f
                                (lint-file f :linters [:misplaced-docstring]))]
          (is (zero? (:fixed result)))
          ;; source must be unmodified
          (is (str/includes? (:content result) "(defn f [x] \"dude\" x)"))))))

  (testing "correctly placed docstring is unchanged"
    (with-temp-file "(defn f \"doc\" [x] x)"
      (fn [f]
        (let [result (apply-fix fixes/fix-misplaced-docstring-in-file f
                                (lint-file f :linters [:misplaced-docstring]))]
          (is (zero? (:fixed result))))))))

;; ============================================================
;; :missing-else-branch
;; ============================================================

(deftest test-missing-else-branch
  (testing "converts (if ...) to (when ...)"
    (with-temp-file "(if true 1)"
      (fn [f]
        (let [result (apply-fix fixes/fix-missing-else-branch-in-file f
                                (lint-file f :linters [:missing-else-branch]))]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when "))))))

  (testing "converts (if-not ...) to (when-not ...)"
    (with-temp-file "(if-not true 1)"
      (fn [f]
        (let [result (apply-fix fixes/fix-missing-else-branch-in-file f
                                (lint-file f :linters [:missing-else-branch]))]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-not "))))))

  (testing "converts (if-let ...) to (when-let ...)"
    (with-temp-file "(if-let [x 1] x)"
      (fn [f]
        (let [result (apply-fix fixes/fix-missing-else-branch-in-file f
                                (lint-file f :linters [:missing-else-branch]))]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-let "))))))

  (testing "converts (if-some ...) to (when-some ...)"
    (with-temp-file "(if-some [x 1] x)"
      (fn [f]
        (let [result (apply-fix fixes/fix-missing-else-branch-in-file f
                                (lint-file f :linters [:missing-else-branch]))]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-some "))))))

  (testing "multiple if variants on same line all converted"
    (with-temp-file "(if true 1) (if-not true 1) (if-let [x 1] x) (if-some [x 1] x)"
      (fn [f]
        (let [result (apply-fix fixes/fix-missing-else-branch-in-file f
                                (lint-file f :linters [:missing-else-branch]))]
          (is (= 4 (:fixed result)))
          (is (str/includes? (:content result) "(when "))
          (is (str/includes? (:content result) "(when-not "))
          (is (str/includes? (:content result) "(when-let "))
          (is (str/includes? (:content result) "(when-some "))))))

  (testing "no change when else branch is present"
    (with-temp-file "(if true 1 2)"
      (fn [f]
        (let [result (apply-fix fixes/fix-missing-else-branch-in-file f
                                (lint-file f :linters [:missing-else-branch]))]
          (is (zero? (:fixed result))))))))

;; ============================================================
;; :unused-private-var
;; ============================================================

(deftest test-unused-private-var
  (testing "prefixes defn- var name, not an earlier same-letter substring"
    ;; Bug target: (ns foo) (defn- f []) — 'f' also appears in 'foo'.
    ;; .indexOf('f') from 0 would find 'foo' first.  With col-based search it
    ;; must find the definition site.
    (with-temp-file "(ns foo) (defn- f [])"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-private-var-in-file f
                                (lint-file f :linters [:unused-private-var]))]
          (is (= 1 (:fixed result)))
          ;; 'foo' must be untouched
          (is (str/includes? (:content result) "(ns foo)"))
          ;; def site must be renamed
          (is (str/includes? (:content result) "defn- _f"))))))

  (testing "prefixes def ^:private var, not earlier substring"
    ;; (ns foo) (def ^:private f) — same issue: 'f' in 'foo' comes first.
    (with-temp-file "(ns foo) (def ^:private f)"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-private-var-in-file f
                                (lint-file f :linters [:unused-private-var]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(ns foo)"))
          (is (str/includes? (:content result) "^:private _f"))))))

  (testing "handles multi-char var name correctly"
    (with-temp-file "(ns foo) (defn- my-helper [])"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-private-var-in-file f
                                (lint-file f :linters [:unused-private-var]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_my-helper"))))))

  (testing "renames second private var independently on same line"
    ;; col points to each var separately; neither should clobber the other
    (with-temp-file "(defn- foo [] (foo)) (defn- bar ([] (bar 1)) ([_]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-unused-private-var-in-file f
                                (lint-file f :linters [:unused-private-var]))]
          (is (= 2 (:fixed result)))
          (is (str/includes? (:content result) "defn- _foo"))
          (is (str/includes? (:content result) "defn- _bar")))))))

;; ============================================================
;; :redundant-do
;; ============================================================

(deftest test-redundant-do
  (testing "removes redundant do wrapper (single-line)"
    (with-temp-file "(when true (do (println \"a\") (println \"b\")))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-do-in-file f
                                (lint-file f :linters [:redundant-do]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "(do"))))))))

;; ============================================================
;; :redundant-let
;; ============================================================

(deftest test-redundant-let
  (testing "single-line: no body"
    ;; (let [x 2] (let [y 1])) → (let [x 2 y 1])
    (with-temp-file "(let [x 2] (let [y 1]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(let [x 2 y 1])"))))))

  (testing "single-line: with body"
    ;; (let [x 2] (let [y 1] (+ x y))) → (let [x 2 y 1] (+ x y))
    (with-temp-file "(let [x 2] (let [y 1] (+ x y)))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(let [x 2 y 1] (+ x y))"))))))

  (testing "multi-line: no body"
    (with-temp-file "(let [x 1]\n  (let [y 2]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "(let [y 2])")))
          (is (str/includes? (:content result) "y 2")))))  )

  (testing "multi-line: with body on its own line"
    (with-temp-file "(let [x 1]\n  (let [y 2]\n    (+ x y)))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "y 2]"))
          (is (str/includes? (:content result) "(+ x y)"))
          ;; only one ) at the end, not ))
          (is (str/ends-with? (str/trim (:content result)) ")"))))))

  (testing "multi-line: body inline with inner binding close"
    ;; (let [x 1]\n  (let [y 2] body)) — inner let all on one line
    (with-temp-file "(let [x 1]\n  (let [y 2] (+ x y)))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "y 2]"))
          (is (str/includes? (:content result) "(+ x y)"))))))

  (testing "multi-line: multiple inner bindings"
    (with-temp-file "(let [a 1]\n  (let [b 2\n        c 3]\n    (+ a b c)))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "b 2"))
          (is (str/includes? (:content result) "c 3]"))
          (is (str/includes? (:content result) "(+ a b c)"))))))

  (testing "intermediate #_ discard form: moved before merged let"
    (with-temp-file "(let [x 1]\n  #_(println \"hello\")\n  (let [y 2]))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          ;; discard form must appear before the let
          (let [lines (str/split-lines (:content result))
                discard-idx (first (keep-indexed #(when (str/includes? %2 "#_") %1) lines))
                let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
            (is (some? discard-idx))
            (is (some? let-idx))
            (is (< discard-idx let-idx)))))))

  (testing "intermediate comment line: moved before merged let"
    (with-temp-file "(let [x 1]\n  ;; important note\n  (let [y 2]\n    body))"
      (fn [f]
        (let [result (apply-fix fixes/fix-redundant-let-in-file f
                                (lint-file f :linters [:redundant-let]))]
          (is (= 1 (:fixed result)))
          (let [lines (str/split-lines (:content result))
                comment-idx (first (keep-indexed #(when (str/includes? %2 ";;") %1) lines))
                let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
            (is (some? comment-idx))
            (is (some? let-idx))
            (is (< comment-idx let-idx)))
          (is (str/includes? (:content result) "body")))))  )

  (testing "skip: outer let with multi-line binding vector"
    ;; (let [x 1\n      y 2]\n  (let [z 3])) — outer binding spans two lines → no-op
    (with-temp-file "(let [x 1\n      y 2]\n  (let [z 3]))"
      (fn [f]
        (let [findings (lint-file f :linters [:redundant-let])
              result   (apply-fix fixes/fix-redundant-let-in-file f findings)]
          ;; should be a safe no-op
          (is (zero? (:fixed result))))))))

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
