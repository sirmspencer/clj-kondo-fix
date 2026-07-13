(ns clj-kondo-fix.rules.unused-namespace-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

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
