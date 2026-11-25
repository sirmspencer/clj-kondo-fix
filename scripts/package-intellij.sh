#!/usr/bin/env bash
# builds the intellij plugin into a dist folder. the plugin bundles no binaries;
# it downloads the release binary that matches its version on first use.
# usage: scripts/package-intellij.sh <version> <dist-dir>
set -euo pipefail

version="${1#v}"
mkdir -p "$2"
dist="$(cd "$2" && pwd)"
plugin="$(cd "$(dirname "$0")/../editors/intellij" && pwd)"

cd "$plugin"
./gradlew buildPlugin -PpluginVersion="$version" --console=plain -q
cp build/distributions/clj-kondo-fix-intellij-"$version".zip "$dist/"
echo "built $dist/clj-kondo-fix-intellij-$version.zip"
