(ns clj-kondo-fix.rules.is-message-not-string-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding]]))

(deftest test-is-message-not-string
  (testing "wraps keyword in double-quoted string"
    (let [result (assert-fix fixes/fix-is-message-not-string-in-file
                             (fixture-path "is-message-not-string" "wraps-in-string-in")
                             [:is-message-not-string] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "is-message-not-string" "wraps-in-string-out")) (:content result)))))

  (testing "no change when message is already a string"
    (assert-no-finding fixes/fix-is-message-not-string-in-file
                       (fixture-path "is-message-not-string" "already-string")
                       [:is-message-not-string])))
