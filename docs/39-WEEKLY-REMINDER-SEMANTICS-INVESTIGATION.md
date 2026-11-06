# docs/39 — Scheduled-Day and Weekly Reminder Semantics Investigation Report

**Date:** 2026-11-05  
**Author:** Dayseed remediation agent (per mission §4)  
**Scope:** Investigate whether reminder eligibility should be gated by scheduled days, weekly target achievement, and flexible weekly-target semantics. Produce a decision report; do NOT change behaviour.

---

## 1. Question Summary

Four interrelated product questions from the mission §4:

1. **Should a daily habit reminder be eligible only on scheduled days?**
2. **Once a weekly target has been achieved, should remaining reminders for that week be suppressed?**
3. **Should reminders occur once per scheduled opportunity or at another frequency?**
4. **How should flexible weekly-target habits behave? How should REST_DAY and effort-aware TINY activity affect the decision?**

---

## 2. Code Evidence

### 2.1 `StreakAndGroveEngine.isScheduledOpportunityOnDate` (canonical schedule domain)

```kotlin
// C:\Dev\Dayseed\app\src\main\java\com\example\dayseed\data\StreakAndGroveEngine.kt:200–212
fun isScheduledOpportunityOnDate(version: HabitVersion, date: LocalDate): Boolean {
    return when (version.scheduleKind) {
        ScheduleKind.DAILY -> true
        ScheduleKind.SELECTED_WEEKDAYS -> {
            val mask = version.scheduledWeekdaysMask ?: WeekdayMask.ALL_DAYS_MASK
            WeekdayMask.contains(mask, date.dayOfWeek)
        }
        ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
            val mask = version.preferredWeekdaysMask
            if (mask != null && mask > 0) WeekdayMask.contains(mask, date.dayOfWeek) else true
        }
    }
}
```

This function **is the authoritative schedule domain** for streak/grove evaluation and is used in:
- `evaluateHabitStreakRuns` (weekly cadence branches)
- `resolveEffectiveVersion` and habit creation validation
- `DayseedRepository.saveHabitSync` (apply-today gate at line 1089)

### 2.2 Reminder Engine (current daily reminder eligibility)

```kotlin
// C:\Dev\Dayseed\app\src\main\java\com\example\dayseed\data\ReminderEngine.kt:153–169
val (status, reason) = when {
    habitVersion.lifecycleState != HabitLifecycleState.ACTIVE || habitVersion.reminderMode == "OFF" ->
        "SUPPRESSED" to "REMINDER_OFF_OR_INACTIVE"
    alreadyLoggedToday ->
        "SUPPRESSED" to "ALREADY_LOGGED"
    isWithinQuietHours(scheduledLocal, quietStartLocal, quietEndLocal) ->
        "SUPPRESSED" to "QUIET_HOURS"
    consumedCountToday >= dailyCap ->
        "SUPPRESSED" to "DAILY_CAP_REACHED"
    else ->
        "SCHEDULED" to null
}
```

**Key finding:** The current `evaluateHabitReminder` does **not** call `isScheduledOpportunityOnDate`. A DAILY, SELECTED_WEEKDAYS, or FLEXIBLE_WEEKLY_TARGET habit with `reminderMode != "OFF"` will be evaluated as a deliverable candidate **every calendar day** (subject only to quiet hours, already-logged, and the 3/day cap).

### 2.3 Weekly-goal reminders (documented, not implemented)

- **Settings panel** (SettingsPanels.kt:690–708) explicitly states: "Weekly reflection reminder preferences are not implemented yet, so there is no switch here."
- **23-WEEKLY-GOAL-REMINDERS.md** defines the approved *direction* for weekly goal reminders (week-start reflection, goal-reached congratulation, inactivity check-ins) but labels them "delegated planning defaults under D-196" and "post-gate technical-validation work."
- **25-DECISION-REGISTER-AND-RECONCILIATION.md:57** records: "Weekly reminders — Trigger/count rules approved; shared coordination defaults delegated; weekly reflection clock time open"
- **D-043/D-044/D-046** (09-DECISION-LOG.md:52–53) are "Approved direction" only: "Owner-approved only to the scope stated in the log; not implementation permission."

### 2.4 Settings panel contract (26-SCREEN-AND-DATA-CONTRACTS.md:314–320)

- "Scheduling per D-222: ReminderScheduler registers inexact AlarmManager.setWindow triggers…"
- "Delivery pipeline in ReminderAlarmReceiver: global opt-in → Android permission → ReminderEngine.evaluateHabitReminder → ledger → NotificationManager"
- No mention of scheduled-day filtering for habit reminders.

### 2.5 Habit model schedule kinds (Models.kt:460–472)

```kotlin
fun isScheduledFor(dayOfWeek: DayOfWeek): Boolean = when (scheduleKind) {
    ScheduleKind.DAILY -> true
    ScheduleKind.SELECTED_WEEKDAYS -> WeekdayMask.contains(mask, dayOfWeek)
    ScheduleKind.FLEXIBLE_WEEKLY_TARGET -> {
        val mask = preferredWeekdaysMask
        if (mask != null && mask > 0) WeekdayMask.contains(mask, dayOfWeek) else true
    }
}
```

This is a **UI/convenience** method on the `Habit` presentation model, not used by the reminder engine.

---

## 3. Documented Requirements vs. Current Implementation

