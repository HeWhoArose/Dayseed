# Proposed Schema Closure, Transaction, and Migration Plan

**Review date:** 2026-10-27  
**Status:** Proposed planning addendum; **not approved for implementation**. No database schema, migration, application code, or tests were changed or executed by this document.

**Authority:** The approved owner choices remain in the [Decision Log](09-DECISION-LOG.md). The current table proposal remains in [Technical Architecture](03-TECHNICAL-ARCHITECTURE.md). This document identifies refinements and acceptance requirements needed to make that proposal coherent. Any disagreement must be reconciled in both documents before the owner approves the complete schema.

## 1. What is already settled

The schema must preserve the following owner-approved rules:

- D-187: local calendar date keys are separate from instants; saved date keys stay stable through timezone travel; historical settings remain effective-dated; archive preserves history; permanent deletion is separate; backdating is explicit.
- D-188/D-193: missed is derived after a scheduled day passes without qualifying activity; no stored user-entered miss and no explicit habit-day Skip action/outcome in v1.
- D-189: numeric/duration activity entries are separate from non-numeric day outcomes; multiple numeric entries per habit/date are valid; completion-only/effort-aware outcomes have at most one current outcome per habit/date.
- D-190: stable UUID identity; same logical retry reuses its idempotency key/identity; distinct intentional activity gets a new key.
- D-191/D-195: DAILY, SELECTED_WEEKDAYS, FLEXIBLE_WEEKLY_TARGET; target period DAY or WEEK as specified; fixed Monday-first weekday mask and invalid mask/schedule combinations rejected.
- D-192: optional per-entry notes and one optional habit/date note can coexist.
- D-194: same-day Apply-today replacement is allowed only before qualifying activity exists today; after qualifying activity, preserve today and offer Apply tomorrow.
- D-186: the Android platform-backup exclusion policy is approved; actual manifest/data-extraction behaviour must be validated separately after authorisation.
- D-091–D-145: portable backup and restore policy is defined at the product level, including full approved coverage, integrity/compatibility checks, Replace/Merge, safety backup, conflict preservation, password-protection direction, and Auto Backup controls.

These approvals do **not** approve the complete schema or prove they are implemented correctly.

## 2. Recommended model refinements to reconcile into the architecture proposal

These are schema recommendations based on approved behaviour, not new owner-approved decisions.

### 2.1 Habit lifecycle and effective-dated configuration

- Treat each configuration version as the source of truth for the state applicable to a local date. Use an explicit lifecycle enum capable of representing ACTIVE, PAUSED, COMPLETED, and ARCHIVED so the approved no-active-habit comeback timer can be calculated from the real effective state.
- Avoid maintaining two competing authoritative archive states. If the current habit row keeps an archived-at value for query convenience, it must be a rebuildable/current-state projection of the latest effective version, updated transactionally—not a second historical source of truth.
- Each habit must have one unambiguous effective configuration for each date. Enforce uniqueness of the pair (habit_id, effective_from_local_date). Resolve historical configuration by selecting the latest version whose effective date is not after the selected record date.
- Preserve configuration history; do not overwrite a version that has already been effective. D-194 governs replacement of a not-yet-effective version for today's date.
- The three D-191 schedule kinds are the approved v1 set. Every-N-days and optional-start-date schedule patterns mentioned in the 2026-10-18 implementation audit are historical proposals and must not be added unless the owner revises the decision log.

### 2.2 Numeric and non-numeric habit records

- Numeric/duration entries use stable UUIDs, a habit ID, ISO local-date key, non-negative whole-number value, optional entry note, creation/update instants, and nullable idempotency key.
- A partial unique index should enforce uniqueness of each non-null idempotency key within numeric entries. Never deduplicate solely because two values or timestamps look similar.
- Non-numeric day outcomes use stable UUIDs, a habit ID, local-date key, explicit outcome kind, optional effort level, creation/update instants, and retry identity/idempotency metadata consistent with D-190. Enforce at most one current outcome per habit/date.
- Completion-only outcomes must not invent an amount or effort score. Effort-aware outcomes must use a valid approved effort value. Quantity/duration records must not satisfy a non-numeric mode, and non-numeric outcomes must not satisfy a quantity/duration mode.
- A table-level unique constraint cannot by itself prevent a cross-table conflict between a numeric entry and a non-numeric outcome for the same habit/date. The write transaction must validate the effective tracking mode and reject contradictory records without altering the prior saved state.
- Numeric daily totals and weekly summaries remain derived from source entries and effective configuration. Do not store a second authoritative total that can drift from those entries.

