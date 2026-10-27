# Weekly Goal Reminders

**Status:** Approved product direction; detailed timing/copy tests remain to be specified.

## Weekly rhythm

- At 9:00 a.m. local time on the first day of each user-configured week, review the previous week's final result before resetting current-week progress, then send the outcome-aware fresh-start message. Use the device's current local time zone. If the scheduled message is missed, deliver it at the next suitable notification opportunity, even if a day or more late, but only once. If multiple weekly reflections were missed across consecutive weeks, send only one reflection for the most recently completed week; do not send a backlog or combine several old weekly reflections. Persist/deduplicate delivery so retries, app restarts, and rescheduling cannot send duplicates.
- Send a fresh-start message informed by that result: celebrate a completed goal, acknowledge partial progress without shame, or offer a gentle invitation to begin again if no progress was recorded.
- When a weekly goal reaches its target, immediately send one small congratulations, regardless of which day it happens. Stop all inactivity reminders for that goal for the remainder of the week.
- At the end of the week, close out the result for the next week's reflection. Do not send a redundant second congratulations solely because the target was already reached; the next week-start message provides the fuller reflection and fresh start.
- If a weekly goal is below target and has had no recorded progress for two full days, send a gentle inactivity check-in. If progress occurs before the reminder is due, cancel/reset the timer. If no progress occurs, another check-in may be sent after each further two full days, up to three inactivity reminders per configured week.
- Stop inactivity reminders when the target is met or the configured week ends. Reset weekly reminder counts at the next configured week boundary.
- The user's chosen first day of week determines all weekly boundaries. At week transition, capture/finalise the previous result before beginning the new week's progress.
- When the weekly fresh-start message and daily habit reminders are due together, combine them into one notification where practical. Summarise both clearly and make the tap destination useful for the combined content (for example, a relevant Today/dashboard view). If a meaningful combined notification is not practical, stagger or separate the reminders while respecting quiet hours and shared notification limits. Never silently drop a reminder's essential information.
- Respect the global onboarding reminder opt-in, Android notification permission, quiet hours, and notification limits. Coordinate with daily-habit reminders to avoid overlapping or excessive notifications.
- Never imply an inactive day is a failure, infer the reason for inactivity, or pressure users who exceeded a target to repeat or beat that performance.

## Shared notification delivery defaults — planning proposal

The following are **delegated planning defaults for owner review**, not proof of Android delivery behaviour and not a new owner-approved decision. They coordinate weekly reminders with daily habit reminders and optional encouragement without overriding the already-approved weekly trigger rules.

- **Quiet hours:** default 9:00 p.m.–8:00 a.m. in the current device-local time zone. Expose the window in Settings and allow the user to change or disable it. Do not post during quiet hours. At the next permitted time, re-check whether the message is still relevant; do not deliver stale daily reminders for a day that has already ended.
- **Shared cap:** aim for no more than three Dayseed OS notifications per local calendar day across all reminder types. Never schedule more than one ordinary reminder for the same habit on the same local day. If many habits are due together, combine them into one useful summary rather than generating a burst.
- **Collision handling:** for eligible notifications due within 15 minutes of one another, combine them when they share a useful destination and the summary can name the relevant habits/goals. Use Today as the default destination for a combined habit summary. If combining would obscure essential details, stagger to a permitted slot at least 15 minutes apart and respect the shared daily cap. Preserve the underlying reminder state so the app can show the complete context in-app.
- **Priority when the cap is reached:** preserve an explicitly configured per-habit reminder first, then a weekly-goal-reached acknowledgement, the week-start reflection, inactivity check-ins, and optional partial-progress tips. Combine eligible high-priority items when possible. Defer a weekly reflection to the next suitable permitted opportunity, but do not queue old daily reminders or generic tips for later days.
- **Delivery must re-check state:** immediately before posting, verify global opt-in, Android permission, quiet hours, the effective schedule, the current local day/week, whether progress was recorded, dismissal/suppression state, whether the weekly target was met, and whether the habit still exists and is active. A late or retried worker must not bypass these checks.
- **Restart and time-zone changes:** persist a stable key for each logical occurrence. On boot/restart, reschedule future reminders and re-evaluate overdue occurrences; do not duplicate a delivered occurrence. Recompute future local delivery times in the current device time zone without changing saved historical date keys. If device time is uncertain, prefer suppressing a questionable stale reminder to fabricating elapsed time.
- **Personalised timing:** learn only on-device from qualifying habit-record timestamps; do not inspect journal text, mood, contacts, location, or send observations over the network. Keep flexible weekly-target reminders on their separately configured weekly rhythm. For a system-managed daily habit reminder, consider a time adjustment only after at least seven qualifying activity timestamps across at least five distinct local dates in a 14-day window, with at least 70% falling within one two-hour local-time band. Use the median time in that band plus 30 minutes, rounded up to a 15-minute boundary, as a check-in candidate. Never override a manually selected time. After a change, hold it through a 28-day validation window; retain it only if sufficient observations still support a stable cluster, otherwise revert to the prior system-managed time and begin a fresh observation window. Sparse or contradictory data means no adjustment.
- **Partial-progress tips:** keep the sequence optional and distinct from comeback messaging. Proposed default: at most three tips over the seven days following a qualifying partial result, never more than one per local day and at least 24 hours apart; suppress a tip if the relevant habit/goal has since been met or the user dismisses/opts out. In-app tips may appear on open; phone tips require the reminder preference and a tips-notification setting to permit them. If this cadence is not wanted in v1, explicitly defer the automated tips rather than shipping an unspecified sequence.

These defaults still require an owner walkthrough or explicit deferral before the planning gate is approved. Actual WorkManager/alarm behaviour, permission handling and device-restart delivery are post-gate technical-validation work.

## Example copy

**Goal reached:** “Weekly goal complete! Well done. Take a moment to appreciate your progress.”

**New week after reaching the target:** “You did it last week! A new week, a new milestone. Let's keep making progress, one step at a time.”

**New week after partial progress:** “You made some progress last week, and that counts. This is a fresh start — let's take one small step at a time.”

**New week after no recorded progress:** “Last week didn't go quite as planned, and that's okay. This is a fresh start, not a reset of your progress.”

## Still to specify

- Week-start scheduling edge cases remain to be tested, especially local-time-zone changes and clock changes. The approved delivery time is 9:00 a.m. local time on the user's configured first day of the week. Missed messages should be delivered at the next suitable opportunity, with only the most recently completed week's reflection delivered after a multi-week gap. The time itself is not an open decision.
- Owner review or explicit deferral of the proposed shared notification defaults above, especially priority/cap behaviour and whether partial-tip notifications belong in v1.
- Final copy variants and the exact destination when combined content covers multiple screens.
- Android scheduling, duplicate prevention, restart/clock/time-zone edge cases and acceptance tests; real OS behaviour is post-gate validation.
