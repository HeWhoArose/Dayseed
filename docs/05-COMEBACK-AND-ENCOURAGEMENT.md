# Comeback Flow and Encouragement

**Status:** Trigger and suppression policy largely owner-approved; the older broad band table is historical hypothesis only. Final presentation/copy and optional partial-progress encouragement rules remain open. Use D-047–D-056 and D-146–D-160 as the active comeback contract.

## 1. Purpose
Make returning after a break feel easy. The flow should acknowledge that time passed without framing the absence as failure, guessing its cause, or implying that previous progress disappeared.

## 2. Historical candidate inactivity bands (not the active v1 trigger contract)
The table below records an earlier copy/design hypothesis. It must not be implemented as multiple active trigger bands. Later owner-approved rules D-047–D-056 and D-146–D-160 govern actual v1 eligibility:

| Time since last app activity | Candidate tone | Intended next step |
|---|---|---|
| 1–2 days | Gentle nudge | Resume with one small action |
| 3–6 days | Reassurance | Choose one habit or simply look around |
| 7–13 days | Fresh start | Pick a manageable next step |
| 14–29 days | New page | Review current habits and adjust |
| 30–89 days | New chapter | Return without needing to explain |
| 90+ days | Open invitation | Explore at the user's own pace |

Test boundary values and language with users. Avoid making the copy sound like a campaign or assuming the person intentionally abandoned a goal.

## 3. Active v1 trigger and suppression rules
- **Two-week experience:** qualify after 14 complete local calendar days without opening Dayseed; show only on the next app opening, not as a notification while the person is away. Any successful app opening resets the app-inactivity timer.
- **One-month experience:** qualify after 30 consecutive days without opening Dayseed, or after no active habit/goal has remained for 30 days even if the person continues opening the app. Starting/reactivating an active habit resets the no-active-habit timer.
- **Precedence:** if both qualify, show only the broader one-month experience.
- **Frequency:** show each qualifying experience at most once per qualifying episode; acknowledge/dismiss/action suppresses repeats for that episode, not future independent episodes.
- **Presentation differences:** the two-week experience is a short welcome with one action to continue into Dayseed; do not add habit-management shortcuts to that welcome. The one-month experience offers the approved optional resume/start-again, create-new, and continue paths according to available habits, with optional reflection never required.
- Distinguish app inactivity from the activity of any single habit. App openings that only read the journal or view history still reset app-inactivity measurement. The separate no-active-habit trigger is governed by its own approved policy.
- Do not infer a reason for absence, reset history, or present old 1–2/3–6/7–13/90-day bands as active triggers.
- The flow must work offline. If date data is missing/corrupt or clock confidence is uncertain, use a neutral fallback instead of inventing an inactivity duration.
- Dismissal and trigger rules are approved; exact presentation, copy and accessibility review remain open. A global “disable comeback flow” setting is not yet an approved requirement.

## 4. Interaction model

### Two-week experience
Use a short welcome and one primary action to continue into Dayseed. Keep normal habit management available from the ordinary dashboard; do not turn the two-week welcome into a review questionnaire or add shortcut actions for managing habits. The message is dismissible and does not block normal app use.

### One-month experience
Use the approved choice flow and adapt it to available data:
- If an eligible paused/archived or completed habit can be deliberately restarted, offer the appropriate **Resume/Start again** path, with settings review and no automatic reactivation.
- Offer **Create a new habit**.
- Offer **Continue into Dayseed** without changes.
- If no eligible habit exists to resume/restart, omit that option rather than showing an empty list.
- Optional reflection may be written/saved through normal journal flow; it can be skipped. An unsaved comeback reflection remains a protected local draft and is never treated as a saved journal entry automatically.

Do not force a questionnaire about why the user was away. Do not require completing a habit or saving a reflection before continuing.

## 5. Language rules
Use neutral observations such as “It’s been a little while. You can start with one small step, or just take a look around.” Copy must not imply disappointment, debt, broken streaks, or that the user owes the app an explanation.

Avoid:
- “You failed your streak.”
- “You disappeared again.”
- “You need to get back on track.”
- “We knew you would come back.”
- Any claim about stress, depression, laziness, motivation, or life circumstances that the user has not stated.

