# Planning Gate and Replit–GitHub Workflow

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
- technical/privacy/data/date/backup/export/delete approach reviewed;
- accessibility and test strategy defined;
- implementation roadmap, acceptance criteria, and release gates reconciled;
- documentation links, duplicate/outdated plans, contradictions, and unsupported claims audited;
- owner reviews and explicitly approves the planning gate.

### Stage B — Bounded native-app feasibility spike

This is not the full feature implementation. After Stage A approval, test the proposed React Native + Expo + TypeScript + SQLite candidate in a separate, minimal native app scaffold.

Acceptance:
1. The project is demonstrably an Android/iOS React Native app project, not a website. Confirm the expected app config, native build path, and Android application artifact.
2. Install dependencies cleanly with SDK-compatible versions and commit a lockfile.
3. Build an APK or development build and install it on a physical Android device.
4. Prove local SQLite data survives app force-stop and restart.
5. Test a versioned schema migration with seeded synthetic records.
6. Test notification permission denial, schedule/cancel, restart behaviour, and device restart where practical. Do not promise exact timing without evidence.
7. Test airplane mode for core app behaviour.
8. Test export/delete using synthetic data.
9. Run static checks, dependency checks, and all available tests.
10. Record exact toolchain/package versions, device model, Android version, commands, logs, artefact location, and limitations.

Do not add analytics, advertising, billing, cloud sync, or AI APIs during the spike. Do not put private journal content, secrets, signing keys, API keys, or local machine paths containing personal information in Git.

### Stage C — Feature implementation

Only after the spike passes (or its blocker is consciously accepted) and the owner approves the stack and implementation plan. Build in roadmap phases, with a working native app and tests at every stage.

## 2. Replit as a coding environment

Replit may be used to assist with the native Expo app after the planning gate. Its ability to generate a project does not establish that the project is native, that the selected packages work, or that the Android build is valid. The agent must be explicitly instructed to create a React Native + Expo mobile app, not a web app or website.

### Required first instruction for the implementation agent

> Read README.md, DESIGN.md, docs/09-DECISION-LOG.md, docs/11-RISKS-AND-OPEN-QUESTIONS.md, docs/12-PROGRESS-AND-HANDOVER.md, docs/14-STACK-COMPARISON.md, and this workflow before changing files. The user has approved implementation only if the planning gate approval is explicitly recorded. Build a native React Native + Expo mobile application targeting Android first; do not create a website, web dashboard, or web app as a substitute. Follow DESIGN.md as the sole authoritative design-token/component reference. Follow the approved product specification and architecture. If a requirement is unresolved, stop and report the question rather than inventing a product decision. Work in small, testable phases. Commit to GitHub frequently, keep main usable, run targeted tests and the full available suite, and report exact build/test evidence. Do not put secrets or private journal data in the repository.

Do not use an agent-generated preview page as proof of native app success. A QR preview may be useful for development but is not a replacement for a built Android artifact and physical-device checks.

## 3. GitHub-first handoff when Replit quota is exhausted

GitHub remains canonical regardless of which coding environment is active.

When quota is exhausted:
1. Stop the agent and ensure all intended changes are committed or saved as a patch; do not leave important work only in the Replit workspace.
2. Check `git status`, current branch, latest commit, and remote.
3. Push the current branch to GitHub and verify the commit appears remotely. If unable to push, export a patch or bundle and record the exact uncommitted state before switching environments.
4. Continue from a fresh clone or pull of the GitHub branch using a local development environment or another authorised coding agent.
5. Install dependencies from the committed lockfile; do not silently regenerate versions.
6. Run the project's documented checks and tests before modifying anything.
7. Make small commits and push them to GitHub.
8. When Replit quota returns, fetch/pull the latest GitHub state into Replit before continuing. Never push an older workspace over newer GitHub commits.
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
- Stack decision D-009:
- UX/design decision D-008:
- Accepted blockers/deferred questions:
- Reviewer notes:

Only the owner may change the approval status. Documentation completion by an assistant or coding agent does not itself authorise implementation.
