---
name: add-rule
description: How to add a new clj-kondo rule or edge-case fixture to clj-kondo-fix
---

## File Map

```
src/clj_kondo_fix/impl/
  rules.clj            — single source of truth: rule-definitions (40 implemented rules),
                         rule-metadata (all 82 rules, status + display name),
                         stub-definitions (82 stubs for not-implemented / not-applicable rules),
                         findings-matching-rule
  core.clj             — pipeline (lint → findings → fix → write)
  utils.clj            — shared low-level helpers (I/O, bracket, token)
  driver.clj           — reduce-findings driver + ->display-path
  require_entry.clj    — ns/require entry removal helpers (require-family rules)
  fixes.clj            — aggregator: re-exports every fix-*-in-file fn (do not add logic here)
  fixes/               — one file per implemented rule (40 files, named <rule_underscored>.clj)

test/clj_kondo_fix/
  test_support.clj     — shared test helpers (fixture-path, assert-fix, assert-skip, assert-no-finding, …)
  integration_test.clj — full-pipeline dry-run and fix tests
  utils_test.clj       — utils unit tests
  rules/
    <rule_underscored>_test.clj  — one test file per rule
    <rule_underscored>/          — fixture files colocated as a sibling directory
      <slug>-in.clj   — input: real Clojure file with the kondo finding
      <slug>-out.clj  — expected output after fix (generated or hand-written)
      <slug>.clj      — single file for no-change tests (skip / no-finding)

rules.md               — generated index; regenerate with `clojure -M:gen-rules`
README.md              — ## Rules section is generated; do not hand-edit that section
local/
  gen_rules_md.clj     — tracked; generator that writes rules.md and README.md ## Rules
  gen_fixtures.clj     — untracked; generates -out.clj fixture files from -in.clj inputs
```

---

## Shared Helpers Reference

### `utils.clj`

Three sections (all public):

**I/O**
- `read-lines` / `write-lines!` — file ↔ line-vec conversion; used by the pipeline in `core.clj`

**Bracket navigation**
- `find-matching-bracket [s start-idx]` — single-line `[]` match
- `find-matching-bracket-across-lines [lines line col]` — multi-line, handles `[` `(` `{`, string-aware; returns `[line col]` or nil
- `find-opening-bracket [lines line-idx col-idx]` — scan left for the `[` containing the position; returns `{:line :col}` or nil
- `enclosing-bracket-type [lines line-idx col-idx]` — returns the character (`[`, `(`, `{`) of the innermost enclosing bracket

**Token helpers**
- `word-end-pos [line col-idx]` — end index (exclusive) of the identifier at col-idx
- `find-binding-on-line [line binding-name approx-col]` — whole-word search; returns start index or nil
- `find-docstring-end [lines start-line-idx]` — returns the line index of the docstring's closing `"`
- `remove-token-span [line start end]` — remove `[start, end)` from line, fixing surrounding whitespace
- `remove-referred-var-from-line [line var-name col-idx]` — remove a var token at col-idx; handles namespaced tokens

### `driver.clj`

- `->display-path [file-path]` — rewrites absolute path to `~/…` for log messages
- `reduce-findings [lines findings per-finding-fn]` / `[... post-fn]` — the loop driver (see below)

### `require_entry.clj`

Used only by require-family rules (unused-namespace, duplicate-require, unused-import, unused-referred-var):

- `remove-require-entry [lines line-idx col-start ns-name log file-url finding-line-num]` — core entry removal; returns `[new-lines changed?]`
- `remove-require-finding [lines finding file-url log]` — thin adapter over `remove-require-entry`
- `cleanup-empty-clauses [lines]` — post-pass removing `(:require )`, `(:import )`, etc.
- `remove-bare-requires [lines ns-names file-url log]` — remove bare `[ns-name]` entries with no `:as`/`:refer`

---

## Adding an Implemented Rule

### 1. Create `src/clj_kondo_fix/impl/fixes/<rule-keyword>.clj`

