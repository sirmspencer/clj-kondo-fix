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

   :redundant-call
   {:message-re #"^Single arg use of .+ always returns the arg itself$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-call-in-file
    :display     "redundant call"}

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

   :equals-float
   {:message-re #"^Equality comparison of floating point numbers$"
    :phase       :default
    :fix-fn      fixes/fix-equals-float-in-file
    :display     "equals float"}

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

   :redundant-nested-call
   {:message-re #"^Redundant nested call: "
    :phase       :default
    :fix-fn      fixes/fix-redundant-nested-call-in-file
    :display     "redundant nested call"}

   :redundant-primitive-coercion
   {:message-re #"^Redundant .* coercion - expression already has type .*$"
    :phase       :default
    :fix-fn      fixes/fix-redundant-primitive-coercion-in-file
    :display     "redundant primitive coercion"}

   :cond-else
   {:message-re #"^use :else as the catch-all test expression in cond$"
    :phase       :default
    :fix-fn      fixes/fix-cond-else-in-file
    :display     "cond else"}

   :docstring-blank
   {:message-re #"^Docstring should not be blank\.$"
    :phase       :default
    :fix-fn      fixes/fix-docstring-blank-in-file
    :display     "docstring blank"}

   :redundant-declare
   {:message-re #"^Redundant declare: "
    :phase       :default
    :fix-fn      fixes/fix-redundant-declare-in-file
    :display     "redundant declare"}

   :uninitialized-var
   {:message-re #"^Uninitialized var$"
    :phase       :default
    :fix-fn      fixes/fix-uninitialized-var-in-file
    :display     "uninitialized var"}

   :dynamic-var-not-earmuffed
   {:message-re #"^Var is declared dynamic but name is not earmuffed: "
    :phase       :default
    :fix-fn      fixes/fix-dynamic-var-not-earmuffed-in-file
    :display     "dynamic var not earmuffed"}

   :earmuffed-var-not-dynamic
   {:message-re #"^Var has earmuffed name but is not declared dynamic: "
    :phase       :default
    :fix-fn      fixes/fix-earmuffed-var-not-dynamic-in-file
    :display     "earmuffed var not dynamic"}

   :equals-expected-position
   {:message-re #"^Write expected value first$"
    :phase       :default
    :fix-fn      fixes/fix-equals-expected-position-in-file
    :display     "equals expected position"}

   :redundant-let-binding
   {:message-re #"^Redundant let binding: "
    :phase       :default
    :fix-fn      fixes/fix-redundant-let-binding-in-file
    :display     "redundant let binding"}

   :unsorted-imports
   {:message-re #"^Unsorted import: "
    :phase       :default
    :fix-fn      fixes/fix-unsorted-imports-in-file
    :display     "unsorted imports"}

   :unsorted-required-namespaces
   {:message-re #"^Unsorted namespace: "
    :phase       :default
    :fix-fn      fixes/fix-unsorted-required-namespaces-in-file
    :display     "unsorted required namespaces"}

   :unused-alias
   {:message-re #"^Unused alias: "
    :phase       :require
    :fix-fn      fixes/fix-unused-alias-in-file
    :display     "unused alias"}

   :alias-same-as-ns
   {:message-re #"^Alias same as namespace name: "
    :phase       :require
    :fix-fn      fixes/fix-alias-same-as-ns-in-file
    :display     "alias same as ns"}

   :duplicate-refer
   {:message-re #"^Duplicate refer: "
    :phase       :refer
    :fix-fn      fixes/fix-duplicate-refer-in-file
    :display     "duplicate refer"}

   :unused-excluded-var
   {:message-re #"^Unused excluded var: "
    :phase       :default
    :fix-fn      fixes/fix-unused-excluded-var-in-file
    :display     "unused excluded var"}

   :unresolved-excluded-var
   {:message-re #"^Unresolved excluded var: "
    :phase       :default
    :fix-fn      fixes/fix-unresolved-excluded-var-in-file
    :display     "unresolved excluded var"}

   :single-operand-comparison
   {:message-re #"^Single operand use of clojure\.core/"
    :phase       :default
    :fix-fn      fixes/fix-single-operand-comparison-in-file
    :display     "single operand comparison"}

   :java-static-field-call
   {:message-re #"^Static fields should be referenced without parens"
    :phase       :default
    :fix-fn      fixes/fix-java-static-field-call-in-file
    :display     "java static field call"}

   :duplicate-set-key
   {:message-re #"^duplicate set element"
    :phase       :default
    :fix-fn      fixes/fix-duplicate-set-key-in-file
    :display     "duplicate set key"}

   :def-fn
   {:message-re #"^Use defn instead"
    :phase       :default
    :fix-fn      fixes/fix-def-fn-in-file
    :display     "def + fn"}

   :aliased-namespace-symbol
   {:message-re #"^An alias is defined for"
    :phase       :default
    :fix-fn      fixes/fix-aliased-namespace-symbol-in-file
    :display     "aliased namespace symbol"}

   :is-message-not-string
   {:message-re #"^Test assertion message should be a string"
    :phase       :default
    :fix-fn      fixes/fix-is-message-not-string-in-file
    :display     "is message not string"}

   :non-arg-vec-return-type-hint
   {:message-re #"^Prefer placing return type hint on arg vector"
    :phase       :default
    :fix-fn      fixes/fix-non-arg-vec-return-type-hint-in-file
    :display     "non arg vec return type hint"}

   :aliased-referred-var
   {:message-re #"^Var .+ is referred but used via alias:"
    :phase       :default
    :fix-fn      fixes/fix-aliased-referred-var-in-file
    :display     "aliased referred var"}

   :used-underscored-binding
   {:message-re #"^Used binding is marked as unused:"
    :phase       :default
    :fix-fn      fixes/fix-used-underscored-binding-in-file
    :display     "used underscored binding"}

   :unreachable-code
   {:message-re #"^unreachable code"
    :phase       :default
    :fix-fn      fixes/fix-unreachable-code-in-file
    :display     "unreachable code"}

   :use
   {:message-re #"^use :require"
    :phase       :default
    :fix-fn      fixes/fix-use-in-file
    :display     "use"}})

