(ns clj-kondo-fix.rules.underscore-in-namespace-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-underscore-in-namespace
  (testing "replaces underscores with hyphens in ns declaration"
    (let [result (assert-fix fixes/fix-underscore-in-namespace-in-file
                              (fixture-path "underscore-in-namespace" "renames-ns-in")
                              [:underscore-in-namespace] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "underscore-in-namespace" "renames-ns-out")) (:content result)))))

  (testing "no change when namespace has no underscores"
    (assert-no-finding fixes/fix-underscore-in-namespace-in-file
                       (fixture-path "underscore-in-namespace" "no-underscore")
                       [:underscore-in-namespace])))
