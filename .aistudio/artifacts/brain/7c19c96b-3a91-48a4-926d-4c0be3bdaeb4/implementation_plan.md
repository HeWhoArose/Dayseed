# Dayseed — Phase 2 Implementation Plan
## Complete Habit Experience, Five-Destination Navigation, Individual Streaks and Living Grove

Phase 2 delivers the complete everyday habit experience in Dayseed (`Stage B` initial-v1 implementation) across navigation, UI screens, ViewModel state, domain/repository helpers, guarded Undo, automated JUnit/Robolectric tests, and canonical documentation reconciliation.

## User Review & Confirmed Decisions
All product and architectural decisions governing Phase 2 are already settled in `docs/09-DECISION-LOG.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, and root `implementation_plan.md` (`Revision 7`):
- **Five-Destination Primary Navigation (`D-215`):** Replace the legacy 4-tab bar with five top-level destinations: `Today`, `Habits`, `Journal`, `Insights`, and `Settings`.
- **Measurement Modes & 7-Band Progress (`D-040`, `D-216`):** New habits support `Quantity`, `Duration`, and `Simple check-off` (`COMPLETION_ONLY`); legacy `EFFORT_AWARE` habits remain editable without data loss. Detailed numeric progress displays all 7 bands (`0%`, `<25%`, `25–49%`, `50–74%`, `75–99%`, `100%`, `>100–150%`, `>150%`) paired with both an `encouragementQuote` and a distinct `motivationQuote`, while compact calendar/history surfaces use the 4 compact buckets (`NONE`, `PARTIAL`, `COMPLETE`, `ABOVE_TARGET`).
- **Weekly Target Clarity (`D-191`, `D-195`, `U-11`, `U-12`):** Explicitly distinguish daily targets, weekly numeric targets (`N units/week`, `N min/week`), weekly check-off targets (`N days/week`), and weekly session-count targets (`N sessions/week`). Today cards and Habit Detail show week-to-date progress against the frozen start-of-week target (`U-12 Option B`) alongside today's contribution, never comparing a single day's entry directly against a weekly target.
- **Individual Streaks & Living Grove (`D-217`, `U-1`..`U-13`):** Streaks are per-habit (`DAYS` for daily-period habits, `WEEKS` for weekly-period habits). Today displays a calm main streak indicator sourced from `repository.selectLongestActiveRun()` (`elapsedActiveSpanCount` with explicit `days` or `weeks` unit) that opens the Living Grove & Individual Streaks view. Runs reaching `>= 7` active span days (`SAPLING_ACTIVE` -> `GROWING_TREE`) remain permanently in the Grove (`DORMANT_TREE` when ended with `MISSED`, `PAUSED`, `ARCHIVED`, or `COMPLETED`), while `< 7` day closed runs remain in separate unqualified streak history.
- **Planned Rest & Effective-Dated Protection (`D-194`, `D-218`, `U-1`, `U-9`):** Unselected weekdays are neutral schedule days; one-off **Take a rest day** (`REST_DAY`) is today-only for active scheduled habits without recorded activity; multi-day pauses use `habit_versions.lifecycle_state = PAUSED`. Editing a habit when today already has recorded activity (including explicit `0`) requires choosing **Apply today** vs **Apply tomorrow** (or next-week boundary for weekly unit/mode changes).

---

## 1. Overview & Core Concept

### 1.1 Baseline Inspection Summary & Phase 2 Gaps Identified
Before modifying any Phase 2 code, we will first run the full Phase 1 test suite (`59` JUnit test methods in `Phase1ImplementationTest`, `DayseedAcceptanceTest`, `DayseedSettingsAndInsightsTest`, and `VisualHierarchyAndContrastTest`, including the post-PR-#5 `t1_43` and `t1_44` tests) to confirm a clean green baseline.

Our source inspection of the current codebase identified the following concrete gaps between the Phase 1 foundation and the Phase 2 specification:
1. **Navigation (`MainActivity.kt`, `DayseedViewModel.kt`):**
   - `NavTab` currently defines 4 tabs (`TODAY`, `HISTORY`, `INSIGHTS`, `SETTINGS`) and `BackHandler` unconditionally resets to `NavTab.TODAY`.
   - Secondary flows (`HabitDetail`, `CreateEditHabit`, `RecordSession`, `LivingGrove`, `CollectedMomentsDetail`) do not yet have a structured back-stack that preserves the originating tab, selected date, habit, and filter state.
2. **Today Screen & Habit Card (`TodayScreen.kt`, `HabitCard.kt`, `GrowthMark.kt`, `DayseedRepository.kt`):**
   - `TodayScreen` currently filters strictly by `activeHabits.filter { it.isScheduledFor(today) }`, leaving unscheduled active habits (`SELECTED_WEEKDAYS` on off-days) hidden on Today without a compact **"Not scheduled today"** section.
   - `HabitCard` currently embeds full inline entry deletion, inline note inputs, and lifecycle dropdowns on the default Today card, and compares `todayEntries` directly against `habit.targetValue` even when `habit.targetPeriod == TargetPeriod.WEEK`.
   - `DayseedRepository.recomputeAllStreakRunsAndSummariesSync` currently computes `HabitSummary.todayTotalUnits`, `todayProgressBand`, and `todayPercentage` against `effToday.targetValue` without computing week-to-date progress (`weekTotalUnits`, `weekTargetValue`, `weekProgressBand`, `weekPercentage`) for `TargetPeriod.WEEK` habits.
   - `GrowthMarkBadge` on Today still renders legacy `Tiny / Usual / Extra` chips and exposes internal `G2`/`G3` code strings in user-facing copy instead of a clean **Collected moments** summary card that opens the shared Growth/date-detail route.
3. **Dedicated Habits Destination (`HabitsScreen.kt`):**
   - Missing dedicated top-level screen showing all habits across **Active**, **Paused**, **Completed**, and **Archived** lifecycle states with search, `sortOrder` reordering, lifecycle actions, and clear empty states.
4. **Habit Detail Secondary Route (`HabitDetailScreen.kt`):**
   - Missing dedicated detail route presenting habit identity, category, measurement/target/schedule explanation, current daily/weekly progress with `D-040` 7-band badge + paired encouragement/motivation quotes, lifetime goal progress (`lifetimeActiveDays / lifetimeGoalDays`), individual sessions list with per-session edit/delete, habit/date note editor, **Take a rest day** action, individual branch & Grove history vs `<7 day` runs, and distinct **Pause / Resume / Complete / Archive / Permanent Delete** controls.
5. **Create/Edit Habit Full-Screen Flow (`CreateEditHabitScreen.kt` / `AddEditHabitDialog.kt`):**
   - Currently a modal dialog that couples `TargetPeriod.WEEK` only to `FLEXIBLE_WEEKLY_TARGET`, lacks `category` and `lifetimeGoalDays` inputs, and does not clearly distinguish weekly check-off (`N days/week`) vs weekly session-count (`N sessions/week`) vs weekly numeric amount (`N units/week`).
6. **Focused Record Session & Correction Flow (`RecordSessionSheet.kt`):**
   - Currently `HabitCard` only appends or deletes entries inline on Today; there is no dedicated Record Session / Edit Session flow supporting individual session value/note correction (`updateHabitEntry`), backdated `LocalDate` selection, explicit `0` check-in, and clear confirmation of daily total vs weekly aggregate.
7. **Individual Streaks, Branches & Living Grove (`GroveComponents.kt`):**
   - The domain engine (`StreakAndGroveEngine` and `habit_streak_runs`) is implemented, but the UI components for the Today longest-active-streak indicator (`days`/`weeks`), per-habit botanical branch Canvas visualization, Living Grove overview (`SAPLING_ACTIVE`, `GROWING_TREE`, `DORMANT_TREE`, and separate `<7 day` runs), and event-driven one-shot run-ending animation (respecting reduced motion) are not yet wired into Compose.

---

## 2. User Experience & Visual Design

All Phase 2 surfaces follow `DESIGN.md` (**Warm Botanical Paper & Ink** palette: Warm Parchment `#FBF8F2` light / Deep Forest Charcoal `#131814` dark, Deep Moss `#2E5A3E` / Sage `#8FB99D` primary, Warm Terracotta `#C86D51` accent, 16dp card radius, 1dp Hairline Warm Stone borders, minimum 48dp touch targets, and WCAG AA contrast).

