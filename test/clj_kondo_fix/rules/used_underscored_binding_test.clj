(ns clj-kondo-fix.rules.used-underscored-binding-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-used-underscored-binding
  (testing "strips underscore from binding name and all usages"
    (let [result (assert-fix fixes/fix-used-underscored-binding-in-file
                             (fixture-path "used_underscored_binding" "fn-param-in")
                             [:used-underscored-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "used_underscored_binding" "fn-param-out")) (:content result))))))
