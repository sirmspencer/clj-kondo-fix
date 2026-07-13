(ns clj-kondo-fix.rules.equals-false-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-equals-false
  (testing "false as first arg: (= false x) → (false? x)"
    (let [result (assert-fix fixes/fix-equals-false-in-file
                             (fixture-path "equals-false" "first-arg-false-in")
                             [:equals-false] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-false" "first-arg-false-out")) (:content result)))))

  (testing "false as second arg: (= x false) → (false? x)"
    (let [result (assert-fix fixes/fix-equals-false-in-file
                             (fixture-path "equals-false" "second-arg-false-in")
                             [:equals-false] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-false" "second-arg-false-out")) (:content result)))))

  (testing "already using false? — no finding"
    (let [result (assert-no-finding fixes/fix-equals-false-in-file
                                    (fixture-path "equals-false" "no-change")
                                    [:equals-false])]
      (is (zero? (:fixed result))))))
