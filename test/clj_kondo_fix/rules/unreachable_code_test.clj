(ns clj-kondo-fix.rules.unreachable-code-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-unreachable-code
  (testing "removes unreachable clause after :else in cond"
    (let [result (assert-fix fixes/fix-unreachable-code-in-file
                             (fixture-path "unreachable_code" "cond-after-else-in")
                             [:unreachable-code] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unreachable_code" "cond-after-else-out")) (:content result))))))
