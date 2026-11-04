# Phase 1 Human-Intervention Test Register

**Project:** Dayseed  
**Phase:** 1 — Domain, data-integrity and historical-record foundation  
**Status:** Prepared for later execution; **NOT RUN**  
**Execution window:** After the planned Stage B implementation phases are complete, as part of one consolidated human/device validation session.  
**Authority:** Phase 1 requirements in the canonical root `implementation_plan.md`, Phase 1 Implementation Plan Revision 7, and owner-approved decisions D-216 through D-219 and U-1 through U-13.

## 1. Purpose and execution rule

This register captures Phase 1 checks that cannot be adequately established by unit tests, repository tests, migration fixtures, or static source inspection alone. They require a real Android installation, interaction with the finished UI, observation of Android lifecycle behaviour, or owner review.

**Do not run these checks immediately after Phase 1.** Add them to the consolidated human-validation session and run them after the planned Stage B phases are integrated. Some cases depend on screens or system workflows delivered in later phases; those dependencies are stated below.

This deferral applies only to human/device checks. Gemini must still run automated unit, repository, migration, backup/restore, and regression tests after each implementation phase. Automated tests added in the Phase 1 remediation must also be executed before Phase 1 is treated as technically verified.

This document is a test plan, not evidence that any test has passed. Stage C independent validation remains a separate gate requiring its own explicit authorisation after Stage B implementation is complete.

## 2. Safety and evidence rules

- Use synthetic test habits and synthetic journal/record data. Do not use the owner's real personal history for destructive migration, restore, deletion, or conflict tests.
- Use a disposable test installation/device profile. Before any destructive scenario, create a recoverable test backup.
- Record the exact Git commit SHA and APK/build identifier under test.
- Record device model, Android version, app version, date/time, steps, expected result, actual result, and evidence.
- Use only these outcomes: **PASS**, **FAIL**, **BLOCKED**, or **NOT RUN**. A blocked test is not a pass.
- Capture screenshots or screen recordings for visible states. Capture logs only where useful; never include Journal text, habit notes, backup passwords, tokens, or other private data.
- If a scenario risks losing data, stop and restore the disposable fixture. Do not improvise a destructive recovery.
- A screenshot of a design mock-up is not evidence of actual device behaviour.

## 3. Phase 1 human-intervention cases

### P1-H01 — Upgrade an existing v2 installation to v3 without losing history

**Why human intervention is needed:** A real app upgrade exercises Android installation and app-private database continuity in addition to the automated migration fixture.

**Dependency:** A reproducible v2 test installation or a documented way to seed a synthetic v2 database on the test device.

**Procedure:**
1. Install the v2 test build and create synthetic habits covering Quantity, Duration, and Simple check-off/legacy-compatible records.
2. Add multiple sessions on one date, an explicit zero record, a completed outcome, date notes, and at least two effective-dated habit configurations.
3. Record the visible totals and history for later comparison.
4. Install the v3 build as an upgrade over the same package without clearing app data.
5. Open Dayseed and inspect the same habits and historical dates.

**Expected:** The upgrade completes without a reset or silent loss. Habit IDs, historical entries/outcomes, notes, effective-dated configuration, and visible totals remain consistent. Any migration block is explicit and leaves the original data recoverable.

**Evidence:** Before/after screenshots, device/app versions, APK identifiers, commit SHA, and migration outcome.

### P1-H02 — Data persistence after force-stop and process death

**Why human intervention is needed:** This verifies the installed Android app and its process lifecycle, not merely an in-memory repository test.

**Procedure:**
1. Create synthetic habits and save several sessions, including an explicit zero and a note.
2. Navigate away and return; verify the saved state.
3. Force-stop Dayseed from Android App Info or the device's app controls.
4. Relaunch Dayseed.
5. Where practical, repeat after Android reclaims the app process or after a device restart.

**Expected:** Committed records, effective configuration, streak/Grove history, and settings reload from persistent storage. No duplicate sessions appear and no saved record silently disappears.

**Evidence:** Before/after screenshots and exact device/build details.

### P1-H03 — Same-day configuration protection after real activity

