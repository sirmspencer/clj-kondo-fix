(ns clj-kondo-fix.rules.condition-always-true-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-condition-always-true
  (testing "(if 1 :then) → :then"
    (let [result (assert-fix fixes/fix-condition-always-true-in-file
                             (fixture-path "condition-always-true" "if-true-in")
                             [:condition-always-true] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "condition-always-true" "if-true-out")) (:content result)))))

  (testing "(if 1 :then :else) → :then"
    (let [result (assert-fix fixes/fix-condition-always-true-in-file
                             (fixture-path "condition-always-true" "if-true-else-in")
                             [:condition-always-true] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "condition-always-true" "if-true-else-out")) (:content result)))))

  (testing "(when 1 :body) → :body"
    (let [result (assert-fix fixes/fix-condition-always-true-in-file
                             (fixture-path "condition-always-true" "when-true-in")
                             [:condition-always-true] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "condition-always-true" "when-true-out")) (:content result)))))

  (testing "variable condition — no finding"
    (let [result (assert-no-finding fixes/fix-condition-always-true-in-file
                                    (fixture-path "condition-always-true" "no-change")
                                    [:condition-always-true])]
      (is (zero? (:fixed result))))))
