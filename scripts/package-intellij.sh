#!/usr/bin/env bash
# builds the intellij plugin with every platform's clj-kondo-fix binary bundled,
# from the release archives in a dist folder. the plugin zip is written to the dist folder.
# usage: scripts/package-intellij.sh <version> <dist-dir>
set -euo pipefail

version="${1#v}"
dist="$(cd "$2" && pwd)"
plugin="$(cd "$(dirname "$0")/../editors/intellij" && pwd)"

rm -rf "$plugin/bin"
for target in darwin-arm64 darwin-amd64 linux-amd64 linux-arm64 windows-amd64; do
  mkdir -p "$plugin/bin/$target"
  if [ -f "$dist/clj-kondo-fix-$target.zip" ]; then
    unzip -q -o "$dist/clj-kondo-fix-$target.zip" clj-kondo-fix.exe -d "$plugin/bin/$target"
  elif [ -f "$dist/clj-kondo-fix-$target.tar.gz" ]; then
    tar -xzf "$dist/clj-kondo-fix-$target.tar.gz" -C "$plugin/bin/$target" ./clj-kondo-fix
    chmod +x "$plugin/bin/$target/clj-kondo-fix"
  else
    echo "skipped $target: no archive" >&2
    rmdir "$plugin/bin/$target"
  fi
done
cd "$plugin"
./gradlew buildPlugin -PpluginVersion="$version" --console=plain -q
cp build/distributions/clj-kondo-fix-intellij-"$version".zip "$dist/"
echo "built $dist/clj-kondo-fix-intellij-$version.zip"
