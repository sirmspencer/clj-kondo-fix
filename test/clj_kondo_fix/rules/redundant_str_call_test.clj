(ns clj-kondo-fix.rules.redundant-str-call-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-redundant-str-call
  (testing "single str arg: (str x) → x"
    (let [result (assert-fix fixes/fix-redundant-str-call-in-file
                             (fixture-path "redundant-str-call" "string-in")
                             [:redundant-str-call] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-str-call" "string-out")) (:content result)))))

  (testing "two str args — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-str-call-in-file
                                    (fixture-path "redundant-str-call" "no-change")
                                    [:redundant-str-call])]
      (is (zero? (:fixed result)))))

  (testing "single untyped arg — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-str-call-in-file
                                    (fixture-path "redundant-str-call" "single-arg-var-no-change")
                                    [:redundant-str-call])]
      (is (zero? (:fixed result)))))

  (testing "two string args — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-str-call-in-file
                                    (fixture-path "redundant-str-call" "multi-string-arg-no-change")
                                    [:redundant-str-call])]
      (is (zero? (:fixed result))))))
