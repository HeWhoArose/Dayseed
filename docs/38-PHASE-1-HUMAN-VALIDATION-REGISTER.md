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

- **Status:** `NOT RUN`

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

- **Status:** `NOT RUN`

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

- **Status:** `NOT RUN`

**Procedure:**
1. Create a scheduled habit and record a qualifying entry.
2. Attempt to change an evaluation-affecting target or schedule with an Apply-today action.
3. Repeat with an explicit zero record.
4. On a separate test habit, create a REST_DAY exception without recording activity, then attempt the same configuration edit.

**Expected:** Existing activity, including explicit zero, protects today's historical evaluation and routes the change to the approved later effective date. A REST_DAY exception alone does not count as qualifying activity and does not trigger the activity lock.

**Evidence:** Screenshots of the offered action, saved configuration, and selected-date result.

### P1-H04 — Weekly target semantics and week-start history

- **Status:** `NOT RUN`

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

- **Status:** `NOT RUN`

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

- **Status:** `NOT RUN`

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

- **Status:** `NOT RUN`

**Procedure:**
1. Build a synthetic history with a missed gap and a separate PAUSED, ARCHIVED, or COMPLETED closure.
2. Use the finished record-edit flow to add or correct a backdated entry around the missed gap.
3. Inspect the affected run identities and Grove history.
4. Attempt a correction that would appear to bridge a lifecycle closure.

**Expected:** Approved MISSED-gap healing/recalculation behaves deterministically. A backdated record never reopens or bridges a PAUSED, ARCHIVED, or COMPLETED boundary. Unrelated habits and run histories remain unchanged.

**Dependency:** Phase 2 record correction and Habit Detail/Grove UI.

**Evidence:** Before/after screenshots and the exact synthetic timeline.

### P1-H08 — Portable backup compatibility and full v3 round-trip

- **Status:** `NOT RUN`

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

- **Status:** `NOT RUN`

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
  3. Enable reminders, enter `9:05`, `9:5`, or `24:00`, tap `Save`, then edit any form field and enter `08:30`.
- **Expected:** Saving with zero preferred weekdays persists `preferredWeekdaysMask = null` and reloads with no weekdays selected. Malformed reminder times (`9:05`, `9:5`, `24:00`, `12:60`) block saving with `habit_reminder_time_error`; editing any field clears stale save errors, and `08:30` saves cleanly.

### P2-C09 — `Today` longest active streak indicator opens complete Living Grove (`F-06`)

- **Status:** `NOT RUN`
- **Procedure:**
  1. With two or more habits having active streaks/Grove entries, view `Today`.
  2. Tap the longest active streak indicator card (`today_main_streak_indicator`) or `Grove` button (`today_open_grove_btn`).
  3. Return to `Today` and tap the secondary `Habit` button (`today_longest_streak_habit_btn`).
