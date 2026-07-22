(ns clj-kondo-fix.rules.unquote-not-syntax-quoted-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-unquote-not-syntax-quoted
  (testing "removes ~ outside syntax-quote"
    (let [result (assert-fix fixes/fix-unquote-not-syntax-quoted-in-file
                              (fixture-path "unquote-not-syntax-quoted" "removes-tilde-in")
                              [:unquote-not-syntax-quoted] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unquote-not-syntax-quoted" "removes-tilde-out")) (:content result)))))

  (testing "removes ~@ splice outside syntax-quote"
    (let [result (assert-fix fixes/fix-unquote-not-syntax-quoted-in-file
                              (fixture-path "unquote-not-syntax-quoted" "removes-splice-in")
                              [:unquote-not-syntax-quoted] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unquote-not-syntax-quoted" "removes-splice-out")) (:content result)))))

  (testing "no change when ~ is inside a valid syntax-quote"
    (assert-no-finding fixes/fix-unquote-not-syntax-quoted-in-file
                       (fixture-path "unquote-not-syntax-quoted" "no-unquote")
                       [:unquote-not-syntax-quoted])))
