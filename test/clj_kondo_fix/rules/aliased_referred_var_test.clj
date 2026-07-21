(ns clj-kondo-fix.rules.aliased-referred-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-aliased-referred-var
  (testing "replaces alias-qualified usage with bare referred var"
    (let [result (assert-fix fixes/fix-aliased-referred-var-in-file
                             (fixture-path "aliased_referred_var" "uses-alias-in")
                             [:aliased-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "aliased_referred_var" "uses-alias-out")) (:content result))))))
