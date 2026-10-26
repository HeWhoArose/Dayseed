# Technical Architecture

**Status:** Repository now contains a native Android Kotlin/Jetpack Compose implementation. Architecture documentation is being reconciled against the actual code; design and data-contract gaps remain.

## Architecture goals
- Native-feeling Android-first experience.
- Local-first data access with reliable persistence.
- Core features available without network.
- Clear separation between UI, domain rules, and storage.
- Schema migrations and testable date logic.
- Minimal collection of personal data.
- A realistic path to iOS later without compromising Android v1.

## Approved stack direction
**Owner decision D-009:** Continue the existing native Android project using Kotlin + Jetpack Compose. The observed baseline includes Android `SQLiteOpenHelper`, a repository, ViewModel/StateFlow, and Compose screens. This is approval of the stack direction, not a claim that the project builds, passes tests, or is ready for feature work. The current database schema is version 1 and its upgrade handler is a placeholder; migration safety remains a planning/verification requirement.

No implementation should start based solely on this proposal.

## Layering
- **Presentation:** screens, accessible components, navigation, UI state.
- **Application/use cases:** create habit, record outcome, save reflection, edit history, calculate summaries, generate comeback message.
- **Domain:** habit schedules, daily outcome semantics, effort levels, date rules, summary calculations.
- **Persistence:** SQLite repositories, migrations, transaction boundaries, export and deletion.
- **Platform adapters:** notifications, app lifecycle, share/export, platform facilities if needed.

Domain logic should be testable without rendering UI or requiring network access.

## Local data model proposal
**Habit:** identifier, name, optional description, tracking mode, schedule, optional reminder configuration, created/updated timestamps, archived timestamp, and version metadata as appropriate.

**HabitRecord / tracking entry:** stable identifier, habit identifier, local date key, mode-specific value/outcome, optional effort level, optional entry-specific note, created/updated timestamps, and retry/idempotency metadata where needed. A habit may have multiple legitimate numeric/duration entries on one local date; sum distinct valid entries into the daily total while retaining each entry for correction. Do not impose a blanket unique constraint on habit + date. Protect against duplicate submission of the same logical entry with stable identity/idempotency rules. Explicit zero, completion-only state, effort-aware state, and no qualifying record must remain distinguishable. A missed scheduled day is derived from the applicable schedule and absence of qualifying activity after the day has passed; it is not a user-entered outcome.

**JournalEntry:** stable identifier, local date key, body, optional title, primary mood and optional secondary mood where present, optional prompt identifier, created/updated timestamps. Multiple entries per date are approved (D-170); editing preserves entry identity, and changing a journal date does not modify habit records. Unfinished drafts remain distinct from saved entries.

**Daily habit state / schedule interpretation:** derive scheduled-day status from the effective schedule/configuration and qualifying activity records. Rest/non-scheduled days are neutral; paused periods are excluded; before a scheduled day has passed, no record is merely unrecorded; after it passes with no qualifying record, derive missed status. Never offer a manual mark-as-missed action. Zero, partial, goal-met, above-target, completion-only, effort-aware, and unrecorded states remain distinct. Do not use a single uniqueness constraint to erase multiple entries or treat missing data as zero.

**Preferences:** explicit user settings such as theme, onboarding completion, and reminder preferences. Avoid storing derived metrics as source of truth if they can safely be recalculated.

Exact schema, indexes, transaction boundaries, cascade behaviour, stable-ID/idempotency strategy, and migration strategy require a reviewed data design. The current SQLiteOpenHelper schema is version 1 with a placeholder upgrade handler; no migration is verified.

## Proposed v1 relational schema — owner review required

**Status: Proposed, not owner-approved and not implementation-ready.** This design turns the approved product invariants into a concrete SQLite model while preserving the existing Kotlin + `SQLiteOpenHelper` stack. It must not be treated as permission to change code. Owner approval is needed for the key structure below; actual schema v1, migrations, and current source behaviour remain unverified.