(def rule-metadata
  {:await-without-async-fn
  {:status :not-applicable
   :reason "Structural fix (wrapping fn in async) requires understanding intent"
   :display "await without async fn"}

  :case-quoted-test
  {:status :not-applicable
   :reason "Removing the quote is trivial but the user may have intended the quoted symbol as a runtime value; requires human judgment"
   :display "case quoted test"}

  :clj-kondo-config
  {:status :not-applicable
   :reason "Config validation errors need human correction"
   :display "clj kondo config"}

  :consistent-alias
  {:status :not-applicable
   :reason "Requires a globally configured alias table; not deterministic from a single file"
   :display "consistent alias"}

  :datalog-syntax
  {:status :not-applicable
   :reason "Invalid datalog syntax requires domain knowledge to correct"
   :display "datalog syntax"}

  :deprecated-namespace
  {:status :not-applicable
   :reason "Replacing a deprecated namespace requires knowing the recommended replacement"
   :display "deprecated namespace"}

  :deprecated-var
  {:status :not-applicable
   :reason "Replacing a deprecated var requires knowing the recommended replacement"
   :display "deprecated var"}

  :discouraged-java-method
  {:status :not-applicable
   :reason "Replacing a discouraged method requires knowing the configured replacement"
   :display "discouraged java method"}

  :discouraged-namespace
  {:status :not-applicable
   :reason "Replacing a discouraged namespace requires knowing the configured replacement"
   :display "discouraged namespace"}

  :discouraged-var
  {:status :not-applicable
   :reason "Replacing a discouraged var requires knowing the configured replacement"
   :display "discouraged var"}

  :file
  {:status :not-applicable
   :reason "File I/O errors cannot be auto-fixed"
   :display "file"}

  :hook
  {:status :not-applicable
   :reason "Hook-related lint; not a code correctness issue"
   :display "hook"}

  :loop-without-recur
  {:status :not-applicable
   :reason "Structural fix (adding recur) requires understanding loop semantics and intent"
   :display "loop without recur"}

  :main-without-gen-class
  {:status :not-applicable
   :reason "Requires adding :gen-class to ns form, which may change compilation behavior"
   :display "main without gen class"}

  :missing-docstring
  {:status :not-applicable
   :reason "Writing a meaningful docstring requires human authorship"
   :display "missing docstring"}

  :missing-protocol-method
  {:status :not-applicable
   :reason "Generating a protocol method implementation requires knowing the intended behavior"
   :display "missing protocol method"}

  :missing-protocol-method-arity
  {:status :not-applicable
   :reason "Same as missing-protocol-method"
   :display "missing protocol method arity"}

  :missing-test-assertion
  {:status :not-applicable
   :reason "Writing a test assertion requires human authorship"
   :display "missing test assertion"}

  :namespace-name-mismatch
  {:status :not-applicable
   :reason "Renaming either the file or the ns declaration is a multi-file operation"
   :display "namespace name mismatch"}

  :protocol-method-arity-mismatch
  {:status :not-applicable
   :reason "Resolving an arity mismatch requires understanding the intended protocol contract"
   :display "protocol method arity mismatch"}

  :protocol-method-varargs
  {:status :not-applicable
   :reason "Varargs protocol methods require structural refactoring"
   :display "protocol method varargs"}

  :quoted-case-test-constant
  {:status :not-applicable
   :reason "Fix is trivial (remove single quote) but safety depends on whether the quoted form is intentional behavior in a performance-sensitive code path"
   :display "quoted case test constant"}

   :redefined-var
   {:status :not-applicable
    :reason "Deciding which definition to keep or merge requires human judgment"
    :display "redefined var"}

   :redundant-ignore
   {:status :not-applicable
    :reason "Removing #_ changes argument positions in the enclosing form, which can alter semantics regardless of whether the ignored expression is pure"
    :display "redundant ignore"}

   :refer-all
  {:status :not-applicable
   :reason "Cannot determine which symbols are actually used without analysis data; producing an explicit :refer list or :as alias requires domain knowledge"
   :display "refer all"}

  :schema-misplaced-return
  {:status :not-applicable
   :reason "Plumatic Schema placement requires understanding the schema structure"
   :display "schema misplaced return"}

  :self-requiring-namespace
  {:status :not-applicable
   :reason "Circular self-require must be resolved by removing the problematic require manually"
   :display "self requiring namespace"}

  :syntax
  {:status :not-applicable
   :reason "Syntax errors cannot be automatically corrected"
   :display "syntax"}

  :type-mismatch
  {:status :not-applicable
   :reason "Type errors require type inference context unavailable at text-transformation level"
   :display "type mismatch"}

  :unresolved-namespace
  {:status :not-applicable
   :reason "Cannot create or locate a missing namespace automatically"
   :display "unresolved namespace"}

  :unresolved-protocol-method
  {:status :not-applicable
   :reason "Resolving a missing protocol method requires human implementation"
   :display "unresolved protocol method"}

  :unresolved-symbol
  {:status :not-applicable
   :reason "Cannot create or locate a missing symbol automatically"
   :display "unresolved symbol"}

  :unresolved-var
  {:status :not-applicable
   :reason "Cannot create or locate a missing var automatically"
   :display "unresolved var"}

  :warn-on-reflection
  {:status :not-applicable
   :reason "Requires adding *warn-on-reflection* binding; intent and placement are contextual"
   :display "warn on reflection"}

  ;; ---- not-implemented ----
   :alias-same-as-ns
   {:status :implemented
    :display "alias same as ns"}

  :aliased-namespace-symbol
   {:status :implemented
    :display "aliased namespace symbol"}

  :aliased-namespace-var-usage
  {:status :not-applicable
   :reason "Fires on :as-alias usage where namespace wasn't loaded; cannot mechanically decide whether to add a real require or remove the usage — requires project knowledge"
   :display "aliased namespace var usage"}

  :aliased-referred-var
  {:status :implemented
   :display "aliased referred var"}

  :case-duplicate-test
  {:status :not-implemented
   :display "case duplicate test"}

  :case-symbol-test
  {:status :not-implemented
   :display "case symbol test"}

  :conditional-build-up
  {:status :not-implemented
   :display "conditional build up"}

  :conflicting-alias
  {:status :not-implemented
   :display "conflicting alias"}

  :def-fn
   {:status :implemented
    :display "def fn"}

  :destructured-or-always-evaluates
  {:status :not-implemented
   :display "destructured or always evaluates"}

  :destructured-or-binding-of-same-map
  {:status :not-implemented
   :display "destructured or binding of same map"}

  :discouraged-tag
  {:status :not-implemented
   :display "discouraged tag"}

  :do-template
  {:status :not-implemented
   :display "do template"}

  :docstring-no-summary
  {:status :not-implemented
   :display "docstring no summary"}

  :duplicate-field-name
  {:status :not-implemented
   :display "duplicate field name"}

  :duplicate-key-args
  {:status :not-implemented
   :display "duplicate key args"}

  :duplicate-map-key
  {:status :not-implemented
   :display "duplicate map key"}

   :duplicate-refer
   {:status :implemented
    :display "duplicate refer"}

   :duplicate-set-key
   {:status :implemented
    :display "duplicate set key"}

  :format
  {:status :not-implemented
   :display "format"}

  :inline-def
  {:status :not-implemented
   :display "inline def"}

  :is-message-not-string
   {:status :implemented
    :display "is message not string"}

   :java-static-field-call
   {:status :implemented
    :display "java static field call"}

  :line-length
  {:status :not-implemented
   :display "line length"}

  :misplaced-async-metadata
  {:status :not-applicable
   :reason "ClojureScript-only linter — cannot trigger or test with .clj fixtures"
   :display "misplaced async metadata"}

  :missing-body-in-when
  {:status :not-implemented
   :display "missing body in when"}

  :missing-clause-in-try
  {:status :not-implemented
   :display "missing clause in try"}

  :missing-map-value
  {:status :not-implemented
   :display "missing map value"}

  :non-arg-vec-return-type-hint
   {:status :implemented
    :display "non arg vec return type hint"}

  :private-call
  {:status :not-implemented
   :display "private call"}

   :refer
  {:status :not-implemented
   :display "refer"}

  :shadowed-fn-param
  {:status :not-implemented
   :display "shadowed fn param"}

  :shadowed-var
  {:status :not-implemented
   :display "shadowed var"}

   :single-operand-comparison
   {:status :implemented
    :display "single operand comparison"}

  :unbound-destructuring-default
  {:status :not-implemented
   :display "unbound destructuring default"}

  :underscore-in-namespace
  {:status :not-implemented
   :display "underscore in namespace"}

  :unexpected-recur
  {:status :not-implemented
   :display "unexpected recur"}

  :unknown-ns-option
  {:status :not-implemented
   :display "unknown ns option"}

  :unknown-require-option
  {:status :not-implemented
   :display "unknown require option"}

  :unquote-not-syntax-quoted
  {:status :not-implemented
   :display "unquote not syntax quoted"}

  :unreachable-code
  {:status :implemented
   :display "unreachable code"}

   :unresolved-excluded-var
   {:status :implemented
    :display "unresolved excluded var"}

   :unused-alias
   {:status :implemented
    :display "unused alias"}

   :unused-excluded-var
   {:status :implemented
    :display "unused excluded var"}

  :unused-value
  {:status :not-implemented
   :display "unused value"}

  :use
  {:status :implemented
   :display "use"}

  :used-underscored-binding
  {:status :implemented
   :display "used underscored binding"}

  :var-same-name-except-case
  {:status :not-implemented
   :display "var same name except case"}})

