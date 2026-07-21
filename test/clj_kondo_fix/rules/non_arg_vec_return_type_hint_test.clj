(ns clj-kondo-fix.rules.non-arg-vec-return-type-hint-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-non-arg-vec-return-type-hint
  (testing "moves type hint from fn name to arg vector"
    (let [result (assert-fix fixes/fix-non-arg-vec-return-type-hint-in-file
                             (fixture-path "non-arg-vec-return-type-hint" "moves-hint-in")
                             [:non-arg-vec-return-type-hint] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "non-arg-vec-return-type-hint" "moves-hint-out")) (:content result)))))

  (testing "no change when hint is already on arg vector"
    (assert-no-finding fixes/fix-non-arg-vec-return-type-hint-in-file
                       (fixture-path "non-arg-vec-return-type-hint" "correct-hint")
                       [:non-arg-vec-return-type-hint])))
