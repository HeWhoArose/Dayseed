# Comeback Experience

**Status:** Approved direction for inactivity-based in-app welcome-back experiences; exact eligibility and copy remain to be specified.

## Two-week comeback

- Define the 14-day inactivity trigger as no app opening for 14 consecutive days. Any opening of Dayseed resets the timer, including opening only to read the journal, browse history, or check insights. After the 14-day threshold is reached, show the gentle comeback message only when the user next opens Dayseed.
- Do not send a separate notification while the user is away for this two-week comeback trigger.
- Keep the message warm and nonjudgmental. Do not assume why the user was away, imply failure, or require an explanation.
- The message should invite the user to resume an existing goal when useful, without forcing a reset or automatically changing goal records.
- App inactivity must not automatically modify active goals: preserve their active status, schedules, progress, and historical records unchanged. Let the user decide whether to continue, pause, or edit a goal. Do not automatically mark days as missed or reset progress merely because Dayseed was not opened.
- Distinguish app inactivity from lack of recorded habit progress: being away from Dayseed does not prove that a habit was abandoned or that no progress occurred.
- Keep the two-week comeback intentionally simple: show a brief, warm welcome and one action to continue into Dayseed. Goal management remains available from the normal dashboard; do not force a goal review or add goal-management shortcuts to this welcome. Show each qualifying comeback experience at most once per inactivity episode. Once the user acknowledges/dismisses it or continues into Dayseed, suppress it until a new inactivity period qualifies. Do not re-show it on every subsequent app opening.

Example: “We missed you! 🌱 How have things been? Whenever you're ready, we can pick up where you left off.”

## Longer inactivity / no active goals

A separate, broader welcome-back experience is approved when either condition applies: (1) the user has not opened Dayseed for 30 consecutive days, or (2) the user has had no active habits or goals for 30 consecutive days, even if they continue opening the app. For the no-active-goal condition, start the timer when the last active goal becomes completed, paused, archived, or deleted, provided no other active goal remains. Creating or reactivating any goal resets the no-active-goal timer. It should invite the user to reflect on what is new and optionally resume an existing habit or create a new one. If both this trigger and the two-week comeback qualify, show only the broader one-month experience rather than stacking both. Show each qualifying experience at most once per inactivity episode; after acknowledgement, dismissal, or an action choice, suppress it until a new inactivity period qualifies. Use a gentle choice screen. Welcome the user and ask “What's new?” as an invitation, not a required question. Offer an optional text field or journal link for reflection; the user can skip it and continue immediately. If they type into the comeback reflection field, preserve the text as a local unsaved draft so accidentally closing or leaving the screen does not erase it. Keep each unsaved comeback reflection draft locally until the user explicitly saves it through the normal journal flow, deletes it, or chooses to discard it. When the user chooses “Discard,” show a confirmation prompt explaining that the unsaved reflection will be permanently deleted and cannot be recovered; delete the draft only after explicit confirmation. If the user cancels, close the prompt and return to the reflection field with the draft unchanged so they can continue writing. Leaving or closing the screen must never count as discarding. App closure, phone restart, and returning later must not erase an unresolved draft. Keep the unsaved draft quietly in local storage. Restore it automatically when the same comeback reflection is reopened, without an extra confirmation step. Do not add a dashboard card, notification, or Journal-list entry for this draft in v1; it remains discoverable through reopening that same comeback reflection. If another comeback reflection would otherwise begin while an unresolved draft exists, reuse and restore that existing draft instead of creating a second comeback-reflection draft. Never silently replace its text. Do not auto-expire or automatically replace an unresolved draft. A draft is not a journal entry until explicitly saved. Once the normal journal flow confirms the entry was saved successfully, remove the unsaved-draft copy. If saving fails or success is not confirmed, retain the draft unchanged so the user can retry. Keep the draft local; do not upload or share it automatically. Clearing Dayseed's app data through Android settings or uninstalling the app may permanently erase the unsaved draft; v1 does not promise recovery after those destructive actions. Unsaved comeback-reflection drafts are excluded from Android/platform backup in v1; Dayseed does not promise draft recovery through device migration or backup restore. Normal app closure, process death, phone restart, and later return must not erase it. Never require a journal entry or explanation before proceeding. Offer three clear actions where available: (1) resume an existing habit, (2) create a new habit, or (3) continue into the app without making changes. For resume, let the user choose an eligible paused or archived habit, review its settings, and explicitly reactivate it. If no eligible paused or archived habits exist, omit the resume option entirely and show only “Create a new habit” and “Continue into Dayseed.” Never show an empty or broken resume option or pressure the user to create a habit. Completed habits remain in history unless the user deliberately chooses to start one again. Preserve historical records; never reactivate or alter a habit automatically. Do not force a journal entry or demand an explanation for the absence. The user remains in control.

Example direction: “Welcome back! 🌿 What's new with you? Ready to pick up an old habit or plant a new one?”

## Saved reflection editing

- Editing an already-saved journal reflection updates the existing entry rather than creating a duplicate or a new version.
- Keep the entry's original date during ordinary text edits. Changing the date, if supported, must be a deliberate user action rather than a side effect of editing.
- This rule applies to saved journal entries; an unsaved comeback draft remains separate until the normal journal flow confirms a successful save.

## Deleting a saved journal entry

- Deleting a saved journal entry requires a confirmation prompt before deletion.
- The prompt must explain plainly that the saved reflection will be permanently deleted and cannot be recovered.
- Delete the entry only after the user explicitly confirms. Cancelling returns the user to the journal without changing the entry.
- This confirmation rule is for saved journal entries; unsaved comeback drafts have their separately specified discard confirmation.

## Handling confirmed journal-entry removal

- After the user confirms removal, remove the saved journal entry permanently. v1 has no recycle bin, recovery window, or post-removal undo action.
- The confirmation prompt is the safeguard against accidental removal; do not retain the removed reflection for later recovery.

## General journal entries and habit links

- Users can create, save, edit, and delete standalone journal entries without recording or linking a habit.
- A journal entry may optionally be linked to a habit when that connection is useful to the user; linking is never mandatory.
- Keep journal writing independent from habit tracking so users can write on days when they record no habit activity.
- Saved-entry editing and deletion follow the separately approved same-entry edit and confirmed permanent-removal rules.

## Journal media scope for v1

- Journal entries are text-only in v1. Do not support photo attachments or embedded images in journal entries.
- Keep this scope focused on a lightweight, offline-friendly writing experience. Photo support may be reconsidered for a later version without being implied as a v1 capability.

## Journal entry titles

