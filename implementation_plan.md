# Dayseed — Stage B Initial v1 Implementation, Stage C Independent Validation & Stage D Release Readiness Plan

## 1. Overview & Approved Product Scope

**Dayseed** is an offline-first, account-free Android habit tracker and micro-journal built with **Kotlin**, **Jetpack Compose**, and **`SQLiteOpenHelper`** (`D-009`). Its guiding promise is:

> **“Your progress is more than your streak.”**

This plan translates the reconciled owner decisions (`D-001` through `D-209`) into a sequenced engineering plan: **Stage A planning (complete)** → **Stage B full initial-v1 implementation in Google AI Studio** → **Stage C independent technical validation after implementation** → **Stage D final owner acceptance and release readiness**. This is a plan, not authorisation to begin Stage B.

- **Offline-first & account-free (`D-008`, `O1`, `P1`):** Zero cloud accounts, zero third-party analytics/telemetry, and no advertising SDKs in v1 (`D-200`).
- **Platform backup exclusion (`D-186`):** `android:allowBackup="false"` and explicit Android 12+ data-extraction rules (`data_extraction_rules.xml` / `backup_rules.xml`) so private habits, journal entries, and drafts are excluded from OS Auto Backup/device transfer while remaining portable via Dayseed's user-controlled backup.
- **12-table proposed relational SQLite schema (`D-189`, `D-190`, `D-197`, `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`):** The schema remains a proposed implementation baseline, not a canonical/finally approved schema. The plan is to upgrade `DayseedDatabaseHelper` (version 2) with transactional fail-closed migration from legacy v1 tables, stable UUID identity, cross-table activity idempotency guards, and foreign-key enforcement only after the proposal is reconciled and Stage B is separately authorised.
- **Soft Botanical & Time-of-Day Theme System (`D-204`, `D-205`, `D-207`):** Synthesis D is the selected broad visual direction. Exact palette values, typography, production tokens, daypart boundaries, and contrast pairings remain unapproved. Support fixed theme selection (`System`, `Light`, `Dark`) and an automatic four-daypart mode, but keep candidate tokens and local-time boundaries in one replaceable configuration source; do not scatter literals through UI code or present candidate contrast pairings as verified.
- **Portable Backup with Optional AES-GCM Password Encryption & Delegated Prompts (`D-042`, `D-094`–`D-106`, `D-202`):** Versioned `.dayseed.json` portable backup with optional password-based AES-256-GCM encryption (PBKDF2-HMAC-SHA256 key derivation), verified pre-restore safety backup, Replace/Merge flows, durable `restore_conflicts` review UI, retrospective journal prompt (`D-042`), and in-app partial-progress tips.

---

## 2. Visual Direction, Provisional Soft Botanical Palette & Configurable Time-Aware Themes

### 2.1 Synthesis D Visual Direction (`D-205`) & Navigation (`N1`, `W1`, `A2`)
- **Primary Navigation (`N1`):** 4 top-level destinations in a Material 3 `NavigationBar` (compact) / `NavigationRail` (expanded):
  1. **Today (`W1` Action-First):** Date context header, **G2 “Collected moments”** compact current-month duration card (tappable to open **G3** Calendar/Month detail), supportive comeback or retrospective banners when eligible, habit cards with one-tap **Complete** and detailed **Record** actions, and quick **Journal** entry card.
  2. **History (`H4` + `A2` Hybrid Journal Archive):** Segmented switcher between **Habit History Calendar** (respecting saved week-start preference, showing completed, partial, above-goal, neutral rest/paused, and derived past missed days with backdated logging/correction) and **Journal Archive (`A2`)** (chronological entry timeline + compact calendar date-jump + local title/body search + mood filter).
  3. **Insights (`I1` + Mood Insights):** Factual habit completion summaries, progress-band breakdown, weekly target vs daily goal distinction, and **Mood Insights** (`This Week` / `This Month` primary mood distribution, optional `Compare with previous period` toggle, and tap-a-mood filtering).
  4. **Settings (`P1`, `R1`):** Theme mode selector (`System`, `Light`, `Dark`, `Time of Day`), Week Start day (`Monday`, `Sunday`, `Saturday`), Reminders & Quiet Hours (`21:00–08:00` default, 3/day cap), Readable Journal Export (`Markdown`, `CSV`, `JSON`), Portable Backup & Restore (with unresolved conflict badge & resolution screen), and Privacy & Delete controls (single habit, single journal entry, delete-all with `D-203` disclosure).

