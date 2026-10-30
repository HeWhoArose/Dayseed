# DAYSEED — CORRECTED MASTER V1 IMPLEMENTATION PLAN
## Reconciled Implementation Specification for Google AI Studio

**Repository:** `HeWhoArose/Dayseed`  
**Branch:** `main`  
**Product:** Dayseed initial v1  
**Technology direction:** Native Android, Kotlin, Jetpack Compose, SQLiteOpenHelper (`D-009`)  
**Decision baseline:** Owner decisions and reconciled documentation through `D-208`  
**Purpose:** Complete implementation of the approved Dayseed initial v1

---

# 1. MISSION AND AUTHORITY

The purpose of this plan is to turn the approved Dayseed product specifications into a complete, functional Android application. A screen is not a completed feature merely because it renders; every feature must be connected to its real data, application logic, persistence, navigation, and interactions.

## 1.1 Source-of-Truth Hierarchy

| Subject | Authority |
|---|---|
| Owner decisions | `docs/09-DECISION-LOG.md` |
| Current decision interpretation | `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` |
| Product requirements | `docs/01-PRODUCT-SPECIFICATION.md` |
| Technical architecture | `docs/03-TECHNICAL-ARCHITECTURE.md` |
| Privacy and data rules | `docs/04-DATA-AND-PRIVACY.md` |
| Testing and quality | `docs/07-TESTING-AND-QUALITY.md` |
| Comeback behaviour | `docs/24-COMEBACK-EXPERIENCE.md` |
| Screen and data contracts | `docs/26-SCREEN-AND-DATA-CONTRACTS.md` |
| Requirement traceability | `docs/28-REQUIREMENT-TRACEABILITY.md` |
| Schema and migration refinements | `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md` |
| Visual design | `DESIGN.md` |
| Agent operating rules | `AGENTS.md` |
| Current project status | `README.md` and `docs/32-CONTINUATION-HANDOVER.md` |

### Decision Status Rules
- **Approved:** Implement as specified.
- **Delegated default:** Use the documented default and record its rationale.
- **Proposed:** Reconcile with the authoritative documents before implementing as a permanent contract; do not misrepresent as owner-approved.
- **Open:** Do not silently decide a material issue.
- **Deferred:** Keep outside initial v1 unless the owner changes the decision.
- **Rejected or superseded:** Do not reintroduce.
- **Unverified:** Do not claim implemented or tested without executed evidence.

---

# 2. PROJECT DISCOVERY AND PREFLIGHT INVENTORY

Inspection of the current workspace (`/settings.gradle.kts`, `/build.gradle.kts`, `/gradle/libs.versions.toml`, `/app/build.gradle.kts`, `/app/src/main/AndroidManifest.xml`, and `/app/src/main/java/com/example/dayseed/`) confirms:

- **Toolchain & Build Configuration:** Native Android Gradle project (`namespace = "com.example.dayseed"`, `applicationId = "com.aistudio.dayseed.wkqmfl"`, `minSdk = 26`, `targetSdk = 36`, `compileSdk = 36`, AGP `9.1.1`, Kotlin `2.2.10`, Java 21). Unit/Robolectric test dependencies (`junit`, `robolectric`, `androidx.test:core`) are not yet declared in `app/build.gradle.kts`.
- **Existing Files:** `MainActivity.kt`, `DayseedViewModel.kt`, `data/DayseedDatabaseHelper.kt`, `data/DayseedRepository.kt`, `data/Models.kt`, `ui/components/{ComebackCard,GrowthMark,HabitCard,ReflectionCard}.kt`, `ui/screens/{TodayScreen,HistoryScreen,InsightsScreen,SettingsScreen,AddEditHabitDialog,JournalEditorDialog}.kt`, `ui/theme/{Color,Theme,Type}.kt`.

## 2.1 Existing Implementation Classification