## 6. Testing requirements
- Test the 14-complete-local-calendar-day app-inactivity boundary (13/14/15 days), the 30-day app-inactivity boundary (29/30/31 days), and the 30-day no-active-habit boundary.
- Test one-month precedence when both triggers qualify, episode-level suppression after acknowledge/dismiss/action, and eligibility in a new independent episode.
- Test timezone travel, manual clock changes, uncertain/missing timestamps, month/year transitions, and app launches that only view Journal/History.
- Test no-active-habit timer reset when a habit is created/reactivated and restart/resume never occurs without explicit user choice.
- Test two-week welcome has one continue action and the one-month choices adapt to available habits; reflection is skippable and drafts remain unsaved.
- Verify no data/history reset or guilt-inducing copy occurs.
- Test entirely offline and review tone, clarity, accessibility and perceived pressure with users when feasible.

## 7. Remaining presentation and policy details
The active trigger source, 14-/30-day thresholds, precedence, and per-episode suppression are approved in D-047–D-056 and D-146–D-160. Do not reopen those as unresolved. Remaining work is final copy/layout, visual hierarchy, accessibility/localisation, and user comprehension review. Any additional global disable/frequency setting must be explicitly approved rather than inferred from the existing dismissal policy.
## 8. Partial-progress encouragement (separate from comeback flow)

**Status:** Approved direction; trigger details and copy remain open.

This is distinct from the Comeback Flow: it responds to a recorded partial result, not app inactivity. For duration or quantity habits, compare the recorded daily total with the target and distinguish Partial, Goal met, and Above goal. Keep the actual amount visible and accurate. Partial is not full daily-target completion, but it is real progress; do not label it failure or silently round it up.

After partial progress, Dayseed may offer a short sequence of optional motivational tips over the following week. The intent is to reinforce that small, repeated steps can help build a habit and offer a practical next step. Tips must be supportive, brief, grounded in the recorded activity, and nonjudgmental. They must not diagnose the user, guess why they did less, imply disappointment, or demand that they make up the difference.

- Do not confuse daily goal met with a flexible weekly target being met; show both clearly when relevant.
- Above-target activity can be recognised without pressuring the user to do more next time.
- The owner prefers both in-app encouragement and optional phone notifications. Keep notifications configurable and permission-respecting; use in-app messages as the fallback when notification permission is denied or reminders are disabled.
- Limit repetition, allow dismissal/opt-out, and do not block core functionality.
- At weekly review, distinguish met days, partial days, above-target days, totals, neutral rest/non-scheduled days, and excluded paused periods.
- Define behaviour when partial activity occurs repeatedly, the weekly target is reached early, an entry is edited, or a target changes midweek.

Offer one optional evening check-in when a scheduled habit remains unrecorded; suppress it after logging/dismissal and respect user settings, quiet hours, and notification limits. Delegated defaults under D-196 now define evidence thresholds, 28-day validation/rollback, and coordination controls in this document and `23-WEEKLY-GOAL-REMINDERS.md`. These routine details may be overridden together in the grouped review; they are not separate Gate A blockers. Actual OS delivery verification remains post-gate.

The reminder cadence, quiet hours, shared cap, collision handling, timing-learning thresholds/rollback, and partial-tip frequency use documented delegated defaults. Remaining planning work is final non-shaming copy and consistency review; the owner may choose to disable phone tips or defer automated tips from v1 during the grouped review.


### Delegated planning defaults for reminders and partial tips

Use the delegated defaults in [Weekly Goal Reminders](23-WEEKLY-GOAL-REMINDERS.md) as the single shared delivery contract unless the owner overrides them in the consolidated review: quiet hours default to 21:00–08:00 local and remain editable; use a shared three-notifications-per-local-day budget; combine eligible events within 15 minutes where a useful single destination exists; re-check eligibility immediately before delivery; and never backfill stale daily reminders after their local day ends.

For system-managed **daily** reminders only, consider learning from local positive activity timestamps after at least seven qualifying records on five or more distinct local dates in 14 days, with at least 70% clustered in a two-hour local-time band. A candidate is the median of that band plus 30 minutes, rounded up to a 15-minute boundary. Never auto-change a manually selected time. Hold a candidate through a 28-day validation window and revert to the prior system-managed time if the observations no longer support a stable pattern. Do not analyse journal text or mood, use network services, or apply this daily learning rule to flexible weekly-target timing.