### 2.1 Five-Destination Navigation & Secondary Route Stack (`D-215`)
- **Bottom Navigation Bar (`MainActivity.kt`):**
  - `Today` (`nav_today`) — Daily action deck, daypart header, longest active streak indicator, scheduled habits, compact "Not scheduled today" section, reflection card, and Collected moments summary.
  - `Habits` (`nav_habits`) — Complete habit directory across `Active`, `Paused`, `Completed`, and `Archived` states, search, reorder, and create habit CTA.
  - `Journal` (`nav_journal`) — Dedicated reflection composer, draft recovery, and journal entries archive.
  - `Insights` (`nav_insights`) — Collected moments hero card, Living Grove & Individual Streaks access, Mood Insights, and historical habit calendar/daily log inspection.
  - `Settings` (`nav_settings`) — Reminders, week-start policy (`U-10`), backup/export, conflict resolution banner, and accessibility/motion info.
- **Secondary Route Stack (`DayseedViewModel.kt`):**
  - Secondary routes (`HabitDetail`, `CreateEditHabit`, `RecordSession`, `LivingGrove`, `CollectedMomentsDetail`) push onto an explicit navigation stack storing the originating `NavTab` and parent secondary route.
  - Pressing system Back or the top-bar back button pops to the exact originating screen and restores selected date, habit, filter chip, and scroll context.

