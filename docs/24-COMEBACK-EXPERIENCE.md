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
- **Offline backup and restore:** Users can create a portable local backup file and restore it on another phone, so moving devices does not require an account or internet connection. The backup/restore format and exact data coverage must be specified before implementation. Restoring must not silently overwrite existing data; conflict and replacement behavior remains to be decided.
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

## Still to specify

- Exact boundary calculation for 14 consecutive days, including clock/time-zone changes and first-launch tracking.
- Exact return-screen handling for active daily and weekly goals; the approved invariant is that app inactivity alone never changes their status, schedules, progress, or history and never automatically marks days missed or resets progress.
- Exact date-boundary and timezone handling for the 30-day no-active-goal period, including how goal status changes are timestamped. The trigger and reset rules are approved: start when the last active goal becomes completed, paused, archived, or deleted with no other active goal remaining; creating/reactivating a goal resets the timer. Repeat suppression is approved: each experience appears once per qualifying inactivity episode, and the one-month experience takes precedence when both triggers qualify.
- Final copy and interaction details for the choice screen, including how deliberately restarting a completed habit is presented; accessibility, localization, privacy-conscious wording, and acceptance tests. Optional reflection is approved and can be skipped. Any typed reflection should be retained locally as an unsaved draft until the user explicitly saves, deletes, or discards it. Discarding requires a confirmation prompt explaining that deletion is permanent and cannot be undone; delete only after explicit confirmation. If the user cancels, close the prompt and return to the reflection field with the draft unchanged. Leaving or closing the screen is not a discard action; app closure, phone restart, and returning later must not erase it. Keep the draft quietly in local storage and restore it directly into the text field when the same comeback reflection is reopened, without an extra confirmation step. In v1, do not add a dashboard card, notification, or Journal-list entry for the unsaved draft. If another comeback reflection would otherwise begin while an unresolved draft exists, reuse and restore that existing draft rather than creating another comeback-reflection draft; never silently replace its text. Do not auto-expire or automatically replace an unresolved draft. It must not become a journal entry until explicitly saved through the normal journal flow. After confirmed successful saving, remove the unsaved-draft copy; if saving fails or success is uncertain, retain the draft unchanged. The no-eligible-habit state is approved: omit resume and show create-new and continue options only.