### 2.2 Soft Botanical Semantic Tokens (`D-204`, `D-207`, `DESIGN.md`)
All UI components consume semantic tokens via `DayseedTheme` (never hardcoded hex literals). **The values and daypart ranges below are exploratory examples only, not owner-approved production tokens or clock boundaries.** Keep the palette, semantic mappings, and daypart start/end times in one central configuration/resource layer so they can be changed without editing individual components. Do not mark them accessible or final until contrast, light/dark, large-text, TalkBack, reduced-motion, and owner review are complete:

| Semantic Role | Light / Day (`09:00–16:59`) | Morning (`05:00–08:59`) | Evening (`17:00–20:59`) | Dark / Night (`21:00–04:59`) |
|---|---|---|---|---|
| `canvas` | `#F7F5EE` | `#F9F6EC` | `#F2ECE1` | `#121A15` |
| `surface` | `#FFFFFF` | `#FFFEFA` | `#FAF6EE` | `#1D2820` |
| `textPrimary` | `#24352B` | `#23342A` | `#26342B` | `#F3F2E8` |
| `textSecondary` | `#526056` | `#546257` | `#566158` | `#C1CBC1` |
| `brand` | `#286143` | `#2D6A4A` | `#265B3F` | `#9CCB9C` |
| `onBrand` | `#FFFFFF` | `#FFFFFF` | `#FFFFFF` | `#17251C` |
| `growthSubtle` | `#E4EBDD` | `#E7EFE0` | `#DFE7D8` | `#293A2D` |
| `accentWarm` | `#B84A3A` | `#B8523E` | `#AD4738` | `#F0A092` |
| `border` | `#D8DED5` | `#DCE1D8` | `#D1D8CE` | `#3C4A3E` |

- **Status Encoding Rule:** Every habit/day state pairs color with an explicit **icon + text label** (`Goal met`, `Huge`, `Extra`, `Nearly there`, `Halfway`, `Small`, `Tiny`, `Recorded 0`, `Rest day`, `Paused`, `Missed (unrecorded)`).

---

## 3. Architecture & 12-Table Proposed SQLite Schema (`docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`)

**Schema status:** proposed implementation baseline only. It is not canonical/finally approved for implementation. Reconcile it against `docs/03-TECHNICAL-ARCHITECTURE.md` and `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`; any discrepancy blocks schema work until resolved. No schema change or migration is authorised by this plan.

```
+-----------------------------------------------------------------------+
|                       UI Layer (Jetpack Compose)                      |
|  TodayScreen | G3GrowthSheet | RecordDetailSheet | HistoryScreen      |
|  JournalArchive | InsightsScreen | BackupRestoreScreen | ComebackFlow |
+-----------------------------------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                  State & Coordinator Layer (ViewModel)                |
|  DayseedViewModel (StateFlow<DayseedUiState>, 8s Undo Coordinator,    |
|  Comeback & Retrospective Eligibility, Time-of-Day Theme Observer)    |
+-----------------------------------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|                   Domain & Repository Layer (Kotlin)                  |
|  DayseedRepository | ProgressBandClassifier | ScheduleEvaluator       |
|  GrowthMarkAggregator | BackupCryptoEngine | ReminderCoordinator      |
|  DayseedClock (SystemClock vs Deterministic TestClock)                |
+-----------------------------------------------------------------------+
                                    |
                                    v
+-----------------------------------------------------------------------+
|               Persistence Layer (SQLiteOpenHelper v2)                 |
|  12 Canonical Tables + Foreign Keys + Cross-Table Triggers + Outbox   |
+-----------------------------------------------------------------------+
```