- **Expected:** Tapping the indicator card (`today_main_streak_indicator`) or `Grove` button (`today_open_grove_btn`) opens `Living Grove` unfiltered (`selectedHabitId = null`, showing all habits' saplings and trees); tapping the secondary `Habit` button (`today_longest_streak_habit_btn`) opens `Habit Detail` for the lead streak habit.





## 8. Phase 2.5 human-intervention cases (appearance, reminders & backup file workflows)

Added 2026-11-04 (Phase 2.5, decisions D-220–D-224). All cases **NOT RUN**; they require a physical device or owner observation and are added to the consolidated human-validation session.

### P25-H01 — Verify the fifth appearance option on device

- **Status:** `NOT RUN`

**Procedure:** Select "Dark + Time-of-Day Artwork" in Settings → Appearance & rhythm; observe the app across the four daypart boundaries (or by changing system time on a test device): the palette stays dark all day, the Morning sprig / Day branch / Evening hills / Night moon each appear in their local-time windows, and transitions remain gradual; relaunch the app and confirm the selection persists; confirm `SOFT_BOTANICAL_AUTO`, `SYSTEM`, `LIGHT`, and fixed `DARK` behave exactly as before; observe the Appearance preview updating over time without leaving Settings.
**Expected:** Dark palette + time-cycling artwork simultaneously; no illustration is locked to the Night moon; preview tracks the Today screen's clock; no duplicate Today screen ever rendered.
**Dependencies:** Physical device or emulator; ability to advance device time.

### P25-H02 — Measure illustration contrast on the dark canvas

- **Status:** `NOT RUN`

**Procedure:** On the `DARK_DAYPART_ART` theme, render each of the four illustrations against the dark canvas on a physical screen; compare visual legibility with the native `SOFT_BOTANICAL_AUTO` night leg; check text over the artwork stays readable; confirm reduced-motion behaviour of theme colour shifts.
**Expected:** All four illustrations remain clearly visible without weakening text readability.
**Dependencies:** Owner visual judgment on a real display.

### P25-H03 — Reminder delivery end-to-end on a physical device (gating check)

- **Status:** `NOT RUN`

**Procedure:** On a device, build a debug APK with at least one active habit reminder due within a few minutes, Android notification channel available; grant `POST_NOTIFICATIONS`; wait for the intended window (± the inexact delivery window); observe the delivered notification; inspect `reminder_deliveries` for the fresh `DELIVERED` record; fire/repeat the same window to confirm no duplicate notification; then revoke permission and repeat (expect no notification and no ledger delivery record).
**Expected:** Exactly one notification per due reminder; idempotency holds across restart/reschedule; quiet hours and the 3/day cap hold on device.
**Rules:** The Settings activation switch may only be enabled after this case **PASSES**. Until then switches stay disabled.

### P25-H04 — Reboot / time-change / Doze reminder recovery

- **Status:** `NOT RUN`

**Procedure:** Schedule a reminder, reboot the device (and separately change system time/timezone), verify the reminder still rings at the intended time within the inexact window; with battery optimisation/Doze active, observe any delay is bounded and copy never promised an exact minute.
**Expected:** Reminders re-registered automatically by the system-event receiver + reconcile worker; delayed-but-delivered outcome is acceptable and never reported as exact-time delivery.

### P25-H05 — Permission-denied UI honesty

- **Status:** `NOT RUN`

**Procedure:** Deny notifications for Dayseed in system settings; open Settings → Notifications & reminders; confirm the panel shows the real denied state, request/route buttons work, and global switches remain disabled with the "delivery not yet available" copy; confirm core app use (habit creation, logging, Journal) is unaffected.
**Expected:** No control implies delivery works while permission is denied.

### P25-H06 — SAF backup save with read-back verification on device

- **Status:** `NOT RUN`

**Procedure:** Settings → Back up or restore → "Save backup to a file (choose location)" with and without a password; pick a Files/Drive/USB document provider; verify the file appears in the chosen location and can be re-opened; attempt a failing destination (write-protected) and confirm honest failure copy without a success claim.
**Expected:** Success only after verified read-back; failure copy accurate; repository backup format unchanged (`dayseed_portable_backup`, format v2).

### P25-H07 — SAF backup restore with metadata on device

- **Status:** `NOT RUN`

**Procedure:** "Choose a backup file…" → select the saved file → confirm the pre-restore metadata line (date, format/schema version, category counts, password flag) matches the file; restore Merge and Replace on a disposable installation; try wrong password, corrupt file, non-Dayseed JSON, and an empty file.
**Expected:** Correct metadata before any mutation; safety backup created before restore; Merge/Replace + conflicts unchanged; unsupported/corrupt files never mutate data and the source stays untouched.

## 9. Phase 1–5 remediation human-intervention cases (2026-11-05..2026-11-08, decisions D-225–D-231)

Added 2026-11-05 (and extended through D-231 on 2026-11-08) for the reminder/backup remediation. Automation covers the cross-connection daily cap, atomic posting-ownership claim (`POSTING_CLAIM:<uuid>`), receiver-vs-sweep lease coordination, failed-post `POST_FAILED` release, lifecycle, schedule/weekly-met semantics, availability gating, probe-store-safety and filename-parsing behaviour at unit/Robolectric level (**165/165 PASSED** across 12 test classes); these device cases cover what only a real device can prove. All cases remain **`NOT RUN`**.

### RM-H01 — Atomic cap, posting ownership, and idempotent delivery on a real device (extends P25-H03; D-225, D-231)

- **Status:** `NOT RUN`

**Procedure:** Configure three active habits with reminders due within a few minutes of each other on a device; ensure quiet hours/comparisons do not interfere; grant `POST_NOTIFICATIONS`; let all alarms fire (inexact window); inspect notifications received and Settings → debug probe ledger view (or `reminder_deliveries` via a debug tool).
**Expected:** At most 3 notifications on the local date under the shared cap; exactly one notification per habit occurrence even if the inexact alarm fires twice concurrently or the app is force-stopped and relaunched mid-window (`evaluateAndClaimHabitReminderForPostingSync` grants posting ownership to at most one active poster); ledger shows `SCHEDULED` promoted to `DELIVERED` (with `suppression_reason` cleared to `NULL`) only for posted occurrences; no `DELIVERED` row exists without the user-observable notification.
**Dependencies:** Physical device; debug build.

### RM-H02 — Alarm cancellation and stale protection on config change

- **Status:** `NOT RUN`

**Procedure:** Schedule a reminder; before it fires, change the habit's reminder time (advance to a later time), then separately pause the habit, then delete a habit with a pending reminder; also edit a habit's reminder time *after* an alarm for the old time was already registered.
**Expected:** No notification is posted for a cancelled/paused/deleted habit; an alarm registered for the old time does not post after the config changed (stale `SKIPPED_*` path with no ledger row creation); the new time delivers within its inexact window.

### RM-H03 — Debug probe four-gate report on device

- **Status:** `NOT RUN`

**Procedure:** In a debug build, open Settings → Notifications & reminders → debug probe section with (a) all gates open, (b) notification permission denied, (c) channel blocked in system settings.
**Expected:** (a) A real notification appears and auto-cancels within ~5 s; (b)/(c) the panel names the exact blocking gate (permission vs app-level vs channel), no alarm is scheduled, and no `reminders_enabled` write or ledger row occurs (verify via app data inspection that global switches stayed untouched).
**Dependencies:** Debug build only; release builds must not show the section at all (verify by installing a release build and inspecting the panel).

### RM-H04 — SAF filename display against real providers

- **Status:** `NOT RUN`

**Procedure:** Save a backup twice to the same folder choosing the same suggested name with (a) the system Files/Downloads provider, (b) a different provider if available (USB/Drive); then restore and check the "Selected file:" line with a file whose name Android renumbers.
**Expected:** The panel shows the **actual** provider filename (e.g. `dayseed-backup-2026-11-05 (1)`) rather than the requested name, plus the duplicate-numbering explanation; restore shows the real selected name; behaviour degrades gracefully (no crash) if a provider reports no name.

### RM-H05 — Force-stop crash recovery sweep and lease expiry (D-225, D-231)

- **Status:** `NOT RUN`

**Procedure:** With a reminder that has just been reserved/claimed (ledger `SCHEDULED`) and before the post completes, force-stop the process (developer options → running app); relaunch before and after the 30-minute pending lease window (`DEFAULT_MAX_PENDING_AGE_MS`) or wait for the next reconcile cycle after lease expiry.
**Expected:** While a claim lease is fresh (<30 min), the sweep does not steal or double-post it; once stale (≥30 min), no notification-less `DELIVERED` row is left permanently — the sweep atomically claims and re-posts when still eligible or records an honest `SUPPRESSED` outcome (`NOT_SCHEDULED`, `WEEKLY_TARGET_MET`, `NOTIFICATIONS_BLOCKED`, `DAILY_CAP_REACHED`, or `PENDING_EXPIRED`); subsequent days deliver normally.

### RM-H06 — Scheduled-day and weekly-target reminder semantics on device (D-229)

- **Status:** `NOT RUN`

**Procedure:** On a device, create (a) a daily habit with a reminder, (b) a weekday habit scheduled only for a weekday that is *not* today, (c) a flexible "3 sessions per week" habit with preferred weekdays that excludes today, and (d) a flexible weekly habit whose target is already met earlier this week. Allow the reminder time to pass with global reminders enabled.
**Expected:** (a) delivers; (b) and (c) produce **no** notification on the unscheduled/preferred-excluded day, and deliver on their next scheduled/preferred day; (d) produces no notification for the rest of the week and resumes the following week. Inspect the ledger (debug): unscheduled days show `SUPPRESSED`/`NOT_SCHEDULED`, met-target days show `SUPPRESSED`/`WEEKLY_TARGET_MET`, and an unrelated reminder-enabled habit on the same day still delivered (per-habit gate, shared cap intact).
**Dependencies:** Physical device; debug build; device date control for week-boundary checks.

### RM-H07 — Multi-habit sequential delivery and channel-blocked honesty on device (D-230)

- **Status:** `NOT RUN`

**Procedure:** Configure two active habits with distinct reminders on the same date (e.g. habit A at 09:00, habit B at 18:00), both global reminders enabled and `POST_NOTIFICATIONS` granted; let each alarm fire at its own time within its inexact window; inspect notifications received and the ledger for each occurrence. Separately, with (a) app notifications disabled in system settings and (b) the `dayseed_reminders` channel blocked at `IMPORTANCE_NONE`, fire a reminder alarm.
**Expected:** (1) habit A delivers at 09:00, habit B delivers at 18:00 — the earlier habit's alarm must never reserve habit B's slot or consume the shared cap on its behalf (ledger row for habit B is absent before its own alarm); both occurrences reach real `DELIVERED`. (2)(a)(b) with the app or channel blocked: no notification is posted, no `DELIVERED` row is created, and the ledger shows `SUPPRESSED`/`NOTIFICATIONS_BLOCKED` with zero cap consumption — device display honesty holds even when the app is blocked.
**Dependencies:** Physical device; debug build; system settings access to disable app notifications and block the `dayseed_reminders` channel.

---

## 10. Phase 3 human-intervention cases (2026-11-08, Status: `NOT RUN`)

All Phase 3 human/device validation cases below (`P3-H01` through `P3-H09`) are prepared for the consolidated human/device validation session after Stage B implementation is complete. None of these cases have been executed during Stage B (`NOT RUN`).

### P3-H01 — Journal archive (`A2`), collapsible date-jump calendar, search, mood filter, and scroll-position recovery

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open the dedicated `Journal` bottom-navigation destination.
  2. Create multiple entries across several dates (including two entries on the same date, one body-only entry, and entries with primary and secondary moods).
  3. Expand the collapsible date-jump month calendar (`journal_toggle_calendar_button`), navigate to an earlier month, and tap a date to filter the archive.
  4. Search by title and body keywords, filter by a primary mood chip, tap `Clear search & filters`, and open/edit an entry from mid-list.
- **Expected:** The `Journal` destination opens directly into the `A2` archive without a nested `Daily Log` tab; date-jump, search, and mood filters narrow entries accurately; clearing filters restores the timeline; returning from editing preserves archive scroll position.

### P3-H02 — Unsaved draft persistence, cursor-position restoration, and draft-to-entry save across process death

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `JournalEditorDialog` for a date, type a multi-sentence reflection, place the text cursor in the middle of the first sentence, and close/force-stop the app without tapping `Save Entry`.
  2. Relaunch Dayseed and reopen the draft from `Today` or the `Journal` archive.
  3. Tap `Save Entry`, then test editing an already-saved entry and cancelling with unsaved edits.
- **Expected:** The unsaved draft is clearly badged as `Draft`, never appears in saved search results, and restores both text and cursor position after relaunch. Saving promotes the draft atomically to a saved `JournalEntry` and removes the draft; cancelling an edit on a saved entry prompts before discarding unsaved changes.

### P3-H03 — Shared Calendar and `Day Detail` inspection across active, paused, completed, and archived habits

- **Status:** `NOT RUN`
- **Procedure:**
  1. In `Insights → 2. Habits`, switch to `Month Calendar` and navigate across current and historical months (> 30 days ago) under both `All habits` and single-habit filters.
  2. Inspect dates with completed, partial, explicit `0`, `REST_DAY`, paused, unscheduled, missed, and flexible-weekly-in-progress states, plus mood and multi-entry Journal markers (`✍️2`).
  3. Tap a date cell to open `DayDetailScreen`, inspect historical habits that were later paused or archived, tap a habit session to edit in `RecordSession`, and press system Back.
- **Expected:** Historical habits remain visible on dates where they were active or had records; one completed habit never paints the entire date green in `All habits` mode; `DayDetailScreen` displays all habit records, notes, moods, and Journal entries for that date and restores the exact parent calendar context on Back.

### P3-H04 — 7-column Weekly Habit Board across Monday, Sunday, and Saturday week starts

- **Status:** `NOT RUN`
- **Procedure:**
  1. In `Insights → 2. Habits`, select `Weekly Board`.
  2. Inspect rows for daily-target habits (`TargetPeriod.DAY`) and weekly-target habits (`TargetPeriod.WEEK`: `Quantity`, `Duration`, `days/week`, and `sessions/week`).
  3. Change the week-start day in `Settings` and inspect the Weekly Board column headers and week range boundaries.
- **Expected:** Columns follow the effective week-start preference; daily habits show daily status and scheduled days met; weekly habits show per-day contributions without false daily failures and evaluate the weekly target once against the frozen start-of-week version (showing `Week in Progress` for the current incomplete week and `Target Met` with neutral remaining days once satisfied early).

### P3-H05 — Growth Mark `G2` (`Collected moments`) and `G3` (`CollectedMomentsDetailSheet`) on device

- **Status:** `NOT RUN`
- **Procedure:**
  1. Inspect the `Collected moments` (`G2`) card on `Today` and in `Insights → 4. Growth` across an empty month, a zero-only duration month, and a month with `min` and `hr` duration sessions.
  2. Tap the card to open `CollectedMomentsDetailSheet` (`G3`), navigate between months, select a date, and tap a duration session to edit or delete it in `RecordSession` (testing the 8-second Undo snackbar).
- **Expected:** `G2` and `G3` display normalized duration totals (`45 min`, `1 hr 30 min`, etc.) from `DURATION` habits only, distinguish explicit `0` duration check-ins from empty dates, and immediately recalculate date and month totals after session edit/delete/Undo.

### P3-H06 — `Insights` 4-section hierarchy (`Overview`, `Habits`, `Mood`, `Growth`), separated metrics, and Mood Insights comparison

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Insights` and switch through `1. Overview`, `2. Habits`, `3. Mood`, and `4. Growth`, testing period filters (`This Week`, `This Month`, `All Time`).
  2. In `1. Overview`, verify that logging a Journal-only day increases `Journal Reflections` without increasing `Habit-Active Dates`, and verify that future dates in `This Week` / `This Month` never count as missed daily opportunities.
  3. In `2. Habits`, inspect the separated Daily (`TargetPeriod.DAY`) and Weekly (`TargetPeriod.WEEK`) `D-040` progress-band distributions.
  4. In `3. Mood`, toggle `This Week` / `This Month`, enable `Compare with previous period`, and tap a primary mood filter chip to inspect matching Journal entries.
- **Expected:** Each metric states its exact unit, numerator, and denominator; `D-040` bands reflect aggregated habit-day and habit-week totals rather than raw session rows; Mood Insights counts and filters by primary mood only while keeping secondary moods visible on entry cards.

### P3-H07 — Small-screen (`320dp–360dp`) and `200%` font-scale layout across `Journal`, `SharedMonthCalendar`, `WeeklyHabitBoard`, `DayDetailScreen`, `G2`/`G3`, and `Insights`

- **Status:** `NOT RUN`
- **Procedure:**
  1. Set display width to `320dp–360dp` and font scale to `200%`.
  2. Inspect `Journal`, `JournalEditorDialog` (with soft keyboard open), `SharedMonthCalendar`, `WeeklyHabitBoard`, `DayDetailScreen`, `CollectedMomentsDetailSheet`, and all 4 `Insights` sections.
- **Expected:** No clipped text, overlapping calendar cells, or unreachable Save/Cancel/Back controls; all touch targets remain at least `48dp`.

### P3-H08 — TalkBack traversal of `Journal`, `SharedMonthCalendar`, `WeeklyHabitBoard`, `DayDetailScreen`, `G2`/`G3`, and `Insights`

- **Status:** `NOT RUN`
- **Procedure:**
  1. Enable Android TalkBack and swipe through `Journal` cards/filters, `SharedMonthCalendar` date cells, `WeeklyHabitBoard` rows/cells, `DayDetailScreen`, `G2`/`G3`, and `Insights`.
- **Expected:** Date cells announce full date, habit status summary, mood, and Journal count; every status has a non-colour symbol/label cue; decorative graphics do not trap focus.

### P3-H09 — Light, dark, `Soft Botanical`, and `Dark + Time-of-Day Artwork` contrast on Phase 3 screens

- **Status:** `NOT RUN`
- **Procedure:**
  1. Inspect `Journal`, `SharedMonthCalendar`, `WeeklyHabitBoard`, `DayDetailScreen`, `G2`/`G3`, and `Insights` across all 5 appearance modes and reduced-motion settings.
- **Expected:** Calendar status indicators, mood chips, draft badges, and chart bars maintain clear contrast and calm transitions across all themes.

## Phase 4 — Human/Device Validation Cases (added 2026-11-09)

All Phase 4 cases are **`NOT RUN`**. They require a physical Android device (Android 13+ for permission cases) and must be executed only in a separately authorised validation stage; they are not satisfied by the Stage B self-checks.

### P4-H01 — Notification permission allow/deny on Android 13+

- **Status:** `NOT RUN`
- **Procedure:**
  1. Install a debug build on Android 13+.
  2. Open Settings → Notifications & reminders and tap "Allow notifications"; deny the first prompt, then grant it via the second attempt or the system settings route.
  3. Confirm all core features (habits, journal, history, insights) remain usable with notifications denied.
- **Expected:** The explanation precedes the system prompt, denial never blocks core features, and the panel's permission state matches the OS state after each choice.

### P4-H02 — Weekly goal acknowledgement

- **Status:** `NOT RUN`
- **Procedure:**
  1. Create a weekly-target habit (e.g. 3 days/week) with reminders enabled and permission granted.
  2. Reach the weekly target mid-week.
- **Expected:** Exactly one small congratulations arrives shortly after the third qualifying day (inexact timing is acceptable), it names only the habit, and no duplicate arrives later in the same week.

### P4-H03 — Week-start reflection and late delivery

- **Status:** `NOT RUN`
- **Procedure:**
  1. Set the week start (Monday and Sunday fixtures) and keep reminders enabled.
  2. Prepare three previous weeks: target met, partial progress, no progress.
  3. Observe the notification at 09:00 device-local on the week-start day; then repeat with the device off at 09:00 and switched on later the same day or the next day.
- **Expected:** Copy matches the previous week's outcome, arrives once, late delivery still happens once without a multi-week backlog, and no reflection is sent when no weekly-target habit existed.

### P4-H04 — Inactivity check-in cadence

- **Status:** `NOT RUN`
- **Procedure:**
  1. With an unmet weekly goal, record no progress and watch days 1–7 of the configured week; then repeat with a planned rest day and with progress resumed mid-week.
- **Expected:** No check-in before two complete days without progress; at most three per week; the check-in stops when the target is met or the week ends; a rest day does not trigger one.

### P4-H05 — Optional encouragement tips

- **Status:** `NOT RUN`
- **Procedure:**
  1. Confirm tips are off by default.
  2. Enable `encouragement_tips_enabled` (debug/settings write) and finish a week with partial progress.
  3. Observe the following seven days.
- **Expected:** At most three tips, never two on one local day, at least 24 hours apart, suppressed when the habit is later met or the user opts out.

### P4-H06 — Reboot, force-stop, time zone and week-start changes

- **Status:** `NOT RUN`
- **Procedure:**
  1. Reboot the device, force-stop the app, change the time zone, change the start-of-week setting, and edit a habit reminder time.
- **Expected:** No duplicate weekly or daily notifications, no reminder for an obsolete configuration, and no notification for a paused, archived, completed or deleted habit.

### P4-H07 — Quiet hours and shared cap across reminder types

- **Status:** `NOT RUN`
- **Procedure:**
  1. Set quiet hours to cover the weekly 09:00 moment; then restore them and configure several habits plus a weekly goal on the same day.
- **Expected:** Nothing posts inside quiet hours; after quiet hours the pending weekly occurrence may still arrive once; total notifications per local day never exceed three.

### P4-H08 — Backup and restore through the system file picker

- **Status:** `NOT RUN`
- **Procedure:**
  1. Export a password-protected backup via the Storage Access Framework, then restore it with the correct password, a wrong password, a truncated file and a tampered file.
  2. Repeat with REPLACE and MERGE, including an intentionally conflicting record, and interrupt a restore.
- **Expected:** Only the correct payload restores; failures leave the source data intact; a verified safety backup exists before Replace; unresolved conflicts stay preserved and reviewable; restored settings rebuild future notifications from the current device's configuration.

### P4-H09 — Deletion outcomes match the disclosure

- **Status:** `NOT RUN`
- **Procedure:**
  1. Delete one journal entry, discard a draft, permanently delete a habit with linked journal text, resolve a restore conflict, then delete all data.
- **Expected:** Journal text survives habit deletion with only the optional link cleared; delete-all removes habits, journal, drafts, streak/Grove records, reminder ledger and settings; the UI never claims that provider or other-app copies were deleted.

### P4-H10 — Accessibility and layout of the Phase 4 surfaces

- **Status:** `NOT RUN`
- **Procedure:**
  1. With TalkBack enabled, traverse Settings → Notifications & reminders and tap a weekly notification.
  2. Set font scale to 200% and width to 320–360 dp; measure contrast in all appearance modes; test keyboard/focus restoration and reduced motion.
- **Expected:** Every switch, row and status announces its state and purpose; no clipped or unreachable controls; touch targets at least 48 dp; contrast meets the project thresholds when measured; tapping a weekly notification opens Dayseed; reduced motion suppresses plant/Grove and theme transitions.

### P4-H11 — Synthetic-data performance on representative devices

- **Status:** `NOT RUN`
- **Procedure:**
  1. Use only synthetic data: a long habit list, several years of habit history, hundreds of journal entries, and multiple entries on the same date.
  2. Profile Today, History, Journal, Insights and Grove while scrolling, switching dates, and returning from Settings on a lower- and mid-range Android device.
- **Expected:** Lists remain responsive, database work does not rescan full history on every Compose recomposition, and no private journal content appears in logs.

### P4-H12 — Core use with Airplane mode enabled

- **Status:** `NOT RUN`
- **Procedure:**
  1. Enable Airplane mode before opening the app.
  2. Create/edit a habit, record progress, write and save a journal entry, browse history/insights, and open privacy/backup settings without exporting a file.
- **Expected:** Core habit and journal use remains available offline; no sign-in or network connection is required.

---

## Canonical UI & Product-Behaviour Reconciliation — Human/Device Validation Cases (added 2026-11-13; D-233)

All `P5-H*` cases below are **`NOT RUN`**. They require installing the upgraded APK (`versionName = "1.0.1"`, `versionCode = 2`) on the physical Android device and observing live device behaviour.

### P5-H01 — In-place APK upgrade (`1.0` → `1.0.1`, build 2) and activity-safe `4c1187a` starter-data cleanup preserving user data

- **Status:** `NOT RUN`
- **Procedure:**
  1. On a phone carrying the historical `4c1187a` starter data (`Gentle Movement`, `Quiet Reading`, `Drink Water`, and the starter journal entry) — or a synthetic mix of untouched starter items, a starter habit with logged user activity, and custom user habits/entries — install the new `1.0.1` (`versionCode = 2`) APK as an in-place update without uninstalling.
  2. Launch Dayseed and inspect `Today`, `Habits`, `Journal`, and `Settings → About Dayseed`.
- **Expected:** Untouched historical `4c1187a` starter habits and the unedited starter journal reflection are removed automatically once; if a starter habit had genuine user activity logged against it, that habit and all of its history remain intact; if 0 habits remain after cleanup, the first-run onboarding welcome card is shown; if any user habits or active starter habits remain, every habit, entry, outcome, note, streak run, and journal entry remains intact. `Settings → About Dayseed` displays `Dayseed 1.0.1 (build 2)` and the build commit SHA.

### P5-H02 — Evidence-derived habit recording without manual `Tiny`/`Usual`/`Extra` effort buttons

- **Status:** `NOT RUN`
- **Procedure:**
  1. Inspect active habit cards on `Today` and open `Record Session` and `Create / Edit Habit`.
  2. Log partial, exact-target, and above-target values for `Duration` and `Quantity` habits, and log a `Simple Check-Off` habit.
  3. Open an imported/historical `EFFORT_AWARE` habit on `Today`, in `Record Session`, and in `Edit Habit`.
- **Expected:** No manual `🌱 Tiny`, `🌿 Usual`, or `🌳 Extra` effort-selection buttons or chips appear in `Today` cards, `Record Session`, or `Create / Edit Habit`. Progress bands (`Tiny` through `Huge`) and paired encouragement/motivation quotes derive automatically from recorded evidence relative to target. Legacy `EFFORT_AWARE` habits preserve historical records and show a calm conversion banner in `Edit Habit`.

### P5-H03 — Shared `DaypartBackgroundLayer` across all primary tabs and secondary screens, plus `LIGHT` vs `SYSTEM`-light coherence

- **Status:** `NOT RUN`
- **Procedure:**
  1. Set appearance to `Time-of-day (Soft Botanical)` and navigate across `Today`, `Habits`, `Journal`, `Insights`, `Settings`, `Habit Detail`, `Create / Edit Habit`, and `Record Session`.
  2. Switch between `Fixed Light` and `Match system` (while the device OS is in light mode) and compare card/screen surfaces.
- **Expected:** The subtle daypart sky wash and horizon hills remain visible behind all primary tabs and secondary sheets/screens rather than disappearing outside `Today`; `Fixed Light` and `Match system` (light) render identical warm ivory card surfaces (`#F7F3EA`).

### P5-H04 — Settings `Reduce motion` toggle and Android system animator scale compliance

- **Status:** `NOT RUN`
- **Procedure:**
  1. Open `Settings → Appearance & rhythm` (and the `Appearance` sheet) and toggle `Reduce motion` on and off.
  2. With `Reduce motion` off in Dayseed, disable animations at the Android OS level (`Remove animations` / `Animator duration scale = off`) and return to Dayseed.
- **Expected:** Enabling `Reduce motion` in Dayseed or disabling animations in Android system settings suppresses continuous botanical sway and daypart artwork blending across the app.

### P5-H05 — Restrained cold-launch brand presentation and adaptive launcher icon on device

- **Status:** `NOT RUN`
- **Procedure:**
  1. Cold-launch Dayseed from the Android home screen launcher icon.
  2. Observe the `dayseed_cold_launch_scene` card on `Today`, then switch tabs or dismiss it and return to `Today`.
- **Expected:** The adaptive Dayseed seed-and-sprout launcher icon appears cleanly on the Android home screen; the opening brand lockup (`Dayseed` + `"Your progress is more than your streak."`) appears calmly once per cold launch without blocking interaction and does not reappear on subsequent tab switches within the same session.

