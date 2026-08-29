#!/usr/bin/env bash

set -euo pipefail

tag="${1:?Usage: create-notes.sh <tag>}"
repository_root="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$repository_root"

python - "$tag" <<'PY'
import os
import re
import subprocess
import sys
from pathlib import Path

tag = sys.argv[1]
changelog = Path("CHANGELOG.md").read_text()
match = re.search(
    r"^## \[Unreleased\]\s*$\n(.*?)(?=^## |\Z)",
    changelog,
    flags=re.MULTILINE | re.DOTALL,
)
if match is None:
    raise SystemExit("CHANGELOG.md does not contain an Unreleased section")

notes = match.group(1).strip()
if not notes:
    raise SystemExit("The Unreleased section in CHANGELOG.md is empty")

previous_tags = subprocess.run(
    [
        "git",
        "tag",
        "--merged",
        "HEAD",
        "--list",
        "v*",
        "--sort=-version:refname",
    ],
    check=True,
    capture_output=True,
    text=True,
).stdout.splitlines()

if previous_tags:
    previous_tag = previous_tags[0]
    repository = os.environ["GITHUB_REPOSITORY"]
    server_url = os.environ["GITHUB_SERVER_URL"]
    notes += (
        f"\n\n**Full Changelog:** "
        f"[`{previous_tag}...{tag}`]"
        f"({server_url}/{repository}/compare/{previous_tag}...{tag})"
    )

Path("release-notes.md").write_text(notes + "\n")
PY
