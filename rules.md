# clj-kondo-fix Rule Index

13 implemented · 76 not yet implemented · 30 not applicable · 1 skipped

## Index

- [:alias-same-as-ns](#alias-same-as-ns)
- [:aliased-namespace-symbol](#aliased-namespace-symbol)
- [:aliased-namespace-var-usage](#aliased-namespace-var-usage)
- [:aliased-referred-var](#aliased-referred-var)
- [:await-without-async-fn](#await-without-async-fn) ❌
- [:case-duplicate-test](#case-duplicate-test)
- [:case-quoted-test](#case-quoted-test)
- [:case-symbol-test](#case-symbol-test)
- [:clj-kondo-config](#clj-kondo-config) ❌
- [:cond-else](#cond-else)
- [:condition-always-true](#condition-always-true)
- [:conditional-build-up](#conditional-build-up)
- [:conflicting-alias](#conflicting-alias)
- [:consistent-alias](#consistent-alias) ❌
- [:datalog-syntax](#datalog-syntax) ❌
- [:def-fn](#def-fn)
- [:deprecated-namespace](#deprecated-namespace) ❌
- [:deprecated-var](#deprecated-var) ❌
- [:destructured-or-always-evaluates](#destructured-or-always-evaluates)
- [:destructured-or-binding-of-same-map](#destructured-or-binding-of-same-map)
- [:discouraged-java-method](#discouraged-java-method) ❌
- [:discouraged-namespace](#discouraged-namespace) ❌
- [:discouraged-tag](#discouraged-tag)
- [:discouraged-var](#discouraged-var) ❌
- [:do-template](#do-template)
- [:docstring-blank](#docstring-blank)
- [:docstring-leading-trailing-whitespace](#docstring-leading-trailing-whitespace)
- [:docstring-no-summary](#docstring-no-summary)
- [:duplicate-field-name](#duplicate-field-name)
- [:duplicate-key-args](#duplicate-key-args)
- [:duplicate-map-key](#duplicate-map-key)
- [:duplicate-refer](#duplicate-refer)
- [:duplicate-require](#duplicate-require) ✅
- [:duplicate-set-key](#duplicate-set-key)
- [:dynamic-var-not-earmuffed](#dynamic-var-not-earmuffed)
- [:earmuffed-var-not-dynamic](#earmuffed-var-not-dynamic)
- [:equals-expected-position](#equals-expected-position)
- [:equals-false](#equals-false) ✅
- [:equals-float](#equals-float)
- [:equals-nil](#equals-nil) ✅
- [:equals-true](#equals-true) ✅
- [:file](#file) ❌
- [:format](#format)
- [:hook](#hook) ❌
- [:if-nil-return](#if-nil-return)
- [:if-x-x-y](#if-x-x-y)
- [:inline-def](#inline-def)
- [:is-message-not-string](#is-message-not-string)
- [:java-static-field-call](#java-static-field-call)
- [:line-length](#line-length)
- [:loop-without-recur](#loop-without-recur) ❌
- [:main-without-gen-class](#main-without-gen-class) ❌
- [:minus-one](#minus-one)
- [:misplaced-async-metadata](#misplaced-async-metadata)
- [:misplaced-docstring](#misplaced-docstring) ✅
- [:missing-body-in-when](#missing-body-in-when)
- [:missing-clause-in-try](#missing-clause-in-try)
- [:missing-docstring](#missing-docstring) ❌
- [:missing-else-branch](#missing-else-branch) ✅
- [:missing-map-value](#missing-map-value)
- [:missing-protocol-method](#missing-protocol-method) ❌
- [:missing-protocol-method-arity](#missing-protocol-method-arity) ❌
- [:missing-test-assertion](#missing-test-assertion) ❌
- [:namespace-name-mismatch](#namespace-name-mismatch) ❌
- [:non-arg-vec-return-type-hint](#non-arg-vec-return-type-hint)
- [:plus-one](#plus-one)
- [:private-call](#private-call)
- [:protocol-method-arity-mismatch](#protocol-method-arity-mismatch) ❌
- [:protocol-method-varargs](#protocol-method-varargs) ❌
- [:quoted-case-test-constant](#quoted-case-test-constant)
- [:redefined-var](#redefined-var) ❌
- [:redundant-call](#redundant-call)
- [:redundant-declare](#redundant-declare)
- [:redundant-do](#redundant-do) ✅
- [:redundant-fn-wrapper](#redundant-fn-wrapper)
- [:redundant-format](#redundant-format)
- [:redundant-ignore](#redundant-ignore)
- [:redundant-let](#redundant-let) ✅
- [:redundant-let-binding](#redundant-let-binding)
- [:redundant-nested-call](#redundant-nested-call)
- [:redundant-primitive-coercion](#redundant-primitive-coercion)
- [:redundant-str-call](#redundant-str-call)
- [:refer](#refer)
- [:refer-all](#refer-all) ⚠️
- [:schema-misplaced-return](#schema-misplaced-return) ❌
- [:self-requiring-namespace](#self-requiring-namespace) ❌
- [:shadowed-fn-param](#shadowed-fn-param)
- [:shadowed-var](#shadowed-var)
- [:single-key-in](#single-key-in)
- [:single-logical-operand](#single-logical-operand)
- [:single-operand-comparison](#single-operand-comparison)
- [:syntax](#syntax) ❌
- [:type-mismatch](#type-mismatch) ❌
- [:unbound-destructuring-default](#unbound-destructuring-default)
- [:underscore-in-namespace](#underscore-in-namespace)
- [:unexpected-recur](#unexpected-recur)
- [:uninitialized-var](#uninitialized-var)
- [:unknown-ns-option](#unknown-ns-option)
- [:unknown-require-option](#unknown-require-option)
- [:unquote-not-syntax-quoted](#unquote-not-syntax-quoted)
- [:unreachable-code](#unreachable-code)
- [:unresolved-excluded-var](#unresolved-excluded-var)
- [:unresolved-namespace](#unresolved-namespace) ❌
- [:unresolved-protocol-method](#unresolved-protocol-method) ❌
- [:unresolved-symbol](#unresolved-symbol) ❌
- [:unresolved-var](#unresolved-var) ❌
- [:unsorted-imports](#unsorted-imports)
- [:unsorted-required-namespaces](#unsorted-required-namespaces)
- [:unused-alias](#unused-alias)
- [:unused-binding](#unused-binding) ✅
- [:unused-excluded-var](#unused-excluded-var)
- [:unused-import](#unused-import) ✅
- [:unused-namespace](#unused-namespace) ✅
- [:unused-private-var](#unused-private-var) ✅
- [:unused-referred-var](#unused-referred-var) ✅
- [:unused-value](#unused-value)
- [:use](#use)
- [:used-underscored-binding](#used-underscored-binding)
- [:var-same-name-except-case](#var-same-name-except-case)
- [:warn-on-reflection](#warn-on-reflection) ❌

## Implemented Rules

### :duplicate-require

**Duplicate require**

warns on namespace that has been required more than once within a namespace

---

### :equals-false

**Equals false**

warn on usage of `(= false x)` or `(= x false)` rather than `(false? x)`

---

### :equals-nil

**Equals nil**

warn on usage of `(= nil x)` or `(= x nil)` rather than `(nil? x)`

---

### :equals-true

**Equals true**

warn on usage of `(= true x)` or `(= x true)` rather than `(true? x)`

---

### :misplaced-docstring

**Misplaced docstring**

warn when docstring appears after argument vector instead of before

---

### :missing-else-branch

**Missing else branch**

warns about missing else branch in `if` expression

---

### :redundant-do

**Redundant do**

warn on usage of do that is redundant. The warning usually arises

---

### :redundant-let

**Redundant let**

warn on usage of let that is redundant. The warning usually arises

---

### :unused-binding

**Unused binding**

warn on unused binding

---

### :unused-import

**Unused import**

warn on unused import

---

### :unused-namespace

**Unused namespace**

warns on required but unused namespace

---

### :unused-private-var

**Unused private var**

warns on unused private vars

---

### :unused-referred-var

**Unused referred var**

warns about unused referred vars

## Not Yet Implemented

These rules could potentially be auto-fixed but have not been tackled yet.

| Rule | Description |
| --- | --- |
| `:alias-same-as-ns` | warn when alias is the same as the namespace it is aliasing |
| `:aliased-namespace-symbol` | warn when the namespace of a qualified symbol has a defined alias |
| `:aliased-namespace-var-usage` | warn when a var from a namespace that was used with `:as-alias` is used |
| `:aliased-referred-var` | warn when a var is both referred and accessed via an alias in the same namespace |
| `:case-duplicate-test` | identify duplicate case test constants |
| `:case-quoted-test` | Warn on quoted test constants in `case`, a common mistake when |
| `:case-symbol-test` | Warn on symbol test constants in `case`. Sometimes this is |
| `:cond-else` | warn on `cond` with a different constant for the else branch than `:else` |
| `:condition-always-true` | warn on a condition that evaluates to an always truthy constant, |
| `:conditional-build-up` | warn when a `let` repeatedly rebinds the same local map using forms like `(if pred (assoc m ...) m)`, which can often be written more clearly with `cond->` |
| `:conflicting-alias` | warn on conflicting alias |
| `:def-fn` | tells about closures defined with the combination of |
| `:destructured-or-always-evaluates` | Warn when an `:or` default value in a destructuring contains an |
| `:destructured-or-binding-of-same-map` | an `:or` default value refers to a destructured binding of the |
| `:discouraged-tag` | warn on the usage of a tagged literal that is discouraged to be used |
| `:do-template` | warn on incorrect usages of `clojure.template/do-template`: no args, no values, or incorrect number of values |
| `:docstring-blank` | warn on blank docstring |
| `:docstring-leading-trailing-whitespace` | warn when docstring has leading or trailing whitespace |
| `:docstring-no-summary` | warn when first _line_ of docstring is not a complete |
| `:duplicate-field-name` | identify duplicate fields in deftype/defrecord fields definition |
| `:duplicate-key-args` | identify duplicate key args in calls to `assoc`, `dissoc`, `hash-map` etc |
| `:duplicate-map-key` | warn on duplicate key in map |
| `:duplicate-refer` | warns on var that has been referred more than once in a `:refer` or `:refer-macros` vector |
| `:duplicate-set-key` | similar to `:duplicate-map-key` but for sets |
| `:dynamic-var-not-earmuffed` | warn when dynamic var doesn't have an earmuffed name |
| `:earmuffed-var-not-dynamic` | warn when var with earmuffed name isn't declared dynamic |
| `:equals-expected-position` | warn on usage of `=` with the expected value, a constant, that is not in the expected (first by default) position |
| `:equals-float` | warn on usage of comparison with `=` on floating point numbers, |
| `:format` | warn on unexpected amount of arguments in `format` |
| `:if-nil-return` | warn when if-like form explicitly returns nil from either |
| `:if-x-x-y` | warn on `(if x x y)` and suggest `(or x y)` instead when `x` is a |
| `:inline-def` | warn on non-toplevel usage of `def` (and `defn`, etc.) |
| `:is-message-not-string` | warn when `clojure.test/is` receives a non-string message argument. This linter relies on the `:type-mismatch` linter being enabled to perform type checking |
| `:java-static-field-call` | warn when invoking a static field on a Java object |
| `:line-length` | warn when lines are longer than a configured length |
| `:minus-one` | warn on usages of `-` that can be replaced with `dec` |
| `:misplaced-async-metadata` | warns when `^:async` metadata is placed where ClojureScript ignores it: on a function's argument vector or on the whole `(fn ...)` form. It must go on the function name. ClojureScript only |
| `:missing-body-in-when` | warn when `when` is called only with a condition |
| `:missing-clause-in-try` | warn when `try` expression misses `catch` or `finally` clause |
| `:missing-map-value` | warn on key with uneven amount of elements, i.e. one of the keys |
| `:non-arg-vec-return-type-hint` | warn when a return type in `defn` is not placed on the argument vector (CLJ only) |
| `:plus-one` | warn on usages of `+` that can be replaced with `inc` |
| `:private-call` | warn when private var is used. The name of this linter should be |
| `:quoted-case-test-constant` | warn when encountering quoted test case constants |
| `:redundant-call` | warn on redundant calls. The warning arises when a single argument |
| `:redundant-declare` | warn when `declare` is used after a var is already defined in the same namespace |
| `:redundant-fn-wrapper` | warn on redundant function wrapper |
| `:redundant-format` | warn when format strings contain no format specifiers |
| `:redundant-ignore` | warn on redundant ignore, i.e. when an ignored expression doesn't trigger any lint warning |
| `:redundant-let-binding` | warn on redundant binding of a symbol to itself. Excludes |
| `:redundant-nested-call` | warn on redundant nested call of functions and macros |
| `:redundant-primitive-coercion` | warn on redundant primitive coercion calls. The warning arises when a |
| `:redundant-str-call` | warn on redundant `str` calls. The warning arises when a single argument |
| `:refer` | warns when `:refer` is used. This can be used when one wants to |
| `:shadowed-fn-param` | warn on fn param that has same name as previously defined one (in the same fn expression) |
| `:shadowed-var` | warn on var that is shadowed by local |
| `:single-key-in` | warn on associative path function with a single value path |
| `:single-logical-operand` | warn on single operand logical operators with always the same value |
| `:single-operand-comparison` | warn on comparison with only one argument |
| `:unbound-destructuring-default` | warn on binding in `:or` which does not occur in destructuring |
| `:underscore-in-namespace` | warns about the usage of the `_` character in the declaration of namespaces (as opposed to `-`) |
| `:unexpected-recur` | `(recur ...)` is called where it's not expected |
| `:uninitialized-var` | warn on var without initial value |
| `:unknown-ns-option` | warn on unknown top-level `ns` options |
| `:unknown-require-option` | warn on unknown `:require` option pairs |
| `:unquote-not-syntax-quoted` | warns when unquote (`~`) or unquote-splicing (`~@`) is used outside of syntax-quote (`` ` ``) |
| `:unreachable-code` | warn on unreachable code |
| `:unresolved-excluded-var` | warns when `:refer-clojure :exclude` contains vars that do not exist in clojure.core or cljs.core |
| `:unsorted-imports` | warns on non-alphabetically sorted imports in `ns` and `require` forms |
| `:unsorted-required-namespaces` | warns on non-alphabetically sorted libspecs in `ns` and `require` forms |
| `:unused-alias` | warn on unused alias introduced in ns form |
| `:unused-excluded-var` | warns when `:refer-clojure :exclude` contains vars that are not redefined in the current namespace. Locals with the same name as an excluded var also count as a redefinition and will suppress this warning |
| `:unused-value` | warn on unused value: constants, unrealized lazy values, pure functions and transient ops (`assoc!`, `conj!` etc) |
| `:use` | warns about `:use` or `use` |
| `:used-underscored-binding` | warn when a underscored (ie marked as unused) binding is used |
| `:var-same-name-except-case` | warn on vars that share the same name with different case (only in Clojure mode) as these could cause clashing class file names on case insensitive filesystems |

## Not Applicable

These rules cannot be meaningfully auto-fixed.

| Rule | Why |
| --- | --- |
| `:await-without-async-fn` | Structural fix (wrapping fn in async) requires understanding intent |
| `:clj-kondo-config` | Config validation errors need human correction |
| `:consistent-alias` | Requires a globally configured alias table; not deterministic from a single file |
| `:datalog-syntax` | Invalid datalog syntax requires domain knowledge to correct |
| `:deprecated-namespace` | Replacing a deprecated namespace requires knowing the recommended replacement |
| `:deprecated-var` | Replacing a deprecated var requires knowing the recommended replacement |
| `:discouraged-java-method` | Replacing a discouraged method requires knowing the configured replacement |
| `:discouraged-namespace` | Replacing a discouraged namespace requires knowing the configured replacement |
| `:discouraged-var` | Replacing a discouraged var requires knowing the configured replacement |
| `:file` | File I/O errors cannot be auto-fixed |
| `:hook` | Hook-related lint; not a code correctness issue |
| `:loop-without-recur` | Structural fix (adding recur) requires understanding loop semantics and intent |
| `:main-without-gen-class` | Requires adding :gen-class to ns form, which may change compilation behavior |
| `:missing-docstring` | Writing a meaningful docstring requires human authorship |
| `:missing-protocol-method` | Generating a protocol method implementation requires knowing the intended behavior |
| `:missing-protocol-method-arity` | Same as missing-protocol-method |
| `:missing-test-assertion` | Writing a test assertion requires human authorship |
| `:namespace-name-mismatch` | Renaming either the file or the ns declaration is a multi-file operation |
| `:protocol-method-arity-mismatch` | Resolving an arity mismatch requires understanding the intended protocol contract |
| `:protocol-method-varargs` | Varargs protocol methods require structural refactoring |
| `:redefined-var` | Deciding which definition to keep or merge requires human judgment |
| `:refer-all` | Strips :refer :all but cannot determine which symbols are
actually used without analysis data; produces broken namespaces for files that
rely on referred symbols |
| `:schema-misplaced-return` | Plumatic Schema placement requires understanding the schema structure |
| `:self-requiring-namespace` | Circular self-require must be resolved by removing the problematic require manually |
| `:syntax` | Syntax errors cannot be automatically corrected |
| `:type-mismatch` | Type errors require type inference context unavailable at text-transformation level |
| `:unresolved-namespace` | Cannot create or locate a missing namespace automatically |
| `:unresolved-protocol-method` | Resolving a missing protocol method requires human implementation |
| `:unresolved-symbol` | Cannot create or locate a missing symbol automatically |
| `:unresolved-var` | Cannot create or locate a missing var automatically |
| `:warn-on-reflection` | Requires adding *warn-on-reflection* binding; intent and placement are contextual |