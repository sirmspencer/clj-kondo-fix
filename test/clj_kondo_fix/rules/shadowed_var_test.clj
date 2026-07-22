(ns clj-kondo-fix.rules.shadowed-var-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-skip assert-no-finding]]))

(deftest test-shadowed-var
  (testing "renames let binding shadowing a core var"
    (let [result (assert-fix fixes/fix-shadowed-var-in-file
                              (fixture-path "shadowed-var" "renames-let-binding-in")
                              [:shadowed-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "shadowed-var" "renames-let-binding-out")) (:content result)))))

  (testing "renames fn param shadowing a core var"
    (let [result (assert-fix fixes/fix-shadowed-var-in-file
                              (fixture-path "shadowed-var" "renames-fn-param-in")
                              [:shadowed-var] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "shadowed-var" "renames-fn-param-out")) (:content result)))))

  (testing "skips :keys destructuring (binding coupled to map key)"
    (assert-skip fixes/fix-shadowed-var-in-file
                 (fixture-path "shadowed-var" "skips-keys-destructuring")
                 [:shadowed-var]))

  (testing "no change when no shadowing"
    (assert-no-finding fixes/fix-shadowed-var-in-file
                       (fixture-path "shadowed-var" "no-shadowing")
                       [:shadowed-var])))
