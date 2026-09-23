#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

if [[ ! -d "$ROOT/.git" ]]; then
  echo "No .git directory found. Initialize git before installing hooks."
  exit 1
fi

mkdir -p "$ROOT/.git/hooks"

install_hook() {
  local name="$1"
  local src="$ROOT/scripts/git-hooks/${name}"
  local dst="$ROOT/.git/hooks/${name}"
  cp "$src" "$dst"
  chmod +x "$dst"
  echo "Installed ${name} -> .git/hooks/${name}"
}

install_hook pre-commit
install_hook commit-msg
install_hook pre-push

echo
echo "Hooks:"
echo "  pre-commit  — branch review/<feature> (+ initial main only) + ./gradlew qualityCheck (+ ui qualityCheck if ui/ staged)"
echo "  commit-msg  — CONTEXT | message"
echo "  pre-push    — block push to main/master after initial commit"
