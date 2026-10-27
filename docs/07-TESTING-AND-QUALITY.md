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
- (Delegated planning default; planned, not executed) Verify permanent habit deletion transactionally removes habit-owned records/configuration and habit-scoped delivery-ledger rows, cancels pending/posted habit-specific OS notifications, preserves independent journal entries and global reminders, and is idempotent. Inject notification-cancellation failure and confirm a delivery worker's active-habit guard suppresses stale notifications and cleanup can retry/reconcile on startup.
- (D-193) Verify there is no explicit habit-day Skip action and no persisted skipped outcome in v1; missed status remains derived after the scheduled day passes without qualifying activity. Confirm rest/non-scheduled days remain neutral, paused periods are excluded, genuine backdated activity recalculates the selected date, and journal entries/memos do not change habit status.

These are planned acceptance cases for the proposed schema, not test results. The complete schema still awaits owner approval, and technical validation requires separate explicit authorisation. D-190 approves stable UUID identity and nullable unique idempotency keys; D-193 settles the no-Skip-action policy; D-194 resolves repeated Apply-today behavior; D-195 resolves the weekday-mask encoding policy. None of these decisions means acceptance cases have been executed. Before finalising acceptance coverage, keep the delegated permanent-deletion reminder-cleanup default covered by the transaction, cancellation-failure, active-habit-guard, and startup-reconciliation cases above; the rule is documented, but neither the schema nor tests are implemented or verified.

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
- Verify same-key retries create/count once, while a new idempotency key preserves a distinct intentional entry even when its value and timestamp are similar.
- Verify entity IDs remain stable after edits and across backup/restore; retrying the same logical submission reuses its idempotency key and cannot create a duplicate, while a distinct intentional submission uses a new key and remains a separate entry.
- (D-191) Verify `DAILY` schedules every local day; `SELECTED_WEEKDAYS` requires and obeys its weekday mask; and `FLEXIBLE_WEEKLY_TARGET` has no required weekdays, with preferred weekdays treated only as guidance.
- (D-195; planned, not executed) Verify Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64; combining weekday selections yields the expected mask; valid selected-weekday masks are non-empty and contain only the seven defined bits; zero or unknown bits are rejected where invalid; `DAILY` and `FLEXIBLE_WEEKLY_TARGET` do not require `scheduled_weekdays_mask`; and optional preferred weekdays for a flexible weekly target never restrict qualifying progress. Confirm mask meanings remain unchanged when locale and saved week-start preference change.
- Verify `target_period` is `DAY` for both `DAILY` and `SELECTED_WEEKDAYS` schedules and `WEEK` for `FLEXIBLE_WEEKLY_TARGET`; reject invalid schedule-kind/period/mask combinations.
- Verify weekly boundaries use the saved week-start preference consistently across targets, summaries, calendar and reminders; locale is used only to choose the initial default.
- Verify numeric/duration weekly totals sum compatible units, while completion-only/effort-aware weekly goals count qualifying completed dates without fabricating numeric values. Schedule edits take effect on the selected effective date and do not rewrite past configuration or records.
- Verify a non-null idempotency key cannot be assigned to two logical submissions, while records with no retry key remain valid where allowed.
- Verify numeric zero, no row, derived missed status, completion-only completion, and effort-aware selection remain distinct; reject contradictory or mode-incompatible states. A missed status is derived after a scheduled day passes without qualifying activity, genuine backdated activity can recalculate that date, and journal memos alone never change status.
- Verify archive preserves accessible history; permanent habit deletion removes only the explicitly covered habit-owned data and never independent journal entries/drafts.
- Verify all multi-row operations roll back completely on injected failure; success is never shown before commit.
- Verify migration/backup compatibility and restore failure recovery only after the owner approves the data model and the technical-validation gate is explicitly authorised. These are planned tests, not executed results.

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