### 2.2 Compact Today Screen (`TodayScreen.kt` & `HabitCard.kt`)
- **Top Header:** Date + time-aware greeting + `DaypartArtwork` (`D-210`/`D-211`) + compact **Longest Active Streak** pill (`testTag("today_main_streak_indicator")`) showing the habit name, normalized active span, and explicit unit (`days` or `weeks`) from `repository.selectLongestActiveRun()`. Tapping opens the **Living Grove & Individual Streaks** secondary route.
- **Scheduled Habits Today:**
  - Compact action-oriented cards showing habit identity, colour bar, schedule/cadence label, current daily or week-to-date progress bar, `D-040` progress band badge (`band.label`), paired `encouragementQuote` + `motivationQuote` when activity is logged, primary quick action (`Complete` / `+Step` / `Goal`), and a **Record / Edit Session** button (`testTag("open_record_session_${habit.id}")`).
  - Tapping the card body opens **Habit Detail** (`testTag("open_habit_detail_${habit.id}")`).
  - Quick actions use deterministic submission guards / idempotency keys to prevent accidental double-tap duplicates while keeping intentional multi-session logging available through **Record Session**.
- **Compact "Not Scheduled Today" Section:**
  - Active habits whose schedule does not include today (`SELECTED_WEEKDAYS` on unselected weekdays, or `FLEXIBLE_WEEKLY_TARGET` with preferred weekdays not matching today when already satisfied) appear in a collapsible/expandable **"Not scheduled today"** section (`testTag("not_scheduled_today_section")`) so users can inspect them or log an intentional bonus session (`U-2` / `U-7`) without cluttering the primary scheduled list.
- **Reflection & Collected Moments Entry Points:**
  - Concise **Daily Reflection** card (`ReflectionCard`) and a clean **Collected moments** summary card (`testTag("collected_moments_card")`, free of internal `G2`/`G3` jargon) that opens the shared Collected Moments / date-detail view (`testTag("collected_moments_detail_sheet")`).

