# Dayseed — System-Wide Audit and Consolidated Initial-v1 Implementation Plan

**Prepared:** 2026-11-01  
**Repository:** HeWhoArose/Dayseed  
**Current repository baseline:** `main` at `41ec70b268df2a1345390292a606e28a610663c0` (2026-11-03); Stage C remediation is isolated on `stage-c-remediation`  
**Technology:** Native Android, Kotlin (`2.2.10`), Jetpack Compose (`BOM 2024.09.00`), AGP (`9.1.1`), `SQLiteOpenHelper` (`DATABASE_VERSION = 3`)  
**Current status (reconciled 2026-11-03):** Stage A planning is closed and Stage B initial-v1 implementation was authorised under D-213. Phase 0 and Phase 1 implementation work is present on `main`; the Phase 1 implementation self-check recorded 57/57 tests on 2026-11-03 but is not independent Stage C evidence. The independent Stage C review found blocking MERGE streak-history deletion and additional restore/schema/test-evidence/documentation gaps. Stage C remains **NOT PASSED** pending Gradle verification; corrective source and tests were merged into `main` through PR #5 as `e3c3dc4360626315b68319570d4fb8f59444d88a`. No Phase 2 implementation, Stage D acceptance, release approval, or merge is authorised by this remediation. The merged source/test changes are included in `e3c3dc4360626315b68319570d4fb8f59444d88a`; the new tests still require execution against that commit.

**Historical note on original audit body (§1–§10):** Sections 1–10 below preserve the read-only source/document audit authored against commit `334679775643ffb43f384646cf7cced75d401f96`. See **Section 11** and **[docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md](docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md)** for the verified Phase 0 baseline at `ebeca3ebff1155c220f8e09f1e00e07fdb28209b`.

> This plan replaces the older root implementation plan. AI Studio agents must use this file as the implementation-plan entry point and must not treat historical AI Studio brain artifacts or older planning branches as the fresher source of truth. The owner decision log, product specification, privacy/architecture contracts and owner-authority rules remain higher authority where this plan identifies an unresolved decision.

---

## 1. Executive summary

Dayseed already has a meaningful native Android implementation. It is not a blank project and must not be rebuilt from scratch. The current source on the audited Phase 1 baseline contains:

- A Kotlin/Jetpack Compose app shell with Today, History, Insights and Settings destinations.
- A SQLiteOpenHelper database at schema version 2, a 12-table schema and a fail-closed version-1-to-version-2 migration path.
- Effective-dated habit configuration, individual numeric entries, daily non-numeric outcomes, habit/date notes, Journal entries and drafts.
- Local backup/restore, encrypted portable-backup support, restore-session/conflict structures and a notification-cancellation outbox.
- A deterministic clock abstraction, derived day-status logic, comeback evaluation, mood insights and Growth Mark aggregation.
- Daypart-aware themes/artwork, a restructured Settings hub and a JVM/Robolectric test suite.

The main problem is the gap between these foundations and complete, accurate user journeys. Several screens do not expose repository capabilities; weekly targets are handled inconsistently by creation, saving, status and reporting; a schedule-edit path can violate the approved historical-preservation rule; reminders are evaluated but not actually posted by Android; and the latest owner direction for individual streaks, the Grove, one-off rest days and expanded navigation does not yet exist in the data model or UI.

This should be a **preservation-first completion**, not a rewrite. The programme is organised as five large workstreams, preceded by a preflight and followed by separate independent validation:

1. **Preflight and source-of-truth reconciliation:** establish the current remote/workspace baseline, reconcile conflicting documents and obtain the correct implementation-gate status.
2. **Domain, SQLite and historical-data correctness:** make schedule, weekly-target, record, progress, rest-exception, streak and Grove behaviour consistent, including safe schema/backup migration.
3. **Complete habit experience:** implement the main five-destination experience and the full habit lifecycle, including compact Today, dedicated Habits, entry editing, derived progress, individual streaks and the Grove.
4. **Journal, calendar, weekly board and Insights:** make date-based information navigable and consistent, with a dedicated Journal, shared calendar/date detail, weekly board and factual statistics.
5. **Android operations and release-quality preparation:** implement real notification delivery, finish backup/restore/export/delete workflows, verify offline/data-safety behaviour, and complete accessibility, resilience and performance work.

A large phase is acceptable; an unreviewed phase is not. Gemini Argon should receive one complete, cohesive workstream at a time, with acceptance tests and regression requirements included in the same assignment. It should not receive one all-at-once instruction to rebuild the entire app.

---

## 2. Audit provenance and evidence limits

### 2.1 Repository snapshot

This audit inspected the public GitHub repository HeWhoArose/Dayseed, main at:

- Commit: 334679775643ffb43f384646cf7cced75d401f96
- Commit date: 2026-10-31 15:18:56 UTC
- Audit date/timezone: 2026-11-01, Africa/Lagos
- The inspected recursive tree contained 185 entries, including 26 Kotlin source files and three test source files.

Inspected paths included:

- App shell/state: MainActivity.kt, DayseedViewModel.kt.
- Data/domain: Models.kt, DayseedClock.kt, DayseedDatabaseHelper.kt, DayseedRepository.kt, ReminderEngine.kt, BackupAndExportManager.kt.
- Screens: TodayScreen.kt, HistoryScreen.kt, InsightsScreen.kt, SettingsScreen.kt, SettingsPanels.kt, SettingsSupport.kt, AddEditHabitDialog.kt, JournalEditorDialog.kt.
- Components: HabitCard.kt, GrowthMark.kt, DaypartArtwork.kt, ReflectionCard.kt, ComebackCard.kt, SettingsGroup.kt.
- Android manifest, backup XML rules, app Gradle configuration, version catalog and current JVM/Robolectric test files.

Relevant symbols were also inspected: schema creation/migration, habit save/update, numeric-entry create/update/delete, non-numeric outcomes, guarded Undo, derived day status, Growth Mark aggregation, reminder evaluation, portable backup/restore and destructive data deletion.

### 2.2 What this audit does and does not establish

**Source-confirmed** means that the implementation/code pattern exists in the inspected commit. It does not mean its complete user journey works or that it passed a build, device test, accessibility test or independent review.

Not performed in this audit:

- No Gradle build or test command was run.
- No APK was produced or installed.
- No physical Android device or emulator was operated.
- No live SQLite file was opened or migrated.
- No Android notification was scheduled or posted.
- No portable backup was imported/exported during runtime.
- No network traffic, privacy behaviour, TalkBack traversal, contrast measurement, large-text layout or reduced-motion behaviour was tested.

All runtime/build claims must remain NOT VERIFIED until there is an executed run with commit-specific logs and environment information.

### 2.3 Source-of-truth precedence

Use this hierarchy:

1. Explicit owner instructions and accepted owner decisions in docs/09-DECISION-LOG.md.
2. Product requirements in docs/01-PRODUCT-SPECIFICATION.md, privacy in docs/04-DATA-AND-PRIVACY.md, and technical architecture in docs/03-TECHNICAL-ARCHITECTURE.md.
3. Current screen/data contracts in docs/26-SCREEN-AND-DATA-CONTRACTS.md and proposed schema/restore mapping in docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md.
4. Decision reconciliation, requirements traceability, test strategy and handover in docs/25, docs/28, docs/07 and docs/32.
5. DESIGN.md is the sole authority for approved design tokens and reusable visual components.
6. This file is the consolidated engineering roadmap. It must not silently turn a proposal into an owner-approved product rule.

Where a source file conflicts with an approved rule, preserve the approved rule and fix the source. Existing code is not proof that current behaviour is the intended behaviour.

---

## 3. Current repository and operational baseline

### 3.1 Application/build configuration

The inspected repository declares:

- Native Android, Kotlin and Jetpack Compose.
- Android Gradle Plugin 9.1.1; Kotlin 2.2.10; Java 21 compile compatibility.
- compileSdk 36, targetSdk 36, minSdk 26.
- Compose BOM 2024.09.00.
- JUnit 4, Robolectric and AndroidX test core for current unit tests.
- Navigation Compose is declared, but MainActivity uses a four-value NavTab switch rather than a complete secondary-route graph.

There is no Gradle wrapper in the inspected repository tree and no checked-in GitHub Actions workflow was found. Historic test records mention Gradle 9.3.1, but a clean checkout cannot be assumed to have that toolchain reproducibly available until a wrapper is established.

The release build type currently selects a release signing configuration that points to debug.keystore and the standard Android debug alias/password. This is not an acceptable production release-signing arrangement. Do not create or commit a production keystore. Configure release signing through a secure owner-controlled CI/local secret mechanism.

The application ID is currently com.aistudio.dayseed.wkqmfl. Confirm that this is the intended long-term Play application identity before release; do not silently change it because it affects app identity, migration and publication.

### 3.2 Existing data model and persistence

The current source declares DayseedDatabaseHelper.DATABASE_VERSION = 2. Its schema-creation path defines 12 tables:

1. habits
2. habit_versions
3. habit_entries
4. habit_day_outcomes
5. habit_date_notes
6. journal_entries
7. journal_drafts
8. app_settings
9. reminder_deliveries
10. restore_sessions
11. restore_conflicts
12. notification_cancel_outbox

The source contains a fail-closed v1-to-v2 migration. It validates legacy data and throws a migration-blocking exception for rows that cannot be mapped losslessly. The acceptance-test source includes coverage for canonical tables, foreign-key enforcement, constraints/triggers and a synthetic v1-to-v2 migration failure case.

This differs from older audit/AI Studio artifact text describing a version-1-only database, an empty onUpgrade handler, a missing migration or a four-table model. Those descriptions are historical and are superseded by the code at the source commit recorded above.

**Migration rule:** do not replace the existing v2 migration with a new clean-install-only schema. Future changes must add a tested v2-to-v3 migration while retaining the v1-to-v2 path. Upgrade tests must cover a fresh install and a genuine v1 database upgraded through v2 to the final schema.

