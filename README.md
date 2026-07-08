# clj-kondo-fix

clj-kondo doesn't have a fix option.  this is to create a tool that runs exactly the same as clj-kondo to give the option to fix the code.

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

## Rules

The library can apply fixes for these clj-kondo linters:

- `unused-namespace` -- remove unused require entries
- `unused-binding` -- prefix unused bindings with `_`
- `unused-import` -- remove unused :import entries
- `unused-referred-var` -- remove unused :refer entries
- `refer-all` -- replace `:refer :all` with bare require
- `missing-else-branch` -- convert `(if ...)` to `(when ...)` when no else
- `misplaced-docstring` -- move docstring before parameter vector
- `unused-private-var` -- prefix `defn-` name with `_`
- `unused-value` -- remove unused value expressions
- `redundant-do` -- remove redundant `do` wrappers
- `redundant-let` -- inline single-binding `let` forms
- `unused-value` (<- check correct name)

## Projects

```clojure
;; deps.edn
{clj-kondo-fix/clj-kondo-fix {:local/root "/path/to/clj-kondo-fix"}}
```

## Development

```bash
# run tests
bb test

# run against a directory
clojure -M:run --lint src/ --fix
```
