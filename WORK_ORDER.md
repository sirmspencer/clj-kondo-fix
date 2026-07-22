# Work Order: Remaining Rules

*Generated with LLM assistance.*

## Process (read before starting)

1. **One rule at a time.** Pick the next item from the list below.
2. **Load the skill first.** Before writing any code, load the `add-rule` skill; it is the authoritative implementation guide.
3. **Plan before coding.** Use the todo list to plan the rule's sub-steps; work through them one at a time.
4. **Update `rules.clj`.** `rule-metadata` in `src/clj_kondo_fix/impl/rules.clj` is the single source of truth for status. Implementing a rule means flipping its status from `:not-implemented` to `:implemented` and moving it out of `stub-definitions`.
5. **Verify.** Run `clojure -M:test` (0 failures), run `cljfmt` on all edited source files, regenerate docs with `clojure -M:gen-rules`.
6. **Remove the rule in the same commit.** The commit that ships the fix must also delete that rule's entry from this file. Do not batch removals. Do not renumber remaining items.
7. **Pause after each rule.** Stop and wait for review before starting the next item.
8. **Commit once approved** make a commit for each rule.
9. **Repeat** go back to step 1 for the next rule.

## Remaining Rules

7. `:docstring-no-summary`
8. `:duplicate-field-name`
9. `:duplicate-key-args`
10. `:duplicate-map-key`
11. `:format`
12. `:inline-def`
13. `:line-length`
14. `:missing-body-in-when`
15. `:missing-clause-in-try`
16. `:missing-map-value`
17. `:private-call`
18. `:refer`
19. `:shadowed-fn-param`
20. `:shadowed-var`
21. `:unbound-destructuring-default`
22. `:underscore-in-namespace`
23. `:unexpected-recur`
24. `:unknown-ns-option`
25. `:unknown-require-option`
26. `:unquote-not-syntax-quoted`
27. `:unused-value`
28. `:var-same-name-except-case`
