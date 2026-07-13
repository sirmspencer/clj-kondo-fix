(ns clj-kondo-fix.impl.fixes
  "Aggregator namespace. Requires all per-rule fix namespaces and re-exports
   each fix-*-in-file function so that rules.clj and core_test.clj can
   continue to reference clj-kondo-fix.impl.fixes/fix-* without change."
  (:require [clj-kondo-fix.impl.fixes.unused-namespace    :as unused-namespace]
            [clj-kondo-fix.impl.fixes.duplicate-require   :as duplicate-require]
            [clj-kondo-fix.impl.fixes.unused-binding      :as unused-binding]
            [clj-kondo-fix.impl.fixes.unused-import       :as unused-import]
            [clj-kondo-fix.impl.fixes.unused-referred-var :as unused-referred-var]
            [clj-kondo-fix.impl.fixes.refer-all           :as refer-all]
            [clj-kondo-fix.impl.fixes.missing-else-branch :as missing-else-branch]
            [clj-kondo-fix.impl.fixes.misplaced-docstring :as misplaced-docstring]
            [clj-kondo-fix.impl.fixes.unused-private-var  :as unused-private-var]
            [clj-kondo-fix.impl.fixes.redundant-do        :as redundant-do]
            [clj-kondo-fix.impl.fixes.redundant-let       :as redundant-let]
            [clj-kondo-fix.impl.fixes.redundant-str-call  :as redundant-str-call]
            [clj-kondo-fix.impl.fixes.if-x-x-y           :as if-x-x-y]
            [clj-kondo-fix.impl.fixes.if-nil-return      :as if-nil-return]
            [clj-kondo-fix.impl.fixes.condition-always-true :as condition-always-true]
            [clj-kondo-fix.impl.fixes.equals-nil          :as equals-nil]
            [clj-kondo-fix.impl.fixes.equals-true         :as equals-true]
            [clj-kondo-fix.impl.fixes.equals-false        :as equals-false]
            [clj-kondo-fix.impl.fixes.plus-one           :as plus-one]
            [clj-kondo-fix.impl.fixes.minus-one          :as minus-one]
            [clj-kondo-fix.impl.fixes.single-logical-operand :as single-logical-operand]
             [clj-kondo-fix.impl.fixes.single-key-in      :as single-key-in]))

(def fix-unused-ns-in-file          unused-namespace/fix-unused-ns-in-file)
(def fix-duplicate-require-in-file  duplicate-require/fix-duplicate-require-in-file)
(def fix-unused-binding-in-file     unused-binding/fix-unused-binding-in-file)
(def fix-unused-import-in-file      unused-import/fix-unused-import-in-file)
(def fix-unused-referred-var-in-file unused-referred-var/fix-unused-referred-var-in-file)
(def fix-refer-all-in-file          refer-all/fix-refer-all-in-file)
(def fix-missing-else-branch-in-file missing-else-branch/fix-missing-else-branch-in-file)
(def fix-misplaced-docstring-in-file misplaced-docstring/fix-misplaced-docstring-in-file)
(def fix-unused-private-var-in-file  unused-private-var/fix-unused-private-var-in-file)
(def fix-redundant-do-in-file        redundant-do/fix-redundant-do-in-file)
(def fix-redundant-let-in-file       redundant-let/fix-redundant-let-in-file)
(def fix-equals-nil-in-file          equals-nil/fix-equals-nil-in-file)
(def fix-equals-true-in-file         equals-true/fix-equals-true-in-file)
(def fix-equals-false-in-file        equals-false/fix-equals-false-in-file)
(def fix-plus-one-in-file           plus-one/fix-plus-one-in-file)
(def fix-minus-one-in-file          minus-one/fix-minus-one-in-file)
(def fix-single-logical-operand-in-file single-logical-operand/fix-single-logical-operand-in-file)
(def fix-single-key-in-in-file          single-key-in/fix-single-key-in-in-file)
(def fix-redundant-str-call-in-file     redundant-str-call/fix-redundant-str-call-in-file)
(def fix-if-x-x-y-in-file              if-x-x-y/fix-if-x-x-y-in-file)
(def fix-if-nil-return-in-file         if-nil-return/fix-if-nil-return-in-file)
(def fix-condition-always-true-in-file condition-always-true/fix-condition-always-true-in-file)
