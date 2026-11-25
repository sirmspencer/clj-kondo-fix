# clj-kondo-fix for VS Code

Fixes Clojure code with [clj-kondo-fix](https://github.com/sirmspencer/clj-kondo-fix), which runs the [clj-kondo](https://github.com/clj-kondo/clj-kondo) linter and automatically corrects the findings it knows how to fix, such as unused requires, redundant forms, and unsorted namespaces. The clj-kondo-fix executable is bundled with the extension; nothing else needs to be installed.

## Usage

Run **clj-kondo-fix: Fix File** from the command palette or the editor's context menu on a `.clj`, `.cljs`, or `.cljc` file. The fixes are applied to the open document, including unsaved changes, as one undoable edit.

To fix files whenever they are saved:

```json
"clj-kondo-fix.fixOnSave": true
```

clj-kondo-fix runs from the file's folder, so the project's `.clj-kondo` configuration applies. The extension does not register a formatter, so formatting stays with Calva or any other Clojure formatter. The [rules](https://github.com/sirmspencer/clj-kondo-fix/blob/master/rules.md) list every finding it fixes.

## Settings

- `clj-kondo-fix.fixOnSave`: fix Clojure files before they are saved. Off by default.
- `clj-kondo-fix.path`: path to a clj-kondo-fix executable to use instead of the bundled one, for example a development build.
