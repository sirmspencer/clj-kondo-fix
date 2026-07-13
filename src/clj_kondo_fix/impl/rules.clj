(ns clj-kondo-fix.impl.rules
  (:require [clj-kondo-fix.impl.fixes :as fixes]))

(def rule-definitions
  {:unused-namespace
   {:message-re #"^namespace (.+) is required but never used$"
    :phase :require
    :fix-fn fixes/fix-unused-ns-in-file
    :display "unused namespace"}

   :duplicate-require
   {:message-re #"^duplicate require of (.+)$"
    :phase :require
    :fix-fn fixes/fix-duplicate-require-in-file
    :display "duplicate require"}

   :unused-binding
   {:message-re #"^unused binding (.+)$"
    :phase :binding
    :fix-fn fixes/fix-unused-binding-in-file
    :config {:fix-contexts #{:as-clause :fn-param :keys-destr-fn :keys-destr-let}}
    :display "unused binding"}

   :unused-import
   {:message-re #"^Unused import (.+)$"
    :phase :import
    :fix-fn fixes/fix-unused-import-in-file
    :display "unused import"}

   :unused-referred-var
   {:message-re #"^#'.+ is referred but never used$"
    :phase :refer
    :fix-fn fixes/fix-unused-referred-var-in-file
    :display "unused referred var"}

   :refer-all
   {:message-re #"^use alias or :refer"
    :phase :refer
    :fix-fn fixes/fix-refer-all-in-file
    :display "refer :all"}

   :misplaced-docstring
   {:message-re #"^Misplaced docstring\.$"
    :phase :default
    :fix-fn fixes/fix-misplaced-docstring-in-file
    :display "misplaced docstring"}

   :missing-else-branch
   {:message-re #"^Missing else branch\.$"
    :phase :default
    :fix-fn fixes/fix-missing-else-branch-in-file
    :display "missing else branch"}

   :unused-private-var
   {:message-re #"^Unused private var (.+)$"
    :phase :default
    :fix-fn fixes/fix-unused-private-var-in-file
    :display "unused private var"}

   :redundant-do
   {:message-re #"^redundant do$"
    :phase :default
    :fix-fn fixes/fix-redundant-do-in-file
    :display "redundant do"}

   :redundant-format
   {:message-re #"^Format string contains no format specifiers$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-format-in-file
    :display     "redundant format"}

   :redundant-let
   {:message-re #"^Redundant let expression\.$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-let-in-file
    :display     "redundant let"}

   :redundant-str-call
   {:message-re #"^Single argument to str already is a string$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-str-call-in-file
    :display     "redundant str call"}

   :equals-nil
   {:message-re #"^Prefer \(nil\? x\) over \(= nil x\)$"
    :phase       :default
    :fix-fn      fixes/fix-equals-nil-in-file
    :display     "equals nil"}

   :equals-true
   {:message-re #"^Prefer \(true\? x\) over \(= true x\)$"
    :phase       :default
    :fix-fn      fixes/fix-equals-true-in-file
    :display     "equals true"}

   :equals-false
   {:message-re #"^Prefer \(false\? x\) over \(= false x\)$"
    :phase       :default
    :fix-fn      fixes/fix-equals-false-in-file
    :display     "equals false"}

   :plus-one
   {:message-re #"^Prefer \(inc x\) over \(\+ 1 x\)$"
    :phase       :default
    :fix-fn      fixes/fix-plus-one-in-file
    :display     "plus one"}

   :minus-one
   {:message-re #"^Prefer \(dec x\) over \(- x 1\)$"
    :phase       :default
    :fix-fn      fixes/fix-minus-one-in-file
    :display     "minus one"}

   :single-logical-operand
   {:message-re #"^Single arg use of (?:and|or) always returns the arg itself$"
    :phase       :default
    :fix-fn      fixes/fix-single-logical-operand-in-file
    :display     "single logical operand"}

   :single-key-in
   {:message-re #"^get-in with single key$"
    :phase       :default
    :fix-fn      fixes/fix-single-key-in-in-file
    :display     "single key in"}

   :if-x-x-y
   {:message-re #"^If condition and then branch are the same; use \(or .+\)$"
    :phase       :default
    :fix-fn      fixes/fix-if-x-x-y-in-file
    :display     "if x x y"}

   :if-nil-return
   {:message-re #"^For nil return, prefer when"
    :phase       :default
    :fix-fn      fixes/fix-if-nil-return-in-file
    :display     "if nil return"}

    :condition-always-true
    {:message-re #"^Condition always true$"
     :phase       :default
     :fix-fn      fixes/fix-condition-always-true-in-file
     :display     "condition always true"}

   :docstring-leading-trailing-whitespace
   {:message-re #"^Docstring should not have leading or trailing whitespace\.$"
    :phase       :default
    :fix-fn      fixes/fix-docstring-leading-trailing-whitespace-in-file
    :display     "docstring leading/trailing whitespace"}

   :keyword-binding
   {:message-re #"^Keyword binding should be a symbol: "
    :phase       :default
    :fix-fn      fixes/fix-keyword-binding-in-file
    :display     "keyword binding"}

   :not-nil?
   {:message-re #"^Use \(some\?"
    :phase       :default
    :fix-fn      fixes/fix-not-nil-in-file
    :display     "not nil?"}

   :redundant-fn-wrapper
   {:message-re #"^Redundant fn wrapper$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-fn-wrapper-in-file
    :display     "redundant fn wrapper"}

   :redundant-primitive-coercion
   {:message-re #"^Redundant .* coercion - expression already has type .*$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-primitive-coercion-in-file
    :display     "redundant primitive coercion"}})

(def category-aliases
  {:unused-ns #{:unused-namespace :duplicate-require}})

(defn resolve-rules [cli-rule-keys]
  (let [requested (or (seq cli-rule-keys)
                      (keys rule-definitions))
        expanded (mapcat (fn [k]
                           (if-let [alias-set (get category-aliases k)]
                             alias-set
                             [k]))
                         requested)]
    (into {}
          (keep (fn [k] (when-let [def (get rule-definitions k)]
                          [k def])))
          expanded)))

(defn findings-matching-rule [findings rule-def]
  (filter #(re-find (:message-re rule-def) (:message %)) findings))
