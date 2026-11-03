# Dayseed — Phase 2 Correction Plan (Targeted Remediation Before Phase 3)

## 1. Execution Rules, Baseline Verification & Branch Discipline

1. **Scope & Phase 3 Prohibition:**
   - The original targeted remediation covered Corrections A–D. The independent final source audit expanded the follow-up to Corrections E–I after finding additional gaps in save-error lifecycle, legacy editor parity, flexible-weekly selected-date status, Record Session failure handling/session-count preview, destructive delete confirmation, deletion Undo reachability, and the run-ending Grove animation.
   - This execution record covers Corrections A–I, the expanded automated regression tests (`t2_c01`–`t2_c06)), the Android validation workflow, and canonical documentation/traceability updates. The canonical `implementation_plan.md` and handover are authoritative.
   - **Explicit Phase 3 Prohibition:** Do **not** begin Phase 3 (Journal, Calendar, History, Insights, or any new feature scope) under any circumstances. Completing these corrections does not authorise starting Phase 3.
2. **Baseline Commit Verification & Dedicated Correction Branch:**
   - Before editing any file, run:
     - `git status`
     - `git rev-parse HEAD`
     - `git branch --show-current`
     - `git ls-remote origin refs/heads/main`
   - Verify the current repository baseline is `main` at `cbc2718bb803315de6ad03d570f960ea59fe57cd`. The original A–D correction commit was present on main; this final follow-up is isolated on `fix/phase-2-final-remediation-audit` and must not be merged without explicit owner approval.
   - Create and switch to a dedicated correction branch before making code or documentation changes (do not implement directly on `main`):
     - `git checkout -b fix/phase-2-remediation-before-phase-3`
3. **Preservation & Integrity Rules:**
   - Do not redesign the existing architecture, remove existing functionality to make tests pass, or silently alter behaviour when an operation encounters an error.
   - Preserve all approved Dayseed decisions (`D-040`, `D-188`–`D-195`, `D-213`–`D-222`, `U-1`–`U-12`), database version 3 compatibility (`D-219`), and existing user data.
   - Never mark tests as `PASSED` without executing them and inspecting their output. Never mark physical-device or human validation items (`P2-C01`–`P2-C05`) as `PASSED` based on code inspection or JVM/Robolectric tests.

---

## 2. Correction A — Create/Edit Habit Navigation, Observable Save-Error State & Deletion Navigation

### 2.1 Root Cause
1. In `CreateEditHabitScreen.kt` (lines 102–105), `onSave` calls `viewModel.saveHabit(habit, applyTodayWhenLogged)` and immediately calls `onNavigateBack()`.
2. In `DayseedViewModel.kt` (lines 634–660), `saveHabit(...)` already calls `closeAddEditHabit()`, which pops `SecondaryRoute.CreateEditHabit` via `navigateBack()`.
3. Consequently:
   - **Double pop on success:** `HabitDetail -> Edit -> Save` pops `CreateEditHabit` inside `saveHabit()` and then pops `HabitDetail` inside `onNavigateBack()`, landing on the primary tab instead of returning to `SecondaryRoute.HabitDetail`.
   - **Pop on failure:** When `saveHabit(...)` catches an exception and returns `null`, `CreateEditHabitScreen` still invokes `onNavigateBack()`, closing the editor and losing all entered form state.
   - **Missing observable save-error state:** `DayseedViewModel` currently only writes errors to `_userMessage` and does not expose a dedicated observable save-error state for the editor, nor does `CreateEditHabitForm`'s `onSave` callback receive a boolean/result indicating whether the save succeeded.
   - **Split deletion navigation:** `CreateEditHabitScreen` calls `viewModel.deleteHabit(id)` followed by `onNavigateBack()`, while `DayseedViewModel.deleteHabit(habitId)` only checks `if (_currentSecondaryRoute.value is SecondaryRoute.HabitDetail)`. Deleting from `HabitDetail -> CreateEditHabit` would pop `CreateEditHabit` and leave the user on a stale `HabitDetail` screen for a deleted habit.

### 2.2 Required Implementation (`DayseedViewModel.kt` & `CreateEditHabitScreen.kt`)
1. **Defined, Observable Save-Error State in `DayseedViewModel.kt`:**
   - Add an explicit `StateFlow` for save errors:
     ```kotlin
     private val _lastSaveError = MutableStateFlow<String?>(null)
     val lastSaveError: StateFlow<String?> = _lastSaveError.asStateFlow()
     val lastErrorMessage: StateFlow<String?> get() = lastSaveError

     fun clearSaveError() {
         _lastSaveError.value = null
     }
     ```
   - Also clear `_lastSaveError.value = null` inside `openCreateEditHabit(...)` and `closeAddEditHabit()`.
2. **Single Save Navigation Ownership & Result Communication:**
   - In `DayseedViewModel.saveHabit(habit: Habit, applyTodayWhenLogged: Boolean = true, policy: HabitConfigEffectivePolicy? = null): HabitVersion?`:
     - Clear `_lastSaveError.value = null` at the start of the attempt.
     - On success (`repository.saveHabitSync(...)` returns `savedVer`):
       - Clear `_lastSaveError.value = null`.
       - Call `closeAddEditHabit()` (which pops `SecondaryRoute.CreateEditHabit` once from `_secondaryRouteStack`, returning to `parentRoute`).
       - Set `_userMessage.value = "Habit saved (effective ${savedVer.effectiveFromLocalDate})"`.
       - Return `savedVer`.
     - On failure (`catch (e: Exception)`):
       - Set `val msg = e.message ?: "Unable to save habit"`.
       - Set `_lastSaveError.value = msg` and `_userMessage.value = msg`.
       - Do **not** call `closeAddEditHabit()` or `navigateBack()`.
       - Return `null`.
   - In `CreateEditHabitScreen.kt`:
     - Collect `val saveError by viewModel.lastSaveError.collectAsState()`.
     - Update `CreateEditHabitForm`'s `onSave: (Habit, Boolean) -> Boolean` callback and `CreateEditHabitScreen`'s `onSave` handler so it calls `viewModel.saveHabit(habit, applyTodayWhenLogged) != null` and does **not** call `onNavigateBack()` directly (since `viewModel.saveHabit` already calls `closeAddEditHabit()` on success, and must keep the screen open on failure).
     - Keep every entered field intact in `CreateEditHabitForm` when `saveHabit` fails.
     - Render `saveError` (when non-null) inside `CreateEditHabitForm` as a prominent inline error card/banner (`Modifier.testTag("habit_save_error_banner")`) displaying the actual failure reason.
     - Clear `saveError` (`viewModel.clearSaveError()`) whenever the user edits relevant inputs (such as changing the `Apply today` / `Apply tomorrow` chip, target, unit, schedule, or name) or retries saving.
3. **Predictable Return Paths on Save and Cancel:**
   - `Today -> CreateEditHabit -> Save` pops `CreateEditHabit` once and returns to `Today` (`viewModel.currentSecondaryRoute.value == null` and `viewModel.selectedTab.value == NavTab.TODAY`).
   - `Habits -> CreateEditHabit -> Save` pops `CreateEditHabit` once and returns to `Habits` (`viewModel.currentSecondaryRoute.value == null` and `viewModel.selectedTab.value == NavTab.HABITS`).
   - `HabitDetail(habitId) -> CreateEditHabit(habitId) -> Save` pops `CreateEditHabit` once and returns to `SecondaryRoute.HabitDetail` for the same `habitId`.
   - `Cancel` or system Back calls `viewModel.closeAddEditHabit()` (via `onNavigateBack`), popping `CreateEditHabit` once and returning to the exact screen that opened it.
4. **Separate, Single-Owner Deletion Navigation (`DayseedViewModel.deleteHabit` & `CreateEditHabitScreen.kt`):**
   - In `DayseedViewModel.deleteHabit(habitId: String)`:
     - Execute `repository.deleteHabitPermanentlySync(habitId)`.
     - Clean up `_secondaryRouteStack` in one place and explicitly synchronize `_currentSecondaryRoute`:
       ```kotlin
       val filteredStack = _secondaryRouteStack.value.filterNot { route ->
           (route is SecondaryRoute.CreateEditHabit && route.habitId == habitId) ||
               (route is SecondaryRoute.HabitDetail && route.habitId == habitId)
       }
       _secondaryRouteStack.value = filteredStack
       _currentSecondaryRoute.value = filteredStack.lastOrNull()
       if (_editingHabit.value?.habitId == habitId) {
           _editingHabit.value = null
       }
       if (_currentSecondaryRoute.value !is SecondaryRoute.CreateEditHabit) {
           _isAddHabitOpen.value = false
       }
       ```
     - This guarantees that deleting a habit from either `HabitDetail` or `HabitDetail -> CreateEditHabit` removes both routes for the deleted habit, keeps `_secondaryRouteStack` and `_currentSecondaryRoute` in sync (`lastOrNull()`), and returns cleanly to the underlying primary tab (`NavTab.TODAY` or `NavTab.HABITS`) without double-popping or landing on a deleted habit's `HabitDetail` screen.
   - In `CreateEditHabitScreen.kt`:
     - Change `onDelete = initialHabit?.let { { id -> viewModel.deleteHabit(id) } }` (removing the redundant `onNavigateBack()` call so deletion navigation has a single owner).

### 2.3 Required Automated Regression Tests (`Phase2ImplementationTest.kt`)
1. **Create from Today -> Save:** Selects `NavTab.TODAY`, opens `CreateEditHabit(null)`, saves a valid habit, and asserts:
   ```kotlin
   assertEquals(null, viewModel.currentSecondaryRoute.value)
   assertEquals(NavTab.TODAY, viewModel.selectedTab.value)
   ```
2. **Create from Habits -> Save:** Selects `NavTab.HABITS`, opens `CreateEditHabit(null)`, saves a valid habit, and asserts:
   ```kotlin
   assertEquals(null, viewModel.currentSecondaryRoute.value)
   assertEquals(NavTab.HABITS, viewModel.selectedTab.value)
   ```
3. **HabitDetail -> Edit -> Save:** Opens `openHabitDetail(habitId)`, opens `openEditHabit(habit)` (`CreateEditHabit(habitId)`), saves an updated habit, and asserts `viewModel.currentSecondaryRoute.value` is `SecondaryRoute.HabitDetail` with `habitId == habit.habitId` (not popped back to the root tab).
4. **HabitDetail -> Edit -> Cancel:** Opens `openHabitDetail(habitId)`, opens `openEditHabit(habit)`, cancels (`closeAddEditHabit()`), and asserts `viewModel.currentSecondaryRoute.value` is `SecondaryRoute.HabitDetail` with `habitId == habit.habitId`.
5. **Failed Save Keeps Editor Open & Populates `lastSaveError` / `lastErrorMessage`:** Triggers a repository save rejection (e.g., `D-194` `Apply Today` lock when activity is already recorded today), asserts `saveHabit(...) == null`, `viewModel.currentSecondaryRoute.value is SecondaryRoute.CreateEditHabit`, `viewModel.lastSaveError.value != null` (`viewModel.lastErrorMessage.value != null`), and verifies that clearing/retrying with valid `Apply Tomorrow` clears `viewModel.lastSaveError.value` and navigates back once to the parent route.
6. **Delete from `HabitDetail -> Edit`:** Selects `NavTab.HABITS`, opens `openHabitDetail(habitId)`, opens `openEditHabit(habit)`, calls `viewModel.deleteHabit(habitId)`, and asserts `viewModel.secondaryRouteStack.value.isEmpty()`, `viewModel.currentSecondaryRoute.value == null`, and `viewModel.selectedTab.value == NavTab.HABITS`.

---

## 3. Correction B — `D-194` Explicit Zero Activity & Effective-Date Protection

### 3.1 Root Cause
1. `CreateEditHabitScreen.kt` (line 84) computes `alreadyLoggedToday` using `viewModel.repository.hasQualifyingActivityOnDate(it.habitId, today)`, which checks `it.valueUnits > 0` and ignores explicit `0` entries (`valueUnits == 0`).
2. However, `DayseedRepository.saveHabitSync` enforces the `D-194` version lock whenever any row exists for `(habitId, today)` in `habit_entries` (including `value_units = 0`) or `habit_day_outcomes` (`hasRecordedActivityForVersionLockOnDate`).
3. Furthermore, `DayseedViewModel.saveHabit` (lines 640–652) catches `HabitConfigurationLockedTodayException` when the user requests `Apply Today` and silently retries `repository.saveHabitSync` with `applyTodayWhenLogged = false` (`HabitConfigEffectivePolicy.APPLY_TOMORROW`), overriding the user's explicit selection without warning.

### 3.2 Required Implementation (`CreateEditHabitScreen.kt`, `DayseedViewModel.kt`, `DayseedRepository.kt`)
1. **Consistent `D-194` Activity Detection in `CreateEditHabitScreen.kt`:**
   - Replace `viewModel.repository.hasQualifyingActivityOnDate(it.habitId, today)` at `CreateEditHabitScreen.kt:84` with `viewModel.repository.hasRecordedActivityForVersionLockOnDate(it.habitId, today)` (also observing `viewModel.habitEntries` and `viewModel.habitDayOutcomes` so `alreadyLoggedToday` updates reactively if activity changes).
   - Verify `DayseedRepository.hasRecordedActivityForVersionLockOnDate(habitId: String, targetDate: LocalDate): Boolean` returns `true` whenever any of the following exist on `targetDate`:
     - Positive `habit_entries` (`valueUnits > 0`)
     - Explicit zero `habit_entries` (`valueUnits == 0`)
     - `habit_day_outcomes` (`COMPLETED` or `EFFORT_LOGGED`)
   - In `CreateEditHabitForm`, initialize `applyTodayWhenLogged` to `!alreadyLoggedToday` (so when activity is already recorded today, `"Apply tomorrow"` is pre-selected by default, while `"Apply today"` and `"Apply tomorrow"` remain selectable chips in `d194_effective_date_prompt`).
2. **Remove Silent Policy Fallback in `DayseedViewModel.saveHabit(...)`:**
   - Remove the `try { ... } catch (_: HabitConfigurationLockedTodayException) { repository.saveHabitSync(..., applyTodayWhenLogged = false, policy = HabitConfigEffectivePolicy.APPLY_TOMORROW) }` block from `DayseedViewModel.saveHabit(...)`.
   - Call `repository.saveHabitSync(habit = habit, applyTodayWhenLogged = applyTodayWhenLogged, policy = policy)` directly.
   - If the user selects `Apply today` (`applyTodayWhenLogged = true` or `policy = HabitConfigEffectivePolicy.APPLY_TODAY`) while today's version is locked by recorded activity and evaluation-affecting fields changed, `saveHabitSync` throws `HabitConfigurationLockedTodayException`, `DayseedViewModel.saveHabit` catches it as a save failure, populates `lastSaveError` (`lastErrorMessage`), keeps `CreateEditHabitScreen` open with all form values intact, and returns `null` without altering the user's selected policy.
3. **Preserve `U-12` Weekly Rules:**
   - Keep `U-12 Option B` intact:
     - Ordinary daily configuration change after recorded activity with `Apply Tomorrow` -> takes effect tomorrow (`today.plusDays(1)`).
     - Weekly target change with the same unit and tracking mode -> takes effect tomorrow (`today.plusDays(1)`) while the current week retains its frozen start-of-week `HabitVersion` configuration.
     - Weekly unit or tracking-mode change -> takes effect at the next weekly boundary (`currentWeekEnd.plusDays(1)`).

### 3.3 Required Automated Regression Tests (`Phase2ImplementationTest.kt`)
1. **Positive Activity Already Recorded Today (`valueUnits > 0`):**
   - Log a positive `QUANTITY` or `DURATION` entry today (`valueUnits = 10`).
   - Verify `repository.hasRecordedActivityForVersionLockOnDate(habitId, today)` is `true`.
   - Attempt an evaluation-affecting edit via `viewModel.saveHabit(updated, applyTodayWhenLogged = true)` -> assert it returns `null`, populates `lastSaveError`, does not silently create a tomorrow version, and leaves today's effective `HabitVersion` and today's entry (`10`) unchanged.
2. **Explicit Zero Activity Already Recorded Today (`valueUnits == 0`):**
   - Log an explicit zero `QUANTITY` entry today (`valueUnits = 0`).
   - Verify `repository.hasRecordedActivityForVersionLockOnDate(habitId, today)` is `true` (while `hasQualifyingActivityOnDate(habitId, today)` is `false`).
   - Attempt an evaluation-affecting edit via `viewModel.saveHabit(updated, applyTodayWhenLogged = true)` -> assert it returns `null`, populates `lastSaveError`, keeps `CreateEditHabit` open, and does not silently retry as `Apply tomorrow`.
3. **Recorded Day Outcome Already Recorded Today (`habit_day_outcomes`):**
   - Log a `COMPLETED` (and/or `EFFORT_LOGGED`) outcome in `habit_day_outcomes` today.
   - Verify `repository.hasRecordedActivityForVersionLockOnDate(habitId, today)` is `true`.
   - Attempt an evaluation-affecting edit with `applyTodayWhenLogged = true` -> assert it is rejected (`returns null`, `lastSaveError != null`) without silent retry.
4. **Ordinary Daily `Apply Tomorrow` Behaviour (Preserves Today's Version & Activity):**
   - After recorded activity today (including explicit `0`) on a daily (`TargetPeriod.DAY`) habit, save an updated target with `applyTodayWhenLogged = false` (`Apply tomorrow`).
   - Assert `saveHabit(...)` succeeds and returns a `HabitVersion` with `effectiveFromLocalDate == today.plusDays(1).toString()`.
   - Assert `repository.getEffectiveHabitVersion(habitId, today)` still has the original target/configuration, and today's recorded entry/outcome is completely unchanged.
5. **Weekly Same-Unit Target Change (`U-12 Option B`):**
   - On a weekly (`TargetPeriod.WEEK`) habit with recorded activity today, change `targetValue` (keeping `unitCode` and `trackingMode` identical) and save with `applyTodayWhenLogged = false`.
   - Assert the saved `HabitVersion` has `effectiveFromLocalDate == today.plusDays(1).toString()`, while the current week's `computeWeeklyProgressSnapshot` retains the frozen start-of-week `HabitVersion` target for the remainder of the current week.
6. **Weekly Unit or Tracking-Mode Change Deferred to Next Weekly Boundary (`U-12 Option B`):**
   - On a weekly (`TargetPeriod.WEEK`) habit, change `unitCode` (e.g., `"sessions"` -> `"pages"`) or `trackingMode` midweek and save.
   - Assert the saved `HabitVersion` is deferred to the next weekly boundary (`effectiveFromLocalDate == currentWeekEnd.plusDays(1).toString()`), and the current week retains its frozen start-of-week `HabitVersion`.

---

## 4. Correction C — Weekly Session Count Calculation & Frozen Start-of-Week Version (`U-12 Option B`)

### 4.1 Root Cause
1. In `StreakAndGroveEngine.computeWeeklyProgressSnapshot` (lines 782–788), `StreakAndGroveEngine.evaluateWeeklySegment` (lines 506–513), and `DayseedRepository.evaluateDerivedDayStatus` (lines 2309–2316), weekly `QUANTITY` habits always sum `entry.valueUnits` (`ProgressBandClassifier.sumUnitsExact(...)`), even when `unitCode == "sessions"`.
2. Additionally, there is a **version-freezing inconsistency** across the three locations:
   - `computeWeeklyProgressSnapshot()` resolves `frozenVersion` at `firstEffectiveDateInWeek` (`maxOf(firstDate, weekStart)`).
   - `evaluateWeeklySegment()` currently resolves `segVersion` at `segmentDates.first()`, which could be midweek if a pause/resume segment starts midweek, rather than using the frozen start-of-week `HabitVersion` for the evaluation week's tracking mode, unit, and weekly target under `U-12 Option B`.
   - `DayseedRepository.evaluateDerivedDayStatus()` (line 2308) uses `eff` (the version effective on `date` rather than the frozen start-of-week `HabitVersion` for that week) to determine weekly schedule semantics, `wTarget`, and whether to count sessions or sum units. If a same-unit weekly target edit took effect midweek (`effectiveFromLocalDate = tomorrow`) or a habit was paused/archived later in the week, `evaluateDerivedDayStatus` must use the frozen start-of-week `HabitVersion` for the weekly measurement configuration and weekly schedule semantics while keeping the individual date's lifecycle state (`eff.lifecycleState`) separate.

### 4.2 Required Implementation (`StreakAndGroveEngine.kt` & `DayseedRepository.kt`)
1. **Shared Frozen Start-of-Week Version & Session-Mode Helper in `StreakAndGroveEngine.kt`:**
   - Add canonical helpers in `StreakAndGroveEngine`:
     ```kotlin
     fun isWeeklySessionCountHabit(version: HabitVersion): Boolean {
         return version.trackingMode.canonical() == TrackingMode.QUANTITY &&
             version.targetPeriod == TargetPeriod.WEEK &&
             version.unitCode?.trim()?.equals("sessions", ignoreCase = true) == true
     }

     fun resolveFrozenWeekVersion(
         sortedVersions: List<HabitVersion>,
         weekStart: LocalDate,
         firstHabitDate: LocalDate,
         fallbackDate: LocalDate,
         fallbackVersion: HabitVersion? = null
     ): HabitVersion? {
         val anchorDate = if (firstHabitDate.isAfter(weekStart)) firstHabitDate else weekStart
         return resolveEffectiveVersion(sortedVersions, anchorDate)
             ?: resolveEffectiveVersion(sortedVersions, fallbackDate)
             ?: fallbackVersion
     }

     fun countDailyUnitsOrSessionsForWeek(
         frozenWeekVersion: HabitVersion,
         dayEntries: List<HabitEntry>
     ): BigInteger {
         return if (isWeeklySessionCountHabit(frozenWeekVersion)) {
             BigInteger.valueOf(dayEntries.count { it.valueUnits > 0 }.toLong())
         } else {
             ProgressBandClassifier.sumUnitsExact(dayEntries.map { it.valueUnits })
         }
     }
     ```
2. **Consistent Application Across All Three Locations:**
   - **Location 1 — `StreakAndGroveEngine.computeWeeklyProgressSnapshot`:**
     - Resolve `frozenVersion` via `resolveFrozenWeekVersion(sortedVersions, weekStart, firstDate, evaluationDate, fallbackVersion)!!`.
     - For `TrackingMode.QUANTITY, TrackingMode.DURATION`, use `countDailyUnitsOrSessionsForWeek(frozenVersion, dayEntries)` so that when `isWeeklySessionCountHabit(frozenVersion)` is true, each positive entry (`valueUnits > 0`) adds `1` session, multiple positive entries on the same date add multiple sessions, and explicit zero entries (`valueUnits == 0`) add `0` sessions.
   - **Location 2 — `StreakAndGroveEngine.evaluateWeeklySegment` (inside `evaluateHabitStreakRuns`):**
     - Resolve `frozenWeekVersion = resolveFrozenWeekVersion(sortedVersions, weekStart, firstDate, segmentDates.first()) ?: return` at the week level (`weekStart`), and use `frozenWeekVersion` consistently for `targetBi`, `trackingMode`, weekly schedule semantics, and `countDailyUnitsOrSessionsForWeek(frozenWeekVersion, dEntries)`.
     - Preserve existing per-day lifecycle segmentation (`activeSegmentDates`, `wasInterruptedByClosureAfter`, `wasPrecededByClosure`, `PAUSED`/`ARCHIVED`/`COMPLETED` run closure on the pause/archive/complete date under `U-5 Option A`).
   - **Location 3 — `DayseedRepository.evaluateDerivedDayStatus`:**
     - Keep the individual date's lifecycle state check (`if (eff.lifecycleState != HabitLifecycleState.ACTIVE) return DerivedDayStatus.INACTIVE_LIFECYCLE`) separate from the frozen weekly measurement configuration so that pausing or archiving a habit never alters historical weekly measurement rules.
     - Resolve the week's frozen start-of-week `HabitVersion` (`frozenWeekVersion`) for `date`'s week using `StreakAndGroveEngine.resolveFrozenWeekVersion(...)`.
     - Use `frozenWeekVersion` not only for `wTarget` (`(frozenWeekVersion.targetValue ?: 1).coerceAtLeast(1)`) and session-counting mode (`StreakAndGroveEngine.countDailyUnitsOrSessionsForWeek(frozenWeekVersion, dEntries)`), but also for the applicable weekly schedule semantics (`frozenWeekVersion.scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET || frozenWeekVersion.targetPeriod == TargetPeriod.WEEK`).
   - **Verify UI & Summary Consumers (`Today`, `HabitDetail`, `HabitSummary`):**
     - Ensure `DayseedRepository.recomputeDerivedState` (which builds `HabitSummary` for `Today`, `Habits`, and `HabitDetail`) uses the `WeeklyProgressSnapshot` from `StreakAndGroveEngine.computeWeeklyProgressSnapshot`, so `Today` progress subtitles (`2 / 3 sessions`), `HabitDetail` weekly progress, flexible weekly target satisfaction (`weekSatisfied`), and weekly streak runs all agree 100%.

### 4.3 Required Automated Regression Tests (`Phase2ImplementationTest.kt`)
1. **Positive Session Entries Count as 1 Session Each (Not Summed by `valueUnits`):**
   - Weekly `QUANTITY` habit (`targetPeriod = TargetPeriod.WEEK`, `targetValue = 3`, `unitCode = "sessions"`).
   - Log Monday entry (`valueUnits = 20`) and Tuesday entry (`valueUnits = 20`).
   - Assert `weekTotalUnits == BigInteger.valueOf(2)`, `weekTargetUnits == BigInteger.valueOf(3)`, `weekPercentage == 66`, `weekProgressBand == ProgressBand.HALFWAY` (not `40 / 3 = 1333%` `ProgressBand.HUGE`).
   - Log Wednesday entry (`valueUnits = 15`) -> assert `3 / 3 sessions` (`100%`, `ProgressBand.GOAL_MET`).
2. **Multiple Positive Sessions on the Same Date Count Separately:**
   - Log two positive entries on Monday (`valueUnits = 10` and `valueUnits = 25`).
   - Assert weekly completed sessions = `2 / 3 sessions` (`66%`, `ProgressBand.HALFWAY`).
3. **Explicit Zero Session Entry (`valueUnits == 0`):**
   - Log an explicit zero entry (`valueUnits = 0`) on Thursday.
   - Assert the zero entry is persisted in `habit_entries` and `hasZero` is recorded for Thursday, but completed weekly sessions remain `2 / 3 sessions` (`0` added to session count).
4. **Editing, Deleting, and Undoing Sessions:**
   - Edit one of two positive sessions from `valueUnits = 20` to `valueUnits = 30` (`updateHabitEntrySync`) -> session count remains `2 / 3`.
   - Edit one positive session to `valueUnits = 0` -> session count drops to `1 / 3` (`33%`, `ProgressBand.GETTING_STARTED`).
   - Delete a positive session (`deleteHabitEntryWithUndoSync`) -> session count drops by `1` immediately.
   - Undo the deletion (`executeUndoSync`) -> session count increases back by `1` immediately.
5. **Midweek Configuration Change & Per-Day Lifecycle Independence (`U-12 Option B` Frozen Start-of-Week Version):**
   - Create a weekly session habit (`3 sessions / week`, `unitCode = "sessions"`) effective Monday.
   - Log 1 session on Monday (`valueUnits = 15`).
   - On Tuesday, save a same-unit target change (`5 sessions / week`, effective Wednesday) and/or a unit change (`pages`, deferred to next Monday).
   - Assert that across Wednesday–Sunday of the current week, `computeWeeklyProgressSnapshot`, `evaluateDerivedDayStatus`, and `evaluateHabitStreakRuns` all continue to use the frozen Monday `HabitVersion` (`3 sessions / week`, `unitCode = "sessions"`), so logging 2 more sessions on Wednesday and Thursday satisfies `3 / 3 sessions` (`100%`, `ProgressBand.GOAL_MET`) in the current week, and the new configuration takes effect in the next week.
6. **Non-Session `QUANTITY` and `DURATION` Summation Preserved:**
   - Verify that a weekly `QUANTITY` habit with `unitCode = "pages"` (`targetValue = 100`) logging `20` and `30` sums to `50 / 100 pages` (`50%`, `ProgressBand.HALFWAY`), and a weekly `DURATION` habit with `unitCode = "min"` (`targetValue = 60`) logging `20` and `25` sums to `45 / 60 min` (`75%`, `ProgressBand.ALMOST_THERE`).

---

## 5. Correction D — Flexible Weekly Habit Partition on `Today`

### 5.1 Root Cause
In `DayseedRepository.kt` (line 2128), `recomputeDerivedState` computes `isScheduledToday` for `ScheduleKind.FLEXIBLE_WEEKLY_TARGET` as:
```kotlin
if (weekSatisfied && !matchesPreferred) false else matchesPreferred
```
When `weekSatisfied == false` and `matchesPreferred == false` (a non-preferred weekday while the weekly target is still incomplete), this expression evaluates to `false`, hiding the incomplete flexible weekly habit under `"Not scheduled today"`.

### 5.2 Required Implementation (`DayseedRepository.kt`)
Replace the `ScheduleKind.FLEXIBLE_WEEKLY_TARGET` branch in `DayseedRepository.recomputeDerivedState` (lines 2124–2129) with:
```kotlin
ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
    val prefMask = effToday.preferredWeekdaysMask
    val matchesPreferred = prefMask == null ||
        prefMask == 0 ||
        WeekdayMask.contains(prefMask, today.dayOfWeek)

    val weekSatisfied = weekSnapshot.weekTotalUnits >= weekSnapshot.weekTargetUnits

    !weekSatisfied || matchesPreferred
}
```
This preserves the existing preferred-day behaviour (`null` or `0` meaning all weekdays, and `WeekdayMask.contains(prefMask, today.dayOfWeek)`) while correcting the incomplete-week behaviour (`!weekSatisfied || matchesPreferred`). Also verify that `ScheduleKind.DAILY` and `ScheduleKind.SELECTED_WEEKDAYS` remain unchanged.

### 5.3 Required Automated Regression Tests (`Phase2ImplementationTest.kt`)
1. **Complete Flexible Weekly Scheduling Sequence (`3 sessions / week`, preferred `Mon/Wed/Fri` via `WeekdayMask.toMask(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))`):**
   - **Before reaching the weekly target (`!weekSatisfied`, e.g., `0 / 3` or `2 / 3`):**
     - On Monday (preferred day): `isScheduledToday == true` (in `"Scheduled today"`).
     - On Tuesday (non-preferred day): `isScheduledToday == true` (in `"Scheduled today"` because `!weekSatisfied` is true).
   - **After reaching the weekly target (`weekSatisfied`, `3 / 3 sessions` met, `ProgressBand.GOAL_MET`):**
     - On Thursday (non-preferred day): `isScheduledToday == false` (moves to `"Not scheduled today"` because `weekSatisfied == true` and `matchesPreferred == false`).
     - On Friday (preferred day): `isScheduledToday == true` (remains in `"Scheduled today"` because `matchesPreferred == true`).
   - **Bonus Logging When Not Scheduled Today:**
     - On Thursday (while in `"Not scheduled today"` at `3 / 3 sessions`), log a 4th positive session -> assert the entry succeeds and updates weekly progress to `4 / 3 sessions` (`133%`, `ProgressBand.EXTRA`).
2. **Unchanged `DAILY` and `SELECTED_WEEKDAYS` Scheduling:**
   - Assert a `ScheduleKind.DAILY` habit has `isScheduledToday == true` on all 7 weekdays whether completed or not.
   - Assert a `ScheduleKind.SELECTED_WEEKDAYS` habit (`Mon/Wed/Fri`) has `isScheduledToday == true` on Monday/Wednesday/Friday and `isScheduledToday == false` on Tuesday/Thursday/Saturday/Sunday regardless of completion state.

---

## 6. Human Validation Register (`docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`)

Append the following five Phase 2 correction human-validation items to `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` with status `NOT RUN` (never mark them `PASSED` without physical device / human execution):

| ID | Scenario | Exact Procedure | Expected Outcome | Status |
|---|---|---|---|---|
| `P2-C01` | Habit Detail -> Edit -> Save Return Path | 1. Open any existing habit's `HabitDetail` screen from `Today` or `Habits`.<br>2. Tap the `Edit` action to open `CreateEditHabitScreen`.<br>3. Change the habit description or name and tap `Save`. | `CreateEditHabitScreen` closes with a single pop and returns directly to `HabitDetail` for that same habit (showing the updated fields), rather than popping back to `Today` or `Habits`. | `NOT RUN` |
| `P2-C02` | Failed Save Keeps Form Open with Error Banner | 1. Log any activity (or `0` check-in) for a habit on `Today`.<br>2. Open `Edit Habit`, change the target value or unit, select `Apply today` in the `D-194` prompt, and tap `Save`. | The save is rejected; `CreateEditHabitScreen` remains open with all entered fields preserved and displays the inline error banner (`habit_save_error_banner`) explaining that today's configuration is locked by recorded activity. Selecting `Apply tomorrow` clears the error and saves cleanly. | `NOT RUN` |
| `P2-C03` | Explicit Zero Entry Locks Today's Configuration (`D-194`) | 1. On `Today`, record an explicit `0` check-in (`Log 0`) for a `QUANTITY` or `DURATION` habit.<br>2. Open `Edit Habit` for that habit on the same day. | The `D-194` `"You already logged this habit today"` effective-timing prompt (`Apply today` / `Apply tomorrow`) is visible. Choosing `Apply tomorrow` saves a new version effective tomorrow while keeping today's version and `0` entry intact. | `NOT RUN` |
| `P2-C04` | Weekly Session Habit Counts Sessions (`unitCode = "sessions"`) | 1. Create a weekly `QUANTITY` habit with target `3` and unit `sessions`.<br>2. Log a session with value `20` on day 1 and another session with value `20` on day 2.<br>3. Inspect `Today` and `HabitDetail`, then edit/delete/undo a session. | Progress displays `2 / 3 sessions` (`66%`, `Halfway`), not `40 / 3`. Logging a `0` check-in keeps progress at `2 / 3 sessions`. Deleting a session reduces progress to `1 / 3 sessions`, and tapping `Undo` restores `2 / 3 sessions`. | `NOT RUN` |
| `P2-C05` | Flexible Weekly Habit Partition Before & After Target Met | 1. Create a flexible weekly habit (`3 sessions / week`, preferred days `Mon/Wed/Fri`).<br>2. View `Today` on a non-preferred day (e.g. `Tue` or `Thu`) while progress is `< 3`.<br>3. Complete the 3rd session so weekly progress reaches `3 / 3`, then view `Today` on a non-preferred day (`Thu`) and a preferred day (`Fri`).<br>4. Log a bonus 4th session from `"Not scheduled today"` on `Thu`. | While `< 3 / 3`, the habit appears under `"Scheduled today"` on both preferred and non-preferred days. Once `3 / 3` is reached, it moves to `"Not scheduled today"` on non-preferred days (`Thu`), remains under `"Scheduled today"` on preferred days (`Fri`), and still permits bonus session logging (`4 / 3 sessions`, `Extra`). | `NOT RUN` |

---

## 7. Build, Test & Documentation Verification Commands

1. **Automated Unit & Robolectric Test Suite:**
   ```bash
   gradle :app:testDebugUnitTest --rerun-tasks
   ```
2. **Debug APK Build Verification:**
   ```bash
   gradle :app:assembleDebug
   ```
3. **Documentation Reconciliation:**
   - Update `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` (add `P2-C01`–`P2-C05` as `NOT RUN`).
   - Update `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/12-PROGRESS-AND-HANDOVER.md`, and root `implementation_plan.md` to reflect Corrections A–D and the verified test counts.

---

## 8. Acceptance Criteria & Final Evidence Report Format

### 8.1 Acceptance Criteria
- **Automated Verification (`PASSED` required before completion):**
  - All four corrections (A, B, C, D) implemented in source code with zero regressions.
  - All existing and new regression unit/Robolectric tests in `Phase1ImplementationTest` and `Phase2ImplementationTest` pass under `gradle :app:testDebugUnitTest --rerun-tasks`.
  - Debug APK builds cleanly under `gradle :app:assembleDebug`.
- **Human / Physical-Device Validation (`PENDING`):**
  - `P2-C01` through `P2-C05` documented in `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` with honest `NOT RUN` status until executed by the owner on a physical device or interactive emulator session.
  - Overall completion status must be reported as:
    `AUTOMATED CORRECTIONS PASSED — HUMAN VALIDATION PENDING`

### 8.2 Final Evidence Report Structure
Upon completing implementation and verification, produce the final report containing:
1. Inspected baseline commit SHA, follow-up branch, PR number, and base/head comparison.
2. Root-cause and correction summary for A–I, with exact changed files.
3. Summary of all regression tests added/expanded (`t2_c01`–`t2_c06`) and the exact workflow result for the PR head.
4. Exact Gradle commands, exit codes, test counts, and retained report/APK artifact links from GitHub Actions.
5. Updated canonical documentation, traceability, screen contracts, handover, and this execution artifact.
6. Human validation register summary: P2-C01–P2-C06 and P2-H01–P2-H17 remain `NOT RUN` until performed on a device/emulator.
7. Remaining limitations and confirmation that Phase 3 has not been started.
8. Final status must distinguish automated CI results from pending human/device validation; do not declare Phase 2 fully accepted while a required check remains outstanding.