- Journal entries support an optional user-written title. A title is never required to save an entry.
- Untitled entries remain easy to recognize through their date and a preview of the entry text.
- Keep title entry lightweight so adding a quick reflection does not become a multi-field chore.

## Journal search

- Provide an offline search for saved journal entries by matching text in the optional title and entry body.
- Search includes titled and untitled saved entries and does not require a habit link.
- Only saved entries appear in results; unresolved unsaved comeback-reflection drafts are not journal entries and are excluded.
- Keep v1 search focused on text matching; advanced filters such as date ranges or habit filters are outside this decision's scope.

## Daily journal-entry limits

- Users may create as many separate journal entries as they want on the same local calendar day; there is no daily entry limit in v1.
- Journal-entry count is independent of habit records, numeric habit totals, and the optional links between journal entries and habits.

## Optional daily writing prompt

- Offer an optional daily writing prompt to help users who want a starting point. Users may ignore or skip it and write freely instead; responding is never required.
- Prompts are bundled with the app and work offline; v1 does not depend on an API or network connection to provide them.
- Ignoring or skipping a prompt has no penalty and does not affect habit progress, streaks, ranks, or comeback eligibility.
- Keep prompts short, gentle, and nonjudgmental. Exact prompt rotation, selection rules, and presentation remain to be specified.

## Daily writing-prompt selection

- Select one daily writing prompt for each local calendar date. Reopening the journal on the same date must show the same selected prompt rather than rotating automatically.
- Prompt selection works offline using bundled prompts. Define and test deterministic date-based selection so the prompt remains stable throughout the day and changes only when the next local calendar date begins.
- The prompt remains optional; users may skip it or write freely, and it has no effect on habit progress, streaks, ranks, or comeback eligibility.

## Daily prompt visibility

- Keep the daily writing prompt available in the journal experience, but make it easy to dismiss or ignore for that session.
- In v1, do not add a setting to disable prompts entirely. Users can simply skip the prompt and write freely; showing or ignoring it never affects habit progress, streaks, ranks, or comeback eligibility.

## Optional mood tagging for journal entries

- Journal entries may include an optional mood tag. Users can save an entry without choosing a mood; mood tagging is never required.
- Prefer a curated vocabulary of distinct mood words across a broad emotional range instead of a small list such as only happy, sad, and angry plus a separate numeric intensity rating. Include meaningful gradations where natural, such as irritated, angry, and furious, while avoiding the assumption that every emotion is simply a stronger or weaker version of another.
- The mood picker initially shows a fixed, system-curated set of up to ten broadly recognizable moods. This starter set is not ranked by an individual user's behavior or by live user-frequency analytics.
- Provide a “More” action that opens the broader mood vocabulary organized into understandable emotional families or spectrums. Within a family, offer more specific words that help users describe what they feel, including milder and stronger expressions where appropriate.
- Mood words are self-description, not a diagnosis. Keep the feature private and local-first; do not infer a mental-health condition or make clinical claims from a selected mood.
- Exact mood taxonomy, starter ten, wording/translations, whether one or multiple moods can be selected per entry, and the visual interaction remain to be specified.

## Number of moods per journal entry

- Allow one primary mood per journal entry and an optional secondary mood to capture mixed feelings without making mood tagging feel like a questionnaire.
- The secondary mood is optional; users can select only the primary mood or skip mood tagging entirely.
- Keep both choices within the same curated mood vocabulary and emotional-family picker. Do not add a numeric mood-intensity rating; use meaningful mood words and gradations instead.

## Mood labels in journal search results

- Journal search results show the entry's primary mood label when one is present, and also show its optional secondary mood label when present.
- Keep the date and matching text preview visible; mood labels supplement these details and do not replace the search match or entry identification.
- Entries without mood tags remain fully searchable and display normally without mood labels. Mood is not a search filter in this decision.

## Local mood history and summaries

- In v1, provide simple, neutral mood-history summaries based only on moods the user has actually recorded in saved journal entries.
- Summaries are generated on-device and work offline. For example, Dayseed may say, "Hopeful appeared in 6 entries this month," when that count is accurate for the selected period.
- Do not infer that one mood caused a habit outcome, diagnose a mental-health condition, label a mood as good or bad, or present a pattern as a certainty beyond the recorded entries.
- Mood tagging remains optional, so summaries must make clear that they describe recorded mood tags, not the user's complete emotional experience. Entries without mood tags are not evidence of any particular mood.
- Cross-analysis or correlations between moods and habit completion are outside this decision's v1 scope.

## Placement of mood history

- Place mood-history summaries in the existing Insights area rather than creating a separate main-navigation destination for mood history.
- Keep individual mood tags visible on their journal entries and in journal search results as already specified. Insights provides the aggregated view; it does not replace access to the original entries.
- Mood summaries remain neutral, optional-data-based, on-device, and available offline.

## Mood Insights time periods

- Provide two summary periods: **This week** and **This month**.
- Keep both summaries neutral and based only on mood tags recorded in saved journal entries during the relevant period. Do not imply that optional mood tags represent every day or the user's complete emotional experience.
- Keep the summaries on-device and available offline. This decision does not introduce mood/habit correlations or diagnostic claims.

## Mood Insights summary format

- For both **This week** and **This month**, show a mood distribution: the primary moods actually recorded in saved journal entries and how often each was recorded during the selected period.
- Use simple counts or a restrained visual chart. Do not reduce the whole period to a single defining mood, infer moods on untagged entries, or imply that the distribution represents the user's complete emotional experience.
- Keep the summary neutral, on-device, and available offline. This does not add mood/habit correlations or diagnostic claims.

## Primary versus secondary moods in Mood Insights

- Mood distributions for both **This week** and **This month** count **primary moods only**, using the primary mood recorded on each saved journal entry.
- Secondary mood tags remain visible on their journal entries and in journal search results, but are excluded from the Mood Insights distribution in v1.
- Do not infer a primary mood when an entry has no mood tag. Keep summaries neutral, on-device, and available offline.

## Mood Insights with limited or no data

- Adapt the presentation to the amount of recorded primary-mood data in the selected period.
- When records are limited, show a small, factual count rather than implying a meaningful trend. Use a distribution when the available records make that presentation useful; do not impose a fixed minimum-record threshold for displaying any information.
- When no primary-mood records exist in the period, show a gentle, neutral empty state that explains mood summaries appear when the user chooses to record moods. Do not infer moods from untagged entries or frame the empty state as failure.
- Apply this behavior to both **This week** and **This month**, on-device and offline.

## Opening journal entries from Mood Insights

