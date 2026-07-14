(ns clj-kondo-fix.rules.equals-float-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-equals-float
  (testing "(= 0.1 x) → (== 0.1 x)"
    (let [result (assert-fix fixes/fix-equals-float-in-file
                             (fixture-path "equals-float" "float-in")
                             [:equals-float] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-float" "float-out")) (:content result)))))

  (testing "(= x 0.5) → (== x 0.5)"
    (let [result (assert-fix fixes/fix-equals-float-in-file
                             (fixture-path "equals-float" "float-second-in")
                             [:equals-float] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-float" "float-second-out")) (:content result)))))

  (testing "(= 1 2) — not float — no finding"
    (let [result (assert-no-finding fixes/fix-equals-float-in-file
                                    (fixture-path "equals-float" "no-change")
                                    [:equals-float])]
      (is (zero? (:fixed result))))))