The schema design in docs/30 remains a proposed planning baseline in parts. The code implements substantial v2 structure, but every new v3 table/column, foreign key, unique constraint, trigger, backup mapping and rollback boundary must still be reconciled before implementation.

### 3.3 Existing tests

The current tree contains three JVM/Robolectric test files:

- DayseedAcceptanceTest.kt
- DayseedSettingsValidationTest.kt
- DayseedVisualResolutionTest.kt

The acceptance suite contains repository/schema/logic coverage. Previous commit records describe 19/19 tests at the daypart implementation checkpoint and 26/26 after the Settings restructure. These are historical, commit-specific results. This audit did not rerun them.

No app/src/androidTest tree or Compose UI-test dependency was found in the inspected tree. Test tags exist in UI code, but a test tag alone is not an automated user-journey test.

### 3.4 AI Studio and model baseline

The current .opencode/MODEL_REGISTRY.md lists google/gemini-4-argon as an existing primary model assignment for architecture-auditor. The registry explicitly labels its inventory as not newly evaluated or owner-approved; it does not prove the model is available in the user's current AI Studio account.

Google's official Gemini 4 Argon documentation describes complex software engineering and multi-step agent workflows, with a 1,048,576-token context window, tool/function calling and code execution. Its default synchronous output limit is documented as 65,536 tokens. Google's model card shows strong results on several coding evaluations, but not a universal win across benchmarks, and Google's material describes limitations including hallucinations and occasional slowness/timeouts. This supports large cohesive workstreams, not blind trust in generated code.

Official model references:
- https://ai.google.dev/gemini-api/docs/models/gemini-4-argon
- https://deepmind.google/models/model-cards/gemini-4-argon/

Use the exact model ID gemini-4-argon when selecting it in AI Studio and verify availability in the user's current provider environment. Do not alter .opencode model assignments as part of application implementation unless separately requested.

---

## 4. System-wide audit findings

Severity definitions:

- **P0 — Data correctness/user-trust blocker:** can corrupt historical meaning, create invalid persistence, misstate a recorded result or claim an OS action occurred when it did not.
- **P1 — Core product completion blocker:** a required user journey is missing or materially incomplete.
- **P2 — Quality/navigation/release blocker:** usability, accessibility, automation, operational setup or release configuration requires work.
- **P3 — Deferred or optional enhancement:** not required for initial-v1 core or explicitly deferred.

### 4.1 P0 — Data integrity, schedule and progress correctness

| ID | Finding | Evidence in inspected source | Required correction |
|---|---|---|---|
| AUD-001 | D-194 same-day schedule edit can overwrite today's effective configuration after activity exists. | AddEditHabitDialog still displays Apply today and Apply tomorrow when alreadyLoggedToday is true. DayseedRepository.saveHabitSync accepts Apply-today when its boolean is true, even after qualifying activity. | Enforce D-194 in the repository/domain transaction, not only UI. After today's qualifying activity, preserve today's version and offer Apply tomorrow. Test first edit, repeated edit before/after activity, cancel and history. |
| AUD-002 | Weekly numeric progress can be displayed as a single-day amount against a weekly target. | TodayScreen passes only today's entries to HabitCard; the card sums the supplied list and compares it with habit.targetValue. | Add one shared week-progress calculation from configured week-start through the selected date. Display week-to-date total and today's sessions separately. |
| AUD-003 | Weekly derived status short-circuits on a record for the selected date. | evaluateDerivedDayStatus evaluates the date's entry/outcome before evaluating FLEXIBLE_WEEKLY_TARGET. | Separate day contribution status from weekly-goal status. A partial day must not conceal that the weekly goal has already been met. |
| AUD-004 | A satisfied weekly goal can be represented as UNSCHEDULED on a no-record date. | In the flexible-weekly branch of evaluateDerivedDayStatus, weekSatisfied returns UNSCHEDULED. | Do not overload UNSCHEDULED to mean a completed weekly goal. Introduce separate period-level result data and selected-day states. |
| AUD-005 | Weekly check-off target value is required by the v2 schema but discarded by save logic. | The v2 habit_versions CHECK requires positive target_value for weekly COMPLETION_ONLY/EFFORT_AWARE. saveHabitSync normalises targetValue to null for both; the editor does not render their weekly count input. | Reconcile measurement modes and weekly target semantics. Persist valid counts for supported check-off/day-count goals. Add database-backed create/edit tests. |
| AUD-006 | Growth summary counts activity records/sessions rather than consistently counting habit-day outcomes. | computeGrowthMetricsSync increments completedCount for each outcome and each positive numeric entry; it classifies individual entries instead of necessarily aggregating all sessions for a habit/date. | Define every metric precisely. Aggregate entries by habit/date before classifying; distinguish session count, qualifying habit-days, unique habits and active dates. Never label one metric as another. |
| AUD-007 | Flexible weekly opportunities are misrepresented in the rolling opportunity metric. | computeGrowthMetricsSync treats preferred weekdays as per-day opportunities for FLEXIBLE_WEEKLY_TARGET. Preferred days are guidance, not daily required opportunities. | Calculate flexible goals as weekly periods, not daily failures/opportunities. Use saved week-start and keep this separate from daily schedule metrics. |
| AUD-008 | D-040 Extra and Huge progress bands are not implemented distinctly. | ProgressBand currently ends with EXCEEDED; classify returns EXCEEDED for every amount above target. | Implement Extra (>100% through 150%) and Huge (>150%), with exact ratio comparison before display rounding. Avoid overflow and test boundaries/tiny targets. |
| AUD-009 | The progress classifier can overflow percentage multiplication for large integers. | ProgressBandClassifier calculates safeActual * 100 using Int arithmetic. | Use overflow-safe comparisons; preserve source values and add upper-bound tests. |
| AUD-010 | Manual effort selection conflicts with the newer owner direction for new habits. | Models.kt and AddEditHabitDialog still offer EFFORT_AWARE/EFFORT_LEVELS and user-selected Tiny/Usual/Extra outcomes. Newer owner direction says the user records activity and Dayseed derives measured progress. | New habits should offer Quantity, Duration and Simple check-off only. Preserve legacy effort data/backup decoding until existing data and old backups have a lossless compatibility path. |

### 4.2 P1 — Core habit and tracking journeys

| ID | Finding | Evidence | Required correction |
|---|---|---|---|
| AUD-011 | No dedicated Habits destination exists. | MainActivity has Today, History, Insights and Settings only. Today filters to habits scheduled today. | Add an all-habits destination with Active, Paused, Completed and Archived groups, add/detail/search/filter/lifecycle actions and reorder. |
| AUD-012 | Active but unscheduled habits disappear from Today. | TodayScreen filters activeHabits by isScheduledFor(today). | Keep Today focused but expose a small Not scheduled today group/link to Habits. Bonus logging must not turn a rest day into a scheduled opportunity. |
| AUD-013 | Habit Detail is missing. | No HabitDetail route/screen exists; HabitCard contains too many record and management controls. | Add a secondary Habit Detail route for category, target/schedule, progress, history, entries, notes and lifecycle. |
| AUD-014 | Create/Edit Habit is a tall dialog with intertwined setup concerns. | AddEditHabitDialog mixes measurement, targets, effort definitions, schedule, reminders, colour and effective-date controls. | Replace with a structured full-screen form: identity/category; measurement/units; target/period; schedule; optional reminder. Show only relevant fields. |
| AUD-015 | Numeric session correction is not exposed. | Repository/ViewModel have updateHabitEntry, but HabitCard exposes Delete only; screens do not wire the update callback. | Add a focused edit flow for one entry through Today, Habits and History. Preserve entry identity and guarded Undo. |
| AUD-016 | Per-session notes exist in storage but cannot be entered through main logging. | entry_note is stored/read, but quick-add/custom controls pass null and offer no note field. | Add an optional note to detailed record/correction. Keep distinct from habit/date note and Journal. |
| AUD-017 | Numeric-entry deletion is not wired to the approved Undo contract. | deleteHabitEntrySync deletes directly and returns no UndoToken. | Add guarded 8-second Undo for the exact deleted entry, consistent with D-032/D-196. Do not make Journal/permanent-habit deletion snackbar-undoable. |
| AUD-018 | COMPLETED lifecycle exists in model but not as a normal user action. | HabitLifecycleState includes COMPLETED; HabitCard offers edit/pause/archive but not Mark completed. | Add Mark completed and Completed collection. Preserve final streak as an achievement, not a broken run. |
| AUD-019 | Users cannot manually reorder habits. | No persisted ordering field/control found; repository ordering is creation-based. | Add stable user ordering if retained after the Habits screen; preserve through backup/restore. |
| AUD-020 | No individual streak/Grove model or UI exists. | No streak, run, Grove or rest-exception models/functions were found. | Implement individual streaks and permanent ended/completed-run Grove records through a testable domain engine and versioned persistence. |

### 4.3 P1 — Journal, history, calendar and Growth Mark

