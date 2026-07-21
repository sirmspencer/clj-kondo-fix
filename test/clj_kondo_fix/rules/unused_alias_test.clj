(ns clj-kondo-fix.rules.unused-alias-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-unused-alias
  (testing "removes :as alias when :refer is still present"
    (let [result (assert-fix fixes/fix-unused-alias-in-file
                             (fixture-path "unused-alias" "removes-alias-keeps-refer-in")
                             [:unused-alias] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-alias" "removes-alias-keeps-refer-out")) (:content result)))))

  (testing "no change when alias is used"
    (assert-no-finding fixes/fix-unused-alias-in-file
                       (fixture-path "unused-alias" "alias-used")
                       [:unused-alias])))
