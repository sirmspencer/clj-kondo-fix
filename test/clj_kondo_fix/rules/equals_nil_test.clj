(ns clj-kondo-fix.rules.equals-nil-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-equals-nil
  (testing "nil as first arg: (= nil x) → (nil? x)"
    (let [result (assert-fix fixes/fix-equals-nil-in-file
                             (fixture-path "equals-nil" "first-arg-nil-in")
                             [:equals-nil] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-nil" "first-arg-nil-out")) (:content result)))))

  (testing "nil as second arg: (= x nil) → (nil? x)"
    (let [result (assert-fix fixes/fix-equals-nil-in-file
                             (fixture-path "equals-nil" "second-arg-nil-in")
                             [:equals-nil] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "equals-nil" "second-arg-nil-out")) (:content result)))))

  (testing "already using nil? — no finding"
    (let [result (assert-no-finding fixes/fix-equals-nil-in-file
                                    (fixture-path "equals-nil" "no-change")
                                    [:equals-nil])]
      (is (zero? (:fixed result))))))
