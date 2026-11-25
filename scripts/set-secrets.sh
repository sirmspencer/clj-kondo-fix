#!/usr/bin/env bash
# stores the release workflow's secrets in the github repository from
# environment variables. values are piped to gh on stdin, so they never appear
# in the process list or in the output. variables that are not set are skipped.
#
# usage:
#   TAP_TOKEN=... scripts/set-secrets.sh [owner/repo]
#
# run it from a shell where the variables are already exported, for example
# after `source` of a private file outside the repository, rather than typing
# the values on the command line, where they would land in shell history.
set -euo pipefail

repo="${1:-sirmspencer/clj-kondo-fix}"
secrets="TAP_TOKEN"

if ! gh auth status >/dev/null 2>&1; then
  echo "gh is not signed in; run gh auth login first" >&2
  exit 1
fi

for name in $secrets; do
  if [ -z "${!name:-}" ]; then
    echo "skipped $name (not set)"
    continue
  fi
  printf '%s' "${!name}" | gh secret set "$name" --repo "$repo"
  echo "set $name"
done
