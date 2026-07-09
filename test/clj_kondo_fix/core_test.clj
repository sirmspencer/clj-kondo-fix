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
  "Apply fix-fn to file-path and return {:fixed N :content string}.
   Writes the result back to the file if changed."
  [fix-fn file-path findings]
  (let [lines  (read-lines file-path)
        log    (atom [])
        result (fix-fn file-path lines findings log)]
    (when (:changed? result)
      (spit file-path (str/join "\n" (:lines result))))
    {:fixed (:fixed result) :content (slurp file-path)}))

(defn assert-fix
  "Assert: expected-count matching findings exist before fix; none after.
   Optional filter-fn narrows which findings count (for partial-fix cases).
   Returns the apply-fix result map for additional content assertions."
  ([fix-fn file-path linters expected-count]
   (assert-fix fix-fn file-path linters expected-count nil))
  ([fix-fn file-path linters expected-count filter-fn]
   (let [all-before (lint-file file-path :linters linters)
         before     (if filter-fn (filter filter-fn all-before) all-before)]
     (is (= expected-count (count before))
         (str "expected " expected-count " finding(s) before fix, got " (count before)))
     (let [result    (apply-fix fix-fn file-path before)
           all-after (lint-file file-path :linters linters)
           after     (if filter-fn (filter filter-fn all-after) all-after)]
       (is (empty? after) "expected no matching findings after fix")
       result))))

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
    (with-temp-file "(ns foo\n  (:require [clojure.string :as s]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.string")))))))

  (testing "all requires removed — (:require) block is removed and ns closes cleanly"
    ;; Reproduces path.handler.digital: both entries unused, result must be
    ;; (ns foo) not (ns foo\n  (:require )))
    (with-temp-file "(ns foo\n  (:require [clojure.string :as s]\n            [clojure.set :as cs]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 2)]
          (is (= 2 (:fixed result)))
          (is (not (str/includes? (:content result) ":require")))
          (is (= "(ns foo)" (str/trim (:content result))))))))

  (testing "leaves used namespace untouched"
    (with-temp-file "(ns foo (:require [clojure.string :as s])) (s/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-no-finding fixes/fix-unused-ns-in-file f [:unused-namespace])]
          (is (zero? (:fixed result)))))))

  (testing "removes both of two unused namespaces on same line"
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.set :as cs]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 2)]
          (is (= 2 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.string")))
          (is (not (str/includes? (:content result) "clojure.set")))))))

  (testing "trailing comment on the removed entry line — no corruption"
    ;; [clojure.set :as cs] ;; for set ops — comment becomes a straggler but
    ;; the linter no longer fires (the entry is gone) and source is valid.
    (with-temp-file "(ns foo\n  (:require [clojure.string :as s]\n            [clojure.set :as cs] ;; for set ops\n))\n(s/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.set :as cs")))
          (is (str/includes? (:content result) "clojure.string :as s"))))))

  (testing "comment-only line before removed entry stays as orphan — no corruption"
    (with-temp-file "(ns foo\n  (:require [clojure.string :as s]\n            ;; this one is unused\n            [clojure.set :as cs]))\n(s/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.set :as cs")))
          (is (str/includes? (:content result) "clojure.string :as s"))))))

  (testing "inline single-line ns: entry removed, (:require ) straggler stays — no corruption"
    ;; cleanup-empty-clauses only matches (:require) when alone on its own line;
    ;; the straggler is harmless and the linter no longer fires.
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.string")))))))

  (testing "last entry removed when prev line is (:require — closing )) merged onto preceding ]"
    ;; Reproduces change_detector.clj:
    ;;   (:require [clojure.set :as set]
    ;;             [clojure.tools.logging :as log]))   ← removed
    ;; Must produce:   (:require [clojure.set :as set]))
    ;; NOT:            (:require [clojure.set :as set]\n            ))
    (with-temp-file "(ns foo\n  (:require [clojure.set :as set]\n            [clojure.tools.logging :as log]))\n(set/difference #{1} #{2})"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.tools.logging")))
          (is (str/includes? (:content result) "[clojure.set :as set]))"))))))

  (testing "last entry removed when prev entry is multi-line — closing ) merged onto :as line"
    ;; Reproduces outside_plant_multiplexer.clj:
    ;;   [clojure.string
    ;;    :as str]
    ;;   [clojure.set :as cs])   ← removed
    ;; Must produce:   [clojure.string\n   :as str])
    ;; NOT:            [clojure.string\n   :as str]\n            )
    (with-temp-file "(ns foo\n  (:require [clojure.string\n             :as str]\n            [clojure.set :as cs]))\n(str/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.set")))
          (is (str/includes? (:content result) ":as str]))"  ))))))

  (testing "multi-line entry removed: middle entry between two single-line entries"
    ;; [path.acceptance...\n :as scenarios] is in the middle; both siblings survive.
    (with-temp-file "(ns foo\n  (:require [clojure.set :as cs]\n            [path.acceptance.augment.remote-phy-device.reserve-daas-port\n             :as scenarios]\n            [clojure.string :as str]))\n(cs/difference #{1} #{2})\n(str/join [\"\"] \"\")  "
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "reserve-daas-port")))
          (is (str/includes? (:content result) "clojure.set :as cs"))
          (is (str/includes? (:content result) "clojure.string :as str"))))))

  (testing "multi-line entry removed: last entry — closing ) merged onto previous ]"
    ;; [path.acceptance...\n :as scenarios]) is last; ) must land on [clojure.set] line.
    (with-temp-file "(ns foo\n  (:require [clojure.set :as cs]\n            [path.acceptance.augment.remote-phy-device.reserve-daas-port\n             :as scenarios]))\n(cs/difference #{1} #{2})"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "reserve-daas-port")))
          ;; closing )) must be on the surviving entry's line, not dangling
          (is (str/includes? (:content result) "[clojure.set :as cs]))"  ))))))

  (testing "multi-line entry removed: only entry in require clause"
    ;; [path.acceptance...\n :as scenarios] is the only require; clause is cleaned up.
    (with-temp-file "(ns foo\n  (:require\n   [path.acceptance.augment.remote-phy-device.reserve-daas-port\n    :as scenarios]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-ns-in-file f [:unused-namespace] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "reserve-daas-port"))))))  ))

(deftest test-duplicate-require
  (testing "case 1: only first alias used — remove reported duplicate, no renames"
    ;; [ns :as s] used via s/join; [ns :as str] unused → remove str entry
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.string :as str])) (s/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-duplicate-require-in-file f [:duplicate-require] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "clojure.string :as s"))
          (is (not (str/includes? (:content result) ":as str")))
          ;; s/ calls must be untouched
          (is (str/includes? (:content result) "s/join"))))))

  (testing "case 2: only duplicate alias used — remove first entry, no renames"
    ;; [ns :as s] not used; [ns :as str] used via str/join → remove s entry, keep str
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.string :as str])) (str/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-duplicate-require-in-file f [:duplicate-require] 1)]
          (is (= 1 (:fixed result)))
          ;; str entry must survive
          (is (str/includes? (:content result) "clojure.string :as str"))
          ;; s entry must be gone
          (is (not (str/includes? (:content result) ":as s ") ))
          ;; str/ calls must be untouched
          (is (str/includes? (:content result) "str/join"))))))

  (testing "case 3: both aliases used — keep longer, rename shorter usages, remove shorter entry"
    ;; [ns :as pt] used + [ns :as toolz] used → toolz is longer → keep toolz, rename pt/ → toolz/
    (with-temp-file "(ns foo\n  (:require [path.tools :as pt]\n            [path.tools :as toolz]))\n(pt/make-endpoint :x)\n(toolz/make-exception {})"
      (fn [f]
        (let [result (assert-fix fixes/fix-duplicate-require-in-file f [:duplicate-require] 1)]
          (is (= 1 (:fixed result)))
          ;; toolz entry survives
          (is (str/includes? (:content result) "path.tools :as toolz"))
          ;; pt entry gone
          (is (not (str/includes? (:content result) ":as pt")))
          ;; pt/ calls renamed to toolz/
          (is (not (str/includes? (:content result) "pt/")))
          (is (str/includes? (:content result) "toolz/make-endpoint"))
          (is (str/includes? (:content result) "toolz/make-exception")))))  )

  (testing "case 3 tie: both aliases same length — keep first (shorter or equal wins)"
    ;; [ns :as aa] and [ns :as bb] both used, same length → keep first (aa), rename bb/ → aa/
    (with-temp-file "(ns foo\n  (:require [clojure.string :as aa]\n            [clojure.string :as bb]))\n(aa/join [\"\"] \"\")\n(bb/upper-case \"x\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-duplicate-require-in-file f [:duplicate-require] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "clojure.string :as aa"))
          (is (not (str/includes? (:content result) ":as bb")))
          (is (str/includes? (:content result) "aa/join"))
          (is (str/includes? (:content result) "aa/upper-case"))
          (is (not (str/includes? (:content result) "bb/")))))))

  (testing "case 4: neither alias used — remove reported duplicate, first entry remains"
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.string :as str]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-duplicate-require-in-file f [:duplicate-require] 1)]
          (is (pos? (:fixed result)))
          (is (= 1 (count (re-seq #"\[clojure\.string" (:content result))))))))  )

  (testing "entries on separate lines"
    (with-temp-file "(ns foo\n  (:require [clojure.string :as s]\n            [clojure.string :as str])) (s/join [\"\"] \"\")"
      (fn [f]
        (let [result (assert-fix fixes/fix-duplicate-require-in-file f [:duplicate-require] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "clojure.string :as s"))
          (is (not (str/includes? (:content result) ":as str"))))))))
;; ============================================================
;; :unused-binding
;; ============================================================

(deftest test-unused-binding
  (testing "prefixes simple unused binding with underscore"
    (with-temp-file "(defn foo [x])"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-binding-in-file f [:unused-binding] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_x"))))))

  (testing "prefixes unused let binding"
    (with-temp-file "(let [x 1])"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-binding-in-file f [:unused-binding] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_x"))))))

  (testing "no change when binding is used"
    (with-temp-file "(defn foo [x] x)"
      (fn [f]
        (let [result (assert-no-finding fixes/fix-unused-binding-in-file f [:unused-binding])]
          (is (zero? (:fixed result)))))))

  (testing "skips namespaced key destructuring — inserting _ would corrupt source"
    ;; kondo reports col pointing into 'id' inside 'patient/id'.
    ;; We must NOT produce {:keys [patient/_id]}.
    (with-temp-file "(let [{:keys [patient/id order/id]} {}] id)"
      (fn [f]
        (let [result (assert-skip fixes/fix-unused-binding-in-file f [:unused-binding])]
          (is (not (str/includes? (:content result) "_id")))))))

  (testing ":as bindings are not reported by :unused-binding"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [result (assert-no-finding fixes/fix-unused-binding-in-file f [:unused-binding])]
          (is (zero? (:fixed result)))))))

  (testing "prefixes multiple unused bindings in same let"
    (with-temp-file "(loop [x 1 y 2])"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-binding-in-file f [:unused-binding] 2)]
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
        (let [list-pred #(str/ends-with? (:message %) "List")
              result    (assert-fix fixes/fix-unused-import-in-file f [:unused-import] 1 list-pred)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "List")))
          ;; Date is still present (we only fixed List)
          (is (str/includes? (:content result) "Date"))
          ;; Date finding still fires
          (is (= 1 (count (filter #(str/ends-with? (:message %) "Date")
                                  (lint-file f :linters [:unused-import])))))))))

  (testing "removes all unused imports from group"
    (with-temp-file "(ns foo (:import [java.util Date List]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-import-in-file f [:unused-import] 2)]
          (is (= 2 (:fixed result)))
          (is (not (str/includes? (:content result) "Date")))
          (is (not (str/includes? (:content result) "List")))))))

  (testing "removes unused import from vector-style standalone import"
    (with-temp-file "(import '[java.util Foo Bar])"
      (fn [f]
        (let [foo-pred #(str/ends-with? (:message %) "Foo")
              result   (assert-fix fixes/fix-unused-import-in-file f [:unused-import] 1 foo-pred)]
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
        (let [pred   #(str/includes? (:message %) "ends-with?")
              result (assert-fix fixes/fix-unused-referred-var-in-file f [:unused-referred-var] 1 pred)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "ends-with?")))
          (is (str/includes? (:content result) "join"))))))

  (testing "works with vars whose names end in ? (word boundary)"
    (with-temp-file "(ns foo (:require [clojure.string :refer [starts-with? ends-with?]]))"
      (fn [f]
        (let [pred   #(str/includes? (:message %) "ends-with?")
              result (assert-fix fixes/fix-unused-referred-var-in-file f [:unused-referred-var] 1 pred)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "ends-with?")))
          (is (str/includes? (:content result) "starts-with?"))))))

  (testing "removes :refer clause when all vars removed"
    (with-temp-file "(ns foo (:require [clojure.string :refer [join]]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-referred-var-in-file f [:unused-referred-var] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer")))))))

  (testing "multi-line :refer vector: removes var from its own line"
    (with-temp-file "(ns foo\n  (:require\n   [clojure.string :refer [join\n                            ends-with?]]))\n(join [\"\"] \"\")"
      (fn [f]
        (let [pred   #(str/includes? (:message %) "ends-with?")
              result (assert-fix fixes/fix-unused-referred-var-in-file f [:unused-referred-var] 1 pred)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "ends-with?")))
          (is (str/includes? (:content result) "join")))))))

