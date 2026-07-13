(ns clj-kondo-fix.rules.not-nil?-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-not-nil
  (testing "(not (nil? x)) → (some? x)"
    (let [result (assert-fix fixes/fix-not-nil-in-file
                             (fixture-path "not-nil?" "not-in")
                             [:not-nil?] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "not-nil?" "not-out")) (:content result)))))

  (testing "(when-not (nil? x) ...) → (when (some? x) ...)"
    (let [result (assert-fix fixes/fix-not-nil-in-file
                             (fixture-path "not-nil?" "when-not-in")
                             [:not-nil?] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "not-nil?" "when-not-out")) (:content result)))))

  (testing "(if-not (nil? x) ...) → (if (some? x) ...)"
    (let [result (assert-fix fixes/fix-not-nil-in-file
                             (fixture-path "not-nil?" "if-not-in")
                             [:not-nil?] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "not-nil?" "if-not-out")) (:content result)))))

  (testing "already using some? — no finding"
    (let [result (assert-no-finding fixes/fix-not-nil-in-file
                                    (fixture-path "not-nil?" "no-change")
                                    [:not-nil?])]
      (is (zero? (:fixed result))))))