| ID | Finding | Evidence | Required correction |
|---|---|---|---|
| AUD-021 | Journal is hidden as a tab inside History. | HistoryScreen uses Daily Log / Journal Archive tabs. | Reconcile five-destination navigation and provide a direct Journal destination. Move history/calendar into Insights/shared date detail. |
| AUD-022 | History is fixed to the latest 30 dates. | HistoryScreen constructs a fixed today-minus-0-to-29 list; no month/year navigation or unrestricted date selection appears. | Add month navigation, date selection and Jump to Today. Older saved records remain accessible/correctable. |
| AUD-023 | Journal Archive lacks prominent New Entry and clear-filter actions. | JournalArchiveView has search/mood filters and list but no new-entry callback or clear-all action for empty filtered results. | Add New Entry in the archive and Clear search/filters on no-results state. |
| AUD-024 | Saved Journal deletion has no confirmation. | JournalEditorDialog directly calls onDelete from the trash icon. | Confirm before deleting a saved entry and explain it cannot be undone. Distinguish discarding a draft. |
| AUD-025 | Today summary displays at most one Journal entry for a date. | TodayScreen uses journals.find; database supports multiple entries/date. | Show count/recent preview and open Journal; do not imply a date has only one entry. |
| AUD-026 | G3 is a decorative dot strip rather than interactive date/session detail. | GrowthMark shows up to 14 current-month dots; only the info action is clickable. | Make G2 open G3 month/date/session detail, with month navigation, accessible date selection and source-session correction. |
| AUD-027 | Insights' 30-Day Rhythm grid is non-interactive and conflates activity types. | InsightsScreen computes one colour from any outcomes/numeric entries/Journal; dates are not selectable. | Replace with shared Daily/Weekly calendar and Day Detail. Show separate habit, mood and Journal markers. |
| AUD-028 | A single all-habits calendar colour can hide mixed per-habit outcomes. | 30-Day Rhythm marks a date if any outcome/Journal exists, without habit identity. | Use small independent habit marks in all-habits view; use full-cell colour/pattern when filtering to one habit. |
| AUD-029 | Current effort-mix displays conflict with the derived progress direction. | GrowthMark and Insights display Tiny/Usual/Extra effort counts. | Replace with correctly aggregated target-relative progress bands or defer metrics that cannot be calculated for legacy rows. |
| AUD-030 | Growth Mark metrics need definitions aligned with source rows. | computeGrowthMetricsSync includes Journal dates in active dates; entry sessions are counted individually; G3 counts outcome and numeric session rows. | Keep G2 duration-only and G3 date/session detail. Define active dates, sessions, habit-days, unique completed habits and weekly opportunities separately. |

### 4.4 P0/P1 — Android reminders and platform operations

| ID | Finding | Evidence | Required correction |
|---|---|---|---|
| AUD-031 | Reminder evaluation is not Android notification delivery. | ReminderEngine writes a ledger row and marks eligible records DELIVERED, including delivered_at/tag/ID. No NotificationManager.notify, NotificationCompat, NotificationChannel, AlarmManager, WorkManager, BroadcastReceiver or runtime permission request was found in inspected app source. | Build actual scheduler/posting service. Mark delivered only after successful OS post. Separate evaluated/scheduled/attempted/delivered/suppressed/failed states and test permission/restart/time changes. |
| AUD-032 | Reminder evaluation is not wired into a runtime trigger. | evaluateTodayRemindersSync exists; no app call site was found in inspected source. | Trigger reconciliation from persistent scheduling/app lifecycle. Re-check state immediately before posting. |
| AUD-033 | Full Reminders Settings surface is missing. | Current Settings hub has appearance, week start, quiet hours, export, privacy, removed habits, About, backup/restore and delete-all. D-212 deferred the full reminders screen. | Expose approved global opt-in, per-habit reminder mode/time, quiet hours and only separately approved tip preferences. |
| AUD-034 | Restore says “from a file” but asks user to paste contents. | BackupRestorePanel opens a dialog with multiline text input; no Storage Access Framework file-picker flow was found. | Implement Android system document-picker restore, validate selected payload, and retain paste only if clearly labelled as an additional path. |
| AUD-035 | Backup export lacks a clear Save-to-file workflow. | BackupRestorePanel previews/copies JSON or sends ACTION_SEND. | Add user-selected document save; preserve explicit share and state where the user chose to save. |
| AUD-036 | Cancellation outbox success may be reported without confirmed OS cancellation. | Repository callback uses nullable NotificationManager and then returns true. | Treat missing service/cancellation failure as retryable; remove outbox rows only after confirmed cancellation. |
| AUD-037 | Permission is declared but runtime request/posting flow is absent. | Manifest declares POST_NOTIFICATIONS; no runtime request/post path was found. | Ask through approved onboarding/Settings, handle denial without blocking core use, and re-check permission before each post. |

### 4.5 P2 — Quality, privacy, automation and release

| ID | Finding | Evidence | Required correction |
|---|---|---|---|
| AUD-038 | No Compose UI/instrumentation test source exists. | No app/src/androidTest tree or Compose UI-test dependency was found. | Add UI tests for navigation, logging/correction, Journal, calendar, permission states and accessibility semantics. |
| AUD-039 | No checked-in Gradle wrapper or CI workflow exists. | No gradlew/gradle wrapper or .github/workflows found in inspected tree. | Pin a compatible Gradle wrapper after baseline toolchain check; add CI for build, unit/Robolectric/UI tests and static validation. |
| AUD-040 | Release signing uses debug configuration. | app/build.gradle.kts release signing points to debug.keystore and debug credentials. | Remove debug signing from release; configure secrets securely outside Git. |
| AUD-041 | Store application ID is not confirmed. | Current ID is com.aistudio.dayseed.wkqmfl. | Confirm stable Play identity before release; do not change casually. |
| AUD-042 | Direct clock calls remain in UI despite DayseedClock. | DayseedClock exists; JournalEditorDialog uses LocalDate.now/System.currentTimeMillis; GrowthMark has LocalDate.now default. | Inject/pass the domain clock for date-sensitive flows and use TestDayseedClock for deterministic tests. |
| AUD-043 | README/plans contain stale contradictory snapshots. | Root implementation plan and .aistudio artifact describe v1-only schema/missing migration; README repeats obsolete claims; docs/35 has contradictory gate summary; PR #1 is based on older main. | Use this root plan as current roadmap. Correct stale README status. Keep historical artifacts labelled. Reconcile old PR changes rather than blindly merging. |
| AUD-044 | Accessibility and device usability remain unverified. | Tags and some decorative semantic clearing exist, but no rendered device/TalkBack/large-text/contrast/reduced-motion evidence was produced here. | Add rendered UI, TalkBack, large text, focus, touch target, contrast and reduced-motion acceptance in Stage C. |
| AUD-045 | Backup/restore/delete/privacy promises are not runtime verified. | Code paths exist but no runtime test/network/device inspection was done in this audit. | Run synthetic-data recovery/deletion tests, inspect data-extraction behaviour and dependencies/network permissions, then reconcile privacy copy with observed behaviour. |

---

## 5. Owner direction and product decisions to reconcile before coding

This section separates previously documented decisions from newer owner direction. The changes below must be reconciled into docs/09 and affected contracts before their corresponding implementation work begins.

### 5.1 Navigation and screen architecture

Previously selected N1 was four destinations: Today, History, Insights, Settings, with Journal Archive nested in History.

The newer direction discussed with the owner prefers five destinations:

1. Today
2. Habits
3. Journal
4. Insights
5. Settings

This plan recommends adopting the five-destination direction, subject to recording how it supersedes N1 before coding. The old History combined screen is separated: Journal gets its own destination; the historical habit calendar moves to Insights, with statistics as a secondary mode. Day Detail and Habit Detail are secondary routes, not additional bottom tabs.

Today remains compact and action-first. Preserve the D-210/D-211 daypart artwork and theme structure; refactor the content hierarchy rather than discarding that work.

### 5.2 Measurement and categories

New-habit choices recommended by the owner:

- Quantity
- Duration
- Simple check-off

Users should not self-rate effort on new habits. Dayseed derives measured progress from actual amount compared with target.

Preserve a legacy-compatible EFFORT_AWARE/EFFORT_LEVELS reader/backup/migration path until old records and backups have a lossless compatibility treatment. Do not drop enum values or erase old outcomes as a shortcut.

Keep these concepts separate:

- Activity category: optional organisation/language such as Reading, Movement, Learning, Hydration or custom.
- Measurement: Quantity, Duration or Simple check-off.
- Schedule/target period: daily, selected weekdays or flexible weekly.

For initial v1 use understandable whole-number units supported by the existing model, such as pages, glasses, repetitions, questions or sessions. Duration uses minutes/hours with canonical aggregation. Decimal quantity units remain out of scope until the precision/data model is deliberately changed.

**Weekly distinction:** “3 days per week” is a check-off/day-count goal counting distinct qualifying dates. “3 sessions per week” is a Quantity goal with unit sessions; multiple sessions on one date may count separately.

### 5.3 Individual streaks, longest-streak indicator and Grove

Owner direction:

- Every habit has its own streak.
- Today has one calm indicator: the longest currently active streak.
- Tapping it shows all individual streaks.
- Every habit has a stable branch identity and distinct colour.
- Branch growth reflects that habit's streak.
- When a run ends, its branch breaks away and joins a permanent Grove.
- Other habits are independent; one ending must not reset others.
- A newly resumed run grows a new branch.
- Completed habits preserve their final run as a completed achievement.
- Pausing or archiving ends that active run; the run remains in the Grove.
- Schedule changes do not themselves break a run.
- Planned rest, unscheduled dates and a one-off rest-day declaration are neutral.
- Partial progress holds the displayed streak value: neither increments nor breaks the current run.
- A fully met or above-target qualifying period increments that habit's streak by one period.

Recommended operational semantics:

| Schedule | Qualifying period | Increment | Hold | Break |
|---|---|---|---|---|
| Daily | Scheduled local day | Daily target met/exceeded | Positive partial progress or declared rest | Scheduled day passes without qualifying result/rest |
| Selected weekdays | Each selected weekday occurrence | Target met/exceeded on that date | Positive partial or rest on that selected date; other weekdays are neutral | Selected weekday passes without qualifying result/rest |
| Flexible weekly | Configured local week | Weekly target reached, once for that week | Positive weekly progress below target | Week ends below target |

A weekly goal increments once per week, not once per session.

