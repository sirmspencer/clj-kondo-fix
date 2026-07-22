(ns clj-kondo-fix.rules.use-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-skip]]))

(deftest test-use
  (testing "(:use [ns :only [syms]]) → (:require [ns :refer [syms]])"
    (let [result (assert-fix fixes/fix-use-in-file
                             (fixture-path "use" "use-only-in")
                             [:use] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "use" "use-only-out")) (:content result)))))

  (testing "bare (:use ns) → (:require [ns :refer :all])"
    (let [result (assert-fix fixes/fix-use-in-file
                             (fixture-path "use" "use-plain")
                             [:use] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "use" "use-plain-out")) (:content result))))))