### 3.1 The 12 Reconciled Tables (`DayseedDatabaseHelper` v2)
1. **`habits`**: `habit_id` (UUID PK), `created_at_utc_ms`, `updated_at_utc_ms`. (No `archived_at_epoch_ms`, `paused_at_epoch_ms`, `completed_at_epoch_ms`, or duplicate lifecycle status column; current lifecycle state is derived from the latest effective `habit_versions.lifecycle_state`.)
2. **`habit_versions`**: `version_id` (UUID PK), `habit_id` (FK `REFERENCES habits(habit_id) ON DELETE CASCADE`), `effective_from_local_date` (`YYYY-MM-DD`, `UNIQUE(habit_id, effective_from_local_date)`), `name`, nullable `description`, `tracking_mode` (`QUANTITY`, `DURATION`, `COMPLETION_ONLY`, `EFFORT_AWARE`), nullable `unit_code`, `target_period` (`DAY`, `WEEK`), `target_value` (positive integer for measured modes and non-numeric `FLEXIBLE_WEEKLY_TARGET` weekly goals; `NULL` for non-numeric `DAY` goals), `schedule_kind` (`DAILY`, `SELECTED_WEEKDAYS`, `FLEXIBLE_WEEKLY_TARGET`), nullable `scheduled_weekdays_mask` (`1..127` for `SELECTED_WEEKDAYS`: Mon=1, Tue=2, Wed=4, Thu=8, Fri=16, Sat=32, Sun=64), nullable `preferred_weekdays_mask` (`1..127` optional for `FLEXIBLE_WEEKLY_TARGET`), `lifecycle_state` (`ACTIVE`, `PAUSED`, `COMPLETED`, `ARCHIVED`), `reminder_mode` (`NONE`, `SYSTEM_MANAGED`, `MANUAL`), nullable `reminder_time_local` (`HH:MM`, default `09:00` when enabled), `created_at_utc_ms`. (No `effective_to_local_date` column.)
3. **`habit_entries`**: Individual measured sessions for `QUANTITY` / `DURATION` habits (`entry_id` UUID PK, `habit_id` FK CASCADE, `local_date`, `value_units` integer `>= 0`, nullable `entry_note`, `created_at_utc_ms`, `updated_at_utc_ms`, nullable `idempotency_key` with partial unique index `WHERE idempotency_key IS NOT NULL`). Multiple entries per habit/date are valid and summed; explicit `0` is preserved and distinct from no row; no redundant `canonical_amount` column (compatible-unit normalization `D-201` is performed in the domain/aggregation layer).
4. **`habit_day_outcomes`**: Single outcome per habit/date for non-numeric modes (`outcome_id` UUID PK, `habit_id` FK CASCADE, `local_date`, `outcome_kind` (`COMPLETION_ONLY` or `EFFORT_AWARE`), nullable `effort_level` (`NULL` for `COMPLETION_ONLY`; `TINY`, `USUAL`, or `EXTRA` for `EFFORT_AWARE`), nullable `idempotency_key` with partial unique index `WHERE idempotency_key IS NOT NULL`, `created_at_utc_ms`, `updated_at_utc_ms`, `UNIQUE(habit_id, local_date)`). SQLite triggers enforce cross-table uniqueness of non-null `idempotency_key` across `habit_entries` and `habit_day_outcomes` (`D-190`) and prevent coexistence of measured entries and day outcomes for the same `(habit_id, local_date)`.
5. **`habit_date_notes`**: At most one context note per `(habit_id, local_date)` (`note_id` UUID PK, `habit_id` FK CASCADE, `local_date`, `body` non-empty, `created_at_utc_ms`, `updated_at_utc_ms`, `UNIQUE(habit_id, local_date)`).
6. **`journal_entries`**: Multiple saved entries per local date (`journal_entry_id` UUID PK, `local_date`, `body` non-empty, nullable `title`, nullable `primary_mood_code`, nullable `secondary_mood_code`, nullable `prompt_id`, nullable `linked_habit_id` FK `REFERENCES habits(habit_id) ON DELETE SET NULL`, `created_at_utc_ms`, `updated_at_utc_ms`).
7. **`journal_drafts`**: Unsaved comeback/reflection/retrospective drafts (`draft_id` UUID PK, nullable `local_date`, nullable `title`, `body`, nullable `primary_mood_code`, nullable `secondary_mood_code`, nullable `prompt_id`, nullable `linked_habit_id` FK `ON DELETE SET NULL`, nullable `intended_journal_entry_id` with partial unique index `WHERE intended_journal_entry_id IS NOT NULL`, nullable `source_context`, `created_at_utc_ms`, `updated_at_utc_ms`). `intended_journal_entry_id` is `NULL` until a save is first prepared; preparing a save assigns and persists a stable UUID, and saving inserts into `journal_entries` with `journal_entry_id = intended_journal_entry_id` and removes the draft in one atomic transaction.
8. **`app_settings`**: Typed, versioned local preferences (`setting_key` PK, `value_type`, `value_text`, `schema_version`, `updated_at_utc_ms`).
9. **`reminder_deliveries`**: Durable reminder-delivery deduplication ledger (`delivery_key` unique PK, `reminder_type` (`HABIT_DAILY`, `HABIT_EVENING`, `WEEKLY_REFLECTION`, `INACTIVITY_2D`), nullable `habit_id` FK CASCADE, `target_period_key`, `delivery_status` (`SCHEDULED`, `DELIVERED`, `SUPPRESSED`, `DISMISSED`, `CANCELLED`), `scheduled_at_utc_ms`, nullable `attempted_at_utc_ms`, nullable `delivered_at_utc_ms`, nullable `platform_notification_tag`, nullable `platform_notification_id`, nullable `suppression_reason`).
10. **`restore_sessions`**: Durable Replace/Merge recovery state (`restore_session_id` UUID PK, `restore_mode` (`REPLACE`, `MERGE`), `format_version`, `source_schema_version`, `session_state` (`IN_PROGRESS`, `RECOVERY_REQUIRED`, `COMPLETED`, `COMPLETED_WITH_CONFLICTS`, `FAILED_SAFE`), `started_at_utc_ms`, `updated_at_utc_ms`, `manifest_json`, `safety_backup_ref`, `safety_backup_verified`).
11. **`restore_conflicts`**: Durable unresolved restore conflicts (`conflict_id` UUID PK, `restore_session_id` FK CASCADE, `record_type`, `record_key`, `current_payload_json`, `backup_payload_json`, `created_at_utc_ms`, `UNIQUE(restore_session_id, record_type, record_key)`). Existence of the row indicates it is unresolved; no redundant status or `allowed_resolutions_mask` schema column.
12. **`notification_cancel_outbox`**: Temporary retryable OS notification cancellation work (`cancel_id` UUID PK, `platform_notification_tag` NOT NULL, `platform_notification_id` NOT NULL, `created_at_utc_ms`, nullable `last_attempt_at_utc_ms`, `attempt_count`, `status` (`PENDING`, `RETRYING`, `FAILED`), nullable `error_category`, `UNIQUE(platform_notification_tag, platform_notification_id)`). Never stores habit names, journal text, or stable habit IDs.


