#!/usr/bin/env bash
# PostToolUse hook: after a Markdown file is edited, run the repo's link checker.
# Reports broken relative links back to Claude (exit 2 + stderr); silent when clean.
set -uo pipefail

input="$(cat)"
path="$(jq -r '.tool_response.filePath // .tool_input.file_path // empty' <<<"$input")"
[[ "$path" == *.md ]] || exit 0

root="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
out="$(bash "$root/scripts/check-doc-links.sh" "$root" 2>&1)"
status=$?
if [[ $status -ne 0 ]]; then
  echo "Markdown link check failed after editing $path:" >&2
  echo "$out" | tail -n 25 >&2
  exit 2
fi
exit 0
