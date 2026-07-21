(ns clj-kondo-fix.rules.unused-excluded-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-unused-excluded-var
  (testing "removes unused var from :exclude vector and cleans up empty clause"
    (let [result (assert-fix fixes/fix-unused-excluded-var-in-file
                             (fixture-path "unused-excluded-var" "removes-excluded-var-in")
                             [:unused-excluded-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-excluded-var" "removes-excluded-var-out")) (:content result)))))

  (testing "no change when excluded var is used"
    (assert-no-finding fixes/fix-unused-excluded-var-in-file
                       (fixture-path "unused-excluded-var" "no-unused-exclude")
                       [:unused-excluded-var])))
