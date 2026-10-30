---
description: Read-only independent multi-model audit of a scope
agent: audit-lead
---
Run a READ-ONLY multi-model audit of: $ARGUMENTS

Mandatory sequence:
1. Read governing project instructions and stage restrictions first. Establish current branch, working-tree status and HEAD SHA, then read the authoritative handover, docs index, active plans/decision log, and latest available CI evidence where permitted.
2. Launch architecture-auditor, bug-hunter, test-auditor, and requirements-auditor as independent child sessions. Give each a narrow but complete scope and tell each not to see other initial findings. Await the real results. If a child model/tool fails or the provider is unavailable, report that failure; do not simulate its report.
3. Respect gate restrictions. Dayseed is currently planning/documentation only; all source/database/build/test/runtime questions are BLOCKED until the documented gates are approved.
4. Compare and verify findings, merge duplicates but not distinct root causes. Model agreement is not proof. Require exact evidence, real caller/owner/consumer mapping, severity, confidence, impact and smallest regression test.
5. Rank the results, separate confirmed bugs, hypotheses, missing evidence, approved requirement gaps, deferred/unapproved ideas, and blocked work.
6. Do not edit code or implement fixes. Return an executive summary, finding matrix, disagreements/resolutions, rejected findings with reasons, exact commit/CI provenance and one recommended next action.

Failure handling: read `.opencode/RECOVERY_POLICY.md`. If an auditor fails, launch its matching `*-fallback` agent once after checking the checkpoint/log. Never present a missing report as if the agent ran. Preserve a stage checkpoint in the project's existing handover/plan when the audit finishes or is blocked.
