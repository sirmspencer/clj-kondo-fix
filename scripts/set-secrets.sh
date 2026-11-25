#!/usr/bin/env bash
# stores every NAME=value line of a file as a github repository secret, from
# .envrc at the repository root by default. JETBRAINS_PUBLISH is stored as a
# repository variable instead, since it is a setting rather than a secret.
# values are piped to gh on stdin, so they never appear in the process list or
# the output.
#
# usage: scripts/set-secrets.sh [file] [owner/repo]
set -euo pipefail

root="$(cd "$(dirname "$0")/.." && pwd)"
file="${1:-$root/.envrc}"
repo="${2:-sirmspencer/clj-kondo-fix}"

if [ ! -f "$file" ]; then
  echo "no secrets file at $file" >&2
  exit 1
fi
if ! gh auth status >/dev/null 2>&1; then
  echo "gh is not signed in; run gh auth login first" >&2
  exit 1
fi

# the names assigned in the file, with or without export
names="$(sed -nE 's/^[[:space:]]*(export[[:space:]]+)?([A-Za-z_][A-Za-z0-9_]*)=.*/\2/p' "$file" | sort -u)"
if [ -z "$names" ]; then
  echo "no NAME=value lines in $file" >&2
  exit 1
fi

# load the values from the file alone, ignoring anything already in the environment
# shellcheck disable=SC2086
unset $names
set -a
# shellcheck source=/dev/null
source "$file"
set +a

count=0
for name in $names; do
  value="${!name:-}"
  if [ -z "$value" ]; then
    echo "skipped $name: empty"
    continue
  fi
  if [ "$name" = "JETBRAINS_PUBLISH" ]; then
    gh variable set "$name" --repo "$repo" --body "$value"
    echo "set variable $name=$value"
    continue
  fi
  printf '%s' "$value" | gh secret set "$name" --repo "$repo"
  echo "set secret $name"
  count=$((count + 1))
done

echo "set $count secrets on $repo"
gh secret list --repo "$repo"
