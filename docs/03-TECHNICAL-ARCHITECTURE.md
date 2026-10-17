# Technical Architecture

**Status:** Proposal; no stack is finally approved and no compatibility claim is verified here.

## Architecture goals
- Native-feeling Android-first experience.
- Local-first data access with reliable persistence.
- Core features available without network.
- Clear separation between UI, domain rules, and storage.
- Schema migrations and testable date logic.
- Minimal collection of personal data.
- A realistic path to iOS later without compromising Android v1.

## Stack candidate
**Candidate only:** React Native + Expo + TypeScript, with local SQLite. This is a starting hypothesis, not a settled choice. Before approval, verify current support for the required SQLite API, migrations, local notifications, Android backup controls, export/sharing, and release builds. Compare with native Kotlin if platform integration or reliability requirements favour it.

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
- Future sync requires a separate privacy, conflict-resolution, and migration gate.
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
The official-docs comparison is recorded in [docs/14-STACK-COMPARISON.md](14-STACK-COMPARISON.md). Its conditional recommendation is **React Native + Expo + TypeScript + expo-sqlite** because Dayseed's v1 is dominated by ordinary app UI and local-first features, a later iOS client remains plausible, and Expo has a documented hosted/local APK/AAB route. This is a narrow planning preference, not final approval.

**Native Kotlin + Jetpack Compose with Room** remains the fallback and has stronger direct Android control plus compile-time query/migration support through Room. Revisit the recommendation if precise reminders or platform integration are core, iOS is no longer expected, or Expo's package/build spike fails.

Exact SDK and library versions are intentionally not specified yet. Once the planning gate is approved, select a compatible SDK family, resolve Expo package versions with Expo's installer, check with Expo Doctor, lock versions, and prove persistence, migration, notifications, and real-device APK install in a bounded spike. No web output is an acceptable substitute for the native app.

### Current package-version caution

Current official docs are versioned and can move independently: Expo's SQLite install instructions use SDK-aware package resolution, while Android's Room docs now distinguish the Room 2.x line from Room 3.0's `androidx.room3` artifacts and Kotlin/KSP/coroutine-first APIs. Do not copy package versions from old examples or combine “latest” dependencies independently. See [the stack comparison](14-STACK-COMPARISON.md) for the sources and required compatibility matrix.
