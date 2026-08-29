#!/usr/bin/env bash

set -euo pipefail

target="${1:?Usage: prepare.sh <target>}"
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repository_root"

version="$(sed -n 's/^mod_version=//p' gradle.properties)"
if [[ -z "$version" ]]; then
  echo "mod_version is not set in gradle.properties" >&2
  exit 1
fi

tag="v$version"
if git rev-parse --quiet --verify "refs/tags/$tag" >/dev/null; then
  echo "Tag $tag already exists" >&2
  exit 1
fi

case "$target" in
  all)
    targets='["fabric-1.21.11"]'
    ;;
  fabric-1.21.11)
    targets="[\"$target\"]"
    ;;
  *)
    echo "Unsupported release target: $target" >&2
    exit 1
    ;;
esac

printf 'tag=%s\n' "$tag"
printf 'targets=%s\n' "$targets"