### 2.3 Dedicated Habits Screen (`HabitsScreen.kt`)
- **Lifecycle Filter Chips:** `All`, `Active`, `Paused`, `Completed`, `Archived` (`testTag("habits_filter_${state}")`), plus local search input (`testTag("habits_search_input")`).
- **Habit Directory Cards:** Show habit name, optional category badge, measurement mode & target summary (e.g., `20 pages / day`, `150 min / week`, `4 days / week`, `3 sessions / week`), schedule summary, current streak / Grove tree badge, lifetime active days, reorder Up/Down controls (`testTag("reorder_up_${habit.id}")`, `testTag("reorder_down_${habit.id}")`) persisted to `habit_versions.sort_order`, quick lifecycle menu, and tap-through to **Habit Detail**.
- **Clear Empty States:** Tailored guidance when no habits exist yet or when a lifecycle/search filter has zero matches, with a prominent **New Habit** CTA (`testTag("habits_add_habit_btn")`).

### 2.4 Habit Detail Secondary Route (`HabitDetailScreen.kt`)
- **Header & Metadata Card:** Name, description, colour identity, optional `category`, measurement mode, unit, target & period (explicitly distinguishing daily target, weekly numeric target, weekly check-off `N days/week`, and weekly session target `N sessions/week`), schedule with neutral unselected weekdays, and reminder status.
- **Current Progress & Quotes Card:** Daily or week-to-date progress bar, `D-040` 7-band badge (`band.label` + `band.shortBadge`), and both `band.encouragementQuote` and `band.motivationQuote`. Also shows lifetime goal progress (`lifetimeActiveDays` / `lifetimeGoalDays` when configured, per `U-6`).
- **Selected Date & Individual Sessions Section:** Date switcher (defaulting to today or selected historical date, enabling backdated inspection/correction per `D-188`), **Record Session** CTA, **Take a rest day** CTA when `canTakeRestDayToday` is true (`D-218`), and a list of individual `HabitEntry` sessions on that date. Each session row shows its timestamp/amount/per-session note and has dedicated **Edit** (`testTag("edit_session_${entry.entryId}")`) and **Delete** (`testTag("delete_ entry_${entry.entryId}")`) actions so editing or deleting one session never overwrites or removes other sessions on the same date.
- **Habit/Date Note Card:** Separate editor for `HabitDateNote` (`habit_date_notes`) on the selected date, explicitly distinguished from per-session notes (`habit_entries.entry_note`) and `JournalEntry`.
- **Individual Streak Branch & Grove History Card:**
  - Renders the habit's botanical branch (`HabitBranchCanvas`) with non-colour shape/label cues (`SAPLING_ACTIVE`, `GROWING_TREE`, `DORMANT_TREE`).
  - Lists permanent **Grove Trees** (`qualifiedForGrove == true`, `>= 7` active span days) with start/end dates, completed streak count, active span, and terminal reason badge (`MISSED`, `PAUSED`, `ARCHIVED`, `COMPLETED`).
  - Separately lists **Short Unqualified Runs** (`!qualifiedForGrove`, `< 7` active span days) so short runs are preserved in history without being mixed into permanent Grove trees (`U-13`).
- **Lifecycle Management Bar:** Distinct actions for **Edit Habit**, **Pause** / **Resume**, **Mark Completed**, **Archive** / **Unarchive**, and **Permanent Delete** (guarded by a confirmation dialog explaining that permanent deletion removes the habit and its records, while still issuing the 8-second `HABIT_DELETION` Undo token).