### Tables and responsibilities

1. **`habits`** — stable identity only: `habit_id` (UUID text primary key), `created_at_utc_ms`, `updated_at_utc_ms`, and nullable `archived_at_utc_ms`. Archiving hides a habit from active use but keeps history. Permanent deletion is a separate explicit operation that removes the selected habit and its owned tracking/configuration data transactionally; it never deletes independent journal entries. Do not retain a tombstone after a confirmed permanent deletion unless a later privacy decision requires one.
2. **`habit_versions`** — immutable, effective-dated snapshots of the habit's user-facing configuration: `version_id` (UUID primary key), `habit_id` (foreign key), `effective_from_local_date` (ISO YYYY-MM-DD), `name`, nullable `description`, `tracking_mode` (quantity, duration, completion-only, effort-aware), nullable `unit_code`, `target_period` (`DAY` or `WEEK`), non-negative `target_value`, `schedule_kind` (`DAILY`, `SELECTED_WEEKDAYS`, or `FLEXIBLE_WEEKLY_TARGET`), nullable `scheduled_weekdays_mask` (used only by `SELECTED_WEEKDAYS`), nullable `preferred_weekdays_mask` (optional guidance for `FLEXIBLE_WEEKLY_TARGET`, never a restriction), `lifecycle_state` (active or paused), reminder mode/time, and `created_at_utc_ms`. Unique constraint: (`habit_id`, `effective_from_local_date`); an edit effective on an existing version date replaces that not-yet-effective version inside a transaction, rather than creating ambiguous versions. Past versions remain unchanged. Store structured schedule fields rather than relying on an opaque JSON blob. The exact supported schedule columns must match the approved schedule vocabulary. D-191 approves the structured fields above: `DAILY` means every local calendar day; `SELECTED_WEEKDAYS` uses a non-empty weekday bitmask to define scheduled days; `FLEXIBLE_WEEKLY_TARGET` has no mandatory weekdays and may store optional preferred weekdays only as routine guidance. `target_period` identifies whether `target_value` is a daily or weekly goal; quantity/duration progress sums compatible recorded units over that period, while completion-only/effort-aware weekly goals count qualifying completed dates, not arbitrary invented numeric values. The saved app-level week-start preference defines every weekly boundary. For daily schedules, `target_period` must be `DAY`; a flexible weekly target uses `WEEK`. Weekday masks use one documented stable weekday encoding, independent of locale; locale affects only the initial week-start default. Enforce enum/mask/target-period combinations in the domain layer.
3. **`habit_entries`** — individually correctable quantity/duration sessions: `entry_id` (UUID primary key), `habit_id` (foreign key), `local_date`, `value_units` (non-negative integer; zero is valid), nullable `entry_note`, `created_at_utc_ms`, `updated_at_utc_ms`, and nullable `idempotency_key`. Do **not** add a unique constraint on (`habit_id`, `local_date`). A partial unique index on non-null `idempotency_key` prevents replay of the same logical submission; the caller must reuse the same key only for retries of that submission, never for a distinct intentional entry. Keep units canonical per habit version and do not mix incompatible units in a daily sum.
4. **`habit_day_outcomes`** — explicit non-numeric positive/effort states only: `outcome_id` (UUID primary key), `habit_id`, `local_date`, `outcome_kind` (completion-only complete or effort-aware selection), nullable `effort_level`, and created/updated instants. Unique constraint: (`habit_id`, `local_date`) so there is at most one current day-level positive/effort outcome for a habit/date. The domain layer validates outcome kind against the effective tracking mode and prevents contradictory numeric-entry/day-outcome states. Completion-only and effort-aware outcomes cannot substitute for quantity/duration entries, and quantity/duration entries cannot satisfy a non-numeric tracking mode. Do not store a user-entered `miss` outcome: missed status is derived from the applicable schedule and absence of qualifying activity after the scheduled day has passed. A numeric zero is a `habit_entries` row, not an outcome. No record before day-end is unrecorded; after a scheduled day passes with no qualifying activity, derive missed. Genuine activity may be explicitly backdated to its actual local date and must recalculate that date's result and dependent summaries. A journal memo alone never changes habit status.
5. **`habit_date_notes`** — optional date-level context independent of session notes: `note_id` (UUID), `habit_id`, `local_date`, `body`, created/updated instants. Unique constraint: (`habit_id`, `local_date`); deleting the note does not delete tracking entries.
6. **`journal_entries`** — saved independent reflections: `journal_entry_id` (UUID), `local_date`, `body`, nullable `title`, nullable primary/secondary mood codes, nullable `prompt_id`, `created_at_utc_ms`, `updated_at_utc_ms`. Multiple entries per date are allowed; no date-level uniqueness. Editing preserves ID and creation instant. A date correction affects only this journal entry.
7. **`journal_drafts`** — unsaved content kept separate from saved entries: `draft_id` (UUID), optional `local_date`, body/title/mood/prompt fields as needed, created/updated instants, and optional `source_context`. A draft is not a journal entry until explicit Save. Multiple drafts may exist; do not silently overwrite one draft with another.
8. **`app_settings`** — typed, versioned local preferences such as week-start, reminder opt-in, quiet hours, theme, and backup preferences. Use a stable setting key and validated value representation; do not store personal journal content here.
9. **`reminder_deliveries`** — durable deduplication ledger: `delivery_key` (unique primary key for a logical occurrence), reminder type, optional habit ID, intended local date/week key, status, scheduled/attempted/delivered instants, and dismiss/suppression metadata as required. Persist the key before or atomically with scheduling state so restart/reschedule does not create duplicate logical deliveries. Do not store message copies containing personal text.

