(ns clj-kondo-fix.rules.unused-referred-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix]]))

(deftest test-unused-referred-var
  (testing "removes single unused referred var, keeps used one"
    (let [pred   #(str/includes? (:message %) "ends-with?")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-one-keeps-other-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-one-keeps-other-out")) (:content result)))))

  (testing "works with vars whose names end in ? (word boundary)"
    (let [pred   #(str/includes? (:message %) "ends-with?")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "predicate-var-name-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "predicate-var-name-out")) (:content result)))))

  (testing "removes :refer clause when all vars removed"
    (let [result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-in")
                             [:unused-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-out")) (:content result)))))

  (testing "removes :refer clause when all vars removed — multi-require ns"
    (let [result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-multi-in")
                             [:unused-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-refer-clause-when-all-removed-multi-out")) (:content result)))))

  (testing "removes entire require entry when only referred var is removed"
    (let [result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-entire-entry-when-only-referred-in")
                             [:unused-referred-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-entire-entry-when-only-referred-out")) (:content result)))))

  (testing "space preserved when removing middle var from :refer vector"
    (let [pred   #(str/includes? (:message %) "run-cucumber")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-middle-var-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-middle-var-out")) (:content result)))))

  (testing "space preserved when removing first var from :refer vector"
    (let [pred   #(str/includes? (:message %) "clojure.string/join")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-first-var-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-first-var-out")) (:content result)))))

  (testing "space preserved when removing last var from :refer vector"
    (let [pred   #(str/includes? (:message %) "clojure.string/split")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "removes-last-var-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "removes-last-var-out")) (:content result)))))

  (testing "multi-line :refer vector: removes var from its own line"
    (let [pred   #(str/includes? (:message %) "ends-with?")
          result (assert-fix fixes/fix-unused-referred-var-in-file
                             (fixture-path "unused-referred-var" "multiline-refer-vector-in")
                             [:unused-referred-var] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-referred-var" "multiline-refer-vector-out")) (:content result))))))
