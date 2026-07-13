(ns clj-kondo-fix.rules.single-key-in-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-single-key-in
  (testing "simple map: (get-in m [:k]) → (get m :k)"
    (let [result (assert-fix fixes/fix-single-key-in-in-file
                             (fixture-path "single-key-in" "simple-map-in")
                             [:single-key-in] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "single-key-in" "simple-map-out")) (:content result)))))

  (testing "complex map: (get-in {:a [1 2]} [:a]) → (get {:a [1 2]} :a)"
    (let [result (assert-fix fixes/fix-single-key-in-in-file
                             (fixture-path "single-key-in" "complex-map-in")
                             [:single-key-in] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "single-key-in" "complex-map-out")) (:content result)))))

  (testing "two keys — no finding"
    (let [result (assert-no-finding fixes/fix-single-key-in-in-file
                                    (fixture-path "single-key-in" "no-change")
                                    [:single-key-in])]
      (is (zero? (:fixed result))))))
