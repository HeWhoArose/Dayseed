# AI Studio–GitHub Workflow and Project Gate

**Status:** Stage A planning complete; planning gate APPROVED / OPEN (owner instruction, 2026-10-30, D-208). Stage B implementation, Stage C independent validation, and Stage D final acceptance remain separately gated.  
**Repository:** https://github.com/HeWhoArose/Dayseed  
**Source of truth:** GitHub repository, default branch `main`.

## 1. Work stages

### Stage A — Planning baseline (COMPLETE; owner-approved 2026-10-30, D-208)

Required before coding:
- product requirements and v1 boundaries reconciled;
- unresolved high-impact product semantics resolved or explicitly deferred with safe defaults;
- UX information architecture and key flows specified;
- Growth Mark and comeback behaviours defined enough to test;
- journal archive direction researched and test plan written;
- design tokens and component rules centralised in DESIGN.md, with approval status clear;
- competitor claims traceable to actual inspected sources/screenshots;
- technical/privacy/data/date/local-backup/Android-platform-backup/export/delete approach reviewed, with initial v1 explicitly excluding accounts, online backup, and cross-device sync;
- accessibility and test strategy defined;
- implementation roadmap, acceptance criteria, and release gates reconciled;
- documentation links, duplicate/outdated plans, contradictions, and unsupported claims audited;
- owner approval is recorded in D-208 and this workflow; post-build acceptance obligations and explicit deferrals remain visible;

### Stage B — Full initial-v1 implementation in Google AI Studio

D-009 is resolved: continue the existing native Kotlin + Jetpack Compose Android project. Do not scaffold a second app or rewrite to React Native/Expo. Stage B starts with a dated implementation preflight against the latest GitHub main; compare the imported AI Studio workspace/base SHA with the remote SHA, preserve useful existing work, reconcile newer remote changes, and then implement the approved initial-v1 scope in the sequenced phases of implementation_plan.md. Stage B is NOT YET AUTHORISED; it requires a separate explicit written owner instruction. D-208 approved Stage A planning only.

Acceptance:
1. Confirm the project type, platform build path, and installable Android artifact for the owner-approved stack; do not substitute a website or preview for the Android app.
2. Install dependencies/toolchain cleanly using compatible, locked versions appropriate to the approved stack.
3. Build the approved Android artifact and install it on a physical Android device.
4. Prove local SQLite data survives app force-stop and restart.
5. Test a versioned schema migration with seeded synthetic records.
6. Test notification permission denial, schedule/cancel, restart behaviour, and device restart where practical. Do not promise exact timing without evidence.
7. Test airplane mode for core app behaviour.
8. Test export/delete using synthetic data.
9. Run static checks, dependency checks, and all available tests.
10. Record exact toolchain/package versions, device model, Android version, commands, logs, artefact location, and limitations.

Do not add analytics, advertising, billing, cloud backup, cross-device sync, or AI APIs during the spike. Initial v1 remains account-free and offline-first for core use; premium online backup/sync is future scope and requires a separate approval gate. Do not put private journal content, secrets, signing keys, API keys, or local machine paths containing personal information in Git.

### Stage C — Independent technical validation

Begin only after Stage B implementation is complete and committed. Inspect the resulting project from the exact current GitHub commit independently of the implementation pass. Verify build/toolchain, schema/migration, persistence, tests, offline operation, backup/restore/delete, notifications, accessibility, privacy/data flow and physical-device behaviour. Record failures and limitations; fix defects within the authorised scope and repeat affected checks. Stage B developer self-checks are not independent Stage C evidence.

### Stage D — Final owner acceptance and release readiness

Present the release candidate, exact commit SHA, executed test/evidence report, known limitations, unresolved risks and privacy/security review for explicit owner acceptance. Do not claim release readiness or perform a release action without that acceptance.

## 2. AI Studio as the active development environment

