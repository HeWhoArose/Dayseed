# Dayseed

**Your progress is more than your streak.**

Dayseed is an Android-native, offline-first habit and micro-journal application built with Kotlin and Jetpack Compose. It is designed for people who want to build consistency without being shamed when life interrupts them.

## Owner-provided visual reference

The [Dayseed UI reference layout](docs/prototypes/dayseed-owner-reference-layout.webp) was supplied on 2026-10-28 and remains a visual guardrail. On 2026-10-30, D-205 selected the synthesis direction: Field Journal warmth/overall identity, Living Editorial typography and hierarchy, and Quiet Instrument discipline for controls and data. Carry forward the reference's habits-first hierarchy, small monthly context, optional journal entry, warm-neutral surfaces, restrained dark-green accents, botanical motif and calm rounded cards as evidence to interpret—not as approval of every screen or component.

**Status boundary:** this is a visual reference, not final UI approval, proof of usability/accessibility, or permission to begin implementation. The owner decision log and grouped decision batch remain authoritative for approval; DESIGN.md remains the sole design-token/component authority. The Stage A planning gate is APPROVED / OPEN under D-208 (2026-10-30); the owner authorised complete Stage B initial-v1 implementation under D-213 (2026-11-01), using root `implementation_plan.md` (with Phase 0 Section 5 choices confirmed under D-215–D-218). Stage C independent validation and Stage D final acceptance remain separately gated.
## Product scope and source components

The current repository contains native Kotlin/Jetpack Compose source for Today, History, Insights, Settings, Add/Edit Habit, and Journal Editor screens, plus source components such as HabitCard, GrowthMark, ComebackCard, and ReflectionCard. Their presence in source is not proof that the app builds, passes tests, or behaves correctly on a device.

The product and planning documents define the intended initial-v1 scope: quantity/duration/check-off tracking, independent text journaling, history correction, local insights, approved 14-/30-day comeback triggers, local journal search, reminders, readable journal export, portable backup/restore and user-controlled deletion. The source now carries the full initial-v1 persistence model (schema v3, 15 tables), the five-destination navigation, weekly reminders, backup/restore v3 and the Phase 4 delivery pipeline; device and accessibility behaviour remains unverified. **For any AI-assisted continuation, start with [AGENTS.md](AGENTS.md), [Continuation Handover and Decision Protocol](docs/32-CONTINUATION-HANDOVER.md), and [Phase 0 Preflight Reconciliation Report](docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md)**; they explain decision authority, delegated defaults, the current Stage A approval, the separate Stage B/C/D gates, and the exact next steps. See also [Consolidated System-Wide Audit and Implementation Plan](implementation_plan.md), [Implementation Reality Audit](docs/22-IMPLEMENTATION-REALITY-AUDIT.md), [Planning Completeness Review](docs/27-PLANNING-COMPLETENESS-REVIEW.md), [Requirement-to-Test Traceability](docs/28-REQUIREMENT-TRACEABILITY.md), [UX/Design Approval Pack](docs/29-UX-DESIGN-APPROVAL-PACK.md), [Schema Closure/Migration Plan](docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md), [Owner Planning Decision Batch](docs/31-OWNER-PLANNING-DECISION-BATCH.md), and [Monetisation and Ads Evaluation](docs/33-MONETISATION-AND-ADS-EVALUATION.md) for current planning status, design/schema limits, recorded owner responses and the pre-launch monetisation review. D-200 supersedes the initial ads-evaluation request for current v1: ads are deferred until a pre-launch revisit.

