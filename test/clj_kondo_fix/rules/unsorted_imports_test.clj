(ns clj-kondo-fix.rules.unsorted-imports-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-unsorted-imports
  (testing "single line import: reorder packages"
    (let [result (assert-fix fixes/fix-unsorted-imports-in-file
                             (fixture-path "unsorted-imports" "single-line-in")
                             [:unsorted-imports] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unsorted-imports" "single-line-out")) (:content result)))))

  (testing "multi line import: reorder packages"
    (let [result (assert-fix fixes/fix-unsorted-imports-in-file
                             (fixture-path "unsorted-imports" "multi-line-in")
                             [:unsorted-imports] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unsorted-imports" "multi-line-out")) (:content result))))))
