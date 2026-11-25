(ns clj-kondo-fix.integration-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clj-kondo-fix.core :as fix]
            [clj-kondo-fix.test-support :refer [with-temp-file]]))

(deftest test-full-pipeline-dry-run
  (testing "dry-run does not write files"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [original (slurp f)
              result   (fix/fix! {:lint [f] :dry-run true})]
          (is (pos? (-> result :summary :total-fixed)))
          (is (= original (slurp f))))))))

(deftest test-full-pipeline-fix
  (testing "fix mode writes changes to disk"
    (with-temp-file "(ns foo (:require [clojure.string :as s]))"
      (fn [f]
        (let [result (fix/fix! {:lint [f] :dry-run false})]
          (is (pos? (-> result :summary :total-fixed)))
          (is (not (str/includes? (slurp f) "clojure.string"))))))))

(deftest test-rule-with-findings-in-other-file-only
  (testing "a stub rule with findings in one file does not crash on another file"
    (with-temp-file "(ns stub-only) (defn f []) (defn f [])"
      (fn [stub-file]
        (with-temp-file "(ns fixable (:require [clojure.string :as s]))"
          (fn [fixable-file]
            (let [result (fix/fix! {:lint [stub-file fixable-file] :dry-run true})]
              (is (pos? (-> result :summary :total-fixed))))))))))

(deftest test-fix-source
  (testing "returns the fixed source without touching any file"
    (let [{:keys [source result]} (fix/fix-source "(ns foo (:require [clojure.string :as s]))\n"
                                                  {:filename "src/foo.clj"})]
      (is (= "(ns foo)\n" source))
      (is (pos? (-> result :summary :total-fixed)))))
  (testing "returns the input unchanged when nothing is fixed"
    (let [input "(ns foo)\n(defn bar [] 1)"]
      (is (= input (:source (fix/fix-source input {}))))))
  (testing "picks the language from the filename extension"
    (let [{:keys [result]} (fix/fix-source "(ns foo (:require [clojure.string :as s]))\n"
                                           {:filename "foo.cljs"})]
      (is (pos? (-> result :summary :total-fixed))))))
