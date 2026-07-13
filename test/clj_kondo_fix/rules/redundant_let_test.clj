(ns clj-kondo-fix.rules.redundant-let-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-redundant-let
  (testing "single-line: no body"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "single-line-no-body-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "single-line-no-body-out")) (:content result)))))

  (testing "single-line: with body"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "single-line-with-body-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "single-line-with-body-out")) (:content result)))))

  (testing "multi-line: no body"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-no-body-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-no-body-out")) (:content result)))))

  (testing "multi-line: with body on its own line"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-body-own-line-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-body-own-line-out")) (:content result)))))

  (testing "multi-line: body inline with inner binding close"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-body-inline-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-body-inline-out")) (:content result)))))

  (testing "multi-line: multiple inner bindings"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-multiple-inner-bindings-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-multiple-inner-bindings-out")) (:content result)))))

  (testing "intermediate #_ discard form: moved before merged let"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "intermediate-discard-form-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (let [lines       (str/split-lines (:content result))
            discard-idx (first (keep-indexed #(when (str/includes? %2 "#_") %1) lines))
            let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
        (is (some? discard-idx))
        (is (some? let-idx))
        (is (< discard-idx let-idx)))))

  (testing "intermediate comment line: moved before merged let"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "intermediate-comment-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (let [lines       (str/split-lines (:content result))
            comment-idx (first (keep-indexed #(when (str/includes? %2 ";;") %1) lines))
            let-idx     (first (keep-indexed #(when (str/starts-with? (str/trimr %2) "(let") %1) lines))]
        (is (some? comment-idx))
        (is (some? let-idx))
        (is (< comment-idx let-idx)))
      (is (str/includes? (:content result) "body"))))

  (testing "outer let with multi-line binding vector — merges inner bindings"
    (let [result (assert-fix fixes/fix-redundant-let-in-file
                             (fixture-path "redundant-let" "multiline-no-body-outer-binding-in")
                             [:redundant-let] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-let" "multiline-no-body-outer-binding-out")) (:content result))))))
