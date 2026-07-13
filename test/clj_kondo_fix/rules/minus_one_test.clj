(ns clj-kondo-fix.rules.minus-one-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-minus-one
  (testing "(- x 1) → (dec x)"
    (let [result (assert-fix fixes/fix-minus-one-in-file
                             (fixture-path "minus-one" "second-arg-one-in")
                             [:minus-one] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "minus-one" "second-arg-one-out")) (:content result)))))

  (testing "already using dec — no finding"
    (let [result (assert-no-finding fixes/fix-minus-one-in-file
                                    (fixture-path "minus-one" "no-change")
                                    [:minus-one])]
      (is (zero? (:fixed result))))))