**Procedure:**
1. Create a scheduled habit and record a qualifying entry.
2. Attempt to change an evaluation-affecting target or schedule with an Apply-today action.
3. Repeat with an explicit zero record.
4. On a separate test habit, create a REST_DAY exception without recording activity, then attempt the same configuration edit.

**Expected:** Existing activity, including explicit zero, protects today's historical evaluation and routes the change to the approved later effective date. A REST_DAY exception alone does not count as qualifying activity and does not trigger the activity lock.

**Evidence:** Screenshots of the offered action, saved configuration, and selected-date result.

### P1-H04 — Weekly target semantics and week-start history

**Procedure:**
1. Create a weekly Quantity or Duration habit and record multiple sessions across several dates.
2. Create a weekly Simple check-off habit and complete it on distinct dates.
3. Create a weekly session-count habit and record multiple sessions on one date.
4. Inspect the displayed totals and qualifying progress.
5. Change the configured week-start day in a disposable profile.
6. Inspect the completed historical period, the current transition period, and the first period using the new boundary.
7. Change a weekly target mid-week; separately attempt a unit/measurement-mode change.

**Expected:** Quantity/Duration uses summed compatible units; days/week counts distinct qualifying dates; sessions/week counts sessions. A completed historical week is not repartitioned. The transition interval follows the approved neutral rule. The current week's target/metric snapshot is not retroactively changed; unit/metric-mode changes take effect only at the approved weekly boundary.

**Evidence:** Screenshots showing dates, week labels, totals, and effective target values.

### P1-H05 — One-off REST_DAY behaviour

**Procedure:**
1. On an eligible scheduled date, choose **Take a rest day** for one habit.
2. Inspect Today, History/Calendar, streak display, and weekly target progress.
3. Confirm no activity entry or completed outcome was created.
4. Attempt REST_DAY on an unscheduled date and on a past/future date.
5. Log real activity, including an explicit zero in a separate run, on a date that currently has REST_DAY.
6. Add a date note without logging activity.

**Expected:** REST_DAY is accepted only under U-1 eligibility rules; it is neutral, holds the relevant streak, does not create activity, does not inflate active-day/check-in totals, and does not reduce a weekly target. Real activity transactionally clears the exception; a note alone does not.

**Evidence:** Screenshots of the date state and totals before/after each action.

### P1-H06 — Individual streak independence and Grove lifecycle

**Procedure:**
1. Create at least two habits with different cadences.
2. Record full progress, positive partial progress, explicit zero, a missed scheduled opportunity, and a REST_DAY in separate controlled scenarios.
3. Pause one habit, resume it, and later qualify a new run.
4. Archive one habit and mark another Completed.
5. Inspect the active streak indicator and the Grove/history view.
6. Reopen the app and inspect the same records.

**Expected:** Each habit has an independent run. Partial progress and rest follow the approved hold rules; explicit zero is neutral; a pause closes the run with PAUSED and a later qualifying run starts separately. Archive and completion retain distinct terminal histories. Mixed-cadence comparison displays the agreed elapsed span with an explicit unit. Grove qualification and unqualified short-run history remain distinct.

**Dependency:** Habit Detail/Grove UI from Phase 2.

**Evidence:** Screenshots and a date-by-date record of the scenario.

### P1-H07 — Backdated correction does not cross lifecycle boundaries

**Procedure:**
1. Build a synthetic history with a missed gap and a separate PAUSED, ARCHIVED, or COMPLETED closure.
2. Use the finished record-edit flow to add or correct a backdated entry around the missed gap.
3. Inspect the affected run identities and Grove history.
4. Attempt a correction that would appear to bridge a lifecycle closure.

**Expected:** Approved MISSED-gap healing/recalculation behaves deterministically. A backdated record never reopens or bridges a PAUSED, ARCHIVED, or COMPLETED boundary. Unrelated habits and run histories remain unchanged.

**Dependency:** Phase 2 record correction and Habit Detail/Grove UI.

**Evidence:** Before/after screenshots and the exact synthetic timeline.

### P1-H08 — Portable backup compatibility and full v3 round-trip

