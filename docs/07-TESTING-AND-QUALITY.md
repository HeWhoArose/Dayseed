# Testing and Quality

**Status:** Required test strategy for the approved native Kotlin + Jetpack Compose direction; implementation and validation remain unauthorised until the planning gate is approved.

Requirement-to-test mapping is tracked in [28-REQUIREMENT-TRACEABILITY.md](28-REQUIREMENT-TRACEABILITY.md). Test cases in that matrix and this file are plans only unless explicit execution evidence is recorded.

## 1. Quality goals
Protect personal data, prevent loss of history, keep core flows usable offline, ensure dates behave predictably, and verify that the app remains supportive after interruptions.

## 2. Test layers
- **Domain unit tests:** effort levels, schedules, outcomes, summary calculations, comeback bands, date boundaries.
- **Persistence tests:** inserts, updates, stable identity, idempotency for retries of the same logical submission, legitimate multiple entries per habit/date, transactions, migrations, deletion, and export/import if supported. Do not impose a blanket unique constraint on habit + date; distinct valid entries must remain separate.
- **Application tests:** use cases with fake repositories and deterministic clocks.
- **UI tests:** first launch, logging, journaling, history correction, privacy settings, reminders, comeback dismissal.
- **Device tests:** offline mode, force-stop/restart, low storage, permission denial, timezone/date changes, accessibility settings.
- **Release checks:** signed Android build, installation/upgrade path, crash-free core flows, privacy disclosures.

## 3. Essential regression scenarios

1. Tiny, Usual, Extra, and completion-only semantics remain correct; actual values above target are preserved.
2. A journal entry can be saved independently of habit completion.
3. Multiple journal entries on one date remain distinct; editing preserves identity, and changing an entry date does not modify habit records.
4. Multiple numeric entries/duration sessions for one habit and local date sum correctly; distinct entries remain individually editable.
5. Explicit zero, completion-only state, effort-aware state, derived missed status, and no record remain distinguishable; no user-entered miss outcome or manual mark-as-missed action exists.
6. A past record edit updates all dependent summaries without changing unrelated records.
7. Archive preserves history; deletion follows the approved deletion policy and does not accidentally erase independent journal entries.
8. Repeated taps or retries of the same logical action do not create duplicate records, while legitimate repeated sessions remain possible.
9. App process termination does not lose acknowledged writes; failed or uncertain saves preserve drafts and do not create duplicate journal entries.
10. Offline operation does not show endless loading or require sign-in.
11. Comeback thresholds, local-date boundaries, and dismissal work at boundary dates; changed or uncertain clocks do not create duplicate or fabricated messages.
12. Notification denial, disabled notifications, quiet hours, and OS restrictions do not break core tasks or bypass reminder opt-in.
13. Readable journal export and local backup/restore work without an account or Dayseed cloud service; validate backup integrity, password retry behavior, Replace/Merge safety, conflict preservation, and recovery from interruption.
14. Initial v1 core flows do not require network access and do not transmit journal/habit data to a Dayseed cloud service; inspect actual SDK and data paths before making privacy claims.
15. Verify D-186's Android platform-backup/device-transfer exclusion separately from Dayseed's user-controlled portable backup file: inspect manifest/data-extraction rules and representative supported OS/transfer paths; confirm sensitive app records/drafts are excluded and portable backup still contains its approved complete coverage.
16. Large fonts, TalkBack, contrast, reduced motion, small-screen layout, and keyboard entry remain usable.
17. Schema migration preserves records and can recover safely from failure; test the current version-1 placeholder upgrade path only after technical validation is authorised.

### Proposed schema acceptance coverage — not executed

