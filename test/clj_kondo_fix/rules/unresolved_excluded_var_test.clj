(ns clj-kondo-fix.rules.unresolved-excluded-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-unresolved-excluded-var
  (testing "removes unresolved var from :exclude vector and cleans up empty clause"
    (let [result (assert-fix fixes/fix-unresolved-excluded-var-in-file
                             (fixture-path "unresolved-excluded-var" "removes-unresolved-var-in")
                             [:unresolved-excluded-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unresolved-excluded-var" "removes-unresolved-var-out")) (:content result)))))

  (testing "no change when excluded var resolves"
    (assert-no-finding fixes/fix-unresolved-excluded-var-in-file
                       (fixture-path "unresolved-excluded-var" "no-unresolved")
                       [:unresolved-excluded-var])))
