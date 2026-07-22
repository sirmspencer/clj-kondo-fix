# Work Order: Remaining Rules

*Generated with LLM assistance.*

## Process (read before starting)

1. **One rule at a time.** Pick the next item from the list below.
2. **Load the skill first.** Before writing any code, load the `add-rule` skill; it is the authoritative implementation guide.
3. **Plan before coding.** Use the todo list to plan the rule's sub-steps; work through them one at a time.
4. **Update `rules.clj`.** `rule-metadata` in `src/clj_kondo_fix/impl/rules.clj` is the single source of truth for status. Implementing a rule means flipping its status from `:not-implemented` to `:implemented` and moving it out of `stub-definitions`.
5. **Verify.** Run `clojure -M:test` (0 failures), run `cljfmt` on all edited source files, regenerate docs with `clojure -M:gen-rules`.
6. **Remove the rule in the same commit.** The commit that ships the fix must also delete that rule's entry from this file. Do not batch removals. Do not renumber remaining items.
7. **Pause here.** Stop and wait for review before starting the next item.
8. **Commit once approved** make a commit for each rule.
9. **Repeat** go back to step 1 and keep going for the next rule.

## Remaining Rules

14. `:underscore-in-namespace`
15. `:unexpected-recur`
16. `:unknown-ns-option`
17. `:unknown-require-option`
18. `:unquote-not-syntax-quoted`
19. `:unused-value`
20. `:var-same-name-except-case`
