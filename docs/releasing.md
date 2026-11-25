*Document generated with LLM assistance*

# Release setup

This document describes the token and one-time steps behind the clj-kondo-fix release workflow, and what happens when a version is released. After the setup is complete, pushing a version tag publishes every package.

## Channels

| Channel | Package | Published by |
|---|---|---|
| GitHub releases | `clj-kondo-fix-<os>-<arch>` archives, the standalone uberjar, and `checksums.txt` | the workflow, with the built-in token |
| Homebrew | `sirmspencer/tap/clj-kondo-fix`, a formula that installs the prebuilt native binary | the workflow, with `TAP_TOKEN` |

The platforms are macOS on Apple silicon and Intel, and Linux on x64 and ARM. The binaries are GraalVM native images. `native-image` cannot cross-compile, so each platform is built and tested on its own runner. The Linux x64 binary is linked statically against musl, so it runs on any distribution.

## Tokens

The token is stored as a repository secret with the name below, under the repository's Settings, Secrets and variables, Actions.

| Secret | Where to create it | Settings |
|---|---|---|
| `TAP_TOKEN` | github.com/settings/personal-access-tokens/new, signed in as `sirmspencer` | resource owner `sirmspencer`; only the `homebrew-tap` repository; Contents: Read and write |

The token expires. When it does, the homebrew job fails, and a new token replaces the secret.

### Setting the secret with a script

`scripts/set-secrets.sh` stores every secret whose environment variable is set and skips the rest. It pipes each value to `gh` on stdin, so values do not appear in the process list or the output.

```sh
# a private file outside the repository, for example ~/.clj-kondo-fix-release.env:
#   export TAP_TOKEN=...
source ~/.clj-kondo-fix-release.env
scripts/set-secrets.sh
```

`gh` must be signed in as an account with admin access to the repository.

## One-time steps

1. **Homebrew users** run `brew trust sirmspencer/tap` once, as recent Homebrew versions require for third-party taps.

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

Running the workflow by hand from the Actions tab runs only the build job, which tests every platform without publishing anything.

## Scripts

| Script | Purpose |
|---|---|
| `scripts/build.sh <version> <os-arch> [static]` | builds the native binary for the current runner and packages it into `dist/` |
| `scripts/update-formula.sh <version> <formula>` | writes the Homebrew formula from a release's checksums |
| `scripts/set-secrets.sh [owner/repo]` | stores the release secrets from environment variables |

## Known issues

- **GitHub retires runner images.** The `macos-14` image was retired in November 2026. The Intel macOS build uses `macos-15-intel`, which GitHub plans to support until August 2027.
- **The CLI exits 1 when it fixes something.** The smoke test and the formula test treat exit code 1 as success after a fix.
