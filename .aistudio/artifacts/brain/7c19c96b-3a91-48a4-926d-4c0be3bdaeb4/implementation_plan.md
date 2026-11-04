# Dayseed — Phase 2 Final Remediation Plan (`F-01` through `F-06`)

Complete and verify all six Phase 2 audit remediations (`F-01` through `F-06`) against the latest `main` baseline (`8c15569a47cac7f447c49b72172d36ee7d9a041c`), reconcile documentation and actual executed test counts, and prepare branch `fix/phase-2-final-remediation-audit` (PR #6) for update without starting Phase 3.

## User Review & Critical Decisions

> [!IMPORTANT]
> This revised plan incorporates the exact permanent-deletion and UndoToken invalidation requirements for **F-02** alongside **F-01**, **F-03**, **F-04**, **F-05**, and **F-06**.

- **Baseline & PR #6 Synchronisation:** Align with the latest `main` baseline (`8c15569a47cac7f447c49b72172d36ee7d9a041c`) and PR #6 (`fix/phase-2-final-remediation-audit`), preserving all working Phase 2 functionality (five-destination navigation, secondary route stack, full-screen Create/Edit Habit, Habits & Habit Detail, U-12 weekly freeze, Living Grove & reduced motion, and CI workflow) while ensuring `AddEditHabitDialog.kt` is never reintroduced.
- **Confirmed `F-02` Permanent Deletion & Selective `UndoToken` Invalidation Contract:**
  - Replace `deleteHabitPermanentlySync(habitId)` so it directly performs the atomic SQLite deletion, notification cancellation outbox enqueueing/processing, and Journal/draft unlinking (`linked_habit_id = NULL`) **without** calling `deleteHabitWithUndoSync()` and **without** generating a `HABIT_DELETION` `UndoToken`.
  - Before deleting the habit's child rows, inspect the currently active `_activeUndoToken.value`: if that `UndoToken` belongs to the habit being deleted (e.g., an entry, outcome, date note, or rest-day exception belonging to `habitId`, including a previously deleted session whose `previousPayloadJson` has `habit_id == habitId`), invalidate that token (`consumedUndoTokenIds.add(token.tokenId)` and `_activeUndoToken.value = null`). If the active `UndoToken` belongs to a **different** habit, preserve it untouched.
  - Update `DayseedViewModel.deleteHabit(habitId)` to report `"Habit permanently deleted"` without promising Undo, and ensure neither the Create/Edit nor Habit Detail deletion route publishes a snackbar Undo action or promises Undo in its confirmation dialog.
- **Confirmed `F-03` (`U-12 Option B` Governing Version in Record Session):** Implement `resolveRecordSessionGoverningVersion(versions, weekStartHistory, evaluationDate, fallbackVersion)` reusing `StreakAndGroveEngine.resolveEffectiveVersion`, `WeekBoundaryHelper.resolveWeekStartPolicyForDate`, `WeekBoundaryHelper.weekBounds`, and `StreakAndGroveEngine.resolveFrozenWeekVersion`, and drive every label, default, preset, preview, and save route in the Record Session sheet from that governing `HabitVersion`.

---

## 1. Overview & Core Concept

- **What It Does:** Finishes the six targeted Phase 2 remediations (`F-01`–`F-06`), verifies all domain and UI state contracts with executable unit and Robolectric tests, and reconciles project documentation (`implementation_plan.md`, `docs/12-PROGRESS-AND-HANDOVER.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, and `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`) with actual executed test counts.
- **Target Audience / Persona:** Dayseed users managing daily and flexible weekly habits who need a focused Today check-in card, irreversible confirmed habit deletion that preserves linked journal reflections, accurate historical and mid-week session recording, optional preferred weekdays on flexible weekly targets, strict reminder validation, and direct access to all habits in the Living Grove.
- **Key Value:** Closes all remaining Phase 2 gaps identified in the PR #6 audit while strictly preserving the Phase 2 / Phase 3 boundary.

---

## 2. User Experience & Visual Design

- **Key User Flows:**
  1. **`F-01` — Streamlined Today Habit Card & Accurate Tended Today Counter:**
     - Tapping the habit header opens **Habit Detail**.
     - Tapping **Complete** / **Done** or **Quick Add** (`+Small`, `+Half`, `+Goal`, or `Tiny` / `Usual` / `Extra`) records immediately.
     - Tapping **Record** opens the dedicated **Record Session** sheet (`entryId = null`).
     - Removes leftover custom-value state (`customAmountText`, `showCustomInput`), unused lifecycle/edit/delete callbacks, and the clickable existing-entry editing path (`onOpenRecordSession(latestEntry.entryId)`) from the Today card, keeping only a non-interactive compact progress summary (`1 session logged today` / `N sessions logged today`).
     - Computes **Tended Today** over scheduled habits only (`scheduledTodayHabits`), counting each scheduled habit at most once when it has: (1) a saved daily outcome today, (2) a positive numeric entry today (`valueUnits > 0`), (3) a `REST_DAY` exception today (`exceptionKind == HabitDateExceptionKind.REST_DAY`), or (4) an already-satisfied flexible weekly target (`weekTotalUnits >= weekTargetUnits`).
  2. **`F-02` — Single Full-Screen Habit Editor, Standalone Permanent Deletion & Selective `UndoToken` Invalidation:**
     - `CreateEditHabitScreen` is the sole habit creation/editing interface (`AddEditHabitDialog.kt` remains removed).
     - Tapping **Delete Habit** in either `CreateEditHabitScreen` or `HabitDetailScreen` opens an explicit destructive confirmation dialog stating that permanent deletion removes the habit, its logged sessions, and its streak runs, preserves linked Journal reflections safely without the habit link, and **cannot be undone**.
     - Confirming deletion calls `DayseedViewModel.deleteHabit(habitId)` -> `repository.deleteHabitPermanentlySync(habitId)`:
       - Executes the atomic SQLite deletion, notification cancellation outbox enqueueing/processing, and Journal/draft unlinking (`linked_habit_id = NULL`) directly without calling `deleteHabitWithUndoSync()` or generating a `HABIT_DELETION` `UndoToken`.
       - Selectively invalidates `_activeUndoToken` **only** if it belongs to a session/record of the deleted habit (`habitId`), leaving any pending `UndoToken` belonging to another habit intact.
       - Cleans up `CreateEditHabit` and `HabitDetail` routes for the deleted habit from `_secondaryRouteStack` and sets `_userMessage.value = "Habit permanently deleted"`.
  3. **`F-03` — Governing `HabitVersion` Resolution in Record Session (`U-12 Option B`):**
     - `resolveRecordSessionGoverningVersion(versions, weekStartHistory, evaluationDate, fallbackVersion)`:
       1. Resolves the version effective on `evaluationDate` via `StreakAndGroveEngine.resolveEffectiveVersion`.
       2. Checks whether weekly rules apply (`targetPeriod == TargetPeriod.WEEK || scheduleKind == ScheduleKind.FLEXIBLE_WEEKLY_TARGET`).
       3. For a weekly habit, resolves the week-start day via `WeekBoundaryHelper.resolveWeekStartPolicyForDate(evaluationDate, weekStartHistory)`, computes `(weekStart, _)` via `WeekBoundaryHelper.weekBounds(evaluationDate, weekStartDay)`, determines `firstHabitDate`, and resolves the frozen start-of-week version via `StreakAndGroveEngine.resolveFrozenWeekVersion`.
       4. For a daily habit, returns the version effective on `evaluationDate`.
     - Recomputes whenever `effectiveDate`, `versionsForHabit`, or `weekStartHistory` changes, and controls:
       - Header habit name, target summary, and target period (`governingVersion.name`, `governingVersion.formatTargetAndPeriodSummary()`).
       - Tracking mode, measurement mode, unit label (`effectiveUnitCode`), and target value (`effectiveTargetValue`).
       - Default input value (`"1"` for new weekly session-count entries) when creating a new session or when switching dates.
       - Quick preset chips (`+quarter`, `+half`, `target`, `extra`, or completion/effort chips).
       - Daily preview total (`computeRecordSessionDailyPreviewTotal`) and weekly aggregate preview.
       - Save-handler routing between quantity/duration and completion/effort recording (`logHabitQuantityOrDuration` vs `logHabit` using the governing version's tracking mode and unit label).
  4. **`F-04` — Empty Preferred-Weekday Selection on Flexible Weekly Habits:**
     - Users can deselect all preferred weekday chips or tap **Clear** (`selectedDays = emptySet()`).
     - Saving a flexible weekly habit with an empty selection persists `preferredWeekdaysMask = null` and `scheduleDays = emptySet()` (never silently converting an empty selection into all 7 weekdays).
     - Reopening the editor for a flexible weekly habit with `preferredWeekdaysMask == null` initializes `selectedDays` to `emptySet()` so the empty selection is displayed accurately.
     - Preserves the inline explanation distinguishing `days/week`, `sessions/week`, and cumulative weekly quantity/duration units.
  5. **`F-05` — Comprehensive Save-Error Clearing & Strict 24-Hour `HH:mm` Reminder Validation:**
     - Every editable field and control in `CreateEditHabitScreen` (name, description, category text & chips, tracking mode, unit text & preset chips, target value, target period, schedule kind, weekday chips & Clear button, tiny/usual/extra descriptions, lifetime goal, color swatches, reminder toggle, reminder time, and Apply Today / Apply Tomorrow radio options) clears any stale save error via `onClearSaveError()`.
     - When reminders are enabled, validates `reminderTimeLocal` against strict 24-hour `HH:mm` (`00:00`–`23:59`, rejecting `9:5`, `24:00`, `12:60`, etc.), displays an inline error (`habit_reminder_time_error`), and blocks saving until corrected. When reminders are disabled, an irrelevant time string does not block saving.
  6. **`F-06` — Complete Living Grove Navigation from Today:**
     - On `TodayLongestStreakIndicator`, the main card tap and **Grove** button invoke `onOpenLivingGrove(null)` -> `viewModel.openLivingGrove(selectedHabitId = null)` so the Living Grove opens with **all habits visible**.
     - The secondary **Habit** shortcut button invokes `onOpenHabitDetail(selection.habit.habitId)` to open the lead habit's detail screen.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Standalone `deleteHabitPermanentlySync` & Selective `UndoToken` Invalidation (`F-02`)**
  - *Chosen Approach:* `deleteHabitPermanentlySync(habitId): Boolean` executes the atomic SQLite transaction (unlinking `journal_entries` and `journal_drafts`, enqueueing notification cancellations, and cascading `habits` + child tables) directly without calling `deleteHabitWithUndoSync()` or creating an `UndoToken`. It checks whether `_activeUndoToken.value` references `habitId` (via `entityId` lookup in the habit's entries/outcomes/notes/exceptions or `previousPayloadJson`'s `habit_id`), invalidating it if so and preserving it if it belongs to another habit.
  - *Why:* Prevents permanent habit deletion from publishing a snackbar Undo token while ensuring a stale session Undo token for the deleted habit cannot remain active, without disrupting a valid pending Undo token for a different habit.
- **Decision 2: Reuse Existing Domain Helpers for `F-03` (`U-12 Option B`)**
  - *Chosen Approach:* Delegate directly to `StreakAndGroveEngine.resolveEffectiveVersion`, `WeekBoundaryHelper.resolveWeekStartPolicyForDate`, `WeekBoundaryHelper.weekBounds`, and `StreakAndGroveEngine.resolveFrozenWeekVersion`.
  - *Why:* Guarantees zero divergence between the repository's weekly progress evaluation and the Record Session sheet's labels, defaults, presets, previews, and save routing across mid-week edits and historical date selection.
- **Decision 3: Preserve Empty `preferredWeekdaysMask = null` Round-Trip (`F-04`)**
  - *Chosen Approach:* When loading or editing a `FLEXIBLE_WEEKLY_TARGET` habit whose `preferredWeekdaysMask` is `null` (or `0`), `CreateEditHabitScreen` initializes `selectedDays` to `emptySet()` and saves `scheduleDays = emptySet()` / `preferredWeekdaysMask = null`, while `DayseedRepository` continues to treat `preferredWeekdaysMask == null || preferredWeekdaysMask == 0` as matching any day of the week for scheduling.
  - *Why:* Ensures clearing preferred days survives save -> reload -> reopen editor without turning into all 7 weekday chips selected.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         MainActivity (Single-Activity)                     │
│  ┌──────────────────────────────────┐  ┌────────────────────────────────┐  │
│  │ Primary Bottom Navigation (5)    │  │ Secondary Route Stack          │  │
│  │ • TodayScreen                    │  │ • HabitDetailScreen            │  │
│  │   ├─ TodayLongestStreakIndicator │  │   └─ Delete Confirm (No Undo)  │  │
│  │   │  ├─ Card/Grove -> Grove(null)│  │ • CreateEditHabitScreen        │  │
│  │   │  └─ Habit Btn  -> Detail(id) │  │   ├─ Delete Confirm (No Undo)  │  │
│  │   ├─ Tended Counter (outcomes +  │  │   ├─ Empty Preferred Weekdays  │  │
│  │   │  entries>0 + REST_DAY +      │  │   ├─ Weekly Semantics Helper   │  │
│  │   │  satisfied weekly target)    │  │   ├─ Full Field Error Clearing │  │
│  │   └─ HabitCard (Header -> Detail,│  │   └─ Strict HH:mm Validation   │  │
│  │      Quick Add/Done, Record Btn, │  │ • RecordSessionSheet           │  │
│  │      Read-Only Session Count)    │  │   └─ resolveRecordSession      │  │
│  │                                  │  │      GoverningVersion (U-12 B) │  │
│  │                                  │  │ • LivingGroveScreen            │  │
│  └──────────────────────────────────┘  └────────────────────────────────┘  │
└───────────────────────────────────────┬────────────────────────────────────┘
                                        │
                                        ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     DayseedViewModel & DayseedRepository                   │
│  • StreakAndGroveEngine + WeekBoundaryHelper (U-12 Option B frozen week)   │
│  • Standalone deleteHabitPermanentlySync (Unlinks Journals, Cancels        │
│    Notifications, Invalidates Same-Habit UndoToken, Preserves Other-Habit  │
│    UndoToken, Publishes No HABIT_DELETION UndoToken)                       │
└────────────────────────────────────────────────────────────────────────────┘
```

### Verification & Documentation Plan

1. **Automated Regression Tests (`Phase2ImplementationTest`):**
   - **`F-01`:** Verify `tendedToday` counts scheduled habits at most once across outcomes, positive entries, `REST_DAY` exceptions (ignoring unscheduled habits), and satisfied flexible weekly targets; verify `HabitCard` has no inline custom-value or existing-entry edit path.
   - **`F-02`:** Verify `AddEditHabitDialog` is absent; verify `deleteHabitPermanentlySync` and `viewModel.deleteHabit` remove the habit and its child rows, unlink preserved Journal entries (`linkedHabitId == null`), clean up `CreateEditHabit` / `HabitDetail` routes from `secondaryRouteStack`, report permanent deletion without promising Undo, do **not** publish a `HABIT_DELETION` `UndoToken`, **invalidate** an active session `UndoToken` belonging to the deleted habit, and **preserve** an active session `UndoToken` belonging to an unrelated habit.
   - **`F-03`:** Verify `resolveRecordSessionGoverningVersion` and `RecordSessionSheet` across:
     - Daily habit: date before vs. on/after configuration change.
     - Weekly habit: `3 sessions/week` edited mid-week to `50 pages/week` effective tomorrow -> later day in the same week resolves frozen `3 sessions/week` (`unitCode == "sessions"`, `targetValue == 3`, default `"1"`, session-count daily preview); next week resolves `50 pages/week` (`unitCode == "pages"`, `targetValue == 50`, default `"50"`, cumulative unit sum preview); selecting an earlier week resolves that earlier week's governing version.
     - Save routing when governing version tracking mode differs from the latest `Habit` snapshot.
   - **`F-04`:** Verify clearing preferred weekdays on a flexible weekly habit saves `preferredWeekdaysMask = null`, reloads from SQLite with `preferredWeekdaysMask == null` and empty preferred days on editor reopen, and remains scheduled/completable across the week.
   - **`F-05`:** Verify strict 24-hour `HH:mm` reminder validation (`00:00`–`23:59` accepted; `9:5`, `24:00`, `12:60` rejected when enabled; ignored when reminder disabled) and `clearSaveError()` across all editable fields and Apply Today / Apply Tomorrow toggles.
   - **`F-06`:** Verify Today Grove action opens `SecondaryRoute.LivingGrove(selectedHabitId = null)`, Habit shortcut opens `SecondaryRoute.HabitDetail(leadHabitId)`, and `navigateBack()` returns to `Today`.
2. **Build, Test & Documentation Reconciliation:**
   - Execute `gradle :app:testDebugUnitTest --rerun-tasks` and `compile_applet`.
   - Record the **exact executed test counts** in `implementation_plan.md`, `docs/12-PROGRESS-AND-HANDOVER.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, and `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`.