- Tapping a primary mood in the weekly or monthly Mood Insights distribution opens the saved journal entries whose **primary mood** matches that mood within the selected period.
- Keep the period constraint (This week or This month) when opening the matching entries. Do not include entries based only on a matching secondary mood.
- This is a direct route from the summary to the original writing; it does not change entries, mood tags, or the existing journal-search behavior.

## Optional comparison with the previous mood period

- Let users optionally compare the current Mood Insights period with the immediately preceding equivalent period: this week versus the previous week, or this month versus the previous month.
- Compare only recorded primary-mood counts or distributions for each period. Do not frame changes as good or bad, diagnose the user, infer causation, or imply that optional mood tags capture the user's complete emotional experience.
- Keep the comparison opt-in rather than automatically adding it to every view. If one or both periods have insufficient recorded data, explain the limitation neutrally and do not imply a meaningful trend.

## Mood comparison control in Insights

- Place a simple **Compare with previous period** control directly in Mood Insights.
- Keep the comparison hidden until the user chooses the control; do not add a permanent comparison panel or require a Settings change.
- The control compares the selected period with its immediately preceding equivalent period (week-to-week or month-to-month), following the neutral and limited-data rules above.

## Mood comparison control

- Place a simple **Compare with previous period** control directly in Insights.
- Keep the comparison hidden until the user activates the control; do not make comparisons automatic or require users to visit Settings to enable them.
- When activated, compare the current week with the previous week or the current month with the previous month, using recorded primary-mood counts/distributions and neutral wording as specified above.

## Export, local backup/restore, and premium cloud sync

- **Readable journal export (v1):** Users can export saved journal entries in a human-readable file, including entry dates and recorded primary/secondary mood tags where present. Use Android's share/save flow. Exporting does not delete or alter the originals.
- **Portable local backup and restore (v1):** Users can create a complete, versioned Dayseed backup file and restore it on another phone without an account or internet connection. D-091–D-145 record the approved backup/restore contract: file format and integrity validation, full backup coverage, optional password protection for manual backups, preview, Replace/Merge choices, conflict handling, safety backup before Replace, safe interruption/recovery, Auto Backup opt-in/frequency/destination/retention/status, and failure handling. These are planning decisions, not implemented features. Android platform Auto Backup/device-transfer exclusion is approved by D-186: sensitive Dayseed app data should be excluded by default. Technical verification of the actual platform rules and device-transfer paths remains required; a system file provider selected by the user may itself sync the portable file.
- **Premium online backup and syncing:** Cloud backup and cross-device sync are planned as premium capabilities. Keep the app's core offline functionality usable without an account, cloud connection, or paid subscription. Online backup and sync must not be implied to exist in v1 until their scope and rollout are confirmed.
- Keep readable export distinct from full backup: the former is for reading/sharing journal content, while backup/restore is intended to preserve app data for moving devices.

## Offline backup contents

- A portable offline backup should preserve the user's complete personal Dayseed experience: habit definitions and schedules, all habit-tracking history, saved journal entries and their mood tags, relevant user preferences, and unresolved comeback reflection drafts where technically feasible.
- Treat unresolved drafts as important user writing: include them in the backup when supported and restore them as drafts, not as saved journal entries. Do not silently replace an existing draft during restore.
- Do not include account credentials, secrets, or premium purchase verification data in the portable backup. These must be handled separately and securely.
- The exact backup schema, version compatibility, and restore conflict/replacement behavior still require decisions before implementation.

## Restore behavior when Dayseed already contains data

- When restoring a portable backup onto an installation that already has personal data, offer two explicit choices: **Replace existing data** or **Merge with existing data**.
- Explain the consequences of each choice in plain language before the user commits to the operation. Do not silently replace or merge data.
- Create a safety backup of the current local Dayseed data before either operation and verify that it was created successfully before proceeding. If safety-backup creation fails, stop the restore and leave existing data unchanged.
- The exact merge rules for duplicate or conflicting records, and the handling of unsupported backup versions, must be defined before implementation.

## Merge rules for duplicate and conflicting records

- Use stable record IDs and reliable type-specific keys to recognize records that already exist in both the backup and current installation. Do not create duplicate copies of the same logical record.
- Preserve distinct records from both datasets.
- When matching records genuinely conflict, do not silently overwrite either version. Detect and surface the conflict for a safe, explicit resolution path.
- Exact conflict rules and resolution UX for each record type remain to be specified before implementation. Merge must preserve the pre-restore safety backup and must not mutate existing data if validation or conflict handling fails.

## Handling genuine merge conflicts

- If matching records genuinely conflict, preserve both versions intact and mark the conflict for later user review.
- Do not interrupt the entire restore to demand a decision for every conflict. Complete the restore safely while retaining both versions and making unresolved conflicts discoverable.
- Do not silently select a winner, overwrite either version, or delete a conflicting version. The conflict-review interface and record-specific resolution rules remain to be specified.

## Finding unresolved merge conflicts

- List unresolved backup-restore merge conflicts in a **Backup & Restore** section within Settings so users can review them whenever convenient.
- Do not add a dashboard notice or require immediate conflict review after restoration. Keep the conflict list discoverable until each conflict is resolved.
- Preserve both versions until the user completes an explicit resolution. Exact review and resolution interactions remain to be specified.

## Resolving journal-entry merge conflicts

- For a genuine conflict between two versions of a journal entry, show both versions clearly so the user can compare them.
- Offer three explicit outcomes: **Keep backup version**, **Keep current version**, or **Keep both as separate journal entries**.
- Do not discard either version until the user chooses an outcome. If keeping both, create distinct entries without falsely presenting them as one entry or silently changing their original content. Preserve relevant dates and mood tags with the correct version where possible.
- The review must be available from Settings → Backup & Restore. Exact behavior for other record types remains to be specified.

## Resolving habit-tracking merge conflicts

- When backup and current-installation habit-tracking records genuinely conflict, preserve the competing records for user review rather than silently choosing a winner.
- Offer the user these outcomes: **Keep backup record**, **Keep current record**, or **Keep both as distinct records** where the data model supports this without corrupting history.
- Never automatically add conflicting numeric totals together. A conflict is not evidence that the values are separate valid contributions.
- Preserve the safety backup and the original conflicting values until the user explicitly resolves the conflict. Exact handling for record types that cannot safely coexist remains to be specified.

## Resolving habit-definition and schedule conflicts

- When the backup and current installation contain conflicting definitions or schedules for the same habit, show the differing settings side by side and let the user choose which definition to keep.
- Where appropriate, also allow the user to preserve both definitions as separate habits. Do not automatically prefer the latest edit or silently replace the current or backup definition.
- Resolving a definition conflict must not rewrite historical tracking records or retroactively change the meaning of past records. Exact field-by-field comparison and resolution details remain to be specified.

