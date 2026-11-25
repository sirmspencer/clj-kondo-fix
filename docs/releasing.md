*Document generated with LLM assistance*

# Release setup

This document describes the tokens and one-time steps behind the clj-kondo-fix release workflow, and what happens when a version is released. After the setup is complete, pushing a version tag publishes every package.

## Channels

| Channel | Package | Published by |
|---|---|---|
| GitHub releases | `clj-kondo-fix-<os>-<arch>` archives, the standalone uberjar, and `checksums.txt` | the workflow, with the built-in token |
| Homebrew | `sirmspencer/tap/clj-kondo-fix`, a formula that installs the prebuilt native binary | the workflow, with `TAP_TOKEN` |
| JetBrains Marketplace | the clj-kondo-fix plugin, one zip with every platform's binary | by hand for the first version, then the workflow, with `JETBRAINS_MARKETPLACE_TOKEN` |

The platforms are macOS on Apple silicon and Intel, and Linux on x64 and ARM. The binaries are GraalVM native images. `native-image` cannot cross-compile, so each platform is built and tested on its own runner. The Linux x64 binary is linked statically against musl, so it runs on any distribution.

## Tokens

Each token is stored as a repository secret with the name below, under the repository's Settings, Secrets and variables, Actions.

| Secret | Where to create it | Settings |
|---|---|---|
| `TAP_TOKEN` | github.com/settings/personal-access-tokens/new, signed in as `sirmspencer` | resource owner `sirmspencer`; only the `homebrew-tap` repository; Contents: Read and write |
| `JETBRAINS_MARKETPLACE_TOKEN` | plugins.jetbrains.com, profile, My Tokens | |

The tokens expire. When one does, the job that uses it fails, and a new token replaces the secret.

### Setting the secrets with a script

`scripts/set-secrets.sh` stores every secret whose environment variable is set and skips the rest. It pipes each value to `gh` on stdin, so values do not appear in the process list or the output.

```sh
# a private file outside the repository, for example ~/.clj-kondo-fix-release.env:
#   export TAP_TOKEN=...
#   export JETBRAINS_MARKETPLACE_TOKEN=...
source ~/.clj-kondo-fix-release.env
scripts/set-secrets.sh
```

The script also sets the repository variable `JETBRAINS_PUBLISH` when that environment variable is set. `gh` must be signed in as an account with admin access to the repository.

## One-time steps

1. **JetBrains Marketplace.** The first version of the plugin must be uploaded by hand at plugins.jetbrains.com/plugin/add, using the `clj-kondo-fix-intellij-<version>.zip` attached to a GitHub release. The form asks for the license (EPL-2.0) and the source code link. JetBrains reviews every new plugin before it becomes public.
2. **Automatic JetBrains publishing.** After the first version is approved, the repository variable `JETBRAINS_PUBLISH` is set to `true`, under Settings, Secrets and variables, Actions, Variables. Until then, the workflow builds the plugin and attaches it to the release without publishing it.
3. **Homebrew users** run `brew trust sirmspencer/tap` once, as recent Homebrew versions require for third-party taps.

## Releasing a version

```sh
git tag v0.2.0
git push origin v0.2.0
```

The tag is the version. The build stamps it into the binary at compile time, so no commit is needed to bump it. Builds from source report the version `dev`.

The `release` workflow then runs these jobs:

1. **build:** runs the tests, builds the native binary, and runs it against the rule fixtures on each platform's runner. The Linux x64 runner also builds the uberjar.
2. **release:** creates the GitHub release with the archives, the uberjar, and `checksums.txt`.
3. **homebrew:** writes the formula from `checksums.txt`, installs and tests it on macOS from a checkout of the tap, and pushes it to `sirmspencer/homebrew-tap` only if the test passes.
4. **jetbrains:** builds the plugin with every binary, attaches it to the release, and publishes it when `JETBRAINS_PUBLISH` is `true`.

Running the workflow by hand from the Actions tab runs only the build job, which tests every platform without publishing anything.

## Scripts

| Script | Purpose |
|---|---|
| `scripts/build.sh <version> <os-arch> [static]` | builds the native binary for the current runner and packages it into `dist/` |
| `scripts/package-intellij.sh <version> <dist>` | builds the IntelliJ plugin with every binary |
| `scripts/update-formula.sh <version> <formula>` | writes the Homebrew formula from a release's checksums |
| `scripts/set-secrets.sh [owner/repo]` | stores the release secrets from environment variables |

## Known issues

- **GitHub retires runner images.** The `macos-14` image was retired in November 2026. The Intel macOS build uses `macos-15-intel`, which GitHub plans to support until August 2027.
- **The CLI exits 1 when it fixes something.** The smoke test and the formula test treat exit code 1 as success after a fix.
- **The plugin is an action, not a formatter.** Registering a formatting service would replace cljfmt for Reformat Code in Cursive and clojure-lsp, so the plugin adds Code | Fix with clj-kondo-fix instead.