**Procedure:**
1. Export a v3 portable backup from a disposable installation containing the Phase 1 data families.
2. If a supported v2 backup fixture is available, restore it into the v3 app using the supported restore flow.
3. Restore the v3 backup into a clean disposable installation.
4. Exercise supported Replace/Merge conflict choices with synthetic collisions.
5. Verify the restored habits, effective versions, entries, outcomes, notes, REST_DAY exceptions, streak runs, and week-start history.

**Expected:** Supported v2 JSON maps explicitly into v3. A v3 round-trip preserves IDs and supported data. Unsupported/corrupt input fails closed; no partial restore is presented as successful. Conflict resolution does not silently delete unrelated activity.

**Dependency:** Phase 4 Android file-picker/backup UI. The data-mapping and rollback assertions remain automated-test responsibilities too.

**Evidence:** Sanitised backup metadata, before/after screenshots, restore outcome, and commit/build details. Never share backup passwords or private payload contents.

### P1-H09 — Permanent habit deletion and selective Undo-token invalidation

**Procedure:**
1. Create a synthetic habit with versions, entries, date notes, REST_DAY/streak history, and a linked Journal entry.
2. Delete a session on a second habit so an 8-second session `Undo` snackbar is active, or delete a session on the habit being deleted.
3. Permanently delete the first habit through `Habit Detail` or `Edit Habit` after confirming the permanent deletion dialog.

**Expected:** Permanent habit deletion requires explicit confirmation, removes the habit and its tracking history permanently without offering a habit-deletion Undo snackbar, preserves the independent Journal entry text (setting `linked_habit_id = null`), invalidates any pending session Undo token belonging to the deleted habit, and leaves any pending session Undo token belonging to another habit intact.

**Dependency:** Phase 2 lifecycle/detail UI.

**Evidence:** Before/after counts and screenshots. Collision cases that cannot be safely triggered through the UI remain automated tests.

## 4. Tests that should remain automated

Do not replace these with manual checks:

- Exact BigInteger arithmetic, boundary ratios, very large totals, and display formatting.
- Every SQLite CHECK, FK, index, trigger, migration row-count/primary-key comparison, rollback, and integrity assertion.
- Malformed metadata and invalid/out-of-range legacy values.
- Deterministic streak-engine permutations, tie-breaks, run identity, and all U-1 through U-13 edge cases.
- Backup schema/format-version combinations, payload validation, atomic rollback, conflict authorization, and deletion collision guards.
- Race conditions, transaction boundaries, and idempotency.

Human review supplements these tests; it does not replace them.

## 5. Consolidated execution gate

Run P1-H01 through P1-H09 together with the human/device checks added by later phases only after:
1. All planned Stage B implementation phases are complete and committed.
2. The full automated suite and debug build have current, inspectable evidence.
3. The exact candidate commit and test APK are frozen for validation.
4. Stage C independent validation has been explicitly authorised.

At that point, execute one coordinated session, record every case as PASS/FAIL/BLOCKED/NOT RUN, triage failures, and repeat affected cases against the corrected candidate. Do not describe Stage C as passed while any required case is failed, blocked, or not run without an explicitly accepted disposition.

---

## 6. Phase 2 human-intervention cases (Status: `NOT RUN`)

All Phase 2 human/device cases below are prepared for the consolidated human/device validation session after Stage B implementation is complete. None of these cases have been executed during Stage B (`NOT RUN`).

### P2-H01 — Five-destination bottom navigation and full back-stack behaviour

- **Status:** `NOT RUN`
- **Procedure:**
  1. Launch Dayseed on a physical Android device or interactive emulator.
  2. Tap each bottom-bar destination in order (`Today`, `Habits`, `Journal`, `Insights`, `Settings`).
  3. From `Habits` (with a filter and search query active), open `Habit Detail`, then open `Record Session`, then press system Back twice.
  4. From `Insights` (with a historical date selected), open `Living Grove`, then open `Habit Detail`, then press system Back twice.
- **Expected:** Each destination loads without layout jump. Returning from secondary routes restores the exact parent screen, selected date, filter, search query, and scroll position without resetting to `Today`.

