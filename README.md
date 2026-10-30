# Dayseed

**Your progress is more than your streak.**

Dayseed is an Android-native, offline-first habit and micro-journal application built with Kotlin and Jetpack Compose. It is designed for people who want to build consistency without being shamed when life interrupts them.

## Owner-provided visual reference

The [Dayseed UI reference layout](docs/prototypes/dayseed-owner-reference-layout.webp) was supplied on 2026-10-28 and remains a visual guardrail. On 2026-10-30, D-205 selected the synthesis direction: Field Journal warmth/overall identity, Living Editorial typography and hierarchy, and Quiet Instrument discipline for controls and data. Carry forward the reference's habits-first hierarchy, small monthly context, optional journal entry, warm-neutral surfaces, restrained dark-green accents, botanical motif and calm rounded cards as evidence to interpret—not as approval of every screen or component.

**Status boundary:** this is a visual reference, not final UI approval, proof of usability/accessibility, or permission to begin implementation. The owner decision log and grouped decision batch remain authoritative for approval; DESIGN.md remains the sole design-token/component authority. The planning gate remains closed.
## Product scope and source components

The current repository contains native Kotlin/Jetpack Compose source for Today, History, Insights, Settings, Add/Edit Habit, and Journal Editor screens, plus source components such as HabitCard, GrowthMark, ComebackCard, and ReflectionCard. Their presence in source is not proof that the app builds, passes tests, or behaves correctly on a device.

The product and planning documents define the intended v1 scope: measured/effort-aware habits, independent text journaling, history correction, local insights, approved 14-/30-day comeback triggers, local journal search, reminders, readable journal export, portable backup/restore, and user-controlled deletion. Some of these requirements are only partially represented by the observed data model. **For any AI-assisted continuation, start with [AGENTS.md](AGENTS.md) and the [Continuation Handover and Decision Protocol](docs/32-CONTINUATION-HANDOVER.md)**; they explain decision authority, delegated defaults, the current planning gate, and the exact next steps. See also [Implementation Reality Audit](docs/22-IMPLEMENTATION-REALITY-AUDIT.md), [Planning Completeness Review](docs/27-PLANNING-COMPLETENESS-REVIEW.md), [Requirement-to-Test Traceability](docs/28-REQUIREMENT-TRACEABILITY.md), [UX/Design Approval Pack](docs/29-UX-DESIGN-APPROVAL-PACK.md), [Schema Closure/Migration Plan](docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md), [Owner Planning Decision Batch](docs/31-OWNER-PLANNING-DECISION-BATCH.md), and [Monetisation and Ads Evaluation](docs/33-MONETISATION-AND-ADS-EVALUATION.md) for current planning status, design/schema limits, recorded owner responses and the pre-launch monetisation review. D-200 supersedes the initial ads-evaluation request for current v1: ads are deferred until a pre-launch revisit.

Important status distinctions:
- The current SQLiteOpenHelper schema was reported as version 1 with a placeholder upgrade handler; it uses a one-row-per-habit/date record model that conflicts with the approved multiple-numeric-entry requirement. The target relational schema remains proposed; no migration is verified.
- The Growth Mark component exists in source. D-199 selects the modified G2 “Collected moments” duration-total summary and G3 calendar/date-level inspection. D-201 sets conservative planning defaults for canonical-unit aggregation, readable duration formatting, current-calendar-month default, month navigation and date/session drill-down; separate day/week period selectors and month-equivalent duration displays are deferred for initial v1. Actual aggregation, corrections, rendered formatting and accessibility remain unverified.
- Approved comeback triggers are 14 complete local calendar days without opening Dayseed and a broader 30-day trigger; the 3-day and 90-day examples are exploratory copy, not approved v1 triggers.
- The initial v1 is intended to work offline without a Dayseed account or cloud service. Actual network/data-flow behavior, privacy configuration, and Android platform-backup exclusion still require technical verification.
- Human-readable journal export is separate from the versioned Dayseed portable-backup format. Neither should be considered release-verified from documentation alone.
- The owner selected W1/N1/A2 as planning directions on 2026-10-28. D-199 selects modified G2 “Collected moments” for duration recorded in duration-tracked habits, defaulting to the current calendar month, and G3 for calendar/date-level detail. D-201 records conservative duration-formatting, compatible-aggregation and period-control defaults: preserve source precision, round only for display, use 24-hour days and 7-day weeks, and defer separate day/week selectors and month-equivalent duration displays in initial v1. D-205 selects the broad visual identity synthesis; D-206 selects two-week and one-month comeback copy; D-207 selects Soft Botanical as palette direction but explicitly defers exact colour values, fonts and production-token approval. D-204 supports both automatic time-aware themes and user-selected fixed themes, with fixed selection overriding automatic switching and gradual transitions between automatic dayparts. Exact theme boundaries, palettes, tokens and accessibility remain open; the planning gate is closed.
- D-200 records the owner's choice to defer ads for initial v1 and revisit monetisation before launch. `docs/33-MONETISATION-AND-ADS-EVALUATION.md` remains pre-launch review material; the include/defer decision for initial v1 is not pending. No ad SDK, placement, rewarded ad, purchase, subscription, or Supporter Pack is approved for initial v1; core functionality must remain usable offline.
- The design system and UX remain subject to accessibility review. Soft Botanical is a selected palette direction only; exact candidate colours, font choices, theme boundaries and production tokens are not approved or accessibility-validated. DESIGN.md remains the sole authority for tokens actually approved later.

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
