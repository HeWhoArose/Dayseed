---
name: dayseed-handover
description: Update Dayseed's canonical docs with verified status after work or decisions (progress ledger, decision register, completeness review, traceability) and state one next action. Use after finishing a slice, recording an owner decision, or ending a session.
argument-hint: "[work or decisions to record]"
---

# Dayseed handover / checkpoint

Record the state of: $ARGUMENTS

Update the existing canonical documents. Never create a competing status or decision file.

## 1. Gather facts (do not rely on chat memory)
Branch, `git rev-parse HEAD`, `git status --short`, `git log --oneline` for the work, files changed, and the real verification results (command, UTC time, SHA, exit result, counts), from `/dayseed-verify` output or re-run if missing. Read the last entry of `docs/12-PROGRESS-AND-HANDOVER.md` first so you extend it consistently.

## 2. Separate the categories
- **Owner-accepted decisions** (explicit written owner instruction) vs **proposals/recommendations**, brainstorms, deferrals and open questions. Proposals stay PROPOSED. Silence, defaults and existing code are never approval.
- **Done and verified** vs **done but unverified** vs **NOT RUN / BLOCKED**.

## 3. Update the documents (AGENTS.md rule 5)
Reconcile every approved decision across all affected documents, then update as relevant:
- `docs/12-PROGRESS-AND-HANDOVER.md` (status, commit, evidence, remaining gates, one next action; keep historical entries labelled historical)
- `docs/09-DECISION-LOG.md` (only for owner-approved decisions) and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`
- `docs/27-PLANNING-COMPLETENESS-REVIEW.md` and `docs/28-REQUIREMENT-TRACEABILITY.md`
- README / CHANGELOG only where the change alters what they claim

## 4. Say precisely
- Files and commit SHA changed; whether the tree is clean or dirty.
- Tests and CI: PASSED / FAILED / BLOCKED / NOT RUN with evidence. Never write PASSED without executed, inspected output. Device, accessibility and usability cases stay NOT RUN until evidenced.
- Stage status: Stage B authorised (D-213); Stage C and Stage D NOT OPEN. A documentation commit does not authorise either.
- Outstanding owner decisions, risks, blockers, and exactly one highest-priority next action.

## 5. Check and commit
Run `bash scripts/check-doc-links.sh` and fix broken links. Commit the doc updates on the designated branch; never merge; push only to the designated branch after re-checking the remote head.

Every inspection statement should carry: date/time + timezone, repository, branch, inspected commit SHA, paths/line ranges inspected, findings, and areas not inspected.
