(ns clj-kondo-fix.rules.keyword-binding-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-keyword-binding
  (testing "{:keys [:a]} → {:keys [a]}"
    (let [result (assert-fix fixes/fix-keyword-binding-in-file
                             (fixture-path "keyword-binding" "keyword-in")
                             [:keyword-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "keyword-binding" "keyword-out")) (:content result)))))

  (testing "already a symbol — no finding"
    (let [result (assert-no-finding fixes/fix-keyword-binding-in-file
                                    (fixture-path "keyword-binding" "no-change")
                                    [:keyword-binding])]
      (is (zero? (:fixed result))))))