### 2.3 Journal records and optional habit link

- Saved journal entries use a stable ID, explicit local date, non-whitespace body, optional title, optional primary/secondary mood codes, optional prompt identifier, and creation/update instants. Multiple entries on one date are allowed.
- A saved journal entry's optional habit link must not make the entry dependent on the habit's continued existence. Proposed refinement: a nullable linked_habit_id foreign key with ON DELETE SET NULL, so permanent habit deletion removes the relationship but preserves the journal entry, text, date, title, and mood tags. No journal entry or draft is deleted merely because a habit is archived or deleted.
- A journal edit preserves ID and original creation instant. A deliberate journal date change affects only the journal entry; it never moves habit records.
- A comeback draft is not a saved journal entry. Saving a draft must use a stable intended journal-entry ID and one transaction so a retry after an uncertain result cannot create a duplicate. If saving fails before commit, leave the draft intact; after commit, the saved entry is visible in Journal.
- Journal body validation must reject whitespace-only text at the domain boundary; a database check should reject ordinary empty/space-only values as defence in depth.

### 2.4 Backup-conflict records and notification cancellation

- Replace/Merge decisions require unresolved conflicts to survive later app restarts. The existing table list does not yet model this explicitly. Proposed addition: a restore-session record and a restore-conflict table that preserve the conflicting record type, identifiers, both validated versions, unresolved/resolved state, and timestamps. Store any journal text only in local protected app data, never logs or notification text; delete conflict payloads after the user resolves the conflict and the chosen records are safely committed.
- A conflict record should remain discoverable in Settings → Backup & Restore. Keeping both journal versions creates separate entry IDs. Tracking conflicts must never be auto-added together. A record type that cannot safely keep both remains unresolved or stops safely; it must not silently select a winner.
- Database transactions cannot make Android's external notification manager part of the SQLite commit. Proposed deletion refinement: within a database transaction, gather opaque IDs/tags for a habit's pending/posted notifications into a small cancellation outbox and remove the habit-owned records/ledger plus habit identity; preserve independent journal entries and global reminders. After commit, cancel the OS notifications and remove outbox rows only after successful cancellation. If cancellation fails or the app stops, retry from the outbox on the next app start; any delivery worker must check that the habit still exists and is active immediately before posting. The outbox should retain only opaque cancellation identifiers, no habit name or journal text.
- Do not transfer OS-specific scheduled-notification IDs as portable data. Preserve logical reminder-delivery keys/status where needed for deduplication, then rebuild eligible future scheduling on restore from current settings and source records.
- The outbox is technical cleanup state, not a user record. Its retention must be limited to the minimum needed for reliable cancellation.

## 3. Candidate relational invariants

The following SQL/domain invariants should appear in the final architecture model and acceptance plan.

### 3.1 Effective-dated habit versions

- Primary key: stable version UUID.
- Foreign key: habit ID references the parent habit; a permanent parent deletion removes its owned versions.
- Unique: (habit_id, effective_from_local_date).
- Required: valid local date, non-empty name, valid tracking mode, valid target period, positive integer target value, and valid lifecycle/reminder enum values.
- Quantity/duration modes require a valid unit code. Completion-only/effort-aware modes do not use a fabricated numeric unit.
- Schedule-kind/target-period/mask combinations:
  - DAILY: target period DAY; scheduled mask NULL; preferred mask NULL.
  - SELECTED_WEEKDAYS: target period DAY; scheduled mask in 1–127; preferred mask NULL.
  - FLEXIBLE_WEEKLY_TARGET: target period WEEK; scheduled mask NULL; preferred mask NULL or in 1–127.
- Every present mask must contain only the seven approved bits. Because the mask is one integer, reject unknown higher bits and invalid combinations rather than silently trimming them.
- For flexible weekly completion-only/effort-aware goals, target value represents a count of qualifying completed dates, not a numeric amount of work. The UI/domain validation must keep the meaning of target value mode-specific.

### 3.2 Entries and outcomes