**Mixed-cadence comparison:** raw counts from different cadences are not directly comparable. A five-day streak and a five-week streak are not equivalent. Recommended main indicator: compare elapsed active-run span and display the right unit (days/weeks), while the detail screen shows each habit's native count and cadence. Do not display a unitless mixed-cadence number. Record this decision explicitly before coding.

**Explicit zero:** preserve it as a genuine saved record, displayed neutrally, but this plan recommends it does not protect or increment an activity streak because no positive activity was recorded. This needs explicit decision-log recording before implementation.

**Grove persistence:** preserve run ID, habit identity, start/end date, cadence, final count/unit, terminal reason (missed, paused, archived, completed) and enough visual identity to render the branch permanently. Backdated corrections/deletions must recompute affected runs. Permanent habit deletion/delete-all must remove appropriate private Grove data transactionally; archive/completion preserve it.

### 5.4 Rest days

Normal schedule selection remains the preferred way to identify ordinary weekly rest days: selected weekdays are required opportunities; other dates are neutral.

For a daily habit with an unusual one-off rest, the owner wants a **Take a rest day** action instead of Complete or Record. It is a neutral exception for one habit/date, not Skip or Miss.

Reconcile this as a new REST_DAY exception, extending D-193 without reintroducing a skipped/missed outcome:

- Optional and scoped to one habit/date.
- Does not create an activity entry/completed outcome.
- Does not increment a streak.
- Holds the streak instead of breaking it.
- Does not silently reduce a flexible weekly target.
- Has a defined correction/reversal rule and cannot overwrite activity already recorded.
- Appears neutral in History/Calendar.
- Is independent of pausing the entire habit.
- Label it Take a rest day, never Skip or Mark missed.

### 5.5 Progress spectrum and encouragement

Use D-040 and implement the missing Extra/Huge distinction:

- Tiny: >0 and <25%.
- Small: 25% to <50%.
- Halfway: 50% to <75%.
- Nearly there: 75% to <100%.
- Goal met: exactly 100%.
- Extra: >100% through 150%.
- Huge: >150%.

**Owner-confirmed presentation rule (D-216, Option A, 2026-11-01):** Show the canonical seven-band D-040 spectrum without collapsing it into the five labels previously listed in an earlier Phase 0 summary. Present each detailed band together with two distinct pieces of copy: an **encouragement quote** and a separate **motivation quote**, each chosen to fit the exact actual-to-target percentage/band. The two roles must remain distinct. Keep the copy supportive and nonjudgmental; never imply that a user must outperform yesterday or beat a personal record. The `Huge` band may receive more enthusiastic encouragement without pressure to keep exceeding past performance. Preserve no-record and explicitly logged `0` as distinct non-positive states. Compact Calendar/History cells may use the separate coarse groupings from §5.6 only.

Dayseed derives the spectrum from actual amount and applicable target. New habits need no self-reported effort rating. Simple check-off shows factual completion, not an invented effort percentage.

Use curated, category-aware message variations grounded in the actual amount. Avoid generic motivational-speaker language, assumptions about why a user did less, moral judgement, repetitive congratulations after every entry and pressure to exceed yesterday. Change immediate acknowledgement when the band changes or a meaningful period result is reached. Existing optional partial tips remain separate and follow D-196 defaults unless the owner overrides them.

### 5.6 Calendar and weekly board

Owner-preferred initial visual language:

| Result | Initial encoding |
|---|---|
| Target met | Solid green |
| Partial | Lighter tint of target-met green |
| Above target | Darker green with a small flame cue |
| Missed scheduled period | Dotted/dashed pattern |
| Rest, unscheduled, paused | Neutral/default cell |
| Explicit zero | Neutral/default; selected date explains zero was recorded |
| No record yet/still open | Neutral/default |

In all-habits view, use small independent habit marks rather than painting a whole date green because one habit succeeded. In a single-habit filter, stronger full-cell colour/pattern may be used.

For mood and Journal:
- Small primary-mood symbol beside date.
- One small Journal marker for one or more entries; use a count/group marker for multiple entries.
- Tapping the date opens Day Detail with every habit result, mood and Journal entry.

Recommended weekly-board candidate: seven date columns aligned to the user's week start, one row/card per habit, each date's contribution, and a separate period total such as “8 / 10 pages this week” or “2 / 3 days this week”. Test it at compact widths and large text on a real phone before calling the visual final.

### 5.7 Initial-v1 deferrals

- Monthly habit targets remain outside initial v1.
- No cloud account, online backup/sync or cross-device sync.
- No runtime AI analysis or AI-generated habit judgement.
- No analytics or advertising SDK, billing, subscriptions or purchases in initial v1; ads are deferred by D-200.
- No decimal quantity measurement without a deliberate precision/data contract.
- No social comparison/leaderboards/XP without separate approval.
- No new every-N-days or monthly schedule types without a new decision.

---

## 6. Consolidated implementation programme

The programme uses **five large implementation workstreams**, preceded by a mandatory preflight and followed by independent validation. Each phase is end-to-end across the UI, ViewModel, domain, repository, persistence, backup/restore, tests and documentation wherever those layers are affected. A phase may contain internal commits, but it should be delivered as one coherent result with a phase-level evidence report.

### Phase 0 — Implementation preflight and source-of-truth reconciliation

**Purpose:** Ensure Gemini Argon starts from the correct code and requirements, not an old AI Studio import or conflicting planning documents.

**Work:**

1. Fetch latest GitHub main. Record exact SHA, branch, working tree and imported AI Studio base SHA.
2. Compare the AI Studio workspace with remote main. If remote is newer, import/reconcile first; never overwrite newer GitHub work.
3. Read AGENTS.md, README.md, DESIGN.md, docs/09, docs/25, docs/27, docs/28, docs/30, docs/32, docs/35 and this plan. Inspect source/tests relevant to the first workstream.
4. Resolve the contradictory readiness/gate language. D-208 and D-209 explicitly say Stage A approval did not authorise full implementation; AGENTS.md and .opencode/OWNER_AUTHORITY.md retain that rule. D-211/D-212 authorised specific UI work only. docs/35 has an inconsistent top summary saying Stage B is authorised while later sections say otherwise. The top summary is not standalone gate authority.
5. Record exact owner-approved Stage B scope before editing app source. A roadmap alone is not Stage B permission.
6. Establish reproducible baseline: install/use compatible Gradle wrapper/toolchain, run existing build and test tasks, retain logs, identify failures before editing, and record dependency versions. Diagnose a failed clean baseline before large feature changes.
7. Check application ID, signing configuration, secret handling and tracked files without printing secret values. Ensure .env remains ignored; never commit credentials.
8. Reconcile planning branches. PR #1 is an older draft documentation PR based on a pre-current main SHA. Compare individual changes against current main before merging anything. Do not merge it blindly. Treat the old docs/dayseed-findings-and-screen-architecture branch as historical findings material.
9. Update canonical documentation only where necessary and ensure decisions, requirements, traceability and handover agree.

**Exit conditions:**
- GitHub main and AI Studio workspace are reconciled to an exact SHA.
- A clean baseline result is recorded accurately as PASSED, FAILED or BLOCKED with command/toolchain/log evidence.
- Current v2 schema and v1-to-v2 migration are confirmed from source.
- Owner-reserved product changes in §5 have a decision-log disposition.
- Implementation gate and phase scope are explicit.
- No application source is edited before these conditions are satisfied.

### Phase 1 — Domain, data-integrity and historical-record foundation

**Purpose:** Make the rules underneath every screen correct before expanding screens. Every later feature depends on consistent data.

**Work:**

**A. Domain model**
- Reconcile three new-habit measurement modes: Quantity, Duration, Simple check-off.
- Preserve legacy EFFORT_AWARE/EFFORT_LEVELS decoding and existing records losslessly.
- Model optional habit category separately from measurement and schedule.
- Clarify weekly target semantics: measured quantity/duration totals; distinct qualifying check-off days; session-count quantity units.
- Implement D-040 bands including Extra/Huge with overflow-safe exact-ratio comparisons.
- Separate daily progress, daily target status, weekly contribution and weekly target status.
- Centralise schedule/period calculations in testable domain functions.
- Route date/time use through DayseedClock or injected clock values.

**B. Schedule and exception model**
- Enforce D-194 in the repository/domain transaction: after today's qualifying activity, do not overwrite today's effective configuration; offer Apply tomorrow.
- Preserve historical schedule/target meaning through effective-dated habit_versions.
- Implement selected weekdays with fixed D-195 weekday mask.
- Implement flexible weekly target calculations using user-selected week-start.
- Add owner-directed one-off REST_DAY exception after reconciling its extension to D-193.
- Define transaction/correction rules, including preventing contradictory activity/rest states.
- Keep pause/archive/completed lifecycle effective-dated.
- Backdated additions, edits and deletions recompute affected day/week results and streak runs without mutating unrelated historic config.

**C. Streak/Grove persistence and domain engine**
- Define stable streak/run identities per habit and terminal reason.
- Derive active streak state from source records/effective schedules where possible; avoid a second mutable count that can drift.
- Persist completed/ended run snapshots required by Grove: count, cadence, dates, reason and visual identity.
- Define mixed-cadence comparison for the main indicator; recommended elapsed-run span with correct unit, while detail shows native count/cadence.
- Define zero, partial hold, rest hold, goal-met increment, flexible-week completion, pause/archive end, completion freeze and schedule-change continuity.
- Reconstruct deterministically after backdating, correction, deletion and restore.
- Permanently deleting a habit/delete-all removes appropriate private Grove data; archiving/completion preserves it.

**D. SQLite v3 and migration**
- Reconcile actual v2 schema with docs/03 and docs/30; do not copy an old proposed schema blindly.
- Design a reviewed v2-to-v3 migration for selected category, rest exception, Grove/streak and measurement changes.
- Retain v1-to-v2 migration. Upgrade path is v1 -> v2 -> v3, not a shortcut that strands v1 users.
- Define CHECK constraints, FKs, unique indexes, idempotency and rollback before schema changes.
- Include new entities in portable backup export/import, conflict detection/resolution, deletion and migration.
- Keep derived totals reconstructable from source records unless a documented integrity reason requires persistence.
- Fail closed for legacy rows/backups that cannot be mapped losslessly; never silently drop unknown data.

