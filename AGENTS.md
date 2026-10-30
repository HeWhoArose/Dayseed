# Instructions for AI agents working on Dayseed

**Read first:** [Continuation Handover and Decision Protocol](docs/32-CONTINUATION-HANDOVER.md). Then refresh GitHub `main` and read `README.md`, `docs/09-DECISION-LOG.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, and `docs/31-OWNER-PLANNING-DECISION-BATCH.md`.

## Non-negotiable working rules

1. GitHub `main` is canonical. Fetch latest content/SHA before edits; verify every write by fetching again.
2. Stage A planning is complete and approved/open under D-208. The authoritative sequence is Stage B full initial-v1 implementation in Google AI Studio, Stage C independent technical validation after implementation, and Stage D final owner acceptance/release readiness. Stage B is **NOT YET AUTHORISED** until the owner separately authorises it in writing. Stage C and Stage D are later gates; this documentation correction does not open them.
3. Treat `docs/09-DECISION-LOG.md` as authoritative for owner decisions. Do not turn recommendations, silence, existing source code, or delegated defaults into owner approval.
4. Follow D-196: decide routine, reversible, low-risk details using conservative documented defaults. Escalate scope/philosophy, privacy/security, data integrity/recovery, accessibility, monetisation, irreversible actions, major identity/design decisions, and conflicts. Batch material owner choices using Accept / Modify / Defer; do not pepper the owner with micro-questions.
5. Reconcile every approved decision across affected documents, then update `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, and `docs/12-PROGRESS-AND-HANDOVER.md` as relevant. Keep historical proposals labelled historical.
6. Never claim builds, tests, usability/accessibility validation, runtime privacy, migrations, or device behaviour passed without inspected evidence. Planned acceptance cases are not executed results.
7. Protect secrets and private data. The repository tree includes `.env`; do not display, quote, copy, or log its contents. Do not commit credentials, signing material, or private journal data.
8. Preserve and consult [the owner-provided UI reference](docs/prototypes/dayseed-owner-reference-layout.webp) during UX planning. It is directional, not final UI/token approval. DESIGN.md remains the sole design-token/component authority.

Stage B implementation is the next product stage after Stage A planning, followed by independent Stage C technical validation and Stage D owner acceptance. Stage B still requires separate explicit owner authorisation. A documentation commit, schema review, or approval of the stack alone does not authorise it.


## Shared OpenCode governance policies

Every project agent, including coordinators, specialists, and fallback agents, must follow these shared policies in addition to this file and the canonical project documents:

1. [Decision Integrity and Independent Evaluation](.opencode/DECISION_INTEGRITY.md) — evaluate evidence independently, challenge proposals constructively, and never agree merely to please the owner or disagree merely to appear critical.
2. [Owner Authority and Approval Boundaries](.opencode/OWNER_AUTHORITY.md) — the owner retains final authority; agents may record/enact a gate approval only when the owner gives a clear, explicit written instruction, and may not infer approval.
3. [Model Registry and Evaluation Rules](.opencode/MODEL_REGISTRY.md) — current model assignments are historical configuration facts, not evidence of approval, quality, availability, or free access.
4. [Model Recovery and Manual Replacement](.opencode/MODEL_RECOVERY_POLICY.md) — distinguish bounded retries from task-level fallback and require manual owner appointment for model replacement.
5. [Beginner-Friendly Communication Standard](.opencode/COMMUNICATION_STANDARD.md) — explain findings and recommendations in clear language, define unfamiliar terms, break complex results into steps, and show how to verify important claims.

Read these policies at the start of every significant task. If a policy conflicts with this file, the canonical decision log, or an explicit owner instruction, preserve the higher-authority project rule and report the conflict rather than silently choosing a weaker rule.

These shared policies apply to the existing agent system without changing any agent's model assignment, permissions, or frontmatter. The project-lead and project-lead-fallback definitions are intentionally unchanged in this configuration update; their direct instruction upgrade is deferred.


## Current stage sequence — authoritative correction (2026-10-30)

This section supersedes older stage labels and sequencing in this document and historical handover text:

- **Stage A — Planning:** complete and approved/open under D-208.
- **Stage B — Full initial-v1 implementation:** implementation in Google AI Studio, beginning with a dated implementation preflight against the latest GitHub main. Preserve useful existing code, reconcile gaps, implement approved scope and commit the work. Stage B is **NOT YET AUTHORISED**; it requires a separate explicit written owner instruction.
- **Stage C — Independent technical validation:** after Stage B is implemented and committed, independently inspect and validate the resulting project from the current GitHub commit; run agreed builds/tests and runtime/device/accessibility/privacy/data-integrity checks, report defects, fix under the authorised workflow, and revalidate. Stage B self-checks are not independent Stage C evidence.
- **Stage D — Final owner acceptance/release readiness:** present the verified release candidate and evidence to the owner for explicit acceptance. No release claim/action without that acceptance.

**Inspection evidence:** Every project inspection must state date/time and timezone, repository, branch, exact inspected remote commit SHA, imported workspace/base SHA (or UNKNOWN), inspected paths plus line ranges/symbols, findings, and uninspected areas. Evidence must be specific to the inspected commit.

**AI Studio/GitHub protection:** GitHub main is canonical. Before editing and immediately before pushing, compare the imported workspace with the latest remote head. If remote main is newer, fetch and reconcile before editing/pushing; never overwrite, force-push, reset away, or push stale workspace state over newer GitHub work. If remote advances during work, stop and reconcile.

**Test-result integrity:** report NOT RUN, BLOCKED, FAILED, or PASSED accurately. PASSED requires actual execution and inspected evidence: exact command/task, date/time, commit SHA, environment/toolchain (and device where relevant), exit result, and retained log/artifact. Planned, inferred, generated, preview-only, or unexecuted checks must never be reported as passed. Never fabricate results.
