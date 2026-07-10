# clj-kondo-fix Rule Index

Sourced from clj-kondo `linters.md`. 10 implemented | 1 skipped | 79 not implemented | 30 not applicable.

**Status key:**
- ✅ **implemented** — auto-fix available in clj-kondo-fix
- ⚠️ **skipped** — attempted but not safe to auto-fix automatically
- 🔲 **not implemented** — could potentially be auto-fixed; not yet done
- — **not applicable** — cannot be meaningfully auto-fixed

## Index

- [:alias-same-as-ns](#alias-same-as-ns)
- [:aliased-namespace-symbol](#aliased-namespace-symbol)
- [:aliased-namespace-var-usage](#aliased-namespace-var-usage)
- [:aliased-referred-var](#aliased-referred-var)
- [:await-without-async-fn](#await-without-async-fn) (not applicable)
- [:case-duplicate-test](#case-duplicate-test)
- [:case-quoted-test](#case-quoted-test)
- [:case-symbol-test](#case-symbol-test)
- [:clj-kondo-config](#clj-kondo-config) (not applicable)
- [:cond-else](#cond-else)
- [:condition-always-true](#condition-always-true)
- [:conditional-build-up](#conditional-build-up)
- [:conflicting-alias](#conflicting-alias)
- [:consistent-alias](#consistent-alias) (not applicable)
- [:datalog-syntax](#datalog-syntax) (not applicable)
- [:def-fn](#def-fn)
- [:deprecated-namespace](#deprecated-namespace) (not applicable)
- [:deprecated-var](#deprecated-var) (not applicable)
- [:destructured-or-always-evaluates](#destructured-or-always-evaluates)
- [:destructured-or-binding-of-same-map](#destructured-or-binding-of-same-map)
- [:discouraged-java-method](#discouraged-java-method) (not applicable)
- [:discouraged-namespace](#discouraged-namespace) (not applicable)
- [:discouraged-tag](#discouraged-tag)
- [:discouraged-var](#discouraged-var) (not applicable)
- [:do-template](#do-template)
- [:docstring-blank](#docstring-blank)
- [:docstring-leading-trailing-whitespace](#docstring-leading-trailing-whitespace)
- [:docstring-no-summary](#docstring-no-summary)
- [:duplicate-field-name](#duplicate-field-name)
- [:duplicate-key-args](#duplicate-key-args)
- [:duplicate-map-key](#duplicate-map-key)
- [:duplicate-refer](#duplicate-refer)
- [:duplicate-require](#duplicate-require) (implemented)
- [:duplicate-set-key](#duplicate-set-key)
- [:dynamic-var-not-earmuffed](#dynamic-var-not-earmuffed)
- [:earmuffed-var-not-dynamic](#earmuffed-var-not-dynamic)
- [:equals-expected-position](#equals-expected-position)
- [:equals-false](#equals-false)
- [:equals-float](#equals-float)
- [:equals-nil](#equals-nil)
- [:equals-true](#equals-true)
- [:file](#file) (not applicable)
- [:format](#format)
- [:hook](#hook) (not applicable)
- [:if-nil-return](#if-nil-return)
- [:if-x-x-y](#if-x-x-y)
- [:inline-def](#inline-def)
- [:is-message-not-string](#is-message-not-string)
- [:java-static-field-call](#java-static-field-call)
- [:line-length](#line-length)
- [:loop-without-recur](#loop-without-recur) (not applicable)
- [:main-without-gen-class](#main-without-gen-class) (not applicable)
- [:minus-one](#minus-one)
- [:misplaced-async-metadata](#misplaced-async-metadata)
- [:misplaced-docstring](#misplaced-docstring) (implemented)
- [:missing-body-in-when](#missing-body-in-when)
- [:missing-clause-in-try](#missing-clause-in-try)
- [:missing-docstring](#missing-docstring) (not applicable)
- [:missing-else-branch](#missing-else-branch) (implemented)
- [:missing-map-value](#missing-map-value)
- [:missing-protocol-method](#missing-protocol-method) (not applicable)
- [:missing-protocol-method-arity](#missing-protocol-method-arity) (not applicable)
- [:missing-test-assertion](#missing-test-assertion) (not applicable)
- [:namespace-name-mismatch](#namespace-name-mismatch) (not applicable)
- [:non-arg-vec-return-type-hint](#non-arg-vec-return-type-hint)
- [:plus-one](#plus-one)
- [:private-call](#private-call)
- [:protocol-method-arity-mismatch](#protocol-method-arity-mismatch) (not applicable)
- [:protocol-method-varargs](#protocol-method-varargs) (not applicable)
- [:quoted-case-test-constant](#quoted-case-test-constant)
- [:redefined-var](#redefined-var) (not applicable)
- [:redundant-call](#redundant-call)
- [:redundant-declare](#redundant-declare)
- [:redundant-do](#redundant-do) (implemented)
- [:redundant-fn-wrapper](#redundant-fn-wrapper)
- [:redundant-format](#redundant-format)
- [:redundant-ignore](#redundant-ignore)
- [:redundant-let](#redundant-let) (implemented)
- [:redundant-let-binding](#redundant-let-binding)
- [:redundant-nested-call](#redundant-nested-call)
- [:redundant-primitive-coercion](#redundant-primitive-coercion)
- [:redundant-str-call](#redundant-str-call)
- [:refer](#refer)
- [:refer-all](#refer-all) (skipped)
- [:schema-misplaced-return](#schema-misplaced-return) (not applicable)
- [:self-requiring-namespace](#self-requiring-namespace) (not applicable)
- [:shadowed-fn-param](#shadowed-fn-param)
- [:shadowed-var](#shadowed-var)
- [:single-key-in](#single-key-in)
- [:single-logical-operand](#single-logical-operand)
- [:single-operand-comparison](#single-operand-comparison)
- [:syntax](#syntax) (not applicable)
- [:type-mismatch](#type-mismatch) (not applicable)
- [:unbound-destructuring-default](#unbound-destructuring-default)
- [:underscore-in-namespace](#underscore-in-namespace)
- [:unexpected-recur](#unexpected-recur)
- [:uninitialized-var](#uninitialized-var)
- [:unknown-ns-option](#unknown-ns-option)
- [:unknown-require-option](#unknown-require-option)
- [:unquote-not-syntax-quoted](#unquote-not-syntax-quoted)
- [:unreachable-code](#unreachable-code)
- [:unresolved-excluded-var](#unresolved-excluded-var)
- [:unresolved-namespace](#unresolved-namespace) (not applicable)
- [:unresolved-protocol-method](#unresolved-protocol-method) (not applicable)
- [:unresolved-symbol](#unresolved-symbol) (not applicable)
- [:unresolved-var](#unresolved-var) (not applicable)
- [:unsorted-imports](#unsorted-imports)
- [:unsorted-required-namespaces](#unsorted-required-namespaces)
- [:unused-alias](#unused-alias)
- [:unused-binding](#unused-binding) (implemented)
- [:unused-excluded-var](#unused-excluded-var)
- [:unused-import](#unused-import) (implemented)
- [:unused-namespace](#unused-namespace) (implemented)
- [:unused-private-var](#unused-private-var) (implemented)
- [:unused-referred-var](#unused-referred-var) (implemented)
- [:unused-value](#unused-value)
- [:use](#use)
- [:used-underscored-binding](#used-underscored-binding)
- [:var-same-name-except-case](#var-same-name-except-case)
- [:warn-on-reflection](#warn-on-reflection) (not applicable)

## Rules

### :alias-same-as-ns

**Alias same as ns name**

warn when alias is the same as the namespace it is aliasing

| Status | 🔲 not implemented |
| --- | --- |

---

### :aliased-namespace-symbol

**Aliased namespace symbol**

warn when the namespace of a qualified symbol has a defined alias

| Status | 🔲 not implemented |
| --- | --- |

---

### :aliased-namespace-var-usage

**Aliased namespace var usage**

warn when a var from a namespace that was used with `:as-alias` is used

| Status | 🔲 not implemented |
| --- | --- |

---

### :aliased-referred-var

**Aliased referred var**

warn when a var is both referred and accessed via an alias in the same namespace

| Status | 🔲 not implemented |
| --- | --- |

---

### :await-without-async-fn

**Await without async fn**

warns when `cljs.core/await` is used outside a function carrying `^:async` metadata. ClojureScript only

| Status | — not applicable |
| --- | --- |

> Structural fix (wrapping fn in async) requires understanding intent

---

### :case-duplicate-test

**Case duplicate test**

identify duplicate case test constants

| Status | 🔲 not implemented |
| --- | --- |

---

### :case-quoted-test

**Case duplicate test**

Warn on quoted test constants in `case`, a common mistake when

| Status | 🔲 not implemented |
| --- | --- |

---

### :case-symbol-test

**Case duplicate test**

Warn on symbol test constants in `case`. Sometimes this is

| Status | 🔲 not implemented |
| --- | --- |

---

### :clj-kondo-config

**Clj-kondo config**

warn on common errors in `.clj-kondo/config` files

| Status | — not applicable |
| --- | --- |

> Config validation errors need human correction

---

### :cond-else

**Cond-else**

warn on `cond` with a different constant for the else branch than `:else`

| Status | 🔲 not implemented |
| --- | --- |

---

### :condition-always-true

**Condition always true**

warn on a condition that evaluates to an always truthy constant,

| Status | 🔲 not implemented |
| --- | --- |

---

### :conditional-build-up

**Conditional build-up**

warn when a `let` repeatedly rebinds the same local map using forms like `(if pred (assoc m ...) m)`, which can often be written more clearly with `cond->`

| Status | 🔲 not implemented |
| --- | --- |

---

### :conflicting-alias

**Conflicting-alias**

warn on conflicting alias

| Status | 🔲 not implemented |
| --- | --- |

---

### :consistent-alias

**Consistent-alias**

Sometimes it's desirable to have a consistent alias for certain

| Status | — not applicable |
| --- | --- |

> Requires a globally configured alias table; not deterministic from a single file

---

### :datalog-syntax

**Datalog syntax**

warn on invalid datalog syntax. This linter is implemented using

| Status | — not applicable |
| --- | --- |

> Invalid datalog syntax requires domain knowledge to correct

---

### :def-fn

**Def + fn instead of defn**

tells about closures defined with the combination of

| Status | 🔲 not implemented |
| --- | --- |

---

### :deprecated-namespace

**Deprecated namespace**

warn on usage of namespace that is deprecated

| Status | — not applicable |
| --- | --- |

> Replacing a deprecated namespace requires knowing the recommended replacement

---

### :deprecated-var

**Deprecated var**

warn on usage of var that is deprecated

| Status | — not applicable |
| --- | --- |

> Replacing a deprecated var requires knowing the recommended replacement

---

### :destructured-or-always-evaluates

**Destructured or always evaluates**

Warn when an `:or` default value in a destructuring contains an

| Status | 🔲 not implemented |
| --- | --- |

---

### :destructured-or-binding-of-same-map

**Destructured or binding of same map**

an `:or` default value refers to a destructured binding of the

| Status | 🔲 not implemented |
| --- | --- |

---

### :discouraged-java-method

**Discouraged Java method**

warn on the usage of a discouraged Java method

| Status | — not applicable |
| --- | --- |

> Replacing a discouraged method requires knowing the configured replacement

---

### :discouraged-namespace

**Discouraged namespace**

warn on the require or usage of a namespace that is discouraged to be used

| Status | — not applicable |
| --- | --- |

> Replacing a discouraged namespace requires knowing the configured replacement

---

### :discouraged-tag

**Discouraged tag**

warn on the usage of a tagged literal that is discouraged to be used

| Status | 🔲 not implemented |
| --- | --- |

---

### :discouraged-var

**Discouraged var**

warn on the usage of a var that is discouraged to be used

| Status | — not applicable |
| --- | --- |

> Replacing a discouraged var requires knowing the configured replacement

---

### :do-template

**Do-template ###**

warn on incorrect usages of `clojure.template/do-template`: no args, no values, or incorrect number of values

| Status | 🔲 not implemented |
| --- | --- |

---

### :docstring-blank

**Docstring blank**

warn on blank docstring

| Status | 🔲 not implemented |
| --- | --- |

---

### :docstring-leading-trailing-whitespace

**Docstring leading trailing whitespace**

warn when docstring has leading or trailing whitespace

| Status | 🔲 not implemented |
| --- | --- |

---

### :docstring-no-summary

**Docstring no summary**

warn when first _line_ of docstring is not a complete

| Status | 🔲 not implemented |
| --- | --- |

---

### :duplicate-field-name

**Duplicate field name**

identify duplicate fields in deftype/defrecord fields definition

| Status | 🔲 not implemented |
| --- | --- |

---

### :duplicate-key-args

**Duplicate key args**

identify duplicate key args in calls to `assoc`, `dissoc`, `hash-map` etc

| Status | 🔲 not implemented |
| --- | --- |

---

### :duplicate-map-key

**Duplicate map key**

warn on duplicate key in map

| Status | 🔲 not implemented |
| --- | --- |

---

### :duplicate-refer

**Duplicate refer**

warns on var that has been referred more than once in a `:refer` or `:refer-macros` vector

| Status | 🔲 not implemented |
| --- | --- |

---

### :duplicate-require

**Duplicate require**

warns on namespace that has been required more than once within a namespace

| Status | ✅ implemented |
| --- | --- |

---

### :duplicate-set-key

**Duplicate set key**

similar to `:duplicate-map-key` but for sets

| Status | 🔲 not implemented |
| --- | --- |

---

### :dynamic-var-not-earmuffed

**Dynamic vars**

warn when dynamic var doesn't have an earmuffed name

| Status | 🔲 not implemented |
| --- | --- |

---

### :earmuffed-var-not-dynamic

**Dynamic vars**

warn when var with earmuffed name isn't declared dynamic

| Status | 🔲 not implemented |
| --- | --- |

---

### :equals-expected-position

**Equals expected position**

warn on usage of `=` with the expected value, a constant, that is not in the expected (first by default) position

| Status | 🔲 not implemented |
| --- | --- |

---

### :equals-false

**Equals false**

warn on usage of `(= false x)` or `(= x false)` rather than `(false? x)`

| Status | 🔲 not implemented |
| --- | --- |

---

### :equals-float

**Equals float**

warn on usage of comparison with `=` on floating point numbers,

| Status | 🔲 not implemented |
| --- | --- |

---

### :equals-nil

**Equals nil**

warn on usage of `(= nil x)` or `(= x nil)` rather than `(nil? x)`

| Status | 🔲 not implemented |
| --- | --- |

---

### :equals-true

**Equals true**

warn on usage of `(= true x)` or `(= x true)` rather than `(true? x)`

| Status | 🔲 not implemented |
| --- | --- |

---

### :file

**File**

warn on error while reading file

| Status | — not applicable |
| --- | --- |

> File I/O errors cannot be auto-fixed

---

### :format

**Format**

warn on unexpected amount of arguments in `format`

| Status | 🔲 not implemented |
| --- | --- |

---

### :hook

**Hook**

a `:macroexpand` or `:analyze-call` hook (including

| Status | — not applicable |
| --- | --- |

> Hook-related lint; not a code correctness issue

---

### :if-nil-return

**Nil return from if-like forms**

warn when if-like form explicitly returns nil from either

| Status | 🔲 not implemented |
| --- | --- |

---

### :if-x-x-y

**If x x y**

warn on `(if x x y)` and suggest `(or x y)` instead when `x` is a

| Status | 🔲 not implemented |
| --- | --- |

---

### :inline-def

**Inline def**

warn on non-toplevel usage of `def` (and `defn`, etc.)

| Status | 🔲 not implemented |
| --- | --- |

---

### :is-message-not-string

**Is message not string**

warn when `clojure.test/is` receives a non-string message argument. This linter relies on the `:type-mismatch` linter being enabled to perform type checking

| Status | 🔲 not implemented |
| --- | --- |

---

### :java-static-field-call

**Static field call**

warn when invoking a static field on a Java object

| Status | 🔲 not implemented |
| --- | --- |

---

### :line-length

**Line length**

warn when lines are longer than a configured length

| Status | 🔲 not implemented |
| --- | --- |

---

### :loop-without-recur

**Loop without recur**

warn when loop does not contain recur

| Status | — not applicable |
| --- | --- |

> Structural fix (adding recur) requires understanding loop semantics and intent

---

### :main-without-gen-class

**Main without gen-class**

warn when -main function is present without corresponding `:gen-class`

| Status | — not applicable |
| --- | --- |

> Requires adding :gen-class to ns form, which may change compilation behavior

---

### :minus-one

**Minus one**

warn on usages of `-` that can be replaced with `dec`

| Status | 🔲 not implemented |
| --- | --- |

---

### :misplaced-async-metadata

**Misplaced async metadata**

warns when `^:async` metadata is placed where ClojureScript ignores it: on a function's argument vector or on the whole `(fn ...)` form. It must go on the function name. ClojureScript only

| Status | 🔲 not implemented |
| --- | --- |

---

### :misplaced-docstring

**Misplaced docstring**

warn when docstring appears after argument vector instead of before

| Status | ✅ implemented |
| --- | --- |

---

### :missing-body-in-when

**Missing body in when**

warn when `when` is called only with a condition

| Status | 🔲 not implemented |
| --- | --- |

---

### :missing-clause-in-try

**Missing clause in try**

warn when `try` expression misses `catch` or `finally` clause

| Status | 🔲 not implemented |
| --- | --- |

---

### :missing-docstring

**Missing docstring**

warn when public var misses docstring

| Status | — not applicable |
| --- | --- |

> Writing a meaningful docstring requires human authorship

---

### :missing-else-branch

**Missing else branch**

warns about missing else branch in `if` expression

| Status | ✅ implemented |
| --- | --- |

---

### :missing-map-value

**Missing map value**

warn on key with uneven amount of elements, i.e. one of the keys

| Status | 🔲 not implemented |
| --- | --- |

---

### :missing-protocol-method

**Missing protocol method**

warn on missing protocol method

| Status | — not applicable |
| --- | --- |

> Generating a protocol method implementation requires knowing the intended behavior

---

### :missing-protocol-method-arity

**Missing protocol method arity**

warn when a protocol method is implemented but not all declared arities are covered

| Status | — not applicable |
| --- | --- |

> Same as missing-protocol-method

---

### :missing-test-assertion

**Missing test assertion**

warn on `deftest` expression without test assertion

| Status | — not applicable |
| --- | --- |

> Writing a test assertion requires human authorship

---

### :namespace-name-mismatch

**Namespace name mismatch**

warn when the namespace in the `ns` form does not

| Status | — not applicable |
| --- | --- |

> Renaming either the file or the ns declaration is a multi-file operation

---

### :non-arg-vec-return-type-hint

**Non-arg vec return type hint**

warn when a return type in `defn` is not placed on the argument vector (CLJ only)

| Status | 🔲 not implemented |
| --- | --- |

---

### :plus-one

**Plus one**

warn on usages of `+` that can be replaced with `inc`

| Status | 🔲 not implemented |
| --- | --- |

---

### :private-call

**Private call**

warn when private var is used. The name of this linter should be

| Status | 🔲 not implemented |
| --- | --- |

---

### :protocol-method-arity-mismatch

**Protocol method arity mismatch**

warn when a protocol method is implemented with an arity that doesn't match any arity declared in the protocol

| Status | — not applicable |
| --- | --- |

> Resolving an arity mismatch requires understanding the intended protocol contract

---

### :protocol-method-varargs

**Protocol method varargs**

warn on definition of varargs protocol method

| Status | — not applicable |
| --- | --- |

> Varargs protocol methods require structural refactoring

---

### :quoted-case-test-constant

**Quoted case test constant**

warn when encountering quoted test case constants

| Status | 🔲 not implemented |
| --- | --- |

---

### :redefined-var

**Redefined var**

warn on redefined var

| Status | — not applicable |
| --- | --- |

> Deciding which definition to keep or merge requires human judgment

---

### :redundant-call

**Redundant call**

warn on redundant calls. The warning arises when a single argument

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-declare

**Redundant declare**

warn when `declare` is used after a var is already defined in the same namespace

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-do

**Redundant do**

warn on usage of do that is redundant. The warning usually arises

| Status | ✅ implemented |
| --- | --- |

---

### :redundant-fn-wrapper

**Redundant fn wrapper**

warn on redundant function wrapper

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-format

**Redundant format**

warn when format strings contain no format specifiers

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-ignore

**Redundant ignore**

warn on redundant ignore, i.e. when an ignored expression doesn't trigger any lint warning

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-let

**Redundant let**

warn on usage of let that is redundant. The warning usually arises

| Status | ✅ implemented |
| --- | --- |

---

### :redundant-let-binding

**Redundant let binding**

warn on redundant binding of a symbol to itself. Excludes

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-nested-call

**Redundant nested call**

warn on redundant nested call of functions and macros

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-primitive-coercion

**Redundant primitive coercion**

warn on redundant primitive coercion calls. The warning arises when a

| Status | 🔲 not implemented |
| --- | --- |

---

### :redundant-str-call

**Redundant str call**

warn on redundant `str` calls. The warning arises when a single argument

| Status | 🔲 not implemented |
| --- | --- |

---

### :refer

**Refer**

warns when `:refer` is used. This can be used when one wants to

| Status | 🔲 not implemented |
| --- | --- |

---

### :refer-all

**Refer all**

warns when `:refer :all` is used

| Status | ⚠️ skipped |
| --- | --- |

> Strips :refer :all but cannot determine which symbols are actually used without analysis data; produces broken namespaces for files that rely on referred symbols

---

### :schema-misplaced-return

**Schema misplaced return**

warn on a misplaced return Schema

| Status | — not applicable |
| --- | --- |

> Plumatic Schema placement requires understanding the schema structure

---

### :self-requiring-namespace

**Self-requiring namespace**

warn on a namespace that requires itself

| Status | — not applicable |
| --- | --- |

> Circular self-require must be resolved by removing the problematic require manually

---

### :shadowed-fn-param

**Shadowed fn param**

warn on fn param that has same name as previously defined one (in the same fn expression)

| Status | 🔲 not implemented |
| --- | --- |

---

### :shadowed-var

**Shadowed var**

warn on var that is shadowed by local

| Status | 🔲 not implemented |
| --- | --- |

---

### :single-key-in

**Single key in**

warn on associative path function with a single value path

| Status | 🔲 not implemented |
| --- | --- |

---

### :single-logical-operand

**Single logical operand**

warn on single operand logical operators with always the same value

| Status | 🔲 not implemented |
| --- | --- |

---

### :single-operand-comparison

**Single operand comparison**

warn on comparison with only one argument

| Status | 🔲 not implemented |
| --- | --- |

---

### :syntax

**Syntax**

warn on invalid syntax

| Status | — not applicable |
| --- | --- |

> Syntax errors cannot be automatically corrected

---

### :type-mismatch

**Type mismatch**

warn on type mismatches, e.g. passing a keyword where a number is expected

| Status | — not applicable |
| --- | --- |

> Type errors require type inference context unavailable at text-transformation level

---

### :unbound-destructuring-default

**Unbound destructuring default**

warn on binding in `:or` which does not occur in destructuring

| Status | 🔲 not implemented |
| --- | --- |

---

### :underscore-in-namespace

**Underscore in namespace**

warns about the usage of the `_` character in the declaration of namespaces (as opposed to `-`)

| Status | 🔲 not implemented |
| --- | --- |

---

### :unexpected-recur

**Unexpected recur**

`(recur ...)` is called where it's not expected

| Status | 🔲 not implemented |
| --- | --- |

---

### :uninitialized-var

**Uninitialized var**

warn on var without initial value

| Status | 🔲 not implemented |
| --- | --- |

---

### :unknown-ns-option

**Unknown ns option**

warn on unknown top-level `ns` options

| Status | 🔲 not implemented |
| --- | --- |

---

### :unknown-require-option

**Unknown :require option**

warn on unknown `:require` option pairs

| Status | 🔲 not implemented |
| --- | --- |

---

### :unquote-not-syntax-quoted

**Unquote outside syntax-quote**

warns when unquote (`~`) or unquote-splicing (`~@`) is used outside of syntax-quote (`` ` ``)

| Status | 🔲 not implemented |
| --- | --- |

---

### :unreachable-code

**Unreachable code**

warn on unreachable code

| Status | 🔲 not implemented |
| --- | --- |

---

### :unresolved-excluded-var

**Unresolved excluded var**

warns when `:refer-clojure :exclude` contains vars that do not exist in clojure.core or cljs.core

| Status | 🔲 not implemented |
| --- | --- |

---

### :unresolved-namespace

**Unresolved namespace**

| Status | — not applicable |
| --- | --- |

> Cannot create or locate a missing namespace automatically

---

### :unresolved-protocol-method

**Unresolved protocol method**

warn on unresolved protocol method

| Status | — not applicable |
| --- | --- |

> Resolving a missing protocol method requires human implementation

---

### :unresolved-symbol

**Unresolved symbol**

| Status | — not applicable |
| --- | --- |

> Cannot create or locate a missing symbol automatically

---

### :unresolved-var

**Unresolved var**

warns on unresolved var from other namespace

| Status | — not applicable |
| --- | --- |

> Cannot create or locate a missing var automatically

---

### :unsorted-imports

**Unsorted imports**

warns on non-alphabetically sorted imports in `ns` and `require` forms

| Status | 🔲 not implemented |
| --- | --- |

---

### :unsorted-required-namespaces

**Unsorted required namespaces**

warns on non-alphabetically sorted libspecs in `ns` and `require` forms

| Status | 🔲 not implemented |
| --- | --- |

---

### :unused-alias

**Unused alias**

warn on unused alias introduced in ns form

| Status | 🔲 not implemented |
| --- | --- |

---

### :unused-binding

**Unused binding**

warn on unused binding

| Status | ✅ implemented |
| --- | --- |

---

### :unused-excluded-var

**Unused excluded var**

warns when `:refer-clojure :exclude` contains vars that are not redefined in the current namespace. Locals with the same name as an excluded var also count as a redefinition and will suppress this warning

| Status | 🔲 not implemented |
| --- | --- |

---

### :unused-import

**Unused import**

warn on unused import

| Status | ✅ implemented |
| --- | --- |

---

### :unused-namespace

**Unused namespace**

warns on required but unused namespace

| Status | ✅ implemented |
| --- | --- |

---

### :unused-private-var

**Unused private var**

warns on unused private vars

| Status | ✅ implemented |
| --- | --- |

---

### :unused-referred-var

**Unused referred var**

warns about unused referred vars

| Status | ✅ implemented |
| --- | --- |

---

### :unused-value

**Unused value**

warn on unused value: constants, unrealized lazy values, pure functions and transient ops (`assoc!`, `conj!` etc)

| Status | 🔲 not implemented |
| --- | --- |

---

### :use

**Use**

warns about `:use` or `use`

| Status | 🔲 not implemented |
| --- | --- |

---

### :used-underscored-binding

**Used underscored bindings**

warn when a underscored (ie marked as unused) binding is used

| Status | 🔲 not implemented |
| --- | --- |

---

### :var-same-name-except-case

**Var same name except case**

warn on vars that share the same name with different case (only in Clojure mode) as these could cause clashing class file names on case insensitive filesystems

| Status | 🔲 not implemented |
| --- | --- |

---

### :warn-on-reflection

**Warn on reflection**

warns about not setting `*warn-on-reflection*` to true in Clojure

| Status | — not applicable |
| --- | --- |

> Requires adding *warn-on-reflection* binding; intent and placement are contextual

---