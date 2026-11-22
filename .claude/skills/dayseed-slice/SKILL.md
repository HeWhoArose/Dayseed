---
name: dayseed-slice
description: Implement one bounded, already-accepted Dayseed change or finding with a regression test, minimal diff and verified results. Use when asked to fix a specific confirmed bug or build one defined slice of the Stage B scope.
argument-hint: "[the accepted finding or slice]"
---

# Implement one Dayseed slice

Implement this accepted slice only: $ARGUMENTS

## Pre-flight (stop without code changes if any fail)
1. Read `AGENTS.md`; confirm the work is inside Stage B scope (D-213, root `implementation_plan.md`). Stage C validation and Stage D acceptance are NOT open. Do not start or imply them.
2. Check the relevant decisions in `docs/09-DECISION-LOG.md` (and `docs/25`) so the change does not contradict an owner decision. Recommendations, silence or existing code are not owner approval.
3. Record base state: `git rev-parse HEAD`, branch, `git status --short`. Fetch `origin/main`; if it is newer than the workspace, reconcile first and never overwrite newer remote work.
4. Confirm the problem is real (reproduced or traced in code), acceptance criteria are clear, and the owning subsystem is identified. If not, stop and report what is missing.
5. If it needs a scope, privacy/security, data-integrity/recovery, accessibility, monetisation or irreversible-action decision, stop and ask the owner (batch as Accept / Modify / Defer per D-196). Decide routine, reversible details using documented conservative defaults.

## Implement
- Trace trigger, caller, owner, contract, consumers and fallback paths before editing.
- Make the smallest correction. No unrelated cleanup, refactors or scope growth.
- Add a regression test that would fail before the fix; ideally run it against the unfixed code first and record that it failed.
- Respect DESIGN.md as the sole design-token/component authority and the owner reference layout for UI work.
- Never read, print or commit secrets, `.env`-style files, keystores or private journal data.

## Verify
Run `/dayseed-verify` (targeted, then neighbouring, then the full CI gate). Report real results only.

## Finish
- Commit on the designated working branch only, with a clear message. Do not merge. Push only to the session's designated branch, after re-checking the remote head. Open a PR only if asked.
- If the change affects decisions, requirements or status, run `/dayseed-handover` to reconcile docs.
- Summarise: what changed (files/symbols), the regression test, exact verification results, anything NOT RUN, and one next action.
