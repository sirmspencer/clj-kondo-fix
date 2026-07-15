(ns clj-kondo-fix.rules.uninitialized-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-uninitialized-var
  (testing "single uninitialized var: add nil"
    (let [result (assert-fix fixes/fix-uninitialized-var-in-file
                             (fixture-path "uninitialized-var" "single-in")
                             [:uninitialized-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "uninitialized-var" "single-out")) (:content result))))))

