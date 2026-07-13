(ns clj-kondo-fix.rules.plus-one-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-plus-one
  (testing "1 as first arg: (+ 1 x) → (inc x)"
    (let [result (assert-fix fixes/fix-plus-one-in-file
                             (fixture-path "plus-one" "first-arg-one-in")
                             [:plus-one] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "plus-one" "first-arg-one-out")) (:content result)))))

  (testing "1 as second arg: (+ x 1) → (inc x)"
    (let [result (assert-fix fixes/fix-plus-one-in-file
                             (fixture-path "plus-one" "second-arg-one-in")
                             [:plus-one] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "plus-one" "second-arg-one-out")) (:content result)))))

  (testing "already using inc — no finding"
    (let [result (assert-no-finding fixes/fix-plus-one-in-file
                                    (fixture-path "plus-one" "no-change")
                                    [:plus-one])]
      (is (zero? (:fixed result))))))