| Requirement / Subsystem | Current Classification | Preflight Evidence & Required Action |
|---|---|---|
| Native Android / Compose shell & 4-tab navigation (`N1`) | **Partially implemented** | `MainActivity.kt` provides 4 tabs (`Today`, `History`, `Insights`, `Settings`), but screens are bound to legacy v1 models. |
| 12-table SQLite schema (`docs/03`, `docs/30`) | **Implemented but contradicts an approved decision** | `DayseedDatabaseHelper.kt` is version 1 with 4 legacy tables (`habits`, `daily_records` with `UNIQUE(habit_id, date)`, `journal_entries`, `app_preferences`), seeds sample data inside `onCreate`, and has an empty `onUpgrade`. Must be upgraded to the canonical 12-table v2 schema. |
| Fail-closed v1-to-v2 migration (`D-197`) | **Missing** | `onUpgrade` is a no-op placeholder. Must implement transactional validation and all-or-nothing migration. |
| Deterministic clock abstraction (`D-187`) | **Missing** | Current code calls `LocalDate.now()` and `System.currentTimeMillis()` directly. Must introduce `DayseedClock`. |
| Platform backup exclusion (`D-186`) | **Implemented but contradicts an approved decision** | `AndroidManifest.xml` has `android:allowBackup="true"` and no data-extraction rules. Must configure `allowBackup="false"` and explicit exclusion rules. |
| Effective-dated `habit_versions` & `D-194` Apply today/tomorrow | **Missing** | `Habit` model is flat and rewrites history on edit. Must implement `habit_versions` and `D-194` rules. |
| Tracking modes (`QUANTITY`, `DURATION`, `COMPLETION_ONLY`, `EFFORT_AWARE`) & `D-040` bands | **Implemented but contradicts an approved decision** | `Models.kt` only has `EFFORT_LEVELS` and `COMPLETION_ONLY`, plus prohibited `DailyOutcome.SKIPPED` (`D-193`). Must implement all 4 modes, multi-entry `habit_entries`, `ProgressBandClassifier`, and remove `SKIPPED`. |
| Quick Complete, Record detail sheet, notes (`D-192`), & 8s Undo (`D-032`) | **Scaffold only** | `HabitCard.kt` only logs single-row effort outcomes; no multi-session sheet, no `habit_date_notes`, and no 8s guarded Undo. |
| Growth Mark `G2` (“Collected moments”) & `G3` calendar/month detail (`D-199`, `D-201`) | **Implemented but contradicts an approved decision** | `GrowthMark.kt` shows completion rate % instead of current-month duration total from duration-tracked habits and lacks `G3` drill-down. |
| Habit History calendar, week-start, & derived missed state (`D-188`, `D-193`) | **Partially implemented** | `HistoryScreen.kt` renders a basic calendar but uses `SKIPPED` / manual states and lacks week-start configuration and effective-dated evaluation. |
| Micro-journal, moods, `journal_drafts`, retrospective prompt (`D-042`), & `A2` archive | **Partially implemented** | `JournalEditorDialog.kt` saves basic text but lacks optional title, primary/secondary moods, `linked_habit_id`, `journal_drafts` with `intended_journal_entry_id`, retrospective prompt, and `A2` search/date-jump archive. |
| 14-day & 30-day Comeback flows (`D-047`–`D-067`, `D-206`) | **Implemented but contradicts an approved decision** | `ComebackCard.kt` uses 3-day/90-day exploratory copy and lacks 30-day zero-active-habit tracking, episode suppression, adaptive resume, and `COMEBACK_30D` draft persistence. |
| Insights & Mood Insights (`AC-009`) | **Partially implemented** | `InsightsScreen.kt` shows basic counts but lacks `This Week` / `This Month` primary mood distribution, `Compare with previous period`, and tap-a-mood entry filtering. |
| Reminders, delivery ledger, learning, & `notification_cancel_outbox` (`R1`) | **Missing** | Not yet implemented. |
| Readable export, Portable Backup/Restore, `restore_conflicts` (`D-202`), & Delete flows (`D-203`) | **Scaffold only** | `SettingsScreen.kt` has basic JSON export/delete all without readable export formats, portable backup verification, safety backup, Replace/Merge, `restore_conflicts` UI, or single-habit delete with outbox cleanup. |
| Soft Botanical & Time-of-Day theme (`D-204`, `D-207`) | **Partially implemented** | `Theme.kt` has basic light/dark colors; lacks user-selectable fixed vs time-aware theme mode and contrast verification tests. |

---

# 3. CANONICAL 12-TABLE DATABASE CONTRACT

`DayseedDatabaseHelper` (version 2) will enforce `PRAGMA foreign_keys = ON` on every connection and implement the exact canonical 12-table contract:

## 3.1 `habits`
- **Purpose:** Stable habit identity.
- **Canonical fields:**
  - `habit_id TEXT PRIMARY KEY` (UUID)
  - `created_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`
- **Invariants:** No `archived_at_epoch_ms`, `paused_at_epoch_ms`, `completed_at_epoch_ms`, or duplicate lifecycle status column. Current lifecycle state is derived from the latest effective `habit_versions.lifecycle_state`.

