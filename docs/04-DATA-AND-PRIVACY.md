# Data and Privacy

**Status:** Privacy wording accepted by owner as a draft baseline on 2026-10-28; legal, SDK and platform behaviour must still be verified before release.

## 1. Privacy promise
Dayseed's initial v1 is local-first and requires no account. Habit names, daily outcomes, and journal text remain in local app storage during ordinary use; the user may deliberately export journal content or create a local backup. Online backup and cross-device sync are premium future capabilities, not included in initial v1, and cannot be added without a separate explicit privacy/security and product approval. Do not describe data as “never leaves the device” until Android backup, crash reporting, analytics, advertising SDKs, and OS sharing behaviour have been inspected and configured.

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
**Approved decision D-186:** Exclude sensitive Dayseed app data from Android platform Auto Backup/device-to-device transfer by default in initial v1. Configure Android backup/data-extraction rules to exclude private habit definitions and records, tracking history, saved journal entries and moods, and unsaved reflections. This is separate from Dayseed's user-controlled portable backup/restore, which remains available for migration, and from opt-in file-based Auto Backup to a user-selected destination. Verify actual manifest/rules and supported Android-version/device-transfer behaviour before claiming exclusion is effective. Assess database encryption separately; do not claim encryption at rest unless implemented and verified. Explain plainly that users should create a Dayseed portable backup before changing phones; OS-managed migration may not carry Dayseed private data.

## 5. Export and user-controlled local backup
Define a human-readable journal export and a versioned, integrity-checked portable backup format before release. Export/backup must be user-initiated or follow the separately approved local Auto Backup settings, explain what is included, avoid accidental public sharing, and use the Android system file picker or share sheet where appropriate. Journal export must be clearly labelled as sensitive. A file picker may expose provider-backed destinations (including providers that sync files online); disclose that the chosen provider may upload/sync the saved file. This is user-directed file handling, not a Dayseed-managed cloud account, online backup service, or cross-device sync feature.

## 6. Deletion
Provide separate, understandable options for deleting a journal entry, deleting a habit and its associated history, and deleting all app data. Confirm destructive actions. After deletion, verify records are removed from the active database and any derived caches. Explain that copies already exported or included in OS/device backups may require separate deletion.

## 7. Analytics and crash reporting
Default proposal: no third-party analytics in the first release until a concrete need is demonstrated. If crash reporting is later adopted, review SDK collection, data residency, retention, identifiers, opt-out, and log scrubbing. Never attach database contents or journal text to crash reports. Do not use journal text to personalise ads.

## 8. Future network features
Accounts, online backup, cross-device sync, AI analysis, and server-side processing are out of initial v1 scope. Online backup and cross-device sync may be considered as premium capabilities in a later release only after a separate decision, threat model, data-flow diagram, clear opt-in/consent UX, retention/deletion policy, conflict/recovery design, provider-terms review, and monetisation approval. Premium status does not itself authorise collection or transfer of personal data.

## Proposed user-facing privacy copy — draft for owner review

The following is candidate wording for the v1 privacy and Backup & Restore screens. It must be checked against the actual manifest, data-extraction rules, dependencies, export behavior, and network/data-flow evidence before release. It is not a verified runtime claim.

- **Core use:** “Dayseed’s core habits, journal, history, and insights work offline and do not require an account. Dayseed does not provide its own online backup or cross-device sync service in this version.”
- **Moving to a new phone:** “Create and verify a Dayseed backup file, then restore it on the new phone. Android’s automatic device backup/transfer is not the intended recovery path for Dayseed’s private records in this version.”
- **Saving a backup or export:** “A Dayseed backup or journal export is a file that may contain private information. Choose a destination you trust. If the selected file provider connects to an online service, that provider may upload or sync the file.”
- **Deleting in Dayseed:** “This removes the selected data from Dayseed. It does not remove copies already saved in backup or export files; those copies must be managed separately.”

Owner accepted the candidate copy above as a **draft baseline** on 2026-10-28 (O-10). This is not release approval. Do not claim “never leaves this device,” “encrypted at rest,” or “Android backup is disabled” unless the relevant behaviour is verified. The platform-backup exclusion is an approved policy decision (D-186), but it remains a release-verification item. If ads are subsequently approved, revise privacy and Data safety disclosures to reflect the actual ad SDK's collection/use/sharing; see [Monetisation and Ads Evaluation](33-MONETISATION-AND-ADS-EVALUATION.md).

