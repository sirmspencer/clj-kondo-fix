(ns clj-kondo-fix.rules.equals-expected-position-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-equals-expected-position
  (testing "constant on right side: swap to left"
    (let [result (assert-fix fixes/fix-equals-expected-position-in-file
                             (fixture-path "equals-expected-position" "single-in")
                             [:equals-expected-position] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-expected-position" "single-out")) (:content result))))))
