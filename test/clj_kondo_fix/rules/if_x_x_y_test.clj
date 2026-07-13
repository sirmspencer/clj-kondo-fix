(ns clj-kondo-fix.rules.if-x-x-y-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-if-x-x-y
  (testing "(if x x y) → (or x y)"
    (let [result (assert-fix fixes/fix-if-x-x-y-in-file
                             (fixture-path "if-x-x-y" "simple-in")
                             [:if-x-x-y] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "if-x-x-y" "simple-out")) (:content result)))))

  (testing "different condition and then — no finding"
    (let [result (assert-no-finding fixes/fix-if-x-x-y-in-file
                                    (fixture-path "if-x-x-y" "no-change")
                                    [:if-x-x-y])]
      (is (zero? (:fixed result))))))