## Resolving conflicting preferences during restore

- When backup and current-installation preferences differ, show the differing values and let the user choose which value to keep.
- Do not silently prefer the backup or current installation. Keep the restore review clear and preserve unrelated preferences.

## Resolving conflicting unfinished comeback drafts

- When the backup and current installation contain different versions of the same unresolved comeback reflection draft, preserve both versions intact and mark them for later review.
- Do not interrupt the whole restore to demand an immediate choice, and do not silently replace or discard either draft. Keep both represented as unresolved drafts until the user explicitly resolves the conflict.

## Validating backup integrity and compatibility

- Validate the backup's integrity, completeness, and compatibility before applying any restore or merge changes.
- If the backup is corrupt, incomplete, or from a version whose compatibility cannot be established safely, stop the restore and explain the issue. Do not partially apply changes or modify existing data.
- Partial recovery is not permitted under this decision. Preserve the existing data and its pre-restore safety backup.
## Choosing a backup save destination

- When creating a portable offline backup, use Android's system file picker so the user chooses where the backup file is saved.
- Support destinations exposed by the picker, such as device storage, an SD card, or another compatible document provider. Do not assume every destination is available on every device.
- Saving the backup must not move or delete the live Dayseed data.

## Optional password protection for offline backups

- Offer optional password protection when creating a portable offline backup.
- Explain clearly that password protection encrypts the backup and that forgetting the password may make its contents unrecoverable; do not promise password recovery unless a separately designed secure recovery mechanism exists.
- Make it clear whether a backup is password-protected, and require the correct password before restoring an encrypted backup. Keep unprotected backup creation available as an explicit user choice.

## Preserve source backup after restore

- Restoring from a portable backup must leave the source backup file untouched.
- Do not rename, modify, move, or delete the source file as part of restoration. The user may keep it as a fallback or remove it manually.

## Offline backup creation, format, and verification

### 64. Confirming a backup password

- **Approved decision:** A — Enter the password twice and confirm it before creating the encrypted backup.
- **Rationale:** Catch mistyped passwords before the user relies on a backup that may later be impossible to decrypt.
- **Acceptance checks:** Test matching and mismatching password entries; a mismatch must block encrypted backup creation.

### 65. Incorrect password during restoration

- **Approved decision:** A — Allow retries with a clear error; do not modify existing data before successful decryption and validation.
- **Rationale:** Give users another chance without risking live data from a failed decryption attempt.
- **Acceptance checks:** Test wrong-password retries and confirm no existing data changes before successful decryption and validation.

### 66. Backup file format

- **Approved decision:** A — Use a dedicated Dayseed format with an app-specific extension, version metadata, and integrity metadata.
- **Rationale:** Make portable backups identifiable and support explicit format/version and integrity checks.
- **Acceptance checks:** Test extension recognition, version metadata, integrity metadata, and rejection of unsupported or damaged files.

### 67. Default backup filename

- **Approved decision:** A — Use a date-and-time filename, for example `Dayseed-Backup-2026-10-23-1245`.
- **Rationale:** Make multiple backups easy to distinguish without requiring a custom name.
- **Acceptance checks:** Test generated names include date and time and avoid unintended overwrites when backups are created close together.

### 68. Backup contents summary

- **Approved decision:** A — Show a pre-creation summary of habits, tracking history, saved journal entries, moods, preferences, and included drafts.
- **Rationale:** Let users understand what the complete backup contains before saving it.
- **Acceptance checks:** Verify summary counts/categories match the data included in the produced backup, including draft inclusion status.

### 69. Excluding data from a backup

- **Approved decision:** A — v1 creates complete backups only; users cannot exclude individual data categories.
- **Rationale:** Keep backups complete and predictable and reduce accidental omission of important data.
- **Acceptance checks:** Verify all approved backup categories are included and no category-exclusion controls are presented in v1.

### 70. Backup creation failure

- **Approved decision:** A — Fail safely, report the failure, never present an incomplete file as valid, and leave Dayseed data untouched.
- **Rationale:** A failed export must not be mistaken for a usable backup or damage the source data.
- **Acceptance checks:** Simulate write/storage failures; verify clear failure feedback, no success state for partial output, and unchanged live data.

### 71. Verifying a newly created backup

- **Approved decision:** A — Verify integrity and readability after writing; report success only if verification passes.
- **Rationale:** Confirm the saved artifact is usable rather than treating a successful write call as sufficient.
- **Acceptance checks:** Test successful verification and corrupted/unreadable output; success must only appear after verification passes.

### 72. Backup history

- **Approved decision:** A — List backups Dayseed can reliably identify, with date, size, and verification status where available.
- **Rationale:** Help users distinguish known backup files without claiming visibility into files the app cannot reliably track.
- **Acceptance checks:** Test available metadata, unavailable verification status, and backups moved or deleted outside Dayseed; never claim exhaustive device-wide history.

### 73. Reminder to create backups

- **Approved decision:** A — Offer an opt-in local reminder; never upload data or nag users who decline.
- **Rationale:** Support backup hygiene while respecting user choice and the offline-first privacy promise.
- **Acceptance checks:** Verify reminder is opt-in, runs locally, sends no data, and remains suppressed after the user declines.

## Backup history, restore safety, and automatic backups

### Decision 74 — Managing old backups

- **Approved decision:** A — Keep backups until the user deletes them.
- **Rationale:** Users retain control over backup retention; Dayseed does not automatically remove old backup files.
- **Acceptance checks:** Verify backups are not automatically deleted or pruned.

### Decision 75 — Actions in backup history

- **Approved decision:** A — Offer restore and Android share/open options where supported.
- **Rationale:** Let users restore a known backup or access platform-supported sharing/opening actions without assuming every provider supports every action.
- **Acceptance checks:** Verify restore and supported share/open actions; gracefully handle unavailable actions.

### Decision 76 — Confirming a Replace restore

- **Approved decision:** A — Explain that current app data will be replaced and require explicit final confirmation after the safety backup succeeds.
- **Rationale:** Make destructive consequences clear and ensure a verified safety backup exists before the final Replace action.
- **Acceptance checks:** Verify Replace cannot proceed without a successful safety backup and explicit final confirmation.

### Decision 77 — Restore preview

- **Approved decision:** A — Show backup date, format/version, and counts of included data categories before restore.
- **Rationale:** Give users a concise view of what they are about to restore without overwhelming the initial restore flow.
- **Acceptance checks:** Verify preview metadata and category counts match the validated backup.

### Decision 78 — Safety-backup failure