## 9. Privacy acceptance checklist
- [ ] Data inventory and data-flow diagram approved.
- [ ] Permissions justified and explained in context.
- [x] Owner selected exclusion of sensitive Dayseed data from Android platform Auto Backup/device transfer (D-186); verify the actual manifest/data-extraction rules and device-transfer behaviour before release.
- [ ] Test Android platform-backup exclusion separately from Dayseed portable backup/restore on supported Android versions and representative device-transfer paths. Verify sensitive private data is absent from OS-managed backup while user-controlled portable backup remains complete and restorable.
- [ ] Confirm no initial-v1 Dayseed-managed account, cloud-backup, or sync service is present; privacy wording matches the actual SDK and platform configuration.
- [ ] Explain that a user-selected Android file provider may upload/sync an exported or backed-up file, without implying Dayseed itself provides online sync.
- [ ] Export includes only the selected scope and warns about sensitive content.
- [ ] Delete flows verified against database, caches, and UI.
- [ ] Logs contain no journal text or unnecessary personal data.
- [ ] Network inspection confirms core features make no required calls.
- [ ] Privacy wording matches observed behaviour and platform settings.
- [ ] Release privacy disclosures match actual SDKs and behaviour.

## 10. Privacy and offline data-flow reconciliation (planning only)

Keep the promise, data paths, and evidence status separate:

| Path or claim | Initial-v1 planning rule | Evidence still required before release claims |
|---|---|---|
| Core use offline | Habit tracking, saved journaling, history, insights and comeback flows must not require network access or an account. | Inspect the actual dependency/permission configuration and observe network behaviour for core flows, including first launch and failure states. |
| Dayseed-managed online services | Accounts, cloud backup, cross-device sync, AI processing and server-side personal-data processing are excluded from initial v1. | Inspect the actual source, dependencies and runtime traffic; any later addition needs a separate explicit approval gate. |
| Android Auto Backup/device transfer | D-186 policy is to exclude sensitive app data and unsaved reflections by default. | Verify manifest and data-extraction rules plus supported OS/device-transfer paths. Policy approval alone does not prove exclusion works. |
| Dayseed portable backup/restore | User-initiated portable backup/restore remains available for migration, with integrity checks and documented contents. | Verify actual exported fields, integrity checks, failure recovery and restore behaviour after separate technical-validation authorisation. |
| Readable journal export / file provider | Export is user-directed and sensitive; a chosen provider may upload or sync the file under its own terms. | Verify scope, destination/share-sheet behaviour and user-facing warnings. Do not describe provider storage as Dayseed cloud sync. |
| Diagnostics, analytics and SDKs | No third-party analytics or advertising SDK is approved for initial v1; crash reporting is not implicitly approved. | Inspect actual dependencies and data flows. If crash reporting or another SDK is proposed later, assess collected fields, identifiers, retention, opt-out and redaction before a separate approval. |
| Deletion | Explain Dayseed-controlled deletion separately from exported, provider-held, OS-retained or other-app copies. | Verify the exact local-data inventory, database/cache deletion and copy-management guidance; never claim external copies were deleted without evidence. |
| Encryption | No encryption-at-rest claim is approved by wording alone. | Inspect and verify the actual storage/encryption design before making a claim. |

Do not collapse these into a single blanket promise such as “everything stays on this device” or “Dayseed never sends data.” The approved direction is local-first/offline core use; the actual network, platform-backup, SDK, file-provider and deletion behaviour is still unverified. This reconciliation records existing decisions and evidence gaps only. It does not create a new product decision, authorise technical inspection/testing, or open the planning gate.

## Date and history integrity (D-187)

- Store the user-intended local calendar date separately from creation/update instants. Travel or a timezone change must not silently move existing habit records or journal entries to another date.
- Use the current device-local date as the default for a new record unless the user explicitly selects another date. Preserve the selected date on save and show it clearly for backdated entries.
- Manual clock changes and uncertain timestamps must not silently rewrite records, fabricate elapsed time, or duplicate reminder/comeback events. Apply conservative eligibility until time confidence is sufficient.
- Target and schedule changes apply prospectively; preserve historical records and the target/schedule interpretation applicable to each past date. Explicit corrections affect only the selected record and dependent calculations.
- Archive habits by default to remove them from active use without erasing accessible history. Permanent deletion is a separate explicit action. Habit lifecycle changes must not delete independent journal entries.
- This is an approved product policy baseline, not evidence of implementation. Technical verification of schema, date boundaries, migrations, deletion, export, and backup behaviour remains gated by explicit owner approval of technical validation.