## 3.2 `habit_versions`
- **Purpose:** Effective-dated snapshots of habit configuration.
- **Canonical fields:**
  - `version_id TEXT PRIMARY KEY` (UUID)
  - `habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE`
  - `effective_from_local_date TEXT NOT NULL` (`YYYY-MM-DD`)
  - `name TEXT NOT NULL CHECK(length(trim(name)) > 0)`
  - `description TEXT`
  - `tracking_mode TEXT NOT NULL CHECK(tracking_mode IN ('QUANTITY','DURATION','COMPLETION_ONLY','EFFORT_AWARE'))`
  - `unit_code TEXT`
  - `target_period TEXT NOT NULL CHECK(target_period IN ('DAY','WEEK'))`
  - `target_value INTEGER CHECK(target_value IS NULL OR target_value > 0)`
  - `schedule_kind TEXT NOT NULL CHECK(schedule_kind IN ('DAILY','SELECTED_WEEKDAYS','FLEXIBLE_WEEKLY_TARGET'))`
  - `scheduled_weekdays_mask INTEGER`
  - `preferred_weekdays_mask INTEGER`
  - `lifecycle_state TEXT NOT NULL CHECK(lifecycle_state IN ('ACTIVE','PAUSED','COMPLETED','ARCHIVED'))`
  - `reminder_mode TEXT NOT NULL DEFAULT 'NONE' CHECK(reminder_mode IN ('NONE','SYSTEM_MANAGED','MANUAL'))`
  - `reminder_time_local TEXT` (`HH:MM`)
  - `created_at_utc_ms INTEGER NOT NULL`
- **Constraints (`D-191`, `D-194`, `D-195`):**
  - `UNIQUE(habit_id, effective_from_local_date)`
  - No `effective_to_local_date` column; effective configuration for date `D` is resolved by selecting the row for `habit_id` with the maximum `effective_from_local_date <= D`.
  - `DAILY` requires `target_period = 'DAY'`, `scheduled_weekdays_mask IS NULL`, `preferred_weekdays_mask IS NULL`.
  - `SELECTED_WEEKDAYS` requires `target_period = 'DAY'`, `scheduled_weekdays_mask BETWEEN 1 AND 127`, `preferred_weekdays_mask IS NULL`.
  - `FLEXIBLE_WEEKLY_TARGET` requires `target_period = 'WEEK'`, `scheduled_weekdays_mask IS NULL`, and `preferred_weekdays_mask IS NULL OR (preferred_weekdays_mask BETWEEN 1 AND 127)`.
  - Weekday bits (`D-195`): Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64.

## 3.3 `habit_entries`
- **Purpose:** Individual quantity/duration activity entries.
- **Canonical fields:**
  - `entry_id TEXT PRIMARY KEY` (UUID)
  - `habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE`
  - `local_date TEXT NOT NULL` (`YYYY-MM-DD`)
  - `value_units INTEGER NOT NULL CHECK(value_units >= 0)`
  - `entry_note TEXT`
  - `created_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`
  - `idempotency_key TEXT`
- **Invariants:** Multiple entries per `(habit_id, local_date)` are valid (no unique constraint on `habit_id, local_date`). Explicit `0` is a valid saved record distinct from no row. Partial unique index on `idempotency_key WHERE idempotency_key IS NOT NULL`. No redundant `canonical_amount` column; compatible-unit normalization (`D-201`) is performed in the domain/aggregation layer using the effective `habit_versions.unit_code`.

## 3.4 `habit_day_outcomes`
- **Purpose:** Non-numeric habit outcomes (`COMPLETION_ONLY` and `EFFORT_AWARE`).
- **Canonical fields:**
  - `outcome_id TEXT PRIMARY KEY` (UUID)
  - `habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE`
  - `local_date TEXT NOT NULL` (`YYYY-MM-DD`)
  - `outcome_kind TEXT NOT NULL CHECK(outcome_kind IN ('COMPLETION_ONLY','EFFORT_AWARE'))`
  - `effort_level TEXT CHECK((outcome_kind = 'COMPLETION_ONLY' AND effort_level IS NULL) OR (outcome_kind = 'EFFORT_AWARE' AND effort_level IN ('TINY','USUAL','EXTRA')))`
  - `idempotency_key TEXT`
  - `created_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`
  - `UNIQUE(habit_id, local_date)`
