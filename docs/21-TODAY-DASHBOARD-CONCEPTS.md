# Today Dashboard: Comparative UI Concepts

**Research date:** 2026-10-17  
**Status:** Concept exploration; no layout, navigation, visual token, or copy is approved.  
**Purpose:** Continue the Dayseed UI research by comparing different Today-screen priorities before committing to a visual direction or building app UI.

## Product promise

**Your progress is more than your streak.**

The Today screen should help a person answer three questions quickly:
1. What can I do today?
2. How can I record an effort without friction?
3. Where can I reflect or see my progress without being judged?

The dashboard must not turn into a crowded productivity dashboard. Core habit logging and reflection must work offline. A tiny action can count when the habit supports effort levels. A missed or unrecorded day must not erase prior progress or be described as failure.

## Shared requirements for every concept

- Show today's date and a clear screen title or welcome.
- Make today's habits and their primary logging action immediately discoverable.
- Support Tiny / Usual / Extra where configured, and a completion-only action where appropriate.
- Make daily reflection easy to find but never compulsory.
- Give the Growth Mark a meaningful role only if its measure is transparent and explainable.
- Keep progress summaries subordinate to today's actions.
- Avoid guilt copy, arbitrary XP/points, punitive decay, streak-dominant messaging, and forced comeback gates.
- Keep important controls accessible on small Android screens, with large text, TalkBack, and 48dp target checks.
- Treat all examples below as hypotheses, not approved requirements.

## Three layouts to compare

### Concept A — Action-first Today

**Priority:** Complete or record today's habits with as little friction as possible.

**Proposed order**
1. Compact greeting and date.
2. Today's habit list, with each habit's main action visible.
3. Small secondary reflection entry point.
4. A restrained Growth Mark summary or “see progress” link.
5. Persistent bottom navigation, if testing supports it.

**Strengths**
- Fast to scan and use one-handed.
- The main job is obvious.
- Progress decoration is less likely to push actions below the fold.

**Risks**
- May feel plain or transactional.
- The Growth Mark and Dayseed's reflective identity could become too easy to overlook.
- Many habits may create a long list; test scrolling and prioritisation.

**Best question to test:** Can a new user log one habit and find reflection without help?

### Concept B — Growth-led editorial Today

**Priority:** Make the user's accumulated effort visible, while keeping daily actions close at hand.

**Proposed order**
1. Compact greeting and date.
2. A modest Growth Mark / collected-moments card with a plain-language label.
3. Today's habit list.
4. Daily reflection prompt.
5. A small weekly insight or history link.
6. Persistent bottom navigation, if testing supports it.

**Strengths**
- Makes Dayseed's “progress beyond streaks” promise more distinctive.
- Can create a sense of continuity without a leaderboard or score.
- Connects today's actions to a longer personal story.

**Risks**
- A large visual mark may push useful actions too far down.
- Users may mistake collected dates for a daily-compliance score.
- A growth metaphor can become arbitrary gamification unless each visual state maps to real records.

**Best question to test:** Can users explain what the mark means, what contributes to it, and what a blank date means?

### Concept C — Reflective Today

**Priority:** Balance habit logging with a calm, low-pressure check-in.

**Proposed order**
1. Compact greeting and date.
2. A short, optional “How would you like to show up today?” or neutral reflection entry point (copy must be tested; no mood tracking is implied).
3. Today's habit list with clear logging actions.
4. A small progress summary.
5. A journal/history link.
6. Persistent bottom navigation, if testing supports it.

**Strengths**
- Signals that Dayseed supports reflection as well as task completion.
- May feel welcoming to users rebuilding consistency.
- Makes space for a quick journal without treating it as a productivity metric.

**Risks**
- A reflective prompt may feel intrusive or slow down people who just want to log a habit.
- Could imply mood tracking or emotional assessment that is not in v1.
- The prompt must be skippable and should not dominate the first screen.