- **Approved decision:** A — Stop restore, explain the failure, and leave existing data untouched if the safety backup cannot be created or verified.
- **Rationale:** A restore must not proceed when its required rollback safeguard is unavailable.
- **Acceptance checks:** Simulate safety-backup creation and verification failures; verify restore stops and current data remains unchanged.

### Decision 79 — Restore progress and interruption

- **Approved decision:** C — Where technically safe, resume or recover cleanly after interruption; never leave a partial restore presented as complete.
- **Rationale:** Protect data integrity across app closure, process termination, or device restart while avoiding unsafe partial application.
- **Acceptance checks:** Test interruption at restore stages, clean recovery or safe rollback, and truthful completion status.

### Decision 80 — Backup compatibility

- **Approved decision:** A — Restore only when compatibility can be established; otherwise stop without changing existing data.
- **Rationale:** Do not risk existing data when backup format or schema compatibility is uncertain.
- **Acceptance checks:** Test supported older versions, unsupported newer versions, and unknown versions; unsafe cases must stop before mutation.

### Decision 81 — Duplicate backups

- **Approved decision:** A — List each identifiable backup file separately; do not assume separate files are duplicates.
- **Rationale:** Distinct files may be intentional copies; avoid hiding files or implying equivalence without reliable evidence.
- **Acceptance checks:** Verify distinct backup files remain separately accessible and are never automatically deleted as duplicates.

### Decision 82 — Automatic backup frequency

- **Approved decision:** A — Offer daily, every 2 days, weekly, monthly, or every 6 months; users need not choose a weekday or calendar date. Manual backup remains available.
- **Rationale:** Provide a simple interval selector while keeping manual backup available regardless of the automatic schedule.
- **Acceptance checks:** Verify all five intervals, schedule persistence, manual backup availability, and no weekday/date picker requirement.

### Decision 83 — Auto Backup setup and default location

- **Approved decision:** A — Under Settings → Backup & Restore, users can enable Auto Backup, choose a destination folder, and select a frequency; manual backup remains available.
- **Rationale:** Keep backup controls in one predictable place and let users choose where automatic backup files are stored.
- **Acceptance checks:** Verify opt-in enablement, folder selection, frequency configuration, manual backup, and clear handling when the chosen location is unavailable.

### Decision 84 — Auto Backup opt-in timing

- **Approved decision:** B — Ask the user about enabling Auto Backup during setup.
- **Rationale:** Make the automatic protection choice visible during onboarding rather than silently enabling it or leaving it undiscovered.
- **Acceptance checks:** Verify setup asks whether to enable Auto Backup, records the choice, and allows the user to decline without losing access to manual backup.

### Decision 85 — First automatic backup timing

- **Approved decision:** A — Run the first automatic backup at the next scheduled interval, not immediately after setup.
- **Rationale:** Keep the setup action distinct from the configured schedule and avoid an unexpected immediate backup.
- **Acceptance checks:** Verify setup alone does not trigger an immediate backup and the first run follows the selected interval.

### Decision 86 — Destination unavailable

- **Approved decision:** B — Require the user to choose a destination again when the configured destination is unavailable.
- **Rationale:** Avoid silently redirecting personal backup data to a location the user did not choose.
- **Acceptance checks:** Simulate a missing or inaccessible destination; automatic backup must not write elsewhere without the user selecting a new destination.

### Decision 87 — Scheduled backup failure

- **Approved decision:** A — Report the failure and wait until the next scheduled run; do not automatically retry in v1.
- **Rationale:** Keep v1 scheduling behavior predictable and avoid repeated background attempts after a failed run.
- **Acceptance checks:** Verify a failed scheduled run is reported, no retry loop starts, and the next configured schedule remains intact.

### Decision 88 — Insufficient storage

- **Approved decision:** B — Warn in advance where possible when storage may be insufficient.
- **Rationale:** Help users address storage constraints before a scheduled backup fails, without risking existing backup files.
- **Acceptance checks:** Where platform information permits, show a clear advance warning; never delete existing backups automatically to free space.

### Decision 89 — Password-protected automatic backups

- **Approved decision:** A — In v1, password protection is available for manual backups only; Auto Backup uses the standard backup mode.
- **Rationale:** Avoid requiring a stored or repeatedly entered password for unattended scheduled runs in v1.
- **Acceptance checks:** Verify manual backup can use the approved password-protection flow and Auto Backup does not silently claim password protection.

### Decision 90 — Changing the backup destination

- **Approved decision:** A — Changing the configured destination leaves existing files in the old location untouched; no automatic migration or copy is offered in v1.
- **Rationale:** Avoid unexpected file movement, duplication, or deletion when changing where future backups are stored.
- **Acceptance checks:** Verify changing the destination affects future runs only and leaves old backup files unchanged.

### Decision 91 — Auto Backup while the app is closed

- **Approved decision:** A — Use Android background scheduling, subject to operating-system restrictions.
- **Rationale:** Support unattended backups while being honest that Android may defer background work because of battery, permission, or system constraints.
- **Acceptance checks:** Verify scheduling uses an Android-supported background mechanism and the UI does not promise exact execution times that the OS cannot guarantee.

### Decision 92 — Backup filename collisions

- **Approved decision:** A — Generate a unique filename suffix rather than overwrite an existing backup.
- **Rationale:** Preserve every existing backup and avoid destructive collision prompts during unattended operation.
- **Acceptance checks:** Create backups with the same base timestamp/name and verify each remains a separate identifiable file with no overwrite.

### Decision 93 — Automatic backup retention

- **Approved decision:** A — Create a new backup file for every automatic run; users manage deletion themselves.
- **Rationale:** Prioritize recoverability and avoid automatic pruning or replacement in v1.
- **Acceptance checks:** Verify successful runs create distinct files, existing backups are not replaced or pruned, and deletion is user-initiated only.

### Decision 94 — When a scheduled backup is due

- **Approved decision:** A — Calculate the next automatic backup interval from the last successful backup.
- **Rationale:** Failed attempts should not silently shift the schedule forward and extend the time since the last usable backup.
- **Acceptance checks:** Verify the next due time is based on the last successful backup; failed attempts do not count as successful completion or reset the interval.

### Decision 95 — Auto Backup after device restart

- **Approved decision:** A — Let Android reschedule eligible background work after restart.
- **Rationale:** Use the platform's supported background scheduling rather than inventing a parallel scheduler.
- **Acceptance checks:** Verify eligible work is scheduled using an Android-supported mechanism after reboot, subject to platform restrictions; do not promise an exact execution time.

### Decision 96 — Revoked destination access

