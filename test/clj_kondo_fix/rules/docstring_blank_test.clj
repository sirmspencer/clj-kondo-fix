(ns clj-kondo-fix.rules.docstring-blank-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-docstring-blank
  (testing "remove blank docstring"
    (let [result (assert-fix fixes/fix-docstring-blank-in-file
                             (fixture-path "docstring-blank" "simple-in")
                             [:docstring-blank] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "docstring-blank" "simple-out")) (:content result))))))