- **Invariants:** Partial unique index on `idempotency_key WHERE idempotency_key IS NOT NULL`. Cross-table triggers enforce that a non-null `idempotency_key` cannot exist in both `habit_entries` and `habit_day_outcomes`, and prevent numeric entries and day outcomes from coexisting for the same `(habit_id, local_date)`. No `SKIPPED` or `MISSED` outcome.

## 3.5 `habit_date_notes`
- **Purpose:** Optional date-level context note for a habit (`D-192`).
- **Canonical fields:**
  - `note_id TEXT PRIMARY KEY` (UUID)
  - `habit_id TEXT NOT NULL REFERENCES habits(habit_id) ON DELETE CASCADE`
  - `local_date TEXT NOT NULL` (`YYYY-MM-DD`)
  - `body TEXT NOT NULL CHECK(length(trim(body)) > 0)`
  - `created_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`
  - `UNIQUE(habit_id, local_date)`

## 3.6 `journal_entries`
- **Purpose:** Saved independent journal reflections.
- **Canonical fields:**
  - `journal_entry_id TEXT PRIMARY KEY` (UUID)
  - `local_date TEXT NOT NULL` (`YYYY-MM-DD`)
  - `body TEXT NOT NULL CHECK(length(trim(body)) > 0)`
  - `title TEXT`
  - `primary_mood_code TEXT`
  - `secondary_mood_code TEXT`
  - `prompt_id TEXT`
  - `linked_habit_id TEXT REFERENCES habits(habit_id) ON DELETE SET NULL`
  - `created_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`

## 3.7 `journal_drafts`
- **Purpose:** Unsaved journal and comeback reflections.
- **Canonical fields:**
  - `draft_id TEXT PRIMARY KEY` (UUID)
  - `local_date TEXT` (`YYYY-MM-DD`, optional)
  - `title TEXT`
  - `body TEXT NOT NULL`
  - `primary_mood_code TEXT`
  - `secondary_mood_code TEXT`
  - `prompt_id TEXT`
  - `linked_habit_id TEXT REFERENCES habits(habit_id) ON DELETE SET NULL`
  - `intended_journal_entry_id TEXT` (nullable until a save is first prepared; partial unique index `WHERE intended_journal_entry_id IS NOT NULL`)
  - `source_context TEXT` (e.g., `COMEBACK_30D`, `JOURNAL_EDITOR`, `RETROSPECTIVE`)
  - `created_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`
- **Save preparation & atomic promotion:** When a save is first prepared, assign a stable UUID to `intended_journal_entry_id` and persist it on the draft; reuse that ID on every retry; insert into `journal_entries` with `journal_entry_id = intended_journal_entry_id` and delete the draft in one transaction.

## 3.8 `app_settings`
- **Purpose:** Typed, versioned local preferences.
- **Canonical fields:**
  - `setting_key TEXT PRIMARY KEY`
  - `value_type TEXT NOT NULL` (`STRING`, `INT`, `LONG`, `BOOLEAN`, `JSON`)
  - `value_text TEXT NOT NULL`
  - `schema_version INTEGER NOT NULL DEFAULT 1`
  - `updated_at_utc_ms INTEGER NOT NULL`

## 3.9 `reminder_deliveries`
- **Purpose:** Durable reminder-delivery deduplication and suppression ledger.
- **Canonical fields:**
  - `delivery_key TEXT PRIMARY KEY` (unique logical occurrence key)
  - `reminder_type TEXT NOT NULL CHECK(reminder_type IN ('HABIT_DAILY','HABIT_EVENING','WEEKLY_REFLECTION','INACTIVITY_2D'))`
  - `habit_id TEXT REFERENCES habits(habit_id) ON DELETE CASCADE`
  - `target_period_key TEXT NOT NULL` (intended local date `YYYY-MM-DD` or week key)
  - `delivery_status TEXT NOT NULL CHECK(delivery_status IN ('SCHEDULED','DELIVERED','SUPPRESSED','DISMISSED','CANCELLED'))`
  - `scheduled_at_utc_ms INTEGER NOT NULL`
  - `attempted_at_utc_ms INTEGER`
  - `delivered_at_utc_ms INTEGER`
  - `platform_notification_tag TEXT`
  - `platform_notification_id INTEGER`
  - `suppression_reason TEXT`

