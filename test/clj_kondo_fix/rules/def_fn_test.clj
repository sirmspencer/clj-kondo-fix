(ns clj-kondo-fix.rules.def-fn-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-def-fn
  (testing "rewrites def+fn to defn"
    (let [result (assert-fix fixes/fix-def-fn-in-file
                             (fixture-path "def-fn" "rewrites-def-fn-in")
                             [:def-fn] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "def-fn" "rewrites-def-fn-out")) (:content result)))))

  (testing "no change when already defn"
    (assert-no-finding fixes/fix-def-fn-in-file
                       (fixture-path "def-fn" "defn-no-change")
                       [:def-fn])))
