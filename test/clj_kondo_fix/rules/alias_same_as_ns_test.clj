(ns clj-kondo-fix.rules.alias-same-as-ns-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-alias-same-as-ns
  (testing "removes redundant :as when alias equals namespace name"
    (let [result (assert-fix fixes/fix-alias-same-as-ns-in-file
                             (fixture-path "alias-same-as-ns" "removes-redundant-as-in")
                             [:alias-same-as-ns] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "alias-same-as-ns" "removes-redundant-as-out")) (:content result)))))

  (testing "no change when alias differs from namespace name"
    (assert-no-finding fixes/fix-alias-same-as-ns-in-file
                       (fixture-path "alias-same-as-ns" "alias-differs")
                       [:alias-same-as-ns])))