**E. Repository and Undo**
- Make create/update/delete/undo transactional and scoped to stable IDs.
- Add guarded Undo to numeric entry deletion as well as supported create/edit flows.
- Never reverse a newer edit or unrelated session.
- Preserve idempotency across retries/restore.
- Recompute dependent aggregates after correction/deletion.

**Exit conditions:**
- Fresh schema and v1 -> v2 -> v3 migration tests pass from synthetic fixtures.
- Migration failure leaves original database intact/recoverable.
- D-194 same-day cases pass.
- Weekly numeric totals, check-off days, session counts and rest exceptions are deterministic for all configured week starts.
- All progress-band boundaries/tiny targets/large integers pass.
- Partial, rest, missed, pause, archive, complete, schedule-change and backdated-correction streak cases pass.
- Backup/export/import/merge/conflict/delete mappings cover every new persistent entity.
- No untested source-of-truth or migration semantics are carried into UI work.

### Phase 2 — Complete habit experience, five-destination navigation and living Grove

**Purpose:** Make the core daily habit workflow complete from habit creation through logging, correction, management and progress review.

**Work:**

**A. Navigation and screen ownership**
- Replace four destinations only after N1 supersession is recorded: Today, Habits, Journal, Insights, Settings.
- Use Navigation Compose or another consistent route/state pattern for secondary destinations.
- Implement correct Back behaviour: return to originating screen/detail, not unconditionally jump to Today.
- Preserve selected dates/records when navigating to detail and back.
- Keep Settings as the existing hub/panel design.

**B. Today**
- Preserve D-210/D-211 daypart heading/artwork and broad visual direction.
- Compact hierarchy: date/greeting, restrained streak/progress indicator, action-focused habit list, short Journal entry point, compact Collected moments link.
- Keep undo/comeback/retrospective prompts conditional and non-blocking.
- Move edit/history/session detail out of the default card.
- Show scheduled habits and a compact Not scheduled today link/group.
- Display correct week-to-date amount for flexible weekly numeric habits.
- Make G2 tappable to shared Growth/date detail.
- Do not make streaks the only/dominant success metric.

**C. Habits and Habit Detail**
- Dedicated Habits destination showing every active habit, even if not scheduled today.
- Active, Paused, Completed and Archived groups/filters.
- Add, search, reorder, lifecycle and empty states.
- Habit Detail includes category, measurement, target, schedule, progress, history, entries, notes and lifecycle actions.
- Mark Completed distinct from Pause, Archive and Permanent Delete.
- Archive preserves history; permanent deletion follows destructive confirmation/cascade contract.

**D. Create/Edit**
- Replace tall dialog with structured full-screen form.
- Separate identity/category, measurement/units, target/period, schedule and optional reminder.
- Offer Quantity, Duration and Simple check-off for new habits.
- Explain days/week vs sessions/week.
- Show fields only when relevant to selected mode.
- Allow normal active weekday selection; remaining days are neutral.
- Offer one-off Take a rest day on a scheduled date, distinct from Skip/Missed/Complete/Record.
- Apply D-194 in UI and repository.
- Keep category separate from calculation semantics.

**E. Record/correct/delete**
- Focused Record Session flow for actual amount, optional per-session note and explicit local date for backdating.
- Correct one selected entry.
- Display individual sessions and daily/weekly totals separately.
- Wire updateHabitEntry through all relevant routes.
- Guarded Undo for eligible numeric deletion/edits.
- Do not make Journal/permanent habit deletion snackbar-undoable.
- Preserve intentional zero as a distinct saved record.

**F. Individual streak/branches/Grove**
- Stable branch per active habit, unique visual identity.
- Branch grows according to that habit's streak/cadence.
- Main indicator selects longest active run using agreed mixed-cadence comparison and correct unit label.
- Tapping opens individual streaks and Grove entries.
- Ending one run creates Grove record and affects no other habit branch.
- Completion preserves final branch as achievement.
- Pause/archive close run with appropriate terminal reason.
- Animation event-driven and plays once on run-ending event, not on every Today render.
- Use Compose Canvas/vector/state animation; no runtime AI-generated art.
- Use stable colours plus shape/label cues for distinguishability.
- Respect reduced motion and lower-end Android performance.

**Exit conditions:**
- Users can create each new-habit mode/schedule without irrelevant fields or invalid combinations.
- Legacy effort records remain readable/correctable.
- Five destinations and secondary routes work with correct Back navigation.
- All habit lifecycle groups are discoverable.
- Complete/log/partial/met/above/zero/correct/delete/undo persist after restart.
- Rest holds a streak but creates no activity and does not reduce a weekly target.
- Individual streaks/Grove survive restart and backdated recalculation.
- Today remains compact and does not conceal other habits' progress.

### Phase 3 — Journal, shared calendar, weekly board, Growth Mark and Insights

**Purpose:** Make Journal, dates, mood and habit records independently stored yet easy to inspect together.

**A. Journal destination**
- Move Archive out of nested History into direct Journal navigation.
- Preserve A2: chronological timeline, date jump/calendar, local title/body search and mood filtering.
- Add prominent New Entry.
- Add Clear search/filters on no-results state.
- Preserve multiple entries on same date.
- Preserve local drafts and stable draft-to-entry identity.
- Confirm saved-entry deletion; distinguish discarding a draft.
- Preserve primary/secondary mood rules and local-only storage.
- Journal entries never count as habit activity or change habit status.
- Editor handles keyboard-open, large-text and date editing without obscuring actions.

**B. Shared calendar and Day Detail**
- Shared calendar/date-detail component for Insights and Growth Mark.
- Month navigation and selected date using saved week-start consistently.
- Day Detail shows all habit results, mood and Journal entries.
- Journal preview opens actual saved entry; habit record opens correction/detail.
- Date cells and controls have screen-reader labels and non-colour cues.

**C. Daily/weekly calendar and board**
- Daily view shows each habit's contribution/status for a selected date.
- Weekly view has seven date columns aligned to saved week-start, per-habit rows/cards and separate weekly total/denominator.
- Numeric weekly target shows week-to-date amount/unit.
- Days/week counts distinct qualifying dates.
- Sessions/week counts individual sessions; multiple sessions/date can count separately.
- Never count unscheduled/paused dates as missed.
- Flexible weekly target does not create daily misses for dates that are not individually required.
- Weekly target met early appears met for that week; no subsequent daily misses for that same flexible target.
- Previous/next period and Jump to Today.
- All-habits view uses individual small marks; single-habit filter may use stronger cell colour/pattern.
- Use owner's initial status encoding: solid green met, pale green partial, darker green/flame above, dotted missed, neutral rest/unscheduled/paused/zero/no-record.

**D. Mood/Journal markers**
- Small primary mood symbol beside date.
- One small Journal marker for one or more entries; count/group mark for multiple.
- Avoid several large icons in cells.
- Tapping opens all entries/moods in Day Detail.

**E. Growth/Statistics**
- G2 remains current-calendar-month total of saved duration from DURATION habits only.
- G3 reuses shared month/date/session list.
- Preserve canonical duration normalisation/source precision/display-only rounding.
- Explicit zero distinct from no session.
- Corrections/deletions recompute G2/G3 from source rows.
- Statistics grouped into Overview, Habits, Mood and Growth rather than one uncontrolled wall of charts.
- Preserve Mood Insights week/month filters and previous-period comparison only where the metric is explainable.
- Replace old Tiny/Usual/Extra effort distribution for new habits with derived target-relative progress bands and correctly named counts.
- Define active dates, sessions, habit-days met, unique habits completed and weekly opportunities separately.

**Exit conditions:**
- Journal create/search/filter/open/edit/delete complete.
- Multiple entries/date retrievable.
- All dates, including older than 30 days, navigable.
- Daily/weekly totals match source records for Monday, Sunday and Saturday week-start fixtures.
- All date statuses/markers pass.
- Weekly target met early is not mislabelled as later daily success/miss.
- G2/G3/Insights recompute after corrections/deletions.
- Journal never changes habit completion state.

### Phase 4 — Android operations, reminders, backup/restore, privacy, accessibility and resilience

**Purpose:** Make Dayseed trustworthy as a real Android app, not merely a collection of UI screens.

**A. Real notification delivery**
- Add notification channel and actual posting service.
- Request POST_NOTIFICATIONS at runtime on Android 13+ after explanation/consent. Denial must not block core features.
- Replace false delivery path: eligibility must not set DELIVERED before Android confirms a post.
- Persist accurate lifecycle such as eligible/scheduled/attempted/delivered/suppressed/failed.
- Integrate approved reminder opt-in, per-habit time, quiet hours, caps, weekly goal acknowledgement, weekly inactivity check-in, week-start reflection and optional tips.
- Persist logical occurrence keys; deduplicate after process restart/reschedule.
- Re-check schedule/config/lifecycle/date/week/activity/opt-in/permission/quiet-hours/cap/dismissal immediately before posting.
- Never post for paused, archived, completed or deleted habits.
- Use WorkManager for reliable deferrable background work and/or inexact AlarmManager where needed. Exact alarms are not the default for ordinary reminders; any future exact timing request needs explicit product rationale and Android access/permission review.
- Reconcile on boot, app update, timezone change, week-start change, schedule edit, restore, deletion and restart.
- Keep cancellation outbox retryable; unavailable NotificationManager/cancel failure is not success.
- Never put Journal text, mood, habit notes or other sensitive content in notifications/logs.