## 3.10 `restore_sessions`
- **Purpose:** Durable Replace/Merge recovery state.
- **Canonical fields:**
  - `restore_session_id TEXT PRIMARY KEY` (UUID)
  - `restore_mode TEXT NOT NULL CHECK(restore_mode IN ('REPLACE','MERGE'))`
  - `format_version INTEGER NOT NULL`
  - `source_schema_version INTEGER NOT NULL`
  - `session_state TEXT NOT NULL CHECK(session_state IN ('IN_PROGRESS','RECOVERY_REQUIRED','COMPLETED','COMPLETED_WITH_CONFLICTS','FAILED_SAFE'))`
  - `started_at_utc_ms INTEGER NOT NULL`
  - `updated_at_utc_ms INTEGER NOT NULL`
  - `manifest_json TEXT NOT NULL`
  - `safety_backup_ref TEXT NOT NULL`
  - `safety_backup_verified INTEGER NOT NULL CHECK(safety_backup_verified IN (0, 1))`

## 3.11 `restore_conflicts`
- **Purpose:** Durable unresolved restore conflicts (`D-202`).
- **Canonical fields:**
  - `conflict_id TEXT PRIMARY KEY` (UUID)
  - `restore_session_id TEXT NOT NULL REFERENCES restore_sessions(restore_session_id) ON DELETE CASCADE`
  - `record_type TEXT NOT NULL` (`HABIT`, `HABIT_VERSION`, `HABIT_ENTRY`, `HABIT_DAY_OUTCOME`, `HABIT_DATE_NOTE`, `JOURNAL_ENTRY`, `JOURNAL_DRAFT`, `APP_SETTING`, `ACTIVITY_IDEMPOTENCY_KEY`)
  - `record_key TEXT NOT NULL`
  - `current_payload_json TEXT NOT NULL`
  - `backup_payload_json TEXT NOT NULL`
  - `created_at_utc_ms INTEGER NOT NULL`
  - `UNIQUE(restore_session_id, record_type, record_key)`
- **Invariants:** Existence of the row means the conflict is unresolved (no redundant status column and no schema `allowed_resolutions_mask`; allowed resolutions are determined by record-type domain rules).

## 3.12 `notification_cancel_outbox`
- **Purpose:** Temporary retryable work for cancelling Android OS notifications after habit deletion or opt-out.
- **Canonical fields:**
  - `cancel_id TEXT PRIMARY KEY` (UUID)
  - `platform_notification_tag TEXT NOT NULL`
  - `platform_notification_id INTEGER NOT NULL`
  - `created_at_utc_ms INTEGER NOT NULL`
  - `last_attempt_at_utc_ms INTEGER`
  - `attempt_count INTEGER NOT NULL DEFAULT 0`
  - `status TEXT NOT NULL CHECK(status IN ('PENDING','RETRYING','FAILED'))`
  - `error_category TEXT`
  - `UNIQUE(platform_notification_tag, platform_notification_id)`
- **Invariants:** Never stores habit names, journal text, or stable habit IDs; excluded from portable backups; removed only after confirmed cancellation.

---

# 4. SEQUENCED IMPLEMENTATION PHASES & VERIFICATION

## Phase 1 — Native Foundation, 12-Table Database & Fail-Closed Migration (`D-186`, `D-187`, `D-189`, `D-190`, `D-197`)
- Configure unit/Robolectric test dependencies in `gradle/libs.versions.toml` and `app/build.gradle.kts`.
- Configure `D-186` platform-backup exclusion (`android:allowBackup="false"`, `data_extraction_rules.xml`, `backup_rules.xml`).
- Implement `DayseedClock` (`SystemDayseedClock` + deterministic `TestDayseedClock`).
- Implement `DayseedDatabaseHelper` v2 with the canonical 12-table schema, foreign-key enforcement, partial unique indexes, cross-table SQLite triggers, and fail-closed v1-to-v2 migration (`D-197`) with no production seed data in `onCreate`.
- **Verification:** Execute `Phase1SchemaAndMigrationTest` via `gradle :app:testDebugUnitTest` covering `AC-001`, `AC-015`, and `AC-017`–`AC-021`.

