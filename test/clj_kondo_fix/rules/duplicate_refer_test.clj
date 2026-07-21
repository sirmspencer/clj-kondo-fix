(ns clj-kondo-fix.rules.duplicate-refer-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-duplicate-refer
  (testing "removes second occurrence of duplicate var in :refer vector"
    (let [result (assert-fix fixes/fix-duplicate-refer-in-file
                             (fixture-path "duplicate-refer" "removes-duplicate-in")
                             [:duplicate-refer] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-refer" "removes-duplicate-out")) (:content result)))))

  (testing "no change when no duplicate refers"
    (assert-no-finding fixes/fix-duplicate-refer-in-file
                       (fixture-path "duplicate-refer" "no-duplicate")
                       [:duplicate-refer])))