File name matches the clj-kondo rule keyword exactly (e.g. `:unused-namespace` → `unused_namespace.clj`).
Namespace name uses hyphens (e.g. `clj-kondo-fix.impl.fixes.unused-namespace`).

```clojure
(ns clj-kondo-fix.impl.fixes.your-rule
  (:require [clojure.string :as str]
            [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            ;; add utils, require-entry etc. as needed
            ))

(defn fix-your-rule-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [current-lines f]
        ;; per-finding logic
        ;; return [current-lines nil]  — no change
        ;; return [new-lines true]     — changed
        )
      ;; optional post-fn, e.g. cleanup-empty-clauses
      )))
```

**`reduce-findings` signature:**
- `per-finding-fn` : `[current-lines finding] → [new-lines changed?]`
- `post-fn` : `[lines] → lines` (optional)
- Returns `{:fixed N :lines [...] :changed? bool}`

Findings are automatically sorted in reverse line order and deduplicated — do not sort manually.

For rules that remove `:require` or `:import` entries:

```clojure
(ns clj-kondo-fix.impl.fixes.your-require-rule
  (:require [clj-kondo-fix.impl.driver :refer [->display-path reduce-findings]]
            [clj-kondo-fix.impl.require-entry :refer [remove-require-finding
                                                       cleanup-empty-clauses]]))

(defn fix-your-require-rule-in-file [file-path lines findings log]
  (let [fu (->display-path file-path)]
    (reduce-findings lines findings
      (fn [ls f] (remove-require-finding ls f fu log))
      cleanup-empty-clauses)))
```

### 2. Re-export from `fixes.clj`

Add one line to the `ns` `:require` block and one `def` at the bottom of `fixes.clj`:

```clojure
;; In the :require block:
[clj-kondo-fix.impl.fixes.your-rule :as your-rule]

;; After the existing defs:
(def fix-your-rule-in-file your-rule/fix-your-rule-in-file)
```

### 3. Register in `rules.clj`

Two places to update:

**a. Add to `rule-definitions`:**

```clojure
:your-rule
{:message-re #"^Exact message pattern from kondo$"
 :phase       :default          ; or :require / :binding / :import / :refer
 :fix-fn      fixes/fix-your-rule-in-file
 :display     "human readable name"}
```

Verify `:message-re` against real kondo output — the pattern must match the full
`:message` string from the finding.

**b. Add to `rule-metadata`:**

```clojure
:your-rule {:status :implemented :display "human readable name"}
```

**c. Remove from `stub-definitions`** if the rule was previously stubbed there.

### 4. Create fixture directory and input files

Fixture files live colocated with the rule's test file:

```
test/clj_kondo_fix/rules/<rule_underscored>/
```

where `<rule_underscored>` is the rule keyword with hyphens replaced by underscores
(e.g. `:unused-namespace` → `unused_namespace`).

Name edge-case files after what they test, not after numbers:

```
removes-single-in.clj
removes-single-out.clj
multiline-form-in.clj
multiline-form-out.clj
no-change-when-used.clj      ← single file, no -in/-out suffix
```

Write `-in.clj` files as normal Clojure — the IDE will show kondo squiggles on them,
which is intentional and useful for visual verification.

### 5. Generate output fixture files

For rules where all findings are fixed (no `filter-fn`):

```bash
clojure -M:gen-fixtures
```

**For pred-based tests** (only a subset of findings fixed), write `-out.clj` by hand.

### 6. Create `test/clj_kondo_fix/rules/<rule_underscored>_test.clj`

One test file per rule, ns name `clj-kondo-fix.rules.<rule-keyword>-test`.
Use `fixture-path` — never embed code strings inline.