## Phase 2 — Habit Management, Effective-Dated Versions, Measured Progress Bands & 8s Undo (`D-005`, `D-031`, `D-032`, `D-040`, `D-191`–`D-195`)
- Implement `ScheduleEvaluator` (`DAILY`, `SELECTED_WEEKDAYS` Monday=1..Sunday=64 bitmask, `FLEXIBLE_WEEKLY_TARGET`), `ProgressBandClassifier` (`Tiny` through `Huge` using exact integer ratio arithmetic before rounding, skipping unreachable bands on tiny targets), `D-194` `Apply today` / `Apply tomorrow` version management, multi-entry `habit_entries` (including explicit `0`), `habit_day_outcomes` (`COMPLETION_ONLY` and `EFFORT_AWARE`), `habit_date_notes`, and the 8-second guarded Undo coordinator.
- **Verification:** Execute `Phase2HabitTrackingAndUndoTest` covering `AC-002`–`AC-004` and `AC-023`.

## Phase 3 — W1 Today, Growth Mark (`G2`/`G3`) & Habit History (`D-188`, `D-193`, `D-199`, `D-201`)
- Implement `TodayScreen` (`W1`), `GrowthMarkAggregator` (`G2` current-month duration total from duration-tracked habits normalized in the domain layer using `min`, `hr`, `24h day`, `7d week`, plus `G3` calendar/month detail and session drill-down/correction), and `HistoryScreen` (respecting saved week-start, derived missed state after a scheduled day passes without qualifying activity, neutral rest/paused states, and backdated record logging/correction).
- **Verification:** Execute `Phase3GrowthMarkAndHistoryTest` covering `AC-005`–`AC-007` and `AC-025`.

## Phase 4 — Journal, Drafts, Moods, Retrospective Prompt & `A2` Archive (`D-042`, `D-071`–`D-090`)
- Implement standalone and habit-linked journal entries, optional title, non-empty body check, 10 starter moods + `More` expanded emotional families (primary + optional secondary mood), deterministic offline daily prompts, `journal_drafts` with save-prepared `intended_journal_entry_id`, `D-042` retrospective prompt, and `A2` hybrid journal archive (chronological timeline + compact calendar date-jump + local title/body search + mood filters).
- **Verification:** Execute `Phase4JournalAndArchiveTest` covering `AC-008` and `AC-024`.

## Phase 5 — 14-Day & 30-Day Comeback Experience & Insights (`D-047`–`D-067`, `D-206`, `AC-009`, `AC-011`)
- Implement the 14-day and 30-day comeback experiences with exact `D-206` copy, 30-day precedence, once-per-episode suppression, adaptive habit resume/restart flow, and persistent `COMEBACK_30D` draft lifecycle.
- Implement `InsightsScreen` with habit completion summaries, progress-band breakdown, weekly vs daily target summaries, and **Mood Insights** (`This Week` and `This Month` primary mood distributions, opt-in `Compare with previous period`, and tap-a-mood entry filter).
- **Verification:** Execute `Phase5ComebackAndInsightsTest` covering `AC-009` (`J1`–`J11` fixture with Monday and Sunday week-starts) and `AC-011`.

## Phase 6 — Reminders, Readable Export, Portable Backup/Restore, Conflicts & Deletion (`D-041`–`D-046`, `D-091`–`D-145`, `D-202`, `D-203`)
- Implement `ReminderCoordinator` (consent, per-habit manual/system-managed 14d/28d timing, weekly reflection, quiet hours, 3/day cap, `reminder_deliveries` ledger, and `notification_cancel_outbox` retry).
- Implement readable journal export (`Markdown`, `CSV`, `JSON` of saved journal entries only).
- Implement versioned portable backup/restore (`.dayseed.json`, documented candidate password-encryption envelope with double-entry confirmation and safe retry, pre-restore verified safety backup, `REPLACE` and `MERGE` with durable `restore_sessions` and `restore_conflicts` resolution UI in Settings → Backup & Restore).
- Implement the 3 distinct deletion flows (`D-203`: single journal entry, single habit permanent delete with `notification_cancel_outbox` and preserved independent journal entries, and delete-all with full disclosure).
- **Verification:** Execute `Phase6RemindersBackupAndPrivacyTest` covering `AC-012`–`AC-014`, `AC-022`, and `AC-026`.

## Phase 7 — Settings, Themes (`D-204`, `D-207`), Accessibility, Traceability & Handover Report
- Complete `SettingsScreen`, semantic theme tokens (`DESIGN.md` candidate tokens + `D-204` fixed `System`/`Light`/`Dark` and automatic time-aware mode using documented candidate daypart boundaries), contrast verification tests, adaptive launcher icon, TalkBack/touch-target/reduced-motion support, `docs/12-PROGRESS-AND-HANDOVER.md` & `docs/28-REQUIREMENT-TRACEABILITY.md` updates, and the complete Section 12 Implementation Handover Report.
