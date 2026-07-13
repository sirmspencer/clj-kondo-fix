(ns clj-kondo-fix.rules.redundant-do-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

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