### 2.5 Structured Create/Edit Habit Flow (`CreateEditHabitScreen.kt`)
- Full-screen secondary route (`AddEditHabitDialog` remains available or delegates to the same structured form) organized into 5 clear sections:
  1. **Identity & Category:** Name (required), Description (optional), Category (optional tag/input), Colour picker, and optional Lifetime Goal Days (`lifetimeGoalDays`).
  2. **Measurement Mode & Units (`D-216`):**
     - New habits: `Quantity`, `Duration`, or `Simple check-off` (`COMPLETION_ONLY`).
     - Legacy `EFFORT_AWARE` habits being edited preserve their mode and Tiny/Usual/Extra descriptions (`U-8`).
     - Mode-specific unit presets (e.g., `min`, `hr` for Duration; `pages`, `reps`, `glasses`, `km`, `sessions` for Quantity; hidden for `Simple check-off`).
  3. **Target & Period (`D-191`, `D-195`):**
     - Period selector: `Daily` (`TargetPeriod.DAY`) vs `Weekly` (`TargetPeriod.WEEK`).
     - When `Simple check-off` + `Weekly` is chosen, the target input represents **days per week** (`1..7 days/week`).
     - When `Quantity` + `Weekly` is chosen with `unitCode = "sessions"`, the form clearly labels **sessions per week** (`N sessions/week`), distinct from general numeric units (`N units/week`) and duration (`N min/week`).
  4. **Schedule (`D-191`):**
     - `Every day` (`DAILY`), `Selected weekdays` (`SELECTED_WEEKDAYS` with 7-day selector explaining that unselected weekdays are neutral rest days that never break a streak), or `Flexible weekly target` (`FLEXIBLE_WEEKLY_TARGET` with optional preferred weekdays for guidance only).
  5. **Reminder & Effective-Date Policy (`D-194`, `U-12`):**
     - Optional reminder toggle + validated `HH:mm` local time.
     - When editing an existing habit that already has recorded activity today (including explicit `0`), displays the **Apply today** vs **Apply tomorrow** policy selector (`U-12` also explains when weekly unit/mode changes defer to the next week boundary).