### Relationships, indexes, and invariants

- Foreign keys must be enabled on each SQLite connection. Owned habit tables reference `habits`; deletion of a habit and its owned rows occurs in one explicit transaction. Journal entries/drafts and global settings have no habit foreign key and are unaffected by habit deletion.
- Minimum indexes: `habit_versions(habit_id, effective_from_local_date)` unique; `habit_entries(habit_id, local_date, created_at_utc_ms)`; partial unique `habit_entries(idempotency_key)` when non-null; `habit_day_outcomes(habit_id, local_date)` unique; `habit_date_notes(habit_id, local_date)` unique; `journal_entries(local_date, created_at_utc_ms)`; `journal_drafts(updated_at_utc_ms)`; `reminder_deliveries(delivery_key)` primary/unique plus an index for pending deliveries.
- Use integer UTC epoch milliseconds for instants and ISO `YYYY-MM-DD` strings for user-intended local date keys. Never derive an existing record's date key by converting its timestamp after a timezone change.
- Recompute daily/weekly totals and progress from source entries and the applicable `habit_versions`; derive missed status from schedule plus absence of qualifying activity after the scheduled day has passed. Do not make cached totals/status authoritative. Any cache introduced later must be rebuildable and invalidated transactionally. Journal entries/notes never count as activity evidence or alter habit status; a genuine backdated activity record does.
- All multi-row actions (save/edit/delete entry, change effective-dated configuration, archive/restore, permanent deletion, restore/merge, and reminder state updates) require explicit transaction boundaries. Validate input and mode/config compatibility before commit; show success only after commit.
- Stable UUIDs are generated once for each entity and retained for its lifetime; editing preserves identity. For a logical activity submission, generate an idempotency key and reuse that same key on retries; a distinct intentional entry gets a new key. Enforce uniqueness for non-null keys, while allowing null for records that do not need retry protection. Never deduplicate based only on equal values or close timestamps. This rule is owner-approved by D-190; it does not approve the complete proposed schema.
- Every habit configuration change must resolve an effective local date using the approved Apply today/Apply tomorrow interaction. The active configuration for a past date is the latest version whose effective date is not after that date. Schedule edits must not synthesize historical records.
- Portable backup format must be versioned and map every owned entity/relationship explicitly. Restore validates all records, IDs, schema compatibility, and constraints before mutation; merge/replace behaviour remains governed by approved backup decisions.

