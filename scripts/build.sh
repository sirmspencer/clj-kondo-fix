#!/usr/bin/env bash
# builds the native binary for the current runner and packages it into dist/.
# native-image cannot cross-compile, so each target is built on its own runner.
# usage: scripts/build.sh <version> <os-arch> [static]
set -euo pipefail

version="$1"
target="$2"
static="${3:-}"
os="${target%-*}"

export CLJ_KONDO_FIX_VERSION="$version"
if [ "$os" = windows ]; then
  # setup-clojure installs the clojure cli on windows as a powershell module
  pwsh -NoProfile -Command "clojure -T:build native"
  name="clj-kondo-fix.exe"
elif [ "$static" = static ]; then
  clojure -T:build native :static true
  name="clj-kondo-fix"
else
  clojure -T:build native
  name="clj-kondo-fix"
fi
"./target/$name" --version

work="dist/$target"
mkdir -p "$work"
cp "target/$name" LICENSE README.md "$work/"
if [ "$os" = windows ]; then
  (cd "$work" && 7z a -tzip "../clj-kondo-fix-$target.zip" . >/dev/null)
else
  tar -czf "dist/clj-kondo-fix-$target.tar.gz" -C "$work" .
fi
rm -rf "$work"
echo "built dist/clj-kondo-fix-$target"
