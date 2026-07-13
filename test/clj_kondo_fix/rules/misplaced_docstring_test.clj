(ns clj-kondo-fix.rules.misplaced-docstring-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-skip assert-no-finding]]))

(deftest test-misplaced-docstring
  (testing "moves docstring before param vector (multi-line form)"
    (let [result (assert-fix fixes/fix-misplaced-docstring-in-file
                             (fixture-path "misplaced-docstring" "moves-before-params-in")
                             [:misplaced-docstring] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "misplaced-docstring" "moves-before-params-out")) (:content result)))))

  (testing "single-line form is skipped — finding is on row 1, def-line-idx = -1"
    (let [result (assert-skip fixes/fix-misplaced-docstring-in-file
                              (fixture-path "misplaced-docstring" "single-line-skip")
                              [:misplaced-docstring])]
      (is (str/includes? (:content result) "(defn f [x] \"dude\" x)"))))

  (testing "correctly placed docstring is unchanged"
    (let [result (assert-no-finding fixes/fix-misplaced-docstring-in-file
                                    (fixture-path "misplaced-docstring" "correctly-placed")
                                    [:misplaced-docstring])]
      (is (zero? (:fixed result)))))

  (testing "comment between params and docstring — safe skip"
    (let [result (assert-skip fixes/fix-misplaced-docstring-in-file
                              (fixture-path "misplaced-docstring" "comment-between-skip")
                              [:misplaced-docstring])]
      (is (str/includes? (:content result) ";; explains x"))
      (is (str/includes? (:content result) "\"doc\""))))

  (testing "multi-line defn signature: params on separate line — safe skip"
    (let [result (assert-skip fixes/fix-misplaced-docstring-in-file
                              (fixture-path "misplaced-docstring" "multiline-sig-skip")
                              [:misplaced-docstring])]
      (is (str/includes? (:content result) "(defn f"))
      (is (str/includes? (:content result) "  [x]"))
      (is (str/includes? (:content result) "\"doc\"")))))
