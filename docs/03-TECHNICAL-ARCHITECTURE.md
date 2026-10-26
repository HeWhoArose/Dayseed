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

**HabitRecord:** identifier, habit identifier, local date key, outcome, optional effort level, optional note, created/updated timestamps. Enforce the approved uniqueness rule for a habit/date in the database, not only in UI.

**JournalEntry:** identifier, local date key, body, optional prompt identifier, created/updated timestamps. Whether one or multiple entries per day are supported is open.

**Preferences:** explicit user settings such as theme, onboarding completion, and reminder preferences. Avoid storing derived metrics as source of truth if they can safely be recalculated.

Exact schema, indexes, cascade behaviour, and migration strategy require a reviewed data design.

## Date and time policy
Separate timestamps (instants) from local calendar dates. Habit completion usually belongs to the user's chosen local day, not a UTC date. Decide behaviour for travel, timezone changes, device date changes, and editing past records. Centralise date logic and test boundaries.

## Offline-first rules
- No network request is required for core actions.
- A successful save means data is committed locally.
- Network absence must not prevent navigation, writing, editing, or history viewing.
- Initial v1 has no account, online backup, or cross-device sync service. Premium online backup/sync is a future capability only and requires a separate explicit product, privacy/security, consent, data-flow, retention/deletion, conflict-resolution, and migration gate.
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
