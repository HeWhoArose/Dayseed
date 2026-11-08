# Dayseed Phase 3 Implementation Plan (Revision 4) — Journal, Shared Calendar, Weekly Board, Growth Mark and Insights

## 1. Baseline, Workspace Honesty & 6-Step Execution Sequence

- **Repository & Canonical Branch:** `HeWhoArose/Dayseed`, branch `main`, expected starting commit `86d3e65c3165bdc4ce8cd92d570547efc61feb39` (`86d3e65`, D-231 follow-up).
- **Workspace Baseline Honesty:**
  - The AI Studio container does not contain a `.git` metadata directory, so the imported workspace base SHA is `UNKNOWN`. We do **not** claim commit equivalence solely from the absence of `.git` metadata or a visual tree check.
  - Before modifying any application source file, we will verify the latest GitHub `main` HEAD (`86d3e65c3165bdc4ce8cd92d570547efc61feb39` or newer if remote `main` has advanced) and reconcile all Phase 3 target files against remote `main` before editing.
- **Mandatory 6-Step Execution Sequence:**
  1. **AUDIT:** Inspect existing code across UI, ViewModel, domain, repository, and tests to catalogue completed functionality vs. Phase 3 gaps.
  2. **BASELINE:** Execute existing unit/Robolectric tests, debug/release builds, and lint before editing application source code, recording exact baseline results.
  3. **IMPLEMENT:** Implement the approved Phase 3 scope across Workstreams A–E.
  4. **FOCUSED TESTS:** Run focused unit/Robolectric tests after each workstream to verify incremental correctness.
  5. **FULL REGRESSION:** Execute the full regression suite (`testDebugUnitTest --rerun-tasks`, `assembleDebug`, `assembleRelease`, `lintDebug`) using the repository's `./gradlew` wrapper where present (or the container's `gradle` runner when `./gradlew` is not present in the workspace, recording the exact command and toolchain).
  6. **READ-ONLY RE-AUDIT:** Perform a separate read-only inspection of the final diff against all Phase 3 requirements, data contracts, and review corrections before reporting completion.
- **Owner-Confirmed UX Choices:**
  1. **Day Detail Presentation:** Dedicated `SecondaryRoute.DayDetail(date, originTab, parentRoute)` route with full `BackHandler` and top-bar Back restoration of parent screen, calendar month/week, filter, and scroll state.
  2. **Journal Archive (`A2`):** Dedicated `Journal` destination screen (`HistoryScreen.kt` refactored to a pure Journal Archive without the nested `Daily Log` tab) featuring a collapsible date-jump month calendar alongside the chronological timeline, local title/body search, mood filter chips, and unsaved draft resume cards.

---

## 2. Workstream Breakdown

### Workstream A — Journal Destination, Editor, Race-Free Draft Promotion, Cursor Restoration & Save-Failure Handling

1. **A1 — Dedicated Journal Archive (`HistoryScreen.kt`, `ReflectionCard.kt`, `TodayScreen.kt`):**
   - Replace the two-tab `Journal Archive / Daily Log` layout on `NavTab.JOURNAL` with a dedicated **Journal Archive (`A2`)** screen (`testTag("journal_screen")`).
   - Provide:
     - Prominent **New Entry** button (`testTag("journal_add_entry_fab")`).
     - Collapsible **Date Jump Calendar** (`testTag("journal_date_jump_toggle")`, `testTag("journal_date_jump_calendar")`) using the shared month calendar so users can jump to any date (including dates older than 30 days), filter/inspect entries on that date, or start an entry for that date.
     - **Unsaved Drafts Section** (`testTag("journal_drafts_section")`) clearly marked with a `"Draft"` badge (`docs/26`) so users can resume or discard local drafts, while keeping drafts strictly excluded from the saved-entries list, saved-entry counts, and search/mood results.
     - **Local Title & Body Search** (`testTag("journal_search_input")`) matching either `title` or `body` (ensuring body-only entries with `title = null` are findable) plus **Mood Filter Chips** (`primaryMoodCode` or `secondaryMoodCode` in archive filtering) and a **Clear search / filters** action (`testTag("journal_clear_filters_btn")`).
     - Distinct **Empty Archive** state (`"No journal entries yet"` + `"Write your first entry"` button, `testTag("journal_empty_write_first_btn")`) and **No Search/Filter Results** state (`"No entries found..."` + `"Clear search"`, `testTag("journal_no_results_clear_btn")`).
     - Support multiple saved entries on the same date, each rendered as an independent card (`testTag("journal_card_${entry.journalEntryId}")`) showing date, optional title, body excerpt, primary/secondary mood badges, and optional linked habit badge.
   - Update `TodayScreen.kt` / `ReflectionCard.kt` so when multiple Journal entries exist for today, it displays the accurate entry count and recent preview rather than implying at most one entry exists (`AUD-025`).

