(ns clj-kondo-fix.rules.cond-else-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-cond-else
  (testing "replace :default with :else"
    (let [result (assert-fix fixes/fix-cond-else-in-file
                             (fixture-path "cond-else" "simple-in")
                             [:cond-else] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "cond-else" "simple-out")) (:content result))))))
