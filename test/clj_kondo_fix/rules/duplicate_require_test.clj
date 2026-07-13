(ns clj-kondo-fix.rules.duplicate-require-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-duplicate-require
  (testing "case 1: only first alias used — remove reported duplicate, no renames"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "first-alias-used-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "first-alias-used-out")) (:content result)))))

  (testing "case 2: only duplicate alias used — remove first entry, no renames"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "second-alias-used-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "second-alias-used-out")) (:content result)))))

  (testing "case 3: both aliases used — keep longer, rename shorter usages, remove shorter entry"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "both-aliases-used-keep-longer-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "both-aliases-used-keep-longer-out")) (:content result)))))

  (testing "case 3 tie: both aliases same length — keep first (shorter or equal wins)"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "both-aliases-used-tie-keep-first-in")
                             [:duplicate-require] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "both-aliases-used-tie-keep-first-out")) (:content result)))))

  (testing "case 4: neither alias used — remove reported duplicate, first entry remains"
    (let [result (assert-fix fixes/fix-duplicate-require-in-file
                             (fixture-path "duplicate-require" "neither-alias-used-in")
                             [:duplicate-require] 1)]
      (is (pos? (:fixed result)))
      (is (= (slurp (fixture-path "duplicate-require" "neither-alias-used-out")) (:content result))))))
