# Dayseed

**Your progress is more than your streak.**

Dayseed is an Android-native, offline-first habit and micro-journal application built with Kotlin and Jetpack Compose. It is designed for people who want to build consistency without being shamed when life interrupts them.

## Owner-provided visual reference

The [Dayseed UI reference layout](docs/prototypes/dayseed-owner-reference-layout.webp) was supplied on 2026-10-28 and is the durable visual direction we are walking toward. It includes Today variants A–E (primary, reflection-higher, compact controls, completed-day, and empty-day states), plus Habit Create/Edit, Habit Detail, History calendar, Journal Entry, Insights Overview, Return-after-a-break, and Settings screens. Carry forward its habits-first hierarchy, small monthly context, optional journal entry, warm-neutral surfaces, restrained dark-green accents, botanical motif, and calm rounded cards as reference points.

**Status boundary:** this is a visual reference, not final UI approval, proof of usability/accessibility, or permission to begin implementation. The owner decision log and grouped decision batch remain authoritative for approval; DESIGN.md remains the sole design-token/component authority. The planning gate remains closed.
## Product scope and source components

The current repository contains native Kotlin/Jetpack Compose source for Today, History, Insights, Settings, Add/Edit Habit, and Journal Editor screens, plus source components such as HabitCard, GrowthMark, ComebackCard, and ReflectionCard. Their presence in source is not proof that the app builds, passes tests, or behaves correctly on a device.

The product and planning documents define the intended v1 scope: measured/effort-aware habits, independent text journaling, history correction, local insights, approved 14-/30-day comeback triggers, local journal search, reminders, readable journal export, portable backup/restore, and user-controlled deletion. Some of these requirements are only partially represented by the observed data model. **For any AI-assisted continuation, start with [AGENTS.md](AGENTS.md) and the [Continuation Handover and Decision Protocol](docs/32-CONTINUATION-HANDOVER.md)**; they explain decision authority, delegated defaults, the current planning gate, and the exact next steps. See also [Implementation Reality Audit](docs/22-IMPLEMENTATION-REALITY-AUDIT.md), [Planning Completeness Review](docs/27-PLANNING-COMPLETENESS-REVIEW.md), [Requirement-to-Test Traceability](docs/28-REQUIREMENT-TRACEABILITY.md), [UX/Design Approval Pack](docs/29-UX-DESIGN-APPROVAL-PACK.md), [Schema Closure/Migration Plan](docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md), and [Owner Planning Decision Batch](docs/31-OWNER-PLANNING-DECISION-BATCH.md) for current planning status, proposed schema gaps, design review and remaining owner choices.

Important status distinctions:
- The current SQLiteOpenHelper schema was reported as version 1 with a placeholder upgrade handler; it uses a one-row-per-habit/date record model that conflicts with the approved multiple-numeric-entry requirement. The target relational schema remains proposed; no migration is verified.
- The Growth Mark component exists in source, but its final metric and visual representation are not approved.
- Approved comeback triggers are 14 complete local calendar days without opening Dayseed and a broader 30-day trigger; the 3-day and 90-day examples are exploratory copy, not approved v1 triggers.
- The initial v1 is intended to work offline without a Dayseed account or cloud service. Actual network/data-flow behavior, privacy configuration, and Android platform-backup exclusion still require technical verification.
- Human-readable journal export is separate from the versioned Dayseed portable-backup format. Neither should be considered release-verified from documentation alone.
- The design system and UX remain proposals; candidate colours, themes, layouts, and design tokens are not yet owner-approved or accessibility-validated.

This repository status does **not** claim a clean build or a functionally complete app. No Gradle build, automated test suite, APK installation, physical-device validation, or database migration has been verified in the current planning status.

## Technology Stack

The project declares a native Android/Kotlin + Jetpack Compose stack with SQLiteOpenHelper, a repository, ViewModel/StateFlow, and Gradle build configuration. The versions below reflect the repository configuration inspected in the previous read-only audit; they have not been confirmed by a successful build in this planning pass.

- **Platform configuration:** minSdk 26, targetSdk 36 (declared; build/runtime not verified)
- **Language/tooling declarations:** Kotlin 2.2.10; Gradle 9.3.1; Android Gradle Plugin 9.1.1 (declared; compatibility not build-verified)
- **UI Toolkit:** Jetpack Compose + Material 3
- **Architecture:** ViewModel/StateFlow and repository-based structure
- **Local persistence:** SQLiteOpenHelper; existing schema/migration path needs review. The approved multi-entry schema is not yet verified as implemented.

## Project status and audits

- [Implementation Reality Audit](docs/22-IMPLEMENTATION-REALITY-AUDIT.md) — reconciles planning claims with source files actually present.
