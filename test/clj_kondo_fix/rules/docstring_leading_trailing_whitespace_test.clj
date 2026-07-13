(ns clj-kondo-fix.rules.docstring-leading-trailing-whitespace-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-docstring-whitespace
  (testing "trim docstring whitespace"
    (let [result (assert-fix fixes/fix-docstring-leading-trailing-whitespace-in-file
                             (fixture-path "docstring-leading-trailing-whitespace" "docstring-in")
                             [:docstring-leading-trailing-whitespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "docstring-leading-trailing-whitespace" "docstring-out")) (:content result)))))

  (testing "already clean — no finding"
    (let [result (assert-no-finding fixes/fix-docstring-leading-trailing-whitespace-in-file
                                    (fixture-path "docstring-leading-trailing-whitespace" "no-change")
                                    [:docstring-leading-trailing-whitespace])]
      (is (zero? (:fixed result))))))
