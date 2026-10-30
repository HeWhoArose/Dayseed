---
description: Resume from the authoritative handover and run the allowed next workflow end-to-end
agent: project-lead
---
Resume and continue the project for this goal: $ARGUMENTS

MANDATORY ORDER:
1. **Load durable context.** Read the applicable AGENTS.md first, then README, docs index, current canonical handover/progress, decision log, active plans and relevant specifications. Establish branch, HEAD SHA, working tree, and current CI evidence. Do not rely on the chat alone.
2. **Resolve project and plan.** Use the repository identity and index to identify active work. If there are several independently active plans and the user's instruction does not identify which one takes priority, explain the candidate next steps and ask before implementation; don't pick an arbitrary phase.
3. **Apply gates.** Read every hard restriction. Dayseed currently remains in planning/documentation only: no source/runtime audit, live DB inspection, app/schema/migration edits, build/test/APK/device validation until owner approval of the planning gate and separate technical-validation authorisation. For DreamVille, respect canonical authority rules, current active plan, branch policy and no-merge/no-push rule.
4. **Plan and audit.** State the intended next slice and acceptance criteria. For substantial work invoke the audit-lead and its independent architecture, bug, test and requirements reviewers. Require actual child reports. Compare findings against canonical evidence and rank them; do not treat model agreement as truth.
5. **Implement only where allowed.** If the user asked to continue implementation and project gates permit it, delegate one bounded slice to implementer. If the change needs a consequential product/security/data-integrity decision or an authority/plan conflict remains unresolved, stop for owner input. Do not silently expand scope.
6. **Verify.** Invoke verification-runner for targeted, adjacent, and required full checks. The verification agent must execute real commands and report results. Do not claim a test passed if it did not run. Local dependency/network failures should be recorded, not hidden.
7. **Independent review.** Invoke final-reviewer via audit-lead. If it returns FAIL, fix the smallest issue and repeat tests/review. If a blocker is not resolved, stop and report it.
8. **Persist the work.** Update the existing authoritative progress/phase/handover docs with status, commit SHA, exact tests/CI evidence, remaining gates and one next action. Commit documentation when appropriate under project rules, but never push/merge without explicit permission. Report working-tree cleanliness accurately.

Do not merely describe steps that can be performed in this session. Perform the allowed work, and clearly stop only at a real gate, ambiguity, provider/tool failure, or required owner decision. If a required child agent cannot be launched, report that stage as BLOCKED instead of pretending it happened.

FAILURE/RECOVERY RULES: Read `.opencode/RECOVERY_POLICY.md`. Use the bounded retry plugin for classified transient request failures. For a failed child agent, launch the matching one-hop fallback only after inspecting the logged error metadata and current checkpoint. For a failed writer, verify the old writer stopped and inspect the diff before handing off. For a failed lead call, do not claim automatic continuation; resume from the last canonical handover in the same or a new session. Never bypass Dayseed's planning gate or DreamVille's branch/merge rules. Never spend paid credits automatically.