### P2-H02 — Create Quantity, Duration, and Simple check-off habits

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Create Habit` from `Today` or `Habits`.
  2. Create one `Quantity` habit (`pages`), one `Duration` habit (`min`), and one `Simple check-off` habit.
  3. Verify that `EFFORT_AWARE` (`Tiny / Usual / Extra`) is not offered for new habits.
- **Expected:** All three habits save cleanly with accurate target/schedule summaries, while legacy `EFFORT_AWARE` habits remain editable when opened from existing data.

### P2-H03 — Edit habit before and after today's activity (`D-194` & `U-12`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. Edit a habit's target before logging any activity today and save.
  2. Log activity for that habit today, then reopen `Edit Habit` and change the target or schedule.
  3. Observe the `Apply today` vs `Apply tomorrow` prompt (`D-194`).
  4. Edit a weekly habit's unit or measurement mode mid-week and observe the `U-12` weekly freeze notice.
- **Expected:** Editing after today's activity protects today's record and defaults safely to `Apply tomorrow`; mid-week weekly unit/mode changes defer to the next week boundary while keeping the current week's start-of-week target frozen.

### P2-H04 — Record multiple sessions, explicit zero, per-entry note, and habit/date note

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Record Session` for a `Quantity` or `Duration` habit on `Today`.
  2. Tap `0 (Honest check-in)` and save; verify the `Zero logged` state.
  3. Add two positive sessions with per-entry notes on the same date, plus a habit/date note in `Habit Detail`.
- **Expected:** Individual sessions are listed separately, sum accurately toward the daily/weekly target, and keep per-entry notes distinct from the habit/date note and the general Journal.

### P2-H05 — Edit and delete one session with 8-second Undo

- **Status:** `NOT RUN`
- **Procedure:**
  1. On a date with two recorded sessions, edit one session's value and note.
  2. Delete one session and tap `Undo` on the 8-second snackbar within 8 seconds.
  3. Delete one session again, wait more than 8 seconds for snackbar expiry, and verify final totals.
- **Expected:** Sibling sessions remain untouched; Undo within 8 seconds restores the exact deleted session and recomputes totals/streaks; after 8 seconds the deletion remains committed.

### P2-H06 — `Today` scheduled vs `Not scheduled today` habits

- **Status:** `NOT RUN`
- **Procedure:**
  1. Configure one habit scheduled for today's weekday and one habit scheduled only for another weekday.
  2. Inspect `Today` and expand the `Not scheduled today` section.
  3. Log bonus activity on the unscheduled habit.
- **Expected:** Scheduled habits appear in the primary list; active unscheduled habits appear inside the collapsed `Not scheduled today` section and can record bonus activity without turning unscheduled days into missed opportunities.

