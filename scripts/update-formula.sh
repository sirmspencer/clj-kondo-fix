#!/usr/bin/env bash
# writes the homebrew formula for a release, using its prebuilt archives and
# checksums.txt, so brew installs a native binary instead of building from source.
# usage: scripts/update-formula.sh <version> <formula-path>
set -euo pipefail

tag="v${1#v}"
version="${tag#v}"
formula="$2"
repo="sirmspencer/clj-kondo-fix"
checksums="$(gh release download "$tag" --repo "$repo" --pattern checksums.txt --output -)"
url="https://github.com/$repo/releases/download/$tag"

sum() {
  local value
  value="$(echo "$checksums" | awk -v file="clj-kondo-fix-$1.tar.gz" '$2 == file { print $1 }')"
  [ -n "$value" ] || { echo "no checksum for clj-kondo-fix-$1.tar.gz" >&2; exit 1; }
  echo "$value"
}

darwin_arm64="$(sum darwin-arm64)"
darwin_amd64="$(sum darwin-amd64)"
linux_arm64="$(sum linux-arm64)"
linux_amd64="$(sum linux-amd64)"

cat > "$formula" <<RUBY
class CljKondoFix < Formula
  desc "Auto-fix Clojure code based on clj-kondo findings"
  homepage "https://github.com/$repo"
  version "$version"
  license "EPL-2.0"

  on_macos do
    on_arm do
      url "$url/clj-kondo-fix-darwin-arm64.tar.gz"
      sha256 "$darwin_arm64"
    end
    on_intel do
      url "$url/clj-kondo-fix-darwin-amd64.tar.gz"
      sha256 "$darwin_amd64"
    end
  end

  on_linux do
    on_arm do
      url "$url/clj-kondo-fix-linux-arm64.tar.gz"
      sha256 "$linux_arm64"
    end
    on_intel do
      url "$url/clj-kondo-fix-linux-amd64.tar.gz"
      sha256 "$linux_amd64"
    end
  end

  def install
    bin.install "clj-kondo-fix"
  end

  test do
    assert_match "clj-kondo-fix #{version}", shell_output("#{bin}/clj-kondo-fix --version")
    (testpath/"example.clj").write "(ns example (:require [clojure.string :as s]))\\n"
    # exits 1 when it fixes something
    shell_output("#{bin}/clj-kondo-fix --lint #{testpath}/example.clj --fix", 1)
    refute_match "clojure.string", (testpath/"example.clj").read
  end
end
RUBY
echo "wrote $formula"
