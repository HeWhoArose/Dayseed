# Progress and Handover

**Last updated:** 2026-11-13 (Canonical UI and Product-Behaviour Reconciliation — `D-233`)
- **Canonical UI and Product-Behaviour Reconciliation (`D-233`, 2026-11-13):** Following the 2026-11-13 Forensic UI Audit of the owner's installed device screenshots against `main` (`31996ee403e02ec36e03666322a488698376c34f`), executed controlled Stage B reconciliation across data cleanup, habit-recording UX, visual/theme consistency, motion accessibility, cold-launch branding, and APK build provenance:
  1. **Automatic `4c1187a` v1 starter-data cleanup (`DayseedDatabaseHelper.cleanupLegacyV1DemoDataIfNeeded`):** runs idempotently on upgrade and open (`legacy_v1_demo_cleanup_completed = "true"`), matching only the exact multi-attribute signature of the 3 historical `4c1187a` starter habits (`Gentle Movement`, `Quiet Reading`, `Drink Water`) and the starter journal entry (`"What is one small thing that went well today?"` / `"Started Dayseed today..."`), resetting `onboarding_completed = "false"` only if 0 habits remain afterward, preserving all real or user-edited habits/entries/streaks, and verifying `PRAGMA foreign_key_check` + `PRAGMA integrity_check`.
  2. **Evidence-derived habit recording (`HabitCard.kt`, `RecordSessionSheet.kt`, `CreateEditHabitScreen.kt`, `DayseedViewModel.kt`):** removed manual `🌱 Tiny` / `🌿 Usual` / `🌳 Extra` effort selection from future logging paths so `D-040` progress bands and paired quotes derive automatically from recorded evidence (`DURATION`, `QUANTITY`, `COMPLETION_ONLY`), while preserving historical `EFFORT_AWARE` records and providing a calm conversion banner in `CreateEditHabitScreen`.
  3. **Visual consistency, reduced motion, and cold-launch brand scene (`MainActivity.kt`, `Theme.kt`, `TodayScreen.kt`, `HabitsScreen.kt`, `InsightsScreen.kt`, `HabitDetailScreen.kt`, `GrowthMark.kt`, `SettingsScreen.kt`, `SettingsPanels.kt`):** hosted shared `DaypartBackgroundLayer` in `MainActivity.kt` behind transparent screen surfaces; aligned `ThemeModeOption.LIGHT` with `LightColorScheme` (`DayJournalSurface`); unified live clock ticks through `LocalDayseedLiveTime`; removed unreachable `SettingsSheet.JOURNAL_EXPORT`; exposed `reduced_motion_enabled` in Settings and honoured Android `ANIMATOR_DURATION_SCALE == 0f`; and added a once-per-cold-launch `DayseedOpeningBrandLockup` card (`dayseed_cold_launch_scene`) on `TodayScreen`.
  4. **APK build provenance (`app/build.gradle.kts`, `.github/workflows/android.yml`, `SettingsPanels.kt`):** preserved `applicationId = "com.aistudio.dayseed.wkqmfl"`, bumped `versionCode = 2` and `versionName = "1.0.1"`, added `BuildConfig.GIT_COMMIT_SHA` (`"unknown"` fallback when `.git` is absent), displayed `Dayseed 1.0.1 (build 2)` and the build SHA in `Settings → About Dayseed`, and updated CI to pass `DAYSEED_GIT_SHA` and upload `app-debug.apk`.
  5. **Executed verification (2026-11-13):** `gradle :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease` **PASSED — 268/268 tests across 19 classes** (including 12 new tests in `CanonicalUiAndBehaviourReconciliationTest`), 0 failures, 0 errors, lint `0 errors, 22 warnings`, debug APK `18,567,940` bytes, release APK `12,053,047` bytes.
- **Owner remediation plan (F-01…F-06) closeout (2026-11-11/12):** all seven workstreams executed on top of `1d882f0` — R1 evidence restoration (`4ef920c`), R2 backup importer limits (`b8a8b87`), R3 off-main-thread DB/crypto (`1b9f9c6`), R4 notification API guard + API-level tests (`096c8cd`, strengthened `f0e58b2`), R5 p4_35/p4_36 hardening (`3dd3d51`), R6 Compose-UI ViewModel-state coverage (`953b9df`), and the doc-link checker defect fix (`3b8016f`: silent-abort on zero-link files and false-BROKEN from Windows python3 path mangling — earlier "clean" checker runs only covered a truncated prefix). Final required command run (R7) at HEAD `f0e58b2`: **`BUILD SUCCESSFUL in 11m 59s`, 240/240 tests across 16 classes, 0 failures/errors, lint 0 errors / 10 warnings (unchanged baseline), debug APK 18,684,234 bytes, release APK 12,293,402 bytes (debug-signed, v2 scheme)**. All negative controls now **executed** (R4 guard-disable → API-34 test fails; R5 reschedule removal → p4_35 fails; R5 MERGE-filter removal → p4_36 fails; checker dangling-link → BROKEN). Full record: [docs/evidence/phase5/remediation-r7-final-verification-2026-11-11.md](evidence/phase5/remediation-r7-final-verification-2026-11-11.md). **Pushed:** the whole chain (`4ef920c`…`ddfef76`) is on `origin/main` at `ddfef76`, fetch-verified. **CI executed:** the first GitHub Actions run of `.github/workflows/android.yml` for `ddfef76` completed successfully (run 43094155264, 2026-11-11T20:11:28Z, conclusion `success`). Device validation stays **NOT RUN** (75 `docs/38` cases); Stage C and Stage D remain **NOT OPEN**.
- **Owner follow-up workstreams (2026-11-12):** R1 documentation reconciliation (`83037df` — README/`docs/12`/`docs/32` aligned with the R7 closeout; link checker 318 targets, 0 broken); R3 completion of the off-main-thread migration (`a61db75` — all remaining UI-reachable `runBlocking` ViewModel mutations converted to `viewModelScope.launch { withContext(ioDispatcher) }`, rapid-tap idempotency and duplicate-save guard preserved, `refreshAllSync` intentionally unchanged; new `DayseedAsyncMutationThreadingTest` with an executed negative control — restored blocking paths fail exactly the 3 predicted tests); backup-capacity validation of the 8 MiB import limit (`3f4f45c` — four deterministic synthetic datasets through the production serializer: small 57,354 B; typical 1,228,842 B; typical encrypted 1,664,282 B; heavy 3,733,342 B — all restore round-trips COMPLETED with count equality; stress 23,131,692 B = 2.76× OVER_LIMIT; boundary pinned below/at/above the limit; **no limit change made — owner decision**). Final required command at single HEAD `3f4f45c`: **`BUILD SUCCESSFUL in 12m 35s`, 256/256 tests across 18 classes, 0 failures/errors, lint 0 errors / 11 warnings (11th = test-only `kotlinx-coroutines-test` version advisory), debug APK 18,700,618 bytes, release APK 12,293,402 bytes (debug-signed)**. Full record: [docs/evidence/phase5/followup-r1-r3-backup-capacity-verification-2026-11-12.md](evidence/phase5/followup-r1-r3-backup-capacity-verification-2026-11-12.md). Device validation stays **NOT RUN**; Stage C and Stage D remain **NOT OPEN**.

**Current repository baseline:**
- **Last verified GitHub `main` HEAD before this Stage B workspace reconciliation:** `31996ee403e02ec36e03666322a488698376c34f` (`31996ee`, 2026-11-13; branding and adaptive launcher iconography update on top of the 2026-11-12 follow-up push `2ba23ef` / `3f4f45c` / `a61db75` / `83037df` and `ddfef76`). Note: the AI Studio container workspace does not carry a `.git` metadata directory, so changes made in this session are verified in the workspace and must be synced/exported to GitHub `main`.
- **Latest application-code baseline (2026-11-13 Stage B workspace):** Canonical UI and Product-Behaviour Reconciliation (`D-233`, `versionName = "1.0.1"`, `versionCode = 2`) on top of `31996ee403e02ec36e03666322a488698376c34f`, `3f4f45c`, `a61db75`, `6561588` (Phase 5 tests `p4_35`/`p4_36`), Phase 4 at `b0fc29d` and `6ae1ea1`, Phase 4A at `7bb8d80` (`D-232`), and the Phase 3 baseline `dd6501f`.
- **Documentation reconciliation:** updated 2026-11-13 for `D-233` across `docs/09`, `docs/12`, `docs/25`, `docs/28`, `docs/38`, and `README.md` (following the Phase 5 closeout commits on 2026-11-10/12).
- **Independent re-verification (2026-11-11):** the closeout sweep was repeated on a separate Windows machine from the committed wrapper at `b30efbc` — see [docs/evidence/phase5/reverify-windows-2026-11-11.md](evidence/phase5/reverify-windows-2026-11-11.md) and the [system-wide audit](41-SYSTEM-AUDIT-2026-11-11.md). The remediation pass at `269becc`/`1d882f0` reproduced 217/217 tests, 0 lint errors, 10 warnings, debug APK 19,508,532 bytes. This is a Stage B re-check, not Stage C validation.
- **Remediation pass (2026-11-11):** lint cut from **57 to 10 warnings** (0 errors); 217/217 tests re-run green; debug APK 19,508,532 bytes. Two real defects fixed: an unguarded `POST_NOTIFICATIONS` check that suppressed every reminder on API 26-32, and a PowerShell bootstrap that aborted on JDK stderr output. Icon set rebuilt as adaptive-only; CI authored but never executed *at that time* (superseded 2026-11-11/12: first workflow run executed successfully for `ddfef76`, run 43094155264). Device validation stays **BLOCKED** (no device, no AVD, no emulator binary), so the 75 `docs/38` cases remain `NOT RUN`. Details: [docs/41](41-SYSTEM-AUDIT-2026-11-11.md) §7. PowerShell bootstrap evidence re-executed and committed at `docs/evidence/phase5/powershell-bootstrap-2026-11-11.md` (initial creation, idempotent re-run, mismatched-keystore rejection).
**Status (2026-11-13):** Stage A planning is closed (`D-208`); Stage B implementation is authorised under `D-213`. **Phases 0–5 are implemented and complete**, the F-01–F-06 remediation (R1–R7), the 2026-11-12 owner follow-up workstreams, the brand logo identity (`31996ee`), and the **2026-11-13 Canonical UI and Product-Behaviour Reconciliation (`D-233`)** are complete. Latest recorded Stage B self-check (2026-11-13): **268/268 tests across 19 classes**, 0 failures/errors/skips, lint `0 errors, 22 warnings`, debug APK `18,567,940` bytes (`1.0.1`, build 2) and a **debug-signed** release APK `12,053,047` bytes. All **75** human/device cases remain **`NOT RUN`** (see `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`). **Stage C** and **Stage D** remain **NOT OPEN**.

**Historical baseline note (2026-11-03/04, superseded by the 2026-11-08 top block):** prior baseline was `main` at `8a20a249f557d2bebbded4a4bd64b4fe1178524b` (after `8c15569…` / `bf19bc11…` / `e3c3dc43…`). PR #5 was merged into `main` on 2026-11-03 as commit `e3c3dc4360626315b68319570d4fb8f59444d88a`; Phase 1 human-validation handoff was committed at `bf19bc11c37dd9a7929594871266bdff07dfc94e`; the Phase 2 baseline was committed at `8c15569a47cac7f447c49b72172d36ee7d9a041c` and `8a20a249f557d2bebbded4a4bd64b4fe1178524b`.

**Historical executed source/test status (Stage B Self-Check, 2026-11-04 — superseded by the 2026-11-08 165/165 run in the top block):** Both the merged PR #5 Phase 1 remediation tests (`t1_43_mergeDoesNotDeleteUnrelatedActiveStreakRun`, `t1_44_replaceRejectsOrphanedStreakRunAndPreservesLocalData`) and the full Phase 2 test suite including Corrections A–D and Final Remediations `F-01`–`F-06` (`Phase2ImplementationTest`, 31 JUnit methods covering `T2-01`..`T2-51` and `t2_c01`..`t2_c09`) were compiled and executed via `gradle :app:testDebugUnitTest --rerun-tasks` on `2026-11-04T16:10:15Z`–`2026-11-04T16:10:44Z` (**90/90 PASSED**, 0 failures, 0 errors, 0 skipped). Stage B self-checks do not constitute independent Stage C validation.

**Schema extension boundary:** v3 reconciliation supports the canonical app-managed schema only. It drops and recreates app triggers and recreates canonical indexes for rebuilt tables. Custom/external triggers are not preserved; custom indexes attached to rebuilt tables are not a supported extension point. Do not add out-of-band schema objects without first changing the reconciliation contract and its tests.


> **Start here for any new AI/session:** [Continuation Handover and Decision Protocol](32-CONTINUATION-HANDOVER.md). It defines source-of-truth order, D-196 decision rules, owner authority, current Stage A approval, corrected Stage B/C/D sequence, GitHub verification discipline and evidence requirements. Root [AGENTS.md](../AGENTS.md) points contributors to the same canonical handover. Do not use historical next-action lists below when they conflict with the current readiness snapshot or handover.

## Repository status

Repository: https://github.com/HeWhoArose/Dayseed  
Canonical branch: `main`. GitHub is the source of truth for planning and future implementation, regardless of the active coding environment.

## Current state

> **Note (superseded historical context):** this section's older planning-era status text predates the owner's 2026-11-01 Stage B authorisation (`D-213`) and the implemented reminder/backup remediation (`D-229`/`D-230`/`D-231`). Statements here such as "feature work is not authorised", "planning continues" and "tests/build: none run" describe the pre-Stage-B planning era and are superseded by the current status block at the top of this file (`main` pushed at `3f0e450` with D-231 follow-up implemented; Stage B implemented; full suite 165/165; physical-device validation still `NOT RUN`). Where this section conflicts with the top block or docs/09, the top block and docs/09 are current.

- Product direction and core v1 boundaries: approved product semantics through D-195 and process rule D-196 are reconciled. Owner responses O-1–O-11 were recorded on 2026-10-28; the later O-6 clarification is recorded as D-199. The schema/backup proposal is a planning baseline only; W1/N1/A2 are selected; the Growth Mark uses modified G2 “Collected moments” with a duration-total headline and G3 for Calendar/date-level or separate-page detail. The compact card defaults to the current calendar month; unit formatting, alternate period controls, mixed-unit handling and accessibility remain to review. Platform typography/system appearance and compact comeback direction were accepted; weekly reflection is 09:00 local on the configured week-start day; privacy wording is draft-only; and initial-launch ads evaluation was requested. Remaining work includes UX/accessibility and semantic consistency review, explicit owner choice on the ads evaluation, and final readiness reporting. Routine Undo/reminder/prompt details still use delegated defaults unless overridden.
- UX directions and information architecture: selected starting directions W1/N1/A2 with the owner reference image; exact tokens and final accessibility review remain open.
- Existing repository source: native Kotlin + Jetpack Compose, `SQLiteOpenHelper` at schema version 3, repository, ViewModel/StateFlow, and screens are implemented. Phase 1 is present on `main`; this Stage C remediation branch is limited to correcting independently identified data-integrity risks and strengthening validation evidence. Do not infer build/test/device success from source presence.
- Growth Mark: D-199 records the owner's clarification. G2 is the compact “Collected moments” card showing recorded duration from duration-tracked habits (current calendar month by default), in readable units; G3 is calendar/date-level detail or a separate page with underlying duration sessions. Define unit-formatting thresholds, period controls, compatible-duration aggregation, empty/zero/correction states and accessibility before any final UI decision. G1's active-date summary remains a comparison alternative, not the selected headline metric. Comeback: compact, dismissible, non-blocking direction accepted; final copy/accessibility review remains. Research is in [docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md).
- Journal archive: owner selected A2 hybrid timeline + compact date jump/calendar and local title/body search as the starting direction; task/accessibility review remains. See [docs/16-JOURNAL-ARCHIVE-RESEARCH.md](16-JOURNAL-ARCHIVE-RESEARCH.md).
- Typography, light/dark candidate palettes, contrast checks, font licensing notes, and small-screen tests documented in [docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md).
- Time-of-day theme direction (D-210): owner-supplied five-panel reference persisted at `docs/prototypes/dayseed-theme-time-of-day-reference.jpeg`; per-daypart palettes, header artwork, shared components and a code-vs-design audit are recorded in [docs/36-TIME-OF-DAY-THEME-SPECIFICATION.md](36-TIME-OF-DAY-THEME-SPECIFICATION.md).
- DESIGN.md is now specified as the sole authoritative token/component reference; candidate values remain unapproved.
- Competitor screenshot evidence for Loop Habit Tracker and Daylio is recorded in [docs/18-COMPETITOR-SCREENSHOT-AUDIT.md](18-COMPETITOR-SCREENSHOT-AUDIT.md). Public listing screenshots were inspected; current live app flows have not been tested.
- AI Studio/GitHub workflow and native-app feasibility-spike acceptance criteria are in [docs/19-AI-STUDIO-GITHUB-WORKFLOW.md](19-AI-STUDIO-GITHUB-WORKFLOW.md).
- Data/privacy baseline and portable-backup decisions are substantially documented. Android platform Auto Backup/device-transfer policy is resolved by D-186 (exclude sensitive Dayseed data by default); technical verification of the actual rules and supported transfer paths remains open. Other remaining items include final deletion/export wording and implementation-level validation.
- Monetisation: owner requested ads evaluation for initial launch; no provider/SDK or placement selected. See [docs/33-MONETISATION-AND-ADS-EVALUATION.md](33-MONETISATION-AND-ADS-EVALUATION.md).
- Existing app source is present on `main`, but the current workflow remains in planning and feature work is not authorised. Do not claim a clean build or verified implementation without evidence.
- Automated tests/build: none run in this planning continuation. Native Android app source exists on `main`; its build, tests, APK installation, and device behavior remain unverified.


## Planning decisions confirmed — 2026-10-18

The owner has confirmed the following product rules in conversation and they are recorded in [Decision Log](09-DECISION-LOG.md):

- Numeric entries are whole numbers only; an intentional zero is valid and differs from no record. Multiple entries for a habit/date sum to a daily total.
- Weekly targets are flexible, with optional preferred days that do not restrict completion.
- Schedule edits offer Apply today/Apply tomorrow while preserving existing activity records unchanged.
- Historical completion uses the target that applied at the time; comparisons against the current target are separate.
- Rest/non-scheduled days are neutral, paused periods are excluded, and scheduled non-completion is a missed opportunity rather than a judgement.
- Habit measurement mode is selected by the user: duration, quantity, or completion-only. For measured modes, progress labels are derived from actual amounts relative to the usual target, not from self-reported feelings.
- Today cards expose separate quick Complete and detailed Record actions. Quick Complete defaults to the usual target for measured habits; Record supports actual amounts, corrections, and an optional habit/date note.

The current phase remains **planning**. Continue documenting product rules, the eight-screen blueprint, navigation, data contracts, and edge cases. Do not start feature implementation. The existing native app source and preview are evidence for the later implementation-reality audit, not proof that implementation is approved or verified. The progress bands, user-selected week start, multiple journal entries, multiple numeric/duration records, and duplicate/undo direction have since been recorded as decisions. Remaining work is to formalise their edge cases in cross-screen/data contracts and tests; do not describe already-decided rules as undecided.

## Cumulative research inventory and audit — 2026-10-17

- Added [Cumulative Research Inventory and Documentation Audit](20-CUMULATIVE-RESEARCH-INVENTORY-AND-AUDIT.md) to distinguish documented hypotheses from completed evidence and approved decisions.
- Enumerated all 26 Markdown files on `main`; checked relative Markdown file targets and found no missing target files. Heading anchors, external URLs, and claim-level citations still need deeper review.
- Confirmed the competitor research document is a plan, not a completed market study; public listing screenshots are not hands-on competitor tests.
- The audit prioritises remaining day-record edge cases, date/time policy, backup/restore and platform backup verification, reminder delivery edge cases, representative usability validation, and the owner approval gate.
- This was a structural/status pass, not a complete line-by-line source and contradiction audit. No app code was created and no implementation is authorised.

## Latest research package

