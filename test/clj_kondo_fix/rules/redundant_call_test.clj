(ns clj-kondo-fix.rules.redundant-call-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-redundant-call
  (testing "(-> 1) → 1"
    (let [result (assert-fix fixes/fix-redundant-call-in-file
                             (fixture-path "redundant-call" "thread-in")
                             [:redundant-call] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-call" "thread-out")) (:content result)))))

  (testing "(merge {:a 1}) → {:a 1}"
    (let [result (assert-fix fixes/fix-redundant-call-in-file
                             (fixture-path "redundant-call" "merge-in")
                             [:redundant-call] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-call" "merge-out")) (:content result)))))

  (testing "two args — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-call-in-file
                                    (fixture-path "redundant-call" "no-change")
                                    [:redundant-call])]
      (is (zero? (:fixed result))))))
