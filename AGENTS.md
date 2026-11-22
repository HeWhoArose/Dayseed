# Instructions for AI agents working on Dayseed

**Read first:** [Continuation Handover and Decision Protocol](docs/32-CONTINUATION-HANDOVER.md). Then refresh GitHub `main` and read `README.md`, `docs/09-DECISION-LOG.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, and `docs/31-OWNER-PLANNING-DECISION-BATCH.md`.

## Non-negotiable working rules

1. GitHub `main` is canonical. Fetch the latest content/SHA before editing and again immediately before pushing; verify every write by fetching. If remote `main` is newer, or advances during work, stop and reconcile. Never overwrite, force-push, reset away, or push stale workspace state over newer GitHub work.
2. Treat `docs/09-DECISION-LOG.md` as authoritative for owner decisions. Do not turn recommendations, silence, existing source code, or delegated defaults into owner approval.
3. Follow D-196: decide routine, reversible, low-risk details using conservative documented defaults. Escalate scope/philosophy, privacy/security, data integrity/recovery, accessibility, monetisation, irreversible actions, major identity/design decisions, and conflicts. Batch material owner choices using Accept / Modify / Defer; do not pepper the owner with micro-questions.
4. Reconcile every approved decision across affected documents, then update `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, and `docs/12-PROGRESS-AND-HANDOVER.md` as relevant. Keep historical proposals labelled historical.
5. Protect secrets and private data. The repository tree includes `.env`; do not display, quote, copy, or log its contents. Do not commit credentials, signing material, or private journal data.
6. DESIGN.md is the sole design-token/component authority. Consult [the owner-provided UI reference](docs/prototypes/dayseed-owner-reference-layout.webp) for UX work; it is directional, not final approval.
7. Never fabricate results. Report NOT RUN, BLOCKED, FAILED, or PASSED accurately. PASSED requires actual execution and inspected evidence: exact command/task, date/time, commit SHA, environment/toolchain (and device where relevant), exit result, and retained log/artifact. Planned, inferred, generated, preview-only, or unexecuted checks are never PASSED, and this covers builds, tests, usability/accessibility validation, runtime privacy, migrations and device behaviour.
8. Every project inspection must state date/time and timezone, repository, branch, exact inspected remote commit SHA, imported workspace/base SHA (or UNKNOWN), inspected paths with line ranges/symbols, findings, and uninspected areas.

## Stage gates (authoritative; D-213, 2026-11-01)

- **Stage A — Planning:** complete, approved under D-208.
- **Stage B — Full initial-v1 implementation:** AUTHORISED by D-213, scope in root `implementation_plan.md` (Phase 0 record: `docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md`; progress bands resolved as owner-confirmed Option A under D-040/D-216).
- **Stage C — Independent technical validation:** NOT AUTHORISED; needs separate owner approval after Stage B is complete and committed. Stage B self-checks are not independent Stage C evidence.
- **Stage D — Final owner acceptance/release readiness:** NOT OPEN. No release claim or action without explicit owner acceptance.

A documentation commit or model recommendation does not authorise a later gate. Later decisions and detail live in `docs/09-DECISION-LOG.md`.

## Shared OpenCode governance policies

Every project agent, including coordinators, specialists and fallback agents, must follow these in addition to this file and the canonical documents. Read them at the start of every significant task.

1. [Decision Integrity and Independent Evaluation](.opencode/DECISION_INTEGRITY.md) — evaluate evidence independently; never agree merely to please the owner or disagree merely to appear critical.
2. [Owner Authority and Approval Boundaries](.opencode/OWNER_AUTHORITY.md) — the owner has final authority; record/enact a gate approval only on a clear, explicit written instruction, never by inference.
3. [Model Registry and Evaluation Rules](.opencode/MODEL_REGISTRY.md) — model assignments are historical configuration facts, not evidence of approval, quality, availability, or free access.
4. [Model Recovery and Manual Replacement](.opencode/MODEL_RECOVERY_POLICY.md) — bounded retries differ from task-level fallback; model replacement needs manual owner appointment.
5. [Beginner-Friendly Communication Standard](.opencode/COMMUNICATION_STANDARD.md) — plain language, define unfamiliar terms, break results into steps, show how to verify.

If a policy conflicts with this file, the decision log, or an explicit owner instruction, keep the higher-authority rule and report the conflict.