- Numeric/duration modes accept multiple distinct entries for the same habit/date and calculate totals from those entries; an explicit zero is retained as a record and differs from no record.
- Completion-only and effort-aware modes store their outcome in the day-outcome model, not as invented numeric values; enforce at most one current outcome per habit/date.
- Reject outcomes that do not match the effective tracking mode and reject contradictory numeric-entry/day-outcome combinations; a rejected write leaves prior records and totals unchanged.
- Resolve the effective-dated habit configuration for the selected local date before validating activity or deriving daily status; historical configuration changes must not reinterpret past entries.
- A scheduled day without qualifying activity is derived as missed only after the day has passed; no manual miss outcome is stored, and a journal memo alone never changes habit status.
- (D-194; planned, not executed) Verify the first Apply-today change creates today's version; a further same-day change replaces it only while no qualifying activity has been recorded today; once activity exists, today's version and activity remain intact and the UI offers Apply tomorrow; Cancel changes nothing; Apply tomorrow creates or updates tomorrow's not-yet-effective version; and earlier dates remain unchanged.
- (Delegated planning default; planned, not executed) Verify permanent habit deletion transactionally removes habit-owned records/configuration and habit-scoped delivery-ledger rows, preserves independent journal entries with their text and global reminders, and queues only opaque platform-notification IDs in the cancellation outbox. Inject cancellation failure and process interruption; confirm delivery workers suppress stale reminders when the habit no longer exists, the outbox retries on startup, and IDs are cleared only after confirmed cancellation.
- (D-193) Verify there is no explicit habit-day Skip action and no persisted skipped outcome in v1; missed status remains derived after the scheduled day passes without qualifying activity. Confirm rest/non-scheduled days remain neutral, paused periods are excluded, genuine backdated activity recalculates the selected date, and journal entries/memos do not change habit status.

### Restore-conflict matrix coverage — D-094–D-102 / AC-014, AC-022 (planned, not executed)

- Verify backup-envelope category counts distinguish live entities from unresolved conflict rows/payload snapshots and minimal conflict-session context; conflict evidence must not inflate live habit, activity, or journal-entry counts.

- Same stable ID plus identical validated content resolves to one logical record; same stable ID plus different content preserves both versions as an unresolved conflict rather than auto-selecting a winner.
- Journal conflict choices allow current, backup, or both as independent entries with distinct stable IDs; “both” never concatenates text or overwrites original dates/moods.
- Distinct numeric entry IDs remain distinct entries. Conflicting versions of the same numeric entry are never summed together; they remain available for explicit current/backup resolution. Do not permit two competing completion/effort outcomes for the same habit/date; where “both” cannot be represented safely, retain the unresolved conflict until the user chooses a supported option.
- Habit-definition/configuration conflicts are compared explicitly; any supported separate-habit option must preserve stable relationships and must not create two effective configuration versions for the same habit/date or rewrite historical interpretation.
- Conflicting preferences require explicit current/backup choice rather than duplicate setting keys. Conflicting comeback drafts remain separate unresolved drafts and do not become saved Journal entries without explicit Save.
- Corrupt, incomplete, or unsupported-version backups stop before changing live data. Safety-backup failure also stops restore; an injected failure/interruption at each Replace/Merge stage must leave a safe, truthful recovery state.
- Unresolved conflicts remain visible under Backup & Restore after restart and remain represented in a later portable backup until resolution. Clearing a conflict and applying the selected record must occur atomically; failed resolution preserves both snapshots.
- Verify the portable-backup round trip field by field: stable IDs and references, effective-dated configurations and weekday masks, explicit zero vs no record, idempotency keys, entry/date notes, saved journal fields/moods/optional habit links, unfinished drafts, approved preferences, and the minimum logical reminder-delivery ledger. Local-date keys remain unchanged; derived totals are rebuilt, not treated as source data.
- Verify backup envelope integrity/version/count metadata agrees with the payload; damaged, unsupported or incomplete files stop before live mutation. A failed write/verification never reports a valid backup or changes source data.
- Verify current and explicitly supported older format/schema versions use the correct converter; unknown/unsupported versions, missing required fields, unrecognised newer schemas, and failed integrity/count checks stop before live mutation. No required field or record may be silently dropped/coerced; a non-mappable record is retained in a reviewable recovery form or the operation stops safely.
- Verify a restore cannot silently run Auto Backup against a source-device destination or stale provider grant. When the destination is unavailable, scheduled writes stay paused until the user reselects/re-authorises it; no fallback destination is used.
- Verify habit/date-note collisions obey the unique one-note-per-habit/local-date constraint: preserve both snapshots for review, offer current/backup rather than an unsafe “both,” and retain the conflict if resolution fails.
- Verify an optional journal-to-habit link is preserved when its habit is restored/mapped, but becomes NULL (with journal text/date/mood intact) when the referenced habit is absent or cannot be safely mapped. If a separate-habit conflict is explicitly supported, remap every dependent entry/outcome/note/configuration relationship to the new habit ID; never duplicate only the parent while leaving children attached ambiguously.
- Verify exclusions: no credentials/passwords/purchase verification, provider access grants, platform notification IDs as active work, `notification_cancel_outbox`, cache-only totals, transient UI state, or device-specific safety-backup reference. A receiving install must reselect/re-authorise an unavailable destination rather than silently redirect.
- Verify unresolved conflicts retain both validated snapshots and a minimal parent-session context in a later backup. Import creates fresh local restore-session IDs and remaps each unresolved conflict's parent reference; never import a foreign session pointer as a live FK or treat an interrupted transaction as a completed portable backup. Conflict rows exist only while unresolved and do not require a status column. Parent context remains until all related conflicts resolve; a successful Replace/Merge ends in `COMPLETED_WITH_CONFLICTS` if any remain or `COMPLETED` if none remain; after the last child conflict is resolved the parent transitions to `COMPLETED` in the same transaction. An interrupted operation uses its recovery state rather than appearing completed; a failed resolution preserves the conflict snapshots and parent state.