- Numeric entry value must be an integer greater than or equal to zero; negative values are rejected without changing saved data.
- Non-null idempotency keys must be unique in the table where they apply. A retry reuses the same key/ID; a new intentional record gets a new key/ID.
- Completion-only day outcomes permit only the completion outcome and no effort value. Effort-aware outcomes require an allowed effort value. A unique constraint on (habit_id, local_date) prevents two current day outcomes for one habit/date.
- Do not permit contradictory numeric-entry/day-outcome combinations for the effective tracking mode. Validate inside the write transaction and roll back if any row fails.
- A note table has at most one habit/date note per (habit_id, local_date); entry notes remain on their specific numeric/duration entry and never become habit outcomes.
- Local date strings must be canonical ISO YYYY-MM-DD and validated as real dates by the domain layer. Creation/update instants are UTC epoch milliseconds; never use a converted UTC date as the stored local-date key.

### 3.3 Journal, settings, reminders and restore state

- Saved journal entry body is non-blank after validation; IDs remain stable across edits; multiple entries per local date are valid.
- Nullable linked_habit_id uses ON DELETE SET NULL; the entry has no cascade dependency on its habit.
- Settings use stable keys and validated value types. Do not put journal text inside generic settings values.
- Reminder delivery keys are unique per logical occurrence; delivery rows contain no personal message copies. Habit-linked delivery rows are deleted with that habit.
- Restore conflicts must preserve both validated versions until explicit resolution, and remain restorable/reviewable after process death. A failed restore must not leave live data partially changed.
- The notification-cancellation outbox stores only opaque platform notification identifiers and timestamps, is retried idempotently, and is cleared after successful cancellation.
- All foreign keys must be enabled on each SQLite connection. Minimum indexes include: effective versions by habit/date; entries by habit/date/time; partial unique non-null idempotency keys; day outcomes by habit/date unique; habit/date notes unique; journal entries by date/time; drafts by updated time; pending reminder deliveries; restore conflicts by unresolved status; and cancellation outbox pending rows.

## 4. Transaction boundary map

| Operation | Required atomic database work | Failure rule |
|---|---|---|
| Create habit | Insert stable habit identity and its initial effective configuration version together | No partially created habit; retry uses the same stable identity |
| Save numeric entry | Validate current effective configuration and unit; insert one entry; update only rebuildable caches if any | No success before commit; retry of the same logical submit counts once |
| Save/edit day outcome | Validate mode and existing numeric rows; insert/update the single current outcome | Reject contradictory/out-of-mode writes and roll back |
| Edit/delete one record | Update or delete only the selected stable ID; invalidate/recompute dependent summaries | Unrelated records remain untouched |
| Change schedule/target | Apply D-194 effective-date rule and write/revise the applicable version | No past record/config reinterpretation; Cancel means no change |
| Save journal entry/draft | Save entry and resolve/remove the source draft in one transaction; preserve stable intended ID | Failure before commit leaves the draft unchanged; retries cannot duplicate |
| Archive/pause/complete/reactivate | Change effective lifecycle state and its effective timestamp/date in one transaction | History stays accessible; active/no-active timer recalculates from effective state |
| Permanently delete habit | Transactionally queue opaque notification-cancel IDs, delete habit-owned tracking/configuration/delivery records, null optional journal links, and delete the habit | Independent journal entries/global reminders survive; cancellation outbox retries separately |
| Create backup | Produce a consistent snapshot of the approved backup data set, write, close, and read/verify output before reporting success | Never present a partial/unverified file as valid; source database unchanged |
| Restore Replace/Merge | Fully validate file first; require verified safety backup; apply data and unresolved conflicts in one controlled transaction/session | Invalid file, failed safety backup, or transaction error leaves pre-restore data intact; interruption recovers or rolls back safely |
| Resolve one conflict | Write the chosen result(s) and clear only that resolved conflict in one transaction | Keep both source versions until the chosen resolution commits |

The undo interaction for newly created habit activity remains an explicit product decision. Proposed safe default for owner review: offer a short, in-session Undo only for a newly created log/outcome and undo that exact stable ID; edits remain explicit corrections, and confirmed permanent journal deletion remains irreversible per D-070. The undo window and whether deletion of a numeric entry itself requires confirmation must be agreed before the planning gate.

## 5. Portable backup mapping

Readable journal export is not the same artifact as the full Dayseed portable backup.

**Readable journal export (v1):** saved journal entries only, including local dates, optional title, text and recorded mood tags. Do not silently include unsaved comeback drafts or reminder metadata. It does not delete or alter source records.