**B. Backup and restore**
- Add real user-selected save/open through Android Storage Access Framework.
- Keep readable Journal export distinct from complete versioned Dayseed backup.
- Preserve optional password encryption and test wrong password/tampering/corrupt/truncated/unsupported payload.
- Verify safety backup before Replace/Merge.
- Validate every record before live mutation; transactionally apply or roll back.
- Preserve unresolved conflict payloads and durable restore-session state.
- Include category/rest/streak/Grove entities in backup/version/conflict mapping.
- Never include secrets, OS notification IDs as active work, cache totals or transient UI state.
- Unsupported unsafe conflicts stay unresolved; never offer Keep both if uniqueness forbids it.
- Rebuild future notifications from current settings/permission, not imported device-specific schedules.

**C. Privacy/local storage/deletion**
- Preserve offline/account-free initial v1; no cloud, analytics, advertising SDK, billing or runtime AI analysis.
- Verify dependency list, permissions, code network calls, provider flows and exports.
- Verify allowBackup=false and data-extraction rules on supported Android versions/device-transfer paths.
- Test permanent habit deletion, Journal delete, draft discard, conflict resolution and delete-all separately.
- Preserve independent Journal entries when a habit is deleted; clear optional links, not reflection text.
- Include all new private entities in delete-all.
- Never claim exported/provider/other-app/OS copies were deleted without evidence.

**D. Accessibility, usability and performance**
- TalkBack traversal for navigation, date cells, habit actions, menus, sheets/dialogs, progress labels and errors.
- 200% text and compact width without clipping or inaccessible overflow.
- Measure contrast; do not infer it from tokens.
- Verify keyboard, focus restoration, touch sizes and Back handling.
- Reduced-motion behaviour for plant/Grove and theme transitions.
- Test long habit lists, large histories, hundreds of entries and multiple Journal entries/day.
- Use synthetic fixtures; never expose Journal text in logs.
- Avoid full DB scans on every Compose recomposition; profile before optimising.

**Exit conditions:**
- OS notification only marked delivered after successful post.
- Denied permission/suppression accurately represented.
- Reboot/restart/timezone/day-boundary tests avoid duplicates/stale reminders.
- Backup file save/open through system document APIs; failures preserve source data.
- Replace/Merge/conflict recovery atomic and interruption-safe.
- Privacy/delete tests match data actually removed/preserved.
- Airplane-mode core use works.
- TalkBack/large text/contrast/focus/keyboard/reduced-motion/compact-width checks have executed evidence.
- Performance acceptable on representative lower/mid-range Android devices.

### Phase 5 — Stage B integration closeout and evidence package

**Purpose:** Deliver one complete, reproducible, committed initial-v1 implementation ready for independent validation. This is still implementation work, not independent Stage C validation.

**Work:**
- Reconcile code/docs to a committed implementation revision.
- Remove stale instructions that contradict implemented reality; retain history explicitly labelled.
- Update docs/09, docs/25, docs/28, docs/12, README and affected architecture/screen contracts based only on actual completion.
- Ensure root plan and actual source agree.
- Run full local unit/Robolectric/UI test suite after integration.
- Build a clean Android debug artifact and verify installation/launch where the authorised Stage B environment supports it.
- Capture exact SHA, toolchain, commands, logs, counts, failures, warnings and artifact location.
- List every NOT RUN check.
- Provide owner handover: implemented scope, remaining Stage B defects, exact Stage C checklist and limitations.

**Exit conditions:**
- Agreed initial-v1 features are integrated and committed.
- Unit/repository/migration/backup and Compose UI tests pass, or failures are openly reported and triaged.
- Clean build is reproducible from committed wrapper/version catalog.
- Debug credentials are not used for production release configuration.
- Traceability maps in-scope requirements to implementation and executed tests.
- Implementation is ready for independent review; no release-ready claim is made.

### Stage C — Independent technical validation (separate gate)

Performed independently against the exact committed Stage B SHA:

- Clean checkout build and full test run.
- v1 -> v2 -> v3 migration with valid/invalid synthetic databases.
- Fresh install, force-stop/restart and process-death persistence.
- Offline/airplane-mode core workflows.
- Schedule/week-boundary/timezone/backdated/rest/streak/Grove edge cases.
- Journal multiplicity, draft-save, edit/delete confirmation and retrieval.
- Numeric update/delete/undo/idempotency.
- Notification permission allow/deny, scheduling, suppression, reboot, quiet hours, time changes, week start and caps.
- Portable backup, encryption, invalid files, Replace/Merge, conflicts, interruption recovery and delete-all.
- Actual manifest/data-extraction behaviour, permissions, dependency/network/privacy inspection.
- TalkBack, 200% text, contrast, focus, keyboard, reduced motion and small-screen tasks.
- Physical-device operation and performance on representative Android devices.

Stage C must produce a separate report with exact commit, device/toolchain, commands, logs and PASS/FAIL/BLOCKED/NOT RUN for every test group. Stage B self-tests do not count as independent Stage C evidence.

### Stage D — Owner acceptance and release readiness (separate gate)

Present the verified release candidate to the owner with the exact commit/artifact, independent Stage C evidence, known limitations, migration/backup/restore/deletion evidence, privacy/security/permission findings, accessibility/device results, release-signing setup and application-ID confirmation. Only the owner can accept the release candidate. No publication or release-ready claim before explicit Stage D acceptance.

---

## 7. Large-workstream execution with Gemini 4 Argon

Google's official Gemini 4 Argon material positions it for complex software engineering and multi-step agent work. Its large context helps it inspect interconnected files, but its output limits and potential for incorrect edits mean one phase should be assigned at a time.

### 7.1 Recommended operating pattern

- Select exact model ID gemini-4-argon in Google AI Studio and confirm it is available to the current workspace/account.
- Use High thinking for architecture, migration and cross-layer integration.
- Provide this plan, canonical decisions/specifications and exact current Git SHA.
- Give the model one whole phase from §6 with complete acceptance criteria. Do not split the phase into dozens of unrelated prompts, and do not combine all phases into a single all-at-once rewrite.
- Require it to inspect current source and tests before edits, not trust old summaries.
- Require cross-layer work within the phase: UI, ViewModel, domain, repository, database, migration, backup/restore, tests and docs as impacted.
- Require a brief data/architecture design before changing persistent structures; then implement the whole phase and run its regression suite.
- Keep commits reviewable; never force-push or overwrite newer remote main.
- Require exact files changed, purpose, migration/backup impact, commands run, test totals, failures and NOT RUN items.
- Run a distinct read-only re-audit against acceptance criteria. Self-review by the implementation pass is not independent evidence.
- If Argon times out/context ends, recover from the last verified Git commit. Do not assume a partial tool action or uncommitted edit completed.

### 7.2 Model limitations

Strong model benchmarks do not prove that code preserves Dayseed's data, historical semantics, privacy or Android notification behaviour. Treat generated code as a skilled but fallible implementation draft. Compile/test it, inspect SQL/transaction boundaries, validate backup compatibility, test real UI journeys and independently review Android runtime behaviour. The model must not make an owner-reserved decision because a particular implementation is easier.

Official references:
- Gemini 4 Argon: https://ai.google.dev/gemini-api/docs/models/gemini-4-argon
- Model card: https://deepmind.google/models/model-cards/gemini-4-argon/

### 7.3 Android notification technology

Android 13+ requires POST_NOTIFICATIONS runtime permission for normal notifications; the manifest declaration alone is insufficient:
https://developer.android.com/develop/ui/compose/notifications/notification-permission

Android recommends WorkManager for persistent deferrable background work. AlarmManager may be used for timing needs, but exact alarms have additional access/permission considerations and should not be the default for ordinary habit reminders:
- https://developer.android.com/develop/background-work/background-tasks/persistent
- https://developer.android.com/develop/background-work/services/alarms

Use the least powerful Android mechanism that meets the approved reminder requirement. Do not promise exact delivery times where Android may defer work.

---

## 8. Cross-cutting acceptance rules

1. **Preserve history:** changing targets, units and schedules must not silently reinterpret old records.
2. **One source of truth:** avoid duplicate mutable derived values unless transactionally maintained and tested.
3. **Idempotency:** retry of one logical submission counts once; intentional additional session counts separately.
4. **Explicit zero:** distinguish a saved zero from no record across database, UI, calendar, backup and tests.
5. **No false completion:** partial is not target met; weekly total is not the same as daily target.
6. **No false delivery:** eligible/evaluated reminder is not an OS notification successfully posted.
7. **Offline-first:** no network/account/cloud requirement may block core use.
8. **Privacy:** never log/upload Journal text, mood, habit notes, backup passwords, secrets or private user data.
9. **Accessibility:** no colour-only status; support screen readers, large text and reduced motion.
10. **Evidence integrity:** report PASSED only when a command/check actually ran on the stated commit and its output was inspected.
11. **Scope protection:** monthly targets, ads, analytics, cloud sync, runtime AI and social comparison remain deferred.
12. **Owner authority:** recommendations, model consensus and code convenience do not become owner decisions.

---

## 9. Repository/documentation reconciliation

The inspected repository contains planning history that must not be mistaken for current source status:

- This root implementation_plan.md is the canonical consolidated engineering roadmap.
- README.md contains historical v1-only schema/missing-migration statements that conflict with current v2 source. Update those statements to describe v2 source/v1-to-v2 migration existence while retaining the fact runtime migration/build was not verified in this audit.
- .aistudio/artifacts/brain/.../implementation_plan.md is a historical generated artifact. Do not use it as active plan; do not silently delete historical context.
- docs/35-PLANNING-READINESS-REPORT.md has a contradictory top status line and later sections preserving Stage B as not yet authorised. The later authority chain and D-208/D-209 control until owner explicitly resolves the gate.
- planning/ux-traceability-closure-2026-10-28 and open draft PR #1 are based on a pre-current main. Review/rebase individual documentation changes before any merge; do not use that PR head as implementation base.
- docs/dayseed-findings-and-screen-architecture is an earlier findings branch; this plan supersedes its roadmap role.
- config/agent-governance-model-registry is an agent-configuration branch. Do not cherry-pick/merge as part of product implementation unless requested; current main already contains shared agent policies/model registry.