```clojure
(ns clj-kondo-fix.rules.your-rule-test
  (:require [clojure.test :refer [deftest is testing]]
            [clj-kondo-fix.impl.fixes :as fixes]
            [clj-kondo-fix.test-support :refer [fixture-path assert-fix assert-no-finding assert-skip]]))

(deftest test-your-rule
  (testing "basic case"
    (let [result (assert-fix fixes/fix-your-rule-in-file
                             (fixture-path "your-rule" "removes-single-in")
                             [:your-rule] 1)]
      (is (= 1 (:fixed result)))
      (is (= (slurp (fixture-path "your-rule" "removes-single-out")) (:content result)))))

  (testing "no change when already correct"
    (let [result (assert-no-finding fixes/fix-your-rule-in-file
                                    (fixture-path "your-rule" "no-change-when-used")
                                    [:your-rule])]
      (is (zero? (:fixed result)))))

  (testing "deliberate skip — unsafe to auto-fix"
    (assert-skip fixes/fix-your-rule-in-file
                 (fixture-path "your-rule" "skip-case")
                 [:your-rule])))
```

Helper reference (all from `test-support`):

| Helper | Use when |
|---|---|
| `assert-fix` | kondo fires, fix is applied, findings clear |
| `assert-skip` | kondo fires, fix deliberately makes no change |
| `assert-no-finding` | kondo does not fire at all (already correct code) |

`assert-fix` accepts an optional 5th arg `filter-fn` to test partial fixes.

### 7. Run the test suite

```bash
clojure -M:test
```

All tests, 0 failures before committing.

### 8. Regenerate `rules.md` and `README.md`

```bash
clojure -M:gen-rules
```

This writes both `rules.md` and the `## Rules` section of `README.md` in one pass.

Commit `rules.md`, `README.md`, and all fixture files together with the implementation.

---

## Registering a Stub Rule

Use this path when a rule exists in clj-kondo but is not yet implemented (or is not
applicable) in clj-kondo-fix. This keeps `rules.md` and `README.md` complete and
eliminates the fallback icon.

### 1. Add to `rule-metadata` in `rules.clj`

```clojure
;; Not yet implemented:
:your-rule {:status :not-implemented :display "human readable name"}

;; Not applicable (kondo fires but no auto-fix makes sense):
:your-rule {:status :not-applicable :display "human readable name"}
```

### 2. Add to `stub-definitions` in `rules.clj`

```clojure
:your-rule
{:message-re #"^Exact message pattern from kondo$"
 :phase       :default
 :fix-fn      stub-fix-fn
 :display     "human readable name"}
```

`:fix-fn` must be `stub-fix-fn` (defined just above `stub-definitions` in `rules.clj`).

### 3. Regenerate `rules.md` and `README.md`

```bash
clojure -M:gen-rules
```

No tests required for stubs — `stub-fix-fn` is a no-op.

---

## Adding an Edge Case to an Existing Rule

1. Write the new `<slug>-in.clj` file in the rule's fixture directory (`test/clj_kondo_fix/rules/<rule_underscored>/`).
2. Run `clojure -M:gen-fixtures` (or write `-out.clj` by hand for partial-fix cases).
3. Add a `testing` block to the existing `deftest` in `test/clj_kondo_fix/rules/<rule_underscored>_test.clj`.
4. Run tests: `clojure -M:test`.

---

## Key Invariants

- **`rules.clj` is the single source of truth.** All rule status and metadata lives in `rule-metadata` inside `rules.clj`. Do not maintain a separate EDN file.

- **Fix functions are pure text transforms.** No kondo calls, no disk I/O. All I/O lives in `core.clj` and the test helpers.

- **`fixes.clj` is a dumb aggregator.** Never add logic there — put it in the rule file.

- **Findings are 1-indexed.** `:line` and `:col` in findings are 1-indexed; convert with `(dec (:line f))`.

- **`reduce-findings` handles sort and dedup automatically.** Do not sort manually in the rule file.

- **No trailing newlines in `lines`.** `read-lines` strips them. Join with `"\n"` and append one `"\n"` on write.

- **Fixture `-in.clj` files intentionally have kondo findings.** Squiggles are expected.

- **`local/gen_rules_md.clj` is tracked.** Commit it when the generator changes. `local/gen_fixtures.clj` remains untracked.

- **Run `cljfmt` after editing any source file:**
  ```bash
  cljfmt fix src/clj_kondo_fix/impl/fixes/your_rule.clj
  ```