Important status distinctions:
- The current source at `main` HEAD `b0fc29d` declares `SQLiteOpenHelper` schema version 3 (`DATABASE_VERSION = 3`) with **15 tables**: `habits`, `habit_versions`, `habit_entries`, `habit_day_outcomes`, `habit_date_notes`, `habit_date_exceptions`, `habit_streak_runs`, `week_start_history`, `journal_entries`, `journal_drafts`, `app_settings`, `reminder_deliveries`, `restore_sessions`, `restore_conflicts`, `notification_cancel_outbox`. Migrations are fail-closed (`migrateV1ToV2FailClosed`, the v2-to-v3 path, and an in-place v3 rebuild for the extended reminder-ledger constraint). Phase 1 delivered and tested the v2-to-v3 migration that the Phase 0 preflight (`docs/37`) identified as necessary.
- The Growth Mark component exists in source. D-199 selects the modified G2 “Collected moments” duration-total summary and G3 calendar/date-level inspection. D-201 sets conservative planning defaults for canonical-unit aggregation, readable duration formatting, current-calendar-month default, month navigation and date/session drill-down; separate day/week period selectors and month-equivalent duration displays are deferred for initial v1. Actual aggregation, corrections, rendered formatting and accessibility remain unverified.
- Approved comeback triggers are 14 complete local calendar days without opening Dayseed and a broader 30-day trigger; the 3-day and 90-day examples are exploratory copy, not approved v1 triggers.
- The initial v1 works offline without a Dayseed account or cloud service. Verified in source and automated tests: no `INTERNET` permission, `allowBackup=false` with data-extraction restrictions, and portable backups that exclude live notification and device-scheduler state. Runtime behaviour on a device still requires Stage C verification.
- Human-readable journal export is separate from the versioned Dayseed portable-backup format. Neither should be considered release-verified from documentation alone.
- The owner selected W1/N1/A2 as planning directions on 2026-10-28, and on 2026-11-01 approved **D-215** superseding N1 with a five-destination navigation structure (`Today`, `Habits`, `Journal`, `Insights`, `Settings`, with `Day Detail` and `Habit Detail` as secondary routes). **D-216** approves `Quantity`, `Duration`, and `Simple check-off` for new habits while preserving legacy `EFFORT_AWARE` records losslessly (with owner-confirmed Option A preserving all seven D-040 bands and requiring both an encouragement quote and a distinct motivation quote for each detailed band). **D-217** approves individual habit streaks, elapsed-span longest-streak display (`days`/`weeks`), neutral explicit zero, and the permanent Grove. **D-218** approves the one-off `Take a rest day` (`REST_DAY`) exception extending D-193. D-199 selects modified G2 “Collected moments” for duration recorded in duration-tracked habits, defaulting to the current calendar month, and G3 for calendar/date-level detail. D-201 records conservative duration-formatting, compatible-aggregation and period-control defaults: preserve source precision, round only for display, use 24-hour days and 7-day weeks, and defer separate day/week selectors and month-equivalent duration displays in initial v1. D-205 selects the broad visual identity synthesis; D-206 selects two-week and one-month comeback copy; D-207 selects Soft Botanical as palette direction but explicitly defers exact colour values, fonts and production-token approval. D-204 and D-210/D-211 support both automatic time-aware themes/artwork and user-selected fixed themes, with fixed selection overriding automatic switching and gradual transitions between automatic dayparts. Exact theme boundaries, palettes, tokens and accessibility remain open. Stage A planning is approved/open under D-208; Stage B implementation is authorised under D-213; Stage C independent validation and Stage D final acceptance remain separately gated.
- D-200 records the owner's choice to defer ads for initial v1 and revisit monetisation before launch. `docs/33-MONETISATION-AND-ADS-EVALUATION.md` remains pre-launch review material; the include/defer decision for initial v1 is not pending. No ad SDK, placement, rewarded ad, purchase, subscription, or Supporter Pack is approved for initial v1; core functionality must remain usable offline.
- The design system and UX remain subject to accessibility review. Soft Botanical is a selected palette direction only; exact candidate colours, font choices, theme boundaries and production tokens are not approved or accessibility-validated. DESIGN.md remains the sole authority for tokens actually approved later.

This repository status does **not** claim a release-ready app. Stage B implementation self-checks do not substitute for Stage C independent technical validation or physical-device validation, and the release variant is signed with the debug keystore until the owner supplies production signing material.

## Technology Stack

The project declares a native Android/Kotlin + Jetpack Compose stack with SQLiteOpenHelper, a repository, ViewModel/StateFlow, and Gradle build configuration.