### Required owner decisions before this proposal becomes the approved model

**Owner decisions recorded — approved:** (a) use effective-dated `habit_versions` snapshots as the source of historical schedule, target, tracking-mode, and other habit-configuration meaning; past versions remain unchanged and configuration changes apply prospectively from their effective local date (previously approved schema decision); and (b) store numeric/duration activity separately from non-numeric daily outcomes (D-189). `habit_entries` retains each quantity/duration session, permits multiple entries per habit/date, and treats explicit zero as a real entry. `habit_day_outcomes` stores at most one current completion-only or effort-aware outcome per habit/date. The domain layer must reject a day outcome that conflicts with the effective tracking mode, reject incompatible numeric entries/outcomes for that mode, and derive missed status rather than storing a miss. These are partial schema decisions only and do not approve the complete schema for implementation. **Remaining owner review:** (c) whether to retain one optional habit/date context note alongside per-entry notes; and (d) structured schedule fields are approved by D-191; confirm only the stable weekday-mask encoding and validate it during technical review. Stable UUID identity and nullable idempotency keys with uniqueness when present are now approved as D-190. D-188 resolves the missed-day rule: no manual miss outcome; derive missed status after a scheduled day passes without qualifying activity, permit genuine explicitly backdated activity to recalculate that date, and never let a journal memo change habit status. Do not implement or migrate until this proposal is approved and the current version-1 schema has been inspected under an explicitly authorised technical-validation gate.

## Date and time policy
Separate timestamps (instants) from local calendar dates. Habit completion belongs to the local calendar date explicitly selected/confirmed for the record, never to a UTC date merely because the timestamp converts to that date. Use one injectable clock/date-policy service for all date-sensitive rules.

### Approved date/time and history contract (D-187 planning baseline)

- **Canonical day key:** store a local calendar date (YYYY-MM-DD) separately from event timestamps. A record's day key is the date the user intentionally chose when recording; store creation/update instants separately for ordering and recovery.
- **Default date for a new entry:** the current device-local date when the user starts/commits the action. If the user explicitly selects a past/future date in a supported flow, use that selected date and make it visible before save. Do not silently move records when the device time zone changes.
- **Travel/time-zone changes:** use the current device-local calendar for new default dates, Today, week labels, and future local reminder scheduling. Preserve existing date keys and event instants; do not reinterpret historical records just because the current zone changed.
- **Week boundaries:** derive week start/end from the user's saved week-start preference; use the device/locale convention as the initial default where available. Apply the same helper to weekly targets, reminders, insights, and calendar UI.
- **Backdating:** allow explicit historical corrections and retrospective journal entries where the product flow supports them. Require explicit save; update only the selected record/entry and dependent summaries. A journal date change must never move habit records.
- **Clock changes / uncertain time:** do not fabricate elapsed time or shift stored records. If timestamps appear inconsistent, preserve user data, avoid duplicate comeback/reminder events, and use conservative eligibility until time confidence is re-established. Do not claim a precise absence duration without sufficient evidence.
- **Daylight-saving changes:** use platform time-zone rules for local date boundaries; store instants independently and test skipped/repeated local clock times where the relevant time zone observes DST. Do not use fixed 24-hour arithmetic for calendar-day logic.
- **Scheduled reminders:** compute eligibility from local calendar/week policy and persist a stable delivery key for each logical reminder occurrence. Respect explicit reminder opt-in, OS permission, quiet hours and approved caps. On zone changes/restarts, recalculate future delivery without duplicating an already-delivered occurrence. Android may defer delivery.
- **Comeback timing:** preserve the specific approved rules in D-146–D-160 (including full local-calendar-day thresholds and no duplicate display after zone changes). This shared policy does not override their trigger-specific rules.
- **Historical schedule/target interpretation:** preserve the settings effective for each historical date. Schedule/target edits apply prospectively using the approved effective-date choice; they do not silently rewrite past records or create missed records retroactively.
- **Habit lifecycle:** archive is the default non-destructive way to remove a habit from active use; archived history remains accessible. Permanent deletion is a separate, clearly explained user action and must not delete independent journal entries.
- **Manual clock changes:** do not use wall-clock jumps alone to infer activity, absence, or elapsed reminder intervals. Preserve saved dates and values, deduplicate logical events, and use conservative eligibility when time confidence is uncertain.

