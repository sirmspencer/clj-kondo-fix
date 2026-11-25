*Document generated with LLM assistance*

# Release setup

This document describes the tokens and one-time steps behind the clj-kondo-fix release workflow, and what happens when a version is released. After the setup is complete, pushing a version tag publishes every package.

## Channels

| Channel | Package | Published by |
|---|---|---|
| GitHub releases | `clj-kondo-fix-<os>-<arch>` archives, the standalone uberjar, and `checksums.txt` | the workflow, with the built-in token |
| Homebrew | `sirmspencer/tap/clj-kondo-fix`, a formula that installs the prebuilt native binary | the workflow, with `TAP_TOKEN` |
| Visual Studio Marketplace | `sirmspencer.clj-kondo-fix`, one package per platform | the workflow, with `VSCE_PAT` |
| Open VSX | `sirmspencer.clj-kondo-fix`, one package per platform | the workflow, with `OVSX_PAT` |
| JetBrains Marketplace | the clj-kondo-fix plugin, one zip with every platform's binary | by hand for the first version, then the workflow, with `JETBRAINS_MARKETPLACE_TOKEN` |

The platforms are macOS on Apple silicon and Intel, Linux on x64 and ARM, and Windows on x64. Homebrew covers macOS and Linux; the editor plugins bundle every platform's binary. The binaries are GraalVM native images. `native-image` cannot cross-compile, so each platform is built and tested on its own runner. The Linux x64 binary is linked statically against musl, so it runs on any distribution.

## Tokens

Each token is stored as a repository secret with the name below, under the repository's Settings, Secrets and variables, Actions.

| Secret | Where to create it | Settings |
|---|---|---|
| `TAP_TOKEN` | github.com/settings/personal-access-tokens/new, signed in as `sirmspencer` | resource owner `sirmspencer`; only the `homebrew-tap` repository; Contents: Read and write |
| `VSCE_PAT` | dev.azure.com, User settings, Personal access tokens | organization: All accessible organizations; scope: Marketplace, Manage |
| `OVSX_PAT` | open-vsx.org, Settings, Access Tokens | |
| `JETBRAINS_MARKETPLACE_TOKEN` | plugins.jetbrains.com, profile, My Tokens | |

The tokens expire. When one does, the job that uses it fails, and a new token replaces the secret.

### Setting the secrets with a script

`scripts/set-secrets.sh` stores every `NAME=value` line of a file as a repository secret, reading `.envrc` at the repository root by default. `JETBRAINS_PUBLISH` is stored as a repository variable instead. The script pipes each value to `gh` on stdin, so values do not appear in the process list or the output. `.envrc` is listed in `.gitignore`.

```sh
scripts/set-secrets.sh                 # reads .envrc
scripts/set-secrets.sh ~/release.env   # reads another file
```

`gh` must be signed in as an account with admin access to the repository.

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
4. **vscode:** packages one extension per platform with its binary, attaches them to the release, and publishes them to the Visual Studio Marketplace and Open VSX.
5. **jetbrains:** builds the plugin with every binary, attaches it to the release, and publishes it when `JETBRAINS_PUBLISH` is `true`.

Running the workflow by hand from the Actions tab runs only the build job, which tests every platform without publishing anything.

## Scripts

| Script | Purpose |
|---|---|
| `scripts/build.sh <version> <os-arch> [static]` | builds the native binary for the current runner and packages it into `dist/` |
| `scripts/package-vscode.sh <version> <dist>` | builds one VS Code package per platform |
| `scripts/package-intellij.sh <version> <dist>` | builds the IntelliJ plugin with every binary |
| `scripts/update-formula.sh <version> <formula>` | writes the Homebrew formula from a release's checksums |
| `scripts/set-secrets.sh [owner/repo]` | stores the release secrets from environment variables |

## Known issues

- **GitHub retires runner images.** The `macos-14` image was retired in November 2026. The Intel macOS build uses `macos-15-intel`, which GitHub plans to support until August 2027.
- **The CLI exits 1 when it fixes something.** The smoke test and the formula test treat exit code 1 as success after a fix.
- **The editor plugins do not register formatters.** A formatting service would replace cljfmt in Cursive, clojure-lsp, and Calva, so the plugins add a fix command instead, and the VS Code extension offers fix on save as a setting.
- **The Clojure CLI on Windows runners is a PowerShell module.** The workflow and `scripts/build.sh` run it through `pwsh` on Windows.
- **Windows checkouts convert line endings** unless told otherwise. `.gitattributes` keeps every file at LF, which the rule fixtures depend on.
- **Maven Central rate limits shared runner addresses.** The build job caches `~/.m2` and `~/.gitlibs`; a `429 Too Many Requests` error is fixed by rerunning the job.
