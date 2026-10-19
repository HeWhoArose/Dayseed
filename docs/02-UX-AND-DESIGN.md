# UX and Design

**Status:** Exploration brief; visual direction and navigation not approved.

## Experience goals

The app should feel encouraging without being childish, calm without being bland, and structured without becoming a productivity dashboard. Make the next useful action obvious and keep logging lightweight.

## Candidate visual directions

These are hypotheses to compare, not final brand decisions.

### A. Field Journal
Warm paper-like surfaces, restrained organic motifs, tactile details, and reflective writing emphasis. Risk: decorative styling or low contrast.

### B. Living Editorial
Confident typography, strong hierarchy, expressive headings, clear cards, and a growth motif used sparingly. Risk: editorial styling competes with daily tasks.

### C. Quiet Instrument
Minimal controls, precise data visualisation, restrained colour, and predictable navigation. Risk: feeling clinical or generic.

**Proposed synthesis:** a warm, distinctive, growth-oriented interface with editorial typography and clear utility. Validate on small Android screens and accessibility settings before approval.

## Information architecture proposal

- **Today:** today's habits, fast completion, daily reflection entry point.
- **History:** habit outcomes and corrections; journal archive access.
- **Insights:** understandable weekly/monthly summaries with transparent metrics.
- **Settings:** reminders, appearance/accessibility, privacy, export/delete, support.

Whether the journal gets a dedicated tab, lives under History, or is accessible from both Today and History remains open. The archive pattern must be tested using the [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md).

## Primary flows

### First launch
Explain the product promise → explain local storage plainly → create first habit or explore a sample → choose optional schedule/reminder → arrive at Today. Do not require account creation.

### Daily logging
Open Today → use Complete for the usual target/quick completion, or Record for actual amounts, corrections, and an optional habit/date note → return to Today. Keep quick completion lightweight and detailed recording optional.

### Reflection
Open reflection → optional prompt → write or skip → save locally → return to Today. Do not require a minimum word count or streak.

### Comeback
Open app after inactivity → supportive, short message or non-blocking card → offer a small step, habit review, or dismissal → resume normal use. Preserve history and avoid asking why the person was away. Use the examples and edge cases in [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md); its copy is not final until reviewed.

### Editing history
Open History → select date/habit → edit outcome → explain that summaries update → save. Corrections should not feel like cheating.

### Privacy
Settings → Privacy and data → explain local storage and OS backup implications → export or delete → clear confirmation and completion state.


## Today dashboard: quick completion and detailed recording

**Status:** Approved interaction direction; visual layout and detailed states remain to be validated.

Each Today habit row/card should make the habit name, measurement mode, usual target, and today's record/completion state easy to scan. Provide two distinct actions where relevant:

- **Complete:** a lightweight quick action. For duration/quantity habits it records the usual target; for completion-only habits it records completion. The interaction must avoid accidental duplicates and offer a clear way to correct an inaccurate quick record.
- **Record:** opens the detailed recording flow for the selected habit and date. It supports the actual duration/quantity, appropriate quick choices derived from the habit's target, a custom amount, review/correction of existing entries, and an optional note about that habit on that date.

For duration/quantity habits, the detailed flow should distinguish the usual target from the actual amount and derive a progress label from actual amount versus target. The label must not imply that Dayseed knows how hard the user tried or how they felt. For completion-only habits, do not show numeric controls or effort-level scales.

A quick action must not force navigation to the full detail page. The detailed flow should be available when users want precision, context, or corrections. An optional habit/date note is distinct from the broader daily journal; neither should be required to log a habit. Support both an optional habit/date note as the default and optional entry-specific notes. Both remain distinct from the general journal. Allow undo of recent actions and later correction/deletion of individual entries; prevent accidental duplicate quick taps without blocking legitimate repeat sessions. Weekly calendar and summaries follow the user's chosen week-start day.

The card must distinguish no record, explicit numeric zero, partial amount, usual target reached, and amount above target. Progress summaries must state their denominator and period. Schedule edits do not rewrite existing activity records; explicit corrections are separate.

The accompanying dashboard mockups are illustrative planning artifacts only. They do not demonstrate working persistence or imply that app UI implementation has been authorised.

## Content and voice

- Warm, direct, adult, and nonjudgmental.
- No guilt, scolding, fake intimacy, or claims about the user's feelings.
- Never say a user “failed” because they missed a day.
- Avoid over-celebrating routine actions.
- Explain consequences before destructive actions.
- Do not imply that journaling reveals a clinical condition.

## Accessibility and small-screen checklist

- Test large system font sizes and display scaling.
- Meet current Android touch-target guidance.
- Ensure sufficient contrast in every theme.
- Colour must not be the only way to distinguish outcomes.
- Provide TalkBack labels, logical focus order, and meaningful control names.
- Support reduced motion; no essential information only in animation.
- Keep important actions reachable with one hand where practical.
- Check keyboard navigation, text entry, empty states, and error messages.
- Follow [DESIGN.md](../DESIGN.md) as the single authoritative token and component reference.

## Research documents

- [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
- [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md)
- [Typography, Colour, and Accessibility Research](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
- [Competitor Screenshot Audit](18-COMPETITOR-SCREENSHOT-AUDIT.md)

## Design deliverables before implementation

Approved information architecture and navigation rationale; wireframes for primary flows; design tokens and component inventory in DESIGN.md; theme and contrast checks; content inventory including comeback variants; reviewed prototype; usability test plan and findings; completed design review checklist.

Do not begin app UI implementation until the planning gate is approved.


## XP and rank — exploration only

The owner is exploring a quiet optional rank/XP indicator near the profile, with restrained rank-up feedback rather than a prominent gamified dashboard. This is not approved for v1. If researched further, it must support the product promise, remain secondary to habit/journal tasks, avoid shame or streak pressure, and have accessible feedback that can be dismissed or reduced.

Online leaderboards and viewing other people's streaks are a separate deferred idea. They would change the local-first/privacy model and require explicit consent, account/network architecture, visibility controls, and safety/moderation research. Do not design them as committed v1 screens.

## Partial progress and weekly review

Show measured outcomes distinctly: **Partial**, **Goal met**, and **Above goal**. Use text and accessible visual indicators, not colour alone. Preserve the exact recorded amount. Do not confuse a missing record with an explicit zero, and do not describe partial progress as full completion.

The weekly review should distinguish days where the daily target was met, days with partial activity, and days above target. Also show weekly-target status separately, since a flexible weekly target may be reached even when some days were partial. Rest/non-scheduled days are neutral and paused periods are excluded.

A short, optional series of supportive tips may follow partial progress during the next week. Keep it separate from the inactivity-based Comeback Flow. Tips should reinforce the value of small steps and suggest a practical next action without guilt, assumptions, or pressure. Cadence, surfaces, opt-out, and exact copy remain open.