This is the approved v1 planning baseline under D-187; trigger-specific approved decisions remain authoritative where more specific. It does not prove implementation correctness. Exact storage columns/indexes, transaction boundaries, migration safety, and test fixtures remain to be finalised and technically verified after the planning gate.

## Offline-first rules
- No network request is required for core actions.
- A successful save means data is committed locally.
- Network absence must not prevent navigation, writing, editing, or history viewing.
- Initial v1 has no account, online backup, or cross-device sync service. Premium online backup/sync is a future capability only and requires a separate explicit product, privacy/security, consent, data-flow, retention/deletion, conflict-resolution, and migration gate.
- **Android platform backup (D-186):** exclude sensitive Dayseed app data from Android Auto Backup/device-transfer by default. Implement and verify explicit backup/data-extraction rules for private records and drafts. This is distinct from Dayseed's user-controlled portable backup and opt-in file-based Auto Backup to a selected destination. Do not claim the exclusion is effective until configuration and supported OS/device-transfer paths have been inspected and tested.
- Ads, if approved, load asynchronously and never delay core UI.

## Failure handling
- Handle database open/write failures with clear recovery messages.
- Use transactions for operations affecting multiple records or settings.
- Avoid silent data loss and never log journal contents.
- A migration failure must preserve recoverable data and must not pretend migration succeeded.
- Notifications may be delayed or restricted by Android; in-app behaviour must remain correct without them.

## Architecture validation before approval
After the planning gate permits technical validation, verify official SDK/toolchain support; Android release build on a real device; SQLite transactions and migrations; offline persistence across force-stop/restart; notification permission/scheduling; export and deletion; app size and performance on representative devices; and accessibility integration.

Record actual versions, commands, device details, outcomes, and limitations. Do not mark checks verified without evidence.

## Stack comparison result (2026-10-17)
The older Expo/Kotlin comparison was a planning artefact and does not describe the code currently in `main`. The repository now contains native Kotlin/Compose source. Treat the current implementation as the working baseline unless the owner explicitly approves a stack change. See `docs/22-IMPLEMENTATION-REALITY-AUDIT.md`.

Room is not currently used in the observed implementation: persistence uses platform `SQLiteOpenHelper`. Consider Room only as a future architectural decision after reviewing migration cost and project needs; do not describe it as installed or implemented.

Exact Android Gradle Plugin, Gradle, Kotlin, Android SDK, and library versions must be read from the existing project and documented before technical validation. Do not upgrade versions or add dependencies by default; first establish the current wrapper/toolchain and compatibility constraints. After the planning gate is explicitly approved, validate the existing project with reproducible Gradle build/test and physical-device evidence. No web preview or second application scaffold is an acceptable substitute for the native Android app.

### Dependency and migration caution

Do not introduce Room or replace `SQLiteOpenHelper` merely because Room is available. Evaluate that migration as a separate architectural change only if the evidence and maintenance needs justify it. Use compatible, pinned dependency versions and avoid independently upgrading “latest” packages. The historical [stack comparison](14-STACK-COMPARISON.md) is research context, not the active stack instruction.
