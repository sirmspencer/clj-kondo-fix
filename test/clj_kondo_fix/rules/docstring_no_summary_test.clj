(ns clj-kondo-fix.rules.docstring-no-summary-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-docstring-no-summary
  (testing "capitalizes first char and adds period"
    (let [result (assert-fix fixes/fix-docstring-no-summary-in-file
                             (fixture-path "docstring_no_summary" "basic-fix-in")
                             [:docstring-no-summary] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "docstring_no_summary" "basic-fix-out")) (:content result)))))

  (testing "already correct — no finding"
    (let [result (assert-no-finding fixes/fix-docstring-no-summary-in-file
                                    (fixture-path "docstring_no_summary" "already-correct")
                                    [:docstring-no-summary])]
      (is (zero? (:fixed result))))))
