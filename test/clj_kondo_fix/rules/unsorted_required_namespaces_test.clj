(ns clj-kondo-fix.rules.unsorted-required-namespaces-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-unsorted-required-namespaces
  (testing "single line require: reorder namespaces"
    (let [result (assert-fix fixes/fix-unsorted-required-namespaces-in-file
                             (fixture-path "unsorted-required-namespaces" "single-line-in")
                             [:unsorted-required-namespaces] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unsorted-required-namespaces" "single-line-out")) (:content result)))))

  (testing "multi line require with :require on its own line"
    (let [result (assert-fix fixes/fix-unsorted-required-namespaces-in-file
                             (fixture-path "unsorted-required-namespaces" "multi-line-in")
                             [:unsorted-required-namespaces] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unsorted-required-namespaces" "multi-line-out")) (:content result)))))

  (testing "first require on same line as :require"
    (let [result (assert-fix fixes/fix-unsorted-required-namespaces-in-file
                             (fixture-path "unsorted-required-namespaces" "same-line-in")
                             [:unsorted-required-namespaces] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unsorted-required-namespaces" "same-line-out")) (:content result))))))