2. **A2 & A3 — Journal Editor, Race-Free Draft Promotion, Cursor Restoration & Save-Failure Handling (`JournalEditorDialog.kt`, `DayseedRepository.kt`, `DayseedViewModel.kt`):**
   - **Preserve Optional `intended_journal_entry_id` & Harden `promoteDraftToJournalEntrySync()` Using `db.beginTransactionNonExclusive()` (Four Explicit Flows):**
     1. **New Draft Autosave (`saveJournalDraftSync`):**
        - Persists or updates a row in `journal_drafts` keyed by `draft_id` (`source_context`, `local_date`, `title`, `body`, `primary_mood_code`, `secondary_mood_code`, `prompt_id`, `linked_habit_id`).
        - `intended_journal_entry_id` remains `NULL` on newly created or autosaved drafts until explicit Save preparation (or preserves an existing non-null `intended_journal_entry_id` if a previous Save attempt already prepared it).
     2. **Preparation of a Draft for Explicit Save (`prepareDraftForSaveSync` / Phase 1 of `promoteDraftToJournalEntrySync`):**
        - Executed inside an Android-managed SQLite `IMMEDIATE` transaction using `db.beginTransactionNonExclusive()` (`try { ... db.setTransactionSuccessful() } finally { db.endTransaction() }`), never raw `execSQL("BEGIN IMMEDIATE")`:
          - Query `journal_drafts` directly from SQLite (`WHERE draft_id = ?`).
          - If `intended_journal_entry_id IS NULL`, execute a conditional update:
            `UPDATE journal_drafts SET intended_journal_entry_id = COALESCE(intended_journal_entry_id, ?), updated_at_utc_ms = ? WHERE draft_id = ? AND intended_journal_entry_id IS NULL`
          - Re-read the authoritative `intended_journal_entry_id` from SQLite and commit the preparation transaction durably so two simultaneous Save attempts for the same `draftId` always converge on the exact same `intended_journal_entry_id`.
     3. **Race-Free Atomic Promotion into a Saved Journal Entry (Phase 2 of `promoteDraftToJournalEntrySync`):**
        - Executed inside an Android-managed SQLite `IMMEDIATE` transaction using `db.beginTransactionNonExclusive()` (`try { ... db.setTransactionSuccessful() } finally { db.endTransaction() }`):
          1. **Re-read the source draft directly from SQLite** (`SELECT * FROM journal_drafts WHERE draft_id = ?`) rather than relying on in-memory `_journalDrafts.value`.
          2. Resolve the prepared `intended_journal_entry_id` (from the re-read SQLite draft row or the prepared ID returned by Phase 1).
          3. **Check whether the intended Journal entry already exists in SQLite** (`SELECT * FROM journal_entries WHERE journal_entry_id = ?`).
          4. **If the `journal_entries` row already exists:** do **not** overwrite or replace it. Clean up the source `draft_id` row (and its `journal_draft_cursor_$draftId` setting key) if still present, call `db.setTransactionSuccessful()`, and return the existing saved `JournalEntry` unchanged (preserving the first caller's saved `body`, `created_at_utc_ms`, and `updated_at_utc_ms`).
          5. **If the `journal_entries` row does not exist:** verify that the re-read SQLite source draft still exists, its `body` is non-blank, and its `intended_journal_entry_id` matches the prepared ID; only then insert the new `journal_entries` row (`SQLiteDatabase.CONFLICT_ABORT` / `insertOrThrow`, **never** `CONFLICT_REPLACE`), delete the exact source draft (`WHERE draft_id = ?`), and delete `app_settings` `WHERE setting_key = 'journal_draft_cursor_' || ?` atomically before calling `db.setTransactionSuccessful()`.
          6. If an error or injected failure occurs during promotion, let `db.endTransaction()` roll back the final transaction while leaving the `journal_drafts` row and its durable `intended_journal_entry_id` intact in SQLite.
     4. **Editing an Already-Saved Journal Entry (`saveJournalEntrySync`):**
        - Updates only the existing `journal_entries` row (`WHERE journal_entry_id = ?`), preserving its `journal_entry_id` and original `created_at_utc_ms`, without deleting unrelated drafts on that date.
   - **Draft Cursor-Position Persistence Across Process Death & Portable Backup Exclusion:**
     - Use `TextFieldValue` in `JournalEditorDialog.kt` (with `rememberSaveable(stateSaver = TextFieldValue.Saver)`) and persist the cursor selection range (`selection.start`, `selection.end`) in `app_settings` (`key = "journal_draft_cursor_$draftId"`, `value_type = "STRING"`, `value_text = "${selection.start}:${selection.end}"`, bounded to `0..body.length`) whenever a draft is autosaved.
     - **Portable Backup Exclusion Rule:** Because cursor position is transient editor UI state:
       - Any `app_settings` key starting with `"journal_draft_cursor_"` **must not be exported** in portable backups (`exportPortableBackupJsonSync`).
       - Any `"journal_draft_cursor_"` key present in an external backup payload **must not be restored or merged** as an application preference during `REPLACE` (`importBackupArraysDirect`) or `MERGE` (`mergeBackupWithConflictDetection`).
       - The `"journal_draft_cursor_$draftId"` row **must be removed** whenever the corresponding draft is promoted (`promoteDraftToJournalEntrySync`) or discarded (`deleteJournalDraftSync`).
     - When reopening an existing draft after process death or recreation, restore both the draft `body` and the saved `TextRange` cursor position (`testTag("journal_content_input")`).
   - **Save-Failure Retention & Clear Error Banner:**
     - Add observable `lastJournalSaveError: StateFlow<String?>` and `clearJournalSaveError()` on `DayseedViewModel` (plus an injectable failure hook in `DayseedRepository` for deterministic testing).
     - When a Journal save or draft promotion fails, keep `JournalEditorDialog` open (`_isJournalEditorOpen.value` remains `true`), preserve the entered content and durable `journal_drafts` row (including its prepared `intended_journal_entry_id`), do **not** show a success snackbar, and render a clear error banner inside the editor (`"Unable to save entry — your draft is safely preserved. Tap Save to retry."`, `Modifier.testTag("journal_save_error_banner")`).
   - **Body-only & Whitespace Validation:** Title and mood are optional; whitespace-only `body` is rejected on Save (`contentError = true`, `testTag("journal_body_error_text")`) while preserving the current draft and entered fields.
   - **Explicit Confirmation Dialogs:**
     - **Delete Saved Entry:** Tapping Delete opens an explicit confirmation dialog (`"Delete this entry? This cannot be undone."`, `testTag("confirm_delete_journal_dialog")`, `testTag("confirm_delete_journal_btn")`) with no snackbar Undo.
     - **Discard Draft / Unsaved Edits:** Provide an explicit `"Discard draft"` action (`testTag("discard_journal_draft_btn")`) with confirmation (`testTag("confirm_discard_draft_dialog")`) calling `deleteJournalDraftSync(draftId)`, and when closing/pressing Back with unsaved edits to an existing saved entry, prompt to discard changes or keep editing (`docs/26`). Navigating away from a new draft preserves the draft locally without silently promoting it to a saved entry.

3. **A4 & A5 — Journal Independence, Mood Rules & Privacy (`DayseedRepository.kt`):**
   - Enforce that creating, editing, deleting, or changing the `local_date` of a Journal entry or draft never creates/modifies `habit_entries` or `habit_day_outcomes`, never alters `evaluateDerivedDayStatus`, and never changes streak/Grove runs.
   - Enforce primary + optional distinct secondary mood selection from `MoodCatalog` (10 starter moods + expanded vocabulary), with zero emotion inference, diagnosis, or network calls.

---

### Workstream B — Shared Calendar, Historical Habit Visibility & Dedicated Day Detail Route

1. **B1 — Shared Calendar Component (`ui/components/SharedCalendarComponents.kt`):**
   - Build a reusable `SharedMonthCalendar` composable used across `InsightsScreen`, `CollectedMomentsDetailSheet` (`G3`), and the `Journal` collapsible date-jump calendar.
   - Features:
     - Previous month (`testTag("calendar_prev_month_btn")`), Next month (`testTag("calendar_next_month_btn")`), and **Jump to Today** (`testTag("calendar_jump_today_btn")`) controls, with unrestricted navigation to months older than 30 days.
     - Weekday column headers ordered dynamically by the user's saved week-start preference (`repository.getWeekStartDay(referenceDate)`).
     - Uses stored `YYYY-MM-DD` local-date keys directly without timezone conversion.
     - Supports both **All-Habits Mode** (compact independent per-habit status dots/marks so one completed habit never paints the whole date green) and **Single-Habit Filter Mode** (full-cell status encoding with non-colour cues), plus primary-mood symbol and Journal count/group marker.
     - Full TalkBack `contentDescription` announcing date, habit summary, mood/journal markers, and selection state, with minimum `48.dp` touch targets.

2. **B2, B3 & Historical Habit Visibility (`ui/screens/DayDetailScreen.kt`, `DayseedRepository.kt`, `DayseedViewModel.kt`, `MainActivity.kt`):**
   - **Historical Habit Visibility Helper (`repository.getHabitsRelevantForDate(date)` / `getHabitsRelevantForDateRange(startDate, endDate)`):**
     - Do **not** filter out a habit in the Shared Calendar, Day Detail, or Weekly Board solely because its *current* lifecycle state (`habit.lifecycleState`) is `PAUSED`, `COMPLETED`, or `ARCHIVED`.
     - Include a habit for a selected date or week whenever:
       1. It has an effective `HabitVersion` covering that date/week (`effective_from_local_date <= date`), or
       2. It has saved source records (`habit_entries`, `habit_day_outcomes`, `habit_date_notes`, or `habit_date_exceptions`) on that date/week.
     - Evaluate every historical date and week using the effective-dated `HabitVersion` (or frozen start-of-week `HabitVersion` when `targetPeriod == TargetPeriod.WEEK`) applicable to that date/week, never reinterpreting historic records using the habit's current lifecycle state or current target.
   - **Dedicated `DayDetailScreen` Secondary Route:**
     - Add `SecondaryRoute.DayDetail(val date: LocalDate, override val originTab: NavTab, override val parentRoute: SecondaryRoute? = null)` to `DayseedViewModel.kt` and wire it in `MainActivity.kt`.
     - Tapping a date cell in `SharedMonthCalendar` or the Weekly Board opens `DayDetailScreen` for that date (`viewModel.openDayDetail(date)`), with previous/next day steppers and `BackHandler` returning to the exact parent screen and calendar state.
     - Displays all relevant habits for that date across all 10 honest date states (`Goal met`, `Partial progress`, `Above target`, `Missed scheduled period`, `Rest day`, `Unscheduled date`, `Paused date`, `Explicit zero (0 recorded)`, `No record`, `Still-open period`), recorded amount/unit, habit/date note, individual sessions, and all saved Journal entries + mood summary for that date.
     - Tapping a habit opens `HabitDetail(habitId, date)`; tapping `Record` opens `RecordSession(habitId, date, null)`; tapping a specific numeric/duration session opens `RecordSession(habitId, date, entry.entryId)`; tapping a saved Journal entry opens `JournalEditorDialog` for that exact entry.

---

### Workstream C — Daily & 7-Column Weekly Calendar Board (`target_period` vs. `schedule_kind` Separation)

1. **C1–C4 — Authoritative `target_period` Evaluation & 7-Column Weekly Board (`ui/components/SharedCalendarComponents.kt`, `InsightsScreen.kt`, `DayseedRepository.kt`):**
   - **Strict Separation of `target_period` and `schedule_kind`:**
     - `HabitVersion.targetPeriod` is the **sole authoritative rule** determining whether a habit is evaluated against a daily target (`TargetPeriod.DAY`) or a weekly target (`TargetPeriod.WEEK`). Never use `scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET` as an alternative or OR-condition for `targetPeriod == TargetPeriod.WEEK`.
     - `HabitVersion.scheduleKind` (`DAILY`, `SELECTED_WEEKDAYS`, `FLEXIBLE_WEEKLY_TARGET`) determines **which dates are eligible** and whether `preferredWeekdaysMask` applies as guidance.
   - **7-Column Weekly Board (`WeeklyHabitBoard`):**
     - Columns ordered by the effective week-start preference (`Monday`, `Sunday`, `Saturday`, etc.) for the selected week, with **Previous Week** (`testTag("weekly_board_prev_btn")`), **Next Week** (`testTag("weekly_board_next_btn")`), **Jump to Current Week** (`testTag("weekly_board_jump_current_btn")`), and clear week range label (`e.g., 26 Oct – 1 Nov 2026`).
     - One row/card per habit relevant to that week (`testTag("weekly_board_row_${habit.habitId}")`):
       - **For Daily-Target Habits (`targetPeriod == TargetPeriod.DAY`):** Each of the 7 day cells shows that date's daily amount/outcome and daily status encoding (`Target met`: solid green + `✓`; `Partial`: lighter green tint + `◐`; `Above target`: darker green + `🔥`; `Missed scheduled period`: dotted/dashed border + `○`; `Rest / Unscheduled / Paused`: neutral + `—`/`⏸`; `Explicit zero`: neutral + `0`; `No record / Still open`: neutral), while the row summary shows the week's qualifying scheduled days met (e.g., `"4 / 5 scheduled days met"`).
       - **For Weekly-Target Habits (`targetPeriod == TargetPeriod.WEEK`):**
         - Individual date cells show **that date's contribution** (`+15p`, `20m`, `1 sess`, `✓`, `0`, `Rest`, or neutral blank) without falsely classifying each individual day against the full weekly target or marking unlogged days as daily failures.
         - The **weekly result** is evaluated and classified **once for the entire week** against the frozen start-of-week `HabitVersion` (`targetPeriod == TargetPeriod.WEEK`) using canonical weekly aggregation:
           - `Quantity` / `Duration`: sum compatible amounts across the week (`"8 / 10 pages this week"`, `"45 / 60 min this week"`).
           - `days/week` (`COMPLETION_ONLY` / legacy `EFFORT_AWARE` with `targetPeriod == TargetPeriod.WEEK`): count distinct qualifying dates (`"2 / 3 days this week"`).
           - `sessions/week` (`QUANTITY` with `targetPeriod == TargetPeriod.WEEK` and `unitCode.equals("sessions", ignoreCase = true)`): count individual positive sessions (`"3 / 3 sessions this week"`).
         - Once the weekly target is met early in the week, the weekly badge shows `Target met` (`✓`) or `Above target` (`🔥`) and subsequent non-preferred or unlogged dates in that week remain neutral (never marked missed).
         - If the selected week is the **current incomplete week** (`referenceToday` is within `weekStart..weekEnd` and the week has not ended yet) and `weekTotal < weekTarget`, the weekly status is presented as **In progress / Still open** (never as a failed/missed weekly opportunity).

---

### Workstream D — Mood & Journal Calendar Markers

- In `SharedMonthCalendar` date cells:
  - Render the date's **primary mood symbol** (from the most recent saved entry with a primary mood on that date) as a compact indicator.
  - Render a compact **Journal marker** (`✍️`) when `1` saved entry exists on that date, and a **count badge** (`✍️2`, `✍️3`, etc., `testTag("calendar_journal_badge_$date")`) when multiple saved entries exist on that date.
  - Keep secondary moods out of primary-mood distribution and calendar primary-mood symbols while displaying them in `DayDetailScreen` and `JournalArchiveCard`.
  - Keep untagged Journal entries (`primaryMoodCode == null`) represented by the Journal marker and accessible in `DayDetailScreen` without fabricating mood counts.

---

### Workstream E — Growth Mark (`G2`/`G3`), Insights Metric Definitions, D-040 Aggregation & Clock Injection

1. **E1 & E2 — Growth Mark `G2` (`CollectedMomentsSummaryCard`) & `G3` (`CollectedMomentsDetailSheet`) (`GrowthMark.kt`, `DayseedRepository.kt`):**
   - **`G2` (`CollectedMomentsSummaryCard`):**
     - Defaults to the current calendar month derived from injected `referenceDate` (`repository.clock.todayLocal()`), with explicit month/year label (`e.g., "November 2026"`).
     - Sums saved duration sessions from `DURATION`-tracked habits only, normalizing compatible units (`min`, `hr`/`hours`) via `DurationNormalizer` and formatting with downward/exact display remainder (`45 min`, `3 hr 40 min`, `1 day 2 hr`, `1 wk 2 days`, using 24-hour days and 7-day weeks, never month-equivalents).
     - Distinguishes:
       - **Normal positive month:** `"Collected moments · X this month (Month YYYY)"`
       - **Zero-only month (explicit `0` duration sessions logged, sum = 0):** `"Collected moments · 0 min this month (0 min recorded)"`
       - **No qualifying sessions (empty month):** `"Collected moments · No duration recorded this month"`
       - **Loading / Query failure state:** Non-blocking loading state and `"Unable to load — tap to retry"` without displaying stale data as current.
   - **`G3` (`CollectedMomentsDetailSheet`):**
     - Supports **Previous Month** (`testTag("g3_prev_month_btn")`), **Next Month** (`testTag("g3_next_month_btn")`), and **Jump to Current Month** navigation (`D-201`: month navigation + date selection, no separate day/week period selector tabs).
     - Computes month duration totals, per-date duration totals, per-date session lists (including explicit `0 min · recorded` sessions), and any incompatible-unit notice dynamically for the selected `yearMonth` via `repository.computeCollectedMomentsMonthDetail(yearMonth)`.
     - Tapping a session opens `SecondaryRoute.RecordSession(habitId, date, entryId)` so editing or deleting immediately recalculates the date total and `G2`/`G3` month total with the 8-second guarded Undo snackbar.

2. **E3 — Correct Insights Metric Definitions & D-040 Progress-Band Aggregation (`Models.kt`, `DayseedRepository.kt`, `InsightsScreen.kt`):**
   - **Separate Habit Activity from Journal Activity (`computeGrowthMetricsSync`):**
     - Remove the loop in `computeGrowthMetricsSync()` that added `_journalEntries` dates into `activeDates`. Journal-only dates must **never** inflate `Habit-Active Dates`.
     - Implement and expose 8 distinct, accurately named metrics with explicit period, source rows, numerator, and denominator:
       1. **Habit-Active Dates (`totalActiveDates` / `habitActiveDatesCount`):**
          - *Counting unit:* Distinct `local_date` keys.
          - *Source rows:* `habit_day_outcomes` (`COMPLETED` / `EFFORT_LOGGED`) and positive `habit_entries` (`value_units > 0`). Excludes explicit `0` entries, `REST_DAY` exceptions, and `journal_entries`.
       2. **Journal Reflections Written (`journalEntriesCount` & `journalActiveDatesCount`):**
          - *Counting unit:* Saved `journal_entries` rows (and distinct `local_date`s with saved Journal entries).
          - *Source rows:* `journal_entries` only (excludes `journal_drafts`).
       3. **Numeric/Duration Sessions (`numericDurationSessionsCount`):**
          - *Counting unit:* Individual positive session rows.
          - *Source rows:* `habit_entries` where `value_units > 0` (for `QUANTITY` and `DURATION` habits). Never includes `habit_day_outcomes`.
       4. **Completion Outcomes (`completionOutcomesCount`):**
          - *Counting unit:* Qualifying daily outcome rows.
          - *Source rows:* `habit_day_outcomes` (`COMPLETED` and legacy `EFFORT_LOGGED`). Never labelled as numeric sessions.
       5. **Daily Scheduled Opportunities — Numerator (`habitDaysMetCount`) vs. Denominator (`scheduledDailyOpportunitiesCount`):**
          - *Scope:* Evaluated strictly over habits whose effective `HabitVersion` on `local_date` has `targetPeriod == TargetPeriod.DAY` and `lifecycleState == HabitLifecycleState.ACTIVE`, where `local_date` is scheduled under `scheduleKind` (`DAILY` or `SELECTED_WEEKDAYS`).
          - *Exclusions from both numerator and denominator:*
            - Unscheduled dates (`!effVersion.isScheduledFor(date)`),
            - Paused, completed, or archived lifecycle dates (`effVersion.lifecycleState != ACTIVE`), and
            - `REST_DAY` exception dates (`habit_date_exceptions` with `REST_DAY`).
          - *Explicit Date Resolution Rules (`local_date` vs. `referenceToday`):*
            1. **`local_date < referenceToday`:** Count the eligible scheduled opportunity in the denominator (`scheduledDailyOpportunitiesCount`), whether its target was met or missed, and count it in the numerator (`habitDaysMetCount`) if its daily target was met.
            2. **`local_date == referenceToday`:** Include it in both numerator (`habitDaysMetCount`) and denominator (`scheduledDailyOpportunitiesCount`) **only if** its daily target has been met. Otherwise, exclude it from both as still open.
            3. **`local_date > referenceToday`:** **Always exclude it from both numerator (`habitDaysMetCount`) and denominator (`scheduledDailyOpportunitiesCount`).** Future scheduled dates must never count as missed opportunities.
          - **`habitDaysMetCount` (Numerator):** Number of eligible scheduled daily opportunities `(habit_id, local_date)` (`local_date <= referenceToday`) in the reporting period where the effective daily target was met (`dailyTotal >= targetValue` for measured daily habits, or a qualifying `habit_day_outcomes` row for daily check-off/effort habits).
          - **`scheduledDailyOpportunitiesCount` (Denominator):** Total number of all eligible, completed/resolved scheduled daily opportunities `(habit_id, local_date)` in the reporting period (all eligible past opportunities `local_date < referenceToday` whether met or missed, plus eligible `local_date == referenceToday` opportunities where the daily target is already met; e.g., if 3 met their target and 2 were missed, `habitDaysMetCount = 3` and `scheduledDailyOpportunitiesCount = 5`, yielding `3 / 5 = 60%`).
       6. **Unique Habits Completed (`uniqueHabitsCompletedCount`):**
          - *Counting unit:* Distinct `habit_id`s that met their applicable daily (`TargetPeriod.DAY`) or weekly (`TargetPeriod.WEEK`) target at least once in the selected reporting period.
          - *Source rows:* Aggregated daily habit-days met and weekly habit-weeks met across `habit_entries`, `habit_day_outcomes`, and `habit_versions`.
       7. **Weekly Opportunities — Numerator (`flexibleWeeklyOpportunitiesMetCount`), Denominator (`flexibleWeeklyOpportunitiesTotalCount`) & In-Progress (`flexibleWeeklyInProgressCount`):**
          - *Scope:* Kept completely separate from daily opportunities; evaluated per configured week `(habit_id, week_start_date)` (`week_start_history`) for habits whose frozen start-of-week `HabitVersion` has `targetPeriod == TargetPeriod.WEEK` and is active during that week.
          - **`flexibleWeeklyOpportunitiesMetCount` (Numerator):** Number of eligible weekly habit-periods where `weekTotalUnits >= weekTargetUnits` (`weekTargetUnits > 0`).
          - **`flexibleWeeklyOpportunitiesTotalCount` (Denominator):** Number of all eligible, completed weekly habit-periods (whether met or missed), plus any current week that has already met its target (`weekTotalUnits >= weekTargetUnits`).
          - **`flexibleWeeklyInProgressCount` (Still Open):** If the current week (`referenceToday` within `weekStart..weekEnd`) has not yet met its target (`weekTotalUnits < weekTargetUnits`), it is counted in `flexibleWeeklyInProgressCount` and **excluded** from failed completed-week opportunities (`flexibleWeeklyOpportunitiesTotalCount`). Preferred weekdays (`scheduleKind` / `preferredWeekdaysMask`) are never counted as daily opportunities or daily failures.
       8. **Planned Rest Days Taken (`plannedRestCount`):**
          - *Counting unit:* Saved `REST_DAY` exception rows in the selected period.
          - *Source rows:* `habit_date_exceptions` where `exception_kind = 'REST_DAY'`.
   - **Aggregate Before Classifying `D-040` Progress Bands (`TargetPeriod.DAY` vs. `TargetPeriod.WEEK`):**
     - Do **not** classify individual `habit_entries` rows against a daily or weekly target.
     - **For daily targets (`governingVersion.targetPeriod == TargetPeriod.DAY`):** Sum all eligible `habit_entries` for a given `(habit_id, local_date)` first, then classify the aggregated `(habit_id, local_date)` total against that date's effective daily `targetValue` using `ProgressBandClassifier.classify(dailyTotal, targetValue)` (preserving explicit `0` as `ProgressBand.ZERO` when only `value_units == 0` entries exist on that date).
     - **For weekly targets (`frozenWeekVersion.targetPeriod == TargetPeriod.WEEK`):** Aggregate the logical `(habit_id, weekStart..weekEnd)` period first — summing compatible units for `Quantity`/`Duration`, counting distinct qualifying dates for `days/week`, or counting positive session rows for `sessions/week` — and classify that weekly period **once** against the frozen weekly `targetValue`.
     - Surface the derived 7-band `D-040` distribution (`Tiny`, `Small`, `Halfway`, `Nearly there`, `Goal met`, `Extra`, `Huge`, plus `Zero logged`) separately for daily habit-days (`TargetPeriod.DAY`) and weekly habit-weeks (`TargetPeriod.WEEK`) in `InsightsScreen` (`Section 2 — Habits`), while keeping any legacy `EFFORT_AWARE` (`TINY`/`USUAL`/`EXTRA`) outcome counts in a separate clearly labelled legacy footnote.

3. **E4 — Mood Insights & `J1–J11` Compliance (`DayseedRepository.kt`, `InsightsScreen.kt`):**
   - Fix `DayseedRepository.computeMoodInsights` (`line 3230`) so selecting a mood filter (`selectedMoodFilter`) filters `currentEntries` strictly by **`primaryMoodCode.equals(selectedMoodFilter, ignoreCase = true)`** (never `secondaryMoodCode`).
   - Support `This Week` (using `getWeekStartDay(referenceDate)`), `This Month`, and `Compare with previous period` (immediately preceding week or month).
   - Tapping a matching Journal entry in Mood Insights opens `JournalEditorDialog` for that entry.

4. **Complete Clock Injection Across Phase 3:**
   - Remove all direct `LocalDate.now()` and `System.currentTimeMillis()` default arguments and calls in:
     - `JournalEditorDialog.kt` (pass `referenceDate: LocalDate` and `nowUtcMs: () -> Long` from `viewModel.repository.clock`),
     - `GrowthMark.kt` (`CollectedMomentsSummaryCard`, `GrowthMarkBadge`, `CollectedMomentsDetailSheet`),
     - `TodayScreen.kt` and `ReflectionCard.kt`,
     - `HistoryScreen.kt` (dedicated Journal destination),
     - `SharedCalendarComponents.kt`, `DayDetailScreen.kt`, and `InsightsScreen.kt`.
   - Ensure all date defaults, month/week bounds, `G2` current-month selection, and `G3` month navigation derive deterministically from `DayseedClock`.

---

## 3. Files Affected

- `app/src/main/java/com/example/dayseed/data/Models.kt` — Expanded `GrowthMetrics` / `InsightsSummary` models with separated metric fields (`habitActiveDatesCount`, `journalEntriesCount`, `journalActiveDatesCount`, `numericDurationSessionsCount`, `completionOutcomesCount`, `habitDaysMetCount`, `scheduledDailyOpportunitiesCount`, `uniqueHabitsCompletedCount`, `flexibleWeeklyOpportunitiesMetCount`, `flexibleWeeklyOpportunitiesTotalCount`, `flexibleWeeklyInProgressCount`, `plannedRestCount`, aggregated `dailyProgressBandCounts`, `weeklyProgressBandCounts`, `legacyEffortCounts`), `CollectedMomentsMonthDetail`, `DayDetailSummary`, and `WeeklyBoardModel`.
- `app/src/main/java/com/example/dayseed/data/DayseedRepository.kt` — Hardened two-phase `promoteDraftToJournalEntrySync` using `db.beginTransactionNonExclusive()` (atomic preparation + race-free final promotion re-reading SQLite draft and checking existing `journal_entries` without overwriting via `CONFLICT_REPLACE`); draft cursor persistence helpers (`saveJournalDraftCursorSync`, `getJournalDraftCursorSync`) with portable backup export/import/merge exclusion for `"journal_draft_cursor_*"` keys; `deleteJournalDraftSync`; corrected `computeGrowthMetricsSync` separating habit vs. Journal activity, explicitly computing `habitDaysMetCount` (numerator) vs. `scheduledDailyOpportunitiesCount` (denominator) with past/current/future date resolution rules, and aggregating `D-040` bands strictly by `TargetPeriod.DAY` vs. `TargetPeriod.WEEK`; fixed primary-only mood filter in `computeMoodInsights`; added `getHabitsRelevantForDate` / `getHabitsRelevantForDateRange`, `computeCollectedMomentsMonthDetail`, `computeDayDetailSummary`, and `computeWeeklyBoard`.
- `app/src/main/java/com/example/dayseed/DayseedViewModel.kt` — Added `SecondaryRoute.DayDetail`, `openDayDetail`, `saveOrPromoteJournalDraft`, `discardJournalDraft`, `lastJournalSaveError` / `clearJournalSaveError`, and month/week selection state for `SharedMonthCalendar`, `WeeklyHabitBoard`, and `CollectedMomentsDetailSheet`.
- `app/src/main/java/com/example/dayseed/MainActivity.kt` — Wired `SecondaryRoute.DayDetail` and passed `repository.clock` to `JournalEditorDialog` and `CollectedMomentsDetailSheet`.
- `app/src/main/java/com/example/dayseed/ui/components/SharedCalendarComponents.kt` *(new)* — Reusable `SharedMonthCalendar` and `WeeklyHabitBoard` composables with `DESIGN.md` semantic tokens, non-colour cues, mood/journal markers, and TalkBack semantics.
- `app/src/main/java/com/example/dayseed/ui/screens/DayDetailScreen.kt` *(new)* — Dedicated `DayDetail` secondary route displaying all 10 honest date states, habit sessions, habit/date notes, and saved Journal entries for the selected date.
- `app/src/main/java/com/example/dayseed/ui/screens/HistoryScreen.kt` — Refactored into the dedicated `Journal` destination (`A2` archive timeline, collapsible date-jump calendar, unsaved drafts section, title+body search, mood filter chips, clear filters, empty/no-results states).
- `app/src/main/java/com/example/dayseed/ui/screens/JournalEditorDialog.kt` — Injected clock, `TextFieldValue` cursor-position persistence and restoration across process death, `journal_save_error_banner` on failed saves while keeping the editor and draft open, Draft vs. Saved badge, explicit confirmation dialogs for saved-entry deletion and permanent draft discard, unsaved-edit close confirmation, and draft promotion wiring.
- `app/src/main/java/com/example/dayseed/ui/components/GrowthMark.kt` — Updated `G2` (`CollectedMomentsSummaryCard`) and `G3` (`CollectedMomentsDetailSheet`) for all `docs/26` states, month navigation, explicit zero session distinction, source-session tap to `RecordSession`, and clock injection.
- `app/src/main/java/com/example/dayseed/ui/screens/InsightsScreen.kt` — Structured into `1. Overview`, `2. Habits` (Shared Calendar, 7-Column Weekly Board, aggregated `D-040` progress bands), `3. Mood` (`J1–J11` compliant), and `4. Growth`.
- `app/src/main/java/com/example/dayseed/ui/screens/TodayScreen.kt` & `app/src/main/java/com/example/dayseed/ui/components/ReflectionCard.kt` — Multiple same-date Journal count/preview support and clock injection.
- `app/src/test/java/com/example/dayseed/Phase3ImplementationTest.kt` *(new)* — Automated JVM/Robolectric tests for 4.1–4.5 and all review amendments.

---

## 4. Automated Testing & Dynamic Regression Reporting Plan

Add `app/src/test/java/com/example/dayseed/Phase3ImplementationTest.kt` covering:

- **4.1 Journal, Race-Free Draft Promotion, Cursor Restoration, Backup Exclusion & Save-Failure Tests:**
  - New draft autosave keeps `intended_journal_entry_id == null` until save preparation.
  - Explicit Save prepares `intended_journal_entry_id` durably using `db.beginTransactionNonExclusive()` and atomically promotes the draft to `journal_entries` while deleting the exact `draft_id` and its `journal_draft_cursor_$draftId` setting.
  - **Two-Caller Post-Preparation Concurrency Test:** Caller A and Caller B both complete `prepareDraftForSaveSync(draftId)` before either runs final promotion; Caller A promotes the draft, then Caller B runs final promotion (even if holding stale draft text/timestamps); verify that Caller B returns Caller A's saved entry and **cannot** overwrite Caller A's saved `body`, `created_at_utc_ms`, or `updated_at_utc_ms`.
  - **Save-Failure Retention & Error Message Test:** Inject a persistence failure during Save/promotion; verify the draft and its `intended_journal_entry_id` remain in SQLite, `JournalEditorDialog` stays open with entered content intact, `lastJournalSaveError` / `journal_save_error_banner` is populated, and retrying after clearing the failure succeeds using the same `intended_journal_entry_id` without duplicate entries.
  - **Draft Cursor-Position Restoration & Backup Exclusion Test:** Create a draft, set the cursor inside its body (`TextRange`), simulate process death / recreation, reopen the draft, and verify the exact cursor position (`TextFieldValue.selection`) is restored. Also export a portable backup while `journal_draft_cursor_$draftId` exists and verify that `journal_draft_cursor_*` keys are excluded from exported `app_settings` and ignored on `REPLACE` / `MERGE` import, and removed when the draft is promoted or discarded.
  - Editing an already-saved entry preserves `journal_entry_id`, `created_at_utc_ms`, and leaves unrelated drafts untouched.
  - Body-only entries, multiple entries on the same date, local title/body search (including body-only matches), mood filtering, clear filters, confirmed saved-entry deletion, confirmed draft discard, and complete independence from habit completion/streaks.
- **4.2 Shared Calendar, Day Detail & Historical Visibility Tests:**
  - Month navigation (`prev`, `next`, `Jump to Today`) and dates older than 30 days using `TestDayseedClock`.
  - Weekday header ordering for Monday, Sunday, and Saturday week starts.
  - `DayDetail` data assembly for dates with multiple habit sessions and multiple Journal entries across all 10 date states.
  - Historical habit visibility: a habit that was `ACTIVE` during a past week/month and later `PAUSED`, `COMPLETED`, or `ARCHIVED` remains visible and accurately evaluated in the historical calendar, Day Detail, and Weekly Board using its historical effective configuration.
- **4.3 Weekly Board, `target_period` Authority & D-040 Aggregation Tests:**
  - 7-column ordering for Monday, Sunday, and Saturday week starts.
  - Authoritative `target_period` evaluation (`TargetPeriod.DAY` vs. `TargetPeriod.WEEK`) independent of `schedule_kind`: weekly totals for `Quantity` (`pages`), `Duration` (`min`/`hr`), `days/week` (distinct qualifying dates), and `sessions/week` (positive session count).
  - Weekly habit's individual date cells show daily contributions without classifying each day against the weekly target; weekly result is classified once for the week; incomplete current week remains **in progress** rather than failed/missed; flexible weekly target met early produces no later daily misses; unscheduled, paused, and `REST_DAY` dates remain neutral.
  - `D-040` progress-band aggregation: multiple partial sessions on the same date (e.g., two `5-page` sessions toward a `10-page` daily target) classify as `1` `Goal met` habit-day rather than `2` `Halfway` sessions; weekly sessions aggregate across the week before classifying the habit-week band.
- **4.4 Mood Insights Fixture Tests (`J1–J11`, `docs/07-TESTING-AND-QUALITY.md`):**
  - Exact `J1–J11` synthetic fixture verified for both Monday and Sunday week-start, including `Calm` primary-only filtering (`J1` and `J3` only in `2026-10-26..2026-11-01`) and immediate recalculation on `J1` edit/deletion.
- **4.5 Growth Mark (`G2`/`G3`) & Separated Insights Metrics Tests (Including Past, Current & Future Scheduled Dates):**
  - `Habit-Active Dates` excludes Journal-only dates, explicit `0` entries, and `REST_DAY` exceptions.
  - `Numeric/Duration Sessions` and `Completion Outcomes` are counted separately and never conflated.
  - **Scheduled Daily Opportunities Numerator/Denominator & Past/Current/Future Resolution Test:** Verify a reporting period containing past (`local_date < referenceToday`), current (`local_date == referenceToday`), and future (`local_date > referenceToday`) scheduled dates alongside unscheduled, paused, and `REST_DAY` dates:
    - Past eligible scheduled dates (`local_date < referenceToday`) count in `scheduledDailyOpportunitiesCount` whether met or missed (e.g., 3 met + 2 missed -> `habitDaysMetCount == 3`, `scheduledDailyOpportunitiesCount == 5`, `60%`).
    - Current-day scheduled opportunity (`local_date == referenceToday`) is excluded while still open (target not met), and included in both `habitDaysMetCount` and `scheduledDailyOpportunitiesCount` once met.
    - Future scheduled dates (`local_date > referenceToday`) are always excluded from both `habitDaysMetCount` and `scheduledDailyOpportunitiesCount` and never create missed opportunities.
    - Weekly opportunities (`TargetPeriod.WEEK`) are tracked separately in `flexibleWeeklyOpportunitiesMetCount`, `flexibleWeeklyOpportunitiesTotalCount`, and `flexibleWeeklyInProgressCount`.
  - `G2` and `G3` duration normalisation, downward display formatting (`45 min`, `3 hr 40 min`, `1 day 2 hr`, `1 wk 2 days`), empty vs. zero-only vs. positive month states, previous/next month navigation under `TestDayseedClock`, and session edit/delete + 8s Undo recalculation.
- **Dynamic Regression Suite Execution:**
  - Execute the Gradle test and build suite so all discovered test classes run dynamically, and report the exact executed test count, discovered test classes, failures, skipped tests, and toolchain/environment.

---

## 5. Human/Device Testing Deferral, Acceptance Gates & Documentation Updates

- **Human/Device Testing Deferral (Mandatory Owner Sequence):**
  - **No Phase 3 human/device tests are to be executed during Phase 3.**
  - Append Phase 3 human-validation cases (`P3-H01` through `P3-H09`) to `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` and mark every one of them **`NOT RUN`**.
  - Preserve all existing Phase 1 (`P1-H*`), Phase 2 (`P2-H*`, `P2-C*`), Phase 2.5 (`P25-H*`), and Reminder/Backup (`RM-H*`) human-validation statuses in `docs/38` as **`NOT RUN`**.
  - All human/device testing is consolidated and deferred until Phases 1–5 have been implemented.
  - Automated unit/Robolectric tests, debug/release builds, and lint **must** run during Phase 3, and passing automated suites must never be claimed as human/device validation.
- **Stage C & Stage D Gate Restrictions:**
  - **Stage C (independent technical validation)** remains separately gated and **NOT AUTHORISED / NOT OPEN**. Stage B self-checks are not Stage C evidence.
  - **Stage D (final owner acceptance / release readiness)** remains **CLOSED / NOT OPEN**.
- **Documentation Reconciliation:**
  - Update `docs/12-PROGRESS-AND-HANDOVER.md` to record that D-231 was committed and pushed on `main` at `86d3e65c3165bdc4ce8cd92d570547efc61feb39` (`86d3e65`) and add the Phase 3 implementation and test evidence summary.
  - Reconcile `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` (`P3-H01`–`P3-H09` as `NOT RUN`), and root `implementation_plan.md`.
