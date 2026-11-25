# Motivational copy audit (2026-11-25)

Scope: every place the app speaks encouragement to the user. Source inspection only; no behaviour changed by this audit. Related: D-216 (band quotes), D-249, D-250.

## Inventory and what each does

| Where | Copy | Trigger / behaviour |
|---|---|---|
| Record session summary (`RecordSessionSheet`) | Band `encouragementQuote` (8 bands: Zero, Tiny, Small, Halfway, Nearly there, Goal met, Extra, Huge; `data/Models.kt` ProgressBand) | Shown under the day's total; chosen purely from today's percentage of target. Static text, no randomness, no stored state. |
| Band `motivationQuote` (same enum) | Second line per band | Defined, but since D-249 removed the Habit Detail progress card it is **no longer shown on any screen** (dead copy). |
| Today, struggling-habit banner (`TodayScreen` ~435) | "Tough week for <habit>? Reflect or adjust anytime." | Appears for a habit with a rough recent run; offers Reflect/Adjust. |
| Comeback card (`DayseedRepository` ~4554) | "Welcome back — your past progress is still here." plus tiered messages by days away (14-day tier shown) with Review & Resume / Continue | Shown once per absence episode (`episodeKey`); no streak-shaming, states nothing was reset. |
| Journal prompts (`Models.kt` ~1328) | 7 daily prompts rotating by date, plus event prompts (tiny version, future self, retrospective, comeback) | Suggestions only; never required. |
| Weekly reminder engine / notifications (`WeeklyReminderEngine`, `ReminderNotifier`) | Neutral reminder wording | Reminders default on (D-248); no guilt or streak-loss language found. |
| Over-target progress (`CompactHabitRow`) | Neutral status text | No praise copy. |

## Findings

1. **Tone is consistent and non-punitive.** No copy threatens loss of a streak, uses "failed", or compares the user to others. This matches the product philosophy.
2. **Dead copy:** the eight `motivationQuote` strings are unused. Recommendation: delete them (or reuse one as a rotating second line). Low risk; owner call because D-216 approved the wording.
3. **Only one place shows a band quote** (Record session). Today rows show progress without words; that is a deliberate calm choice, not a gap.
4. **Static, not adaptive:** the same quote appears every time the band is reached, so it may feel repetitive. Optional: two variants per band chosen by date.
5. **"Huge" band** copy ("celebrate the abundance") is the one line that could read as pressure for people tracking health targets; its second line already counters this. Consider "You went well beyond today's target."
6. **Honest-zero** ("Checking in honestly is still showing up") is a good, deliberate line; keep.

## Not changed / open for owner
Remove dead `motivationQuote` strings; add variants; soften "Huge". Nothing here is implemented.