### 2.6 Focused Record Session & Correction Sheet (`RecordSessionSheet.kt`)
- Modal / secondary sheet opened from Today, Habits, or Habit Detail:
  - Supports **New Session** or **Edit Existing Session** (`editingEntryId != null`).
  - Date selector (`LocalDate`, today or past dates up to creation date per `D-188`; future dates rejected).
  - Value input (`>= 0`, explicitly supporting `0` as an intentional explicit-zero check-in with clear explanation that `0` records a check-in without qualifying for streak completion) + quick preset chips.
  - Optional per-session `entryNote` field (labeled as a session note, distinct from the habit's daily note and the app Journal).
  - Live preview of the resulting **Daily Total**, **Weekly Aggregate** (for weekly habits), and **D-040 Progress Band** before saving.

---

## 3. Key Product Decisions & Trade-Offs

1. **Weekly Progress Representation on Today & Habit Detail (`D-195`, `U-11`, `U-12`):**
   - *Decision:* Extend `HabitSummary` in `Models.kt` and `DayseedRepository.recomputeAllStreakRunsAndSummariesSync` with current-week fields (`weekTotalUnits`, `weekTargetUnits`, `weekProgressBand`, `weekPercentage`, `weekStartLocalDate`, `weekEndLocalDate`) computed against the frozen start-of-week `HabitVersion` (`U-12 Option B`).
   - *Rationale:* Prevents the defect where a weekly habit with a target of `150 min/week` shows `30 / 150 min (20% - Tiny Step)` on Today even when the user has already logged `120 min` earlier in the week (`150 / 150 min = 100% Goal Met` for the week).
2. **Weekly Check-Off (`N days/week`) vs Weekly Session Count (`N sessions/week`):**
   - *Decision:* Allow `TargetPeriod.WEEK` for both `TrackingMode.COMPLETION_ONLY` (where `targetValue` in `1..7` means distinct completed days per week) and `TrackingMode.QUANTITY` / `TrackingMode.DURATION` (where `unitCode = "sessions"` means total logged sessions or `valueUnits` per week).
   - *Rationale:* Directly satisfies Checklist items 20, 21, 31, and 32 while remaining 100% compatible with the v3 SQLite schema (`target_period IN ('DAY','WEEK')` and `target_value IS NULL OR target_value >= 1`).
3. **Event-Driven Run-Ending Animation & Reduced Motion (`D-217`, Checklist 44–45):**
   - *Decision:* Track acknowledged closed run IDs in `DayseedViewModel` (and `app_settings` key `acknowledged_terminal_run_ids`) so the gentle dormant-leaf transition banner/animation triggers only once when an active run transitions to `MISSED`/`PAUSED`/`ARCHIVED`/`COMPLETED`, never replaying on normal Today recompositions, and skips motion when Android's `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` or reduced-motion is active.

---

## 4. Technical Architecture & Data Strategy

### 4.1 Navigation & Component Architecture
```text
MainActivity (Scaffold + 5-Tab NavigationBar + Guarded Undo Snackbar + BackHandler)
 ├── Primary Tabs (NavTab)
 │    ├── TODAY    -> TodayScreen (DaypartHeader + LongestActiveStreakPill + ScheduledHabits + NotScheduledTodaySection + ReflectionCard + CollectedMomentsCard)
 │    ├── HABITS   -> HabitsScreen (Lifecycle FilterChips [All/Active/Paused/Completed/Archived] + Search + Reorder + HabitDirectoryCards)
 │    ├── JOURNAL  -> HistoryScreen (Journal Composer + Draft Recovery + Mood Tags + Reflection Archive)
 │    ├── INSIGHTS -> InsightsScreen (Collected Moments Hero + Living Grove CTA + Mood Insights + Historical Calendar & Daily Log)
 │    └── SETTINGS -> SettingsScreen (Reminders + Week-Start Policy + Backup/Restore + Conflict Banner)
 └── Secondary Route Stack (SecondaryRoute in DayseedViewModel)
      ├── HabitDetail(habitId, selectedDate, originTab)          -> HabitDetailScreen
      ├── CreateEditHabit(habitId?, originTab, parentRoute?)     -> CreateEditHabitScreen
      ├── RecordSession(habitId, date, entryId?, originTab, ...) -> RecordSessionSheet / Screen
      ├── LivingGrove(selectedHabitId?, originTab, ...)          -> LivingGroveScreen (GroveComponents)
      └── CollectedMomentsDetail(yearMonth, selectedDate?, ...)  -> CollectedMomentsDetailSheet
```

### 4.2 Interactive Element & State Mapping
| Screen / Route | Interactive Element | Repository / ViewModel Action | Persistence & Undo Effect |
| :--- | :--- | :--- | :--- |
| **MainActivity** | 5 Bottom Nav Tabs (`nav_today`, `nav_habits`, `nav_journal`, `nav_insights`, `nav_settings`) | `viewModel.selectTab(tab)` | Clears secondary route or preserves per-tab state; Back returns to origin |
| **TodayScreen** | Longest Active Streak pill (`today_main_streak_indicator`) | `viewModel.openLivingGrove()` | Opens `LivingGroveScreen` with `selectLongestActiveRun()` highlighted |
| **TodayScreen** | Habit Card body / Quick Action / Record Session button | `openHabitDetail()`, `logEntry()` / `logOutcome()`, `openRecordSession()` | Persists entry/outcome with idempotency guard + 8s `UndoToken` |
| **TodayScreen** | "Not scheduled today" expandable section (`not_scheduled_today_section`) | Expand/collapse + tap to open detail or log bonus session | Allows `U-2` bonus full completion or `U-7` unscheduled partial hold |
| **TodayScreen** | Collected moments card (`collected_moments_card`) | `viewModel.openCollectedMomentsDetail()` | Opens shared monthly/date detail view without `G2`/`G3` jargon |
| **HabitsScreen** | Lifecycle filter chips, Search bar, Reorder Up/Down, Add Habit FAB | `setHabitsFilter()`, `setHabitsSearchQuery()`, `moveHabitOrder()`, `openCreateHabit()` | Updates `habit_versions.sort_order` and refreshes `habits` flow |
| **HabitDetailScreen** | Session Edit / Delete, Date Note Save, Take a Rest Day, Lifecycle buttons | `openRecordSession(entryId)`, `deleteEntryWithUndo()`, `saveDateNote()`, `takeRestDay()`, `updateHabitLifecycle()`, `deleteHabit()` | Mutates only targeted row; issues guarded 8s `UndoToken` |
| **CreateEditHabitScreen** | 5-section form + `Apply today` / `Apply tomorrow` policy chips | `viewModel.saveHabit(habit, applyToday, policy)` | Enforces `D-194` & `U-12`; creates/updates effective `HabitVersion` |
| **RecordSessionSheet** | Value input (`>=0`), Date selector, Session Note, Save | `viewModel.saveSessionRecord(...)` | Calls `logHabitEntry` (new) or `updateHabitEntry` (existing) |
| **LivingGroveScreen** | Habit branch cards, Grove tree filter, Unqualified `<7d` run toggle | `viewModel.openHabitDetail(habitId)` | Displays `SAPLING_ACTIVE`, `GROWING_TREE`, `DORMANT_TREE`, and `<7d` history |

### 4.3 Step-by-Step Execution Plan (`AUDIT -> DESIGN -> IMPLEMENT -> TEST -> REGRESSION -> RE-AUDIT -> DOCUMENT`)
1. **Step 1 — Baseline Verification:** Run `gradle :app:testDebugUnitTest --rerun-tasks` to verify all 59 Phase 1 JUnit tests (including `t1_43` and `t1_44`) pass before Phase 2 edits begin.
2. **Step 2 — Domain & Repository Enhancements (`Models.kt`, `DayseedRepository.kt`):**
   - Enrich `HabitSummary` with week-to-date progress fields (`weekTotalUnits`, `weekTargetUnits`, `weekProgressBand`, `weekPercentage`, `weekStartLocalDate`, `weekEndLocalDate`) evaluated against the frozen start-of-week `HabitVersion` (`U-12 Option B`).
   - Support weekly check-off targets (`COMPLETION_ONLY` with `TargetPeriod.WEEK` and `targetValue in 1..7`) in `DayseedRepository.saveHabitSync` and `StreakAndGroveEngine`.
   - Add `reorderHabitSync(habitId: String, moveUp: Boolean)` and `deleteHabitEntryWithUndoSync(entryId: String): UndoToken?` in `DayseedRepository` so deleting a single session from Habit Detail or Record Session also supports guarded 8-second Undo.
3. **Step 3 — ViewModel & Five-Destination Navigation (`DayseedViewModel.kt`, `MainActivity.kt`):**
   - Replace 4-tab `NavTab` with 5 destinations (`TODAY`, `HABITS`, `JOURNAL`, `INSIGHTS`, `SETTINGS`).
   - Implement `SecondaryRoute` back-stack, origin-preserving `navigateBack()`, quick-action idempotency guard, and one-shot run-ending transition state respecting reduced motion.
4. **Step 4 — UI Screens & Components (`TodayScreen.kt`, `HabitCard.kt`, `GrowthMark.kt`, `HabitsScreen.kt`, `HabitDetailScreen.kt`, `CreateEditHabitScreen.kt`, `RecordSessionSheet.kt`, `GroveComponents.kt`, `InsightsScreen.kt`):**
   - Implement all Phase 2 screens and components adhering strictly to `DESIGN.md` and `D-040`/`D-215`/`D-216`/`D-217`/`D-218`.
5. **Step 5 — Automated Verification (`Phase2ImplementationTest.kt`):**
   - Write comprehensive Robolectric + Compose UI + Repository/ViewModel tests covering all 60 checklist items in §5 of the assignment.
   - Run `gradle :app:testDebugUnitTest --rerun-tasks` and `compile_applet` to verify 100% pass rate across all Phase 1 and Phase 2 tests.
6. **Step 6 — Documentation Reconciliation & Human Validation Register:**
   - Append Phase 2 human/device validation test cases (`P2-H01`..`P2-H17`, marked `NOT RUN`) to `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`.
   - Reconcile `docs/09-DECISION-LOG.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/12-PROGRESS-AND-HANDOVER.md`, and root `implementation_plan.md`.