New research documents:
1. [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
2. [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md)
3. [Typography, Colour, and Accessibility Research](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
4. [Competitor Screenshot Audit](18-COMPETITOR-SCREENSHOT-AUDIT.md)
5. [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md)

Updated references: README.md, DESIGN.md, docs/02-UX-AND-DESIGN.md, docs/08-IMPLEMENTATION-ROADMAP.md, and docs/09-DECISION-LOG.md.

## Next actions

Use [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) as the current checklist; it supersedes older next-action lists in this historical handover where they describe resolved decisions as open.

1. Present the reconciled written schema in docs 03 and 30 for owner review, especially portable-backup format/schema compatibility and restore conflicts that cannot safely be kept both. Do not inspect or change the live database during this planning review.
2. Keep the D-196 delegated defaults for guarded 8-second Undo, single-entry Delete+Undo, shared reminder coordination, local-only timing-learning safeguards, and retrospective journal-prompt cadence. Include one explicit owner choice in the grouped review for the weekly reflection's local clock time; do not inherit the per-habit 9:00 a.m. default.
3. Review the rendered boards `docs/prototypes/dayseed-today-variants.svg` and `docs/prototypes/dayseed-navigation-archive-variants.svg` against the task/accessibility scorecard; record design choices or explicit deferrals for Today hierarchy, navigation, archive, Growth Mark, typography/colour/theme and comeback presentation.
4. Close the remaining gaps in [28-REQUIREMENT-TRACEABILITY.md](28-REQUIREMENT-TRACEABILITY.md), then finish the semantic claim/link/anchor review without treating plans or prototypes as test evidence.
5. Present the genuine owner choices and safe deferrals together; keep the planning gate closed until the owner explicitly approves it.
6. Only after separate authorisation, run the bounded native technical-validation stage. Do not treat repository source presence as proof of a clean build or verified behaviour.

## Rules for the next AI or contributor

- Read README.md, DESIGN.md, docs/09-DECISION-LOG.md, docs/11-RISKS-AND-OPEN-QUESTIONS.md, this handover, and docs/19-AI-STUDIO-GITHUB-WORKFLOW.md first.
- AI Studio is the active AI-assisted development environment; do not create a website as a substitute for the Android app. Do not start implementation until the owner explicitly approves the planning gate.
- Treat approved, proposed, open, deferred, and verified as distinct states.
- DESIGN.md is the sole authoritative design-token/component reference.
- Do not invent research sources, user research results, competitor capabilities, SDK compatibility, tests, or build status.
- Do not start app implementation until the planning gate is explicitly approved.
- GitHub is canonical. Before continuing after an AI Studio session/context reset, refresh from the latest committed GitHub state and reconcile local changes before pushing.
- Keep journal privacy and offline operation as core constraints.
- For changes, update cross-referenced documents and run a consistency audit.
- At each handover, state exactly what changed, evidence, unresolved issues, and next action.

## Verification status

The repository tree confirms a native Kotlin/Compose Android project and application source files exist. Source inspection identified a version-1 SQLiteOpenHelper database and placeholder `onUpgrade`; no Gradle build, test suite, APK install, or physical-device run was performed during this documentation audit. Older statements that app implementation has not started are stale. See `docs/22-IMPLEMENTATION-REALITY-AUDIT.md`.

## Research follow-up — 2026-10-17

The head-to-head comparison is in [docs/14-STACK-COMPARISON.md](14-STACK-COMPARISON.md). The five new research and workflow documents are linked above. D-009 (native Kotlin + Jetpack Compose) is now approved. D-008 (visual direction), D-016 (Growth Mark), D-017 (comeback), D-018 (journal archive), D-019 (typography/colour), D-021 (competitor evidence), and D-022 (AI Studio/GitHub workflow) remain open or proposed. No design or implementation approval has been recorded.


## Additional planning decisions confirmed — 2026-10-19

- Users may choose the first day of their week; weekly goals, summaries, and calendar displays must use that choice consistently.
- Multiple duration sessions for the same habit/date are summed into a daily duration total, while individual sessions remain inspectable and editable. Different habits keep separate totals and units.
- Support both an optional habit/date note as the default and optional notes on individual entries. These are separate from the general daily journal.
- Provide a way to undo a recent accidental action, correct/delete entries later, and prevent accidental duplicate quick-completion taps without blocking legitimate repeat sessions.
- Discreet XP/rank near the profile is an idea to research, not an approved feature. Online leaderboards and viewing other people's streaks remain deferred pending explicit approval and privacy/safety/architecture research.

- Daily duration/quantity outcomes distinguish Partial, Goal met, and Above goal while preserving actual amounts. Weekly review distinguishes daily outcomes from flexible weekly-target progress. A short optional, nonjudgmental encouragement sequence after partial progress is approved in direction only; cadence, trigger, copy, and opt-out remain open.

- The owner prefers both in-app encouragement and optional phone notifications. Plan outcome-aware messages for partial, goal-met, above-goal, and still-unrecorded scheduled habits, plus a later-day check-in. Explore learning a user's common completion times over repeated observations, with manual controls, quiet hours, rate limits, opt-out, and privacy safeguards. Learning thresholds, timing defaults, and reminder coordination remain open; this is planning only.

- Reminder setup decision: ask once during onboarding whether reminders are wanted. Yes enables reminders; No creates none. Each habit creation/edit form shows a reminder-time control defaulted to 9:00 a.m. local time, and that habit uses the displayed time unless changed. For habits on system-managed timing, Dayseed may auto-adjust after 14 days of sufficient completion observations, then observe the new time for 28 further days to validate/solidify the adjustment. If the routine changes again, it may auto-adjust again while system-managed timing remains enabled. A manually edited time is protected: later pattern changes may be suggested but not applied automatically unless the user explicitly returns the habit to automatic timing. Option B for follow-up is selected: one optional evening check-in if still unrecorded. Open: evidence thresholds, evidence thresholds, 28-day validation and rollback rules, opt-out and per-habit/global controls, quiet hours, Android permission, and notification limits.

Continue in planning. Do not implement these ideas or start the architecture audit until the remaining product decisions, screen blueprint, cross-screen contracts, and planning consistency review are complete.

## Implementation reality audit (2026-10-18)

The current repository contains a native Kotlin/Jetpack Compose Android app and SQLite persistence code; the older planning-only status is stale. Source presence is not build/test verification. Read [Implementation Reality Audit](22-IMPLEMENTATION-REALITY-AUDIT.md) before changing architecture or adding features. Gradle build, tests, APK install, and device validation remain unverified by this audit. AI Studio is the active AI-assisted development environment; GitHub `main` remains canonical.


## Planning reconciliation — 2026-10-26

- Added `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` to centralise approved decisions, delegated defaults, owner-only decisions, and cross-document conflicts. Commit: `e721ed18ba50fde55169ec7057eb24e0f791a204`.
- Updated `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md` to align with the owner-approved native Kotlin + Jetpack Compose stack. Commit: `fdeb80fbbc96b220dc8ab0cbd79c649789b8c590`.
- Corrected the historical stack comparison's link to `docs/22-IMPLEMENTATION-REALITY-AUDIT.md`. Commit: `1ec1ca523ce8341748783cf8b85dad3181dfc6e1`.
- **Historical note (superseded on 2026-10-27):** an earlier reconciliation recorded 9:00 a.m. as the approved weekly reflection time. That conflated it with the separate per-habit default. The current rule is in D-045 and docs 23/25/27/31: weekly reflection trigger/catch-up rules remain recorded, but the weekly clock time requires separate owner confirmation.
- Re-fetched and verified each changed file after writing.
- No application code was changed; no implementation audit or build/test run was started.
- D-009 is resolved: continue the existing native Kotlin + Jetpack Compose project. R-004 is reconciled: initial v1 includes readable journal export and user-controlled portable local backup/restore, while online backup/cross-device sync remain future premium direction outside v1 and require a separate explicit approval gate.


## D-009 stack decision — 2026-10-26

- Owner decision: continue the existing native Kotlin + Jetpack Compose Android project. The active stack direction is approved; do not scaffold a second app or rewrite to React Native/Expo without a new explicit decision.
- Updated the decision log, technical architecture, AI Studio workflow, and decision register to align with D-009.
- This approval does not authorise feature implementation or an implementation audit. Build/test/APK/device status remains unverified. Continue planning and wait for explicit approval of the planning gate before technical validation.
- R-004 cloud/premium scope conflict is reconciled: initial v1 remains offline/account-free for core use, includes readable journal export and local backup/restore, and excludes online backup/cross-device sync. The future premium direction is recorded but not authorised for implementation.


## Cloud/premium scope reconciliation — 2026-10-26

- Reconciled R-004 across the project charter, product specification, technical architecture, data/privacy, monetisation, testing, roadmap, decision log, workflow, risk register, decision register, and this handover.
- Initial v1: core habit/journal use is offline and account-free; include readable journal export and user-controlled portable local backup/restore. Exclude Dayseed-managed accounts, online backup, and cross-device sync.
- Future direction: online backup/cross-device sync may be premium later, but no service, price, purchase provider, or monetisation model is approved. A later release needs a separate explicit product, privacy/security, consent, data-flow, recovery/conflict, retention/deletion, and monetisation gate.
- Clarified that Dayseed's portable backup is distinct from Android/platform backup. A user-selected Android file provider may itself sync a file online; privacy copy must disclose that possibility without implying Dayseed provides that service.
- Verified after writes: charter `8ee2b1ca9be10e2af4331bd98e6db00c4abff514`; product spec `895612ca99de2eb0f1c8038cb911c219a10bd555`; architecture `f4011b8dbadb2c6f9254d3cc864b42ff8123bce8`; privacy `ac65182f95c04ae4a2db3eae0a4890b2736845be`; monetisation `8212243adbf76cace936fb3da793587bd2e8ec4c`; testing `a79d684b81a67a73de0852a7945a45dc0207fcca`; roadmap `c5483589d06c6ef82597a80208c83d98251b227f`; workflow `26b485aff636389801a71f32409a23af89d4b3dd`; decision log latest backup/privacy clarification `beb7eb06b291696ecf9bc89ae7b00c284317448e`; decision register `bc91e23f4349e060e2e3ce40ab7befedba046d63`; risk register `7c4279caadb8e0714d32ab7950081dd9b158516a`.
- Planning only: no app code changed; no build, tests, APK install, device validation, or implementation audit run.
- Next: continue the remaining planning reconciliation and open UX/data decisions, then perform a planning completeness review. Wait for the owner's explicit approval before technical validation or implementation.


## Planning reconciliation continuation — 2026-10-26

- Normalised the mood-insight decision rows D-083–D-090 in `docs/09-DECISION-LOG.md` to the standard column semantics and removed a duplicate D-090 row. No later decision IDs were renumbered; the broader audit of decision rows after D-090 remains open. Commit: `c9292807c1eac245da95bd14c39d50249073455a`.
- Updated `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` to record that R-003 is partially corrected, with the remaining column-semantics audit explicitly tracked. Commit: `b6afc0768842aead18be70f7c178fbf8422c8b56`.
- Reconciled `docs/11-RISKS-AND-OPEN-QUESTIONS.md` with existing approved decisions so resolved items (schedule direction, multiple entries, backup scope, and native stack) are no longer presented as wholly unanswered. Remaining edge cases stay open. Commit: `ae8161fb733dca6870d6de57ae1da2a4027d29db`.
- Re-fetched and verified each updated document after writing. No app code changed. No build, tests, APK install, device validation, or implementation audit was run.
- Next: finish the broader decision-log row audit, align the handover and roadmap to the reconciled state, then continue the planning completeness review. Do not cross the implementation gate without explicit owner approval.


## Decision-log structure audit completed — 2026-10-26

- Completed the broader decision-log structure pass. Normalised D-106–D-185 to the standard five-column schema, preserving the chosen behaviour in Current position and retaining rationale/acceptance criteria plus decision date in Next action. D-083–D-090 were already normalised in the preceding pass.
- Re-fetched `docs/09-DECISION-LOG.md` after the update and verified every Markdown table row has the expected five fields (six pipe characters including row boundaries), with exactly one D-090 row. Commit: `f96cd3ede7b618ab262efcf64828d02944021d4d`.
- Updated the decision register to mark R-003 corrected and the formatting checklist complete; re-fetched and verified it. Commit: `c4cd44111703e8a548aeb43c96a96703b0c1dc0b`.
- This is a documentation-format correction only: decision IDs were not renumbered, and no product decision was changed. No app code changed; no build, tests, APK installation, device validation, or implementation audit was run.
- Next planning task: reconcile the implementation roadmap and screen/data contracts with the latest approved decisions, then perform the planning completeness review. Owner approval is still required before technical validation or implementation.


## Architecture and quality-plan reconciliation — 2026-10-26

- Updated `docs/03-TECHNICAL-ARCHITECTURE.md` to remove the incorrect one-record-per-habit/date uniqueness rule and the claim that journal-entry multiplicity is undecided. It now reflects approved multiple numeric/duration entries and multiple independent journal entries, while explicitly leaving exact schema, idempotency, date policy, and migration details open. Commit: `a7d9cd86fa461af1809754e8bcd9473380532898`.
- Updated `docs/08-IMPLEMENTATION-ROADMAP.md` so monetisation is not part of initial v1 unless a separate explicit owner decision approves it. Commit: `1c4f0b3762ca4fc2d3e31c406390a9ee8ce985b8`.
- Reordered and expanded the regression checklist in `docs/07-TESTING-AND-QUALITY.md` to cover multi-entry records, independent journal entries, zero/no-record semantics, backup/restore safety, privacy boundaries, and migrations. Commit: `9867c42ad061b8e620832ba2a60514bbd5cd71ab`.
- Added R-008/R-009 to the decision register and split the data-model checklist into completed alignment versus remaining schema/date/migration/platform-backup work. Commit: `5205d47ec20c6518a9258c89bc081c5f811c70b8`.
- Re-fetched and verified every changed document after writing. These are planning-document changes only. No app code, build, tests, APK installation, device validation, or implementation audit was performed.
- Next: reconcile remaining duplicate requirement identifiers and stale product-specification wording, then review screen/data contracts and complete the planning completeness pass. The planning gate remains closed pending explicit owner approval.


## Product requirement identifier cleanup — 2026-10-26

- Re-fetched the product specification and decision log and checked requirement/decision IDs for duplicates. The decision log now has 179 decision rows with no duplicate D-IDs.
- Renamed the second H4 section in `docs/01-PRODUCT-SPECIFICATION.md` to H4b and clarified that retry idempotency and legitimate repeated entries are approved, while the exact undo window/recovery interaction remains open. Re-fetched and verified the product spec; requirement-heading IDs are unique. Commit: `89684eb5e35aef184d711aa80a6313f2b366c7b3`.
- Added R-010 to the decision register and verified it. Commit: `75c5ddb128d4754e4a2c24657af5b8e3a2ce9d87`.
- Documentation only; no app code, build, tests, APK installation, device validation, or implementation audit was performed.


## Date/time and cross-screen contract pass — 2026-10-26

- Added a provisional shared date/time contract to `docs/03-TECHNICAL-ARCHITECTURE.md`: separate local date keys from event instants, preserve historical date keys across travel/time-zone changes, use the selected week-start consistently, handle backdated records explicitly, avoid fixed-24-hour calendar arithmetic, and deduplicate reminder/comeback occurrences. Trigger-specific approved decisions D-146–D-160 remain authoritative. Commit: `41f026348e9e4b3a0068f6960c86fab96127ff35`.
- Updated `docs/04-DATA-AND-PRIVACY.md` and the decision register to require an explicit owner choice on Android Auto Backup/device-transfer inclusion of sensitive data. This trade-off was deliberately not silently decided: inclusion can ease recovery but may create additional copies; exclusion can reduce unintended copies but may impair device migration. Commits: `3c5a20b8f2cdc381f96a6b1996bf6fec7525fafe`, `ec39a2179364b6a7274f6cca3fa02dfcea247df7`.
- Created `docs/26-SCREEN-AND-DATA-CONTRACTS.md` covering Today, record detail/edit, journal, history/calendar, insights, comeback, reminders, backup/restore, privacy, shared data invariants, failure states, and remaining owner/technical gates. Re-fetched and verified the full file. Commit: `0a2e844095b9f58714872daee1e8d68035bff48f`.
- Added R-011 and R-012 to the reconciliation register for the date/time contract and cross-screen contract; re-fetched and verified. Commit: `5c7f02be65b4f03f72283de22b1e271d2f99d528`.
- No app code changed. No build, automated tests, APK installation, physical-device validation, or implementation audit was run.
- Next: complete cross-document review of reminder/privacy/backup details, verify requirement-to-test traceability and remaining stale wording, then produce the planning completeness review. The implementation gate remains closed.


## Cross-document backup consistency pass — 2026-10-26

- Found and corrected a stale paragraph in `docs/24-COMEBACK-EXPERIENCE.md` that still described portable-backup coverage and restore conflicts as undecided despite approved D-091–D-145 decisions. It now points to the approved backup/restore contract and explicitly keeps Android platform-backup inclusion as a separate owner decision. Re-fetched and verified. Commit: `a2e1b2d215e3fbdc6878e28244883e19a6e9fec0`.
- Added R-013 to `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` and re-fetched/verified. Commit: `a4502873fb65f429da5bafab5aa2b357c0b2fc56`.
- A follow-up edit to the persistence-test wording in `docs/07-TESTING-AND-QUALITY.md` was not applied because the repository update was rejected; that one line still needs correction to specify identity/idempotency constraints without a blanket unique habit/date constraint. No claim is made that it changed.
- No app code changed. No build, automated tests, APK installation, device validation, or implementation audit was run.

- Updated Phase 3 in `docs/08-IMPLEMENTATION-ROADMAP.md` to include the cross-screen contract and require the Android Auto Backup/device-transfer owner decision to be explicitly resolved or consciously deferred with a safe documented rule. Re-fetched and verified. Commit: `5afee35c5803e0ae1a071e04e7fd6a681fe69b7d`.


## Android platform-backup policy resolved — 2026-10-26

- Owner decision D-186 is recorded in `docs/09-DECISION-LOG.md`: exclude sensitive Dayseed app data from Android platform Auto Backup/device transfer by default in initial v1. Users should use the clearly explained Dayseed portable backup/restore flow for phone migration. This is distinct from the app's opt-in file-based Auto Backup to a user-selected destination.
- Updated and re-fetched `docs/04-DATA-AND-PRIVACY.md`, `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/01-PRODUCT-SPECIFICATION.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/08-IMPLEMENTATION-ROADMAP.md`, and `docs/24-COMEBACK-EXPERIENCE.md` to remove the unresolved owner-choice wording and carry D-186 through privacy, UX, architecture, and planning gates.
- Updated `docs/07-TESTING-AND-QUALITY.md` with a specific acceptance scenario to verify manifest/data-extraction rules and representative supported OS/device-transfer paths separately from Dayseed portable backup. Re-fetched again after an initially stale read; current file contains the updated scenario. These are planned tests, not executed tests.
- Verified changed file contents after writes. Commits: decision log `54867ec87069e7acbb33688a5df28e2af546ab2b`; privacy `5a116f73cf362d1136dbb35e4364da966238fcb2`; architecture `24fce3481bc0cf82f40f18ac31c2c4830a9133a5`; screen/data contract `2fc5aed98ae3f60d45ec057b8524da9e36e79c3f`; product spec `e68a34bea0d2cdb53722cf2e4378f4a9cb830b83`; register `64c0ac0479fb10bae8b4347af6d0fcd708a44b5b`; roadmap `941c035ad6023be86901379bebab2836a55b5d79`; comeback document `501e5551962bfb8472c065525ec158eba133db28`; testing plan `80d5bd8d2d0752d48423bbbbc87be0ae7b1f41e0`.
- No app code changed. No build, automated tests, APK installation, physical-device validation, or implementation audit was run. Technical verification of the actual Android backup configuration remains pending and must wait for the authorised validation gate.

## Date/history policy approved — 2026-10-26

- Owner approved D-187: preserve saved local-date keys across timezone travel; handle manual clock/timezone changes conservatively without silently rewriting records, fabricating elapsed time, or duplicating logical events; apply target/schedule changes prospectively while preserving historical interpretation; archive habits by default and retain accessible history; keep permanent deletion separate and explicit; require explicit date selection for backdated records.
- Recorded D-187 in `docs/09-DECISION-LOG.md` and reconciled `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/04-DATA-AND-PRIVACY.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, `docs/01-PRODUCT-SPECIFICATION.md`, `docs/08-IMPLEMENTATION-ROADMAP.md`, and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`.
- Re-fetched each changed document after writing and verified the decision/policy text and the corrected persistence-test wording.
- Planning only. No app features were implemented; no build, automated tests, APK installation, device validation, or implementation audit was started. Schema/index/transaction/idempotency design, migration safety, and clock/timezone/reminder edge-case evidence remain unverified and must wait for explicit technical-validation authorisation.

## Proposed schema decision — separate activity and daily outcome storage

- Owner approved the partial schema choice to store quantity/duration sessions in `habit_entries` and completion-only/effort-aware selections in `habit_day_outcomes` (D-189). Numeric sessions remain individually correctable and multiple distinct entries per habit/date are allowed; explicit zero is a recorded numeric entry. The domain must validate each write against the effective-dated tracking mode and reject contradictory numeric/non-numeric states.
- Updated the proposed model and affected architecture, screen/data contract, planned acceptance coverage, risk/open-question status, and decision register. The complete schema remains proposed; D-192 resolves note cardinality by approving both optional per-entry notes and one optional habit/date note. D-190 approves stable UUID identity and nullable unique idempotency keys, with key reuse limited to retries of the same logical submission. D-191 subsequently approved the structured schedule fields; stable weekday-mask encoding and technical validation remain gated.
- Each file was fetched from `main` before its update and re-fetched immediately after. Planning only: no implementation, build, tests, APK/device validation, or implementation audit.

## Proposed v1 data model — 2026-10-26

- Added a concrete candidate relational schema to `docs/03-TECHNICAL-ARCHITECTURE.md`, explicitly marked proposed and awaiting owner review. Candidate entities cover stable habit identity, effective-dated habit configuration, numeric/duration entries, daily non-numeric outcomes, date-level habit notes, saved journal entries, separate drafts, typed app settings, and reminder-delivery deduplication.
- Updated `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, and `docs/11-RISKS-AND-OPEN-QUESTIONS.md` to reference the proposal, state the key invariants, and list acceptance coverage that has not been executed. The decision register records owner approval as pending.
- The effective-dated configuration snapshot choice remains approved as a partial schema decision. D-189 now also approves separate numeric/duration entries and per-day completion-only/effort-aware outcomes, with mode validation and contradictory-state rejection. The complete schema remains proposed, not approved for implementation. D-192 resolves note cardinality: both optional per-entry notes and one optional habit/date note are supported. D-190 approves stable UUIDs plus nullable unique idempotency keys; D-191 approves the structured schedule fields aligned with the existing schedule decisions. D-188 resolves explicit-miss replacement by removing user-entered miss outcomes and deriving missed status from schedule plus absence of qualifying activity.
- Fetched each changed file before updating and re-fetched after each update; post-write checks confirmed the expected proposed/pending status.
- Planning only: no application code, schema, migration, build, test, APK, or device behaviour was changed or validated. Do not implement or migrate until the owner approves the model and separately authorises technical validation.


## Missed-day and backdated activity policy approved — 2026-10-26

- Owner approved D-188: do not offer a manual “mark as missed” control or store a user-entered miss outcome. A scheduled day becomes derived-missed after it has passed without qualifying activity recorded; rest/non-scheduled days remain neutral and paused periods are excluded.
- Allow genuine habit activity to be explicitly backdated to its actual local date. Recalculate that date's result and dependent summaries from the activity and the configuration effective on that date. A journal memo can explain why activity was not recorded, but a memo alone must never create, remove, or change habit status.
- Updated `docs/09-DECISION-LOG.md`, `docs/01-PRODUCT-SPECIFICATION.md`, `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, `docs/11-RISKS-AND-OPEN-QUESTIONS.md`, and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`. Each file was fetched before its update and re-fetched afterward; expected policy text was checked. One intermediate screen-contract verification exposed stale wording, which was corrected and re-verified.
- The relational schema remains proposed and owner approval is still pending for remaining model choices other than note cardinality, which D-192 resolves. D-191 approved the structured schedule fields; stable weekday-mask encoding and technical validation remain gated. This is planning documentation only: no app code, schema, migration, build, tests, APK, device validation, or implementation audit was run or changed.

- Reconciliation follow-up: marked D-183 as superseded in part by D-188 so its older suggestion of a user-entered explicit miss cannot conflict with the approved derived-missed policy. Updated regression and screen contracts to distinguish no record, explicit zero, and derived missed status, and specified that the calendar/history visibly labels past scheduled days with no qualifying activity. Re-fetched and verified each changed file after its update.


## Proposed schema decision — stable IDs and retry idempotency (D-190)

- Owner approved stable UUID identity for habits, entries, journal entries, and drafts; edits preserve identity.
- A retry of the same logical activity submission reuses the same idempotency key. A distinct intentional entry uses a new key. Non-null keys must be unique; null is permitted where retry protection is not applicable. Do not deduplicate based only on matching values or timestamps.
- Updated the architecture, screen/data contract, planned acceptance coverage, risk/open-question status, and decision register. Schema remains proposed; habit/date note cardinality is resolved by D-192. D-191 approves the structured schedule fields; the stable weekday-mask encoding and technical validation remain gated. Planning only; no code, builds, tests, APK/device validation, or implementation audit.


## Proposed schema decision — structured schedule fields (D-191)

- Owner approved explicit schedule representation: `DAILY`, `SELECTED_WEEKDAYS`, or `FLEXIBLE_WEEKLY_TARGET`; explicit target period (`DAY` or `WEEK`); a selected-weekday mask for selected-day schedules; and optional preferred weekdays for flexible weekly targets as guidance only.
- `DAILY` and `SELECTED_WEEKDAYS` schedules use a day target; `FLEXIBLE_WEEKLY_TARGET` uses a week target. Numeric/duration weekly goals sum compatible units; completion-only/effort-aware weekly goals count qualifying completed dates. The saved app-level week-start preference governs all week calculations. Locale informs the initial week-start default only.
- Updated the proposed architecture, screen/data contracts, planned acceptance coverage, risk/open-question status and decision register. Schema remains proposed; D-192 resolves the date-level note decision, while separate technical validation and stable weekday-mask encoding remain gated. Planning only; no code, builds, tests, APK/device validation or implementation audit.

## Proposed schema consistency review — 2026-10-26

- Reviewed the proposed relational schema against the current screen/data contract, quality plan, risk register, decision register, handover, and decision log.
- Corrected the schedule wording so `DAILY` means every local calendar day, and both `DAILY` and `SELECTED_WEEKDAYS` use `target_period = DAY`; `FLEXIBLE_WEEKLY_TARGET` uses `WEEK`. Clarified that an absent numeric record is distinct from an explicit zero and removed stale wording that implied the recently decided schema choices were still awaiting individual owner decisions.
- Added R-015 to track remaining proposal questions: stable weekday-mask encoding; repeated Apply-today edits when a version effective today already exists; and handling habit-linked reminder-ledger rows and pending OS notifications on permanent deletion. The initial review also noted the explicit skipped-action question, later resolved by D-193.
- Re-fetched each changed document immediately after its update and verified the intended wording. The schema is still proposed, not approved for implementation; the weekday-mask encoding and identified behavioral questions remain unresolved. These are planning-document corrections only.
- No app code or database schema changed. No build, automated tests, APK installation, device validation, or implementation audit was run. Do not begin technical validation or implementation without the owner's separate explicit authorisation.

## Habit-day Skip policy approved — 2026-10-27

- Owner approved D-193: v1 has no explicit “Skip this day” action for a habit and no persisted skipped habit-day outcome.
- A scheduled day without qualifying activity becomes derived-missed only after the scheduled local day passes. Rest/non-scheduled days remain neutral, paused periods are excluded, genuine activity may be explicitly backdated to its actual date, and journal memos do not change habit status. Optional skipping of journal prompts and comeback reflections is unaffected.
- Marked D-006 as superseded in part by D-193 so its older suggestion that skipped habit-days need a separate state no longer conflicts. Reconciled the product specification, proposed schema, screen/data contract, quality plan, risk register, and decision register; test coverage is planned, not executed.
- The complete data schema remains proposed. Following D-195, the weekday-mask encoding policy is approved; technical validation of the mask constraints and reminder-ledger/pending-notification cleanup on permanent habit deletion remain open. No app code, build, tests, APK/device validation, or implementation audit was run. Do not begin technical validation or implementation without separate explicit authorisation.

## Repeated Apply-today behavior approved — 2026-10-27

- Owner approved D-194: the first Apply-today change creates today's configuration version. A further same-day edit replaces today's version only while no qualifying habit activity has been recorded today; after activity exists, preserve today's configuration and activity and offer Apply tomorrow. Cancel changes nothing; Apply tomorrow creates or updates tomorrow's not-yet-effective version.
- Reconciled the decision log, product specification, proposed architecture, screen/data contract, planned quality coverage, risk register, and decision register. The repeated Apply-today owner-policy question is resolved; D-195 now resolves the weekday-mask encoding policy. Technical validation and cleanup of habit-linked reminder-ledger rows/pending OS notifications on permanent deletion remain open.
- The relational schema remains proposed, not approved for implementation. Acceptance cases are planned, not executed. No code, build, automated tests, APK/device validation, or implementation audit was performed; technical validation and implementation remain gated by separate explicit authorisation.

## Weekday-mask encoding approved — 2026-10-27

- Owner approved D-195: encode weekdays with a locale-independent 7-bit integer mask, Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. Combine chosen days by bitwise OR. `SELECTED_WEEKDAYS` requires a valid non-empty mask; `DAILY` and `FLEXIBLE_WEEKLY_TARGET` do not require a scheduled-day mask; flexible weekly preferred weekdays are optional guidance only. Unknown bits and invalid schedule/mask combinations must be rejected.
- Updated the decision log, proposed architecture, screen/data contract, planned acceptance coverage, risk register, and decision register. Re-fetched each updated file and verified the intended content. The encoding policy is resolved; technical validation remains pending.
- The complete schema remains proposed, and acceptance cases remain planned, not executed. Permanent-deletion notification cleanup now has a delegated planning default in docs 03/30/26/07; remaining work is owner review of the complete schema and later authorised verification of transaction, cancellation-failure, startup-retry, and stale-worker guard behaviour. No code, build, tests, APK/device validation, or implementation audit was performed; any technical validation or implementation still requires separate explicit authorisation.


## Current readiness snapshot — 2026-10-27

See [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) for the current workstream matrix and P0/P1 checklist. It supersedes older “next action” lists in this historical handover where they describe already-resolved decisions as open.

- D-195 weekday-mask encoding is approved and reconciled: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. The mapping is locale-independent; valid/non-empty and schedule-kind constraints are planned for validation, not tested.
- The formerly open permanent-habit-deletion notification question now has a delegated planning default: queue opaque notification IDs in a cancellation outbox, delete habit-owned data transactionally, preserve independent journal entries/global reminders, cancel OS notifications after the database commit, clear the outbox only on success, retry after interruption, and guard workers against deleted/inactive habits. This is unimplemented/unverified and may be overridden before schema approval.
- Earlier Expo-first text in the Android platform research and Expo-specific font-packaging wording were corrected to align with D-009 and native Kotlin/Compose.
- Clarified in the product specification that complete schema design is a planning deliverable, while runtime/technical verification (build, real database migration, Android backup rules, device behaviour) waits for separate owner authorisation after the planning gate.
- Created `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/29-UX-DESIGN-APPROVAL-PACK.md`, `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`, and `docs/31-OWNER-PLANNING-DECISION-BATCH.md`. Expanded the architecture proposal with lifecycle refinements, restore-session/conflict concepts, a journal-habit link and notification-cancellation outbox; these remain proposed, not owner-approved.
- Existing repository source is native Kotlin/Compose with SQLite persistence, according to the earlier read-only audit. Build, tests, APK installation, physical-device behaviour and safe migration remain unverified. Do not state that the runtime has passed.
- Documentation updates were fetched before each write and re-fetched for verification. No application code or database was changed; no build, test, migration, APK installation, or device validation was run during this continuation.

### Next planning actions

1. **Written schema reconciliation is complete.** Present `docs/03-TECHNICAL-ARCHITECTURE.md` and `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md` together for owner review of the proposed data model, portable format/schema compatibility boundary, and restore-conflict choices where “Keep both” is unsafe. D-197's fail-closed migration rule remains the delegated safety default. Do not inspect or migrate the live database during this planning review.
2. Use `docs/31-OWNER-PLANNING-DECISION-BATCH.md` for a single grouped review of genuinely consequential choices: schema/restore compatibility; visual and navigation choices after prototype review; final privacy/export/delete wording; whether monetisation remains excluded from initial v1 (recommended default); and explicit planning-gate approval. Undo, reminder coordination and retrospective-prompt details are delegated under D-196 unless the owner wants to override them.
3. Review all three rendered planning boards—`docs/prototypes/dayseed-today-variants.svg`, `docs/prototypes/dayseed-navigation-archive-variants.svg`, and `docs/prototypes/dayseed-growth-mark-variants.svg`—against the same task and accessibility scorecard. Record owner choices or explicit deferrals for Today hierarchy, navigation, archive, Growth Mark, typography/colour/theme, and comeback presentation. They are planning artefacts, not approved UI or usability evidence.
4. Close the remaining `docs/28-REQUIREMENT-TRACEABILITY.md` gaps, especially accepted schema failure/recovery cases, privacy wording, and UX/accessibility coverage. The Mood Insights fixture has been reviewed for its expected counts and boundaries; all acceptance cases remain planned, not executed.
5. Complete the final semantic-claim/duplicate/conflict/supported-evidence review and present the consolidated planning package. Keep the planning gate closed until the owner explicitly approves it in `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md`.
6. Only after separate explicit authorisation, begin the bounded native technical-validation stage. Feature implementation remains a later, separate stage.


## Latest planning continuation — 2026-10-27

- Reconfirmed D-195 is already approved on `main`; the seven-bit weekday mask is not a new unresolved owner choice.
- Drafted comparable text wireframes in `docs/29-UX-DESIGN-APPROVAL-PACK.md`: three Today hierarchies, two navigation variants and three journal archive patterns. No rendered prototype, user study or design approval is claimed.
- Documented delegated reminder safety defaults in docs 09/23/05/26/28 under D-196: editable 21:00–08:00 local quiet hours, shared three-notifications/day cap, collision/late-delivery revalidation, local-only personalisation evidence/rollback and capped optional-tip sequence. The defaults are owner-overridable, not separate approval blockers; OS scheduling remains unverified.
- **Historical note (superseded by D-196):** the 8-second single-mutation Undo and selected numeric-entry Delete+Undo interaction were previously listed as owner decisions. They are now delegated planning defaults in D-032/docs 01/03/26/07/28/30; tests remain planned, and the owner may override these details in the grouped review.
- Reconciled traceability IDs: AC-022 remains restore conflict/interruption recovery; AC-023 is the Undo proposal. Removed the duplicate AC-022 row added during reconciliation and re-fetched the matrix to verify the IDs and section boundary.
- The schema closure document 30 exists and contains proposed table/index constraints, transactions, restore sessions/conflicts, notification cancellation outbox, backup mapping and migration design. It must still be reconciled and reviewed with the canonical architecture and owner-approved rules.
- No app code or live database was modified. No build, automated test, migration, APK installation, device validation or implementation audit was run. All acceptance cases remain planned, not executed. The planning gate remains not approved.

- **Historical note (superseded by D-196):** the D-042 retrospective prompt cadence was previously listed as awaiting owner acceptance. It is now a delegated default: one non-blocking prompt for the most recent fully completed local date without a saved entry; no backlog/OS notification; one per date; reuse same-date draft; do not stack with comeback. AC-024 remains a planned test, not executed.
- Created `docs/31-OWNER-PLANNING-DECISION-BATCH.md` and linked it from the planning review/register. It consolidates D-195 and other settled policies, the proposed schema review, reminder/retrospective-prompt/Undo proposals, design review topics, safe v1 deferrals, and the separate post-gate validation boundary. The document records no new owner approval.
- Added a deterministic synthetic Mood Insights dataset and expected results for AC-009 in `docs/07-TESTING-AND-QUALITY.md`; linked expected behavior into `docs/28-REQUIREMENT-TRACEABILITY.md` and updated docs 27/31. This strengthens the plan only; no tests were executed.
- Added deterministic progress-band fixtures for AC-002 in docs 01/07/28/27. Classification uses the exact actual-to-target ratio before rounding display percentage, with examples for 100-, 10-, and 1-unit targets, including unreachable bands for tiny targets; fixtures are planned, not executed.
- Tightened the canonical architecture's D-195 schema invariant wording: each schedule kind now explicitly specifies its permitted target period and NULL/non-NULL weekday-mask combinations, rejecting unknown bits rather than trimming them. Re-fetched and verified the architecture, product specification, test plan, traceability matrix, planning review, decision register, schema closure checklist and handover updates. Relevant documentation commits include `0f92e122`, `69db52a5`, `3ff425be`, `4d0c94a7`, `1fd964a0`, `e13a41f6`, and `5b3adbe3`. No application code/database was changed; tests are still planned only.
- Added a proposed record-type restore-conflict matrix to `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`, applying D-094–D-102 to journal entries, tracking records, habit definitions/configuration, preferences, drafts and unsafe backup files. It is a conservative schema proposal, not new owner approval; doc 27/28/31 now identify owner review as remaining. Commit: `b00d18ec`.
- Added planned restore-conflict acceptance cases to `docs/07-TESTING-AND-QUALITY.md` and linked them to AC-014/AC-022 in `docs/28-REQUIREMENT-TRACEABILITY.md`; updated docs 27/31 to show that test planning exists but owner review of the proposed conflict mapping and unsafe “Keep both” cases remains open. Commit: `91a06593`.
- Added candidate privacy-screen and Backup & Restore disclosure copy in `docs/04-DATA-AND-PRIVACY.md`, distinguishing offline/account-free core use, user-controlled backup files, provider-backed syncing, phone migration, and in-app deletion versus copies in saved files. The wording is explicitly draft pending owner review and actual platform/data-flow verification. Commit: `3f2f1105`.
- Updated README.md to link the current planning review, traceability matrix, UX/design pack, schema-closure plan and owner decision batch. A relative-target check of README and docs 27–31 found no missing relative targets in those checked files; the full repository-wide post-edit link/heading audit remains open.


- Schema reconciliation continuation: reconciled docs 03 and 30 on durable restore-session lifecycle, atomic Replace/Merge completion, interrupted-restore recovery states, index notation, notification-cancellation outbox uniqueness and proposed global idempotency-key enforcement across `habit_entries` and `habit_day_outcomes`. Added planned failure/recovery cases to docs 07/28 and updated the planning readiness checklist in doc 27. This narrows the schema gaps but does not mean the complete schema has owner approval or that any tests were run.
- Decision-log rendering fix: re-fetched `docs/09-DECISION-LOG.md`, removed blank-line breaks between adjacent D-001–D-190 rows and added a proper five-column table header for D-191–D-197. A fresh check found 191 decision rows, all with five columns, rendered as two continuous table blocks; no decision content or IDs were changed.
- Traceability/semantic review follow-up: corrected AC-009's synthetic October month fixture from Calm 2 to Calm 3 (J1, J3, J5); J5 was a primary Calm entry within October. Consolidated the duplicate Mood Insights comparison-control section in `docs/24-COMEBACK-EXPERIENCE.md`. Both are planning-document corrections; no test was run and no application code changed.
- Effective-date consistency fix: docs 03/30 previously described habit versions as immutable once effective, which did not precisely express D-194's same-day edit exception. Both now allow replacing today's version only before qualifying activity is recorded on that date; after activity, the flow offers Apply tomorrow and preserves the current-day/past history. Added the exact behavior to the planned test cases in doc 07. No test was run.
- Conflict/outbox design refinement: docs 03/30 now require a single canonical `ACTIVITY_IDEMPOTENCY_KEY` conflict for cross-table collisions, using the collided key as `record_key`, and require non-null opaque platform tag/notification-ID pairs in the cancellation outbox. Planned tests and traceability now cover these rules; they remain proposed schema requirements, not runtime evidence.
- Restore-conflict identity refinement: docs 03/30 now distinguish a canonical cross-table `ACTIVITY_IDEMPOTENCY_KEY` conflict from a same-stable-ID record conflict and require overlapping constraints for the same two payloads to be consolidated into one review item. Planned cases were added to docs 07/28 and the owner review batch doc 31. These are proposed schema/recovery rules only; no tests were executed.
- Documentation link/reference validation: parsed internal Markdown links across all 37 Markdown files and found no missing relative targets or heading fragments (there are no cross-file fragment links in the current set). All current external URLs in docs 13/18 and the repository links in the handover/workflow were opened successfully, including Google Play listing/screenshot assets, Android signing, AdMob, Play Billing, Android contrast guidance and WCAG; Expo-specific references remain explicitly historical. Updated doc 27 to mark the link/external-URL pass complete. The broader stale-claim/semantic contradiction pass remains open.
## Planning continuation — delegated defaults and current remaining work — 2026-10-27

- Re-fetched `main` and confirmed D-195 is already **Approved** in `docs/09-DECISION-LOG.md`; no duplicate decision was added. The fixed mask is documented in the data contracts and proposed schema (Monday=1 through Sunday=64; valid non-empty masks 1–127; selected-day mask required only for `SELECTED_WEEKDAYS`; flexible-weekly preferred days are optional guidance).
- Added D-196 as an approved planning-process rule: delegate conservative, reversible micro-decisions; reserve owner review for product scope/philosophy, privacy/security, data integrity, accessibility, monetisation, irreversible choices, conflicting policy, and owner-taste/identity. Reconciled this rule into the decision register, planning completeness review, owner decision batch, project charter and AI Studio workflow. It does not authorise application implementation or technical validation.
- Reminder provenance correction: D-045/D-046 approve weekly reflection trigger/catch-up/deduplication and no-backlog behaviour; they do not approve a clock time. Select the weekly reflection's device-local clock time separately in the grouped owner review. Do not infer it from the per-habit 9:00 a.m. default. Runtime delivery remains unverified.
- D-010 now records the owner's preferred *candidate* monetisation approach (subtle banners on suitable secondary screens, optional user-initiated rewarded extras, and a possible one-time Supporter Pack). This is not approval to ship monetisation in initial v1; no SDK, ad placement, purchase provider or monetisation implementation is authorised by that preference.
- **Planning-only status:** the weekday-mask schema rule is documented; no application code/schema/migration was changed, and no build, test, APK install, device check or implementation audit was performed in this continuation. The planning gate remains not approved.

### Consolidated planning work still required

1. **Written schema reconciliation is complete.** Present `docs/03-TECHNICAL-ARCHITECTURE.md` and `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md` together for owner review of the proposed field/constraint model, portable format/schema compatibility boundary, and restore-conflict cases where “Keep both” is unsafe. D-197's fail-closed migration default preserves the original database and rolls back if any legacy row cannot be mapped losslessly. Do not inspect or migrate the live database during this planning review.
2. Use `docs/31-OWNER-PLANNING-DECISION-BATCH.md` for one grouped review of genuinely consequential items: (a) complete schema and backup/restore compatibility/conflict handling; (b) design choices/deferrals after rendered prototypes; (c) privacy/export/delete wording; (d) whether monetisation remains excluded from initial v1 (recommended default); and (e) explicit planning-gate approval. Undo/reminder/prompt details are delegated under D-196 unless the owner chooses to override them.
3. Review all three rendered low-fidelity comparison boards using the task/accessibility scorecard; record owner decisions or deliberate v1 deferrals for navigation, Today hierarchy, Growth Mark, journal archive, typography/colour/theme, and comeback presentation. The boards already exist; user testing/accessibility validation has not been performed.
4. Close remaining requirement-to-test traceability and semantic stale-claim/duplicate/conflict/supported-evidence review. Internal relative-link, heading-fragment and external-URL checks are complete across all 37 Markdown files; repeat only if further edits affect links/headings. All acceptance cases remain planned, not executed.
5. Keep post-gate native build/test/device/schema/migration/backup/notification validation separate. It requires explicit authorisation after the planning gate; feature implementation is a later, separate stage.

The canonical readiness snapshot is `docs/27-PLANNING-COMPLETENESS-REVIEW.md`; the decision log remains authoritative. Older historical handover entries should not be treated as current when they conflict with that snapshot.


## Portable backup schema reconciliation — 2026-10-27

- Added and verified a field-level portable-backup mapping in doc 30 and aligned docs 03/07/28/27/31: entity fields and relationships, preservation of stable IDs/local-date keys/idempotency, distinct format/schema-version compatibility rules, the minimum portable context for unresolved restore conflicts, fresh local restore-session ID remapping, constrained “Keep both” and idempotency/note-conflict handling, and exclusions for device-only notification/cancellation state, provider grants, caches and secrets. Removed the superseded category-only list to avoid two competing mapping descriptions. Acceptance coverage is planned only; owner review of the proposed compatibility boundary and unsafe conflict choices remains open.
- This is a documentation-only refinement. No app code or live database was changed; no build, test, migration, APK install, device validation or implementation audit was performed.
- A further consistency pass reconciled restore-conflict lifecycle: conflict rows exist only while unresolved (no separate status column); the parent restore session uses an explicit proposed state and remains `COMPLETED_WITH_CONFLICTS` until its last child conflict is resolved, then transitions to `COMPLETED` atomically. Updated docs 03/30/07/28 and recorded the discrepancy as R-016 in doc 25. These are proposed schema rules with planned tests, not an implementation finding.


## Schema reconciliation completed; owner review still pending — 2026-10-27

- Created three editable SVG planning boards: `docs/prototypes/dayseed-today-variants.svg` (W1 Action-first, W2 Reflective, W3 Compact), `docs/prototypes/dayseed-navigation-archive-variants.svg` (N1/N2 navigation and A1/A2/A3 archive), and `docs/prototypes/dayseed-growth-mark-variants.svg` (G1/G2/G3 monthly-summary representations). The Today board was reconciled to the owner-selected daily summary, one-line monthly context and quiet route to Progress. Both use synthetic data; neither is approved UI or usability evidence. Task/accessibility review and owner selection or explicit deferral remain.

- Reconciled docs 03 and 30 on the proposed relational model, including one authoritative effective-dated lifecycle (no separate archived timestamp projection), D-195 weekday-mask/schedule constraints, cross-table activity idempotency conflict handling, durable restore-session and conflict-row lifecycle, field-level portable-backup mapping, transaction boundaries, stable journal-draft save identity, and migration rollback rules.
- Added `intended_journal_entry_id` to the proposed journal-draft model with a partial unique index for non-null values. A prepared save reuses that stable ID, atomically inserts the saved journal entry and resolves its source draft, and preserves the draft/ID on failure or uncertain outcome. Acceptance coverage is planned only.
- Added D-197 as a delegated fail-closed migration default: if any legacy record cannot be mapped losslessly, roll back the entire migration and leave the original DB/schema version intact. No automatic dropping/coercion or assumed quarantine table. Synthetic migration-failure tests remain gated.
- Updated docs 07/28 and the planning review to trace these cases. The written model is reconciled, but the full schema has not been owner-approved; owner review of portable format/schema compatibility and unsafe restore-conflict choices remains. No code/database change, build, test, migration, APK, device validation or implementation audit was performed.

- Traceability continuation: reviewed AC-009's deterministic Mood Insights fixture in docs 07/28. It covers configured Monday/Sunday week starts, month/week boundaries, primary-only counts, entries without mood tags, sparse/empty states, matching-entry navigation and source-record correction/deletion. No further written AC-009 gap is currently identified; the fixture remains planned, not executed.


## Rendered UX comparison boards added — 2026-10-28

- Created `docs/prototypes/dayseed-today-variants.svg`, with three comparable Today hierarchies using identical synthetic habit data and the owner-selected monthly context/Progress route.
- Created `docs/prototypes/dayseed-navigation-archive-variants.svg`, comparing four versus five bottom destinations and three journal archive layouts.
- Created `docs/prototypes/dayseed-growth-mark-variants.svg`, isolating plain monthly context, a “Collected moments” label with count, and count plus inspectable date marks using the same synthetic activity.
- All three SVG files were re-fetched from `main` and passed a simple XML tag-balance check. They are planning artefacts only—not approved UI, app code, or usability evidence. Next: review them against the task/accessibility scorecard, then record choices or explicit deferrals. No app code, build, tests, database, migration, APK, device state or implementation audit was changed or run.


## Continuation handover and decision protocol — 2026-10-28

- Added [`AGENTS.md`](../AGENTS.md) as the repository-root entry point for AI agents, and [`docs/32-CONTINUATION-HANDOVER.md`](32-CONTINUATION-HANDOVER.md) as the self-contained handover/decision protocol. The README now points new assistants to both.
- The handover captures project purpose and stack, canonical decision/document hierarchy, D-196 decision-making rules, escalation criteria, grouped Accept/Modify/Defer workflow, GitHub write/verification discipline, current status and the ordered next steps.
- It records the current schema and privacy caveats, the weekly-reflection-time owner choice, the three planning-only SVG boards, and the boundary between planning, technical validation, and later implementation. It does not add owner approvals or open the gate.
- GitHub writes for `docs/32-CONTINUATION-HANDOVER.md`, root `AGENTS.md`, and the README link were re-fetched and verified. The three SVG boards are present under `docs/prototypes/`; their contents are synthetic planning artefacts, not approved UI or usability/accessibility evidence.
- **Next action:** use O-1–O-11 in `docs/31-OWNER-PLANNING-DECISION-BATCH.md` for one grouped owner review; then record actual decisions, reconcile affected documents, finish design/accessibility and traceability/semantic checks, and only then present a final planning-gate readiness decision.
- **Not done:** no application code/schema/migration changes, live-database inspection, implementation audit, build, tests, APK installation, device validation, or feature implementation.



## Owner-provided UI reference preserved — 2026-10-28

- Added [the owner-provided Dayseed multi-screen UI reference](prototypes/dayseed-owner-reference-layout.webp) to the repository as a durable visual direction. It includes Today variants/states A–E and supporting Habit Create/Edit, Habit Detail, History calendar, Journal Entry, Insights Overview, Return-after-a-break, and Settings screens.
- Linked the reference from the README, root AI-agent instructions, UX/Design Approval Pack, and canonical Continuation Handover so future agents are directed to it before design work.
- The reference is a visual north star, not final screen/token/navigation approval or usability/accessibility evidence. DESIGN.md remains the sole design-token/component authority, owner choices in docs 09/31 remain open as recorded, and the planning gate remains not approved.
- No application code, schema, database, migration, build, test, APK, or device validation was changed or run.

## Owner planning responses reconciled — 2026-10-28

- Recorded O-1 Accept: reconciled schema accepted as the written planning baseline only; no live schema/migration approval. O-2 Accept: portable-backup compatibility and restore-conflict rules accepted as the planning policy baseline.
- Recorded O-3 W1 Action-first, O-4 N1 four destinations, and O-5 A2 hybrid journal archive as selected planning directions. The later O-6 clarification is D-199: modified G2 duration-total “Collected moments” summary, with G3 calendar/detail. Current-month default period, display thresholds, alternative period controls and accessibility remain open; no final UI is approved. O-7 accepted platform typography/system light/dark baseline while exact colour/type tokens remain open. O-8 accepted the compact, dismissible, non-blocking comeback direction.
- Recorded O-9 weekly reflection at 9:00 a.m. device-local on the configured week-start day. O-10 accepted privacy/export/delete wording as a draft only, pending runtime verification.
- Recorded O-11 request to evaluate initial-launch ads because the owner states a full launch needs at least ads. Added `docs/33-MONETISATION-AND-ADS-EVALUATION.md` based on current official Google Play and AdMob guidance. Proposed banner-first evaluation is not approval to add an SDK or ship ads; rewarded extras, purchases, subscriptions and Supporter Pack remain unapproved.
- Reconciled docs 00/01/03/04/06/08/09/11/12/19/23/25/27/28/29/30/31/32/33, DESIGN.md, README, and the workflow/traceability documents as applicable. This earlier record reflects O-6's initial Modify response; the subsequent D-199 clarification now selects modified G2 duration-total summary and G3 calendar/detail roles. The planning gate remains **not approved**. No application code, database, migration, build, tests, APK, or device validation was changed or run.
- Next action: complete remaining task/accessibility and semantic-consistency reviews; review the ads evaluation and obtain any explicit follow-up decision required; then prepare the planning readiness report. Technical validation and implementation remain separate, gated stages.

## Final planning reconciliation and link check — 2026-10-28

- Reconciled the owner responses O-1–O-11 across the authoritative Decision Log, response batch, decision register, readiness review, UX pack, schema plan, privacy/weekly-reminder documents, risk register, charter, product specification, monetisation guidance, roadmap, workflow, README, DESIGN.md and continuation handover.
- Added `docs/33-MONETISATION-AND-ADS-EVALUATION.md` based on current official Google Play and AdMob guidance. O-11 requests an evaluation and states that at least ads are needed for a full launch; no SDK, format, placement or release integration is approved. The banner-first option remains a proposal for a distinct owner choice.
- Corrected and recorded the follow-up O-6 clarification as D-199 throughout current planning: modified G2 “Collected moments” shows total recorded duration from duration-tracked habits, with the current calendar month as the compact summary's default period; G3 provides Calendar/date-level or separate-page detail for inspecting records. G1 remains an alternative active-date summary. Formatting, period controls and accessibility remain open.
- Confirmed O-9 as 9:00 a.m. device-local on the configured week-start day. O-1/O-2 remain planning-baseline decisions only; the written schema is not approved for implementation. O-10 remains draft-only privacy copy.
- Checked relative Markdown targets in all 40 repository Markdown files after the latest edits; the checker found no missing relative file targets or invalid same-file fragments. This was a documentation link check, not an external-URL audit of every historical research link. The new ads-policy references were separately checked against current official Google Play/AdMob pages.
- No code, database/schema implementation, migration, Gradle build, automated test, APK install, device validation or implementation audit was run. The planning gate remains **not approved**.

## Growth Mark clarification reconciled — 2026-10-28

- Recorded O-6 follow-up in D-199: G2 is now the selected planning direction for a compact “Collected moments” summary showing total recorded duration from duration-tracked habits, defaulting to the current calendar month; G3 is the date-level Calendar/detail view or separate page. The summary should use readable units at varying magnitudes. Calendar-month duration approximations, exact thresholds, alternate day/week/month period controls, compatible-unit aggregation and edge-state behavior still need planning review.
- Reconciled `docs/09-DECISION-LOG.md`, `docs/31-OWNER-PLANNING-DECISION-BATCH.md`, `docs/29-UX-DESIGN-APPROVAL-PACK.md`, `docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md`, the Growth Mark SVG, and the relevant planning contract/readiness docs as recorded by their commits. Added planned acceptance coverage AC-025 and Growth Mark cases in `docs/07-TESTING-AND-QUALITY.md`.
- This is planning and documentation only. No application code, database/schema, migration, build, automated test, APK installation, device validation or implementation audit was run. The planning gate remains **not approved**.
- Verification for this continuation: current decision/status files were re-fetched from `main`; D-199 agrees with the revised G2/G3 contract, AC-025 and the prototype; the owner batch, README and handover reflect the clarification. A targeted current-status scan across all 40 Markdown files found no remaining live claim that O-6's intent is unspecified; historical progress notes are explicitly superseded by D-199. A post-edit relative-link target pass parsed 245 inline relative Markdown links across the 40 files and found zero missing file targets; the Markdown table-width scan found no malformed rows in the audited files. Same-file heading-fragment review previously passed, and this pass added no heading-anchor links or renamed referenced headings. This is documentation validation only, not app/test validation.
- Next: finish task/accessibility review of W1/N1/A2 and G2/G3, settle readable duration-unit thresholds and day/week/month detail-period controls, and obtain the explicit include/defer decision after review of the ads evaluation. The open work is not further clarification of O-6. The planning gate remains **not approved**; technical validation and implementation remain separately authorised stages.


## Planning completeness and semantic reconciliation continuation — 2026-10-29

- Updated `README.md` to align current status with D-200 (initial-v1 ads deferred; pre-launch revisit) and D-201 (duration formatting, compatible aggregation, and period defaults recorded). Removed stale wording that treated the initial-v1 ads decision and Growth Mark unit/period defaults as pending.
- Updated `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md` to stop listing D-201's already-recorded duration/period defaults as unfinished planning questions.
- Reconciled `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`: the log and register each contain 197 unique decision rows through D-203; updated R-020's stale 193-row count and the D-203 follow-up to reference docs 04/26/28/35 and R-034.
- Updated `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/32-CONTINUATION-HANDOVER.md`, and `docs/35-PLANNING-READINESS-REPORT.md` to record the latest bounded semantic pass, the current D-200/D-201 positions, and an honest static textual heuristic review of the three SVG boards. Findings: W1 keeps logging first; N1's Journal-under-History placement remains a discoverability risk; A2 is denser but offers timeline plus date jump; G2 labels month/duration; G3's synthetic sessions total 3 hr 40 min, matching G2. The binary owner reference and rendered-pixel contrast/accessibility were not inspected.
- Verification: re-fetched current `main`; counted 197 unique decision rows in both `docs/09-DECISION-LOG.md` and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`; re-fetched updated README, workflow, decision register, completeness review, handover and readiness report. Targeted stale statements were corrected. This is not an exhaustive post-update repository-wide link/semantic audit.
- Not done: no application source changes, schema/migration changes, build, automated tests, APK installation, physical-device validation, runtime privacy/deletion/restore tests, user study, rendered accessibility verification, or technical validation.
- Current gate: **planning gate remains closed / not approved**. Next: reconcile any remaining stale status claims in historical/handover material, cross-check doc 27 against doc 35, and then present the bounded remaining planning items for owner review. Owner review is not gate approval.


## Traceability and checklist reconciliation continuation — 2026-10-29

- Reconciled `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/31-OWNER-PLANNING-DECISION-BATCH.md`, `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md`, and `docs/35-PLANNING-READINESS-REPORT.md` after cross-checking their remaining-work statements.
- Corrected stale claims that D-201's Growth Mark formatting/period defaults remained undecided and that the initial-v1 ads decision was pending. Doc 28 now identifies the D-200 deferral and treats D-201 semantics as settled planning defaults with later acceptance obligations. Doc 31 now reflects D-201 and D-203 in the current decision summary; docs 27/35 agree that final semantic/traceability and post-edit structural checks remain open.
- Reconciled the design review wording: only the SVG text/structure received a static heuristic pass. Rendered UI, the binary owner reference image, actual contrast, TalkBack, large-text behaviour, user tasks and runtime accessibility remain unverified.
- Verification: re-fetched each edited document from `main` after its write. Decision log and register each retain 197 unique rows through D-203. No application files, schema, migration, build, tests, APK, device state or runtime data were changed or tested.
- Remaining: complete the final bounded semantic/traceability review and post-edit internal-link, heading-fragment and table-shape checks; then present the remaining planning items for owner review. This is not planning-gate approval.
- **Current gate: CLOSED / NOT APPROVED.** No technical validation or implementation is authorised by this documentation work.


## Final bounded planning reconciliation check — 2026-10-29

- Re-fetched the current planning set after the latest edits. Corrected two malformed Markdown table rows (R-029/R-030) in `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`; the row-width check now passes for docs 25, 27, 28 and 31 with no malformed rows found.
- Decision coverage remains consistent: `docs/09-DECISION-LOG.md` and the decision matrix each contain 197 unique decision IDs through D-203, with gaps preserved.
- Targeted stale-status scans across README and docs 19/27/28/31/32/35/12/25 found no remaining matches for the specific superseded D-200 ads-pending, D-201 formatting/period-open, or inaccurate completed-accessibility-review wording that this pass targeted. This is a bounded phrase/status scan, not proof of exhaustive semantic consistency across every historical paragraph.
- Internal links and heading fragments were not comprehensively re-crawled across the entire repository after every latest text edit. The edited paragraphs did not intentionally add or change Markdown destinations/headings; final repository-wide link/heading verification remains a closeout check if the docs continue changing.
- No app code/schema/migration changes, build, tests, APK, device validation, runtime privacy/deletion/restore checks or user study were performed. **Planning gate remains CLOSED / NOT APPROVED.**
- Next: present the bounded remaining planning items for owner review; do not treat that review as gate approval.


### Follow-up consistency corrections — 2026-10-29

- The final cross-check caught two remaining stale statements in the traceability/owner-decision documents: AC-014/gap 1 still described O-2's format/schema compatibility boundary as awaiting owner review, and the O-6 summary still presented period controls as open. Updated docs 28 and 31 to reflect O-2's accepted planning baseline and D-201's settled duration/period defaults. Updated R-029 in doc 25 to remove the stale owner-review claim and repaired its Markdown table row width along with R-030.
- Re-fetched README and docs 19/25/27/28/31/32/35. The targeted stale-status scan found no remaining matches for the superseded ads-pending, D-201-open, O-2-pending or inaccurate accessibility-review wording. The decision log and register still each contain 197 unique IDs through D-203. Table-width checks for docs 25/27/28/31 found no malformed rows.
- A full repository-wide post-edit link/heading-fragment crawl remains unclaimed; no destinations or heading anchors were intentionally changed in this correction. The broad semantic/traceability closeout remains open and all acceptance cases remain planned, not executed. Planning gate remains **CLOSED / NOT APPROVED**.


## Bounded schema and recovery traceability closeout — 2026-10-29

- Compared the current main-branch versions of `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/07-TESTING-AND-QUALITY.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, and `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`.
- Cross-checked the proposed D-197 all-or-nothing migration rollback, stable `intended_journal_entry_id` draft-save/retry handling, per-table non-null idempotency indexes plus cross-table activity-key guard, durable restore-session states, preservation/remapping of unresolved conflict snapshots, atomic conflict resolution, and the planned acceptance cases. No clear contradiction was found in this bounded comparison; the proposed schema remains unimplemented and the acceptance cases remain planned, not executed.
- Updated docs 27/28/35 so they distinguish this bounded comparison from the broader repository-wide semantic audit. No acceptance test, live database inspection, source-code audit, migration, build, APK, device validation, or runtime privacy/restore/deletion check was performed.
- An exhaustive repository-wide link/heading crawl after all edits is still not claimed. Only the prior targeted table-shape and stale-status checks are established; any final internal-link/heading check must be described at the actual scope performed.
- **Gate remains CLOSED / NOT APPROVED.** Next: present the bounded remaining planning items for owner review. Owner review is not gate approval; technical validation and implementation require separate explicit authorisation.


## Repository-wide Markdown target and status reconciliation — 2026-10-29

- Rebuilt the current repository file list from GitHub's recursive tree and checked relative Markdown link targets across all 42 Markdown files. The reviewed batches parsed 285 relative Markdown links; no missing relative file targets were found. Same-file heading fragments were checked in the audited batches with no missing fragments found. Cross-file heading-fragment targets were not exhaustively crawled, so this is not described as a complete heading-anchor audit.
- The key planning-table width checks passed for docs 03/09/25/26/27/28/30/31. The bounded schema/restore/migration comparison remains consistent with the previous closeout.
- The broader semantic pass identified stale current-status language in doc 29 (Navigation/Journal placement and Journal archive still labelled open; prototype descriptions said no style/navigation choice was approved) and doc 31 (D-201's period and compatible-aggregation defaults still described as awaiting specification). Updated those passages to reflect N1/A2 and modified G2/G3 as selected planning directions and D-201 as the current planning baseline. Validation remains outstanding and the directions are not approved/validated final UI.
- Updated doc 27 and doc 35 to record the actual scope of link and semantic checks without claiming a full cross-file heading-anchor crawl. Historical dated records remain historical; unresolved accessibility, privacy/runtime, and implementation verification items are still open.
- **Gate remains CLOSED / NOT APPROVED.** No source code, schema, migration, build, tests, APK, or device validation was performed.


- Follow-up UX status correction (2026-10-29): updated doc 29 to distinguish selected planning directions from final UI approval. D-201 formatting/period defaults are settled planning defaults; actual UI, aggregation/correction, task comprehension, large text and accessibility remain unverified.

## Bounded planning closeout — 2026-10-30

- Reconciled docs 28/29/33/19 and docs 11/27/35 against current decisions. AC-018/AC-021/AC-025 now distinguish selected W1/N1/A2 and modified G2/G3 planning directions from later task/accessibility/runtime evidence. D-200's initial-v1 ads deferral is reflected in the risk register, UX pack and monetisation evaluation; D-201 defaults are not presented as open choices.
- Remaining owner planning choices: (1) confirm or explicitly defer D-008 broad visual identity; (2) decide whether D-019 exact colour/type tokens must be fixed before the gate or explicitly deferred to a later design stage; (3) approve or revise D-017 final comeback copy/localisation. These can be resolved as a concise owner decision batch; no need to reopen settled N1/A2 or D-201 semantics without a material reason.
- After those choices, perform one final current-set reconciliation of docs 27/28/29/31/35/12/19, update the approval record only if the owner explicitly approves the planning gate, and record approved scope/version. The gate remains **CLOSED / NOT APPROVED**.
- Separate later authorisation is required for source/build/schema/migration/tests/device validation and any implementation. Rendered accessibility, user tasks, runtime privacy/deletion/restore and platform behaviour are not proven by this documentation pass.


## Final documentation and handover reconciliation — 2026-10-30

- **Branch:** `main` (GitHub remote). Work remained planning/documentation only.
- Reconciled `docs/09-DECISION-LOG.md` and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`: D-091–D-105 next-action gaps now point to later decisions/contracts and planned acceptance coverage. R-019 is closed as a documentation follow-up issue, not as technical verification. The decision log and register each contain 201 unique decision rows through D-207, with gaps preserved.
- Reconciled stale current-status statements in docs 27/32 and README. D-205 selects the visual identity synthesis; D-206 selects two-week/one-month comeback copy; D-207 selects Soft Botanical while deferring exact tokens/fonts; D-204 records both automatic and fixed theme modes with gradual automatic transitions. D-201 Growth Mark semantics remain settled planning defaults.
- Cross-checked docs 09/12/17/25/27/28/29/30/31/32/35 and README. The bounded current planning set is aligned on owner choices, Growth Mark, backup/restore follow-up, traceability and gate boundaries. Cross-file heading-fragment and external-URL checks were not exhaustive; older dated history is retained as historical evidence rather than rewritten wholesale.
- Re-fetched and verified each updated document after writing. No acceptance case was marked passed. No source/runtime audit, schema inspection, migration, build, test, APK installation, device validation or user study was performed.
- **Planning gate: CLOSED / NOT APPROVED.** The next action is owner review of `docs/35-PLANNING-READINESS-REPORT.md` and a distinct explicit gate decision. The handover and approval record must not be changed to Approved unless the owner expressly authorises it.


## Owner-approved Stage A planning gate — 2026-10-30

- Owner explicitly instructed the project to open the planning gate; the decision is recorded as D-208 in `docs/09-DECISION-LOG.md`.
- Stage A planning baseline: **COMPLETE; APPROVED / OPEN**.
- Stage B bounded technical validation: **NOT YET AUTHORISED**. Do not inspect or change source/schema/database, build, test, install APKs or perform device/runtime checks until separate written owner authorisation.
- Stage C feature implementation: **NOT AUTHORISED**; requires Stage B exit review and separate approval of the implementation plan.
- Rendered UI, task comprehension, accessibility, runtime privacy/data-flow, backup/restore/delete, migration and device checks remain post-build acceptance obligations. None is marked passed or waived.
- No technical validation or implementation was performed as part of recording D-208.


## Current authoritative workflow correction — 2026-10-30

This dated addendum supersedes earlier status statements and next-action lists in this chronological document where they describe Stage B as technical validation or Stage C as implementation. Stage A planning remains complete/approved-open under D-208. The correct sequence is **Stage B full initial-v1 implementation in Google AI Studio** (not yet authorised) → **Stage C independent technical validation after implementation** → **Stage D final owner acceptance/release readiness**. The documentation correction does not authorise Stage B. No app code, database, migration, build, test, APK or device validation was performed by this documentation update.

Every future project inspection must record date/time/timezone, repository, branch, exact inspected remote commit SHA, imported workspace/base SHA (or UNKNOWN), inspected paths and line ranges/symbols, findings, and uninspected areas. AI Studio must refresh/reconcile against latest GitHub main before editing and before pushing; never overwrite or force-push newer remote work. Test results must be marked NOT RUN/BLOCKED/FAILED/PASSED honestly; PASSED requires actual execution and retained command/log/environment/commit evidence.


## Stage B / Stage C Full Initial-v1 Implementation Completed in Google AI Studio — 2026-10-30

- **Authorisation:** Executed under explicit owner instruction in Google AI Studio following approval of the Corrected Master V1 Implementation Plan (`implementation_plan.md`).
- **Inspection & Preflight Baseline:**
  - **Date/Time:** `2026-10-30T22:49:12Z` (`2026-10-30T15:49:12-07:00`)
  - **Repository & Branch:** `HeWhoArose/Dayseed`, branch `main`
  - **Remote Base Commit SHA (`origin/main`):** `f858f01590f270544cf0534c1c8f1082b9584fa0`
- **What Was Implemented (Phases 1–7):**
  1. **Phase 1 — Canonical 12-Table SQLite v2 Schema, Fail-Closed Migration & Clock (`DayseedClock.kt`, `Models.kt`, `DayseedDatabaseHelper.kt`):**
     - Upgraded `dayseed.db` to `DATABASE_VERSION = 2` with `PRAGMA foreign_keys = ON` in `onConfigure` and `onOpen`.
     - Created all 12 canonical tables (`habits`, `habit_versions`, `habit_entries`, `habit_day_outcomes`, `habit_date_notes`, `journal_entries`, `journal_drafts`, `app_settings`, `reminder_deliveries`, `restore_sessions`, `restore_conflicts`, `notification_cancel_outbox`), partial unique indexes, and cross-table triggers (`trg_habit_entries_insert_guards`, `trg_habit_entries_update_guards`, `trg_habit_day_outcomes_insert_guards`, `trg_habit_day_outcomes_update_guards`).
     - Implemented fail-closed `migrateV1ToV2FailClosed` (`D-197`) that validates all legacy v1 rows before mutating schema and rolls back with `DayseedMigrationBlockedException` if any legacy row contains an unmappable `SKIPPED` or invalid state.
  2. **Phase 2 — Habit Engine, Effective-Dated Versions, Progress Bands, Guarded Undo & Growth Mark (`DayseedRepository.kt`, `HabitCard.kt`, `AddEditHabitDialog.kt`, `GrowthMark.kt`, `TodayScreen.kt`):**
     - Implemented all 4 tracking modes (`QUANTITY`, `DURATION`, `COMPLETION_ONLY`, `EFFORT_AWARE`), 3 schedule kinds (`DAILY`, `SELECTED_WEEKDAYS`, `FLEXIBLE_WEEKLY_TARGET`), 7-bit ISO `WeekdayMask` (`1..127`), `ProgressBandClassifier` (`D-040`), `DurationNormalizer` (`D-198`), `D-194` `Apply today` vs `Apply tomorrow` dialog flow when today already has activity, 8-second guarded `UndoToken` (`D-209`), and Growth Mark `G2` (“Collected moments” current-month duration total) + `G3` (monthly session breakdown and 12-dot rhythmic ring).
  3. **Phase 3 — Micro-Journal, Atomic Draft Promotion, A2 Hybrid Archive & Retrospective Prompts (`DayseedRepository.kt`, `ReflectionCard.kt`, `JournalEditorDialog.kt`, `HistoryScreen.kt`):**
     - Implemented `journal_drafts` autosave, two-phase atomic draft promotion using `intended_journal_entry_id` (`D-191`), curated 10-mood vocabulary (6 starter + 4 additional moods, 1 primary + 1 optional secondary), `A2` hybrid journal archive (calendar date jump + chronological timeline + local title/body search + mood filter), and `D-042` low-frequency retrospective journal prompts with 7-day dismissal cooldown.
  4. **Phase 4 — Comeback 14-Day & 30-Day Experience (`DayseedRepository.kt`, `ComebackCard.kt`, `TodayScreen.kt`):**
     - Implemented deterministic 14-day (`14..29` days) and 30-day (`>= 30` days, superseding 14-day) comeback detection, clock-uncertainty suppression (`D-187`), once-per-episode suppression, owner-approved `D-206` copy, per-habit Resume / Lower Target / Pause controls, and optional returning reflection promotion.
  5. **Phase 5 — Mood Insights (`J1–J11`), Quiet-Hours Reminder Ledger & Notification Cancel Outbox (`ReminderEngine.kt`, `DayseedRepository.kt`, `InsightsScreen.kt`):**
     - Implemented `computeMoodInsights` honoring `app_settings.week_start` (`MONDAY` or `SUNDAY`), primary-mood-only distribution counts and percentages, previous-period comparison, and tap-to-filter matching entries.
     - Implemented `ReminderEngine` enforcing `21:00–08:00` quiet hours, shared 3-notifications/day cap, weekly reflection at `09:00` on configured week-start (`O-9`), and post-commit `notification_cancel_outbox` processing on permanent habit deletion.
  6. **Phase 6 — Privacy, Readable Exports, AES-256-GCM Portable Backup/Restore & Delete Flows (`BackupAndExportManager.kt`, `SettingsScreen.kt`, `AndroidManifest.xml`, `backup_rules.xml`, `data_extraction_rules.xml`):**
     - Configured `android:allowBackup="false"` and platform backup exclusion rules (`D-186`).
     - Implemented readable Journal export (`Markdown`, `CSV`, `JSON`) excluding `journal_drafts`, portable `.dayseed.json` backup with optional password-based `PBKDF2WithHmacSHA256` + `AES/GCM/NoPadding` encryption (`BackupCryptoConfig`), pre-restore safety backup verification, `REPLACE` and `MERGE` restore modes, interactive `RestoreConflict` resolution (`KEEP_CURRENT`, `USE_BACKUP`, and `KEEP_BOTH` restricted to `journal_entry`/`habit_entry`), and 3 distinct deletion scopes with `D-203` external-copy disclosure.
  7. **Phase 7 — Soft Botanical Time-of-Day Palette, Fixed Themes & Accessibility Semantics (`Color.kt`, `Theme.kt`, `MainActivity.kt`):**
     - Isolated provisional Soft Botanical daypart palettes (`MORNING`, `DAY`, `EVENING`, `NIGHT`) in `SoftBotanicalThemeConfig` (`D-204`, `D-207`) alongside fixed `SYSTEM`, `LIGHT`, and `DARK` modes, 48dp minimum touch targets, semantic `testTag`s, and `BackHandler` navigation on secondary tabs.
- **Verification Evidence:**
  - `compile_applet`: **PASSED**.
  - `gradle :app:testDebugUnitTest --info`: **PASSED** (`12 tests completed, 0 failed, 0 skipped` in `DayseedAcceptanceTest`; retained report at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.DayseedAcceptanceTest.xml`).
- **Gate Status:** Stage B implementation self-checks are complete. **Stage C independent technical validation** and **Stage D final owner acceptance / release readiness** remain open and are not claimed by this implementation pass.


## Time-of-day theme direction recorded and audited — 2026-10-31 (D-210)

The owner supplied a five-panel theme reference sheet and instructed that Dayseed's theme direction is the **colours, designs and per-time-of-day pictures** in that sheet, ignoring its sample items and wording, and asked for it to be recorded explicitly and audited against the current implementation.

- **Reference persisted:** `docs/prototypes/dayseed-theme-time-of-day-reference.jpeg`, copied from the owner submission so the direction survives independently of the chat attachment.
- **Recorded:** D-210 in `09-DECISION-LOG.md`; register row plus finding R-035 in `25-DECISION-REGISTER-AND-RECONCILIATION.md`; extended notes in `17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md` §9 and `29-UX-DESIGN-APPROVAL-PACK.md` §5.8; direction pointer in `DESIGN.md` with candidate tokens explicitly left unapproved.
- **Audit scope:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`, `ui/screens/TodayScreen.kt`, and `app/src/main/res/drawable` at commit `444dc41`.
- **Audit result — does not match yet:** evening still uses a green primary instead of the reference's terracotta/clay action; no daypart header artwork exists anywhere (only launcher assets in `res/drawable`); the night theme has no crescent-moon/night-foliage treatment; morning and day tokens are nearly identical; display typography is system-default without the reference's serif headings (a known O-7/D-019 deferral); and no time-aware greeting is rendered. What does align is the broad botanical palette family and the four-slot daypart mechanism with centralised provisional config.
- **Status of the gaps:** recorded as findings only. Exact hex values, clock boundaries, typography, transition timing and reduced-motion remain delegated or deferred, are not approved tokens, and carry no contrast, large-text, TalkBack or rendered-device evidence. Resolving the gaps is implementation work and is not authorised by D-210.

## Distinct themed layout implemented — 2026-10-31 (D-211, commit `137790d`)

The owner then instructed that the frontend-design skills be used as a guide and the distinct layout be created. That instruction authorised this specific Stage B implementation step.

- **Theme tokens (`ui/theme/Color.kt`, `Theme.kt`):** Morning is now "Sage Modern" (leaf-green `#3E7C59` action on airy parchment `#F7F9F1`); Evening is now "Earth & Clay" (terracotta `#AC5530` action on warm stone `#F4EBDD`, border `#DECDB4`); daylight and night keep the canonical candidate tokens, so evening no longer reuses the daylight green (audit gaps a and d closed).
- **Central resolution (`ui/theme/DaypartVisuals.kt`):** one rule maps theme mode + local time to one of four header artworks and the daypart greeting; fixed LIGHT/DARK/SYSTEM modes keep stable artwork while automatic mode follows the unchanged D-204 clock boundaries (05/11/17/21).
- **Artwork (`ui/components/DaypartArtwork.kt`):** four static vector Canvas compositions — morning sprig, daylight branch, sunset hills with low sun, and the Night Garden golden crescent moon with foliage (gaps b and c closed). No bitmap assets, no new dependencies, no motion; semantics cleared so TalkBack never stops on decoration.
- **Today header (`ui/screens/TodayScreen.kt`):** left-aligned — serif date line, time-aware greeting (gap f closed), product tagline, 104dp artwork on the right. The old "Dayseed" title text is replaced by the greeting as the editorial headline.
- **Display type (`ui/theme/Type.kt`):** provisional platform-serif greeting/date styles using the Android system serif family (no licence/bundle cost); formally still deferred under O-7/D-019 pending the readability review.
- **Verification evidence (executed):**
  - `:app:testDebugUnitTest --tests com.example.dayseed.DayseedVisualResolutionTest` → **PASSED**, 7/7, 0 failures/errors/skipped (suite XML at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.DayseedVisualResolutionTest.xml`, timestamp `2026-10-31T12:09:32Z`; build output first failed on a composability restriction at `DaypartArtwork.kt:179` — fixed by hoisting ambient reads — and on `invalid source release: 21` because only JDK 17 was present; a temporary JDK 21.0.13 was extracted under `%TEMP%\opencode\jdk21` and passed to Gradle via `-Dorg.gradle.java.home` with no system change).
  - `:app:testDebugUnitTest --rerun` → **PASSED**, 19/19 total (`DayseedAcceptanceTest` 12 + `DayseedVisualResolutionTest` 7), 0 failures/errors/skipped, BUILD SUCCESSFUL.
  - Environment: Windows 11, Gradle 9.3.1 (from `~/.gradle/wrapper/dists`), JDK 21.0.13 (Temurin), Android SDK API 36.
- **Not claimed:** rendered-device visual review, measured contrast (evening terracotta/white pairing only estimated ≈5:1), large-text (200%) layout with artwork, TalkBack traversal/reading order, reduced-motion confirmation, and gradual daypart transitions (not yet implemented; no motion was added). These remain Stage C obligations.

## Settings restructured into hub with bottom-sheet panels — 2026-10-31 (D-212, commit `0c4933e`)

The owner asked for an audit of the Settings screen ("shows everything bare", asking for the best way to restructure it). The audit (`SettingsScreen.kt` at `34ad3e7`, plus `docs/26-SCREEN-AND-DATA-CONTRACTS.md:161-222` and `docs/29-UX-DESIGN-APPROVAL-PACK.md`) recorded eight causes: decision IDs and schema vocabulary printed as user copy; no progressive disclosure; no current-state summaries; text-only theme selection despite the D-210 visual language; bare free-text `HH:mm` fields; debug-toolbar export/backup behaviour with an icon-only Share; six hand-copied card shapes; and the unimplemented portions of the docs/26 Settings map (no reminders surface, thin delete-all disclosure). Research against Material Design settings guidance ("secondary text conveys the current state"; version info belongs outside Settings), Apple HIG and design-system references confirmed the owner's proposed "press the button and the corresponding settings appear" as the standard mobile pattern (bottom sheet on phones; list-detail only on wide screens).

- **Structure implemented (owner option A):** the hub is now a short list in the Material settings pattern — Appearance, Week starts, Quiet hours (Everyday); Journal export, Back up or restore, Privacy, Removed habits (Your data); About; and Delete-all as its own isolated row. Every row shows its current state as secondary text and opens the corresponding panel on tap: `ModalBottomSheet` for the light groups, the full `BackupRestorePanel` sub-screen (with `BackHandler`) for backup/restore.
- **Copy hygiene:** internal labels `(D-204 / D-207)`, `(D-193)`, `.dayseed.json`, `AES-256-GCM`, `Schema v2 • 12 Tables`, `Danger Zone`, `12 database tables` and similar are gone from the UI; versions/licensing moved to the About panel per Material guidance.
- **Behaviour additions:** quiet-hours entry is validated with inline errors and normalised to strict `HH:mm` before saving (replacing unvalidated free text); Appearance shows a live D-210 `DaypartArtwork` preview for the selected mode; permanent habit deletion now requires a confirmation dialog (docs/26 destructive-action contract); the delete-all disclosure is expanded to the D-203-aligned scope + external-copies wording; restore failure surfaces an inline message instead of silently keeping the dialog open; Removed habits gained an empty state.
- **New files:** `ui/screens/SettingsPanels.kt`, `ui/screens/SettingsSupport.kt`, `ui/components/SettingsGroup.kt`, `app/src/test/java/com/example/dayseed/DayseedSettingsValidationTest.kt`. `SettingsScreen.kt` rewritten. All pre-existing test tags (`settings_screen`, `theme_chip_*`, `week_start_*`, `export_journal_*_btn`, `backup_password_input`, `export_data_btn`, `open_restore_dialog_btn`, `restore_json_input`, `confirm_restore_btn`, `delete_all_data_btn`, `perm_delete_habit_*`, `conflict_card_*`, `resolve_*`) are preserved; new tags added for hub rows and panels.
- **Verification evidence (executed):** targeted `DayseedSettingsValidationTest` run first **FAILED** 1/7 (NPE — the test asserted "0:5" parses while the strict two-digit-minute contract correctly rejects it; the test was tightened); then `:app:testDebugUnitTest --rerun` **PASSED 26/26** total (`DayseedAcceptanceTest` 12 + `DayseedVisualResolutionTest` 7 + `DayseedSettingsValidationTest` 7), 0 failures/errors/skipped, Gradle 9.3.1, JDK 21.0.13, Windows 11.
- **Not claimed / open:** rendered-device review, TalkBack traversal and ordering, 200% text scaling, sheet focus behaviour and contrast remain unverified Stage C obligations; the native time-picker was deferred in favour of validated text fields (conservative D-196 default); the full reminders settings surface required by docs/26 remains unimplemented pending a separate owner decision; the drafted delete-all/privacy wording awaits owner copy review. Stage C and Stage D are not claimed.

## Four-section Settings arrangement refinement implemented — 2026-10-31 (D-214; renumbered from workspace D-213)

Following the owner's second Settings audit request ("Audit the settings and create a better way and arrangement of how the settings should be") and explicit Planning Mode choices:

- **Four-Section Soft Botanical Hub (`SettingsScreen.kt`, `SettingsGroup.kt`):**
  1. **Appearance & rhythm:** Inline visual theme swatch cards (`InlineAppearanceSelector` with per-palette canvas/accent dots + active check) and live `DaypartArtwork` atmosphere preview, plus an inline Monday/Sunday segmented toggle (`InlineWeekStartSelector`) directly on the hub card.
  2. **Habits & gentle prompts:** Moves `Removed habits` (paused/archived habits with a live count pill) out of Data and pairs it with `Quiet hours & reminders`.
  3. **Data, backup & journal copy:** Displays a live on-device data summary subtitle on the hub and routes both `Back up or restore` and `Read a copy of your journal` into the unified Data sub-screen with section focus.
  4. **Trust & about:** Pairs `How Dayseed protects your privacy` with `About Dayseed`, followed by a clearly framed `Reset on-device data` card.
- **Quiet Hours Presets, Steppers & Reminder Guidance (`SettingsPanels.kt`, `SettingsSupport.kt`):**
  - Added 4 one-tap preset chips (`21:00 – 08:00`, `22:00 – 07:00`, `23:00 – 07:30`, `20:00 – 06:30`), `-30m` / `+30m` time-stepper buttons (`stepQuietHours` wrapping cleanly across midnight), and a reminder status card explaining default `09:00` habit reminder times, the 3/day cap, and Android notification deferral.
- **Unified Data, Backup & Journal Sub-Screen (`SettingsPanels.kt`, `SettingsSupport.kt`):**
  - Added an **On-device data summary banner** (`dataSummaryLine`), masked password input with visibility toggle and optional confirmation check (`backupPasswordMismatchError`), a **Paste from clipboard** button and live pre-restore payload inspection (`inspectBackupPayloadSummary`) in the Restore dialog, and direct Android Share actions for both portable backups and Markdown journal exports.
- **Verification Evidence (executed):**
  - `compile_applet`: **PASSED**.
  - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 29/29** tests (`DayseedAcceptanceTest` 12 + `DayseedVisualResolutionTest` 7 + `DayseedSettingsValidationTest` 10), 0 failures/errors/skipped (XML report at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.DayseedSettingsValidationTest.xml`, timestamp `2026-10-31T21:57:40.108Z`, Gradle 9.3.1, Temurin JDK 21, Android SDK 36).
- **Not claimed:** rendered-device visual review, TalkBack traversal/focus order, 200% font scaling, and Stage C / Stage D validation remain open.

## Full Stage B initial-v1 implementation authorised & Phase 0 Preflight completed — 2026-11-01 (D-213, D-215–D-218)

- **Owner authorisation (`D-213`):** On 2026-11-01, after reviewing the system-wide native Android application audit and consolidated Stage B implementation plan (`implementation_plan.md`, PR #3 on branch `planning/system-wide-audit-implementation-plan-2026-11-01`, commit `d4c1be93ef953ea56f704a07304a0018fd7d1cdd`), the owner explicitly authorised the complete Stage B implementation for initial-v1 across Phase 0 and Phases 1–5.
- **Repository & workspace reconciliation:**
  - Inspected `origin/main` (`334679775643ffb43f384646cf7cced75d401f96`) and PR #3 (`d4c1be93ef953ea56f704a07304a0018fd7d1cdd`).
  - Synced the canonical 807-line `implementation_plan.md` from PR #3 into the workspace root and appended Section 11 (`Phase 0 Preflight & Section 5 Reconciliation Addendum`).
  - Preserved all uncommitted 2026-10-31 Settings refinement changes and renumbered that workspace decision from `D-213` to `D-214` to prevent any ID collision with `D-213` (Full Stage B authorisation).
  - Reconciled `AGENTS.md`, `.opencode/OWNER_AUTHORITY.md`, `README.md` (correcting stale v1-only schema claims to reflect `DATABASE_VERSION = 2` and `migrateV1ToV2FailClosed`), `docs/09`, `docs/12`, `docs/19`, `docs/25`, `docs/27`, `docs/28`, `docs/32`, and `docs/35` (fixing the contradictory top status line).
- **Section 5 product decisions resolved with owner (`D-215`–`D-218`):**
  - **`D-215` (`§5.1` Navigation):** Approved 5-tab bottom navigation (`Today`, `Habits`, `Journal`, `Insights`, `Settings`), superseding 4-tab `N1`, with historical habit calendar inside `Insights` and `Day Detail` / `Habit Detail` as secondary routes.
  - **`D-216` (`§5.2` Measurement modes):** Approved `Quantity`, `Duration`, and `Simple check-off` (`COMPLETION_ONLY`) for new habits with automatic `D-040` progress-band derivation (`Minimal`, `Partial`, `Target met`, `Extra` `>100%..150%`, `Huge` `>150%`) and lossless legacy `EFFORT_AWARE` / `EFFORT_LEVELS` preservation.
  - **`D-217` (`§5.3` Streaks, explicit zero & Grove):** Approved individual per-habit streaks/branches/colours, `Today` longest-active-streak display using elapsed active-run span with explicit `days`/`weeks` unit, streak-holding on positive partial progress or rest, neutral explicit `0`, and the permanent Grove (`MISSED`, `PAUSED`, `ARCHIVED`, `COMPLETED`).
  - **`D-218` (`§5.4` One-off rest day):** Approved one-off `Take a rest day` (`REST_DAY`) date exception holding a habit's streak neutrally without fake check-ins or weekly-target reduction.
- **Phase 0 baseline verification evidence (executed 2026-11-01, Stage B self-check):**
  - `compile_applet`: **PASSED**.
  - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 30/30** tests (`DayseedAcceptanceTest` 12 [XML `2026-11-01T19:26:16.603Z`] + `DayseedVisualResolutionTest` 8 [XML `2026-11-01T19:27:30.634Z`] + `DayseedSettingsValidationTest` 10 [XML `2026-11-01T19:27:30.616Z`]), 0 failures/errors/skipped, `BUILD SUCCESSFUL in 2m 19s`, Gradle `9.3.1`, Temurin JDK `21.0.13+9-LTS`, Linux `6.9.12 amd64`.
- **Stage gates:** Stage B is **AUTHORISED** (`D-213`); Stage C (independent technical validation) remains **NOT AUTHORISED** and Stage D (final owner acceptance) remains **NOT OPEN**.

## Phase 0 Reconciliation & Verified Exit Report against `main` `ebeca3eb` — 2026-11-01 (`docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md`)

Following the owner's commit of the workspace changes to GitHub `main` at **`ebeca3ebff1155c220f8e09f1e00e07fdb28209b`** (`docs: authorize stage b implementation`) and closure of PR #3 (`planning/system-wide-audit-implementation-plan-2026-11-01`) as superseded, a full reconciliation pass was executed and recorded in **[docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md](37-PHASE-0-PREFLIGHT-RECONCILIATION.md)**:

- **Workspace vs. Remote `main` (`ebeca3ebff1155c220f8e09f1e00e07fdb28209b`):** Verified 0 file differences between `/app/applet` and `origin/main` at `2026-11-01T19:56:47Z` (while noting `/app/applet` has no local `.git` metadata directory).
- **Corrected Build & Schema Inventories:**
  - Corrected declared build configuration from `gradle/libs.versions.toml` and `app/build.gradle.kts`: `AGP = 9.1.1`, `Kotlin = 2.2.10`, `Compose BOM = 2024.09.00`, `Robolectric = 4.14.1`, `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`, `namespace = "com.example.dayseed"`, `applicationId = "com.aistudio.dayseed.wkqmfl"`, `Java = 21` (distinguished from container-installed `Gradle 9.3.1` and `Temurin JDK 21.0.13+9-LTS`; no checked-in Gradle wrapper in GitHub).
  - Corrected the 12-table v2 schema inventory in `DayseedDatabaseHelper.kt` (`habits`, `habit_versions`, `habit_entries`, `habit_day_outcomes`, `habit_date_notes`, `journal_entries`, `journal_drafts`, `app_settings`, `reminder_deliveries`, `restore_sessions`, `restore_conflicts`, `notification_cancel_outbox`), removing erroneous references to draft names (`app_metadata`, `user_settings`, `habit_pauses`, `journal_entry_habits`).
- **`D-214` Commit-History Qualification:** Qualified commit `ebeca3ebff1155c220f8e09f1e00e07fdb28209b` as containing both the 5 previously authorised `D-214` Settings code/test files (authored `2026-10-31`) and the 14 Phase 0 documentation/planning files (`2026-11-01`).
- **Freshly Executed Baseline Verification (`2026-11-01T19:57:26Z` – `2026-11-01T19:57:50Z`):**
  - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 30/30** (`DayseedAcceptanceTest` 12 [XML `2026-11-01T19:57:44.002Z`] + `DayseedSettingsValidationTest` 10 [XML `2026-11-01T19:57:50.753Z`] + `DayseedVisualResolutionTest` 8 [XML `2026-11-01T19:57:50.776Z`]), 0 failures/errors/skipped (`BUILD SUCCESSFUL in 24s`).
  - `compile_applet`: **PASSED**.
- **Remaining Open Owner Decision (`D-040` vs. `D-216` Progress-Band Labels):** Phase 0 exit status is recorded as **`PARTIAL`** solely because `D-040` defines 7 positive progress bands (`Tiny`, `Small`, `Halfway`, `Nearly there`, `Goal met`, `Extra`, `Huge`) whereas the initial `D-216` summary parenthetically listed 5 labels (`Minimal`, `Partial`, `Target met`, `Extra`, `Huge`). All Phase 1 data-integrity, effective-dating (`D-194`), flexible weekly target, `REST_DAY` (`D-218`), individual streak/Grove (`D-217`), neutral zero, and schema v2-to-v3 migration work is ready to start immediately; changing user-visible `ProgressBand` enum labels (`AUD-008`, `AUD-029`) awaits the owner's confirmation on the `D-040` / `D-216` item.

## Phase 1 Implementation & Corrective Completion — 2026-11-02 / 2026-11-03 (`D-040`, `D-216` Option A, `D-217`–`D-227`, `U-1`–`U-13`)

Following the owner's confirmation of `D-216` Option A (preserving all seven `D-040` progress-band labels and pairing each band with both an encouragement quote and a distinct motivation quote), approval of the Phase 1 Implementation Plan (Revision 7), and approval of the **Phase 1 Corrective Completion Plan (Revised)**:

- **Inspection Baseline:**
  - **Repository & Branch:** `HeWhoArose/Dayseed`, branch `main` (carried forward from inspected baseline `097f314e068d4f30db78ba4728729e8e01f10737` / `ebeca3ebff1155c220f8e09f1e00e07fdb28209b`; the AI Studio container workspace `/app/applet` has no `.git` directory).
  - **Inspected & Modified Files:**
    - `app/src/main/java/com/example/dayseed/data/Models.kt`
    - `app/src/main/java/com/example/dayseed/data/StreakAndGroveEngine.kt`
    - `app/src/main/java/com/example/dayseed/data/DayseedDatabaseHelper.kt`
    - `app/src/main/java/com/example/dayseed/data/DayseedRepository.kt`
    - `app/src/test/java/com/example/dayseed/DayseedAcceptanceTest.kt`
    - `app/src/test/java/com/example/dayseed/Phase1ImplementationTest.kt`
    - `implementation_plan.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/12-PROGRESS-AND-HANDOVER.md`
- **Summary of Phase 1 & Corrective Changes Implemented:**
  1. **Correction A — `D-040` / `D-216` Progress Classification, Labels, Fractional-Safe Badges & Dual Quotes (`Models.kt`):**
     - Fixed the `150%` boundary in `ProgressBandClassifier.classify` (`scaled <= t150 -> ProgressBand.EXTRA`) so exact `150%` (`150/100`, `3/2`, `300/200`) classifies as `ProgressBand.EXTRA` and strictly `> 150%` classifies as `ProgressBand.HUGE`.
     - Updated `ProgressBand` visible `label` strings (`Zero logged`, `Tiny`, `Small`, `Halfway`, `Nearly there`, `Goal met`, `Extra`, `Huge`) and fractional-safe `shortBadge` strings (`0%`, `>0–<25%`, `25–<50%`, `50–<75%`, `75–<100%`, `100%`, `>100–150%`, `>150%`), while preserving distinct non-empty `encouragementQuote` and `motivationQuote` strings on every band and keeping `CompactHistoryBucket` separate.
  2. **Correction B — SQLite v3 (`DATABASE_VERSION = 3`, `D-219`), Chained `v1 -> v2 -> v3` Migration, `U-11` Weekly Conversion & In-Place v3 Schema Reconciliation (`DayseedDatabaseHelper.kt`):**
     - Updated `buildHabitVersionsTableSql` so weekly `COMPLETION_ONLY` and `EFFORT_AWARE` habits (`target_period = 'WEEK'`) can persist `target_value >= 1` (`N days/week`) while daily check-off retains `target_value IS NULL`.
     - Extracted `buildRestoreConflictsTableSql` with `'habit_streak_run'` in `record_type IN (...)`.
     - Updated `migrateV1ToV2FailClosed` so v1 `QUANTITY` and `DURATION` daily records migrate into `habit_entries` (`value_units = 1`) rather than `habit_day_outcomes`, and archived v1 habits with historical records receive an `ACTIVE` effective version covering their historical record dates and an `ARCHIVED` effective version on the day after their latest record.
     - Updated `migrateV2ToV3FailClosed` and `validateV2DataBeforeMigration` to convert legacy v2 weekly schedules `(DAILY, WEEK)` and `(SELECTED_WEEKDAYS, mask, WEEK)` into `(FLEXIBLE_WEEKLY_TARGET, WEEK)` (`U-11`) and fail-closed on malformed v2 `habit_versions` schedule/period/mask/target rows.
     - Implemented `reconcileExistingV3SchemaIfNeeded(db)` in `onOpen(db)` to idempotently and transactionally upgrade any existing v3 database (`DATABASE_VERSION = 3` per `D-219`) that still has the older `habit_versions` or `restore_conflicts` CHECK constraints, rolling back cleanly on failure.
  3. **Correction C — Complete v3 Portable Backup, `REPLACE`, `MERGE` & Per-Conflict Destructive Authorization (`DayseedRepository.kt`, `StreakAndGroveEngine.kt`):**
     - Preserved `targetValue >= 1` in `saveHabitSync` for weekly `COMPLETION_ONLY` and `EFFORT_AWARE` habits.
     - Updated `MutableRun` in `StreakAndGroveEngine.kt` to preserve prior `qualifiedAtUtcMs` when `activeSpanDays >= 7` and reset `qualifiedForGrove = false` / `qualifiedAtUtcMs = null` when a previously qualified run shrinks below 7 days.
     - Updated `REPLACE` (`importBackupArraysDirect`) to import valid `habit_streak_runs` (preserving original `run_id`, `created_at_utc_ms`, and `qualified_at_utc_ms` across recalculation) and seed `week_start_history` for legacy v2 backups at the earliest restored local date using the restored `week_start` setting without overwriting `week_start`.
     - Updated `MERGE` (`mergeBackupWithConflictDetection`) and `resolveRestoreConflictSync` to detect and resolve conflicts across `"habit"`, `"habit_date_exception"` (requiring explicit per-conflict `allowDestructiveActivityRemoval == true && authorizedDestructiveConflictId == conflictId` before `USE_BACKUP` removes local activity on that date), `"habit_streak_run"`, `"week_start_history"`, and `"journal_draft"`, while disallowing `KEEP_BOTH` for those five record types.
- **Verification Evidence (Executed `2026-11-03T06:29:54Z` – `2026-11-03T06:30:31Z`, Stage B Self-Check):**
  - `compile_applet`: **PASSED** (`Build succeeded - the applet is compiled`).
  - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 57/57** executed JUnit test methods across all 4 test suites, **0 failed, 0 errors, 0 skipped** (`BUILD SUCCESSFUL in 31s`, Gradle `9.3.1`, Temurin JDK `21.0.13+9-LTS`, Robolectric `4.14.1` SDK 34):
    - `com.example.dayseed.Phase1ImplementationTest`: **27/27 PASSED** (`10.118s`, XML timestamp `2026-11-03T06:30:19.975Z`, covering `T1-01` through `T1-42` at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.Phase1ImplementationTest.xml`).
    - `com.example.dayseed.DayseedAcceptanceTest`: **12/12 PASSED** (`1.386s`, XML timestamp `2026-11-03T06:30:30.118Z`, at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.DayseedAcceptanceTest.xml`).
    - `com.example.dayseed.DayseedSettingsValidationTest`: **10/10 PASSED** (`0.011s`, XML timestamp `2026-11-03T06:30:31.510Z`, at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.DayseedSettingsValidationTest.xml`).
    - `com.example.dayseed.DayseedVisualResolutionTest`: **8/8 PASSED** (`0.060s`, XML timestamp `2026-11-03T06:30:31.525Z`, at `app/build/test-results/testDebugUnitTest/TEST-com.example.dayseed.DayseedVisualResolutionTest.xml`).
- **Stage Gate Status:** Stage B Phase 1 implementation and corrective completion self-checks are **COMPLETE**. **Stage C (independent technical validation)** and **Stage D (final owner acceptance / release readiness)** remain **NOT OPEN**.

## Phase 2 Implementation Completion — 2026-11-03 (`D-213`, `D-215`–`D-219`, Stage B Self-Check)

Following the owner's Phase 2 Implementation Assignment (`Complete Habit Experience, Five-Destination Navigation, Individual Streaks and Living Grove`) on top of the `bf19bc11c37dd9a7929594871266bdff07dfc94e` baseline:

- **Summary of Phase 2 Changes Implemented:**
  1. **Five-Destination Bottom Navigation & Deterministic Secondary Route Stack (`DayseedViewModel.kt`, `MainActivity.kt`, `HistoryScreen.kt`):**
     - Replaced the 4-tab bottom bar with the approved 5-destination navigation (`Today`, `Habits`, `Journal`, `Insights`, `Settings` under `D-215`).
     - Added top-level `JournalDestinationScreen` (`Journal` tab) and retained `Insights` (`Calendar & history`, `Mood insights`, `Statistics`) and `Settings` (`D-214`).
     - Implemented the deterministic `SecondaryRoute` stack (`HabitDetail`, `CreateEditHabit`, `RecordSession`, `LivingGrove`, `CollectedMomentsDetail`) with unified `BackHandler` and top-bar Back navigation that restores the exact parent tab, secondary route stack, selected date, `Habits` filter/search/scroll state, and `Today` scroll state without resetting to `Today`.
  2. **Refactored `Today` Screen, `HabitCard` & `CollectedMomentsDetailSheet` (`TodayScreen.kt`, `HabitCard.kt`, `GrowthMark.kt`, `Models.kt`, `DayseedRepository.kt`):**
     - Preserved `D-210` / `D-211` daypart header artwork (`DaypartHeaderBanner`) and added the calm longest active streak indicator pill (`today_longest_streak_indicator`) using `selectLongestActiveRun()` with explicit `"days"` vs `"weeks"` unit labels and deterministic tie-breaking (`activeSpanDays` desc → `completedStreakCount` desc → `startedOnLocalDate` asc → `sortOrder` asc → `habitId` asc).
     - Separated scheduled habits from a collapsible `"Not scheduled today"` section (`not_scheduled_today_section`) supporting extra/unscheduled logging (`U-2`, `U-7`).
     - Refactored `HabitCard` (`W1` action-focused hierarchy) with derived `D-040` progress bands, distinct `encouragementQuote` and `motivationQuote` (`D-216` Option A), explicit weekly (`X / Y units this week` or `X / N days this week`) vs daily progress, and 350ms rapid-tap idempotency protection (`lastQuickActionIdempotencyKey`).
     - Made the `Collected moments` (`G2`) card clickable (`collected_moments_card`) to open `CollectedMomentsDetailSheet` (`G3` month selector, recorded duration total, active days, session count, and per-session drill-down to `RecordSession` or `HabitDetail`).
  3. **Dedicated `Habits` Directory (`HabitsScreen.kt`, `DayseedRepository.kt`):**
     - Implemented lifecycle filter chips (`All`, `Active`, `Paused`, `Completed`, `Archived`), real-time name/category search (`habits_search_input`), contiguous `0..N-1` `sort_order` manual reordering (`reorderHabitSync`), and empty states.
  4. **Full-Screen `CreateEditHabitScreen` (`CreateEditHabitScreen.kt`):**
     - Replaced the legacy modal dialog with a full-screen form enforcing `D-216` mode gating (`Quantity`, `Duration`, `Simple check-off` for new habits while preserving legacy `EFFORT_AWARE` edits), `1..7 days/week` check-off target validation, `D-195` weekday bitmasks, optional `category` and `lifetimeGoalDays`, `D-194` `Apply today` vs `Apply tomorrow` selector when today has recorded activity, and `U-12` Option B weekly freeze notice.
  5. **Focused `RecordSessionSheet` & Single-Entry Guarded Undo (`RecordSessionSheet.kt`, `DayseedRepository.kt`):**
     - Enforced strict `D-188` date bounds (`earliestEffectiveDate <= selectedDate <= todayLocal()`) in both UI and repository (`logHabitEntrySync`, `logHabitDayOutcomeSync`).
     - Added `+1`, `+5`, `+10`, and `0 (Honest check-in)` quick chips, optional per-entry notes, individual session editing (`updateHabitEntrySync`), and single-entry deletion with an 8-second guarded `UndoToken` (`deleteHabitEntryWithUndoSync`, including a collision guard when a `REST_DAY` exception is added before Undo).
  6. **`HabitDetailScreen` & `LivingGroveScreen` (`HabitDetailScreen.kt`, `LivingGroveScreen.kt`):**
     - Implemented `HabitDetailScreen` with 6 structured sections: Identity & Configuration, Current Period Progress + dual quotes, Selected-Date Sessions/Notes/One-Off `Take a rest day` (on eligible `Today` dates), Recent 14-Day Strip, Streak & Grove History separating qualified Grove trees (`>= 7` active span days) from short runs (`1–6` span days, `UNQUALIFIED_HISTORY`), and Lifecycle Controls (`Pause`, `Resume`, `Mark as Completed`, `Archive`, and confirmed `Delete Permanently` preserving linked Journal text without snackbar Undo).
     - Implemented `LivingGroveScreen` with summary metrics, filter chips (`All`, `Growing`, `Dormant`, `Completed`, `Saplings`), custom Compose Canvas botanical illustrations (`GroveTreeIllustration` for saplings `1–6d`, growing trees `>= 7d`, dormant trees, and completed trees with reduced-motion support), and one-shot dormant transition acknowledgment persisted in `app_settings` (`acknowledged_terminal_run_ids`).
  7. **Human Validation Register & Canonical Documentation (`docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`):**
     - Appended Section 6 (`P2-H01` through `P2-H17`, all marked `NOT RUN`) and Section 7 (`P2-C01` through `P2-C09`, all marked `NOT RUN`) to `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`.
- **Phase 2 Final Audit Remediations (`F-01` through `F-06`, 2026-11-04):**
  1. **`F-01` (`HabitCard.kt`, `TodayScreen.kt`, `HistoryScreen.kt`):** Removed duplicate 3-dot lifecycle/edit menus, inline custom amount fields, inline date-note editors, individual session-row edit/delete buttons, and direct existing-entry editing from `HabitCard.kt` (leaving a compact read-only session count summary and a dedicated `Record` button opening `RecordSessionSheet(entryId = null)`). Implemented `isScheduledHabitTendedToday` and `computeTendedTodayCount` in `TodayScreen.kt` so each scheduled habit is counted at most once when it has a daily outcome today, a positive entry today (`valueUnits > 0`), a `REST_DAY` exception today, or a satisfied flexible weekly target (`(targetPeriod == WEEK || scheduleKind == FLEXIBLE_WEEKLY_TARGET) && weekTargetUnits > BigInteger.ZERO && weekTotalUnits >= weekTargetUnits`).
  2. **`F-02` (`DayseedRepository.kt`, `DayseedViewModel.kt`, `CreateEditHabitScreen.kt`, `HabitDetailScreen.kt`):** Confirmed `AddEditHabitDialog.kt` remains deleted and `CreateEditHabitScreen` is the sole habit editor. Replaced `deleteHabitPermanentlySync(habitId)` so permanent habit deletion performs atomic SQLite deletion, notification cancellation outbox enqueueing/processing, and Journal/draft unlinking (`linked_habit_id = NULL`) without calling `deleteHabitWithUndoSync()` or generating a `HABIT_DELETION` `UndoToken`, while selectively invalidating `_activeUndoToken` if it belongs to a session/record of the deleted `habitId` and preserving unrelated `UndoToken`s.
  3. **`F-03` (`RecordSessionSheet.kt`, `DayseedRepository.kt`, `DayseedViewModel.kt`, `Models.kt`):** Added `resolveRecordSessionGoverningVersion` and `DayseedRepository.getGoverningHabitVersion` checking `targetPeriod == TargetPeriod.WEEK || scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET` (`StreakAndGroveEngine.resolveFrozenWeekVersion` for weekly habits under `U-12 Option B`; `StreakAndGroveEngine.resolveEffectiveVersion` for daily habits) and wired `governingVersion` across the `RecordSessionSheet` header subtitle, unit label, target value, default `valueText`, preset chips, daily/weekly previews, and `DayseedViewModel.logHabit` save routing.
  4. **`F-04` (`CreateEditHabitScreen.kt`, `DayseedRepository.kt`):** Added a `Clear` control (`habit_clear_preferred_weekdays_btn`) and `weeklyTargetSemanticsExplanation` (`habit_weekly_semantics_explanation`) on `FLEXIBLE_WEEKLY_TARGET`, and updated `saveHabitSync` and `loadHabitsSync` so an empty preferred-weekday selection persists `preferred_weekdays_mask = NULL` and reloads `scheduleDays = emptySet()`.
  5. **`F-05` (`CreateEditHabitScreen.kt`, `SettingsSupport.kt`):** Wired `onClearSaveError()` across all editable fields in `CreateEditHabitScreen.kt` and enforced strict two-digit 24-hour `HH:mm` validation (`00:00`–`23:59`, rejecting single-digit hours like `9:05` as well as `9:5`, `24:00`, and `12:60`) via `SettingsSupport.parseStrictHhMm` when `reminderEnabled == true` (`habit_reminder_time_error`) without modifying `SettingsSupport.parseQuietHours`.
  6. **`F-06` (`GroveComponents.kt`, `TodayScreen.kt`):** Updated `TodayLongestStreakIndicator` so tapping the card (`today_main_streak_indicator`) or primary `Grove` button (`today_open_grove_btn`) opens `SecondaryRoute.LivingGrove(selectedHabitId = null)` (all habits) and tapping the secondary `Habit` button (`today_longest_streak_habit_btn`) opens `SecondaryRoute.HabitDetail(leadHabitId)`.
- **Verification Evidence (Executed `2026-11-04T16:10:15Z` – `2026-11-04T16:10:44Z`, Stage B Self-Check):**
  - `compile_applet` (`:app:assembleDebug`): **PASSED**.
  - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 90/90** executed JUnit test methods across all 5 test classes, **0 failed, 0 errors, 0 skipped** (`BUILD SUCCESSFUL`, Gradle `9.3.1`, Temurin JDK `21.0.13+9-LTS`, Robolectric `4.14.1` SDK 34):
    - `com.example.dayseed.Phase2ImplementationTest`: **31/31 PASSED** (`10.058s`, XML timestamp `2026-11-04T16:10:31.029Z`, covering `T2-01`..`T2-51` and `t2_c01`..`t2_c09`).
    - `com.example.dayseed.Phase1ImplementationTest`: **29/29 PASSED** (`1.574s`, XML timestamp `2026-11-04T16:10:42.213Z`, covering `T1-01`..`T1-44`).
    - `com.example.dayseed.DayseedAcceptanceTest`: **12/12 PASSED** (`1.034s`, XML timestamp `2026-11-04T16:10:41.101Z`).
    - `com.example.dayseed.DayseedSettingsValidationTest`: **10/10 PASSED** (`0.005s`, XML timestamp `2026-11-04T16:10:42.139Z`).
    - `com.example.dayseed.DayseedVisualResolutionTest`: **8/8 PASSED** (`0.029s`, XML timestamp `2026-11-04T16:10:42.145Z`).
- **Stage Gate Status:** Stage B Phase 2 implementation, targeted remediations (`Corrections A–D`), and final audit remediations (`F-01`–`F-06`) are **COMPLETE**. Human/device checks (`P1-H01`–`P1-H27`, `P2-H01`–`P2-H17`, and `P2-C01`–`P2-C09`) remain **`NOT RUN`**. **Stage C (independent technical validation)** and **Stage D (final owner acceptance / release readiness)** remain **NOT OPEN**.




## Phase 2.5 Implementation — Appearance, Reminders Panel & Manual File Backup — 2026-11-04 (D-220–D-224; Stage B Self-Check)

- **Scope (assignment executed in three sequenced parts):** (A) audit confirmed the canonical Today screen is unique (`MainActivity -> NavTab.TODAY -> TodayScreen`, sole call site `MainActivity.kt:261`) and nothing was deleted; (B) delivered the fifth appearance option **`DARK_DAYPART_ART`** ("Dark + Time-of-Day Artwork": fixed dark palette at every hour, all four daypart illustrations still following local time through the shared `SoftBotanicalThemeConfig` clock) with live 30-second Settings preview and fifth swatch chip; (C) completed the Settings audit surfaces under the owner's explicit 2026-11-04 rulings: a real **Notifications & reminders** panel whose activation switches stay **disabled until physical-device delivery verification passes** (honest "delivery is not active yet" copy), an inexact-`AlarmManager` + WorkManager-reconciliation scheduler (no exact-alarm permission; copy never promises exact times), **manual SAF file backup save/restore with mandatory read-back verification and pre-restore metadata**, clipboard routes retained, and **automatic file backup DEFERRED** as a separate recorded proposal (D-223).
- **Key files:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/DaypartVisuals.kt`, `ui/screens/SettingsPanels.kt`, `ui/screens/SettingsSupport.kt`, `data/ReminderScheduler.kt`, `data/ReminderAlarmReceiver.kt`, `data/ReminderReconcileWorker.kt`, `data/ReminderSystemEventReceiver.kt`, `data/DayseedRepository.kt`, `data/ReminderEngine.kt`, `AndroidManifest.xml`, `gradle/libs.versions.toml`, `app/build.gradle.kts`.
- **Test-infra fix (Windows-only, this workstation):** bisected on the clean baseline — `t1_26_to_31_…` failed with `SQLiteCantOpenDatabaseException` because Robolectric sandbox paths (which embed the test method name) exceeded the legacy Windows MAX_PATH 260 limit (≈273 chars) at `%TEMP%`; fixed by redirecting the unit-test JVM `java.io.tmpdir` to a short directory in `app/build.gradle.kts` (overridable via `-ProbolectricTempDir=` or `DAYSEED_ROBOLECTRIC_TMP`). Not a product change and not a Phase 2.5 regression.
- **Executed evidence (Stage B self-check, `2026-11-04T22:21:55+01:00` – `22:32:46+01:00`, workspace baseline `e4ed178`):** `gradle :app:testDebugUnitTest :app:assembleDebug --rerun-tasks` → **PASSED 110/110** unit tests across 7 classes (0 failed, 0 errors, 0 skipped), including the new/extended `DayseedVisualResolutionTest` 15/15 (+7 `DARK_DAYPART_ART`), `DayseedAppearanceSettingsTest` 4/4, and `DayseedReminderAndBackupTest` 9/9; `:app:assembleDebug` **PASSED** after a local gitignored debug keystore was generated (first invocation failed only at `:app:validateSigningDebug` on the missing keystore file, an environment issue) — artifact `app-debug.apk` 18,848,132 bytes. Details in `docs/28` Phase 2.5 traceability entry and `docs/09` D-220–D-224.
- **Stage gate status:** Phase 2.5 Stage B implementation steps are **COMPLETE**. All device/human checks — including `P25-H03` (the gating reminder-delivery check) — remain **`NOT RUN`** (`docs/38` §8, `P25-H01`–`P25-H07`). **Stage C** and **Stage D** remain **NOT OPEN** and require separate owner gates.

## Phase 3 Implementation — Journal, Shared Calendar, Weekly Board, Growth Mark and Insights — 2026-11-08/09 (Stage B Self-Check)

Following the owner's Phase 3 Implementation Assignment (`Dayseed Phase 3 — Journal, Shared Calendar, Weekly Board, Growth Mark and Insights`), Planning Mode UX confirmations, Phase 3 Plan Review Amendments (Revisions 1–4), and the post-commit Phase 3 reviews against GitHub `main` at `dd6501f69cac9558e5423eca873e7ab1da469594` (`dd6501f`, committed on top of preceding baselines `f0d8c32ac1281ca89050cf0f60ce42bfb0e007ea` [`f0d8c32`], `8a282150db3a48be8ab85ff21ca6df871eda4e7c` [`8a28215`], and `86d3e65c3165bdc4ce8cd92d570547efc61feb39`):

- **Inspection Baseline:**
  - **Repository & Branch:** `HeWhoArose/Dayseed`, canonical branch `main`.
  - **Inspected Remote Commit SHA:** `dd6501f69cac9558e5423eca873e7ab1da469594` (`dd6501f`, with preceding Phase 3 baselines `f0d8c32ac1281ca89050cf0f60ce42bfb0e007ea` [`f0d8c32`] and `8a282150db3a48be8ab85ff21ca6df871eda4e7c` [`8a28215`] on top of `86d3e65c3165bdc4ce8cd92d570547efc61feb39`).
  - **Imported Workspace / Base SHA:** `UNKNOWN` (AI Studio container `/app/applet` has no `.git` metadata directory; verified file tree matches `dd6501f69cac9558e5423eca873e7ab1da469594`).
- **Summary of Phase 3 Changes Implemented:**
  1. **Workstream A — Dedicated `Journal` Destination (`A2` Archive + Collapsible Date-Jump Calendar), Two-Phase Race-Free Draft Promotion, Cursor Persistence & Save-Failure Handling (`HistoryScreen.kt`, `JournalEditorDialog.kt`, `ReflectionCard.kt`, `TodayScreen.kt`, `DayseedRepository.kt`, `DayseedViewModel.kt`, `MainActivity.kt`):**
     - Refactored `HistoryScreen.kt` into a pure, dedicated `Journal` archive destination (`A2` hybrid timeline with a collapsible date-jump month calendar `journal_date_jump_toggle` / `journal_date_jump_calendar`, prominent `New Entry` action `journal_add_entry_fab`, unsaved drafts section `journal_drafts_section`, local title/body search `journal_search_input`, primary-mood filter chips `journal_mood_filter_*`, and `Clear search` `journal_clear_filters_btn` / `journal_no_results_clear_btn`).
     - Preserved optional `intended_journal_entry_id = null` on newly created/autosaved drafts (`saveJournalDraftSync`) and hardened `promoteDraftToJournalEntrySync(draftId)` into a two-phase race-free workflow using `withImmediateTransaction` (`db.beginTransactionNonExclusive()`):
       - **Phase 1 (`prepareDraftForSaveSync`):** Atomically assigns `intended_journal_entry_id` via `UPDATE journal_drafts SET intended_journal_entry_id = COALESCE(intended_journal_entry_id, ?), updated_at_utc_ms = ? WHERE draft_id = ? AND intended_journal_entry_id IS NULL`, re-reads the authoritative ID from SQLite, and commits durably before entry insertion.
       - **Phase 2 (`promoteDraftToJournalEntrySync`):** Inside `withImmediateTransaction`, re-reads the draft from SQLite, checks `SELECT * FROM journal_entries WHERE journal_entry_id = ?` first (returning the existing saved `JournalEntry` unchanged without overwriting if a previous attempt already committed), otherwise inserts into `journal_entries` with `SQLiteDatabase.CONFLICT_ABORT`, deletes only `WHERE draft_id = ?` from `journal_drafts`, and removes `setting_key = 'journal_draft_cursor_' || ?` from `app_settings` atomically.
     - Updated `saveJournalEntrySync` so editing an already-saved entry updates `WHERE journal_entry_id = ?` and never deletes unrelated drafts on that `local_date`.
     - Added cursor-selection persistence (`saveJournalDraftCursorSync` / `getJournalDraftCursorSync` under `app_settings` key `journal_draft_cursor_<draftId>`) and excluded `journal_draft_cursor_*` keys from portable backup export (`exportPortableBackupSync`), `REPLACE` restore (`importBackupArraysDirect`), and `MERGE` restore (`mergeBackupWithConflictDetection`).
     - Added `DayseedViewModel.lastJournalSaveError: StateFlow<String?>`, `clearJournalSaveError()`, and `DayseedRepository.injectedJournalSaveFailure` so failed Journal saves keep `JournalEditorDialog` open with entered content and draft intact, display `journal_save_error_banner`, and never emit false success snackbars or Undo tokens.
     - Updated `ReflectionCard` on `Today` to display multiple-entry count badges (`entriesCountForDate`) and a `+ Write another` button (`reflection_write_another_button`).
  2. **Workstream B & D — Shared Month Calendar (`SharedMonthCalendar`), 10 Honest Date States, Historical Habit Visibility, Mood/Journal Markers & Dedicated `DayDetailScreen` (`SharedCalendarComponents.kt`, `DayDetailScreen.kt`, `StreakAndGroveEngine.kt`, `Models.kt`, `DayseedRepository.kt`, `DayseedViewModel.kt`, `MainActivity.kt`):**
     - Created `SharedCalendarComponents.kt` (`SharedMonthCalendar`) with unrestricted previous/next month navigation (`*_prev_month_btn`, `*_next_month_btn`), `Jump to Today` (`*_jump_today_btn`), week-start-ordered headers (`getWeekStartDay`), All-Habits multi-dot mode and Single-Habit status-cell mode, primary-mood symbol, and Journal count marker (`✍️` / `✍️N`).
     - Implemented `getHabitsRelevantForDate(date)` and `getHabitsRelevantForDateRange(startDate, endDate)` so paused, completed, and archived habits remain visible on historical dates/weeks where they had an effective version or saved records.
     - Implemented `evaluateHonestCalendarDateState(habitId, date, referenceToday)` covering all 10 `HonestCalendarDateState` values (`COMPLETED_TARGET_MET`, `PARTIAL_OR_TINY_PROGRESS`, `REST_DAY_PLANNED_PAUSE`, `PAUSED_DATE_RANGE`, `UNSCHEDULED_DATE`, `FLEXIBLE_WEEKLY_IN_PROGRESS`, `EXPLICIT_ZERO_NOT_DONE`, `MISSED_SCHEDULED_DATE`, `FUTURE_DATE`, `NEUTRAL_TRANSITION_GAP`).
     - Added `SecondaryRoute.DayDetail` and `DayDetailScreen.kt` with `BackHandler` and top-bar Back restoration of parent screen, month/week, filter, and scroll state, plus direct drill-down to `HabitDetail`, `RecordSession` (new or specific `entryId`), and `JournalEditorDialog`.
     - **Post-Commit Review Fix 1 (`f0d8c32ac1281ca89050cf0f60ce42bfb0e007ea` — `StreakAndGroveEngine.evaluateDayRecordState`, `DayseedRepository.computeDayDetailSummary`, `Models.DayDetailHabitItem`, `DayDetailScreen.DayDetailHabitCard`):** For `TargetPeriod.WEEK` habits, `evaluateDayRecordState` and `computeDayDetailSummary` no longer classify an individual date's contribution against the full weekly `targetValue` (`progressBand = ProgressBand.ZERO`, `targetUnits = BigInteger.ZERO`, `percentage = 0`), while calculating the weekly result once using the frozen start-of-week `HabitVersion` (`computeWeeklyProgressSnapshot`) and exposing `weekTotalUnits`, `weekTargetUnits`, `weekPercentage`, `weekProgressBand`, and `weekProgressSummary`. `DayDetailHabitCard` renders `"Selected date contribution: ${item.totalUnits} $unitLabel"` without `/ ${item.targetUnits}` or a daily `D-040` band, and renders `weekProgressSummary` (`day_detail_habit_week_summary_${item.habit.habitId}`) distinctly below it, while preserving `TargetPeriod.DAY` `DayDetail` behaviour unchanged (`p3_13`).
     - **Post-Commit Review Fix 2 (`dd6501f69cac9558e5423eca873e7ab1da469594` — Legacy Weekly `EFFORT_AWARE` Contribution Consistency — `StreakAndGroveEngine.isQualifyingWeeklyEffortOutcome`, `DayseedRepository.computeDayDetailSummary`, `DayseedRepository.computeWeeklyBoard`, `DayDetailScreen.DayDetailHabitCard`):** Extracted `StreakAndGroveEngine.isQualifyingWeeklyEffortOutcome` (`outcomeKind == "COMPLETED" || (outcomeKind == "EFFORT_LOGGED" && effortLevel in setOf("USUAL", "EXTRA"))`) and used it in `computeWeeklyProgressSnapshot`, `computeDayDetailSummary`, and `computeWeeklyBoard` so a legacy weekly `EFFORT_AWARE` habit with `TINY` effort reports `totalUnits == BigInteger.ZERO` qualifying days while preserving recorded effort (`outcomes` non-empty, `DayRecordState.PARTIAL_PROGRESS`, `HonestCalendarDateState.PARTIAL_OR_TINY_PROGRESS`, `CompactHistoryBucket.PARTIAL`, and `"Selected date contribution: 0 days (TINY effort recorded)"` in `DayDetailHabitCard`), whereas `USUAL` and `EXTRA` outcomes report `BigInteger.ONE` qualifying day (`p3_14`).
  3. **Workstream C — 7-Column Weekly Habit Board (`WeeklyHabitBoard`) & Authoritative `target_period` Evaluation (`SharedCalendarComponents.kt`, `DayseedRepository.kt`, `InsightsScreen.kt`):**
     - Enforced `HabitVersion.targetPeriod` (`DAY` vs. `WEEK`) as the sole authoritative rule for daily vs. weekly target evaluation across `getGoverningHabitVersion`, `evaluateDerivedDayStatus`, `evaluateHonestCalendarDateState`, `computeWeeklyBoard`, and `computeGrowthMetricsForPeriodSync`, while using `scheduleKind` strictly for date eligibility and preferred-weekday guidance.
     - Implemented `computeWeeklyBoard` and `WeeklyHabitBoard` with 7 week-start-ordered columns, per-day contribution labels for weekly habits without false daily failures, once-per-week classification against the frozen start-of-week `HabitVersion` (`Quantity`/`Duration` unit sums, `days/week` distinct qualifying dates, `sessions/week` positive session counts), early weekly completion without subsequent daily misses, and `WeeklyPeriodStatus.IN_PROGRESS` for the current open week.
  4. **Workstream E — Growth Mark (`G2`/`G3`), Separated Insights Metrics, Past/Current/Future Daily Opportunity Rules, Aggregated `D-040` Bands & Primary-Mood-Only `J1–J11` Filtering (`GrowthMark.kt`, `InsightsScreen.kt`, `Models.kt`, `DayseedRepository.kt`):**
     - Updated `CollectedMomentsSummaryCard` / `GrowthMarkSummaryCard` (`G2`) and `CollectedMomentsDetailSheet` (`G3`) with `DurationNormalizer.formatCollectedMomentsDuration` (`0 min`, `45 min`, `1 hr 25 min`, `1 day 2 hr`, `1 wk 2 days`), explicit `0`-session distinction (`currentMonthDurationZeroSessionsCount`), unsupported duration unit exclusion notice (`currentMonthUnsupportedDurationUnits`), month switcher (`g3_prev_month_btn`, `g3_next_month_btn`), and direct session tap to `RecordSession(habitId, date, entryId)`.
     - Implemented `computeGrowthMetricsForPeriodSync(startDate, endDate, referenceToday)` separating `habitActiveDatesCount` (`totalActiveDates`, never including Journal-only dates) from `journalEntriesCount` and `journalActiveDatesCount`, `numericDurationSessionsCount`, `completionOutcomesCount`, `uniqueHabitsCompletedCount`, `plannedRestCount`, `flexibleWeeklyOpportunitiesMetCount / flexibleWeeklyOpportunitiesTotalCount` (`+ flexibleWeeklyInProgressCount`), and `habitDaysMetCount / scheduledDailyOpportunitiesCount` with explicit date resolution:
       1. `local_date < referenceToday`: counted in `scheduledDailyOpportunitiesCount` whether met or missed, and in `habitDaysMetCount` if met.
       2. `local_date == referenceToday`: counted in both `habitDaysMetCount` and `scheduledDailyOpportunitiesCount` **only if** its daily target is met; otherwise excluded as still open.
       3. `local_date > referenceToday`: **always excluded** from both `habitDaysMetCount` and `scheduledDailyOpportunitiesCount`.
     - Aggregated `D-040` progress bands per `(habit_id, local_date)` for `TargetPeriod.DAY` (`dailyProgressBandCounts`) and per `(habit_id, weekStart)` for `TargetPeriod.WEEK` (`weeklyProgressBandCounts`) before classification.
     - Fixed `computeMoodInsights` so `selectedMoodFilter` matches `primaryMoodCode` only (never `secondaryMoodCode`) and added `SAD` and `JOYFUL` to `MoodCatalog.allMoods` (`J1–J11` fixture).
     - Reorganized `InsightsScreen.kt` into the 4 canonical sections (`1. Overview`, `2. Habits`, `3. Mood`, `4. Growth`) and removed direct `LocalDate.now()` / `System.currentTimeMillis()` calls in favour of injected `DayseedClock`.
- **Verification Evidence (Executed `2026-11-09T08:00:05Z` – `2026-11-09T08:04:38Z`, Stage B Self-Check):**
  - `compile_applet`: **PASSED** (`Build succeeded - the applet is compiled`).
  - `gradle :app:testDebugUnitTest --tests "com.example.dayseed.Phase3ImplementationTest" --rerun-tasks`: **PASSED 14/14** (`BUILD SUCCESSFUL in 26s`, 30 actionable tasks: 30 executed).
  - `gradle :app:testDebugUnitTest --rerun-tasks :app:assembleDebug :app:assembleRelease :app:lintDebug`: **PASSED 179/179** executed JUnit test methods across all 13 test classes, **0 failed, 0 errors, 0 skipped, 0 lint errors** (`BUILD SUCCESSFUL in 4m 31s`, 105 actionable tasks executed, Gradle `9.3.1`, Temurin JDK `21.0.13+9-LTS`, Robolectric `4.14.1` SDK 34):
    - `com.example.dayseed.Phase3ImplementationTest`: **14/14 PASSED** (`1.598s`, XML timestamp `2026-11-09T08:02:53.662Z`, covering `p3_01`..`p3_14`).
    - `com.example.dayseed.Phase2ImplementationTest`: **31/31 PASSED** (`10.697s`, XML timestamp `2026-11-09T08:02:42.954Z`).
    - `com.example.dayseed.Phase1ImplementationTest`: **29/29 PASSED** (`3.813s`, XML timestamp `2026-11-09T08:02:39.048Z`).
    - `com.example.dayseed.DayseedAcceptanceTest`: **12/12 PASSED** (`18.806s`, XML timestamp `2026-11-09T08:02:12.174Z`).
    - `com.example.dayseed.DayseedReminderAlarmLifecycleTest`: **16/16 PASSED** (`2.01s`, XML timestamp `2026-11-09T08:02:31.364Z`).
    - `com.example.dayseed.DayseedReminderSemanticsTest`: **16/16 PASSED** (`1.081s`, XML timestamp `2026-11-09T08:02:37.261Z`).
    - `com.example.dayseed.DayseedReminderConcurrencyTest`: **11/11 PASSED** (`1.676s`, XML timestamp `2026-11-09T08:02:35.074Z`).
    - `com.example.dayseed.DayseedVisualResolutionTest`: **15/15 PASSED** (`0.094s`, XML timestamp `2026-11-09T08:02:38.858Z`).
    - `com.example.dayseed.DayseedSettingsValidationTest`: **10/10 PASSED** (`0.014s`, XML timestamp `2026-11-09T08:02:38.842Z`).
    - `com.example.dayseed.DayseedReminderAndBackupTest`: **9/9 PASSED** (`1.624s`, XML timestamp `2026-11-09T08:02:33.435Z`).
    - `com.example.dayseed.DayseedSafFilenameFeedbackTest`: **7/7 PASSED** (`0.488s`, XML timestamp `2026-11-09T08:02:38.349Z`).
    - `com.example.dayseed.DayseedReminderDebugProbeTest`: **5/5 PASSED** (`0.437s`, XML timestamp `2026-11-09T08:02:36.755Z`).
    - `com.example.dayseed.DayseedAppearanceSettingsTest`: **4/4 PASSED** (`0.344s`, XML timestamp `2026-11-09T08:02:30.993Z`).
- **Stage Gate Status:** Stage B Phase 3 implementation and self-checks are **COMPLETE**. All human/device checks (`P1-H*`, `P2-H*`, `P2-C*`, `P25-H*`, `RM-H01`–`RM-H07`, and `P3-H01`–`P3-H09` in `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`) remain **`NOT RUN`**. **Stage C (independent technical validation)** and **Stage D (final owner acceptance / release readiness)** remain **NOT OPEN**.

## Phase 4A Implementation — Weekly reminder family (goal acknowledgement, week-start reflection, inactivity check-in, optional tips) — 2026-11-09 (Stage B Self-Check)

**Inspection baseline:** repository `HeWhoArose/Dayseed`, branch `main`, pulled to `fe0846e` (`fe0846e` = "docs: update repository baseline documentation", 2026-11-09T11:17:03+01:00) before this work; the Phase 4A changes below are committed on `main` at `7bb8d80` (`7bb8d80ca4a59eb9cb7d5a5a0aeb79001ea745d1`), pushed on 2026-11-09 on top of `fe0846e`.

**Gap that Phase 4A closed:** `implementation_plan.md` Phase 4 §A requires the weekly-goal acknowledgement, weekly inactivity check-in, week-start reflection and optional tips to be integrated into the real reminder pipeline. Before this change the ledger only carried `HABIT_DAILY`/`COMEBACK_*` occurrence types and Settings stated that weekly reflections were not implemented. Everything else in Phase 4 §A (channel, real posting, `POST_NOTIFICATIONS` request with explanation, honest `SCHEDULED → DELIVERED` promotion, lifecycle persistence, occurrence keys, pre-post re-checks, inexact `AlarmManager` + `WorkManager` split, boot/time/timezone/package-replaced reconciliation, retryable cancel outbox, privacy-safe copy) already existed from D-225–D-231 and was re-verified by inspection.

**Implemented:**
1. **`WeeklyReminderEngine.kt`** — occurrence identity (`WEEKLY_GOAL_REACHED:<habit>:<weekStart>`, `WEEK_START_REFLECTION:<weekStart>`, `WEEKLY_INACTIVITY:<habit>:<date>`, `PARTIAL_PROGRESS_TIP:<habit>:<date>`), approved copy from `docs/23`, cadence helpers (`inactivityCheckInDates`, `tipDates`), and the atomic ledger reservation (`RESERVED` / `SUPPRESSED` / `DEFERRED` / `ALREADY_FINALISED`) with the same `POSTING_CLAIM:<uuid>` lease contract as the daily pipeline.
2. **`DayseedRepository`** — `weeklyHabitStatesForWeek` (weekly-target snapshot, progress anchors including planned rest days), `planWeeklyRemindersSync` (priority order goal-reached → week-start → inactivity → tips, one weekly prompt per habit per local day, max 3 inactivity and 3 tips), and `runWeeklyReminderPassSync` (reserve inside `BEGIN IMMEDIATE`, post outside, promote only after a real post).
3. **`DayseedDatabaseHelper`** — v3 `reminder_deliveries` CHECK extended with the four weekly types plus a `reconcileExistingV3SchemaIfNeeded` rebuild for existing v3 databases, preserving `DATABASE_VERSION = 3` (D-219 pattern).
4. **`ReminderNotifier.postWeeklyReminder`** — same channel, BigText style, content intent that opens Dayseed, no private content.
5. **`ReminderReconcileWorker`** — runs the weekly pass when reminders are globally enabled.
6. **`SettingsPanels.RemindersPanel`** — weekly reflection and encouragement-tips rows shown read-only with honest wording (activation still gated on physical-device verification).

**Executed evidence (Stage B self-check, 2026-11-09, this workspace: Temurin JDK 21.0.13, Gradle 9.3.1, Android platform 36 + build-tools 36.0.0, Robolectric 4.14.1 SDK 34):**
- `gradle :app:compileDebugUnitTestKotlin` → **BUILD SUCCESSFUL**.
- `gradle :app:testDebugUnitTest --tests "com.example.dayseed.Phase4ImplementationTest"` → **PASSED 18/18** (`p4_01`..`p4_18`).
- `gradle :app:testDebugUnitTest --rerun-tasks` → **PASSED 197/197** across 14 classes, 0 failures, 0 errors, 0 skipped: `Phase4ImplementationTest` **18/18**, `Phase3ImplementationTest` **14/14**, `Phase2ImplementationTest` **31/31**, `Phase1ImplementationTest` **29/29**, `DayseedAcceptanceTest` **12/12**, `DayseedReminderAlarmLifecycleTest` **16/16**, `DayseedReminderSemanticsTest` **16/16**, `DayseedReminderConcurrencyTest` **11/11**, `DayseedVisualResolutionTest` **15/15**, `DayseedSettingsValidationTest` **10/10**, `DayseedReminderAndBackupTest` **9/9**, `DayseedSafFilenameFeedbackTest` **7/7**, `DayseedReminderDebugProbeTest` **5/5**, `DayseedAppearanceSettingsTest` **4/4**.
- `gradle :app:assembleDebug :app:lintDebug` → **BUILD SUCCESSFUL in 2m 19s**, `0 errors, 57 warnings`; artifact `app/build/outputs/apk/debug/app-debug.apk` (19,110,276 bytes). A local gitignored `debug.keystore` was generated for the debug signing config (the first attempt failed only at `:app:validateSigningDebug` on the missing keystore file — an environment issue, not product code).

**Not verified here:** SQLite-to-`NotificationManager` behaviour on a real device, reboot/time-zone/week-start delivery, Doze deferral, TalkBack traversal, 200% text on device, measured contrast, and performance on representative hardware — all remain **NOT RUN**. Stage C and Stage D remain **NOT OPEN**.

## Phase 4 audit corrections — reset alarm cancellation, week-start realignment, portable-backup hygiene — 2026-11-10 (Stage B Self-Check)

**Inspection baseline:** repository `HeWhoArose/Dayseed`, branch `main`, pulled to `4308111` (`4308111dc1be02943f8ef443a5997542e6e97d98`, "docs: add Phase 4 device validation cases P4-H11 and P4-H12 to validation register") before this work. A Phase 4 exit-condition audit (`implementation_plan.md` Phase 4 §A–D) found the code scope implemented but three behaviour gaps against the plan's own bullets. The three corrections below were committed on `main` at **`b0fc29d`** (`b0fc29dab7ba0b70cb97381eb2434d1d2ca41648`, "fix(reminders): close three Phase 4 exit-condition gaps", 2026-11-10), pushed to `main` on top of `4308111`.

1. **Delete-all left armed reminder alarms.** `deleteAllDataSync` cancelled notifications and the cancel outbox and then deleted `app_settings` — the row holding the `reminder_scheduled_alarm_ids` registry — without cancelling the registered `AlarmManager` alarms. Each survivor fired once against deleted data and nothing was left to identify it. Added `ReminderScheduler.cancelAllRegistered(context, repository)` and call it at the start of `deleteAllDataSync`, while the registry still exists.
2. **Week-start change did not realign alarms.** `setWeekStartSync` (and the legacy-baseline branch of `setSettingSync("week_start")`) refreshed state but did not reschedule, so stale registrations waited for the 15-minute reconcile pass. Both paths now call `rescheduleReminderAlarms()` after `refreshAllSync()`, matching save/delete/restore/undo.
3. **Portable backup carried device-local scheduler state.** The export filtered only `journal_draft_cursor_*`, so `reminder_scheduled_alarm_ids` (which alarms *this* device armed) shipped in the backup file, and both restore paths would adopt a foreign registry. The export now skips that key and the REPLACE and MERGE import loops ignore it; local state is re-derived by `ReminderScheduler.scheduleAll`.

**Executed evidence — exact committed tree `b0fc29d` (Stage B self-check, 2026-11-10, this workspace: Temurin JDK 21.0.13, Gradle 9.3.1, Android platform 36 + build-tools 36.0.0, Robolectric SDK 34):**
- The same content was exercised before the commit existed: `gradle :app:testDebugUnitTest --tests "com.example.dayseed.Phase4ImplementationTest"` → **PASSED 34/34**; **negative control** with the three production edits temporarily reverted → `p4_32`, `p4_33`, `p4_34` **FAILED** (`34 tests completed, 3 failed`), then **PASSED** after restoring the fixes, confirming the new checks are regression guards rather than tautologies.
- `gradle clean :app:testDebugUnitTest --rerun-tasks :app:assembleDebug :app:assembleRelease :app:lintDebug` on the exact `b0fc29d` tree (`git status` clean apart from an untracked working note) → **BUILD SUCCESSFUL in 2m 37s**, 106 actionable tasks executed; **PASSED 215/215** across 14 classes, 0 failures, 0 errors, 0 skipped (`Phase4ImplementationTest` **34/34**, `Phase3ImplementationTest` 14/14, `Phase2ImplementationTest` 31/31, `Phase1ImplementationTest` 29/29, `DayseedAcceptanceTest` 12/12, `DayseedReminderAlarmLifecycleTest` 16/16, `DayseedReminderSemanticsTest` 16/16, `DayseedReminderConcurrencyTest` 11/11, `DayseedVisualResolutionTest` 15/15, `DayseedSettingsValidationTest` 11/11, `DayseedReminderAndBackupTest` 9/9, `DayseedSafFilenameFeedbackTest` 7/7, `DayseedReminderDebugProbeTest` 5/5, `DayseedAppearanceSettingsTest` 5/5); lint **0 errors, 57 warnings**; `app/build/outputs/apk/debug/app-debug.apk` **19,126,660 bytes**; `app/build/outputs/apk/release/app-release.apk` **12,671,118 bytes** whose signer is `CN=Android Debug, O=Android, C=US` (SHA-256 `E3:34:23:39:75:3D:86:9F:A7:47:4F:36:5F:5A:24:C8:D9:6F:C1:49:11:C4:19:ED:87:A8:BB:14:9D:C1:E8:41`). The release artifact is **debug-signed for installation testing only and must not be represented as a production-signed release**.
- **Known coverage limits of `p4_32`–`p4_34`:** `p4_33` verifies that a stale registration is removed when the week start changes, but it does not prove that an eligible habit's alarm is re-armed; `p4_34` exercises the REPLACE restore path only, not MERGE. Both gaps are scheduled in the Phase 5 closeout as `p4_35` and `p4_36`; until those land, this entry must not be read as full positive-path coverage.

**Still not met:** Phase 4's device/human exit conditions (reboot, time-zone change, Doze, SQLite-to-`NotificationManager` end-to-end, TalkBack, 200% text, measured contrast, performance) remain **NOT RUN**; `P4-H*` in `docs/38` remain `NOT RUN`. Stage C and Stage D remain **NOT OPEN**.

## Phase 5 closeout — consolidated human/device handover (2026-11-10)

One consolidated list of every outstanding human/device case, reconciled against `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`. Every case now carries an explicit per-case `- **Status:**` line; all 70 are `NOT RUN` and none has been executed. IDs and counts below were parsed from the register, not transcribed by hand.

| Group | Cases | IDs | Status | Register section |
| --- | --- | --- | --- | --- |
| `P1-H` | 9 | `P1-H01`–`P1-H09` | `NOT RUN` | Phase 1 (v2→v3 upgrade, persistence, config protection, migration, backup safety) |
| `P2-C` | 9 | `P2-C01`–`P2-C09` | `NOT RUN` | Phase 2 correction cases |
| `P2-H` | 17 | `P2-H01`–`P2-H17` | `NOT RUN` | Phase 2 (navigation, Today, Habits, streaks, Grove) |
| `P25-H` | 7 | `P25-H01`–`P25-H07` | `NOT RUN` | Phase 2.5 (appearance, contrast, daypart artwork) |
| `P3-H` | 9 | `P3-H01`–`P3-H09` | `NOT RUN` | Phase 3 (journal, shared calendar, weekly board, insights) |
| `P4-H` | 12 | `P4-H01`–`P4-H12` | `NOT RUN` | Phase 4 (permission, weekly copy, quiet hours/cap, restart dedupe, SAF, delete-all, accessibility, performance, airplane mode) |
| `RM-H` | 7 | `RM-H01`–`RM-H07` | `NOT RUN` | Reminder/backup remediation (cap, posting ownership, cancellation, probe) |
| **Total** | **70** | | | |

**Rules for executing these cases (from `docs/38`):** use only `PASS` / `FAIL` / `BLOCKED` / `NOT RUN`; a blocked test is not a pass. Record the exact commit SHA and APK under test, device model, Android version, date/time, steps, expected/actual and evidence. Use synthetic data only; never include journal text, notes, passwords or tokens in captured logs. Do not execute destructive migration/restore cases against the owner's real history.

### Artifact and evidence inventory

| Item | Location / value |
| --- | --- |
| Final commit under test | recorded with the sweep log (see the Phase 5 report); implementation baseline `b0fc29d`, tests `6561588` |
| JUnit/Robolectric results | `app/build/test-results/testDebugUnitTest/*.xml` — 217 tests, 14 classes, 0 failures/errors/skips |
| Lint report | `app/build/reports/lint-results-debug.txt` — `0 errors, 57 warnings` |
| Debug APK | `app/build/outputs/apk/debug/app-debug.apk` — 19,126,660 bytes |
| Release APK | `app/build/outputs/apk/release/app-release.apk` — **debug-signed** (`CN=Android Debug, O=Android, C=US`); installation artifact only |
| Toolchain | Temurin JDK 21.0.13, Gradle 9.3.1 via committed wrapper, Android platform 36, build-tools 36.0.0 |
| Build/bootstrap | `./scripts/bootstrap-debug-keystore.sh` (POSIX) or `.\scripts\bootstrap-debug-keystore.ps1` (Windows, **not executed in this environment**) |

The release APK is signed with the debug keystore by design; it must never be presented as a production-signed release, and production signing material requires an owner decision.
