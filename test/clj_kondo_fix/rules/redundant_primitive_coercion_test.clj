(ns clj-kondo-fix.rules.redundant-primitive-coercion-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-redundant-primitive-coercion
  (testing "(double (double 1)) → (double 1)"
    (let [result (assert-fix fixes/fix-redundant-primitive-coercion-in-file
                             (fixture-path "redundant-primitive-coercion" "coercion-in")
                             [:redundant-primitive-coercion :type-mismatch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-primitive-coercion" "coercion-out")) (:content result)))))

  (testing "no nested coercion — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-primitive-coercion-in-file
                                    (fixture-path "redundant-primitive-coercion" "no-change")
                                    [:redundant-primitive-coercion :type-mismatch])]
      (is (zero? (:fixed result))))))
