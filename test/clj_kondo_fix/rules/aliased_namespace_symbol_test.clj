(ns clj-kondo-fix.rules.aliased-namespace-symbol-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-aliased-namespace-symbol
  (testing "replaces full namespace with alias"
    (let [result (assert-fix fixes/fix-aliased-namespace-symbol-in-file
                             (fixture-path "aliased-namespace-symbol" "uses-full-ns-in")
                             [:aliased-namespace-symbol] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "aliased-namespace-symbol" "uses-full-ns-out")) (:content result)))))

  (testing "no change when already using alias"
    (assert-no-finding fixes/fix-aliased-namespace-symbol-in-file
                       (fixture-path "aliased-namespace-symbol" "already-aliased")
                       [:aliased-namespace-symbol])))
