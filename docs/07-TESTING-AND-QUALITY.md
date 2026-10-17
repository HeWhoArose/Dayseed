# Testing and Quality

**Status:** Required test strategy; exact tooling is pending stack approval.

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
1. Tiny, Usual, Extra, and completion-only semantics remain correct.
2. A journal entry can be saved independently of habit completion.
3. A past record edit updates all dependent summaries.
4. Archive preserves history; deletion follows the approved deletion policy.
5. Repeated taps or retries do not create duplicate records.
6. App process termination does not lose acknowledged writes.
7. Offline operation does not show endless loading or require sign-in.
8. Comeback thresholds and dismissal work at boundary dates.
9. Missing or invalid clock data does not generate shaming or fabricated messages.
10. Notification denial, disabled notifications, and OS restrictions do not break core tasks.
11. Large fonts, TalkBack, contrast, and reduced motion remain usable.
12. Schema migration preserves records and can recover safely from failure.

## 4. Test data and privacy
Use synthetic test data. Do not use real journal entries in fixtures, screenshots, logs, or issue reports. Scrub sensitive values from crash reports and automated test output.

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