(ns clj-kondo-fix.rules.unused-binding-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-skip assert-no-finding]]))

(deftest test-unused-binding
  (testing "prefixes simple unused fn param with underscore"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "simple-fn-param-in")
                             [:unused-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "simple-fn-param-out")) (:content result)))))

  (testing "let binding is skipped by default — too risky (may be side-effectful)"
    (assert-skip fixes/fix-unused-binding-in-file
                 (fixture-path "unused-binding" "let-binding-skip")
                 [:unused-binding]))

  (testing "no change when binding is used"
    (let [result (assert-no-finding fixes/fix-unused-binding-in-file
                                    (fixture-path "unused-binding" "binding-used")
                                    [:unused-binding])]
      (is (zero? (:fixed result)))))

  (testing "removes unused namespaced key from :keys vector"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "namespaced-key-in")
                             [:unused-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "namespaced-key-out")) (:content result)))))

  (testing ":as bindings are not reported by :unused-binding"
    (let [result (assert-no-finding fixes/fix-unused-binding-in-file
                                    (fixture-path "unused-binding" "as-alias-no-finding")
                                    [:unused-binding])]
      (is (zero? (:fixed result)))))

  (testing "loop/for bindings are skipped by default"
    (assert-skip fixes/fix-unused-binding-in-file
                 (fixture-path "unused-binding" "loop-binding-skip")
                 [:unused-binding]))

  (testing ":as clause in destructuring: removed when unused"
    (let [pred   #(str/includes? (:message %) "config")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "as-clause-removed-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "as-clause-removed-out")) (:content result)))))

  (testing ":as clause: all concrete bindings unused → map collapses to _as-name"
    (let [pred   #(str/includes? (:message %) " db")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "as-clause-collapses-to-name-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "as-clause-collapses-to-name-out")) (:content result)))))

  (testing "map inside function call (let rhs) is NOT collapsed — not in destructuring position"
    (let [pred   #(str/includes? (:message %) " query")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "fn-call-arg-not-collapsed-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "fn-call-arg-not-collapsed-out")) (:content result)))))

  (testing ":as and concrete binding both unused → :as removed, map collapses to _"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "as-and-binding-both-unused-in")
                             [:unused-binding] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "as-and-binding-both-unused-out")) (:content result)))))

  (testing "multi-line: :as removed, map collapses to _"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "multiline-as-and-binding-both-unused-in")
                             [:unused-binding] 2)]
      (is (= 2 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "multiline-as-and-binding-both-unused-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes unused key from :keys vector"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-first-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-first-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes first key, rest preserved"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-first-in")
                             [:unused-binding] 1 pred)]
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-first-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes middle key, space preserved"
    (let [pred   #(str/includes? (:message %) " y")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-middle-in")
                             [:unused-binding] 1 pred)]
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-middle-out")) (:content result)))))

  (testing "keys-destr in fn-param: removes last key, preceding preserved"
    (let [pred   #(str/includes? (:message %) " z")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-removes-last-in")
                             [:unused-binding] 1 pred)]
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-removes-last-out")) (:content result)))))

  (testing "keys-destr in fn-param: only key removed, entire map collapses to _"
    (let [result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-only-key-collapses-in")
                             [:unused-binding] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-only-key-collapses-out")) (:content result)))))

  (testing "keys-destr in let: unused key removed — safe, just a deref on existing var"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-let-safe-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-let-safe-out")) (:content result)))))

  (testing ":strs destructuring in let: unused key removed — same safe behaviour as :keys"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "strs-destr-let-safe-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "strs-destr-let-safe-out")) (:content result)))))

  (testing "let scalar binding is still skipped — may be side-effectful"
    (assert-skip fixes/fix-unused-binding-in-file
                 (fixture-path "unused-binding" "let-binding-skip")
                 [:unused-binding]))

  (testing "keys-destr multi-line: unused key is only item on its line — line removed"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-first-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-first-key-out")) (:content result)))))

  (testing "keys-destr multi-line: unused key is middle item on its own line — line removed"
    (let [pred   #(str/includes? (:message %) " y")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-middle-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-middle-key-out")) (:content result)))))

  (testing "keys-destr multi-line: unused key is last item on its own line — line removed"
    (let [pred   #(str/includes? (:message %) " z")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-last-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-last-key-out")) (:content result)))))

  (testing "keys-destr multi-line: unused key shares line with other keys — others preserved"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-shared-line-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-shared-line-out")) (:content result)))))

  (testing "keys-destr multi-line: first key was only thing after {:keys [ — pull next key up"
    (let [pred   #(str/includes? (:message %) " x")
          result (assert-fix fixes/fix-unused-binding-in-file
                             (fixture-path "unused-binding" "keys-destr-multiline-first-key-in")
                             [:unused-binding] 1 pred)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "unused-binding" "keys-destr-multiline-first-key-out")) (:content result))))))
