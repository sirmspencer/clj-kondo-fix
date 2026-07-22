(ns clj-kondo-fix.rules.unbound-destructuring-default-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-unbound-destructuring-default
  (testing "removes unbound :or entry and cleans up empty :or"
    (let [result (assert-fix fixes/fix-unbound-destructuring-default-in-file
                             (fixture-path "unbound-destructuring-default" "removes-unbound-in")
                             [:unbound-destructuring-default] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unbound-destructuring-default" "removes-unbound-out")) (:content result)))))

  (testing "removes unbound :or entry, keeps valid one"
    (let [result (assert-fix fixes/fix-unbound-destructuring-default-in-file
                             (fixture-path "unbound-destructuring-default" "keeps-valid-removes-unbound-in")
                             [:unbound-destructuring-default] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unbound-destructuring-default" "keeps-valid-removes-unbound-out")) (:content result)))))

  (testing "no change when all :or entries are bound"
    (assert-no-finding fixes/fix-unbound-destructuring-default-in-file
                       (fixture-path "unbound-destructuring-default" "no-unbound")
                       [:unbound-destructuring-default])))