### P2-H07 — `Take a rest day` on eligible and ineligible dates

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Habit Detail` for a scheduled habit on `Today` with no activity logged yet; tap `Take a rest day`.
  2. Select a past date in `Habit Detail` and inspect the rest-day control area.
  3. Log real activity on `Today` after taking a rest day.
- **Expected:** `Take a rest day` is available only on `Today` when scheduled and unlogged, holds the active streak neutrally, is not offered retroactively on past dates, and is automatically cleared if real activity is logged on `Today`.

### P2-H08 — Longest active streak indicator and Living Grove inspection

- **Status:** `NOT RUN`
- **Procedure:**
  1. Create daily and weekly habits with active streaks.
  2. Inspect the `Today` longest active streak indicator and verify explicit unit labels (`days` vs `weeks`).
  3. Tap the indicator card or `Grove` button to open the complete `Living Grove` (all habits), and tap the secondary `Habit` button (`today_longest_streak_habit_btn`) to open the lead habit's `Habit Detail`.
- **Expected:** The indicator never shows a unitless mixed-cadence number; tapping the card or `Grove` button opens the complete `Living Grove` across all habits (`selectedHabitId = null`), while the secondary `Habit` button opens the lead habit's `Habit Detail`.

### P2-H09 — Pause, resume, complete, archive, and permanently delete habits

- **Status:** `NOT RUN`
- **Procedure:**
  1. In `Habit Detail`, test `Pause`, `Resume`, `Mark as Completed`, and `Archive`.
  2. Filter by each lifecycle state in the `Habits` directory.
  3. Permanently delete a habit that has a linked Journal entry and confirm the permanent deletion dialog.
- **Expected:** Lifecycle changes preserve Grove history appropriately; permanent deletion requires confirmation (`"This action is permanent and cannot be undone."`), preserves the independent Journal entry text (setting `linked_habit_id = null`), does not offer a snackbar Undo action for the permanently deleted habit, and invalidates any pending session Undo token belonging to the deleted habit while preserving unrelated Undo tokens.

### P2-H10 — Manual habit ordering in `Habits` and `Today`

- **Status:** `NOT RUN`
- **Procedure:**
  1. In `Habits`, use the `Move up` and `Move down` controls to reorder habits.
  2. Switch to `Today`, force-stop/relaunch the app, and inspect habit order.
- **Expected:** Reordering persists contiguous `sort_order` values (`0..N-1`) and is reflected consistently on `Today` and across app restarts.

### P2-H11 — Small-screen layout (`320dp–360dp` width)

- **Status:** `NOT RUN`
- **Procedure:**
  1. Set emulator/device display width to `320dp–360dp`.
  2. Inspect `Today`, `Habits`, `Habit Detail`, `Create/Edit Habit`, `Record Session`, and `Living Grove`.
- **Expected:** No clipped controls, overlapping badges, or unreachable buttons; chip rows wrap cleanly and primary actions remain visible.

### P2-H12 — `200%` font scale

- **Status:** `NOT RUN`
- **Procedure:**
  1. Set Android system font scale to `200%`.
  2. Traverse all Phase 2 screens, cards, progress badges, dual quotes, and dialogs.
- **Expected:** Text reflows cleanly without truncation or overlap; `Record Session` and `Create/Edit Habit` scroll vertically with save actions accessible.

### P2-H13 — Keyboard-open layout in `Create/Edit Habit` and `Record Session`

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Create/Edit Habit` and focus the name, unit, target, and description inputs with the soft keyboard visible.
  2. Open `Record Session` and focus the numeric amount and session note inputs with the soft keyboard visible.
- **Expected:** Focused fields and primary Save/Cancel actions remain reachable above the soft keyboard (`imePadding()`).

### P2-H14 — TalkBack traversal of `Today`, `Habits`, `Habit Detail`, `Record Session`, and `Living Grove`

- **Status:** `NOT RUN`
- **Procedure:**
  1. Enable Android TalkBack.
  2. Swipe through `Today` (longest streak indicator, habit cards, `Collected moments`), `Habits` (filters, search, reorder buttons), `Habit Detail` (all 6 sections, Grove trees, short runs), `Record Session`, and `Living Grove`.
- **Expected:** Every interactive control has a clear spoken label, state, and action; decorative canvases are merged into parent semantics; progress bands and tree states are never conveyed by colour alone.

### P2-H15 — Reduced-motion behaviour

- **Status:** `NOT RUN`
- **Procedure:**
  1. Enable Android system `Remove animations` (`ANIMATOR_DURATION_SCALE = 0f`) or Dayseed's reduced-motion setting.
  2. Navigate across tabs, expand/collapse `Not scheduled today`, log progress, and inspect `Living Grove` / dormant tree transitions.
- **Expected:** Decorative sway and transition animations are disabled in favour of calm, instant state updates.

### P2-H16 — Light, dark, and daypart theme contrast on all Phase 2 screens

- **Status:** `NOT RUN`
- **Procedure:**
  1. Inspect all Phase 2 screens under `Soft Botanical` dayparts (`Morning`, `Day`, `Evening`, `Night`) and fixed `Light` / `Dark` modes.
- **Expected:** Text, badges, buttons, and botanical illustrations maintain legible contrast and calm visual hierarchy across all modes.

### P2-H17 — Process death and relaunch across Phase 2 flows

- **Status:** `NOT RUN`
- **Procedure:**
  1. Log sessions, take a rest day, reorder habits, and acknowledge a dormant Grove transition.
  2. Force-stop or kill the app process and relaunch.
