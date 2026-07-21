(ns clj-kondo-fix.rules.single-operand-comparison-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-single-operand-comparison
  (testing "replaces single-operand comparisons with true"
    (let [result (assert-fix fixes/fix-single-operand-comparison-in-file
                             (fixture-path "single-operand-comparison" "removes-comparison-in")
                             [:single-operand-comparison] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "single-operand-comparison" "removes-comparison-out")) (:content result)))))

  (testing "no change when comparison has multiple operands"
    (assert-no-finding fixes/fix-single-operand-comparison-in-file
                       (fixture-path "single-operand-comparison" "normal-comparison")
                       [:single-operand-comparison])))
