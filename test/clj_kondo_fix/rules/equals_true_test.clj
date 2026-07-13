(ns clj-kondo-fix.rules.equals-true-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-equals-true
  (testing "true as first arg: (= true x) → (true? x)"
    (let [result (assert-fix fixes/fix-equals-true-in-file
                             (fixture-path "equals-true" "first-arg-true-in")
                             [:equals-true] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-true" "first-arg-true-out")) (:content result)))))

  (testing "true as second arg: (= x true) → (true? x)"
    (let [result (assert-fix fixes/fix-equals-true-in-file
                             (fixture-path "equals-true" "second-arg-true-in")
                             [:equals-true] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-true" "second-arg-true-out")) (:content result)))))

  (testing "already using true? — no finding"
    (let [result (assert-no-finding fixes/fix-equals-true-in-file
                                    (fixture-path "equals-true" "no-change")
                                    [:equals-true])]
      (is (zero? (:fixed result))))))