These cases translate the approved D-094–D-102 conflict principles into tests against the proposed record model. The specific per-type mapping in doc 30 is still a planning proposal for owner review. No restore tests have been executed.



These are planned acceptance cases for the proposed schema, not test results. The complete schema still awaits owner approval, and technical validation requires separate explicit authorisation. D-190 approves stable UUID identity and nullable unique idempotency keys; D-193 settles the no-Skip-action policy; D-194 resolves repeated Apply-today behavior; D-195 resolves the weekday-mask encoding policy. None of these decisions means acceptance cases have been executed. Before finalising acceptance coverage, keep the delegated permanent-deletion reminder-cleanup default covered by the transaction, cancellation-failure, active-habit-guard, and startup-reconciliation cases above; the rule is documented, but neither the schema nor tests are implemented or verified.

### Retrospective journal prompt — proposed, not executed

- On the first eligible open for a local date, offer a prompt only when the most recent fully completed local date has no saved journal entry; existing habit records on that date do not suppress it.
- Verify an existing saved entry suppresses the prompt; multiple older empty dates create no backlog; display/dismissal/Not now/leave suppress repeat display for the same target date.
- Verify no OS notification is created, and no more than one retrospective prompt appears per app opening.
- Verify 14-/30-day comeback UI takes precedence without stacking a second prompt.
- Verify timezone travel does not move historical date keys or re-trigger the same target date; uncertain/corrupt clock data suppresses rather than fabricates the prompt date.
- Verify an existing draft for the target date is reused, save failure preserves it, and saving a retrospective entry does not change habit records or outcomes.
- This cadence is a delegated default under D-196, owner-overridable before Gate A; no tests have been executed.

### Undo and recovery — proposed, not executed

- Verify the Undo affordance appears only after the source mutation commits and remains available for eight seconds; the next mutation replaces the pending Undo.
- Undo a newly created numeric entry/day outcome and verify only that exact stable-ID record is removed and dependent calculations recalculate. Undo an edit and verify previous fields restore only when the entity has not changed again.
- Verify Undo never deletes another same-date entry, never uses value/timestamp matching, never reverses a distinct idempotent submission, and refuses an unsafe inverse after a newer conflicting edit.
- After timeout or process death, confirm committed records remain and the stale Undo control does not reappear; normal History correction remains available.
- Confirm journal-entry deletion, permanent habit deletion, delete-all and Replace/Merge restore do not show snackbar Undo; delete confirmation and verified restore safety-backup requirements remain effective.
- Inject a failed/uncertain original save and verify no Undo is offered as though the mutation succeeded; drafts and existing data remain recoverable.
- This is acceptance coverage for the delegated planning default under D-196; the owner may override it before Gate A. No implementation or test result is claimed.

### Reminder coordination and personalisation — planned, not executed

