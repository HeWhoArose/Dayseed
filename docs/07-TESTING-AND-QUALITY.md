# Testing and Quality

**Status:** Required test strategy for the approved native Kotlin + Jetpack Compose direction; implementation and validation remain unauthorised until the planning gate is approved.

## 1. Quality goals
Protect personal data, prevent loss of history, keep core flows usable offline, ensure dates behave predictably, and verify that the app remains supportive after interruptions.

## 2. Test layers
- **Domain unit tests:** effort levels, schedules, outcomes, summary calculations, comeback bands, date boundaries.
- **Persistence tests:** inserts, updates, uniqueness constraints, transactions, migrations, deletion, export/import if supported.
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
15. Android platform backup/device transfer is tested and documented separately from Dayseed's user-controlled portable backup file.
16. Large fonts, TalkBack, contrast, reduced motion, small-screen layout, and keyboard entry remain usable.
17. Schema migration preserves records and can recover safely from failure; test the current version-1 placeholder upgrade path only after technical validation is authorised.

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