### 3.3 Required proposed SQLite CHECK constraints (corrected)

These are proposed DDL constraints for the schema above, not evidence that the current database already contains them. Preserve table/column names and enum spelling consistently when translating them into final DDL.

**A. `habit_versions.target_value` — required for measured modes and non-numeric weekly targets**

```sql
CHECK (
	(
		tracking_mode IN ('QUANTITY', 'DURATION')
		AND target_value IS NOT NULL
		AND typeof(target_value) = 'integer'
		AND target_value > 0
	)
	OR
	(
		tracking_mode IN ('COMPLETION_ONLY', 'EFFORT_AWARE')
		AND target_period = 'DAY'
		AND target_value IS NULL
	)
	OR
	(
		tracking_mode IN ('COMPLETION_ONLY', 'EFFORT_AWARE')
		AND target_period = 'WEEK'
		AND target_value IS NOT NULL
		AND typeof(target_value) = 'integer'
		AND target_value > 0
	)
)
```

This prevents a measured habit from passing the CHECK with a missing target. It also prevents a non-numeric mode from carrying a numeric target. `tracking_mode` itself must be `NOT NULL` and constrained to the four supported values. This reflects the architecture rule that a daily completion/effort habit has an implicit one-day outcome, while a flexible weekly target counts qualifying completed dates and therefore needs a positive integer count. `target_period` must be `NOT NULL`; mode/period combinations and the applicable effective configuration must also be checked by the domain/write guard.

