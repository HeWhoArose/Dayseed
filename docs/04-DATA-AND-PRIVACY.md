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

## 3A. Data lifecycle inventory (planning baseline; implementation unverified)

The current written model distinguishes these data classes and destinations. This is a planning inventory, not proof that each item exists in the current app or that the runtime includes/deletes it exactly as described.

| Data class | Ordinary Dayseed storage / lifecycle | Readable journal export | Portable Dayseed backup | Android platform Auto Backup/device transfer | Deletion/privacy evidence still required |
|---|---|---|---|---|---|
| Habit definitions, effective schedules and preferences | Local app data; archive preserves history, while permanent deletion is separate | Excluded | Included categories must be listed in the pre-creation summary; approved backup scope includes habit definitions and relevant preferences | D-186 policy: exclude sensitive data by default | Verify exact fields and delete scope; confirm manifest/data-extraction behaviour |
| Numeric/duration entries, daily outcomes, notes and derived history | Local app data; derived totals/status should be rebuilt from source records | Excluded | Approved scope includes all tracking history; cache-only totals are excluded | D-186 policy: exclude sensitive data by default | Verify source rows, derived caches and deletion/restore behaviour |
| Saved journal entries, dates, titles and recorded mood tags | Local app data; independent journal entries survive habit deletion | Included in readable export, within the selected export scope | Included | D-186 policy: exclude sensitive data by default | Verify exact export fields, deletion and backup configuration |
| Unsaved comeback/journal drafts | Local app data; not a saved journal entry; kept until explicitly saved, deleted or discarded under the relevant draft rules | Excluded | Approved portable-backup direction includes unresolved drafts where feasible; the pre-creation summary must disclose inclusion | D-186/D-067 policy: exclude drafts by default | Verify draft-specific retention, save/delete/discard behaviour and actual backup exclusions |
| App settings and reminder preferences | Local app data | Excluded | Approved portable-backup scope includes relevant preferences; device-specific permissions/provider grants are not portable | D-186 policy: exclude sensitive data by default where applicable | Verify which settings transfer and require destination/provider re-authorisation |
| Restore conflicts and conflict-session context | Protected local recovery data until supported resolution; payloads must not enter logs/notifications | Excluded | Unresolved conflicts and minimal context for completed conflict-bearing sessions are included under the proposed mapping | D-186 policy applies to sensitive local data | Verify implementation, payload retention, export mapping and deletion scope after separate authorisation |
| OS notification identifiers, cancellation outbox, transient UI state, caches and provider grants | Device-specific/transient data; rebuild or clear only under defined lifecycle rules | Excluded | Excluded from portable backup where documented as device-specific, transient, cache-only or security-sensitive | Must not be assumed excluded without checking platform rules | Verify exact local cleanup and ensure portable backups do not transfer device-specific work or access grants |
| Exported files and user-selected provider copies | Outside Dayseed's controlled database after creation; provider may sync/upload them | The export itself is a separate file | The backup itself is a separate file | Not the same as Android's app-data backup | Explain that Dayseed deletion cannot promise to delete copies managed elsewhere; provide user-controlled removal guidance |

**Important distinctions:** readable journal export is not a full backup; portable backup may include drafts and unresolved conflict material that readable export omits; Android platform backup/device transfer is a separate OS-controlled path; and a user-selected file provider may store or sync a file outside Dayseed. The exact backup mapping remains subject to the proposed schema plan, and no row in this table is a runtime verification claim.

## 4. Device backup and encryption
**Approved decision D-186:** Exclude sensitive Dayseed app data from Android platform Auto Backup/device-to-device transfer by default in initial v1. Configure Android backup/data-extraction rules to exclude private habit definitions and records, tracking history, saved journal entries and moods, and unsaved reflections. This is separate from Dayseed's user-controlled portable backup/restore, which remains available for migration, and from opt-in file-based Auto Backup to a user-selected destination. Verify actual manifest/rules and supported Android-version/device-transfer behaviour before claiming exclusion is effective. Assess database encryption separately; do not claim encryption at rest unless implemented and verified. Explain plainly that users should create a Dayseed portable backup before changing phones; OS-managed migration may not carry Dayseed private data.

## 5. Export and user-controlled local backup
Define a human-readable journal export and a versioned, integrity-checked portable backup format before release. Export/backup must be user-initiated or follow the separately approved local Auto Backup settings, explain what is included, avoid accidental public sharing, and use the Android system file picker or share sheet where appropriate. Journal export must be clearly labelled as sensitive. A file picker may expose provider-backed destinations (including providers that sync files online); disclose that the chosen provider may upload/sync the saved file. This is user-directed file handling, not a Dayseed-managed cloud account, online backup service, or cross-device sync feature.

## 6. Deletion
Provide separate, understandable options for deleting a journal entry, deleting a habit and its associated history, and deleting all app data. Confirm destructive actions. After deletion, verify records are removed from the active database and any derived caches. Explain that copies already exported or included in OS/device backups may require separate deletion.

## 6A. Delete-all scope inventory (D-203; planning only)

Before final confirmation, the delete-all screen must name the Dayseed-controlled local data covered by the operation in plain language. The following is the planning inventory to reconcile against the actual schema and runtime before release; it is not proof that every category exists or that deletion currently removes it:

- Habit definitions and effective configuration/schedule history.
- Tracking history, including numeric/duration entries, day outcomes, notes and derived history/cache data.
- Saved journal entries and their titles, dates, mood tags and optional links.
- Unsaved journal/comeback drafts and any persisted draft-save context.
- App preferences and reminder preferences stored by Dayseed.
- Local restore sessions, unresolved conflict payloads and related recovery context.
- Dayseed-managed transient/cache state and pending notification-cancellation work, where present.

The confirmation must separately explain that exported files, portable-backup files, provider-held/synced copies, OS-retained copies and copies saved by other apps may remain. Offer practical removal guidance for copies the user controls. Do not claim those external copies were deleted. Device-level notifications or other OS-managed state may require separate cancellation/cleanup; do not promise they have disappeared until the relevant behaviour is verified. Exact inventory, transactional scope, cache cleanup, notification cleanup and runtime deletion effects remain unverified and must be reconciled with the actual implementation before release.

This section records the existing D-203 policy for planning consistency only. It does not approve implementation, technical inspection/testing, or open the planning gate.

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