- **Approved decision:** A — Report the access problem and ask the user to select the destination again.
- **Rationale:** Make a broken destination understandable and restore access only through an explicit user choice.
- **Acceptance checks:** Simulate revoked or expired access; show a clear status, do not write elsewhere, and provide a path to reselect the destination.

### Decision 97 — Backup when data has not changed

- **Approved decision:** A — Skip creating a new file only when unchanged data can be reliably detected.
- **Rationale:** Avoid unnecessary duplicate backups when equivalence is known without compromising recovery confidence.
- **Acceptance checks:** Verify reliably unchanged data can be skipped with visible status; if change detection is unavailable or uncertain, do not incorrectly claim the backup was refreshed.

### Decision 98 — Successful Auto Backup status

- **Approved decision:** A — Show a quiet status in Settings → Backup & Restore, including the last-success date.
- **Rationale:** Give users confidence that automatic protection is working without interrupting them after every run.
- **Acceptance checks:** Verify the last-success status updates only after a completed and verified backup; failures must not update it as though successful.

### Decision 99 — Low battery during Auto Backup

- **Approved decision:** A — Let Android apply its background-work and battery restrictions.
- **Rationale:** Respect system-managed battery behavior and avoid introducing an extra battery-threshold setting in v1.
- **Acceptance checks:** Verify scheduling uses supported Android background behavior and the UI does not promise execution during system deferral.

### Decision 100 — Repeated automatic backup failures

- **Approved decision:** A — Show failure status in Backup & Restore; do not send repeated failure notifications.
- **Rationale:** Keep failures discoverable without generating recurring notification noise.
- **Acceptance checks:** Verify failures are visible in the backup screen and repeated failures do not produce repeated notifications.

### Decision 101 — Disabling Auto Backup

- **Approved decision:** A — Stop future scheduled runs and leave all existing backup files untouched.
- **Rationale:** Disabling a schedule must not unexpectedly remove the user's recovery copies.
- **Acceptance checks:** Verify disabling prevents future scheduled runs and does not delete or modify existing backups.

### Decision 102 — Manual backup while Auto Backup is enabled

- **Approved decision:** A — Always allow “Back up now”; a successful manual backup does not reset or disable the configured automatic schedule.
- **Rationale:** Preserve user control for an immediate backup without making the schedule unpredictable.
- **Acceptance checks:** Verify manual backup remains available with Auto Backup enabled and does not alter the configured interval or disable scheduling.

### Decision 103 — Displaying the next scheduled run

- **Approved decision:** A — Show an estimated next-run time and explain that Android may defer background work.
- **Rationale:** Make the schedule understandable without falsely promising exact execution under operating-system control.
- **Acceptance checks:** Verify the estimate reflects the configured interval and last successful backup, is refreshed when relevant settings change, and is labelled as an estimate rather than a guarantee.

### Decision 104 — Calculating 14 days of app inactivity

- **Approved decision:** A — Use 14 complete elapsed calendar days in the device's local time zone.
- **Rationale:** Make the threshold understandable in ordinary calendar terms while consistently using the current local date.
- **Acceptance checks:** Verify the 14-day threshold uses local calendar-day boundaries and does not fire before 14 complete calendar days have elapsed.

### Decision 105 — Time-zone changes during inactivity

- **Approved decision:** A — Recalculate using the current local calendar date and avoid duplicate comeback displays.
- **Rationale:** Keep the experience aligned with the user's current calendar without showing the same qualifying episode repeatedly because of a time-zone change.
- **Acceptance checks:** Test time-zone changes across dates; verify the qualifying state is recalculated consistently and an already-shown episode is not shown again solely because the time zone changed.

### Decision 106 — Starting inactivity measurement

- **Approved decision:** A — Record the first successful app opening and begin counting inactivity from then.
- **Rationale:** Do not treat time before the user has ever opened Dayseed as an absence from the app.
- **Acceptance checks:** Verify installation alone does not trigger a comeback experience; the first successful opening establishes the initial activity timestamp.

### Decision 107 — Thirty days without active habits

- **Approved decision:** A — Start the period when the last active habit becomes completed, paused, archived, or deleted and no other active habits remain.
- **Rationale:** Base the experience on the absence of active habits rather than the absence of historical records or recent logging.
- **Acceptance checks:** Verify the timer starts only when no active habits remain and the last active habit enters one of the approved states.

### Decision 108 — Creating or reactivating a habit during the no-active-habit period

- **Approved decision:** A — Reset the no-active-habit timer; begin a new period if all active habits later end.
- **Rationale:** A newly active habit means the user has returned to maintaining an active habit, so the previous no-active-habit period no longer applies.
- **Acceptance checks:** Verify creating or reactivating an active habit resets the timer; if all active habits later end, a fresh 30-day period begins.

### Decision 109 — Language about progress during an absence

- **Approved decision:** A — Do not assume what happened during the absence; invite the user to continue when ready.
- **Rationale:** Keep the Comeback Flow nonjudgmental and avoid claiming that progress was missed or lost when the app cannot know the user's circumstances.
- **Acceptance checks:** Review copy for guilt, blame, or unsupported claims about missed progress; provide a clear, welcoming next step.

### Decision 110 — Required comeback reflection

- **Approved decision:** A — Reflection is optional and can be skipped.
- **Rationale:** Keep returning to Dayseed low-friction and avoid making journaling a prerequisite for resuming.
- **Acceptance checks:** Verify the user can continue without entering or saving a reflection and that optional draft preservation rules remain in effect.

### Decision 111 — Comeback screen when no habit is available to resume

- **Approved decision:** A — Offer create-new and continue options only; omit resume.
- **Rationale:** Avoid presenting a nonfunctional resume action when there is no eligible active habit.
- **Acceptance checks:** Verify the no-eligible-habit state offers create-new and continue, with no resume action or empty resume list.

### Decision 112 — Dismissing a comeback experience

- **Approved decision:** A — Suppress that experience for the current qualifying inactivity episode.
- **Rationale:** Respect dismissal without permanently disabling future comeback experiences for later episodes.
- **Acceptance checks:** Verify dismissal prevents repeated display during the same qualifying episode and a later distinct qualifying episode can be evaluated normally.

### Decision 113 — Simultaneous 14-day and 30-day comeback triggers

- **Approved decision:** A — Show only the broader 30-day welcome-back experience when both triggers qualify.
- **Rationale:** Prevent stacked or duplicate comeback screens and give precedence to the more comprehensive experience.
- **Acceptance checks:** Verify when both triggers qualify, only the 30-day experience is shown for that episode; no 14-day screen is queued behind it.

### Decision 114 — Exact 14-day comeback eligibility boundary

