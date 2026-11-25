#!/usr/bin/env bash
# builds the native binary for the current runner and packages it into dist/.
# native-image cannot cross-compile, so each target is built on its own runner.
# usage: scripts/build.sh <version> <os-arch> [static]
set -euo pipefail

version="$1"
target="$2"
static="${3:-}"

build_args=(:version "\"$version\"")
[ "$static" = static ] && build_args+=(:static true)

clojure -T:build native "${build_args[@]}"
./target/clj-kondo-fix --version

work="dist/$target"
mkdir -p "$work"
cp target/clj-kondo-fix LICENSE README.md "$work/"
tar -czf "dist/clj-kondo-fix-$target.tar.gz" -C "$work" .
rm -rf "$work"
echo "built dist/clj-kondo-fix-$target.tar.gz"
