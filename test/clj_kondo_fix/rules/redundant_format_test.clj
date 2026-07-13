(ns clj-kondo-fix.rules.redundant-format-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-redundant-format
  (testing "(format \"hello\") → \"hello\""
    (let [result (assert-fix fixes/fix-redundant-format-in-file
                             (fixture-path "redundant-format" "format-in")
                             [:redundant-format] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-format" "format-out")) (:content result)))))

  (testing "format with specifier — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-format-in-file
                                    (fixture-path "redundant-format" "no-change")
                                    [:redundant-format])]
      (is (zero? (:fixed result))))))
