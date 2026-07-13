(ns clj-kondo-fix.rules.unused-private-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-unused-private-var
  (testing "removes defn- form entirely"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-defn-form-in")
                             [:unused-private-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-defn-form-out")) (:content result)))))

  (testing "removes def ^:private form entirely"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-def-private-form-in")
                             [:unused-private-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-def-private-form-out")) (:content result)))))

  (testing "removes multi-line def ^:private form"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-multiline-def-private-in")
                             [:unused-private-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-multiline-def-private-out")) (:content result)))))

  (testing "removes two independent private vars"
    (let [result (assert-fix fixes/fix-unused-private-var-in-file
                             (fixture-path "unused-private-var" "removes-two-private-vars-in")
                             [:unused-private-var] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-private-var" "removes-two-private-vars-out")) (:content result))))))