---

## 10. Stage gate and immediate next action

**This commit is the audit and implementation-plan deliverable only. It does not edit application source or execute Stage B.**

The current repository authority chain says:

- D-208 opened the planning gate only and did not authorise feature implementation or technical validation.
- D-209 corrected the Stage A -> Stage B implementation -> Stage C independent validation -> Stage D owner acceptance sequence, but says Stage B still requires a separate explicit written owner instruction.
- D-211 and D-212 authorised two specific Stage B UI work items (daypart layout and Settings restructure); they do not automatically authorise the entire initial-v1 programme.
- AGENTS.md, .opencode/OWNER_AUTHORITY.md and docs/19-AI-STUDIO-GITHUB-WORKFLOW.md retain the rule that broad Stage B implementation needs explicit owner authorisation.
- docs/35 has a conflicting top summary saying Stage B is authorised while its later gate sections say otherwise. The canonical decision log and owner-authority documents take precedence over that inconsistent top summary.

The owner explicitly authorised the complete Stage B initial-v1 scope on 2026-11-01 under D-213, tied to this plan on `planning/system-wide-audit-implementation-plan-2026-11-01`. Record D-213 in the canonical decision/authority documents before feature work. Stage B includes Phase 0 and Phases 1–5. Stage C independent validation remains NOT AUTHORISED until separate written owner approval after Stage B is committed; Stage D release acceptance remains NOT OPEN until the owner accepts the Stage C-verified candidate.

**Recommended next action:** after Stage B is explicitly opened, start Phase 0 preflight; then implement Phases 1–5 as the large workstreams above, preserving existing working code and committing each phase to a reviewable Git branch. Do not merge stale PR #1 or begin from an outdated AI Studio import.

---

## 11. Phase 0 Preflight & Section 5 Reconciliation Addendum (2026-11-01, D-213–D-218; Reconciled in `docs/37`)