**Portable Dayseed backup (v1):** map every included entity/relationship explicitly using stable IDs and a dedicated versioned format. Proposed categories:

- Habit identities and every effective configuration version, including schedule masks, target/tracking modes, units, lifecycle and reminder preferences.
- All numeric/duration entries, non-numeric day outcomes, habit/date notes, and entry-specific notes.
- All saved journal entries including optional habit links and mood tags, and unresolved journal drafts as drafts rather than saved entries.
- Relevant app preferences such as week-start, theme, reminder opt-in/time preferences and approved backup preferences, excluding credentials/secrets/premium purchase verification.
- Logical reminder-delivery keys/statuses needed to deduplicate past logical occurrences; do not carry OS-specific notification IDs as active scheduled work.
- Unresolved restore-conflict payloads until user resolution, so a second migration/backup cannot silently lose either version.
- Do not include transient UI state, cache-only totals, platform notification cancellation outbox rows, or an Android provider's access-grant tokens as portable data. Destination access must be checked/reselected on the receiving installation when unavailable.

The file should carry format/schema version, creation instant, an integrity value and a category/count summary. If compatibility cannot be established or integrity fails, stop before live mutation. Manual password-protected backup needs a dedicated encryption envelope; the exact cryptographic parameters remain subject to a security review before release.

## 6. Proposed migration plan from the observed version-1 database

The read-only source audit dated 2026-10-18 reported SQLiteOpenHelper database version 1, a placeholder upgrade handler, and a daily_records table with a unique habit/date pair. Do not assume the observed schema has changed since that audit; inspect the actual checkout only after separate technical-validation authorisation.

1. **Design the versioned migration before code:** record the exact old schema and all current columns/indexes after the technical gate opens. Preserve v1 as the rollback source until the migration is fully verified.
2. **Create a deterministic mapping:** each existing daily_records row may become one numeric/duration entry or one appropriate non-numeric day outcome based on its actual saved tracking mode and value. A legacy daily aggregate cannot be reconstructed as multiple historical sessions; never invent sessions.
3. **Convert schedule representation explicitly:** translate the legacy selected-day representation to the approved Monday-first mask by mapping the actual stored weekday identities, not by assuming current locale or week-start.
4. **Preserve awkward records:** missing habit parents, invalid dates, unsupported values, incompatible modes, and unmappable records must not be silently dropped or coerced. Migration must either preserve them in a reviewable recovery form or stop and roll back; choose the concrete quarantine strategy only after inspecting the real v1 schema.
5. **Migrate inside a controlled transaction:** create the new tables/indexes, migrate every source row, validate row counts and invariants, check foreign keys and recomputed totals, then advance the SQLite user version only after all checks pass.
6. **Prove rollback first:** use synthetic v1 fixtures with duplicate/zero/negative/corrupt values, each tracking mode, old weekday representations, orphan rows, and injected failure at every migration stage. Verify rollback preserves the v1 database and no partially upgraded version is presented as complete.
7. **Keep device/OS backup separate:** D-186 says sensitive Dayseed data should not be included in Android platform backup by default. Portable backup/restore coverage and actual platform exclusion must be tested separately, after authorisation.

No migration should be implemented from this document alone. The actual schema, wrapper/toolchain, and data quality must be inspected under the separate technical-validation gate before the migration is finalised.

## 7. Remaining closure checklist

- [ ] Review the proposed lifecycle-state refinement and optional journal-habit link against D-071, D-149–D-160, and D-172; reconcile into the canonical architecture.
- [ ] Add proposed restore-session/conflict structures and the notification-cancellation outbox to the schema proposal, including their retention and backup rules.
- [ ] Confirm the full mask/target/mode checks, indexes, per-operation transactions, and rollback tests in the architecture and test plan.
- [ ] Decide the short-lived undo contract and whether deleting an individual numeric entry needs confirmation; do not silently default destructive recovery behaviour.
- [ ] Map all entities/fields into the portable backup and journal export separately; close unresolved “Keep both” limitations without changing the approved conflict policy.
- [ ] Record the proposed schema for explicit owner review and approval before any application schema change.
- [ ] Only after the planning gate and separate technical-validation authorisation, inspect actual source/schema/build/toolchain, run synthetic migration tests, and verify backup/restore, deletion, notification and device behavior.

**Status boundary:** This document improves planning detail only. No code, database, migration, build, test, APK, or device was modified or validated.
