---
name: add-rule
description: How to add a new clj-kondo rule or edge-case fixture to clj-kondo-fix
---

## File Map

```
src/clj_kondo_fix/impl/
  rules.clj          — rule registry (keyword → fix-fn + message-re)
  fixes.clj          — all fix functions
  core.clj           — pipeline (lint → findings → fix → write)
  utils.clj          — shared helpers (read-lines, find-matching-bracket-across-lines, etc.)

test/clj_kondo_fix/
  core_test.clj      — all tests; uses fixture files; never embeds code strings
  fixtures/
    <rule-name>/     — one directory per rule, named after the clj-kondo keyword
      <slug>-in.clj  — input: real Clojure file with the kondo finding
      <slug>-out.clj — expected output after fix (generated or hand-written)
      <slug>.clj     — single file for no-change tests (skip / no-finding)

resources/
  rule-notes.edn     — status + reason for rules not in rules.clj; drives rules.md

rules.md             — generated index; regenerate with `clojure -M:gen-rules`
local/               — untracked scratch scripts (gen_fixtures.clj, gen_rules_md.clj)
```

---

## Adding a New Rule

### 1. Write the fix function in `fixes.clj`

Convention: `fix-<rule-keyword>-in-file`

Signature:

```clojure
(defn fix-<rule>-in-file [file-path lines findings log]
  ...)
```

- `file-path` — absolute path string; used only for log messages, never for I/O
- `lines` — `vec` of strings, one per line, no trailing newlines
- `findings` — seq of `{:line :col :message}` maps (1-indexed)
- `log` — `atom [string]`; `(swap! log conj "  ~/path:N  description")`
- Return: `{:fixed N :lines [...] :changed? bool}`

Process findings in **reverse line order** (sort descending by `:line`) so earlier
edits do not shift the indices of later ones. Use `find-matching-bracket-across-lines`
and `find-opening-bracket` from `utils.clj` for bracket navigation.

### 2. Register the rule in `rules.clj`

Add an entry to `rule-definitions`:

```clojure
:your-rule
{:message-re #"^Exact message pattern from kondo$"
 :phase       :default          ; or :require / :binding / :import / :refer
 :fix-fn      fixes/fix-your-rule-in-file
 :display     "human readable name"}
```

Verify `:message-re` against real kondo output — the pattern must match the full
message string from `(:message finding)`.

### 3. Create fixture directories and input files

```
test/clj_kondo_fix/fixtures/<rule-name>/
```

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

### 4. Generate output fixture files

For rules where all findings are fixed (no `filter-fn`):

```bash
clojure -M:gen-fixtures
```

This copies each `-in.clj` to `-out.clj` and runs the fix in-place.

**For pred-based tests** (where only a subset of findings is fixed), the generated
`-out.clj` will be wrong — it applies all findings. Write the correct output by hand:

```clojure
;; Test uses pred: #(str/ends-with? (:message %) "List")
;; gen-fixtures removes ALL imports; hand-write the file removing only List.
```

### 5. Write tests in `core_test.clj`

Add a `deftest` block. Use `fixture-path` — never embed code strings inline.

```clojure
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
                 (fixture-path "your-rule" "skip-namespaced-key")
                 [:your-rule])))
```

Helper reference:

| Helper | Use when |
|---|---|
| `assert-fix` | kondo fires, fix is applied, findings clear |
| `assert-skip` | kondo fires, fix deliberately makes no change |
| `assert-no-finding` | kondo does not fire at all (already correct code) |

`assert-fix` accepts an optional 5th arg `filter-fn` to test partial fixes:

```clojure
(let [pred   #(str/includes? (:message %) "specific-var")
      result (assert-fix fix-fn path [:rule] 1 pred)]
  ...)
```

### 6. Run the test suite

```bash
clojure -M:test -m clj-kondo-fix.core-test
```

All 14+ tests, 0 failures before committing.

### 7. Update `resources/rule-notes.edn`

Remove the rule's entry (or change its `:status` to `:implemented` if you want
to keep a note). Rules absent from both `rule-definitions` and `rule-notes.edn`
default to `:not-implemented` in the index.

### 8. Regenerate `rules.md`

```bash
clojure -M:gen-rules
```

Commit `rules.md`, `resources/rule-notes.edn`, and all fixture files together
with the implementation.

---

## Adding an Edge Case to an Existing Rule

1. Write the new `<slug>-in.clj` file in the rule's fixture directory.
2. Generate the output:
   - Run `clojure -M:gen-fixtures` for full-fix cases.
   - Write `<slug>-out.clj` by hand for pred-based partial-fix cases.
3. Add a `testing` block to the existing `deftest` in `core_test.clj`.
4. Run tests: `clojure -M:test -m clj-kondo-fix.core-test`.

---

## Key Invariants

- **Fix functions are pure text transforms.** They never call kondo, never read from
  disk, never write to disk. All I/O is handled by the pipeline in `core.clj` and
  the test helpers in `core_test.clj`.

- **`apply-fix` in tests is in-memory.** Fixture files are never modified by the
  test suite. The after-lint check in `assert-fix` writes to a temp file internally
  and deletes it immediately.

- **Findings are 1-indexed.** `:line` and `:col` in findings are 1-indexed;
  convert to 0-indexed with `(dec (:line f))` before indexing into `lines`.

- **Process findings in reverse order.** Sort by line/col descending before looping
  so that removing or replacing content on later lines does not corrupt indices for
  earlier lines in the same pass.

- **No trailing newlines in `lines`.** `read-lines` strips them. The pipeline joins
  with `"\n"` and appends one final `"\n"` on write. `apply-fix` in tests does the
  same: `(str (str/join "\n" (:lines result)) "\n")`.

- **Fixture `-in.clj` files intentionally have kondo findings.** The IDE will show
  squiggles; that is expected. `-out.clj` files should be clean.

- **`local/` is untracked.** Scripts in `local/` (`gen_fixtures.clj`,
  `gen_rules_md.clj`) are not committed. The outputs they produce (`rules.md`,
  fixture `-out.clj` files) are committed.

- **Run `cljfmt` after editing `fixes.clj` or `rules.clj`:**
  ```bash
  cljfmt fix --config clj-kondo-fix/.cljfmt.edn clj-kondo-fix/src/clj_kondo_fix/impl/fixes.clj
  ```