During **Phase 0 (Implementation Preflight, Repository Reconciliation and Baseline Verification)** on `2026-11-01`—and its subsequent verification pass against `main` commit `ebeca3ebff1155c220f8e09f1e00e07fdb28209b` (recorded in **[docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md](docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md)** after PR #3 was closed as superseded)—the workspace, declared Gradle/SDK configuration (`AGP 9.1.1`, `Kotlin 2.2.10`, `Compose BOM 2024.09.00`, `compileSdk 36`, `targetSdk 36`, `minSdk 26`, `applicationId = "com.aistudio.dayseed.wkqmfl"`), 12-table v2 SQLite schema (`habits`, `habit_versions`, `habit_entries`, `habit_day_outcomes`, `habit_date_notes`, `journal_entries`, `journal_drafts`, `app_settings`, `reminder_deliveries`, `restore_sessions`, `restore_conflicts`, `notification_cancel_outbox`), and baseline tests (`gradle :app:testDebugUnitTest --rerun-tasks` → **30/30 PASSED**; `compile_applet` → **PASSED**) were verified, and `D-213`–`D-218` were reconciled in `docs/09-DECISION-LOG.md` and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`:

- **`D-213` — Full Stage B initial-v1 implementation authorisation (2026-11-01):** Owner explicitly authorised complete Stage B implementation across Phase 0 and Phases 1–5 on the native Android stack (`Kotlin`, `Jetpack Compose`, `SQLiteOpenHelper`). Stage C (independent technical validation) and Stage D (final owner acceptance) remain separate later gates and are not yet open.
- **`D-214` — Four-section Settings arrangement refinement (2026-10-31; renumbered from workspace `D-213`):** Preserves the separately authorised 2026-10-31 Settings refinement (`SettingsGroup.kt`, `SettingsPanels.kt`, `SettingsScreen.kt`, `SettingsSupport.kt`, `DayseedSettingsValidationTest.kt`) committed in `ebeca3ebff1155c220f8e09f1e00e07fdb28209b` alongside the Phase 0 documentation updates.
- **`D-215` — Resolves `§5.1` Primary navigation reconciliation:** Approved **5-tab navigation (`Today`, `Habits`, `Journal`, `Insights`, `Settings`)**, superseding 4-tab `N1` (`D-042` / `O-2`). Historical habit calendar lives inside `Insights` (`Calendar & history` tab), and `Day Detail` / `Habit Detail` are secondary drill-down routes with `BackHandler`.
- **`D-216` — Resolves `§5.2` new-habit measurement modes, legacy Effort-aware preservation, and progress presentation:** Approved **`Quantity`, `Duration`, and `Simple check-off` (`COMPLETION_ONLY`)** for new habits with automatic target-relative progress derivation, lossless preservation of existing/imported `EFFORT_AWARE` / `EFFORT_LEVELS` records, separate optional activity category, and distinct weekly check-off (`N days/week`) vs. weekly session count (`N sessions/week`). The owner confirmed **Option A** for progress: preserve all seven D-040 names and exact thresholds (`Tiny`, `Small`, `Halfway`, `Nearly there`, `Goal met`, `Extra`, `Huge`); the earlier five-label parenthetical is not approved. Pair each detailed band with both a distinct encouragement quote and a distinct motivation quote selected for the actual-to-target percentage. Use compact coarse Calendar/History groupings only under §5.6. Keep the wording supportive and nonjudgmental, without pressure to outperform past results. See `docs/37-PHASE-0-PREFLIGHT-RECONCILIATION.md` §6.2.
- **`D-217` — Resolves `§5.3` Individual habit streaks, elapsed-span display, neutral explicit zero, and permanent Grove:** Approved **individual per-habit streaks, branch identity, and colour**, `Today` longest-active-streak display using **elapsed active-run span with explicit `days` / `weeks` unit**, streak-holding on positive partial progress or rest, **neutral explicit `0`** (saved record that neither holds nor increments an activity streak), and the permanent **Grove** (`MISSED`, `PAUSED`, `ARCHIVED`, `COMPLETED`) with optional habit lifetime goal duration.
- **`D-218` — Resolves `§5.4` One-off `Take a rest day` (`REST_DAY`) date exception:** Approved extending `D-193` with a single-date per-habit `REST_DAY` exception always labelled **`Take a rest day`** (never `Skip` or `Mark missed`) that holds the streak neutrally without creating a fake `habit_entries` row, inflating check-in/active-day totals, or reducing a flexible weekly target (`3x/week` stays `3x/week`).

**Phase 1 Readiness (Reconciled):** Phase 1 work on `D-194` effective-dating enforcement (`AUD-001`), flexible weekly target persistence and status (`AUD-002`–`AUD-005`), schema v2-to-v3 migration (`habit_pauses`, `habit_date_exceptions`, `habit_streak_runs`, habit metadata), individual streaks/Grove (`D-217`), one-off rest days (`D-218`), neutral explicit zero (`AUD-015`), and `Long` ratio overflow prevention (`AUD-009`) can begin immediately. Implementing the owner-confirmed seven D-040 progress-band labels, separate compact Calendar/History groupings, and the paired encouragement/motivation quote presentation is now unblocked (`AUD-008`, `AUD-029`); Phase 1 must add boundary and quote-selection tests.

---

## 12. Phase 1 Implementation & Corrective Completion Addendum (2026-11-02 / 2026-11-03, Stage B Self-Check)

Following the owner's approval of the Phase 1 Implementation Plan (Revision 7, resolving `D-219`–`D-227` and `U-1`–`U-13`, including eliminating a separate `habit_pauses` table in favour of deriving pause ranges from `habit_versions.lifecycle_state = 'PAUSED'`) and the subsequent **Phase 1 Corrective Completion Plan (Revised)** on `2026-11-02` / `2026-11-03`:

1. **`D-040` / `D-216` Progress-Band Classification, Labels, Fractional-Safe Badges & Dual Quotes (`Models.kt`):**
   - Corrected `ProgressBandClassifier.classify` (`scaled <= t150 -> ProgressBand.EXTRA`) so exact `150%` (`150/100`, `3/2`, `300/200`) classifies as `ProgressBand.EXTRA` and only strictly `> 150%` classifies as `ProgressBand.HUGE`.
   - Updated `ProgressBand` visible `label` strings (`Zero logged`, `Tiny`, `Small`, `Halfway`, `Nearly there`, `Goal met`, `Extra`, `Huge`) and fractional-safe `shortBadge` strings (`0%`, `>0–<25%`, `25–<50%`, `50–<75%`, `75–<100%`, `100%`, `>100–150%`, `>150%`).
   - Preserved both `encouragementQuote` and `motivationQuote` on every `ProgressBand` (`D-216` Option A) and kept `CompactHistoryBucket` (`NONE`, `ZERO_LOGGED`, `PARTIAL`, `REST_DAY`, `TARGET_MET`, `OVER_TARGET`) separate (`§5.6`).
2. **SQLite v3 Schema (`DATABASE_VERSION = 3`, `D-219`), Chained `v1 -> v2 -> v3` Migration, `U-11` Weekly Conversion & In-Place v3 Reconciliation (`DayseedDatabaseHelper.kt`):**
   - Updated `habit_versions` CHECK constraints to allow weekly `COMPLETION_ONLY` and `EFFORT_AWARE` habits (`target_period = 'WEEK'`) to store `target_value >= 1` (`N days/week`) while keeping daily check-off `target_value IS NULL` and allowing legacy weekly `NULL`.
   - Added `'habit_streak_run'` to `restore_conflicts.record_type IN (...)`.
   - Updated `migrateV1ToV2FailClosed` so v1 `QUANTITY` and `DURATION` daily records migrate into `habit_entries` (`value_units = 1`) rather than `habit_day_outcomes`, and archived v1 habits with historical records retain an `ACTIVE` effective version covering their record dates before transitioning to an `ARCHIVED` effective version on the day after their latest record.
   - Updated `migrateV2ToV3FailClosed` and `validateV2DataBeforeMigration` to convert legacy v2 weekly schedules `(DAILY, WEEK)` -> `(FLEXIBLE_WEEKLY_TARGET, WEEK, preferred_weekdays_mask = null)` and `(SELECTED_WEEKDAYS, mask, WEEK)` -> `(FLEXIBLE_WEEKLY_TARGET, WEEK, scheduled_weekdays_mask = null, preferred_weekdays_mask = mask)` (`U-11`), and fail-closed on malformed v2 `habit_versions` schedule/period/mask/target rows.
   - Implemented `reconcileExistingV3SchemaIfNeeded(db)` invoked from `onOpen(db)` to idempotently and transactionally rebuild `habit_versions` and `restore_conflicts` on any existing v3 database (`DATABASE_VERSION = 3` per `D-219`) that still has the older CHECK constraints, preserving all records, foreign keys, indexes, and triggers and rolling back cleanly on failure.
3. **Complete v3 Portable Backup, `REPLACE`, `MERGE` & Per-Conflict Destructive Authorization (`DayseedRepository.kt`, `StreakAndGroveEngine.kt`):**
   - Preserved `targetValue >= 1` in `saveHabitSync` for weekly `COMPLETION_ONLY` and `EFFORT_AWARE` habits.
   - Updated `MutableRun` in `StreakAndGroveEngine.kt` to preserve prior `qualifiedAtUtcMs` when `activeSpanDays >= 7` and reset `qualifiedForGrove = false` / `qualifiedAtUtcMs = null` when a previously qualified run shrinks below 7 days.
   - Updated `REPLACE` (`importBackupArraysDirect`) to import valid `habit_streak_runs` (preserving original `run_id`, `created_at_utc_ms`, and `qualified_at_utc_ms` across post-restore recalculation) and seed `week_start_history` for legacy v2 backups at the earliest restored local date using the restored `week_start` setting without overwriting `week_start`.
   - Updated `MERGE` (`mergeBackupWithConflictDetection`) and `resolveRestoreConflictSync` to detect and resolve conflicts across `"habit"`, `"habit_date_exception"` (requiring explicit per-conflict `allowDestructiveActivityRemoval == true && authorizedDestructiveConflictId == conflictId` before `USE_BACKUP` removes local activity on that date), `"habit_streak_run"`, `"week_start_history"`, and `"journal_draft"`, while disallowing `KEEP_BOTH` for those five record types.
4. **Executed Build & Test Verification Evidence (Stage B Self-Check, `2026-11-03T06:29:54Z` – `2026-11-03T06:30:31Z`):**
   - `compile_applet`: **PASSED** (`Build succeeded - the applet is compiled`).
   - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 57/57** executed JUnit test methods across 4 test classes (`Phase1ImplementationTest`: **27/27** JUnit methods covering `T1-01` through `T1-42` [XML `2026-11-03T06:30:19.975Z`, `10.118s`]; `DayseedAcceptanceTest`: **12/12** [XML `2026-11-03T06:30:30.118Z`, `1.386s`]; `DayseedSettingsValidationTest`: **10/10** [XML `2026-11-03T06:30:31.510Z`, `0.011s`]; `DayseedVisualResolutionTest`: **8/8** [XML `2026-11-03T06:30:31.525Z`, `0.06s`]), **0 failed, 0 errors, 0 skipped** (`BUILD SUCCESSFUL in 31s`).
   - **Stage gates:** Stage B Phase 1 self-checks are complete. Stage C (independent technical validation) and Stage D (final owner acceptance) remain separate later gates and are not claimed here.





---

## 13. Phase 1 human/device validation and Phase 2 handoff (2026-11-03)

The owner requested that tests requiring human intervention be consolidated and executed together after the planned implementation phases, rather than interrupting each phase with device-testing sessions.

- **Automated tests are not deferred.** Each phase must run its relevant automated tests and full regression suite as specified. The Phase 1 remediation tests merged through PR #5 must be executed; the earlier 57/57 self-check predates that remediation and is not evidence for the final merged test set.
- **Human/device checks are deferred as a group.** Phase 1 cases are recorded in [docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md](docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md). They remain NOT RUN until Stage B implementation phases are complete, the candidate build/commit is frozen, and Stage C is explicitly authorised.
- The register includes on-device v2-to-v3 upgrade, process-death persistence, D-194 configuration protection, weekly target/week-start behaviour, REST_DAY, individual streak/Grove lifecycle, backdated correction, portable backup/restore, and habit-deletion Undo. Cases that depend on later screens or Android system workflows are labelled with their phase dependency.
- Phase 2 is the complete habit experience, five-destination navigation, Habit Detail, record/correct/delete/Undo flows, and individual streak/Grove presentation. The owner has already supplied the Phase 2 implementation assignment to Gemini; its temporary copy-ready prompt was intentionally removed from the repository after use so future phase handovers are not confused by a consumed prompt. Use the approved decisions, contracts, roadmap, and current implementation state when preparing the Phase 3 assignment.
- Phase 2 must append its own human/device cases to the same register; Phase 3 and Phase 4 must do likewise. Do not execute human tests during those implementation phases.
- The known main SHA when this addendum was prepared is `bf19bc11c37dd9a7929594871266bdff07dfc94e`. Every implementation session must fetch and verify the latest remote SHA before editing.

This addendum records the owner's test scheduling instruction. It does not mark Phase 1's post-remediation automated suite as passed, does not open Stage C, and does not authorise Stage D or release.

---

## 14. Phase 2 Implementation & Verification Addendum (2026-11-03, Stage B Self-Check)

On `2026-11-03`, **Phase 2 (`Complete Habit Experience, Five-Destination Navigation, Individual Streaks and Living Grove`)** was implemented on top of the `bf19bc11c37dd9a7929594871266bdff07dfc94e` baseline:

1. **Five-Destination Navigation & Secondary Route Stack (`DayseedViewModel.kt`, `MainActivity.kt`, `HistoryScreen.kt`):**
   - Implemented the 5-tab bottom navigation (`Today`, `Habits`, `Journal`, `Insights`, `Settings` under `D-215`) and deterministic `SecondaryRoute` stack (`HabitDetail`, `CreateEditHabit`, `RecordSession`, `LivingGrove`, `CollectedMomentsDetail`) with unified `BackHandler` and top-bar Back navigation restoring exact parent tab, selected date, filters, search query, and scroll position.
2. **`Today` Screen, `HabitCard` & `CollectedMomentsDetailSheet` (`TodayScreen.kt`, `HabitCard.kt`, `GrowthMark.kt`, `Models.kt`, `DayseedRepository.kt`):**
   - Preserved `D-210`/`D-211` daypart header artwork, added the longest active streak indicator pill (`selectLongestActiveRun`) with explicit `"days"` vs `"weeks"` units, separated scheduled habits from collapsible `"Not scheduled today"` habits (`U-2`, `U-7`), surfaced `D-040` progress bands with paired encouragement and motivation quotes (`D-216` Option A), added 350ms rapid-tap idempotency protection, and wired the clickable `Collected moments` (`G2`) card to `CollectedMomentsDetailSheet` (`G3`).
3. **`Habits` Directory, `CreateEditHabitScreen`, `RecordSessionSheet`, `HabitDetailScreen` & `LivingGroveScreen`:**
   - Implemented `HabitsScreen` (`All`, `Active`, `Paused`, `Completed`, `Archived` filters, search, contiguous `0..N-1` `sort_order` reordering via `reorderHabitSync`), full-screen `CreateEditHabitScreen` (`D-216` mode gating, `1..7 days/week` check-off targets, `D-194` `Apply today` vs `Apply tomorrow` prompt, `U-12` weekly freeze notice), `RecordSessionSheet` (`D-188` date bounds, `+1/+5/+10` and `0` chips, per-entry notes, session edit, and single-entry 8s guarded Undo via `deleteHabitEntryWithUndoSync`), `HabitDetailScreen` (6 sections including `Take a rest day`, Grove trees `>= 7` vs short runs `1–6`, and confirmed permanent deletion with 8s Undo), and `LivingGroveScreen` (Canvas botanical trees, reduced-motion support, and one-shot dormant transition acknowledgment in `app_settings`).
4. **Executed Build & Test Verification Evidence (`2026-11-03T18:00:00Z` – `2026-11-03T18:00:48Z`, Stage B Self-Check):**
   - `compile_applet` (`:app:assembleDebug`): **PASSED**.
   - `gradle :app:testDebugUnitTest --rerun-tasks`: **PASSED 81/81** executed JUnit test methods across all 5 test classes (`Phase1ImplementationTest`: **29/29** including merged PR #5 remediation tests `t1_43` and `t1_44` [XML `2026-11-03T18:00:45.843Z`, `2.379s`]; `Phase2ImplementationTest`: **22/22** covering `T2-01`..`T2-51` [XML `2026-11-03T18:00:28.583Z`, `15.454s`]; `DayseedAcceptanceTest`: **12/12** [XML `2026-11-03T18:00:44.064Z`, `1.653s`]; `DayseedSettingsValidationTest`: **10/10** [XML `2026-11-03T18:00:45.725Z`, `0.017s`]; `DayseedVisualResolutionTest`: **8/8** [XML `2026-11-03T18:00:45.745Z`, `0.044s`]), **0 failed, 0 errors, 0 skipped** (`BUILD SUCCESSFUL in 47s`).
   - Appended Phase 2 human/device validation cases (`P2-H01` through `P2-H17`, all marked `NOT RUN`) to `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`. Stage C independent technical validation and Stage D final owner acceptance remain **NOT OPEN**.
