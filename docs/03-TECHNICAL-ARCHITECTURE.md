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

**HabitRecord / tracking entry:** stable identifier, habit identifier, local date key, mode-specific value/outcome, optional effort level, optional entry-specific note, created/updated timestamps, and retry/idempotency metadata where needed. A habit may have multiple legitimate numeric/duration entries on one local date; sum distinct valid entries into the daily total while retaining each entry for correction. Do not impose a blanket unique constraint on habit + date. Protect against duplicate submission of the same logical entry with stable identity/idempotency rules. Explicit zero, completion-only state, effort-aware state, explicit miss, and no record must remain distinguishable.

**JournalEntry:** stable identifier, local date key, body, optional title, primary mood and optional secondary mood where present, optional prompt identifier, created/updated timestamps. Multiple entries per date are approved (D-170); editing preserves entry identity, and changing a journal date does not modify habit records. Unfinished drafts remain distinct from saved entries.

**Daily habit state / schedule interpretation:** define how scheduled, rest/non-scheduled, paused, explicit miss, zero, partial, goal-met, and unrecorded states are represented or derived. Do not use a single uniqueness constraint to erase multiple entries or treat missing data as zero.

**Preferences:** explicit user settings such as theme, onboarding completion, and reminder preferences. Avoid storing derived metrics as source of truth if they can safely be recalculated.

Exact schema, indexes, transaction boundaries, cascade behaviour, stable-ID/idempotency strategy, and migration strategy require a reviewed data design. The current SQLiteOpenHelper schema is version 1 with a placeholder upgrade handler; no migration is verified.

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
