#!/usr/bin/env bash

set -euo pipefail

target="${1:?Usage: stage-artifact.sh <target>}"
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repository_root"

artifact_directory="packages/$target/build/libs"
shopt -s nullglob
candidates=("$artifact_directory"/*.jar)
jars=()

for candidate in "${candidates[@]}"; do
  case "$candidate" in
    *-sources.jar | *-dev.jar) ;;
    *) jars+=("$candidate") ;;
  esac
done

if [[ ${#jars[@]} -ne 1 ]]; then
  echo "Expected one release JAR for $target, found ${#jars[@]}" >&2
  printf '%s\n' "${jars[@]}" >&2
  exit 1
fi

mkdir -p release-artifacts
filename="$(basename "${jars[0]}" .jar)"
cp "${jars[0]}" "release-artifacts/$filename-$target.jar"
