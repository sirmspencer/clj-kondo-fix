(ns clj-kondo-fix.rules.missing-else-branch-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-missing-else-branch
  (testing "converts (if ...) to (when ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-out")) (:content result)))))

  (testing "converts (if-not ...) to (when-not ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-not-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-not-out")) (:content result)))))

  (testing "converts (if-let ...) to (when-let ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-let-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-let-out")) (:content result)))))

  (testing "converts (if-some ...) to (when-some ...)"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-if-some-in")
                             [:missing-else-branch] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-if-some-out")) (:content result)))))

  (testing "multiple if variants on same line all converted"
    (let [result (assert-fix fixes/fix-missing-else-branch-in-file
                             (fixture-path "missing-else-branch" "converts-multiple-variants-in")
                             [:missing-else-branch] 4)]
      (is (= 4 (:fixed result)))
      (is (= (slurp (fixture-path "missing-else-branch" "converts-multiple-variants-out")) (:content result)))))

  (testing "no change when else branch is present"
    (let [result (assert-no-finding fixes/fix-missing-else-branch-in-file
                                    (fixture-path "missing-else-branch" "else-branch-present")
                                    [:missing-else-branch])]
      (is (zero? (:fixed result))))))
