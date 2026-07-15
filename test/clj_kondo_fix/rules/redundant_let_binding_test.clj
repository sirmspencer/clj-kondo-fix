(ns clj-kondo-fix.rules.redundant-let-binding-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-redundant-let-binding
  (testing "single redundant binding: remove binding pair"
    (let [result (assert-fix fixes/fix-redundant-let-binding-in-file
                             (fixture-path "redundant-let-binding" "single-in")
                             [:redundant-let-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let-binding" "single-out")) (:content result)))))

  (testing "first of multiple: remove only redundant pair"
    (let [result (assert-fix fixes/fix-redundant-let-binding-in-file
                             (fixture-path "redundant-let-binding" "multi-first-in")
                             [:redundant-let-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let-binding" "multi-first-out")) (:content result)))))

  (testing "last of multiple: remove only redundant pair"
    (let [result (assert-fix fixes/fix-redundant-let-binding-in-file
                             (fixture-path "redundant-let-binding" "multi-last-in")
                             [:redundant-let-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let-binding" "multi-last-out")) (:content result))))))