- Verify default quiet hours (21:00–08:00 local), user edits, suppression during quiet hours, and next-window revalidation.
- Verify at most three Dayseed OS notifications per local date and one ordinary reminder per habit/date; same-window events combine within 15 minutes where the destination preserves all essential information, otherwise stagger under the cap.
- Inject late workers, reboot, time-zone changes, clock jumps and OS permission denial; ensure each delivery re-checks current habit/goal state and a stable occurrence key prevents duplicates. Do not backfill a stale daily reminder after its local date passes; retain only the most recent weekly reflection after multi-week absence.
- Confirm a logged entry, dismissal, inactive/archived/deleted habit, met weekly goal, expired week or global opt-out suppresses the relevant delivery. A deleted habit's stale worker must never post.
- For personalisation, verify no change before the minimum 14-day evidence threshold; minimum evidence is seven qualifying timestamps on at least five dates with at least 70% in a two-hour band; calculate candidate as band median plus 30 minutes rounded up to a 15-minute boundary; manually selected times never change automatically; a 28-day validation window prevents timing churn; insufficient evidence reverts to the prior system-managed time; confirm no journal text/mood/network access.
- For partial tips, verify at most three per seven-day sequence, 24-hour minimum spacing, one/day maximum, goal-met suppression, and dismissal/opt-out. Confirm in-app feedback remains possible when Android notification permission is denied.
- These cases test the delegated planning defaults only after the technical-validation stage is authorised; they do not mean the behaviour is implemented or tested. The owner may override the defaults before Gate A.

## Date/history acceptance coverage (D-187)

- Travel across time zones and confirm existing habit/journal local-date keys do not change; new entries default to the current device-local date unless another date is explicitly selected.
- Change the device clock forward/backward and simulate uncertain time; confirm no historical rewrite, fabricated elapsed time, duplicate logical record, or duplicate reminder/comeback delivery.
- Change a habit target/schedule and confirm changes apply prospectively while past records retain their historical interpretation.
- Archive a habit and confirm its history remains accessible; perform any separately authorised permanent deletion flow and confirm independent journal entries remain intact.
- Backdate and correct a specific entry; confirm only the selected record and dependent calculations change.

- Confirm no screen or data action allows users to manually mark a scheduled day as missed.
- For a scheduled day that has not passed, verify absent activity remains unrecorded; after the day passes with no qualifying activity, verify the missed status is derived from the applicable schedule and source records.
- Add a journal memo to a missed day and verify its status and habit totals do not change; memo text is never treated as activity evidence.
- Add genuine quantity/duration activity with explicit past-date selection and verify the historical day's result and dependent summaries recalculate using the configuration effective on that date, while unrelated dates remain unchanged.
- Verify completion-only and effort-aware habits use valid backdated outcomes for their mode; journal entries alone never create these outcomes.

### Proposed data-model acceptance coverage — pending owner approval

