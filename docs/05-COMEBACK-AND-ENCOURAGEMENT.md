# Comeback Flow and Encouragement

**Status:** Proposed experience rules; exact thresholds and copy require testing.

## 1. Purpose
Make returning after a break feel easy. The flow should acknowledge that time passed without framing the absence as failure, guessing its cause, or implying that previous progress disappeared.

## 2. Candidate inactivity bands
These bands are initial copy/design hypotheses, not approved behavioural rules:

| Time since last app activity | Candidate tone | Intended next step |
|---|---|---|
| 1–2 days | Gentle nudge | Resume with one small action |
| 3–6 days | Reassurance | Choose one habit or simply look around |
| 7–13 days | Fresh start | Pick a manageable next step |
| 14–29 days | New page | Review current habits and adjust |
| 30–89 days | New chapter | Return without needing to explain |
| 90+ days | Open invitation | Explore at the user's own pace |

Test boundary values and language with users. Avoid making the copy sound like a campaign or assuming the person intentionally abandoned a goal.

## 3. Triggering rules
- Track app inactivity separately from each habit's activity.
- Do not infer a reason for absence.
- Do not reset or hide history.
- Do not repeatedly show the same prompt after dismissal.
- A person may use the journal while not completing habits; do not treat that as total disengagement.
- The flow must work offline.
- If there is insufficient or corrupted date data, use a neutral fallback rather than inventing an inactivity duration.
- Respect explicit user preference to dismiss or disable encouragement prompts.

## 4. Interaction model
Offer a short message and optional actions:
1. **Choose one small step** — go to today's habit list.
2. **Review my habits** — edit, pause, or archive goals that no longer fit.
3. **Just look around** — dismiss the prompt and continue using the app.

Do not force a questionnaire about why the user was away. Do not require completing a habit to dismiss the flow.

## 5. Language rules
Use neutral observations such as “It’s been a little while. You can start with one small step, or just take a look around.” Copy must not imply disappointment, debt, broken streaks, or that the user owes the app an explanation.

Avoid:
- “You failed your streak.”
- “You disappeared again.”
- “You need to get back on track.”
- “We knew you would come back.”
- Any claim about stress, depression, laziness, motivation, or life circumstances that the user has not stated.

## 6. Testing requirements
- Unit-test each band at one day below, exactly on, and one day above each boundary.
- Test timezone changes, daylight-saving changes where applicable, device clock changes, and missing timestamps.
- Test app inactivity while journal use continues.
- Test individual habit inactivity while other habits remain active.
- Test dismissal persistence and disabled prompts.
- Verify no data or streak reset occurs.
- Have users assess tone, clarity, and perceived pressure.
- Test entirely offline.

## 7. Open decisions
Approve the inactivity source (last app open, meaningful action, or separate signals); exact threshold boundaries; whether first return after installation gets a welcome rather than comeback message; dismissal persistence; and how users control encouragement frequency.
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

The exact one-week schedule, thresholds, number of tips, copy, and controls remain open and need review before implementation.


## 9. Personalised reminders and outcome-aware messages

**Status:** Approved direction; exact policy and feasibility remain planning work.

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

Open: minimum observations and confidence before personalisation; initial default reminders; exact reminder windows; quiet hours and end-of-day definition; notification cadence; copy variants; and whether pattern learning remains entirely on-device. These require product, privacy, UX, and technical review before implementation.
