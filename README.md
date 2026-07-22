# clj-kondo-fix

clj-kondo doesn't have a fix option.  this is to create a tool that runs exactly the same as clj-kondo to give the option to fix the code.

## Install

Requires the [Clojure CLI](https://clojure.org/guides/install_clojure) (`clojure`) on your PATH.

```bash
git clone https://github.com/your-org/clj-kondo-fix
cd clj-kondo-fix
./bin/link          # symlinks bin/clj-kondo-fix into /usr/local/bin
```

Pass a custom destination if you prefer a different directory:

```bash
./bin/link ~/bin
```

Verify the install:

```bash
clj-kondo-fix --help
```

The first run resolves and caches dependencies via the Clojure CLI — subsequent runs are faster.

To remove:

```bash
./bin/unlink        # removes the symlink from /usr/local/bin
./bin/unlink ~/bin  # or wherever you installed it
```

## Usage

### CLI

```bash
clojure -M:run --lint src/ --fix
```

Options:

| Flag | Description |
|------|-------------|
| `--lint PATH` | File or directory to lint (repeatable) |
| `--fix` | Enable auto-fix mode (without this, just runs kondo) |
| `--rules RULES` | Comma-separated list of active fix rules (default: all) |
| `--config EDN` | clj-kondo config map as EDN string |
| `--output FORMAT` | Output format: `text` (default) or `edn` |
| `--help` | Print usage |
| `--version` | Print version |

### Library

```clojure
(require '[clj-kondo-fix.core :as kfix])

;; Run lint and apply fixes
(def result (kfix/fix! {:lint ["src/"] :fix true}))

;; Print results
(kfix/print! result)
```

`fix!` returns a map with `:results` (per-file fix stats), `:findings`,
`:summary`, and `:log`.


## Projects

```clojure
;; deps.edn
{clj-kondo-fix/clj-kondo-fix {:local/root "/path/to/clj-kondo-fix"}}
```

## Development

```bash
# run tests
clojure -M:test

# run against a directory
clojure -M:run --lint src/ --fix
```

## Rules

✅ implemented · ❌ not applicable · ⚠️ skipped · (no icon) not yet implemented

See [rules.md](rules.md) for implementation notes and before/after examples.

- [:alias-same-as-ns](rules.md#alias-same-as-ns) ✅
- [:aliased-namespace-symbol](rules.md#aliased-namespace-symbol) ✅
- [:aliased-namespace-var-usage](rules.md#aliased-namespace-var-usage) ❌
- [:aliased-referred-var](rules.md#aliased-referred-var) ✅
- [:await-without-async-fn](rules.md#await-without-async-fn) ❌
- [:case-duplicate-test](rules.md#case-duplicate-test) ❌
- [:case-quoted-test](rules.md#case-quoted-test) ❌
- [:case-symbol-test](rules.md#case-symbol-test) ❌
- [:clj-kondo-config](rules.md#clj-kondo-config) ❌
- [:cond-else](rules.md#cond-else) ✅
- [:condition-always-true](rules.md#condition-always-true) ✅
- [:conditional-build-up](rules.md#conditional-build-up) ❌
- [:conflicting-alias](rules.md#conflicting-alias) ❌
- [:consistent-alias](rules.md#consistent-alias) ❌
- [:datalog-syntax](rules.md#datalog-syntax) ❌
- [:def-fn](rules.md#def-fn) ✅
- [:deprecated-namespace](rules.md#deprecated-namespace) ❌
- [:deprecated-var](rules.md#deprecated-var) ❌
- [:destructured-or-always-evaluates](rules.md#destructured-or-always-evaluates) ❌
- [:destructured-or-binding-of-same-map](rules.md#destructured-or-binding-of-same-map) ❌
- [:discouraged-java-method](rules.md#discouraged-java-method) ❌
- [:discouraged-namespace](rules.md#discouraged-namespace) ❌
- [:discouraged-tag](rules.md#discouraged-tag) ❌
- [:discouraged-var](rules.md#discouraged-var) ❌
- [:do-template](rules.md#do-template) ❌
- [:docstring-blank](rules.md#docstring-blank) ✅
- [:docstring-leading-trailing-whitespace](rules.md#docstring-leading-trailing-whitespace) ✅
- [:docstring-no-summary](rules.md#docstring-no-summary) ✅
- [:duplicate-field-name](rules.md#duplicate-field-name) ✅
- [:duplicate-key-args](rules.md#duplicate-key-args) ❌
- [:duplicate-map-key](rules.md#duplicate-map-key) ❌
- [:duplicate-refer](rules.md#duplicate-refer) ✅
- [:duplicate-require](rules.md#duplicate-require) ✅
- [:duplicate-set-key](rules.md#duplicate-set-key) ✅
- [:dynamic-var-not-earmuffed](rules.md#dynamic-var-not-earmuffed) ✅
- [:earmuffed-var-not-dynamic](rules.md#earmuffed-var-not-dynamic) ✅
- [:equals-expected-position](rules.md#equals-expected-position) ✅
- [:equals-false](rules.md#equals-false) ✅
- [:equals-float](rules.md#equals-float) ✅
- [:equals-nil](rules.md#equals-nil) ✅
- [:equals-true](rules.md#equals-true) ✅
- [:file](rules.md#file) ❌
- [:format](rules.md#format) ❌
- [:hook](rules.md#hook) ❌
- [:if-nil-return](rules.md#if-nil-return) ✅
- [:if-x-x-y](rules.md#if-x-x-y) ✅
- [:inline-def](rules.md#inline-def) ❌
- [:is-message-not-string](rules.md#is-message-not-string) ✅
- [:java-static-field-call](rules.md#java-static-field-call) ✅
- [:line-length](rules.md#line-length) ❌
- [:loop-without-recur](rules.md#loop-without-recur) ❌
- [:main-without-gen-class](rules.md#main-without-gen-class) ❌
- [:minus-one](rules.md#minus-one) ✅
- [:misplaced-async-metadata](rules.md#misplaced-async-metadata) ❌
- [:misplaced-docstring](rules.md#misplaced-docstring) ✅
- [:missing-body-in-when](rules.md#missing-body-in-when) ✅
- [:missing-clause-in-try](rules.md#missing-clause-in-try) ❌
- [:missing-docstring](rules.md#missing-docstring) ❌
- [:missing-else-branch](rules.md#missing-else-branch) ✅
- [:missing-map-value](rules.md#missing-map-value) ❌
- [:missing-protocol-method](rules.md#missing-protocol-method) ❌
- [:missing-protocol-method-arity](rules.md#missing-protocol-method-arity) ❌
- [:missing-test-assertion](rules.md#missing-test-assertion) ❌
- [:namespace-name-mismatch](rules.md#namespace-name-mismatch) ❌
- [:non-arg-vec-return-type-hint](rules.md#non-arg-vec-return-type-hint) ✅
- [:plus-one](rules.md#plus-one) ✅
- [:private-call](rules.md#private-call) ❌
- [:protocol-method-arity-mismatch](rules.md#protocol-method-arity-mismatch) ❌
- [:protocol-method-varargs](rules.md#protocol-method-varargs) ❌
- [:quoted-case-test-constant](rules.md#quoted-case-test-constant) ❌
- [:redefined-var](rules.md#redefined-var) ❌
- [:redundant-call](rules.md#redundant-call) ✅
- [:redundant-declare](rules.md#redundant-declare) ✅
- [:redundant-do](rules.md#redundant-do) ✅
- [:redundant-fn-wrapper](rules.md#redundant-fn-wrapper) ✅
- [:redundant-format](rules.md#redundant-format) ✅
- [:redundant-ignore](rules.md#redundant-ignore) ❌
- [:redundant-let](rules.md#redundant-let) ✅
- [:redundant-let-binding](rules.md#redundant-let-binding) ✅
- [:redundant-nested-call](rules.md#redundant-nested-call) ✅
- [:redundant-primitive-coercion](rules.md#redundant-primitive-coercion) ✅
- [:redundant-str-call](rules.md#redundant-str-call) ✅
- [:refer](rules.md#refer) ❌
- [:refer-all](rules.md#refer-all) ❌
- [:schema-misplaced-return](rules.md#schema-misplaced-return) ❌
- [:self-requiring-namespace](rules.md#self-requiring-namespace) ❌
- [:shadowed-fn-param](rules.md#shadowed-fn-param) ❌
- [:shadowed-var](rules.md#shadowed-var) ✅
- [:single-key-in](rules.md#single-key-in) ✅
- [:single-logical-operand](rules.md#single-logical-operand) ✅
- [:single-operand-comparison](rules.md#single-operand-comparison) ✅
- [:syntax](rules.md#syntax) ❌
- [:type-mismatch](rules.md#type-mismatch) ❌
- [:unbound-destructuring-default](rules.md#unbound-destructuring-default) ✅
- [:underscore-in-namespace](rules.md#underscore-in-namespace) ✅
- [:unexpected-recur](rules.md#unexpected-recur) ❌
- [:uninitialized-var](rules.md#uninitialized-var) ✅
- [:unknown-ns-option](rules.md#unknown-ns-option) ❌
- [:unknown-require-option](rules.md#unknown-require-option) ❌
- [:unquote-not-syntax-quoted](rules.md#unquote-not-syntax-quoted) ✅
- [:unreachable-code](rules.md#unreachable-code) ✅
- [:unresolved-excluded-var](rules.md#unresolved-excluded-var) ✅
- [:unresolved-namespace](rules.md#unresolved-namespace) ❌
- [:unresolved-protocol-method](rules.md#unresolved-protocol-method) ❌
- [:unresolved-symbol](rules.md#unresolved-symbol) ❌
- [:unresolved-var](rules.md#unresolved-var) ❌
- [:unsorted-imports](rules.md#unsorted-imports) ✅
- [:unsorted-required-namespaces](rules.md#unsorted-required-namespaces) ✅
- [:unused-alias](rules.md#unused-alias) ✅
- [:unused-binding](rules.md#unused-binding) ✅
- [:unused-excluded-var](rules.md#unused-excluded-var) ✅
- [:unused-import](rules.md#unused-import) ✅
- [:unused-namespace](rules.md#unused-namespace) ✅
- [:unused-private-var](rules.md#unused-private-var) ✅
- [:unused-referred-var](rules.md#unused-referred-var) ✅
- [:unused-value](rules.md#unused-value) ✅
- [:use](rules.md#use) ✅
- [:used-underscored-binding](rules.md#used-underscored-binding) ✅
- [:var-same-name-except-case](rules.md#var-same-name-except-case) ❌
- [:warn-on-reflection](rules.md#warn-on-reflection) ❌