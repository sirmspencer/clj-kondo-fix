(ns clj-kondo-fix.rules.single-logical-operand-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-single-logical-operand
  (testing "(and x) → x"
    (let [result (assert-fix fixes/fix-single-logical-operand-in-file
                             (fixture-path "single-logical-operand" "single-and-in")
                             [:single-logical-operand] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "single-logical-operand" "single-and-out")) (:content result)))))

  (testing "(or x) → x"
    (let [result (assert-fix fixes/fix-single-logical-operand-in-file
                             (fixture-path "single-logical-operand" "single-or-in")
                             [:single-logical-operand] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "single-logical-operand" "single-or-out")) (:content result)))))

  (testing "multiple operands — no finding"
    (let [result (assert-no-finding fixes/fix-single-logical-operand-in-file
                                    (fixture-path "single-logical-operand" "no-change")
                                    [:single-logical-operand])]
      (is (zero? (:fixed result))))))
