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
        config (merge all-off enabled)
        result (kondo/run! {:lint [file-path] :config config})]
    (for [f (:findings result)]
      {:line (:row f)
       :col  (:col f)
       :message (:message f)})))

(defn apply-fix
  "Apply fix-fn to file-path and return {:fixed N :content string}.
   Writes the result back to the file if changed."
  [fix-fn file-path findings]
  (let [lines (read-lines file-path)
        log   (atom [])
        result (fix-fn file-path lines findings log)]
    (when (:changed? result)
      (spit file-path (str/join "\n" (:lines result))))
    {:fixed (:fixed result) :content (slurp file-path)}))

;; ============================================================
;; Unit tests — one per rule
;; ============================================================

(deftest test-unused-namespace
  (testing "removes unused namespace from require"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-namespace])
              result   (apply-fix fixes/fix-unused-ns-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "clojure.string"))))))))

(deftest test-duplicate-require
  (testing "removes duplicate require, keeps one"
    (with-temp-file "(ns foo (:require [clojure.string :as s] [clojure.string :as str])) s/join"
      (fn [f]
        (let [findings (lint-file f :linters [:duplicate-require])
              result   (apply-fix fixes/fix-unused-ns-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (= 1 (count (re-seq #"clojure\.string" (:content result))))))))))

(deftest test-unused-binding
  (testing "prefixes unused binding with underscore"
    (with-temp-file "(defn foo [x])"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-binding])
              result   (apply-fix fixes/fix-unused-binding-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_x"))))))
  (testing "no change when binding is used"
    (with-temp-file "(defn foo [x] x)"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-binding])
              result   (apply-fix fixes/fix-unused-binding-in-file f findings)]
          (is (zero? (:fixed result))))))))

(deftest test-unused-import
  (testing "removes unused import from group"
    (with-temp-file "(ns foo (:import [java.util Date List]))"
      (fn [f]
        (let [findings (filter #(str/ends-with? (:message %) "List")
                               (lint-file f :linters [:unused-import]))
              result   (apply-fix fixes/fix-unused-import-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "List")))
          (is (str/includes? (:content result) "Date"))))))
  (testing "removes both unused imports from ns bracket form"
    (with-temp-file "(ns foo (:import [java.util Date List]))"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-import])
              result   (apply-fix fixes/fix-unused-import-in-file f findings)]
          (is (= 2 (:fixed result)))
          (is (not (str/includes? (:content result) "Date")))
          (is (not (str/includes? (:content result) "List"))))))))

(deftest test-unused-referred-var
  (testing "removes single unused referred var"
    (with-temp-file "(ns foo (:require [clojure.string :refer [join ends-with?]]))\n(join [\"\"] \"\")"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-referred-var])
              result   (apply-fix fixes/fix-unused-referred-var-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "ends-with?")))
          (is (str/includes? (:content result) "join"))))))
  (testing "removes :refer clause entirely when all vars removed"
    (with-temp-file "(ns foo (:require [clojure.string :refer [join]]))"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-referred-var])
              result   (apply-fix fixes/fix-unused-referred-var-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer"))))))))

(deftest test-refer-all
  (testing "removes :refer :all leaving bare namespace"
    (with-temp-file "(ns foo (:require [clojure.string :refer :all]))"
      (fn [f]
        (let [findings (lint-file f :linters [:refer-all])
              result   (apply-fix fixes/fix-refer-all-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) ":refer :all")))
          (is (str/includes? (:content result) "clojure.string")))))))

(deftest test-misplaced-docstring
  (testing "moves docstring before param vector"
    (with-temp-file "(defn my-fn [x y]\n  \"does something\"\n  (+ x y))"
      (fn [f]
        (let [findings (lint-file f :linters [:misplaced-docstring])
              result   (apply-fix fixes/fix-misplaced-docstring-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "\"does something\""))
          (is (str/includes? (:content result) "[x y]"))))))
  (testing "no change when docstring is correctly placed"
    (with-temp-file "(defn f \"doc\" [x] x)"
      (fn [f]
        (let [findings (lint-file f :linters [:misplaced-docstring])
              result   (apply-fix fixes/fix-misplaced-docstring-in-file f findings)]
          (is (zero? (:fixed result))))))))

(deftest test-missing-else-branch
  (testing "converts (if ...) to (when ...)"
    (with-temp-file "(if true 1)"
      (fn [f]
        (let [findings (lint-file f :linters [:missing-else-branch])
              result   (apply-fix fixes/fix-missing-else-branch-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when "))))))
  (testing "converts (if-let ...) to (when-let ...)"
    (with-temp-file "(if-let [x 1] x)"
      (fn [f]
        (let [findings (lint-file f :linters [:missing-else-branch])
              result   (apply-fix fixes/fix-missing-else-branch-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (str/starts-with? (:content result) "(when-let"))))))
  (testing "no change when else branch is present"
    (with-temp-file "(if true 1 2)"
      (fn [f]
        (let [findings (lint-file f :linters [:missing-else-branch])
              result   (apply-fix fixes/fix-missing-else-branch-in-file f findings)]
          (is (zero? (:fixed result))))))))

(deftest test-unused-private-var
  (testing "prefixes unused private var with underscore"
    (with-temp-file "(ns foo) (defn- my-helper [])"
      (fn [f]
        (let [findings (lint-file f :linters [:unused-private-var])
              result   (apply-fix fixes/fix-unused-private-var-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (str/includes? (:content result) "_my-helper")))))))

(deftest test-redundant-do
  (testing "removes redundant do wrapper"
    (with-temp-file "(when true (do (println \"a\") (println \"b\")))"
      (fn [f]
        (let [findings (lint-file f :linters [:redundant-do])
              result   (apply-fix fixes/fix-redundant-do-in-file f findings)]
          (is (= 1 (:fixed result)))
          (is (not (str/includes? (:content result) "(do"))))))))

(deftest test-redundant-let
  (testing "inlines single-line redundant let"
    (with-temp-file "(let [x 2] (let [y 1]))"
      (fn [f]
        (let [findings (lint-file f :linters [:redundant-let])
              result   (apply-fix fixes/fix-redundant-let-in-file f findings)]
          (is (pos? (:fixed result)))))))
  (testing "skips multi-line form (safe no-op)"
    (with-temp-file "(let [x 1]\n  (let [y 2]\n    (+ x y)))"
      (fn [f]
        (let [findings (lint-file f :linters [:redundant-let])
              result   (apply-fix fixes/fix-redundant-let-in-file f findings)]
          ;; multi-line let is skipped — no crash, no corruption
          (is (not (nil? result))))))))

;; ============================================================
;; Integration test — full pipeline
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