- **Platform configuration:** `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`, `namespace = "com.example.dayseed"`, `applicationId = "com.aistudio.dayseed.wkqmfl"`
- **Declared language/plugin versions (`gradle/libs.versions.toml`):** Kotlin Gradle Plugin `2.2.10`; Android Gradle Plugin `9.1.1`; Compose BOM `2024.09.00`; Robolectric `4.14.1`; Java `21` (`JavaVersion.VERSION_21`). Build toolchain verified in the Stage B self-checks: `Gradle 9.3.1` through the committed wrapper, `Temurin JDK 21.0.13+9-LTS`, Android platform 36 and build-tools 36.0.0.
- **UI Toolkit:** Jetpack Compose + Material 3
- **Architecture:** ViewModel/StateFlow and repository-based structure
- **Local persistence:** SQLiteOpenHelper (`DATABASE_VERSION = 3`, 15 tables, fail-closed migrations, portable backup format v3).

## Building from source

Prerequisites (versions this project is verified against):

- **JDK 21** — required, not optional: `app/build.gradle.kts` sets `sourceCompatibility`/`targetCompatibility` to `VERSION_21`.
- **Android SDK platform 36** with **build-tools 36.0.0** (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`).
- The **Gradle wrapper** in this repository (Gradle 9.3.1). Do not substitute a different Gradle version.

Point the build at the SDK either with the `ANDROID_HOME` environment variable or a gitignored `local.properties` containing `sdk.dir=/path/to/android-sdk`.

Both signing configs reference `${rootDir}/debug.keystore`, which is gitignored, so a fresh checkout cannot assemble an APK until that file exists. Create it once with the development-only bootstrap script (it refuses to overwrite an existing keystore):

```bash
./scripts/bootstrap-debug-keystore.sh       # macOS / Linux
.\scripts\bootstrap-debug-keystore.ps1      # Windows
```

Then build, test and lint:

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest --rerun-tasks
./gradlew :app:lintDebug
```

The release variant is signed with the **same debug keystore**, deliberately, until the owner supplies production signing material: `:app:assembleRelease` produces an installable artifact for testing, never a production-signed release. Never commit `debug.keystore`, `local.properties` or any credential.

## Project status and audits

- [Phase 0 Preflight Reconciliation Report](docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md) — verified baseline at `main` `ebeca3ebff1155c220f8e09f1e00e07fdb28209b`, corrected build/schema/signing inventories, executed `30/30` test evidence, and the owner-confirmed D-040/D-216 Option A resolution (seven bands plus paired encouragement/motivation quotes).
- [Consolidated System-Wide Audit and Implementation Plan](implementation_plan.md) — canonical audit findings, source baseline, owner-direction reconciliation and large phase-by-phase initial-v1 implementation roadmap.
- [Implementation Reality Audit](docs/22-IMPLEMENTATION-REALITY-AUDIT.md) — historical planning/audit snapshot; reconcile its claims against current source before relying on it.


## Current implementation and validation sequence

Stage A planning is complete and approved/open under D-208, and Stage B full initial-v1 implementation was authorised by the owner on 2026-11-01 under D-213. **Phases 0–4 are implemented** and self-checked at Stage B; **Phase 5 (Stage B integration closeout)** covers reproducible build infrastructure, documentation reconciliation and a final exact-commit evidence sweep.

- **Latest recorded automated evidence:** 217/217 JUnit/Robolectric tests across 14 classes, 0 failures, 0 errors, 0 skipped; lint `0 errors, 57 warnings`; debug APK 19,126,660 bytes and a **debug-signed** release APK. See [Progress and Handover](docs/12-PROGRESS-AND-HANDOVER.md) and [Requirement Traceability](docs/28-REQUIREMENT-TRACEABILITY.md).
- **Human/device validation: all 70 cases `NOT RUN`.** The register is [Phase 1 Human-Validation Register](docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md); it covers P1-H, P2-C, P2-H, P25-H, P3-H, P4-H and RM-H cases, scheduled for one consolidated Stage C session.
- **Gates:** Stage C independent technical validation and Stage D owner acceptance remain **NOT OPEN**. Nothing here authorises either gate, a release claim, or production signing.
