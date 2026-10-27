# Growth Mark and Comeback Return Research

**Research date:** 2026-10-17  
**Status:** Design hypothesis and copy examples; not approved for implementation.  

**Current-status note (2026-10-27):** The visual Growth Mark measure/representation and the final comeback presentation/copy remain open design decisions. However, this older research predates approved trigger rules: D-047–D-056 and D-146–D-160 govern the v1 comeback experience, including the 14-complete-local-calendar-day app-inactivity trigger, 30-day broader trigger (including no active habits), 30-day precedence, and suppression for each qualifying episode. The 3-day and 90-day examples below are copy exploration only, not approved v1 triggers. Use the [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) and [Requirement Traceability Matrix](28-REQUIREMENT-TRACEABILITY.md) for current planning status.
  
**Purpose:** Define what the Growth Mark communicates and how Dayseed welcomes people back without turning absence into a penalty.

## 1. Non-negotiable principle

The Growth Mark must visualise evidence of the user's own activity, not assign an arbitrary level, score, rank, or moral value. It must never imply that a person's worth, character, health, or overall progress can be measured by an app.

It is a summary, not a reward economy. No points, XP, loot, punitive decay, broken-plant animation, or visual deterioration for absence in v1.

## 2. Candidate meanings

| Concept | What it measures | Display idea | Strength | Risk |
|---|---|---|---|---|
| **Collected moments** (preferred test candidate) | Number of dates with at least one valid user-recorded habit action in a selected period | A small branch/field of marks; each mark maps to a real recorded date | Concrete and explainable | Could be mistaken for daily compliance unless label and gaps are clear |
| **Effort rings** | Distribution of Tiny, Usual, and Extra actions, counted only where the habit supports effort levels | Three nested or segmented arcs with a text legend | Makes small efforts visible | Nested circles may imply Tiny is inferior or Extra is always better |
| **Growing timeline** | Actual recorded activity over time, including pauses and rest days | A line of dated dots or short stems | Shows continuity without pretending it is unbroken | A timeline can look like a streak chart unless pauses are neutral |
| **Personal garden** | No separate score; visual metaphor reflects collected records and user-chosen milestones | One or more leaves appear when a defined, explainable threshold is reached | Distinctive and emotionally warm | Risks becoming arbitrary gamification; defer decorative growth until each change has a clear rule |
| **Progress summary without emblem** | Count of valid actions, active dates, or completion rate against scheduled opportunities | Editorial card with a plain-language sentence and a small chart | Most transparent | Less ownable as a brand motif |

### Recommendation to test

Test **Collected moments** against a plain editorial summary, not against an elaborate gamified tree. A growth illustration is permitted only if every visual change can be explained in one sentence and maps to a real, auditable event.

The initial Growth Mark should not combine unrelated measures into one composite score. Show one measure at a time and label its period.

## 3. Metric definitions and guardrails

Candidate definitions for usability testing; not approved product semantics:

- **Recorded effort:** number of habit actions the user explicitly logged in a period. If a single habit can be completed multiple times per day, count records according to the final data rules and label accordingly.
- **Active dates:** distinct local calendar dates containing at least one qualifying user-recorded action. Do not treat an unrecorded date as proof of inactivity.
- **Scheduled completion rate:** qualifying completions divided by eligible scheduled opportunities, only if schedule and missed/rest semantics have been explicitly defined. Planned rest days are excluded from eligible opportunities.
- **Effort mix:** counts of Tiny/Usual/Extra actions. Do not assign an invented numeric value to Tiny/Usual/Extra or imply that Extra is morally superior.
- **Journal activity:** journal entries shown separately from habit actions. Journaling alone should not silently increment a habit metric.

Rules:
1. Every number has a plain-language label, time period, and accessible explanation.
2. No streak is the sole or dominant progress measure.
3. No score decays merely because time passes.
4. No inferred events. An absence of records is “not recorded,” not “failed.”
5. Corrections to historical records recompute summaries.
6. Empty states do not show zero as a judgement; invite the first record without pressure.
7. Colour, shape, text, and accessible labels all communicate meaning; colour alone never does.
8. The user can inspect the records behind a summary.
9. If the measure cannot be explained simply or tested with known examples, do not ship it.

## 4. Comeback flow: behaviour contract

The return experience is a brief, dismissible welcome, not a gate. It must:
- preserve all prior records and visual progress;
- avoid asking why the person was away;
- never label a lapse as failure or imply they owe the app an explanation;
- offer a low-friction next step and an equally valid “not now” path;
- distinguish app inactivity from inactivity in one specific habit;
- avoid interrupting someone who has continued journaling but not logging habits;
- run offline using local dates and stored records;
- be shown at a restrained frequency, with dismissal persistence specified before implementation;
- not rely on notification permission or a network connection.

