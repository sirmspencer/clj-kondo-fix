(ns clj-kondo-fix.rules.unused-import-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-unused-import
  (testing "removes one unused import from group, leaves the other"
    (let [pred   #(str/ends-with? (:message %) "List")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-one-from-group-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-one-from-group-out")) (:content result)))))

  (testing "removes all unused imports from group"
    (let [result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-all-from-group-in")
                             [:unused-import] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-all-from-group-out")) (:content result)))))

  (testing "removes unused import from vector-style standalone import"
    (let [pred   #(str/ends-with? (:message %) "Foo")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-from-standalone-vector-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-from-standalone-vector-out")) (:content result)))))

  (testing "removes entire import group when last class removed — no bare [package] left"
    (let [result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-last-class-removes-group-in")
                             [:unused-import] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-last-class-removes-group-out")) (:content result)))))

  (testing "removes middle unused import — first and last preserved with correct spacing"
    (let [pred   #(str/ends-with? (:message %) "Instant")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-middle-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-middle-out")) (:content result)))))

  (testing "removes first unused import from group — rest preserved"
    (let [pred   #(str/ends-with? (:message %) "Date")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-first-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-first-out")) (:content result)))))

  (testing "removes last unused import from group — preceding preserved"
    (let [pred   #(str/ends-with? (:message %) "List")
          result (assert-fix fixes/fix-unused-import-in-file
                             (fixture-path "unused-import" "removes-last-in")
                             [:unused-import] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-import" "removes-last-out")) (:content result))))))
