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

Feature implementation is a later stage after technical validation and separate owner approval. A documentation commit, schema review, or approval of the stack alone does not authorise it.
