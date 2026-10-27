# AI Studio–GitHub Workflow and Project Gate

**Status:** Proposed operating procedure; implementation is not authorised until the owner explicitly approves the planning gate.  
**Repository:** https://github.com/HeWhoArose/Dayseed  
**Source of truth:** GitHub repository, default branch `main`.

## 1. Work stages

### Stage A — Finish planning (current stage)

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
- owner reviews and explicitly approves the planning gate;
- the current status and remaining blockers are captured in `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, the requirement map in `docs/28-REQUIREMENT-TRACEABILITY.md`, the UX recommendations in `docs/29-UX-DESIGN-APPROVAL-PACK.md`, the proposed schema closure plan in `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`, the consolidated owner review in `docs/31-OWNER-PLANNING-DECISION-BATCH.md`, and the handover.

### Stage B — Bounded stack-specific feasibility validation

This is not full feature implementation. D-009 is now resolved: the owner approved continuing the existing native Kotlin + Jetpack Compose Android project. Do not scaffold a second app or rewrite to React Native/Expo. When—and only when—the owner explicitly authorises the planning gate and technical validation, inspect the existing source/build configuration and validate that same native project. This decision does not authorise validation or feature implementation by itself.

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

### Stage C — Feature implementation

Only after the spike passes (or its blocker is consciously accepted) and the owner approves the stack and implementation plan. Build in roadmap phases, with a working native app and tests at every stage.

## 2. AI Studio as the active development environment

AI Studio is the active AI-assisted development environment for Dayseed. Treat the current GitHub repository and its actual latest state as authoritative; do not assume older planning notes accurately describe the implementation. Verify whether each feature exists in code and tests before changing it. Confirm the project type, Android build path, dependency compatibility, and installable artifact from repository files and build evidence rather than relying on a preview or generated description. Do not create a website as a substitute for the Android app.

### Required operating instructions for AI Studio

> Read README.md, DESIGN.md, docs/09-DECISION-LOG.md, docs/11-RISKS-AND-OPEN-QUESTIONS.md, docs/12-PROGRESS-AND-HANDOVER.md, docs/14-STACK-COMPARISON.md, and this workflow before changing files. Use the latest owner-approved scope and decisions recorded in GitHub. Continue the existing native Kotlin + Jetpack Compose Android project as approved in D-009. Do not create a website, web dashboard, or web app as a substitute. Do not start a rewrite or second app scaffold without a new explicit owner decision. Follow DESIGN.md as the sole authoritative design-token/component reference. Follow the approved product specification and architecture. If a requirement is unresolved, stop and report the question rather than inventing a product decision. Work in small, testable phases. Commit to GitHub frequently, keep main usable, run targeted tests and the full available suite, and report exact build/test evidence. Do not put secrets or private journal data in the repository.

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

- Planning gate decision: **Not approved**
- Owner approval date:
- Approved scope/version:
- Stack decision D-009: **Approved — continue the existing native Kotlin + Jetpack Compose Android project** (2026-10-26). This is not planning-gate approval or authorisation to implement features.
- UX/design decision D-008:
- Accepted blockers/deferred questions:
- Reviewer notes:

Only the owner may change the approval status. Documentation completion by an assistant or coding agent does not itself authorise implementation.


## Current environment note (2026-10-18)

AI Studio is the active development environment. Remove and avoid reintroducing Replit-specific instructions, quota recovery procedures, file names, or assumptions in current project documentation. GitHub remains the canonical shared source of truth. This note updates environment guidance only; it does not by itself change implementation authorization or product decisions.
