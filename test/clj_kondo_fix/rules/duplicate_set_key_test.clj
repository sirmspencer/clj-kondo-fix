(ns clj-kondo-fix.rules.duplicate-set-key-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-duplicate-set-key
  (testing "removes duplicate set element"
    (let [result (assert-fix fixes/fix-duplicate-set-key-in-file
                             (fixture-path "duplicate-set-key" "removes-duplicate-in")
                             [:duplicate-set-key] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-set-key" "removes-duplicate-out")) (:content result)))))

  (testing "no change when set has no duplicates"
    (assert-no-finding fixes/fix-duplicate-set-key-in-file
                       (fixture-path "duplicate-set-key" "no-duplicate")
                       [:duplicate-set-key])))