- **Expected:** All committed entries, outcomes, notes, `REST_DAY` exceptions, streak runs, habit sort orders, and dormant transition acknowledgments persist without duplication or loss.

---

## 7. Phase 2 targeted and final remediation human-intervention cases (Status: `NOT RUN`)

The following nine human/device validation cases cover the Phase 2 targeted remediation items (`Corrections A–D`) and final audit remediation items (`F-01` through `F-06`). None of these cases have been executed on a physical device or interactive emulator session during Stage B (`NOT RUN`).

### P2-C01 — `Habit Detail -> Edit -> Save` single-pop return path and deletion navigation

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open any existing habit's `Habit Detail` screen from `Today` or `Habits`.
  2. Tap the `Edit` action to open `Create/Edit Habit`.
  3. Change the habit description or name and tap `Save`.
  4. Re-open `Edit` from `Habit Detail` and tap `Cancel` / system Back.
  5. On a disposable synthetic habit, open `Habit Detail` from `Habits`, tap `Edit`, and delete the habit.
- **Expected:** Saving or cancelling from `Habit Detail -> Edit` pops only `Create/Edit Habit` once and returns directly to `Habit Detail` for that same habit (showing the updated fields), rather than double-popping to `Today` or `Habits`. Deleting a habit from `Habit Detail -> Edit` removes both the editor and the deleted habit's detail route and returns cleanly to `Habits`.

### P2-C02 — Failed save keeps editor open with inline error banner and preserves form inputs

- **Status:** `NOT RUN`
- **Procedure:**
  1. Log any activity (or `0` check-in) for a habit on `Today`.
  2. Open `Edit Habit`, change the target value or unit, select `Apply today` in the `D-194` effective-timing prompt, and tap `Save`.
  3. Inspect the editor state, then select `Apply tomorrow` and tap `Save`.
- **Expected:** The `Apply today` save is rejected without any silent fallback to `Apply tomorrow`; `Create/Edit Habit` remains open with all entered fields preserved and displays the inline error banner (`habit_save_error_banner`) explaining why today's configuration is locked. Selecting `Apply tomorrow` clears the error banner and saves cleanly.

### P2-C03 — Explicit zero check-in locks today's configuration (`D-194`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. On `Today`, record an explicit `0` check-in (`0 (Honest check-in)`) for a `Quantity` or `Duration` habit that has no other entries today.
  2. Open `Edit Habit` for that habit on the same day.
  3. Inspect the `5. Reminder & Effective Timing` section and save a target change with `Apply tomorrow`.
- **Expected:** Recording an explicit `0` triggers the `D-194` `"You already logged this habit today"` prompt (`Apply today` / `Apply tomorrow`) with `Apply tomorrow` pre-selected. Saving with `Apply tomorrow` creates a version effective tomorrow while leaving today's effective configuration and `0` entry unchanged.

### P2-C04 — Weekly session habit counts positive sessions (`unitCode = "sessions"`) across log, zero, edit, delete, and Undo

- **Status:** `NOT RUN`
- **Procedure:**
  1. Create a weekly `Quantity` habit with target `3` and unit `sessions`.
  2. Log a session with value `20` on day 1 and another session with value `20` on day 2.
  3. Inspect `Today` and `Habit Detail`.
  4. Record an explicit `0` check-in on day 3.
  5. Edit one positive session's value, delete one positive session, and tap `Undo` within 8 seconds.
- **Expected:** Progress displays `2 / 3 sessions` (`66%`, `Halfway`), not `40 / 3`. The explicit `0` check-in remains recorded as `Checked in (0 recorded)` on day 3 while keeping weekly progress at `2 / 3 sessions`. Deleting a positive session reduces weekly progress to `1 / 3 sessions`, and tapping `Undo` restores `2 / 3 sessions`.

### P2-C05 — Flexible weekly habit partition on `Today` before and after weekly target satisfaction

