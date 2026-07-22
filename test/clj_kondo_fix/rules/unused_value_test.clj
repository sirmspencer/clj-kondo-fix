(ns clj-kondo-fix.rules.unused-value-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-unused-value
  (testing "removes unused literals and expressions"
    (let [result (assert-fix fixes/fix-unused-value-in-file
                              (fixture-path "unused-value" "removes-unused-in")
                              [:unused-value] 4)]
      (is (= 4 (:fixed result)))
      (is (= (slurp (fixture-path "unused-value" "removes-unused-out")) (:content result)))))

  (testing "no change when all values are used"
    (assert-no-finding fixes/fix-unused-value-in-file
                       (fixture-path "unused-value" "no-unused")
                       [:unused-value])))
