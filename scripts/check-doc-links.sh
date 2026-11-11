#!/usr/bin/env bash
# check-doc-links.sh — detect missing relative Markdown targets.
#
# Usage: ./scripts/check-doc-links.sh [root_dir]
#   root_dir defaults to the repository root (script's parent-parent).
#
# Scans every *.md file under root_dir for inline markdown links
#   [text](target)
# resolves relative targets against the source file's directory, skips
# absolute URLs (http://, https://, mailto:) and fragment-only links,
# and reports any target that does not exist on disk.
#
# Exit status: 0 if all relative markdown targets resolve, 1 otherwise.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT="${1:-$(cd "$SCRIPT_DIR/.." && pwd)}"

broken=0
checked=0

while IFS= read -r -d '' mdfile; do
    srcdir="$(cd "$(dirname "$mdfile")" && pwd)"
    # Extract link targets from [text](target) or [text](target#fragment).
    targets="$(grep -oE '\[[^][]*\]\([^)]+\)' "$mdfile" \
        | sed -E 's/^.*\]\(//; s/\)$//')"
    while IFS= read -r target; do
        [[ -z "$target" ]] && continue
        case "$target" in
            http://*|https://*|mailto://*|mailto:*) continue ;;
            \#*) continue ;;            # fragment-only within same page
        esac
        target_path="${target%%\#*}"     # strip fragment
        [[ -z "$target_path" ]] && continue
        resolved="$(cd "$srcdir" && pwd -P)/$target_path"
        # Normalise ./ and .. segments so the existence check is robust.
        resolved="$(cd "$srcdir" && python3 -c "import os,sys; print(os.path.normpath(os.path.join('$resolved'.replace(chr(92),'/'))))" 2>/dev/null || echo "$resolved")"
        checked=$((checked + 1))
        if [[ ! -f "$resolved" ]]; then
            echo "BROKEN: $mdfile -> $target (resolved: $resolved)"
            broken=1
        fi
    done <<< "$targets"
done < <(find "$ROOT" -name '*.md' -type f -print0)

echo "checked $checked relative markdown link targets; $broken broken"
exit $broken
