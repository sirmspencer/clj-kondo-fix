(ns clj-kondo-fix.rules.duplicate-field-name-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-duplicate-field-name
  (testing "deftype with duplicate field"
    (let [result (assert-fix fixes/fix-duplicate-field-name-in-file
                             (fixture-path "duplicate-field-name" "deftype-duplicate-in")
                             [:duplicate-field] 2)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-field-name" "deftype-duplicate-out")) (:content result)))))

  (testing "defrecord with duplicate field"
    (let [result (assert-fix fixes/fix-duplicate-field-name-in-file
                             (fixture-path "duplicate-field-name" "defrecord-duplicate-in")
                             [:duplicate-field] 2)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-field-name" "defrecord-duplicate-out")) (:content result)))))

  (testing "multiple duplicate fields"
    (let [result (assert-fix fixes/fix-duplicate-field-name-in-file
                             (fixture-path "duplicate-field-name" "multiple-duplicates-in")
                             [:duplicate-field] 4)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-field-name" "multiple-duplicates-out")) (:content result)))))

  (testing "no change when no duplicates"
    (let [result (assert-no-finding fixes/fix-duplicate-field-name-in-file
                                    (fixture-path "duplicate-field-name" "no-duplicates")
                                    [:duplicate-field])]
      (is (zero? (:fixed result))))))
