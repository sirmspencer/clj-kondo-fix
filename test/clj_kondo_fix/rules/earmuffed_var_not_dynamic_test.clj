(ns clj-kondo-fix.rules.earmuffed-var-not-dynamic-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-earmuffed-var-not-dynamic
  (testing "single earmuffed var without ^:dynamic: add ^:dynamic"
    (let [result (assert-fix fixes/fix-earmuffed-var-not-dynamic-in-file
                             (fixture-path "earmuffed-var-not-dynamic" "single-in")
                             [:earmuffed-var-not-dynamic] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "earmuffed-var-not-dynamic" "single-out")) (:content result))))))
