# clj-kondo-fix Rule Index

38 implemented · 49 not yet implemented · 33 not applicable · 0 skipped

## Index

- [:alias-same-as-ns](#alias-same-as-ns)
- [:aliased-namespace-symbol](#aliased-namespace-symbol)
- [:aliased-namespace-var-usage](#aliased-namespace-var-usage)
- [:aliased-referred-var](#aliased-referred-var)
- [:await-without-async-fn](#await-without-async-fn) ❌
- [:case-duplicate-test](#case-duplicate-test)
- [:case-quoted-test](#case-quoted-test) ❌
- [:case-symbol-test](#case-symbol-test)
- [:clj-kondo-config](#clj-kondo-config) ❌
- [:cond-else](#cond-else) ✅
- [:condition-always-true](#condition-always-true) ✅
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
- [:docstring-blank](#docstring-blank) ✅
- [:docstring-leading-trailing-whitespace](#docstring-leading-trailing-whitespace) ✅
- [:docstring-no-summary](#docstring-no-summary)
- [:duplicate-field-name](#duplicate-field-name)
- [:duplicate-key-args](#duplicate-key-args)
- [:duplicate-map-key](#duplicate-map-key)
- [:duplicate-refer](#duplicate-refer)
- [:duplicate-require](#duplicate-require) ✅
- [:duplicate-set-key](#duplicate-set-key)
- [:dynamic-var-not-earmuffed](#dynamic-var-not-earmuffed) ✅
- [:earmuffed-var-not-dynamic](#earmuffed-var-not-dynamic) ✅
- [:equals-expected-position](#equals-expected-position) ✅
- [:equals-false](#equals-false) ✅
- [:equals-float](#equals-float) ✅
- [:equals-nil](#equals-nil) ✅
- [:equals-true](#equals-true) ✅
- [:file](#file) ❌
- [:format](#format)
- [:hook](#hook) ❌
- [:if-nil-return](#if-nil-return) ✅
- [:if-x-x-y](#if-x-x-y) ✅
- [:inline-def](#inline-def)
- [:is-message-not-string](#is-message-not-string)
- [:java-static-field-call](#java-static-field-call)
- [:line-length](#line-length)
- [:loop-without-recur](#loop-without-recur) ❌
- [:main-without-gen-class](#main-without-gen-class) ❌
- [:minus-one](#minus-one) ✅
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
- [:plus-one](#plus-one) ✅
- [:private-call](#private-call)
- [:protocol-method-arity-mismatch](#protocol-method-arity-mismatch) ❌
- [:protocol-method-varargs](#protocol-method-varargs) ❌
- [:quoted-case-test-constant](#quoted-case-test-constant) ❌
- [:redefined-var](#redefined-var) ❌
- [:redundant-call](#redundant-call) ✅
- [:redundant-declare](#redundant-declare) ✅
- [:redundant-do](#redundant-do) ✅
- [:redundant-fn-wrapper](#redundant-fn-wrapper) ✅
- [:redundant-format](#redundant-format) ✅
- [:redundant-ignore](#redundant-ignore)
- [:redundant-let](#redundant-let) ✅
- [:redundant-let-binding](#redundant-let-binding) ✅
- [:redundant-nested-call](#redundant-nested-call) ✅
- [:redundant-primitive-coercion](#redundant-primitive-coercion) ✅
- [:redundant-str-call](#redundant-str-call) ✅
- [:refer](#refer)
- [:refer-all](#refer-all) ❌
- [:schema-misplaced-return](#schema-misplaced-return) ❌
- [:self-requiring-namespace](#self-requiring-namespace) ❌
- [:shadowed-fn-param](#shadowed-fn-param)
- [:shadowed-var](#shadowed-var)
- [:single-key-in](#single-key-in) ✅
- [:single-logical-operand](#single-logical-operand) ✅
- [:single-operand-comparison](#single-operand-comparison)
- [:syntax](#syntax) ❌
- [:type-mismatch](#type-mismatch) ❌
- [:unbound-destructuring-default](#unbound-destructuring-default)
- [:underscore-in-namespace](#underscore-in-namespace)
- [:unexpected-recur](#unexpected-recur)
- [:uninitialized-var](#uninitialized-var) ✅
- [:unknown-ns-option](#unknown-ns-option)
- [:unknown-require-option](#unknown-require-option)
- [:unquote-not-syntax-quoted](#unquote-not-syntax-quoted)
- [:unreachable-code](#unreachable-code)
- [:unresolved-excluded-var](#unresolved-excluded-var)
- [:unresolved-namespace](#unresolved-namespace) ❌
- [:unresolved-protocol-method](#unresolved-protocol-method) ❌
- [:unresolved-symbol](#unresolved-symbol) ❌
- [:unresolved-var](#unresolved-var) ❌
- [:unsorted-imports](#unsorted-imports) ✅
- [:unsorted-required-namespaces](#unsorted-required-namespaces) ✅
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

### :cond-else

**Cond-else**

warn on `cond` with a different constant for the else branch than `:else`

```clojure
(cond (odd? 1) :foo :default :bar)
```

↓

```clojure
(cond (odd? 1) :foo :else :bar)
```

---

### :condition-always-true

**Condition always true**

warn on a condition that evaluates to an always truthy constant,

**(if 1 :then) → :then**

```clojure
(defn f [] (if 1 :then))
```

↓

```clojure
(defn f [] :then)
```

---

**(when 1 :body) → :body**

```clojure
(defn f [] (when 1 :body))
```

↓

```clojure
(defn f [] :body)
```

---

**(if 1 :then :else) → :then**

```clojure
(defn f [] (if 1 :then :else))
```

↓

```clojure
(defn f [] :then)
```

---

### :docstring-blank

**Docstring blank**

warn on blank docstring

```clojure
(defn foo "" [a b] 1)
```

↓

```clojure
(defn foo [a b] 1)
```

---

### :docstring-leading-trailing-whitespace

**Docstring leading trailing whitespace**

warn when docstring has leading or trailing whitespace

**docstring leading/trailing whitespace**

```clojure
(defn foo " text " [a b] 1)
```

↓

```clojure
(defn foo "text" [a b] 1)
```

---

### :duplicate-require

**Duplicate require**

warns on namespace that has been required more than once within a namespace

**only the first alias is used; duplicate (second) entry removed, no renames needed**

```clojure
(ns foo (:require [clojure.string :as s]
                  [clojure.string :as str]))

(s/join [""] "")
```

↓

```clojure
(ns foo (:require [clojure.string :as s]))

(s/join [""] "")
```

---

**both aliases used; longer alias wins — shorter alias usages renamed and its entry removed**

```clojure
(ns foo
  (:require [my.tools :as pt]
            [my.tools :as toolz]))

(pt/make-endpoint :x)
(toolz/make-exception {})
```

↓

```clojure
(ns foo
  (:require [my.tools :as toolz]))

(toolz/make-endpoint :x)
(toolz/make-exception {})
```

---

### :dynamic-var-not-earmuffed

**Dynamic vars**

warn when dynamic var doesn't have an earmuffed name

```clojure
(def ^:dynamic x 1)
```

↓

```clojure
(def ^:dynamic *x* 1)
```

---

### :earmuffed-var-not-dynamic

**Dynamic vars**

warn when var with earmuffed name isn't declared dynamic

```clojure
(def *x* 1)
```

↓

```clojure
(def ^:dynamic *x* 1)
```

---

### :equals-expected-position

**Equals expected position**

warn on usage of `=` with the expected value, a constant, that is not in the expected (first by default) position

```clojure
(= x 1)
```

↓

```clojure
(= 1 x)
```

---

### :equals-false

**Equals false**

warn on usage of `(= false x)` or `(= x false)` rather than `(false? x)`

**false is the first argument; replaced with (false? x)**

```clojure
(defn check [x]
  (= false x))
```

↓

```clojure
(defn check [x]
  (false? x))
```

---

**false is the second argument; replaced with (false? x)**

```clojure
(defn check [x]
  (= x false))
```

↓

```clojure
(defn check [x]
  (false? x))
```

---

### :equals-float

**Equals float**

warn on usage of comparison with `=` on floating point numbers,

**(= x 0.5) → (== x 0.5)**

```clojure
(= x 0.5)
```

↓

```clojure
(== x 0.5)
```

---

**(= 0.1 x) → (== 0.1 x)**

```clojure
(= 0.1 x)
```

↓

```clojure
(== 0.1 x)
```

---

### :equals-nil

**Equals nil**

warn on usage of `(= nil x)` or `(= x nil)` rather than `(nil? x)`

**nil is the first argument; replaced with (nil? x)**

```clojure
(defn check [x]
  (= nil x))
```

↓

```clojure
(defn check [x]
  (nil? x))
```

---

**nil is the second argument; replaced with (nil? x)**

```clojure
(defn check [x]
  (= x nil))
```

↓

```clojure
(defn check [x]
  (nil? x))
```

---

### :equals-true

**Equals true**

warn on usage of `(= true x)` or `(= x true)` rather than `(true? x)`

**true is the first argument; replaced with (true? x)**

```clojure
(defn check [x]
  (= true x))
```

↓

```clojure
(defn check [x]
  (true? x))
```

---

**true is the second argument; replaced with (true? x)**

```clojure
(defn check [x]
  (= x true))
```

↓

```clojure
(defn check [x]
  (true? x))
```

---

### :if-nil-return

**Nil return from if-like forms**

warn when if-like form explicitly returns nil from either

**(if x y nil) → (when x y)**

```clojure
(defn f [x y] (if x y nil))
```

↓

```clojure
(defn f [x y] (when x y))
```

---

**(if x nil y) → (when-not x y)**

```clojure
(defn f [x y] (if x nil y))
```

↓

```clojure
(defn f [x y] (when-not x y))
```

---

### :if-x-x-y

**If x x y**

warn on `(if x x y)` and suggest `(or x y)` instead when `x` is a

**(if x x y) → (or x y)**

```clojure
(defn f [x y] (if x x y))
```

↓

```clojure
(defn f [x y] (or x y))
```

---

### :minus-one

**Minus one**

warn on usages of `-` that can be replaced with `dec`

**(- x 1) is replaced with (dec x)**

```clojure
(defn f [x] (- x 1))
```

↓

```clojure
(defn f [x] (dec x))
```

---

### :misplaced-docstring

**Misplaced docstring**

warn when docstring appears after argument vector instead of before

**docstring placed after the param vector; moved before it to correct position**

```clojure
(defn my-fn [x y]
  "does something"
  (+ x y))
```

↓

```clojure
(defn my-fn
  "does something"
  [x y]
  (+ x y))
```

---

### :missing-else-branch

**Missing else branch**

warns about missing else branch in `if` expression

**bare (if cond then) with no else branch; converted to (when ...)**

```clojure
(if true 1)
```

↓

```clojure
(when true 1)
```

---

**multiple if-family forms on one line all lacking else branches; all converted to when-family**

```clojure
(if true 1) (if-not true 1) (if-let [x 1] x) (if-some [x 1] x)
```

↓

```clojure
(when true 1) (when-not true 1) (when-let [x 1] x) (when-some [x 1] x)
```

---

### :plus-one

**Plus one**

warn on usages of `+` that can be replaced with `inc`

**1 is the first argument; replaced with (inc x)**

```clojure
(defn f [x] (+ 1 x))
```

↓

```clojure
(defn f [x] (inc x))
```

---

**1 is the second argument; replaced with (inc x)**

```clojure
(defn f [x] (+ x 1))
```

↓

```clojure
(defn f [x] (inc x))
```

---

### :redundant-call

**Redundant call**

warn on redundant calls. The warning arises when a single argument

**(-> 1) → 1**

```clojure
(-> 1)
```

↓

```clojure
1
```

---

**(merge {:a 1}) → {:a 1}**

```clojure
(merge {:a 1})
```

↓

```clojure
{:a 1}
```

---

### :redundant-declare

**Redundant declare**

warn when `declare` is used after a var is already defined in the same namespace

```clojure
(defn foo [] 1) (declare foo)
```

↓

```clojure
(defn foo [] 1)
```

---

```clojure
(defn foo [] 1) (declare foo bar)
```

↓

```clojure
(defn foo [] 1) (declare bar)
```

---

```clojure
(defn foo [] 1) (defn bar [] 2) (declare foo bar)
```

↓

```clojure
(defn foo [] 1) (defn bar [] 2)
```

---

### :redundant-do

**Redundant do**

warn on usage of do that is redundant. The warning usually arises

**single-line (when (do ...)); do wrapper removed, extra spaces collapsed**

```clojure
(when true (do (println "a") (println "b")))
```

↓

```clojure
(when true (println "a") (println "b"))
```

---

**multi-line (when (do ...)); do line removed and body dedented two spaces**

```clojure
(when true
  (do
    (println "a")
    (println "b")))
```

↓

```clojure
(when true
  (println "a")
  (println "b"))
```

---

### :redundant-fn-wrapper

**Redundant fn wrapper**

warn on redundant function wrapper

**#(identity %) → identity**

```clojure
(map #(identity %) [1 2 3])
```

↓

```clojure
(map identity [1 2 3])
```

---

### :redundant-format

**Redundant format**

warn when format strings contain no format specifiers

**(format "hello") → "hello"**

```clojure
(defn f [] (format "hello"))
```

↓

```clojure
(defn f [] "hello")
```

---

### :redundant-let

**Redundant let**

warn on usage of let that is redundant. The warning usually arises

**nested lets on one line with a body; inner bindings merged into outer, body preserved**

```clojure
(let [x 2] (let [y 1] (+ x y)))
```

↓

```clojure
(let [x 2 y 1] (+ x y))
```

---

**inner let has multiple bindings; all merged into outer binding vector**

```clojure
(let [a 1]
  (let [b 2
        c 3]
    (+ a b c)))
```

↓

```clojure
(let [a 1
      b 2
      c 3]
  (+ a b c))
```

---

### :redundant-let-binding

**Redundant let binding**

warn on redundant binding of a symbol to itself. Excludes

```clojure
(let [x x] x)
```

↓

```clojure
(let [] x)
```

---

```clojure
(let [y 1 x x] y)
```

↓

```clojure
(let [y 1] y)
```

---

```clojure
(let [x x y 1] y)
```

↓

```clojure
(let [y 1] y)
```

---

### :redundant-nested-call

**Redundant nested call**

warn on redundant nested call of functions and macros

**(+ 1 2 (+ 1 2 3)) → (+ 1 2 1 2 3)**

```clojure
(+ 1 2 (+ 1 2 3))
```

↓

```clojure
(+ 1 2 1 2 3)
```

---

### :redundant-primitive-coercion

**Redundant primitive coercion**

warn on redundant primitive coercion calls. The warning arises when a

**(double (double 1)) → (double 1)**

```clojure
(defn f [] (double (double 1)))
```

↓

```clojure
(defn f [] (double 1))
```

---

### :redundant-str-call

**Redundant str call**

warn on redundant `str` calls. The warning arises when a single argument

**(str "hello") → "hello"**

```clojure
(defn greet [] (str "hello"))
```

↓

```clojure
(defn greet [] "hello")
```

---

### :single-key-in

**Single key in**

warn on associative path function with a single value path

**(get-in m [:k]) → (get m :k)**

```clojure
(defn f [m] (get-in m [:k]))
```

↓

```clojure
(defn f [m] (get m :k))
```

---

**(get-in {:a [1 2]} [:a]) → (get {:a [1 2]} :a)**

```clojure
(defn f [] (get-in {:a [1 2]} [:a]))
```

↓

```clojure
(defn f [] (get {:a [1 2]} :a))
```

---

### :single-logical-operand

**Single logical operand**

warn on single operand logical operators with always the same value

**(or x) collapses to x**

```clojure
(defn f [x] (or x))
```

↓

```clojure
(defn f [x] x)
```

---

**(and x) collapses to x**

```clojure
(defn f [x] (and x))
```

↓

```clojure
(defn f [x] x)
```

---

### :uninitialized-var

**Uninitialized var**

warn on var without initial value

```clojure
(def x)
```

↓

```clojure
(def x nil)
```

---

### :unsorted-imports

**Unsorted imports**

warns on non-alphabetically sorted imports in `ns` and `require` forms

```clojure
(ns test
  (:import [java.util Date ArrayList] [java.io File]))
```

↓

```clojure
(ns test
  (:import [java.io File] [java.util Date ArrayList]))
```

---

```clojure
(ns test
  (:import
   [java.util Date ArrayList]
   [java.io File]))
```

↓

```clojure
(ns test
  (:import [java.io File] [java.util Date ArrayList])
```

---

### :unsorted-required-namespaces

**Unsorted required namespaces**

warns on non-alphabetically sorted libspecs in `ns` and `require` forms

```clojure
(ns test
  (:require [b.core] [a.core]))
```

↓

```clojure
(ns test
  (:require [a.core] [b.core]))
```

---

```clojure
(ns test
  (:require
   [b.core :as b]
   [a.core :as a]))
```

↓

```clojure
(ns test
  (:require
   [a.core :as a]
   [b.core :as b]))
```

---

```clojure
(ns test
  (:require [b.core :as b]
            [a.core :as a]))
```

↓

```clojure
(ns test
  (:require [a.core :as a]
            [b.core :as b]))
```

---

### :unused-binding

**Unused binding**

warn on unused binding

**simple unused fn param; prefixed with _ to signal intentional non-use**

```clojure
(defn foo [x])
```

↓

```clojure
(defn foo [_x])
```

---

**:as config unused but :keys binding is used; :as clause removed entirely**

```clojure
(defn f [{:keys [a] :as config}] a)
```

↓

```clojure
(defn f [{:keys [a]}] a)
```

---

**first key in :keys vector unused; key removed, remaining keys preserved**

```clojure
(defn f [{:keys [x y z]}] (+ y z))
```

↓

```clojure
(defn f [{:keys [y z]}] (+ y z))
```

---

**unused namespaced key patient/id in :keys destructuring; full token removed, order/id kept**

```clojure
(let [{:keys [patient/id order/id]} {}] id)
```

↓

```clojure
(let [{:keys [order/id]} {}] id)
```

---

### :unused-import

**Unused import**

warn on unused import

**one of two classes in an import group unused; that class removed, other preserved**

```clojure
(ns foo (:import [java.util Date List]))
```

↓

```clojure
(ns foo (:import [java.util Date]))
```

---

**last remaining class in an import group unused; entire group removed, no bare [package] left**

```clojure
(ns foo
  (:import [java.time Instant]))
```

↓

```clojure
(ns foo)
```

---

### :unused-namespace

**Unused namespace**

warns on required but unused namespace

**single unused require on its own line; entry removed and empty :require clause cleaned up**

```clojure
(ns foo
  (:require [clojure.string :as s]))
```

↓

```clojure
(ns foo)
```

---

**both requires unused; entries and the entire :require block removed, ns closes cleanly**

```clojure
(ns foo
  (:require [clojure.string :as s]
            [clojure.set :as cs]))
```

↓

```clojure
(ns foo)
```

---

**removed entry has a trailing ;; comment on the same line; comment removed with the entry**

```clojure
(ns foo
  (:require [clojure.string :as s]
            [clojure.set :as cs] ;; for set ops
            ))

(s/join [""] "")
```

↓

```clojure
(ns foo
  (:require [clojure.string :as s]))

(s/join [""] "")
```

---

### :unused-private-var

**Unused private var**

warns on unused private vars

**unused defn- form; entire defn- removed including its preceding blank line**

```clojure
(ns foo)

(defn- helper [])

(defn public [] :ok)
```

↓

```clojure
(ns foo)

(defn public [] :ok)
```

---

**unused multi-line def ^:private form; entire form removed**

```clojure
(ns foo)

(def ^:private
  default-str
  [:re "^[a-z]+$"])

(defn public [] :ok)
```

↓

```clojure
(ns foo)

(defn public [] :ok)
```

---

### :unused-referred-var

**Unused referred var**

warns about unused referred vars

**one referred var unused, other is used; unused var removed, used one stays**

```clojure
(ns foo (:require [clojure.string :refer [join ends-with?]]))

(join [""] "")
```

↓

```clojure
(ns foo (:require [clojure.string :refer [join]]))

(join [""] "")
```

---

**entry has no :as alias and all :refer vars removed; entire require entry removed**

```clojure
(ns foo
  (:require [clojure.string :as s]
            [clojure.set :refer [rename-keys]]))

(s/join [""] "")
```

↓

```clojure
(ns foo
  (:require [clojure.string :as s]))

(s/join [""] "")
```

## Not Yet Implemented

These rules could potentially be auto-fixed but have not been tackled yet.

| Rule | Description |
| --- | --- |
| `:alias-same-as-ns` | warn when alias is the same as the namespace it is aliasing |
| `:aliased-namespace-symbol` | warn when the namespace of a qualified symbol has a defined alias |
| `:aliased-namespace-var-usage` | warn when a var from a namespace that was used with `:as-alias` is used |
| `:aliased-referred-var` | warn when a var is both referred and accessed via an alias in the same namespace |
| `:case-duplicate-test` | identify duplicate case test constants |
| `:case-symbol-test` | Warn on symbol test constants in `case`. Sometimes this is |
| `:conditional-build-up` | warn when a `let` repeatedly rebinds the same local map using forms like `(if pred (assoc m ...) m)`, which can often be written more clearly with `cond->` |
| `:conflicting-alias` | warn on conflicting alias |
| `:def-fn` | tells about closures defined with the combination of |
| `:destructured-or-always-evaluates` | Warn when an `:or` default value in a destructuring contains an |
| `:destructured-or-binding-of-same-map` | an `:or` default value refers to a destructured binding of the |
| `:discouraged-tag` | warn on the usage of a tagged literal that is discouraged to be used |
| `:do-template` | warn on incorrect usages of `clojure.template/do-template`: no args, no values, or incorrect number of values |
| `:docstring-no-summary` | warn when first _line_ of docstring is not a complete |
| `:duplicate-field-name` | identify duplicate fields in deftype/defrecord fields definition |
| `:duplicate-key-args` | identify duplicate key args in calls to `assoc`, `dissoc`, `hash-map` etc |
| `:duplicate-map-key` | warn on duplicate key in map |
| `:duplicate-refer` | warns on var that has been referred more than once in a `:refer` or `:refer-macros` vector |
| `:duplicate-set-key` | similar to `:duplicate-map-key` but for sets |
| `:format` | warn on unexpected amount of arguments in `format` |
| `:inline-def` | warn on non-toplevel usage of `def` (and `defn`, etc.) |
| `:is-message-not-string` | warn when `clojure.test/is` receives a non-string message argument. This linter relies on the `:type-mismatch` linter being enabled to perform type checking |
| `:java-static-field-call` | warn when invoking a static field on a Java object |
| `:line-length` | warn when lines are longer than a configured length |
| `:misplaced-async-metadata` | warns when `^:async` metadata is placed where ClojureScript ignores it: on a function's argument vector or on the whole `(fn ...)` form. It must go on the function name. ClojureScript only |
| `:missing-body-in-when` | warn when `when` is called only with a condition |
| `:missing-clause-in-try` | warn when `try` expression misses `catch` or `finally` clause |
| `:missing-map-value` | warn on key with uneven amount of elements, i.e. one of the keys |
| `:non-arg-vec-return-type-hint` | warn when a return type in `defn` is not placed on the argument vector (CLJ only) |
| `:private-call` | warn when private var is used. The name of this linter should be |
| `:redundant-ignore` | warn on redundant ignore, i.e. when an ignored expression doesn't trigger any lint warning |
| `:refer` | warns when `:refer` is used. This can be used when one wants to |
| `:shadowed-fn-param` | warn on fn param that has same name as previously defined one (in the same fn expression) |
| `:shadowed-var` | warn on var that is shadowed by local |
| `:single-operand-comparison` | warn on comparison with only one argument |
| `:unbound-destructuring-default` | warn on binding in `:or` which does not occur in destructuring |
| `:underscore-in-namespace` | warns about the usage of the `_` character in the declaration of namespaces (as opposed to `-`) |
| `:unexpected-recur` | `(recur ...)` is called where it's not expected |
| `:unknown-ns-option` | warn on unknown top-level `ns` options |
| `:unknown-require-option` | warn on unknown `:require` option pairs |
| `:unquote-not-syntax-quoted` | warns when unquote (`~`) or unquote-splicing (`~@`) is used outside of syntax-quote (`` ` ``) |
| `:unreachable-code` | warn on unreachable code |
| `:unresolved-excluded-var` | warns when `:refer-clojure :exclude` contains vars that do not exist in clojure.core or cljs.core |
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
| `:case-quoted-test` | Removing the quote is trivial but the user may have intended the quoted symbol as a runtime value; requires human judgment |
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
| `:quoted-case-test-constant` | Fix is trivial (remove single quote) but safety depends on whether the quoted form is intentional behavior in a performance-sensitive code path |
| `:redefined-var` | Deciding which definition to keep or merge requires human judgment |
| `:refer-all` | Cannot determine which symbols are actually used without analysis data; producing an explicit :refer list or :as alias requires domain knowledge |
| `:schema-misplaced-return` | Plumatic Schema placement requires understanding the schema structure |
| `:self-requiring-namespace` | Circular self-require must be resolved by removing the problematic require manually |
| `:syntax` | Syntax errors cannot be automatically corrected |
| `:type-mismatch` | Type errors require type inference context unavailable at text-transformation level |
| `:unresolved-namespace` | Cannot create or locate a missing namespace automatically |
| `:unresolved-protocol-method` | Resolving a missing protocol method requires human implementation |
| `:unresolved-symbol` | Cannot create or locate a missing symbol automatically |
| `:unresolved-var` | Cannot create or locate a missing var automatically |
| `:warn-on-reflection` | Requires adding *warn-on-reflection* binding; intent and placement are contextual |