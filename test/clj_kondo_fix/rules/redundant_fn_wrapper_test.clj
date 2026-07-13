(ns clj-kondo-fix.rules.redundant-fn-wrapper-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-redundant-fn-wrapper
  (testing "#(identity %) → identity"
    (let [result (assert-fix fixes/fix-redundant-fn-wrapper-in-file
                             (fixture-path "redundant-fn-wrapper" "wrapper-in")
                             [:redundant-fn-wrapper] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "redundant-fn-wrapper" "wrapper-out")) (:content result)))))

  (testing "already using fn directly — no finding"
    (let [result (assert-no-finding fixes/fix-redundant-fn-wrapper-in-file
                                    (fixture-path "redundant-fn-wrapper" "no-change")
                                    [:redundant-fn-wrapper])]
      (is (zero? (:fixed result))))))