- **Approved decision:** A — Qualify at the start of the local date after 14 complete local calendar days have elapsed.
- **Rationale:** Use a predictable calendar-day threshold while ensuring the full approved period has elapsed.
- **Acceptance checks:** Test across month/year boundaries and local date changes; do not trigger before 14 complete local calendar days have elapsed.

### Decision 115 — Uncertain or changed device clock

- **Approved decision:** A — Recalculate from available local timestamps, avoid duplicate displays, and never invent certainty.
- **Rationale:** Clock changes can make elapsed-time calculations uncertain; the app should handle this conservatively and transparently.
- **Acceptance checks:** Test clock changes and inconsistent timestamps; avoid duplicate comeback displays and do not claim a precise absence duration when it cannot be established.

### Decision 116 — Start of the 30-day no-active-habit timer

- **Approved decision:** A — Use the recorded local date/time when the last active habit becomes inactive and no active habits remain.
- **Rationale:** Anchor the period to the actual status change rather than delaying it until a later app opening.
- **Acceptance checks:** Verify the event timestamp is recorded when the last active habit becomes completed, paused, archived, or deleted while no other active habits remain.

### Decision 117 — Time-zone changes during the no-active-habit period

- **Approved decision:** A — Preserve the recorded event timestamp and evaluate eligibility using the current local calendar.
- **Rationale:** Do not restart or freeze an inactivity period merely because the device's time zone changes.
- **Acceptance checks:** Test time-zone changes and ensure the original event timestamp is retained while current local-calendar eligibility is recalculated consistently.

### Decision 118 — Repeated habit-status changes during the no-active-habit period

- **Approved decision:** A — Recompute from the latest valid state; active habits reset the timer, and a new no-active period begins only when none remain active.
- **Rationale:** Keep eligibility aligned with current habit state rather than stale earlier status changes.
- **Acceptance checks:** Test completing, pausing, archiving, deleting, creating, and reactivating habits in different orders; only a period with no active habits can qualify.

### Decision 119 — Restarting a completed habit from the comeback choice screen

- **Approved decision:** A — Offer a clear optional “Start again” choice without implying previous progress is erased.
- **Rationale:** Support a fresh attempt while preserving historical tracking and the user's control.
- **Acceptance checks:** Verify restart is explicit, prior records remain intact, and the copy does not imply that restarting deletes or rewrites history.

### Decision 120 — Comeback reflection draft after unexpected app closure

- **Approved decision:** A — Keep the draft locally and restore it automatically when that reflection is reopened.
- **Rationale:** Prevent accidental loss of personal writing and avoid unnecessary confirmation steps.
- **Acceptance checks:** Verify draft survives app closure and phone restart, is restored directly into the field, and is not shown as a saved journal entry.

### Decision 121 — Failed or uncertain comeback reflection save

- **Approved decision:** A — Keep the draft unchanged until saving is confirmed; do not create a duplicate journal entry.
- **Rationale:** Preserve user-authored text and avoid duplicate entries when the result of a save operation is uncertain.
- **Acceptance checks:** Simulate failed and uncertain saves; retain the draft unchanged until success is confirmed and ensure retry/recovery does not duplicate the journal entry.

### Decision 122 — Discarding a comeback reflection draft

- **Approved decision:** A — Require explicit confirmation that permanent deletion cannot be undone.
- **Rationale:** Protect personal writing from accidental destructive actions.
- **Acceptance checks:** The confirmation explains deletion is permanent; cancel returns to the unchanged draft, and deletion occurs only after explicit confirmation.

### Decision 123 — Leaving an unfinished comeback reflection

- **Approved decision:** A — Preserve it as a local draft without adding it to Journal or showing a dashboard reminder.
- **Rationale:** Keep unsaved writing private and unobtrusive until the user chooses to save it.
- **Acceptance checks:** Leaving or closing the screen does not discard the draft; it remains absent from Journal and the dashboard until explicitly saved through the normal journal flow.

### Decision 124 — Saving journal entries

- **Approved decision:** A — A journal entry becomes permanent only after the user explicitly saves it.
- **Rationale:** Keep unsaved writing distinct from saved journal history and avoid unexpected entries.
- **Acceptance checks:** Leaving the editor or closing the app does not create a saved journal entry; explicit save is required, and unfinished drafts follow the existing local draft-preservation rules.

### Decision 125 — Editing an existing journal entry

- **Approved decision:** A — Update the existing entry after explicit save; preserve its identity and creation timestamp.
- **Rationale:** Edits should not silently create duplicates or make an old entry appear newly created.
- **Acceptance checks:** Saving edits updates the same entry; its stable identity and original creation timestamp remain unchanged.

### Decision 126 — Deleting a journal entry

- **Approved decision:** A — Require confirmation before permanent deletion.
- **Rationale:** Protect personal writing from accidental destructive actions.
- **Acceptance checks:** Cancel leaves the entry unchanged; permanent deletion occurs only after explicit confirmation and is communicated clearly.

### Decision 127 — Changing a journal entry's date

- **Approved decision:** A — Change the entry date only after explicit save; do not silently rewrite habit records.
- **Rationale:** Journal dates and habit-tracking records are distinct data with independent histories.
- **Acceptance checks:** Unsaved date edits do not persist; saved changes affect the journal entry date only and do not move or modify habit records.

### Decision 128 — Multiple journal entries on one date

- **Approved decision:** A — Allow multiple entries for the same date; each remains distinct.
- **Rationale:** Let users capture separate reflections without merging or overwriting their writing.
- **Acceptance checks:** Multiple entries can coexist on one date, retain distinct identities and text, and appear individually in journal history.

### Decision 129 — Blank journal entries

- **Approved decision:** A — Do not save an entry containing only whitespace.
- **Rationale:** Avoid empty entries cluttering journal history.
- **Acceptance checks:** Empty or whitespace-only content cannot be saved as a journal entry; existing drafts remain intact when save is rejected.

### Decision 130 — Journal entries associated with an archived or deleted habit

- **Approved decision:** A — Keep the journal entry and its text intact.
- **Rationale:** Habit lifecycle changes must not erase independent personal writing.
- **Acceptance checks:** Archiving or deleting a habit leaves related journal entries and their content intact and accessible according to normal journal rules.

### Decision 131 — Correcting historical habit tracking

- **Approved decision:** A — Let the user explicitly correct a specific record, then recalculate affected insights from the corrected data.
- **Rationale:** Support accurate history while avoiding automatic changes to neighboring dates.
- **Acceptance checks:** Corrections target the selected record, are persisted explicitly, and update dependent insights without silently changing unrelated records.

