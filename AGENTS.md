# Instructions for AI agents working on Dayseed

**Read first:** [Continuation Handover and Decision Protocol](docs/32-CONTINUATION-HANDOVER.md). Then refresh GitHub `main` and read `README.md`, `docs/09-DECISION-LOG.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, and `docs/31-OWNER-PLANNING-DECISION-BATCH.md`.

## Non-negotiable working rules

1. GitHub `main` is canonical. Fetch latest content/SHA before edits; verify every write by fetching again.
2. The current stage is planning/documentation. The planning gate is **not approved**. Do not edit application code/schema/migrations, inspect the live database, run an implementation audit, build/tests, install an APK, or validate on a device until the owner explicitly approves the planning gate and separately authorises technical validation.
3. Treat `docs/09-DECISION-LOG.md` as authoritative for owner decisions. Do not turn recommendations, silence, existing source code, or delegated defaults into owner approval.
4. Follow D-196: decide routine, reversible, low-risk details using conservative documented defaults. Escalate scope/philosophy, privacy/security, data integrity/recovery, accessibility, monetisation, irreversible actions, major identity/design decisions, and conflicts. Batch material owner choices using Accept / Modify / Defer; do not pepper the owner with micro-questions.
5. Reconcile every approved decision across affected documents, then update `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, and `docs/12-PROGRESS-AND-HANDOVER.md` as relevant. Keep historical proposals labelled historical.
6. Never claim builds, tests, usability/accessibility validation, runtime privacy, migrations, or device behaviour passed without inspected evidence. Planned acceptance cases are not executed results.
7. Protect secrets and private data. The repository tree includes `.env`; do not display, quote, copy, or log its contents. Do not commit credentials, signing material, or private journal data.
8. Preserve and consult [the owner-provided UI reference](docs/prototypes/dayseed-owner-reference-layout.webp) during UX planning. It is directional, not final UI/token approval and does not open the planning gate. DESIGN.md remains the sole design-token/component authority.

Feature implementation is a later stage after technical validation and separate owner approval. A documentation commit, schema review, or approval of the stack alone does not authorise it.


## Shared OpenCode governance policies

Every project agent, including coordinators, specialists, and fallback agents, must follow these shared policies in addition to this file and the canonical project documents:

1. [Decision Integrity and Independent Evaluation](.opencode/DECISION_INTEGRITY.md) — evaluate evidence independently, challenge proposals constructively, and never agree merely to please the owner or disagree merely to appear critical.
2. [Owner Authority and Approval Boundaries](.opencode/OWNER_AUTHORITY.md) — the owner retains final authority; agents cannot approve owner decisions or open a project approval gate.
3. [Model Registry and Evaluation Rules](.opencode/MODEL_REGISTRY.md) — current model assignments are historical configuration facts, not evidence of approval, quality, availability, or free access.
4. [Model Recovery and Manual Replacement](.opencode/MODEL_RECOVERY_POLICY.md) — distinguish bounded retries from task-level fallback and require manual owner appointment for model replacement.
5. [Beginner-Friendly Communication Standard](.opencode/COMMUNICATION_STANDARD.md) — explain findings and recommendations in clear language, define unfamiliar terms, break complex results into steps, and show how to verify important claims.

Read these policies at the start of every significant task. If a policy conflicts with this file, the canonical decision log, or an explicit owner instruction, preserve the higher-authority project rule and report the conflict rather than silently choosing a weaker rule.

These shared policies apply to the existing agent system without changing any agent's model assignment, permissions, or frontmatter. The project-lead and project-lead-fallback definitions are intentionally unchanged in this configuration update; their direct instruction upgrade is deferred.