Proposed partial-tip default: up to three optional tips in the seven days after partial progress, at least 24 hours apart, no more than one per local day, and suppressed if the relevant target is met or the user dismisses/opts out. In-app tips may remain available when OS notification permission is denied; phone tips require a separate enabling setting. These are delegated planning defaults under D-196, owner-overridable before Gate A; they are not final copy or evidence of working scheduling.

## 9. Personalised reminders and outcome-aware messages

**Status:** Approved direction; delegated timing/coordination defaults are documented under D-196. Final non-shaming copy and cross-document consistency remain planning work; Android scheduling feasibility requires separate post-gate technical validation.

Dayseed should explore a gentle reminder system that can learn from the user's own completion patterns over time. For example, if a habit is often completed around 3 p.m. and remains unrecorded by a later chosen/check-in time, Dayseed may offer a reminder around 4 p.m. This must be treated as a pattern-based suggestion, not a certainty or a judgement. Sparse data should not create false confidence.

The intended experience has complementary surfaces:
- **In-app:** when the user opens Dayseed, show a context-matched, brief message based on the recorded state for the relevant habit/day.
- **Optional notification:** send a timely reminder or encouragement when allowed by Android permissions and user settings.
- **Earlier reminder:** if a habit remains unrecorded around a time the user commonly completes it, a gentle prompt may help.
- **Later-day check-in:** near the end of the user's configured day or chosen reminder window, optionally surface an unrecorded scheduled habit or acknowledge what was recorded.
- **Outcome-aware acknowledgement:** partial progress receives encouragement; meeting the target receives a proportionate congratulations; above-target activity receives a restrained acknowledgement; a missed scheduled opportunity gets a supportive invitation without shame. Rest/non-scheduled days and paused habits should not be treated as missed.

Planning safeguards:
- No repeated "you missed a day" notifications or guilt-inducing wording.
- User controls for enabling reminders, per-habit participation, preferred windows/quiet hours, frequency, and disabling notifications.
- Respect Android notification permission; in-app feedback remains available when permission is denied.
- Rate limits, deduplication, and suppression when the user has already logged the activity or dismissed the prompt.
- Learning uses only the minimum local activity metadata needed; do not infer sensitive reasons or analyse journal text to predict routines.
- A learned pattern is not a fixed schedule and must be explainable/editable by the user. Provide a simple manual schedule as an alternative.
- Keep this reminder system distinct from the inactivity-based Comeback Flow and the one-week partial-progress encouragement sequence, while coordinating them to avoid message overload.


### Reminder opt-in during onboarding (owner-approved direction)
- Ask once during first-run onboarding whether the user wants reminders.
- If the user chooses **Yes**, enable reminders. If the user chooses **No**, create no reminders. Each habit form displays a reminder-time control defaulted to 9:00 a.m. local time; the habit uses that time unless the user changes it.
- Do not repeat the global yes/no question for each newly created habit.
- Habit creation/editing includes a small reminder/timer control. The current default is used unless the user explicitly selects a different time for that habit.
- A saved custom reminder takes precedence and must not be overwritten by defaults. Users can review, change, or disable reminders in Settings.
- For habits left on system-managed timing, Dayseed may automatically adjust after 14 days of sufficient observations, then observe the new time for a further 28 days to validate/solidify the adjustment. If the routine changes again, Dayseed may automatically adjust again while system-managed timing remains enabled. If the user manually edits a time, routine changes can prompt suggestions only; never automatically change a manually selected time unless the user explicitly returns the habit to automatic timing. Android notification permission is separate and declining it must not block app use.

Delegated planning defaults now cover per-habit manual-time protection, local-only learning thresholds/rollback, quiet hours and shared cap. The owner may override these defaults before Gate A; routine details are not separate approval blockers. Still-open planning detail is limited to checking consistency of the timing-learning contract and final non-shaming copy. Actual OS delivery and restart/timezone behaviour remain unverified until separately authorised post-gate validation. Preserve the 14-day observation and 28-day validation direction, and never learn from journal text/mood or network data.
