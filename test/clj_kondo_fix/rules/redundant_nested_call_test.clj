(ns clj-kondo-fix.rules.redundant-nested-call-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-redundant-nested-call
  (testing "(+ 1 2 (+ 1 2 3)) → (+ 1 2 1 2 3)"
    (let [result (assert-fix fixes/fix-redundant-nested-call-in-file
                             (fixture-path "redundant-nested-call" "plus-in")
                             [:redundant-nested-call] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-nested-call" "plus-out")) (:content result)))))

  (testing "no nested call — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-nested-call-in-file
                                    (fixture-path "redundant-nested-call" "no-change")
                                    [:redundant-nested-call])]
      (is (zero? (:fixed result))))))
