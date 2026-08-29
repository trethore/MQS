#!/usr/bin/env bash

set -euo pipefail

tag="${1:?Usage: publish.sh <tag> <draft>}"
draft="${2:?Usage: publish.sh <tag> <draft>}"
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repository_root"

if [[ "$draft" != "true" && "$draft" != "false" ]]; then
  echo "Draft must be true or false" >&2
  exit 1
fi

shopt -s nullglob
artifacts=(release-artifacts/*)
if [[ ${#artifacts[@]} -eq 0 ]]; then
  echo "No release artifacts found" >&2
  exit 1
fi

arguments=(
  release create "$tag"
  "${artifacts[@]}"
  --target "$GITHUB_SHA"
  --title "$tag"
  --notes-file release-notes.md
)

if [[ "$draft" == "true" ]]; then
  arguments+=(--draft)
fi

gh "${arguments[@]}"
