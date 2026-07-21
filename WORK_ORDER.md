# Work Order: Remaining Rules

*Generated with LLM assistance.*

## Process (read before starting)

1. **One rule at a time.** Pick the next unchecked item from the list below.
2. **Load the skill first.** Before writing any code, load the `add-rule` skill
   for the step-by-step implementation guide.
3. **Plan before coding.** Use the todo list to plan the rule's sub-steps; work
   through them one at a time.
4. **Pause after each rule.** When the implementation is complete and tests pass,
   stop and wait for review before moving to the next item.
5. **Remove the rule in the same commit.** The commit that ships the fix must also
   delete the rule's entry from this file. Do not batch removals. Do not
   renumber any items.
6. **Generate the next plan.** After committing, present the plan for the next
   item on the list and wait for approval before implementing.
7. **Next item.** After plan approval, implement. Repeat from step 1.

---

This is the implementation backlog for clj-kondo rules that are currently
`:not-implemented` in `src/clj_kondo_fix/impl/rules.clj`. Items are ordered
easiest first. Difficulty is driven by the shape of the fix transform: token or
entry removal (reusing `require_entry.clj` and `utils.clj` helpers) is cheap,
single-form deterministic rewrites are moderate, and usage-site rewrites that
touch multiple locations or require scope tracking are the most involved.

Of the 48 `:not-implemented` rules, 18 are worth implementing (below). The
remaining 30 are recommended for reclassification to `:not-applicable`; they are
listed in the appendix for accounting but are out of scope for this work order.

Follow the `add-rule` skill for each item: create the fix namespace, re-export
from `fixes.clj`, register in `rule-definitions` and `rule-metadata`, add
fixtures and a test, then regenerate `rules.md` and `README.md`.

---

## Tier 1: Trivial (token or entry removal, existing infra)

These reuse the require-family removal machinery
(`require_entry.clj`: `remove-token-span`, `cleanup-empty-clauses`;
`utils.clj`: `remove-referred-var-from-line`). Little new logic.

### 3. `duplicate-refer`

- **Trigger:** the same var appears twice in a `:refer [..]` vector.
- **Fix:** remove the duplicate token, keep the first occurrence.
- **Reuse:** `remove-referred-var-from-line` / `remove-token-span`.
- **Effort:** XS.

### 4. `unused-excluded-var`

- **Trigger:** a var listed in `:refer-clojure :exclude [..]` is not used.
- **Fix:** remove the token from the exclude vector.
- **Reuse:** token-span removal.
- **Effort:** S.

### 5. `unresolved-excluded-var`

- **Trigger:** a var listed in an `:exclude` vector does not resolve.
- **Fix:** remove the token from the exclude vector.
- **Reuse:** shares machinery with unused-excluded-var; do the pair together.
- **Effort:** S.

---

## Tier 2: Easy (single deterministic form rewrite, no cross-file analysis)

Localized rewrites of one form. No scope or usage analysis needed.

### 6. `single-operand-comparison`

- **Trigger:** a comparison called with one operand always returns true,
  for example `(< x)` or `(= x)`.
- **Fix:** replace the whole form with `true`.
- **Reuse:** mirror `single-logical-operand` almost directly.
- **Effort:** S.

### 7. `java-static-field-call`

- **Trigger:** a static field is called as if it were a function,
  for example `(Math/PI)`.
- **Fix:** strip the wrapping parens so the field is referenced directly,
  for example `Math/PI`.
- **Reuse:** bracket handling similar to `redundant-call`.
- **Effort:** S to M.

### 8. `duplicate-set-key`

- **Trigger:** a set literal contains an identical duplicate element,
  for example `#{:a :a}`.
- **Fix:** remove the duplicate element. Safe because the elements are identical.
- **Reuse:** token-span removal.
- **Effort:** S.

### 9. `def-fn`

- **Trigger:** `(def foo (fn [..] ..))` should be `(defn foo [..] ..)`.
- **Fix:** rewrite the `def` plus `fn` into a `defn`, splicing the arg vector and
  body. Handle docstring, metadata, and multi-arity forms.