The older broad inactivity-band proposal is superseded by owner-approved trigger/date rules D-047–D-056 and D-146–D-160. The active v1 contract triggers the two-week experience after 14 complete local calendar days without opening the app and the broader one-month experience after 30 days without opening or after 30 days without an active habit/goal; the 30-day experience takes precedence if both qualify. Each experience is shown at most once per qualifying episode. The remaining design work is presentation, final copy, accessibility/localisation and usability review—not reopening those trigger policies.

## 5. Return page copy examples

Copy examples are prototypes for review, not final copy. Use a calm title, one short paragraph, and no more than three actions. Do not make the page a modal that blocks the Today screen.

### Exploratory copy — 3 days away (not an approved v1 trigger)

**Known state:** the app has not been opened for three days. We do not know why.

**Example**
- Title: “Good to see you again.”
- Body: “Your earlier progress is still here. If it helps, choose one small thing for today — or take a look around first.”
- Primary: “Choose one small step”
- Secondary: “Review my habits”
- Quiet option: “Not now”

### After 14 days away

**Known state:** the app has not been opened for fourteen days. We do not infer that the user gave up or that their life was difficult.

**Example**
- Title: “You can pick this up from here.”
- Body: “Nothing you recorded has been erased. You can start with a tiny action, adjust a habit, or simply look at what you’ve already done.”
- Primary: “Start small”
- Secondary: “Review my progress”
- Quiet option: “Skip for now”

### Exploratory copy — 90 days away (not an approved v1 trigger)

**Known state:** the app has not been opened for ninety days. Avoid melodrama or treating this as a special failure state.

**Example**
- Title: “A new day, whenever you’re ready.”
- Body: “Your history is still yours. There’s no catch-up required. Start fresh with one small step, change your plan, or continue whenever it suits you.”
- Primary: “Choose a small step”
- Secondary: “Adjust my habits”
- Quiet option: “Just look around”

### Returning after never completing a habit

**Known state:** a habit exists, but there is no qualifying completion record. Do not say “welcome back to your progress” if no action has been recorded, and do not imply the user has failed.

**Example**
- Title: “Want to make the first step smaller?”
- Body: “You haven’t logged this habit yet. That’s useful to know: you can make the first step easier, choose a different plan, or leave it for later.”
- Primary: “Make it tiny”
- Secondary: “Edit this habit”
- Quiet option: “Not now”

The copy must not assume the habit is difficult, that the user feels guilty, or that they want to continue it. If the habit has been archived or deleted, do not resurrect it automatically.

## 6. State matrix and test cases

| Scenario | Required behaviour |
|---|---|
| 3 days since app opened; habits logged recently | Welcome is about app return, not falsely claiming all habits stopped |
| 14 days since app opened; one habit active, another inactive | Do not describe all habits as abandoned |
| 90 days since app opened; prior history exists | Preserve and offer to inspect history; no reset |
| Habit exists with zero completions | Use first-step language; do not imply lost progress |
| User journals daily but has no habit completions | Do not classify the whole person/app as inactive based on habit records |
| Planned rest or a non-scheduled date | Do not count as a missed opportunity |
| User dismisses the page | Respect dismissal according to the eventual persistence rule |
| Device timezone changed | Use the approved date policy; do not produce a false inactivity band |
| No network connection | Same copy and behaviour available offline |
| No habits exist | Offer create/explore/skip; do not show an empty progress score |

Boundary tests must cover every proposed band edge (1/2/3, 6/7, 13/14, 29/30, 89/90), zero history, archived habits, edited records, timezone travel, manual clock changes, and repeat launches. Use a central date policy rather than scattering date arithmetic across screens.

## 7. Research plan

Conduct short, task-based sessions with people who use habit apps and people who have stopped using one. Do not claim results until sessions happen.

Tasks:
1. Explain what the Growth Mark means without reading a tooltip.
2. Identify what data contributes to the mark and how to inspect it.
3. Explain what an empty date means.
4. Read each return page and say what action they believe is expected.
5. Ask whether the copy implies guilt, judgement, or a requirement to catch up.
6. Compare the mark with a plain text progress summary.
7. Repeat on a small screen and with enlarged text.

Record observed behaviour, confusion, quotes only with consent, participant context at a non-identifying level, and resulting design changes. Do not collect private journal content.

## 8. Open decisions

- What exact events count toward “recorded effort”?
- Is Growth Mark a brand illustration, a data visualisation, or both?
- Is the first release better served by a simple count instead of an emblem?
- What persists after a user dismisses a comeback page, and for how long?
- Is a comeback page shown once per absence episode, or only as a non-blocking Today card?
- Which timezone and date policy governs elapsed days?
- Should users disable comeback prompts entirely?

No implementation should encode these choices until the decision log is updated and the owner approves the relevant semantics.
