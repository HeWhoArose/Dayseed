---
description: Run permitted regression checks and report real outcomes
agent: verification-runner
model: opencode/nemotron-3.5-super-vl-free
---
Verify the current change for: $ARGUMENTS

Read project instructions and stage restrictions first. Do not bypass Dayseed's closed planning gate. When execution is allowed, record branch/HEAD/working-tree status; inspect scripts and exact test guidance; run targeted tests, related integration tests, required full tests, lint/type-check/build and release contracts as appropriate. Report exact commands, exit codes, counts and exact-head CI evidence. If not authorised or unable to run, report BLOCKED/NOT RUN and the precise reason; never invent a result or claim a live-provider/device check passed from mocked CI.

If the verification model fails, use `verification-runner-fallback` once. Distinguish model/provider failure from an actual failing test. Capture exact command output, exit code, and commit; do not mark an unavailable check as PASS.
