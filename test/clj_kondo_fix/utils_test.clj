(ns clj-kondo-fix.utils-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.utils :refer [find-matching-bracket-across-lines]]))

(deftest test-find-matching-bracket
  (testing "finds matching paren across lines"
    (is (= [3 0] (find-matching-bracket-across-lines ["(foo" "  (bar" "    baz)" ")"] 0 0))))
  (testing "handles paren depth correctly"
    (is (= [1 3] (find-matching-bracket-across-lines ["(let [x (foo bar)]" "  x)"] 0 0))))
  (testing "skips brackets inside strings"
    (is (= [1 3] (find-matching-bracket-across-lines ["(let [x \"hello [world]\"]" "  x)"] 0 0))))
  (testing "returns nil for non-bracket start"
    (is (nil? (find-matching-bracket-across-lines ["foo bar"] 0 0)))))
