(ns clj-kondo-fix.rules.dynamic-var-not-earmuffed-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-dynamic-var-not-earmuffed
  (testing "single dynamic var without earmuffs: add earmuffs"
    (let [result (assert-fix fixes/fix-dynamic-var-not-earmuffed-in-file
                             (fixture-path "dynamic-var-not-earmuffed" "single-in")
                             [:dynamic-var-not-earmuffed] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "dynamic-var-not-earmuffed" "single-out")) (:content result))))))