**B. `habit_day_outcomes.outcome_kind` / `effort_level` — reject missing effort values**

```sql
CHECK (
	(
		outcome_kind = 'COMPLETION_ONLY'
		AND effort_level IS NULL
	)
	OR
	(
		outcome_kind = 'EFFORT_AWARE'
		AND effort_level IS NOT NULL
		AND effort_level IN ('TINY', 'USUAL', 'EXTRA')
	)
)
```

`outcome_kind` must also be `NOT NULL` and restricted to `COMPLETION_ONLY` / `EFFORT_AWARE`. The explicit `effort_level IS NOT NULL` is essential: SQLite accepts a CHECK expression that evaluates to NULL, so `effort_level IN (...)` by itself does not reject a missing value. The domain transaction/write guard must additionally ensure that `outcome_kind` matches the effective tracking mode for that habit/date. Mirror both checks in domain validation and migration validation.

### 3.2 Fail-Closed v1-to-v2 Migration (`D-197`)
- During `onUpgrade(db, 1, 2)`:
  - Inspect legacy v1 tables (`habits`, `habit_records`, `journal_entries`, `app_settings`).
  - Validate every row before mutation: reject any unrecognised tracking mode, non-positive target, invalid date format, or legacy `SKIPPED` outcome (which violates `D-193`). If any row fails validation, throw a `DayseedMigrationBlockedException` inside the SQLite transaction so SQLite rolls back completely, preserving the v1 database untouched.
  - Map valid v1 habits to `habits` + initial `habit_versions` (`effective_from_date` set to the earliest record date or creation date), map `habit_records` to `habit_entries` (for `QUANTITY`/`DURATION`) or `habit_day_outcomes` (for `COMPLETION_ONLY`/`EFFORT_AWARE`), and map `journal_entries` and `app_settings` losslessly.

---

## 4. Sequenced Implementation Phases

### Phase 1: Stage B implementation preflight and core persistence foundation
- **Deliverables:**
  - Create `DayseedClock` interface (`SystemDayseedClock` + `TestDayseedClock`) for deterministic local dates (`YYYY-MM-DD`), local time (`HH:MM`), time-zone IDs, and monotonic/wall-clock uncertainty detection (`D-187`).
  - Configure `AndroidManifest.xml` (`android:allowBackup="false"`, `android:dataExtractionRules="@xml/data_extraction_rules"`, `android:fullBackupContent="@xml/backup_rules"`) and create `/app/src/main/res/xml/data_extraction_rules.xml` and `/app/src/main/res/xml/backup_rules.xml` excluding `dayseed.db` and shared preferences (`D-186`).
  - Implement `DayseedDatabaseHelper` v2 with all 12 tables, foreign keys (`PRAGMA foreign_keys = ON`), partial unique indexes, cross-table idempotency triggers, and fail-closed v1-to-v2 migration (`D-197`).
