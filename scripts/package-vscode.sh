#!/usr/bin/env bash
# builds one vs code extension package per platform, each with that platform's
# clj-kondo-fix binary bundled, from the release archives in a dist folder.
# usage: scripts/package-vscode.sh <version> <dist-dir>
set -euo pipefail

version="${1#v}"
dist="$(cd "$2" && pwd)"
extension="$(cd "$(dirname "$0")/../editors/vscode" && pwd)"

# release archive name -> vs code target
targets="darwin-arm64:darwin-arm64 darwin-amd64:darwin-x64 linux-amd64:linux-x64 linux-arm64:linux-arm64 windows-amd64:win32-x64"

cd "$extension"
npm pkg set version="$version"
npm run build
for pair in $targets; do
  archive="${pair%%:*}"
  target="${pair##*:}"
  rm -rf bin && mkdir bin
  if [ -f "$dist/clj-kondo-fix-$archive.zip" ]; then
    unzip -q -o "$dist/clj-kondo-fix-$archive.zip" clj-kondo-fix.exe -d bin
  elif [ -f "$dist/clj-kondo-fix-$archive.tar.gz" ]; then
    tar -xzf "$dist/clj-kondo-fix-$archive.tar.gz" -C bin ./clj-kondo-fix
    chmod +x bin/clj-kondo-fix
  else
    echo "skipping $target: no archive for $archive"
    continue
  fi
  npx vsce package --target "$target" --out "$dist/clj-kondo-fix-$version-$target.vsix"
done
rm -rf bin
