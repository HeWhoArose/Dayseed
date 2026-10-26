# Testing and Quality

**Status:** Required test strategy for the approved native Kotlin + Jetpack Compose direction; implementation and validation remain unauthorised until the planning gate is approved.

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
5. Explicit zero, completion-only state, effort-aware state, explicit miss, and no record remain distinguishable.
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

### Date/history acceptance coverage (D-187)

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
- Confirm effective-dated configuration chooses the right target/schedule for dates before, on, and after a change; editing a future version must not rewrite past records.
- Verify same-key retries create/count once, while a new idempotency key preserves a distinct intentional entry even when its value and timestamp are similar.
- Verify numeric zero, no row, explicit miss, completion-only completion, and effort-aware selection remain distinct; reject contradictory or mode-incompatible states.
- Verify archive preserves accessible history; permanent habit deletion removes only the explicitly covered habit-owned data and never independent journal entries/drafts.
- Verify all multi-row operations roll back completely on injected failure; success is never shown before commit.
- Verify migration/backup compatibility and restore failure recovery only after the owner approves the data model and the technical-validation gate is explicitly authorised. These are planned tests, not executed results.

## 4. Test data and privacy
Use synthetic test data. Do not use real journal entries in fixtures, screenshots, logs, or issue reports. Scrub sensitive values from crash reports and automated test output. Initial v1 excludes accounts, online backup, and cross-device sync; premium future sync must not be added under this test plan without a separate approved privacy/security and product gate.

## 5. Performance and reliability
Define measurable budgets only after selecting the stack and measuring a baseline on representative devices. Test launch, Today rendering, history navigation, database growth, and longer use. Include lower-end Android hardware and constrained storage/network conditions.

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