;; ============================================================
;; :refer-all
;; ============================================================

(deftest test-refer-all
  (testing "removes :refer :all leaving bare require"
    (with-temp-file "(ns foo (:require [clojure.string :refer :all]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-refer-all-in-file f [:refer-all] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer :all")))
          (is (str/includes? (:content result) "clojure.string"))))))

  (testing "removes :refer :all when :as alias also present"
    (with-temp-file "(ns foo (:require [clojure.string :as s :refer :all]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-refer-all-in-file f [:refer-all] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer :all")))
          (is (str/includes? (:content result) ":as s"))))))

  (testing "multi-line: :refer :all on separate line from ns — safe skip"
    ;; find-require-entry-start scans backward on the :all line only;
    ;; the opening [ is on the previous line so it returns nil → no-op.
    (with-temp-file "(ns foo\n  (:require [clojure.string\n             :refer :all]))"
      (fn [f]
        (let [result (assert-skip fixes/fix-refer-all-in-file f [:refer-all])]
          (is (str/includes? (:content result) ":refer :all")))))))

;; ============================================================
;; :misplaced-docstring
;; ============================================================

(deftest test-misplaced-docstring
  (testing "moves docstring before param vector (multi-line form)"
    (with-temp-file "(defn my-fn [x y]\n  \"does something\"\n  (+ x y))"
      (fn [f]
        (let [result (assert-fix fixes/fix-misplaced-docstring-in-file f [:misplaced-docstring] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "\"does something\""))
          (is (str/includes? (:content result) "[x y]"))))))

  (testing "single-line form is skipped — finding is on row 1, def-line-idx = -1"
    (with-temp-file "(defn f [x] \"dude\" x)"
      (fn [f]
        (let [result (assert-skip fixes/fix-misplaced-docstring-in-file f [:misplaced-docstring])]
          (is (str/includes? (:content result) "(defn f [x] \"dude\" x)"))))))

  (testing "correctly placed docstring is unchanged"
    (with-temp-file "(defn f \"doc\" [x] x)"
      (fn [f]
        (let [result (assert-no-finding fixes/fix-misplaced-docstring-in-file f [:misplaced-docstring])]
          (is (zero? (:fixed result)))))))

  (testing "comment between params and docstring — safe skip"
    ;; def-line-idx points to the comment line; find-bracket finds no [ there.
    (with-temp-file "(defn f [x]\n  ;; explains x\n  \"doc\"\n  x)"
      (fn [f]
        (let [result (assert-skip fixes/fix-misplaced-docstring-in-file f [:misplaced-docstring])]
          (is (str/includes? (:content result) ";; explains x"))
          (is (str/includes? (:content result) "\"doc\""))))))

  (testing "multi-line defn signature: params on separate line — safe skip"
    ;; def-line is \"  [x]\", prefix is blank; blank-prefix guard skips
    ;; to avoid emitting a malformed defn with an injected empty line.
    (with-temp-file "(defn f\n  [x]\n  \"doc\"\n  x)"
      (fn [f]
        (let [result (assert-skip fixes/fix-misplaced-docstring-in-file f [:misplaced-docstring])]
          (is (str/includes? (:content result) "(defn f"))
          (is (str/includes? (:content result) "  [x]"))
          (is (str/includes? (:content result) "\"doc\"")))))))

;; ============================================================
;; :missing-else-branch
;; ============================================================

(deftest test-missing-else-branch
  (testing "converts (if ...) to (when ...)"
    (with-temp-file "(if true 1)"
      (fn [f]
        (let [result (assert-fix fixes/fix-missing-else-branch-in-file f [:missing-else-branch] 1)]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when "))))))

  (testing "converts (if-not ...) to (when-not ...)"
    (with-temp-file "(if-not true 1)"
      (fn [f]
        (let [result (assert-fix fixes/fix-missing-else-branch-in-file f [:missing-else-branch] 1)]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-not "))))))

  (testing "converts (if-let ...) to (when-let ...)"
    (with-temp-file "(if-let [x 1] x)"
      (fn [f]
        (let [result (assert-fix fixes/fix-missing-else-branch-in-file f [:missing-else-branch] 1)]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-let "))))))

  (testing "converts (if-some ...) to (when-some ...)"
    (with-temp-file "(if-some [x 1] x)"
      (fn [f]
        (let [result (assert-fix fixes/fix-missing-else-branch-in-file f [:missing-else-branch] 1)]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-some "))))))

  (testing "multiple if variants on same line all converted"
    (with-temp-file "(if true 1) (if-not true 1) (if-let [x 1] x) (if-some [x 1] x)"
      (fn [f]
        (let [result (assert-fix fixes/fix-missing-else-branch-in-file f [:missing-else-branch] 4)]
          (is (= 4 (:fixed result)))
          (is (str/includes? (:content result) "(when "))
          (is (str/includes? (:content result) "(when-not "))
          (is (str/includes? (:content result) "(when-let "))
          (is (str/includes? (:content result) "(when-some "))))))

  (testing "no change when else branch is present"
    (with-temp-file "(if true 1 2)"
      (fn [f]
        (let [result (assert-no-finding fixes/fix-missing-else-branch-in-file f [:missing-else-branch])]
          (is (zero? (:fixed result))))))))

;; ============================================================
;; :unused-private-var
;; ============================================================

(deftest test-unused-private-var
  (testing "prefixes defn- var name, not an earlier same-letter substring"
    ;; 'f' also appears in 'foo'; col-based search must land on the definition.
    (with-temp-file "(ns foo) (defn- f [])"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-private-var-in-file f [:unused-private-var] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(ns foo)"))
          (is (str/includes? (:content result) "defn- _f"))))))

  (testing "prefixes def ^:private var, not earlier substring"
    (with-temp-file "(ns foo) (def ^:private f)"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-private-var-in-file f [:unused-private-var] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(ns foo)"))
          (is (str/includes? (:content result) "^:private _f"))))))

  (testing "handles multi-char var name correctly"
    (with-temp-file "(ns foo) (defn- my-helper [])"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-private-var-in-file f [:unused-private-var] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_my-helper"))))))

  (testing "renames two private vars independently on same line"
    (with-temp-file "(defn- foo [] (foo)) (defn- bar ([] (bar 1)) ([_]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-unused-private-var-in-file f [:unused-private-var] 2)]
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
        (let [result (assert-fix fixes/fix-redundant-do-in-file f [:redundant-do] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "(do")))))))

  (testing "removes redundant do wrapper (multi-line)"
    (with-temp-file "(when true\n  (do\n    (println \"a\")\n    (println \"b\")))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-do-in-file f [:redundant-do] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "(do")))
          (is (str/includes? (:content result) "(println \"a\")"))
          (is (str/includes? (:content result) "(println \"b\")")))))))

;; ============================================================
;; :redundant-let
;; ============================================================

(deftest test-redundant-let
  (testing "single-line: no body"
    (with-temp-file "(let [x 2] (let [y 1]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(let [x 2 y 1])"))))))

  (testing "single-line: with body"
    (with-temp-file "(let [x 2] (let [y 1] (+ x y)))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "(let [x 2 y 1] (+ x y))"))))))

  (testing "multi-line: no body"
    (with-temp-file "(let [x 1]\n  (let [y 2]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "(let [y 2])")))
          (is (str/includes? (:content result) "y 2"))))))

  (testing "multi-line: with body on its own line"
    (with-temp-file "(let [x 1]\n  (let [y 2]\n    (+ x y)))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "y 2]"))
          (is (str/includes? (:content result) "(+ x y)"))
          (is (str/ends-with? (str/trim (:content result)) ")"))))))

  (testing "multi-line: body inline with inner binding close"
    (with-temp-file "(let [x 1]\n  (let [y 2] (+ x y)))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "y 2]"))
          (is (str/includes? (:content result) "(+ x y)"))))))

  (testing "multi-line: multiple inner bindings"
    (with-temp-file "(let [a 1]\n  (let [b 2\n        c 3]\n    (+ a b c)))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "b 2"))
          (is (str/includes? (:content result) "c 3]"))
          (is (str/includes? (:content result) "(+ a b c)"))))))

  (testing "intermediate #_ discard form: moved before merged let"
    (with-temp-file "(let [x 1]\n  #_(println \"hello\")\n  (let [y 2]))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (let [lines      (str/split-lines (:content result))
                discard-idx (first (keep-indexed #(when (str/includes? %2 "#_") %1) lines))
                let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
            (is (some? discard-idx))
            (is (some? let-idx))
            (is (< discard-idx let-idx)))))))

  (testing "intermediate comment line: moved before merged let"
    (with-temp-file "(let [x 1]\n  ;; important note\n  (let [y 2]\n    body))"
      (fn [f]
        (let [result (assert-fix fixes/fix-redundant-let-in-file f [:redundant-let] 1)]
          (is (= 1 (:fixed result)))
          (let [lines       (str/split-lines (:content result))
                comment-idx (first (keep-indexed #(when (str/includes? %2 ";;") %1) lines))
                let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
            (is (some? comment-idx))
            (is (some? let-idx))
            (is (< comment-idx let-idx)))
          (is (str/includes? (:content result) "body"))))))

  (testing "skip: outer let with multi-line binding vector"
    ;; outer binding spans two lines — unsupported structure, safe no-op
    (with-temp-file "(let [x 1\n      y 2]\n  (let [z 3]))"
      (fn [f]
        (let [result (assert-skip fixes/fix-redundant-let-in-file f [:redundant-let])]
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
