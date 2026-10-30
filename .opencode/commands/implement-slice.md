---
description: Implement one bounded, accepted finding with regression coverage
agent: implementer
model: nvidia/poolside/laguna-xs-2.1
---
Implement this accepted slice only: $ARGUMENTS

Pre-flight: read project instructions, active plan, handover, decision log, exact accepted finding, current branch, HEAD and working tree. Respect all stage gates. If the problem is not confirmed, acceptance criteria are not clear, the owning subsystem is not identified, or the required authorisation is absent, stop without code changes.

For Dayseed, while planning gate approval and separate technical-validation authorisation are absent, do not inspect application source/live database, change application/schema/migration files, run builds/tests or perform device validation. Report BLOCKED; permitted documentation-only planning can continue.

For an allowed change: trace trigger/caller, owner, contract, consumers, fallbacks and tests; make the smallest correction; add a regression that would fail before the fix; run targeted checks followed by required neighbouring/full checks; report exact results. Do not bundle unrelated cleanup, weaken tests to pass, merge or push. If the user explicitly asked for the full allowed lifecycle, continue to verification and re-audit rather than stopping after writing code.

Before editing, read `.opencode/RECOVERY_POLICY.md` and record the base HEAD and working-tree state. If the implementer provider fails mid-task, do not start a second writer until the first has stopped; inspect partial diffs, then hand off to `implementer-fallback` once with the checkpoint. Do not use a paid model automatically.