- Validate each schema constraint and foreign key, including a habit with many entries on one date and many journal entries on one date.
- **Approved policy, planned test only:** effective-dated configuration chooses the correct target, schedule, tracking mode, and other applicable settings for dates before, on, and after a change; editing a future version must not rewrite past records. This owner decision does not mean the schema or tests are implemented or executed.
- **D-194 same-day configuration edit (planned, not executed):** the first Apply-today change creates the current local-date version; a second Apply-today edit replaces that version only while no qualifying habit activity has been recorded on that date. Once qualifying activity exists, preserve today's version and offer Apply tomorrow. Cancel changes nothing. Apply tomorrow creates or revises only tomorrow's not-yet-effective version. Past versions and their historical interpretation remain unchanged.
- Verify same-key retries create/count once, while a new idempotency key preserves a distinct intentional entry even when its value and timestamp are similar.
- Verify entity IDs remain stable after edits and across backup/restore; retrying the same logical submission reuses its idempotency key and cannot create a duplicate, while a distinct intentional submission uses a new key and remains a separate entry.
- (D-191) Verify `DAILY` schedules every local day; `SELECTED_WEEKDAYS` requires and obeys its weekday mask; and `FLEXIBLE_WEEKLY_TARGET` has no required weekdays, with preferred weekdays treated only as guidance.
- (D-195; planned, not executed) Verify Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64; combining weekday selections yields the expected mask; valid selected-weekday masks are non-empty and contain only the seven defined bits; zero or unknown bits are rejected where invalid; `DAILY` and `FLEXIBLE_WEEKLY_TARGET` do not require `scheduled_weekdays_mask`; and optional preferred weekdays for a flexible weekly target never restrict qualifying progress. Confirm mask meanings remain unchanged when locale and saved week-start preference change.
- Verify `target_period` is `DAY` for both `DAILY` and `SELECTED_WEEKDAYS` schedules and `WEEK` for `FLEXIBLE_WEEKLY_TARGET`; reject invalid schedule-kind/period/mask combinations.
- Verify weekly boundaries use the saved week-start preference consistently across targets, summaries, calendar and reminders; locale is used only to choose the initial default.
- Verify numeric/duration weekly totals sum compatible units, while completion-only/effort-aware weekly goals count qualifying completed dates without fabricating numeric values. Schedule edits take effect on the selected effective date and do not rewrite past configuration or records.
- Verify a non-null idempotency key cannot be assigned to two logical submissions, while records with no retry key remain valid where allowed.
- Verify the global activity-submission idempotency rule across both `habit_entries` and `habit_day_outcomes`: reject the same non-null key within either table and across the two tables; allow NULL where the contract permits it; preserve both versions as a reviewable restore conflict when a backup introduces a cross-table collision; and verify transaction failure leaves neither a duplicate activity nor an orphaned key reservation. These are planned schema cases, not executed tests. A cross-table collision uses one canonical `ACTIVITY_IDEMPOTENCY_KEY` conflict keyed by the collided key. If the same stable-ID pair also carries the same key, retain one record-identity conflict and validate the key constraint during resolution instead of creating two conflict rows for the same candidate versions.
- Verify numeric zero, no row, derived missed status, completion-only completion, and effort-aware selection remain distinct; reject contradictory or mode-incompatible states. A missed status is derived after a scheduled day passes without qualifying activity, genuine backdated activity can recalculate that date, and journal memos alone never change status.
- Verify archive preserves accessible history; permanent habit deletion removes only the explicitly covered habit-owned data and never independent journal entries/drafts.
- Verify all multi-row operations roll back completely on injected failure; success is never shown before commit.
- Verify migration/backup compatibility and restore failure recovery only after the owner approves the data model and the technical-validation gate is explicitly authorised. These are planned tests, not executed results.
- Verify restore-session lifecycle: invalid input or failed safety-backup verification never starts a live restore; after preflight, the durable `IN_PROGRESS` session is committed before the live-data transaction; successful live changes, conflict rows, and the final `COMPLETED`/`COMPLETED_WITH_CONFLICTS` status commit atomically. Inject failure and process interruption before/during that transaction; confirm live data and conflict rows roll back completely, an observed rollback becomes `FAILED_SAFE`, and a durable `IN_PROGRESS` session discovered after restart becomes `RECOVERY_REQUIRED` only after verifying that the live transaction did not commit. No success message or automatic duplicate restore is allowed. Resolve the last conflict and verify the parent becomes `COMPLETED` in the same transaction.
- Verify notification-cancellation outbox idempotency: enqueuing the same non-null opaque platform tag/ID twice creates one work item; invalid/missing tag or notification ID is rejected; failed/interrupted cancellation remains retryable; success removes the item; and a stale worker never posts for a deleted or inactive habit. Do not export these platform-specific cancellation rows in portable backup.

### Mood Insights synthetic fixture (AC-009) — planned, not executed

Use synthetic journal entries only. For this fixture, set week-start to Monday and interpret dates as saved local-date keys.

| ID | Local date | Primary mood | Secondary mood |
|---|---|---|---|
| J1 | 2026-10-26 | Calm | Hopeful |
| J2 | 2026-10-26 | Focused | None |
| J3 | 2026-10-27 | Calm | Tired |
| J4 | 2026-10-25 | Sad | Hopeful |
| J5 | 2026-10-19 | Calm | None |
| J6 | 2026-10-01 | Joyful | None |
| J7 | 2026-09-30 | Tired | Calm |
| J8 | 2026-10-02 | None | None |
| J9 | 2026-10-27 | None | None |
| J10 | 2026-10-22 | Focused | Calm |
| J11 | 2026-11-01 | Hopeful | None |

