# Weekly Goal Reminders

**Status:** Approved product direction; detailed timing/copy tests remain to be specified.

## Weekly rhythm

- At 9:00 a.m. local time on the first day of each user-configured week, review the previous week's final result before resetting current-week progress, then send the outcome-aware fresh-start message. Use the device's current local time zone. If the scheduled time is missed because the device was unavailable, deliver at the next suitable opportunity without duplicating the message.
- Send a fresh-start message informed by that result: celebrate a completed goal, acknowledge partial progress without shame, or offer a gentle invitation to begin again if no progress was recorded.
- When a weekly goal reaches its target, immediately send one small congratulations, regardless of which day it happens. Stop all inactivity reminders for that goal for the remainder of the week.
- At the end of the week, close out the result for the next week's reflection. Do not send a redundant second congratulations solely because the target was already reached; the next week-start message provides the fuller reflection and fresh start.
- If a weekly goal is below target and has had no recorded progress for two full days, send a gentle inactivity check-in. If progress occurs before the reminder is due, cancel/reset the timer. If no progress occurs, another check-in may be sent after each further two full days, up to three inactivity reminders per configured week.
- Stop inactivity reminders when the target is met or the configured week ends. Reset weekly reminder counts at the next configured week boundary.
- The user's chosen first day of week determines all weekly boundaries. At week transition, capture/finalise the previous result before beginning the new week's progress.
- When the weekly fresh-start message and daily habit reminders are due together, combine them into one notification where practical. Summarise both clearly and make the tap destination useful for the combined content (for example, a relevant Today/dashboard view). If a meaningful combined notification is not practical, stagger or separate the reminders while respecting quiet hours and shared notification limits. Never silently drop a reminder's essential information.
- Respect the global onboarding reminder opt-in, Android notification permission, quiet hours, and notification limits. Coordinate with daily-habit reminders to avoid overlapping or excessive notifications.
- Never imply an inactive day is a failure, infer the reason for inactivity, or pressure users who exceeded a target to repeat or beat that performance.

## Example copy

**Goal reached:** “Weekly goal complete! Well done. Take a moment to appreciate your progress.”

**New week after reaching the target:** “You did it last week! A new week, a new milestone. Let's keep making progress, one step at a time.”

**New week after partial progress:** “You made some progress last week, and that counts. This is a fresh start — let's take one small step at a time.”

**New week after no recorded progress:** “Last week didn't go quite as planned, and that's okay. This is a fresh start, not a reset of your progress.”

## Still to specify

- Exact week-start scheduling edge cases, including missed delivery while the device is unavailable and local-time-zone changes. The approved week-start delivery time is 9:00 a.m. local time on the user's configured first day of the week.
- Exact notification-combination rules, tap destination when combined content covers multiple screens, and Android scheduling constraints.
- Copy variants, duplicate prevention, clock/timezone changes, and edge-case acceptance tests.
