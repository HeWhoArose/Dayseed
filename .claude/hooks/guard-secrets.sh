#!/usr/bin/env bash
# PreToolUse guard: block Read/Edit/Write/Bash access to secrets and signing material.
# Blocks: .env (NOT .env.example), *.keystore, *.keystore.base64, *.jks, local.properties.
# Exit 2 = block (stderr is shown to Claude). Exit 0 = allow.
set -uo pipefail

input="$(cat)"
tool="$(jq -r '.tool_name // empty' <<<"$input")"

block() {
  echo "Blocked by .claude/hooks/guard-secrets.sh: $1. Secrets and signing material are off-limits (AGENTS.md rule 7). Use .env.example for variable names." >&2
  exit 2
}

# Path-style check (file tools): match the final path component.
check_path() {
  local base
  base="$(basename -- "$1")"
  case "$base" in
    .env.example) return 0 ;;
    .env|.env.*) block "access to $base" ;;
    *.keystore|*.keystore.base64|*.jks) block "access to $base" ;;
    local.properties) block "access to $base" ;;
  esac
}

case "$tool" in
  Read|Edit|Write|NotebookEdit)
    path="$(jq -r '.tool_input.file_path // .tool_input.notebook_path // empty' <<<"$input")"
    [[ -n "$path" ]] && check_path "$path"
    ;;
  Grep|Glob)
    # A search rooted directly at a secret file; directory searches are not blocked here.
    path="$(jq -r '.tool_input.path // empty' <<<"$input")"
    [[ -n "$path" && ! -d "$path" ]] && check_path "$path"
    ;;
  Bash)
    cmd="$(jq -r '.tool_input.command // empty' <<<"$input")"
    # Word-boundary matches so scripts/bootstrap-debug-keystore.sh and .env.example stay allowed.
    if grep -Eq '(^|[/[:space:]"'"'"'=])\.env($|[[:space:]"'"'"'/;|&)])' <<<"$cmd"; then
      block "a command that references .env"
    fi
    if grep -Eq '\.(keystore|jks)(\.base64)?($|[[:space:]"'"'"';|&)])' <<<"$cmd"; then
      block "a command that references a keystore file"
    fi
    if grep -Eq '(^|[/[:space:]"'"'"'=])local\.properties($|[[:space:]"'"'"';|&)])' <<<"$cmd"; then
      block "a command that references local.properties"
    fi
    ;;
esac
exit 0