Expected results when week-start is Monday:
- **This week (2026-10-26–2026-11-01):** Calm 2 (J1,J3), Focused 1 (J2), Hopeful 1 (J11). J9 has no mood; it contributes nothing. J3's secondary Tired does not count.
- **Previous week (2026-10-19–2026-10-25):** Calm 1 (J5), Sad 1 (J4), Focused 1 (J10). Verify week comparison uses the immediately preceding equivalent week, with primary moods only.
- **This month (2026-10-01–2026-10-31):** Calm 3 (J1,J3,J5), Focused 2 (J2,J10), Sad 1 (J4), Joyful 1 (J6). J11 (Nov 1) is outside the month despite being in the selected week; J7 (Sep 30) is outside the month even though Calm is its secondary mood.
- Tapping **Calm** in This week opens J1 and J3 only, in the selected period. It must not include J10 because Calm is secondary there; it must not include J7 because that entry is outside the period and Calm is secondary.
- Entries J8/J9 remain searchable and visible without mood tags; they do not fabricate a mood count. A period with no primary-mood entries uses the neutral empty state. One or few tagged entries show factual counts without claiming a trend.
- Repeat the boundary assertions with Sunday as the configured week-start; only period membership changes, not stored dates or the primary-versus-secondary rule.
- Verify correction/deletion of J1 updates distribution and selected-mood results from source data without altering J2/J3 or unrelated entries. No real journal text is used in fixtures.

### Measured progress-band boundary fixtures — AC-002 (planned, not executed)

Classify with the exact actual/target ratio before rounding any displayed percentage. Fixtures below verify labels and stored values; they are a test plan, not test results.

| Positive integer target | Actual value(s) | Expected label |
|---:|---|---|
| 100 | 1, 24 | Tiny |
| 100 | 25, 49 | Small |
| 100 | 50, 74 | Halfway |
| 100 | 75, 99 | Nearly there |
| 100 | 100 | Goal met |
| 100 | 101, 150 | Extra |
| 100 | 151 | Huge |
| 10 | 1, 2 | Tiny |
| 10 | 3, 4 | Small |
| 10 | 5, 6, 7 | Halfway |
| 10 | 8, 9 | Nearly there |
| 10 | 10 | Goal met |
| 10 | 11, 15 | Extra |
| 10 | 16 | Huge |
| 1 | 1 | Goal met; intermediate bands are unreachable |
| 1 | 2 | Huge (200%); do not misclassify from rounded display |

Also verify: actual zero is retained as an explicit saved record but does not receive the positive-progress Tiny label; no record remains a separate state; actual amounts are never capped or changed; rounded percentage display cannot change the classification; units/targets validate as positive whole-number targets while recorded numeric values may be zero. These cases must eventually be implemented as deterministic acceptance tests after the planning gate and technical-validation authorisation; they have not been run.

## 4. Test data and privacy
Use synthetic test data. Do not use real journal entries in fixtures, screenshots, logs, or issue reports. Scrub sensitive values from crash reports and automated test output. Initial v1 excludes accounts, online backup, and cross-device sync; premium future sync must not be added under this test plan without a separate approved privacy/security and product gate.

## 5. Performance and reliability
Define measurable budgets only after selecting the stack and measuring a baseline on representative devices. Test launch, Today rendering, history navigation, database growth, and longer use. Include lower-end Android hardware and constrained storage/network conditions.

## Proposed schema acceptance coverage — habit notes (D-192; not executed)

- Allow an optional note on an individual quantity/duration activity entry; editing or deleting that note must not alter the entry's value, identity, or totals.
- Allow at most one optional habit/date context note per habit and local date, independent of the number of activity entries; create/edit/delete it without changing activity entries or calculated habit status.
- Allow both note levels to coexist on the same date and display them distinctly; neither is the general journal, and journal content must not affect habit outcomes.
- Verify note ownership and date association across archive/restore, backup/restore, and any supported habit deletion flow before implementation is authorised.

## 6. Release gate
A release candidate must have:
- all required automated tests passing;
- documented results for manual device checks;
- no known data-loss or privacy-critical defect;
- completed accessibility and privacy checklists;
- reproducible release build instructions;
- reviewed migration and rollback/recovery strategy;
- known issues documented with severity and user impact.

A green test suite does not replace manual privacy, accessibility, and usability review.