### Decision 132 — Editing a habit target or schedule after history exists

- **Approved decision:** A — Apply changes prospectively; preserve past records and their historical interpretation.
- **Rationale:** Current settings should not rewrite what a past day meant under its historical target or schedule.
- **Acceptance checks:** Changes have a defined effective date; prior records and historical completion status remain based on the settings applicable to those dates.

### Decision 133 — Accidental duplicate habit-tracking records

- **Approved decision:** A — Prevent duplicates for the same logical entry; never silently add duplicate numeric totals.
- **Rationale:** Duplicate submissions must not inflate progress or distort insights.
- **Acceptance checks:** Repeated submission of the same logical entry is idempotent or rejected safely; distinct legitimate numeric entries remain separate and are counted according to the approved aggregation rules.

## Decisions 134–143 — Numeric records and tracking-mode semantics

### Decision 134 — Explicit zero versus no record
- **Approved decision:** A — Treat an explicit numeric value of zero as a recorded value, distinct from having no record.
- **Rationale:** Zero can be a meaningful measurement and must not be confused with missing data.
- **Acceptance checks:** A saved zero is displayed and stored as a real record; a date with no entry remains unrecorded; insights distinguish both states.

### Decision 135 — Multiple numeric entries on one habit date
- **Approved decision:** A — Sum distinct valid numeric entries for the same habit and local calendar date.
- **Rationale:** Separate sessions or entries should contribute to one daily total without losing their individual identity.
- **Acceptance checks:** Each distinct entry remains individually addressable; the daily total equals the sum of valid entries for that local date; entries on other dates are excluded.

### Decision 136 — Editing a numeric entry
- **Approved decision:** A — Update the specific selected entry and recalculate the daily total and dependent insights.
- **Rationale:** Corrections should replace the mistaken value rather than inflate progress by adding the corrected amount.
- **Acceptance checks:** Only the selected entry changes; its identity is preserved; daily totals and dependent insights reflect the corrected value.

### Decision 137 — Removing an accidental numeric entry
- **Approved decision:** A — Provide explicit deletion of the selected entry and recalculate totals.
- **Rationale:** Users need a direct way to remove accidental input without falsifying it as a zero.
- **Acceptance checks:** Deleting one entry removes only that entry; remaining entries are preserved; daily totals and dependent insights recalculate; a zero-valued record is not used as a substitute for deletion.

### Decision 138 — Retried numeric submission
- **Approved decision:** A — Detect retries of the same logical submission and prevent double-counting.
- **Rationale:** Repeated taps or uncertain saves must not inflate a total, while legitimate separate entries must remain possible.
- **Acceptance checks:** Retrying the same logical submission is idempotent or safely rejected; a distinct intentional entry is retained separately; totals count each logical entry once.

### Decision 139 — Completion-only tracking semantics
- **Approved decision:** A — Store completion state explicitly and keep it distinct from numeric amounts and missing records.
- **Rationale:** Completion-only habits have different meaning from numeric measurement, including a numeric value of one or an absent record.
- **Acceptance checks:** Completion-only records use an explicit completion state; they are not silently represented as numeric quantity 1; missing records remain distinguishable.

### Decision 140 — Effort-aware tracking semantics
- **Approved decision:** A — Store the selected effort level explicitly, distinct from numeric quantity and completion-only state.
- **Rationale:** An effort rating communicates a qualitative level and should not be converted into an invented quantity.
- **Acceptance checks:** The selected effort value is preserved as its own tracking mode/value; it is not summed as a numeric amount; insights interpret it according to effort-aware rules.

### Decision 141 — Scheduled day with no tracking record
- **Approved decision:** A — Keep “no recorded completion” distinct from explicit zero and an explicitly recorded miss.
- **Rationale:** The app must not invent user activity or turn missing data into a measurement.
- **Acceptance checks:** A scheduled day with no record remains unrecorded; explicit zero remains a numeric record; an explicit miss, where supported, remains a separate state; no completion is inferred from previous days.

### Decision 142 — Numeric amount above target
- **Approved decision:** A — Accept valid amounts above target and show progress above 100% without capping the stored value.
- **Rationale:** Actual effort or quantity should remain truthful even when it exceeds the planned target.
- **Acceptance checks:** Values above target are saved unchanged; progress may exceed 100%; target display or insight calculations do not overwrite the recorded amount.

### Decision 143 — Negative value for a non-negative counter
- **Approved decision:** A — Reject a negative amount with a clear validation message and preserve existing entries.
- **Rationale:** Non-negative counters must not accept values that violate their defined domain, and invalid input must not corrupt valid data.
- **Acceptance checks:** Negative input is rejected before saving; the user receives a clear explanation; existing entries and totals remain unchanged.

## Still to specify

- Exact boundary calculation for 14 consecutive days, including clock/time-zone changes and first-launch tracking.
- Exact return-screen handling for active daily and weekly goals; the approved invariant is that app inactivity alone never changes their status, schedules, progress, or history and never automatically marks days missed or resets progress.
- Exact date-boundary and timezone handling for the 30-day no-active-goal period, including how goal status changes are timestamped. The trigger and reset rules are approved: start when the last active goal becomes completed, paused, archived, or deleted with no other active goal remaining; creating/reactivating a goal resets the timer. Repeat suppression is approved: each experience appears once per qualifying inactivity episode, and the one-month experience takes precedence when both triggers qualify.
- Final copy and interaction details for the choice screen, including how deliberately restarting a completed habit is presented; accessibility, localization, privacy-conscious wording, and acceptance tests. Optional reflection is approved and can be skipped. Any typed reflection should be retained locally as an unsaved draft until the user explicitly saves, deletes, or discards it. Discarding requires a confirmation prompt explaining that deletion is permanent and cannot be undone; delete only after explicit confirmation. If the user cancels, close the prompt and return to the reflection field with the draft unchanged. Leaving or closing the screen is not a discard action; app closure, phone restart, and returning later must not erase it. Keep the draft quietly in local storage and restore it directly into the text field when the same comeback reflection is reopened, without an extra confirmation step. In v1, do not add a dashboard card, notification, or Journal-list entry for the unsaved draft. If another comeback reflection would otherwise begin while an unresolved draft exists, reuse and restore that existing draft rather than creating another comeback-reflection draft; never silently replace its text. Do not auto-expire or automatically replace an unresolved draft. It must not become a journal entry until explicitly saved through the normal journal flow. After confirmed successful saving, remove the unsaved-draft copy; if saving fails or success is uncertain, retain the draft unchanged. The no-eligible-habit state is approved: omit resume and show create-new and continue options only.