- **Reuse:** none directly; localized structural edit.
- **Effort:** M (bounded by the multi-arity and metadata cases).

---

## Tier 3: Medium (usage-site rewrites, multiple locations, or structural move)

These require finding and rewriting several positions in the file, or moving a
form while preserving surrounding structure.

### 10. `aliased-namespace-symbol`

- **Trigger:** a namespace is referenced by its full name where an alias exists.
- **Fix:** replace the fully qualified reference with the alias.
- **Effort:** M (multiple usage sites).

### 11. `aliased-namespace-var-usage`

- **Trigger:** a var is used by its fully qualified namespace where an alias exists.
- **Fix:** rewrite to the alias-qualified form.
- **Effort:** M.

### 12. `aliased-referred-var`

- **Trigger:** a var is used alias-qualified while also referred (or the reverse).
- **Fix:** normalize to one form consistently.
- **Effort:** M.

### 13. `non-arg-vec-return-type-hint`

- **Trigger:** a return type hint sits on the fn name instead of the arg vector.
- **Fix:** move the `^Type` hint onto the arg vector.
- **Effort:** M (structural move, localized).

### 14. `misplaced-async-metadata`

- **Trigger:** async metadata is attached in the wrong position.
- **Fix:** move the metadata to the correct position.
- **Effort:** M.

### 15. `used-underscored-binding`

- **Trigger:** a binding named `_foo` is actually used in its scope.
- **Fix:** rename `_foo` to `foo` at the binding site and every usage in scope.
- **Effort:** M (requires scope tracking).

### 16. `unreachable-code`

- **Trigger:** forms appear after a `throw`, `recur`, or terminal return.
- **Fix:** remove the unreachable forms, delimiting the span carefully.
- **Effort:** M.

### 17. `is-message-not-string`

- **Trigger:** the message argument to `clojure.test/is` is not a string.
- **Fix:** reposition or adjust the message argument.
- **Effort:** M (verify the safe cases before implementing; some may be skip).

### 18. `use`

- **Trigger:** a `:use` clause in the ns form is discouraged.
- **Fix:** rewrite `:use` to `:require ... :refer [..]`.
- **Effort:** M to L (ns-form structural rewrite).

---

## Appendix: Recommended skip (reclassify to `:not-applicable`)

Documented for accounting only. No code changes in this work order. Each needs
human judgment, cannot be mechanically fixed, or is ambiguous about which side to
keep.

- `case-duplicate-test`, `case-symbol-test`: ambiguous which branch is intended.
- `conditional-build-up`: suggests `cond->`; structural, intent-dependent.
- `conflicting-alias`: alias collision needs a rename decision.
- `destructured-or-always-evaluates`, `destructured-or-binding-of-same-map`: intent-dependent.
- `discouraged-tag`: replacement is config-driven, same family as the other `discouraged-*` rules.
- `do-template`: structural, intent-dependent.
- `docstring-no-summary`: requires human authorship.
- `duplicate-field-name`, `duplicate-key-args`, `duplicate-map-key`: ambiguous which value to keep.
- `format`: format-string correctness is not mechanical.
- `inline-def`: lifting a nested `def` out changes structure and scope.
- `line-length`: reflow is a formatter concern (cljfmt).
- `missing-body-in-when`, `missing-clause-in-try`, `missing-map-value`: cannot invent the missing code.
- `private-call`: no safe mechanical rewrite.
- `refer`: discouraged usage, same family as `refer-all` (already not-applicable).
- `shadowed-fn-param`, `shadowed-var`, `var-same-name-except-case`: require a rename decision.
- `unbound-destructuring-default`: intent-dependent.
- `underscore-in-namespace`: renaming the ns is a multi-file operation.
- `unexpected-recur`: not mechanically fixable.
- `unknown-ns-option`, `unknown-require-option`: removing an option can change behavior.
- `unquote-not-syntax-quoted`: not mechanically fixable.
- `unused-value`: removing a discarded expression can drop intended side effects.