AI Studio is the active AI-assisted development environment for Dayseed. Treat the current GitHub repository and its actual latest state as authoritative; do not assume older planning notes accurately describe the implementation. Verify whether each feature exists in code and tests before changing it. Confirm the project type, Android build path, dependency compatibility, and installable artifact from repository files and build evidence rather than relying on a preview or generated description. Do not create a website as a substitute for the Android app.

### Required operating instructions for AI Studio

> Read README.md, DESIGN.md, docs/09-DECISION-LOG.md, docs/11-RISKS-AND-OPEN-QUESTIONS.md, docs/12-PROGRESS-AND-HANDOVER.md, docs/14-STACK-COMPARISON.md, and this workflow before changing files. Use the latest owner-approved scope and decisions recorded in GitHub. Continue the existing native Kotlin + Jetpack Compose Android project as approved in D-009. Do not create a website, web dashboard, or web app as a substitute. Do not start a rewrite or second app scaffold without a new explicit owner decision. Follow DESIGN.md as the sole authoritative design-token/component reference. Follow the approved product specification and architecture. Apply D-196 and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`: for routine, low-risk, reversible implementation or UX details, use the documented delegated default, record rationale and acceptance checks, and do not interrupt the owner with a micro-question. If a choice affects product scope/philosophy, data integrity, privacy/security, accessibility, monetisation, an irreversible action, or owner-level visual/product identity—or conflicts with an approved decision—keep it proposed/open and include it in the consolidated owner decision batch. Never turn a delegated default into approval for new scope. Work in small, testable phases. Commit to GitHub frequently, keep main usable, run only checks appropriate to the current authorised stage, and report exact evidence. Before editing and immediately before pushing, compare the workspace with latest remote main; never overwrite or force-push over newer remote commits. If remote advances, stop and reconcile. Never fabricate test/build results: mark checks NOT RUN, BLOCKED, FAILED, or PASSED; PASSED requires actual execution, inspected output, exact command, date/time, commit SHA, environment/toolchain and retained log/artifact. Do not put secrets or private journal data in the repository.

Do not use an agent-generated preview page as proof of native app success. A QR preview may be useful for development but is not a replacement for a built Android artifact and physical-device checks.

## 3. AI Studio and GitHub synchronization

GitHub remains canonical regardless of which coding environment is active.

When session or context ends:
1. Before a substantial change, inspect the current branch, working tree, and latest GitHub commits; identify any uncommitted work.
2. Check `git status`, current branch, latest commit, and remote.
3. Push the current branch to GitHub and verify the commit appears remotely. If unable to push, export a patch or bundle and record the exact uncommitted state before switching environments.
4. Continue from a fresh clone or pull of the GitHub branch using a local development environment or another authorised coding agent.
5. Install dependencies from the committed lockfile where one exists; do not silently regenerate versions or switch frameworks.
6. Run the project's documented checks and tests before modifying anything.
7. Make small commits and push them to GitHub.
8. Before continuing in AI Studio after a context/session reset, refresh from the latest GitHub state and reconcile local changes. Never push an older workspace over newer GitHub commits.
9. If both environments changed, reconcile by diff/rebase or a review branch; never overwrite history blindly.
10. After every handoff, record commit SHA, branch, tests run, failures, and next task in docs/12-PROGRESS-AND-HANDOVER.md.

Do not commit generated secrets, `.env` files, signing credentials, private journal data, build artifacts that the repo policy excludes, or local absolute paths. Use `.gitignore` and secret scanning. Any key accidentally committed must be revoked, not merely deleted in a later commit.

## 4. Definition of a verified native milestone

A milestone is not complete merely because code exists or a web preview loads. Evidence should include:
- repository commit SHA and branch;
- dependency installation result;
- successful Android build command and artifact;
- installation and launch on a physical Android device where available;
- relevant automated test results;
- manual acceptance checklist and known limitations;
- confirmation that no website has been substituted for the native app.

## 5. Failure and recovery discipline

For every issue: reproduce → identify root cause → implement the smallest safe fix → targeted test → regression test → full suite → re-audit. Keep the branch recoverable and the handover current. Do not report a test as passing unless it was actually run and its result inspected.

## 6. Approval record

- Planning gate decision: **APPROVED / OPEN — 2026-10-30 (D-208), owner explicit written instruction.**
- Owner planning responses O-1–O-11 recorded: **2026-10-28**; subsequent decisions D-204–D-208 are in the canonical decision log.
- D-200 defers ads for initial v1 and retains `docs/33-MONETISATION-AND-ADS-EVALUATION.md` as pre-launch material. D-201's duration-unit, formatting, aggregation, and period-control defaults are recorded in the decision log, screen/data contracts, and traceability matrix; they are not pending planning choices. The bounded semantic/traceability closeout is recorded in docs 27/28/32/35. The remaining owner planning choices and explicit deferrals are listed in docs 27/31/35. Rendered interaction/accessibility and runtime validation are reserved for separately authorised later stages. No implementation or technical validation is authorised.
- Owner approval date: **2026-10-30**
- Approved scope/version: **Dayseed initial-v1 planning baseline on GitHub `main`, including owner decisions and documented acceptance criteria/deferrals through D-208.**
- Stack decision D-009: **Approved — continue the existing native Kotlin + Jetpack Compose Android project** (2026-10-26). This is not planning-gate approval or authorisation to implement features.
- UX/design decision D-008:
- Accepted blockers/deferred questions:
- Reviewer notes:

An agent may record/enact an approval only when the owner gives a clear, explicit written instruction; never infer it from “continue”, review, silence, documentation completion or an assistant recommendation. Stage B remains unauthorised until separately approved; Stage C requires the Stage B exit review and separate implementation-plan approval.


## Current environment note (2026-10-18)

AI Studio is the active development environment. Remove and avoid reintroducing instructions, quota-recovery procedures, filenames, or assumptions from previous coding environments in current project documentation. GitHub remains the canonical shared source of truth. This note updates environment guidance only; it does not by itself change implementation authorization or product decisions.


## Current stage sequence — authoritative correction (2026-10-30)

This section supersedes older stage labels and sequencing in this document and historical handover text:

- **Stage A — Planning:** complete and approved/open under D-208.
- **Stage B — Full initial-v1 implementation:** implementation in Google AI Studio, beginning with a dated implementation preflight against the latest GitHub main. Preserve useful existing code, reconcile gaps, implement approved scope and commit the work. Stage B is **NOT YET AUTHORISED**; it requires a separate explicit written owner instruction.
- **Stage C — Independent technical validation:** after Stage B is implemented and committed, independently inspect and validate the resulting project from the current GitHub commit; run agreed builds/tests and runtime/device/accessibility/privacy/data-integrity checks, report defects, fix under the authorised workflow, and revalidate. Stage B self-checks are not independent Stage C evidence.
- **Stage D — Final owner acceptance/release readiness:** present the verified release candidate and evidence to the owner for explicit acceptance. No release claim/action without that acceptance.

**Inspection evidence:** Every project inspection must state date/time and timezone, repository, branch, exact inspected remote commit SHA, imported workspace/base SHA (or UNKNOWN), inspected paths plus line ranges/symbols, findings, and uninspected areas. Evidence must be specific to the inspected commit.

**AI Studio/GitHub protection:** GitHub main is canonical. Before editing and immediately before pushing, compare the imported workspace with the latest remote head. If remote main is newer, fetch and reconcile before editing/pushing; never overwrite, force-push, reset away, or push stale workspace state over newer GitHub work. If remote advances during work, stop and reconcile.

**Test-result integrity:** report NOT RUN, BLOCKED, FAILED, or PASSED accurately. PASSED requires actual execution and inspected evidence: exact command/task, date/time, commit SHA, environment/toolchain (and device where relevant), exit result, and retained log/artifact. Planned, inferred, generated, preview-only, or unexecuted checks must never be reported as passed. Never fabricate results.
