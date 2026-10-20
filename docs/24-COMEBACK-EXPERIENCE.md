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

A separate, broader welcome-back experience is approved when either condition applies: (1) the user has not opened Dayseed for 30 consecutive days, or (2) the user has had no active habits or goals for 30 consecutive days, even if they continue opening the app. For the no-active-goal condition, start the timer when the last active goal becomes completed, paused, archived, or deleted, provided no other active goal remains. Creating or reactivating any goal resets the no-active-goal timer. It should invite the user to reflect on what is new and optionally resume an existing habit or create a new one. If both this trigger and the two-week comeback qualify, show only the broader one-month experience rather than stacking both. Show each qualifying experience at most once per inactivity episode; after acknowledgement, dismissal, or an action choice, suppress it until a new inactivity period qualifies. Use a gentle choice screen. Welcome the user, ask what's new, and offer three clear actions: (1) resume an existing habit, (2) create a new habit, or (3) continue into the app without making changes. For resume, let the user choose an eligible paused or archived habit, review its settings, and explicitly reactivate it. If no eligible paused or archived habits exist, omit the resume option entirely and show only “Create a new habit” and “Continue into Dayseed.” Never show an empty or broken resume option or pressure the user to create a habit. Completed habits remain in history unless the user deliberately chooses to start one again. Preserve historical records; never reactivate or alter a habit automatically. Do not force a journal entry or demand an explanation for the absence. The user remains in control.

Example direction: “Welcome back! 🌿 What's new with you? Ready to pick up an old habit or plant a new one?”

## Still to specify

- Exact boundary calculation for 14 consecutive days, including clock/time-zone changes and first-launch tracking.
- Exact return-screen handling for active daily and weekly goals; the approved invariant is that app inactivity alone never changes their status, schedules, progress, or history and never automatically marks days missed or resets progress.
- Exact date-boundary and timezone handling for the 30-day no-active-goal period, including how goal status changes are timestamped. The trigger and reset rules are approved: start when the last active goal becomes completed, paused, archived, or deleted with no other active goal remaining; creating/reactivating a goal resets the timer. Repeat suppression is approved: each experience appears once per qualifying inactivity episode, and the one-month experience takes precedence when both triggers qualify.
- Final copy and interaction details for the choice screen, including how deliberately restarting a completed habit is presented; accessibility, localization, privacy-conscious wording, and acceptance tests. The no-eligible-habit state is approved: omit resume and show create-new and continue options only.
