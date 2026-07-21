(ns clj-kondo-fix.rules.java-static-field-call-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-java-static-field-call
  (testing "strips wrapping parens from static field reference"
    (let [result (assert-fix fixes/fix-java-static-field-call-in-file
                             (fixture-path "java-static-field-call" "strips-parens-in")
                             [:java-static-field-call] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "java-static-field-call" "strips-parens-out")) (:content result)))))

  (testing "no change for normal method calls"
    (assert-no-finding fixes/fix-java-static-field-call-in-file
                       (fixture-path "java-static-field-call" "normal-method-call")
                       [:java-static-field-call])))
