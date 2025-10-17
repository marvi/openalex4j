#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<USAGE
Usage: $(basename "$0") <release-version> [next-snapshot]

Examples:
  $(basename "$0") 1.0.0
  $(basename "$0") 1.0.0 1.1.0-SNAPSHOT

The script expects to run from the repository root with a clean Git worktree.
It updates pom.xml, commits release and snapshot bumps, and creates an annotated tag.
After it finishes, push changes and tags: git push origin main --follow-tags
USAGE
}

if [[ $# -lt 1 || $# -gt 2 ]]; then
  usage
  exit 1
fi

if [[ ! -f pom.xml ]]; then
  echo "error: run this script from the repository root containing pom.xml" >&2
  exit 1
fi

release_version="$1"
next_snapshot="${2:-}"

if [[ "$release_version" == *"-SNAPSHOT" ]]; then
  echo "error: release version must not contain -SNAPSHOT" >&2
  exit 1
fi

if [[ -n "$next_snapshot" && "$next_snapshot" != *"-SNAPSHOT" ]]; then
  echo "error: next snapshot version must end with -SNAPSHOT" >&2
  exit 1
fi

if [[ -z "$(git status --porcelain)" ]]; then
  true
else
  echo "error: git worktree is dirty. Commit or stash changes before releasing." >&2
  exit 1
fi

echo "==> Setting project version to ${release_version}"
mvn -B -ntp versions:set -DnewVersion="${release_version}"
mvn -B -ntp versions:commit
rm -f pom.xml.versionsBackup

git commit -am "chore: release v${release_version}"
git tag -a "v${release_version}" -m "Release v${release_version}"

if [[ -n "${next_snapshot}" ]]; then
  echo "==> Setting project version to ${next_snapshot}"
  mvn -B -ntp versions:set -DnewVersion="${next_snapshot}"
  mvn -B -ntp versions:commit
  rm -f pom.xml.versionsBackup
  git commit -am "chore: start ${next_snapshot}"
fi

cat <<DONE

Release preparation complete.
Next steps:
  git push origin main
  git push origin --tags
Trigger the GitHub Packages publish workflow via the pushed tag (v${release_version}).
DONE
