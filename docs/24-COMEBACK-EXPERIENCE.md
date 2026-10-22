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

## Still to specify

- Exact boundary calculation for 14 consecutive days, including clock/time-zone changes and first-launch tracking.
- Exact return-screen handling for active daily and weekly goals; the approved invariant is that app inactivity alone never changes their status, schedules, progress, or history and never automatically marks days missed or resets progress.
- Exact date-boundary and timezone handling for the 30-day no-active-goal period, including how goal status changes are timestamped. The trigger and reset rules are approved: start when the last active goal becomes completed, paused, archived, or deleted with no other active goal remaining; creating/reactivating a goal resets the timer. Repeat suppression is approved: each experience appears once per qualifying inactivity episode, and the one-month experience takes precedence when both triggers qualify.
- Final copy and interaction details for the choice screen, including how deliberately restarting a completed habit is presented; accessibility, localization, privacy-conscious wording, and acceptance tests. Optional reflection is approved and can be skipped. Any typed reflection should be retained locally as an unsaved draft until the user explicitly saves, deletes, or discards it. Discarding requires a confirmation prompt explaining that deletion is permanent and cannot be undone; delete only after explicit confirmation. If the user cancels, close the prompt and return to the reflection field with the draft unchanged. Leaving or closing the screen is not a discard action; app closure, phone restart, and returning later must not erase it. Keep the draft quietly in local storage and restore it directly into the text field when the same comeback reflection is reopened, without an extra confirmation step. In v1, do not add a dashboard card, notification, or Journal-list entry for the unsaved draft. If another comeback reflection would otherwise begin while an unresolved draft exists, reuse and restore that existing draft rather than creating another comeback-reflection draft; never silently replace its text. Do not auto-expire or automatically replace an unresolved draft. It must not become a journal entry until explicitly saved through the normal journal flow. After confirmed successful saving, remove the unsaved-draft copy; if saving fails or success is uncertain, retain the draft unchanged. The no-eligible-habit state is approved: omit resume and show create-new and continue options only.