- **Verification:** Run the actual Android Gradle compile/build and relevant Robolectric persistence/migration tests (`AC-001`, `AC-015`, `AC-017`–`AC-021`) only after confirming the exact project tasks and dependencies from the imported repository. `compile_applet` is an unverified legacy placeholder, not an approved command and must not be reported as executed.

### Phase 2: Habits, Effective-Dated Versions, Measured Progress Bands & 8-Second Guarded Undo
- **Deliverables:**
  - Implement `ScheduleEvaluator` (`DAILY`, `SELECTED_WEEKDAYS` 7-bit mask `D-195`, `FLEXIBLE_WEEKLY_TARGET` with optional preferred days `D-191`) and `ProgressBandClassifier` (`Tiny` >0%..<25%, `Small` 25%..<50%, `Halfway` 50%..<75%, `Nearly there` 75%..<100%, `Goal met` 100%, `Extra` >100%..150%, `Huge` >150% using exact integer ratio arithmetic before percentage rounding `D-040`, omitting unreachable bands on tiny targets such as target=1).
  - Implement `D-194` same-day configuration edit logic: first **Apply today** creates today's `habit_versions` row; subsequent edits on the same day replace today's version only if no qualifying activity exists today; once activity exists today, preserve today's version and offer **Apply tomorrow** (or **Cancel**).
  - Implement **Today (`W1`)** habit cards with quick **Complete** button and **Record Detail Sheet** (multi-session numeric/duration entries including explicit `0`, entry notes, and 1 per-habit/date context note `D-192`).
  - Implement the **8-second guarded Snackbar Undo** (`D-032`) reversing only the exact stable-ID record if unchanged, plus optional in-app partial-progress tips (`H6`, max 3 per 7-day sequence, 24h spacing, 1/day max).
- **Verification:** Run Robolectric tests for `AC-002` (exact progress-band boundary table), `AC-003` (multi-entry summation & explicit zero vs no record), `AC-004` (`D-194` Apply today/tomorrow), and `AC-023` (8-second guarded Undo).

### Phase 3: Growth Mark (`G2` / `G3`) & Habit History Calendar (`H4`)
- **Deliverables:**
  - Implement `GrowthMarkAggregator` (`D-199`, `D-201`) calculating current-month duration from saved duration-tracked habit sessions normalized to canonical minutes, formatted using readable whole units (`min`, `hr`, `24h day`, `7d week`; no month-equivalents).
  - Build the **G2 “Collected moments”** card on Today and the **G3 Calendar/Month Detail Sheet/Screen** with month/week/day inspection, previous/next month navigation, per-date session list, and inline session correction.
  - Build the **Habit History Calendar** under the `History` tab respecting the user's configured week-start (`Monday`, `Sunday`, `Saturday`), displaying derived missed status only after a scheduled day passes without qualifying activity (`D-188`, `D-193` — no Skip button or manual miss), neutral rest/paused states, and explicit backdated entry logging/editing.
- **Verification:** Run Robolectric tests for `AC-025` (Growth Mark G2/G3 aggregation and unit formatting) and `AC-005`–`AC-007` (derived missed status, backdated edits, week-start boundaries).

