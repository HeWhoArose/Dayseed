---
description: Update the canonical project handover with verified status and one next action
agent: project-lead
---
Prepare the project handover/checkpoint for: $ARGUMENTS

Read project instructions, the current authoritative handover/progress ledger, docs index, active plan, decision log, current branch/HEAD/working tree and latest actual verification evidence. Update the project's existing authoritative documents instead of creating competing sources of truth. If no such documentation exists in a new project, use `new-project-template/` as a starting point and adapt it to the project.

Record accepted versus proposed decisions accurately; changes actually completed; exact files and commit; commands/results/CI links; uncommitted state; tests not run and why; outstanding gates, decisions, risks and the single highest-priority next step. Preserve historical provenance. For Dayseed under the closed gate, only documentation work explicitly permitted by AGENTS.md may occur. Do not claim PASS without evidence. Never merge or push.

Include recovery/failover status: which model roles actually ran, which failed, whether a fallback was used, partial edits, current Git state, tests/CI evidence, and any lane that remains BLOCKED. The next session must inspect the repository and handover before continuing.
