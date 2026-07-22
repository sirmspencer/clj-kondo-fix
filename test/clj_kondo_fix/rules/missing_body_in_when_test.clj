(ns clj-kondo-fix.rules.missing-body-in-when-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-skip assert-no-finding]]))

(deftest test-missing-body-in-when
  (testing "removes standalone (when condition) form"
    (let [result (assert-fix fixes/fix-missing-body-in-when-in-file
                              (fixture-path "missing-body-in-when" "removes-standalone-in")
                              [:missing-body-in-when] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "missing-body-in-when" "removes-standalone-out")) (:content result)))))

  (testing "skips when condition is a function call"
    (assert-skip fixes/fix-missing-body-in-when-in-file
                 (fixture-path "missing-body-in-when" "skips-side-effect")
                 [:missing-body-in-when]))

  (testing "no change when body is present"
    (assert-no-finding fixes/fix-missing-body-in-when-in-file
                       (fixture-path "missing-body-in-when" "no-missing-body")
                       [:missing-body-in-when])))