| Question | Documented Answer | Current Code Behaviour |
|----------|-------------------|------------------------|
| Daily reminder only on scheduled days? | `isScheduledOpportunityOnDate` exists and is used for streak/grove; weekly reminder doc §7.2 says "effective schedule" is re-checked at delivery time (26-SCREEN-AND-DATA-CONTRACTS.md:319) but does not explicitly require scheduled-day gate for *habit* reminders. | **No gate.** Every calendar day is evaluated if habit is ACTIVE and reminder is ON. |
| Weekly target reached → suppress remaining reminders? | 23-WEEKLY-GOAL-REMINDERS.md:9: "When a weekly goal reaches its target, immediately send one small congratulations… Stop all inactivity reminders for that goal for the remainder of the week." But this applies to *weekly-goal* reminders (congratulation, inactivity), not per-habit daily reminders. | N/A — weekly-goal reminders not implemented. Per-habit daily reminders continue regardless of weekly target. |
| Once per scheduled opportunity? | 26-SCREEN-AND-DATA-CONTRACTS.md:172: "never schedule more than one ordinary reminder per habit/date." This is per-date dedupe, not per-scheduled-day. | Enforced by `delivery_key` primary key (HABIT_DAILY:habitId:date) → at most one per calendar date. |
| FLEXIBLE_WEEKLY_TARGET behaviour? | 23-WEEKLY-GOAL-REMINDERS.md doesn't specify habit-reminder behaviour for flexible weekly targets; only weekly-goal reflection reminders. | Evaluated daily (no scheduled-day gate); preferredWeekdaysMask is ignored by reminder engine. |
| REST_DAY / effort-aware TINY? | 05-COMEBACK-AND-ENCOURAGEMENT.md distinguishes app inactivity from lack of recorded habit progress (line 12). No document binds REST_DAY or EFFORT_LEVELS to reminder suppression. | Not consulted by reminder engine. |

---

## 4. Authoritative Citations

| Document | Section | Authority |
|----------|---------|-----------|
| `26-SCREEN-AND-DATA-CONTRACTS.md` | §7.2, §2.4 (Reminders) | **Stage B contracts** — owner-accepted via D-221/D-222 (2026-11-04) |
| `23-WEEKLY-GOAL-REMINDERS.md` | All | **Approved direction**; explicit: "post-gate technical-validation work", "not proof of Android delivery behaviour" |
| `09-DECISION-LOG.md` | D-041/D-043/D-044/D-046 | **Approved direction only** — "Owner-approved only to the scope stated in the log; not implementation permission" |
| `25-DECISION-REGISTER-AND-RECONCILIATION.md` | Line 57, 184–187 | Registers decisions as **direction-only**, not implementation authorisation |
| `StreakAndGroveEngine.kt` | `isScheduledOpportunityOnDate` | **Canonical schedule domain** for streak/grove — used by repository on save, not by reminder engine |

---

## 5. Conclusion — No Unambiguous Answer in Existing Documents

The existing documentation **does not provide an unambiguous answer** that would authorise a silent product behaviour change for any of the four questions:

1. **Scheduled-day gate for daily habit reminders:** The canonical function exists but is not used by the reminder pipeline. The contracts mention "effective schedule" re-check at delivery time (26-SCREEN-AND-DATA-CONTRACTS.md:319) but only in the context of the shared cap/collision rules and the *weekly reflection* reminder, not per-habit daily reminders.

2. **Weekly-target suppression:** Documented only for weekly-goal *reflection* reminders (congratulation/inactivity), which are not implemented. No document extends this to per-habit daily reminders.

3. **Once per scheduled opportunity vs. once per calendar day:** The contract explicitly caps at "one ordinary reminder per habit/date" (26-SCREEN-AND-DATA-CONTRACTS.md:172) — a calendar-day key, not a scheduled-day key.

4. **Flexible weekly target / REST_DAY / effort-aware:** No document binds these to reminder eligibility.

---

## 6. Decision Required

**Owner decision needed (batch via Accept / Modify / Defer):**

| ID | Question | Recommended Default (D-196) | Requires Owner Input |
|----|----------|------------------------------|---------------------|
| R-WR-01 | Gate daily habit reminders by `isScheduledOpportunityOnDate`? | **Yes** — aligns with streak domain and user mental model; prevents reminders on off-days for SELECTED_WEEKDAYS/FLEXIBLE_WEEKLY_TARGET. | **Yes** — scope/philosophy |
| R-WR-02 | Suppress daily habit reminders after weekly target met? | **No** — weekly-goal reflection reminders are separate; daily habit reminders serve streak continuity. | **Yes** — product philosophy |
| R-WR-03 | Frequency: once per scheduled day (not calendar day)? | **Yes** — already enforced by delivery_key per calendar date; scheduled-day gate would naturally limit frequency. | Implied by R-WR-01 |
| R-WR-04 | FLEXIBLE_WEEKLY_TARGET: use `preferredWeekdaysMask` or fall back to daily? | **Use `preferredWeekdaysMask` if set, else daily** — mirrors `isScheduledOpportunityOnDate` behaviour. | **Yes** — design decision |
| R-WR-05 | REST_DAY / effort-aware TINY → suppress reminders? | **No** — these are logging affordances, not schedule modifiers. | **Yes** if owner wants otherwise |

---

## 7. Recommendation

**Defer implementation changes.** The approved remediation phases (1–5) may proceed independently. The scheduled-day/weekly semantics questions should be batched for owner decision under D-196 before any behaviour change. Current behaviour (daily evaluation for all active reminder-enabled habits) is safe and documented as the pre-verification state.

---

## 8. Traceability Links

- `docs/28-REQUIREMENT-TRACEABILITY.md` — no requirement currently maps to scheduled-day gating for habit reminders
- `docs/12-PROGRESS-AND-HANDOVER.md` — this report to be recorded as investigation complete, decision deferred
- `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` — register R-WR-01 through R-WR-05 as pending owner batch