### Phase 4: Micro-Journal, Mood Taxonomy, Drafts & `A2` Hybrid Archive
- **Deliverables:**
  - Implement standalone and habit-linked journal entries (`ON DELETE SET NULL`), optional title, deterministic daily offline reflection prompts, and optional primary + secondary mood selection from the 10 starter moods (`Calm`, `Content`, `Joyful`, `Hopeful`, `Grateful`, `Curious`, `Tired`, `Anxious`, `Sad`, `Angry`) plus the **More** expanded mood families (`Focused`, `Energised`, `Peaceful`, `Relieved`, `Proud`, `Overwhelmed`, `Lonely`, `Frustrated`, `Uncertain`, `Numb`).
  - Implement `journal_drafts` with non-null unique `intended_journal_entry_id` so saving atomically promotes the draft to `journal_entries` with that exact ID and removes the draft (`AC-008`).
  - Implement the **Retrospective Journal Prompt (`D-042`)** on Today when yesterday (most recent completed local date) has no saved journal entry, suppressed once dismissed or when 14d/30d comeback UI is active (`AC-024`).
  - Build the **`A2` Hybrid Journal Archive** inside the `History` tab with chronological timeline, compact date-jump calendar strip, local title/body search, and mood filter chips.
- **Verification:** Run Robolectric tests for `AC-008` (draft-to-entry atomic promotion & idempotent retry) and `AC-024` (retrospective journal prompt rules).

### Phase 5: 14-Day & 30-Day Comeback Experience (`C1`) & Mood/Habit Insights (`I1`)
- **Deliverables:**
  - Implement **14-day simple welcome banner** (`“Good to see you again. Your progress is still here. Take today at your own pace.”`) and **30-day welcome-back screen** (`“Welcome back! 🌿 What's new with you? You can pick up an old habit, start something new, or simply continue into Dayseed.”`) triggered by 30+ days of app inactivity or 30+ days with zero active habits (`D-047`–`D-067`, `D-206`), with 30d precedence over 14d, once-per-episode suppression, adaptive habit reactivation sheet, and persistent local `COMEBACK_30D` reflection draft.
  - Build the **Insights (`I1`)** screen with habit completion counts, effort/progress-band breakdown, weekly target vs daily target summaries, and **Mood Insights** (`This Week` and `This Month` primary mood distribution, `Compare with previous period` toggle, and tap-a-mood entry filter list matching synthetic fixture `AC-009`).
- **Verification:** Run Robolectric tests for `AC-009` (exact J1–J11 Mood Insights synthetic fixture with Monday and Sunday week-starts) and `AC-011` (14d/30d comeback eligibility, precedence, and draft persistence).

### Phase 6: Local Reminders (`R1`), Portable Backup/Restore with provisional AES-GCM parameters & Conflict Resolution (`D-202`), and Privacy/Delete Flows (`D-203`)
- **Deliverables:**
  - Implement `ReminderCoordinator` (`D-041`–`D-046`): first-run global reminder opt-in card, per-habit reminder time (`09:00` default, `MANUAL` vs `SYSTEM_MANAGED` 14d/28d local learning rule), weekly reflection reminder (`09:00` on week-start day), 2-day inactivity check-in (max 3/week), quiet hours (`21:00–08:00`), shared 3 notifications/day cap, `reminder_deliveries` ledger, and `notification_cancel_outbox` worker/startup retry.
  - Implement **Readable Journal Export** (`Markdown`, `CSV`, `JSON` of saved journal entries only with sensitivity disclosure).
  - Implement **Portable Backup & Restore (`D-094`–`D-106`, `D-202`)**:
    - Versioned `.dayseed.json` envelope with SHA-256 integrity hash, entity category counts, and optional **AES-256-GCM password encryption**. PBKDF2-HMAC-SHA256, 120,000 iterations, a random 16-byte salt, and a 12-byte IV are candidate parameters only; they remain provisional pending security review and must be configurable/versioned in the backup format rather than treated as approved constants.
    - Pre-backup content summary and mandatory verified local safety backup before `REPLACE` or `MERGE` restore.
    - Conflict detection and durable storage in `restore_sessions` / `restore_conflicts` (`D-202`), plus an interactive **Restore Conflict Resolution UI** that offers only record-type-specific supported choices. Preserve both complete validated payloads for review and block an unsafe resolution or affected operation until a supported choice exists. Never silently overwrite, merge, or discard either payload. Do not show a generic `Keep Both` action; offer a both-versions outcome only where the record type and schema explicitly support two distinct records (for example, two journal entries with distinct IDs).
  - Implement the **3 distinct deletion flows (`D-203`)**: single journal entry delete, single habit permanent delete (preserving independent journal entries with `linked_habit_id = NULL` and queueing notification IDs in `notification_cancel_outbox`), and delete-all Dayseed data with full `D-203` scope disclosure.
