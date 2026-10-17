# Data and Privacy

**Status:** Proposed privacy baseline; legal and platform details require review before release.

## 1. Privacy promise
Dayseed v1 should be local-first. Habit names, daily outcomes, and journal text should remain on the device unless the user explicitly initiates an export or a future separately approved feature changes this model. Do not describe data as “never leaves the device” until Android backup, crash reporting, analytics, advertising SDKs, and OS sharing behaviour have been inspected and configured.

## 2. Data minimisation
Collect only what is required for the app to work. Do not require account details, contacts, precise location, microphone, camera, or unrelated permissions for core functionality. Reminders may require notification permission. Explain any additional permission in context.

## 3. Data categories and handling
- Habit definitions: local database.
- Daily records: local database.
- Journal text: local database; treat as especially sensitive personal content.
- Preferences: local storage.
- Diagnostic events: only if eventually approved, minimise them and never include journal text or habit names.
- Advertising data: keep separate from journal and habit records; do not use personal records for ad targeting.

## 4. Device backup and encryption
Investigate Android Auto Backup and device-to-device transfer behaviour for the selected platform and storage mechanism. Decide whether private data should be included in backups, excluded, or protected through a documented design. Assess whether database encryption is necessary and feasible; do not claim encryption at rest unless implemented and verified. Document threat model, device compromise limitations, and recovery trade-offs in plain language.

## 5. Export
Define a human-readable and machine-readable export format before release. Export must be user-initiated, explain what is included, avoid accidental public sharing, and use the Android share sheet or a save destination chosen by the user. Consider JSON for complete restoration and CSV for tabular records, with journal export clearly labelled as sensitive.

## 6. Deletion
Provide separate, understandable options for deleting a journal entry, deleting a habit and its associated history, and deleting all app data. Confirm destructive actions. After deletion, verify records are removed from the active database and any derived caches. Explain that copies already exported or included in OS/device backups may require separate deletion.

## 7. Analytics and crash reporting
Default proposal: no third-party analytics in the first release until a concrete need is demonstrated. If crash reporting is later adopted, review SDK collection, data residency, retention, identifiers, opt-out, and log scrubbing. Never attach database contents or journal text to crash reports. Do not use journal text to personalise ads.

## 8. Future network features
Cloud sync, accounts, AI analysis, and server-side processing are out of v1 scope. Each requires a separate decision, threat model, data-flow diagram, consent UX, retention/deletion policy, and review of provider terms before implementation.

## 9. Privacy acceptance checklist
- [ ] Data inventory and data-flow diagram approved.
- [ ] Permissions justified and explained in context.
- [ ] Backup inclusion/exclusion tested on target Android versions.
- [ ] Export includes only the selected scope and warns about sensitive content.
- [ ] Delete flows verified against database, caches, and UI.
- [ ] Logs contain no journal text or unnecessary personal data.
- [ ] Network inspection confirms core features make no required calls.
- [ ] Privacy wording matches observed behaviour and platform settings.
- [ ] Release privacy disclosures match actual SDKs and behaviour.