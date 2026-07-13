(ns clj-kondo-fix.rules.if-nil-return-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-if-nil-return
  (testing "(if x nil y) → (when-not x y)"
    (let [result (assert-fix fixes/fix-if-nil-return-in-file
                             (fixture-path "if-nil-return" "then-nil-in")
                             [:if-nil-return] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "if-nil-return" "then-nil-out")) (:content result)))))

  (testing "(if x y nil) → (when x y)"
    (let [result (assert-fix fixes/fix-if-nil-return-in-file
                             (fixture-path "if-nil-return" "else-nil-in")
                             [:if-nil-return] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "if-nil-return" "else-nil-out")) (:content result)))))

  (testing "no nil branch — no finding"
    (let [result (assert-no-finding fixes/fix-if-nil-return-in-file
                                    (fixture-path "if-nil-return" "no-change")
                                    [:if-nil-return])]
      (is (zero? (:fixed result))))))
