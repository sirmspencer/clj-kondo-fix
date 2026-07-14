(ns clj-kondo-fix.rules.redundant-declare-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-redundant-declare
  (testing "single redundant var: remove entire declare"
    (let [result (assert-fix fixes/fix-redundant-declare-in-file
                             (fixture-path "redundant-declare" "single-var-in")
                             [:redundant-declare] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-declare" "single-var-out")) (:content result)))))

  (testing "multi-var partial: remove only redundant var"
    (let [result (assert-fix fixes/fix-redundant-declare-in-file
                             (fixture-path "redundant-declare" "multi-var-partial-in")
                             [:redundant-declare] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-declare" "multi-var-partial-out")) (:content result)))))

  (testing "multi-var all redundant: remove entire declare"
    (let [result (assert-fix fixes/fix-redundant-declare-in-file
                             (fixture-path "redundant-declare" "multi-var-all-in")
                             [:redundant-declare] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-declare" "multi-var-all-out")) (:content result))))))