(defn stub-fix-fn [file-path lines findings log]
  (let [rule-key (:type (first findings))
        meta (get rule-metadata rule-key)]
    (swap! log conj (str "[" (name rule-key) "] "
                         (case (:status meta)
                           :not-applicable (str "cannot auto-fix: " (:reason meta))
                           :not-implemented "not yet implemented"
                           "unknown status")))
    {:fixed 0 :lines lines}))

(def stub-definitions
  {:await-without-async-fn
  {:message-re #"^"
   :display "await without async fn"
   :fix-fn stub-fix-fn}

  :case-quoted-test
  {:message-re #"^"
   :display "case quoted test"
   :fix-fn stub-fix-fn}

  :clj-kondo-config
  {:message-re #"^"
   :display "clj kondo config"
   :fix-fn stub-fix-fn}

  :consistent-alias
  {:message-re #"^"
   :display "consistent alias"
   :fix-fn stub-fix-fn}

  :datalog-syntax
  {:message-re #"^"
   :display "datalog syntax"
   :fix-fn stub-fix-fn}

  :deprecated-namespace
  {:message-re #"^"
   :display "deprecated namespace"
   :fix-fn stub-fix-fn}

  :deprecated-var
  {:message-re #"^"
   :display "deprecated var"
   :fix-fn stub-fix-fn}

  :discouraged-java-method
  {:message-re #"^"
   :display "discouraged java method"
   :fix-fn stub-fix-fn}

  :discouraged-namespace
  {:message-re #"^"
   :display "discouraged namespace"
   :fix-fn stub-fix-fn}

  :discouraged-var
  {:message-re #"^"
   :display "discouraged var"
   :fix-fn stub-fix-fn}

  :file
  {:message-re #"^"
   :display "file"
   :fix-fn stub-fix-fn}

  :hook
  {:message-re #"^"
   :display "hook"
   :fix-fn stub-fix-fn}

  :loop-without-recur
  {:message-re #"^"
   :display "loop without recur"
   :fix-fn stub-fix-fn}

  :main-without-gen-class
  {:message-re #"^"
   :display "main without gen class"
   :fix-fn stub-fix-fn}

  :missing-docstring
  {:message-re #"^"
   :display "missing docstring"
   :fix-fn stub-fix-fn}

  :missing-protocol-method
  {:message-re #"^"
   :display "missing protocol method"
   :fix-fn stub-fix-fn}

  :missing-protocol-method-arity
  {:message-re #"^"
   :display "missing protocol method arity"
   :fix-fn stub-fix-fn}

  :missing-test-assertion
  {:message-re #"^"
   :display "missing test assertion"
   :fix-fn stub-fix-fn}

  :namespace-name-mismatch
  {:message-re #"^"
   :display "namespace name mismatch"
   :fix-fn stub-fix-fn}

  :protocol-method-arity-mismatch
  {:message-re #"^"
   :display "protocol method arity mismatch"
   :fix-fn stub-fix-fn}

  :protocol-method-varargs
  {:message-re #"^"
   :display "protocol method varargs"
   :fix-fn stub-fix-fn}

  :quoted-case-test-constant
  {:message-re #"^"
   :display "quoted case test constant"
   :fix-fn stub-fix-fn}

  :redefined-var
  {:message-re #"^"
   :display "redefined var"
   :fix-fn stub-fix-fn}

  :refer-all
  {:message-re #"^"
   :display "refer all"
   :fix-fn stub-fix-fn}

  :schema-misplaced-return
  {:message-re #"^"
   :display "schema misplaced return"
   :fix-fn stub-fix-fn}

  :self-requiring-namespace
  {:message-re #"^"
   :display "self requiring namespace"
   :fix-fn stub-fix-fn}

  :syntax
  {:message-re #"^"
   :display "syntax"
   :fix-fn stub-fix-fn}

  :type-mismatch
  {:message-re #"^"
   :display "type mismatch"
   :fix-fn stub-fix-fn}

  :unresolved-namespace
  {:message-re #"^"
   :display "unresolved namespace"
   :fix-fn stub-fix-fn}

  :unresolved-protocol-method
  {:message-re #"^"
   :display "unresolved protocol method"
   :fix-fn stub-fix-fn}

  :unresolved-symbol
  {:message-re #"^"
   :display "unresolved symbol"
   :fix-fn stub-fix-fn}

  :unresolved-var
  {:message-re #"^"
   :display "unresolved var"
   :fix-fn stub-fix-fn}

  :warn-on-reflection
  {:message-re #"^"
   :display "warn on reflection"
   :fix-fn stub-fix-fn}

  ;; ---- stubs (not-implemented) ----
  :case-duplicate-test
  {:message-re #"^"
   :display "case duplicate test"
   :fix-fn stub-fix-fn}

  :case-symbol-test
  {:message-re #"^"
   :display "case symbol test"
   :fix-fn stub-fix-fn}

  :conditional-build-up
  {:message-re #"^"
   :display "conditional build up"
   :fix-fn stub-fix-fn}

  :conflicting-alias
  {:message-re #"^"
   :display "conflicting alias"
   :fix-fn stub-fix-fn}

   :destructured-or-always-evaluates
  {:message-re #"^"
   :display "destructured or always evaluates"
   :fix-fn stub-fix-fn}

  :destructured-or-binding-of-same-map
  {:message-re #"^"
   :display "destructured or binding of same map"
   :fix-fn stub-fix-fn}

  :discouraged-tag
  {:message-re #"^"
   :display "discouraged tag"
   :fix-fn stub-fix-fn}

  :do-template
  {:message-re #"^"
   :display "do template"
   :fix-fn stub-fix-fn}

  :docstring-no-summary
  {:message-re #"^"
   :display "docstring no summary"
   :fix-fn stub-fix-fn}

  :duplicate-field-name
  {:message-re #"^"
   :display "duplicate field name"
   :fix-fn stub-fix-fn}

  :duplicate-key-args
  {:message-re #"^"
   :display "duplicate key args"
   :fix-fn stub-fix-fn}

  :duplicate-map-key
  {:message-re #"^"
   :display "duplicate map key"
   :fix-fn stub-fix-fn}

   :format
  {:message-re #"^"
   :display "format"
   :fix-fn stub-fix-fn}

  :inline-def
  {:message-re #"^"
   :display "inline def"
   :fix-fn stub-fix-fn}

   :line-length
  {:message-re #"^"
   :display "line length"
   :fix-fn stub-fix-fn}

  :missing-body-in-when
  {:message-re #"^"
   :display "missing body in when"
   :fix-fn stub-fix-fn}

  :missing-clause-in-try
  {:message-re #"^"
   :display "missing clause in try"
   :fix-fn stub-fix-fn}

  :missing-map-value
  {:message-re #"^"
   :display "missing map value"
   :fix-fn stub-fix-fn}

   :private-call
  {:message-re #"^"
   :display "private call"
   :fix-fn stub-fix-fn}

  :redundant-ignore
  {:message-re #"^"
   :display "redundant ignore"
   :fix-fn stub-fix-fn}

  :refer
  {:message-re #"^"
   :display "refer"
   :fix-fn stub-fix-fn}

  :shadowed-fn-param
  {:message-re #"^"
   :display "shadowed fn param"
   :fix-fn stub-fix-fn}

  :shadowed-var
  {:message-re #"^"
   :display "shadowed var"
   :fix-fn stub-fix-fn}

   :unbound-destructuring-default
  {:message-re #"^"
   :display "unbound destructuring default"
   :fix-fn stub-fix-fn}

  :underscore-in-namespace
  {:message-re #"^"
   :display "underscore in namespace"
   :fix-fn stub-fix-fn}

  :unexpected-recur
  {:message-re #"^"
   :display "unexpected recur"
   :fix-fn stub-fix-fn}

  :unknown-ns-option
  {:message-re #"^"
   :display "unknown ns option"
   :fix-fn stub-fix-fn}

  :unknown-require-option
  {:message-re #"^"
   :display "unknown require option"
   :fix-fn stub-fix-fn}

  :unquote-not-syntax-quoted
  {:message-re #"^"
   :display "unquote not syntax quoted"
   :fix-fn stub-fix-fn}

   :unused-value
   {:message-re #"^"
    :display "unused value"
    :fix-fn stub-fix-fn}

  :var-same-name-except-case
  {:message-re #"^"
   :display "var same name except case"
   :fix-fn stub-fix-fn}})

(def category-aliases
  {:unused-ns #{:unused-namespace :duplicate-require}})

(defn resolve-rules [cli-rule-keys]
  (let [all-defs (merge rule-definitions stub-definitions)
        requested (or (seq cli-rule-keys)
                      (keys all-defs))
        expanded (mapcat (fn [k]
                           (if-let [alias-set (get category-aliases k)]
                             alias-set
                             [k]))
                         requested)]
    (into {}
          (keep (fn [k] (when-let [def (get all-defs k)]
                          [k def])))
          expanded)))

(defn findings-matching-rule [findings rule-key rule-def]
  (->> findings
       (filter #(= (:type %) rule-key))
       (filter #(re-find (:message-re rule-def) (:message %)))))