- **Verification:** Run Robolectric tests for `AC-012`–`AC-014`, `AC-022`, and `AC-026` (reminder ledger/outbox, AES-GCM backup round-trip, Replace/Merge conflict matrix, and habit/all-data deletion boundaries).

### Phase 7: Soft Botanical Theme Polish, Accessibility Audit & Handover Documentation
- **Deliverables:**
  - Apply the **Soft Botanical** Fixed (`System`, `Light`, `Dark`) and **Time of Day** (`Morning`, `Day`, `Evening`, `Night`) theme engine across all screens, verify custom adaptive launcher icon, and enforce 48dp minimum touch targets, `Modifier.testTag(...)` identifiers, and TalkBack `contentDescription` / semantics.
  - Run full JVM and Robolectric test suite (`gradle :app:testDebugUnitTest`) and `compile_applet`.
  - Update `docs/12-PROGRESS-AND-HANDOVER.md` and `docs/28-REQUIREMENT-TRACEABILITY.md` with actual executed test evidence and produce the phase-by-phase handover report.
## Stage sequence and approval boundary

- **Stage A — Planning:** complete and approved/open under D-208.
- **Stage B — Full initial-v1 implementation in Google AI Studio:** begin with an implementation preflight against the latest GitHub `main`, then complete Phases 1–7 above. Preserve useful existing work; identify and reconcile gaps rather than blindly replacing the project. Stage B is **not authorised by this document or by D-209**; it requires a separate explicit written owner instruction.
- **Stage C — Independent technical validation:** begin only after Stage B implementation is complete and committed. Use a fresh/current GitHub checkout and independently verify architecture, schema/migration, builds, automated tests, offline behaviour, backup/restore/delete, notifications, accessibility, and physical-device behaviour. Record defects, correct them through the authorised workflow, and repeat affected validation. Do not treat Stage B developer self-checks as independent Stage C evidence.
- **Stage D — Final owner acceptance and release readiness:** present the verified commit, release candidate, complete test/evidence report, known limitations, privacy/security review, and unresolved risks for explicit owner acceptance. No release-readiness claim or release action without that acceptance.

## AI Studio / GitHub preflight and change-protection rules

Before editing in AI Studio, record the inspection date/time and timezone, repository and branch, latest remote `main` SHA, imported workspace/base SHA (or explicitly state that it cannot be established), and the paths/code locations inspected. Compare the imported workspace with the latest remote branch and preserve useful local work. If the remote branch is newer than the imported workspace, fetch and reconcile the changes before editing; never push the older workspace over newer GitHub commits. Re-check the remote head immediately before pushing. If it advances, stop and reconcile; never force-push, reset away, or overwrite remote work.

Every project inspection record must be dated and commit-specific: include the exact commit SHA, branch, inspected file paths and line ranges or symbols, findings, and any uninspected areas. A statement such as “the project was inspected” without this evidence is insufficient.

AI Studio and every agent must report only tests/builds actually executed and results actually inspected. Use explicit statuses such as `NOT RUN`, `BLOCKED`, `FAILED`, and `PASSED`. A `PASSED` result requires the exact command/task, date/time, commit SHA, environment/toolchain (and device details where relevant), exit result, and retained log/artifact evidence. Planned tests, generated summaries, previews, inferred results, and commands that were not run must never be described as passing. Never fabricate test output, success, or verification.

**Current authorisation:** this is a corrected implementation/validation plan only. It does not start Stage B, inspect or change application source, edit the live database, run builds/tests, install an APK, or authorise Stage C/D.