**Best question to test:** Do users find the reflection prompt supportive, irrelevant, or pressuring—and can they skip it easily?

## Early comparison matrix

| Criterion | A: Action-first | B: Growth-led | C: Reflective |
|---|---|---|---|
| Speed of habit logging | Expected strongest | Depends on mark size/order | Depends on prompt size/order |
| Distinctive Dayseed identity | Moderate | Expected strongest | Moderate |
| Reflection visibility | Secondary | Secondary | Strongest |
| Risk of visual clutter | Low to moderate | Moderate to high | Moderate |
| Risk of pressure/misinterpretation | Low | Higher if metric is unclear | Higher if prompt feels compulsory |
| Evidence status | Hypothesis | Hypothesis | Hypothesis |

These are qualitative predictions, not measured scores or user-test results. Do not select a winner from this table alone.

## Growth Mark integration decision

The Growth Mark should not be given a large dashboard footprint until its purpose is chosen and understood. Research in [15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md) recommends comparing **Collected Moments** with a plain editorial summary. Prototype both in Concepts A and B.

Every number or visual mark must map to an inspectable, explicitly logged event and show its period. An unrecorded day must not be presented as failure. Journal activity must remain distinct from habit activity unless a future, explicit decision says otherwise.

## Shared first-pass content sketch

Example content only; not final copy:

- Header: “Today” plus the local date.
- Habit section: “Your habits” or another tested label.
- Habit row: habit name, optional schedule/context, and a clear logging control.
- Reflection: “Daily reflection” with a short invitation and a clear skip path.
- Progress: a labelled summary such as “Dates with a recorded habit action this week” only if the metric definition is approved.
- Empty state: explain what can be done next without showing a judgemental zero.
- Comeback: a short dismissible message or card, never a blocking gate.

Do not assume a greeting based on time of day is needed; test whether it adds warmth or noise.

## Prototype and usability plan

Create equivalent low-fidelity wireframes for A, B, and C using the same sample habits, same content, same phone viewport, and same visual style. Vary layout hierarchy—not colour, typography, or decorative styling—so tests compare layout rather than branding.

Tasks:
1. Open Today and explain what can be done.
2. Log a Tiny action and explain whether it counts.
3. Find the daily reflection and skip it.
4. Explain the Growth Mark or progress summary.
5. Find yesterday's record and correct it.
6. Find the journal archive.
7. Repeat the primary task with enlarged text.

Record task success, hesitation, wrong taps, time-to-action, misinterpretations, and subjective pressure. Do not claim user research has happened until sessions are conducted. Include people who have struggled to maintain habits as well as confident trackers; do not collect journal content.

## Suggested next sequence

1. Agree on the user jobs and compare A/B/C wireframes.
2. Research the Growth Mark separately and decide whether it deserves a prominent Today card.
3. Prototype the Return Page states (including 3-, 14-, and 90-day examples) without making them blocking.
4. Prototype the journal archive (timeline versus hybrid timeline/calendar).
5. Revisit navigation and screen hierarchy with these concepts together.
6. Run accessibility checks on the chosen candidate, then record explicit decisions in [09-DECISION-LOG.md](09-DECISION-LOG.md).
7. Do not implement app UI until the planning gate is explicitly approved.

## Generated visual reference

A concept-board image was generated during the 2026-10-17 discussion to make the general warm/editorial direction easier to discuss. It depicts five example screens (Today, Growth Mark, Return Page, Journal Archive, and Insights). It is an illustrative reference only: generated imagery may contain inaccurate text, inconsistent details, and invented interface decisions. It is not a functional prototype, user-tested design, or approved specification. The three layout alternatives above still need comparable wireframes.

## Related documents

- [UX and Design](02-UX-AND-DESIGN.md)
- [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
- [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md)
- [Typography, Colour, and Accessibility](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
- [Design Review Checklist](DESIGN-REVIEW-CHECKLIST.md)
- [Decision Log](09-DECISION-LOG.md)