- **Status:** `NOT RUN`
- **Procedure:**
  1. Create a flexible weekly habit (`3 sessions / week`, preferred weekdays `Mon / Wed / Fri`).
  2. View `Today` on a non-preferred weekday (e.g. `Tue` or `Thu`) while weekly progress is `< 3 / 3`.
  3. Complete the 3rd session so weekly progress reaches `3 / 3` (`Goal met`), then view `Today` on a non-preferred weekday (`Thu`) and on a preferred weekday (`Fri`).
  4. Expand `"Not scheduled today"` on `Thu` and log a bonus 4th session.
- **Expected:** Before reaching `3 / 3`, the habit appears under `"Scheduled today"` on every day of the week (preferred and non-preferred). After reaching `3 / 3`, it moves to `"Not scheduled today"` on non-preferred days (`Thu`), remains under `"Scheduled today"` on preferred days (`Fri`), and still permits bonus session logging (`4 / 3 sessions`, `133%`, `Extra`).

### P2-C06 — Simplified `Today` habit cards and `Tended today` counter (`F-01`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. On `Today`, inspect scheduled habit cards across `Simple check-off`, `Quantity`, and `Duration` modes.
  2. Log a positive session on one habit, take a rest day on a second habit, record an explicit `0` on a third habit, and satisfy a flexible weekly habit's target earlier in the week.
  3. Inspect the `Tended today` counter (`today_tended_count_text`) and the compact session count summary (`habit_today_sessions_summary_<habitId>`).
- **Expected:** `Today` habit cards do not display a 3-dot lifecycle/edit menu, inline custom amount field, inline date-note editor, or individual session edit/delete rows; tapping the card header opens `Habit Detail` and tapping `Record` opens `Record Session` for a new entry. The `Tended today` counter counts the positive-entry habit, the `REST_DAY` habit, and the already-satisfied flexible weekly habit (at most once per scheduled habit), and excludes the explicit-zero-only habit.

### P2-C07 — `Record Session` governing-version resolution across historical dates and mid-week frozen weekly configs (`F-03`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. Edit a daily habit's target or unit effective today, then open `Record Session` and switch the selected date to an earlier date governed by the prior version.
  2. Edit a weekly habit's unit or tracking mode mid-week (deferred to the next week boundary under `U-12 Option B`), then open `Record Session` for a date in the current week.
- **Expected:** `Record Session` resolves the effective-date `HabitVersion` for daily habits and the frozen start-of-week `HabitVersion` for weekly habits, updating the header subtitle, unit label, default new-session value (`1` for weekly `"sessions"` habits), quick chips, and daily/weekly preview totals accordingly.

### P2-C08 — Clear preferred weekdays, weekly semantics explanation, and strict `HH:mm` reminder time validation (`F-04`, `F-05`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Create/Edit Habit`, select `Per week (flexible)` (`FLEXIBLE_WEEKLY_TARGET`), select two preferred weekdays, then tap `Clear` (`habit_clear_preferred_weekdays_btn`) or deselect all weekdays and save.
  2. Reopen `Edit Habit` for that habit and inspect the preferred weekday chips and the weekly target semantics explanation (`habit_weekly_semantics_explanation`).
  3. Enable reminders, enter `9:5` or `24:00`, tap `Save`, then edit any form field and enter `08:30`.
- **Expected:** Saving with zero preferred weekdays persists `preferredWeekdaysMask = null` and reloads with no weekdays selected. Malformed reminder times (`9:5`, `24:00`, `12:60`) block saving with `habit_reminder_time_error`; editing any field clears stale save errors, and `08:30` saves cleanly.

### P2-C09 — `Today` longest active streak indicator opens complete Living Grove (`F-06`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. With two or more habits having active streaks/Grove entries, view `Today`.
  2. Tap the longest active streak indicator card (`today_longest_streak_indicator`) or `Grove` button (`today_longest_streak_open_grove_btn`).
  3. Return to `Today` and tap the secondary `Habit` button (`today_longest_streak_habit_btn`).
- **Expected:** Tapping the indicator card or `Grove` button opens `Living Grove` unfiltered (`selectedHabitId = null`, showing all habits' saplings and trees); tapping the secondary `Habit` button opens `Habit Detail` for the lead streak habit.



