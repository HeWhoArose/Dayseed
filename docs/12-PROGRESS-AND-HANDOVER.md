# Progress and Handover

**Last updated:** 2026-10-18  
**Status:** Planning phase; planning gate not approved; feature implementation is not authorised. Existing source files/preview are evidence for the later audit, not proof of a verified build or approval to add features.

## Repository status

Repository: https://github.com/HeWhoArose/Dayseed  
Canonical branch: `main`. GitHub is the source of truth for planning and future implementation, regardless of the active coding environment.

## Current state

- Product direction and core v1 boundaries: documented as a planning baseline; some product semantics remain open.
- UX directions and information architecture: explored, not approved.
- Existing repository source: native Kotlin + Jetpack Compose, `SQLiteOpenHelper`, repository, ViewModel/StateFlow, and screens are present. Treat the project as being in the planning phase for our workflow; do not add features yet. Source/preview presence does not verify build, tests, installation, or device behavior. The later audit must reconcile observed source with the approved plan.
- Growth Mark and return experience: concepts, definitions, sample copy for 3/14/90 days, zero-completion state, and test cases documented in [docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md). Not approved.
- Journal archive: timeline/calendar/editorial comparison and task-based test plan documented in [docs/16-JOURNAL-ARCHIVE-RESEARCH.md](16-JOURNAL-ARCHIVE-RESEARCH.md). Hybrid timeline + calendar is only a proposal.
- Typography, light/dark candidate palettes, contrast checks, font licensing notes, and small-screen tests documented in [docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md).
- DESIGN.md is now specified as the sole authoritative token/component reference; candidate values remain unapproved.
- Competitor screenshot evidence for Loop Habit Tracker and Daylio is recorded in [docs/18-COMPETITOR-SCREENSHOT-AUDIT.md](18-COMPETITOR-SCREENSHOT-AUDIT.md). Public listing screenshots were inspected; current live app flows have not been tested.
- AI Studio/GitHub workflow and native-app feasibility-spike acceptance criteria are in [docs/19-AI-STUDIO-GITHUB-WORKFLOW.md](19-AI-STUDIO-GITHUB-WORKFLOW.md).
- Data/privacy baseline is drafted; Android backup behaviour, exact date policy, and export/delete details still need decisions.
- Monetisation remains a hypothesis; no provider/SDK selected.
- Existing app source is present on `main`, but the current workflow remains in planning and feature work is not authorised. Do not claim a clean build or verified implementation without evidence.
- Automated tests/build: none run; there is no app source in this planning package.


## Planning decisions confirmed — 2026-10-18

The owner has confirmed the following product rules in conversation and they are recorded in [Decision Log](09-DECISION-LOG.md):

- Numeric entries are whole numbers only; an intentional zero is valid and differs from no record. Multiple entries for a habit/date sum to a daily total.
- Weekly targets are flexible, with optional preferred days that do not restrict completion.
- Schedule edits offer Apply today/Apply tomorrow while preserving existing activity records unchanged.
- Historical completion uses the target that applied at the time; comparisons against the current target are separate.
- Rest/non-scheduled days are neutral, paused periods are excluded, and scheduled non-completion is a missed opportunity rather than a judgement.
- Habit measurement mode is selected by the user: duration, quantity, or completion-only. For measured modes, progress labels are derived from actual amounts relative to the usual target, not from self-reported feelings.
- Today cards expose separate quick Complete and detailed Record actions. Quick Complete defaults to the usual target for measured habits; Record supports actual amounts, corrections, and an optional habit/date note.

The current phase remains **planning**. Continue documenting product rules, the eight-screen blueprint, navigation, data contracts, and edge cases. Do not start feature implementation. The existing native app source and preview are evidence for the later implementation-reality audit, not proof that implementation is approved or verified. Exact progress-level thresholds, week boundaries, note cardinality, duplicate/undo behavior, and the detailed historical schedule model remain open.

## Cumulative research inventory and audit — 2026-10-17

- Added [Cumulative Research Inventory and Documentation Audit](20-CUMULATIVE-RESEARCH-INVENTORY-AND-AUDIT.md) to distinguish documented hypotheses from completed evidence and approved decisions.
- Enumerated all 26 Markdown files on `main`; checked relative Markdown file targets and found no missing target files. Heading anchors, external URLs, and claim-level citations still need deeper review.
- Confirmed the competitor research document is a plan, not a completed market study; public listing screenshots are not hands-on competitor tests.
- The audit prioritises day-record semantics, date/time policy, journal cardinality, reminders, privacy/export/deletion, representative usability validation, and the owner approval gate.
- This was a structural/status pass, not a complete line-by-line source and contradiction audit. No app code was created and no implementation is authorised.

## Latest research package

New research documents:
1. [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
2. [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md)
3. [Typography, Colour, and Accessibility Research](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
4. [Competitor Screenshot Audit](18-COMPETITOR-SCREENSHOT-AUDIT.md)
5. [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md)

Updated references: README.md, DESIGN.md, docs/02-UX-AND-DESIGN.md, docs/08-IMPLEMENTATION-ROADMAP.md, and docs/09-DECISION-LOG.md.

## Next actions

1. Audit all documentation for contradictions, dead links, duplicates, stale claims, and missing acceptance criteria.
2. Resolve or explicitly defer high-impact product questions: daily outcome semantics, schedule/missed-day rules, local date/time policy, journal multiplicity, data backup/export/delete, and reminder behaviour.
3. Create comparable low-fidelity screen samples or wireframes for Today, Growth Mark, comeback return, journal archive, and settings/privacy; do not create app code as a substitute.
4. Inspect current live competitor flows on an Android device where possible; until then, keep conclusions limited to publisher screenshots/listing copy.
5. Run typography/contrast and enlarged-text checks on prototype screens; update DESIGN.md only when a token is approved.
6. Complete the planning gate and record explicit owner approval before authorising the bounded native-app feasibility spike.
7. After the spike passes and stack approval is explicit, begin feature implementation in small testable phases.

## Rules for the next AI or contributor

- Read README.md, DESIGN.md, docs/09-DECISION-LOG.md, docs/11-RISKS-AND-OPEN-QUESTIONS.md, this handover, and docs/19-AI-STUDIO-GITHUB-WORKFLOW.md first.
- AI Studio is the active AI-assisted development environment; do not create a website as a substitute for the Android app. Do not start implementation until the owner explicitly approves the planning gate.
- Treat approved, proposed, open, deferred, and verified as distinct states.
- DESIGN.md is the sole authoritative design-token/component reference.
- Do not invent research sources, user research results, competitor capabilities, SDK compatibility, tests, or build status.
- Do not start app implementation until the planning gate is explicitly approved.
- GitHub is canonical. Before continuing after an AI Studio session/context reset, refresh from the latest committed GitHub state and reconcile local changes before pushing.
- Keep journal privacy and offline operation as core constraints.
- For changes, update cross-referenced documents and run a consistency audit.
- At each handover, state exactly what changed, evidence, unresolved issues, and next action.

## Verification status

The repository tree confirms a native Kotlin/Compose Android project and application source files exist. Source inspection identified a version-1 SQLiteOpenHelper database and placeholder `onUpgrade`; no Gradle build, test suite, APK install, or physical-device run was performed during this documentation audit. Older statements that app implementation has not started are stale. See `docs/22-IMPLEMENTATION-REALITY-AUDIT.md`.

## Research follow-up — 2026-10-17

The head-to-head comparison is in [docs/14-STACK-COMPARISON.md](14-STACK-COMPARISON.md). The five new research and workflow documents are linked above. D-008 (visual direction), D-009 (stack), D-016 (Growth Mark), D-017 (comeback), D-018 (journal archive), D-019 (typography/colour), D-021 (competitor evidence), and D-022 (AI Studio/GitHub workflow) remain open or proposed. No design or implementation approval has been recorded.


## Additional planning decisions confirmed — 2026-10-19

- Users may choose the first day of their week; weekly goals, summaries, and calendar displays must use that choice consistently.
- Multiple duration sessions for the same habit/date are summed into a daily duration total, while individual sessions remain inspectable and editable. Different habits keep separate totals and units.
- Support both an optional habit/date note as the default and optional notes on individual entries. These are separate from the general daily journal.
- Provide a way to undo a recent accidental action, correct/delete entries later, and prevent accidental duplicate quick-completion taps without blocking legitimate repeat sessions.
- Discreet XP/rank near the profile is an idea to research, not an approved feature. Online leaderboards and viewing other people's streaks remain deferred pending explicit approval and privacy/safety/architecture research.

- Daily duration/quantity outcomes distinguish Partial, Goal met, and Above goal while preserving actual amounts. Weekly review distinguishes daily outcomes from flexible weekly-target progress. A short optional, nonjudgmental encouragement sequence after partial progress is approved in direction only; cadence, trigger, copy, and opt-out remain open.

- The owner prefers both in-app encouragement and optional phone notifications. Plan outcome-aware messages for partial, goal-met, above-goal, and still-unrecorded scheduled habits, plus a later-day check-in. Explore learning a user's common completion times over repeated observations, with manual controls, quiet hours, rate limits, opt-out, and privacy safeguards. Learning thresholds, timing defaults, and reminder coordination remain open; this is planning only.

- Reminder setup decision: ask once during onboarding whether reminders are wanted. Yes suggests 9:00 a.m. local time as the default daily reminder, which the user can change; No creates none. Do not repeat the global question for every habit. Each habit offers a small reminder/timer control and inherits the current default unless the user chooses a custom time. Existing custom times must not be overwritten. Dayseed may suggest time changes from observed completion patterns but must not apply them silently. Suggested default-time method, per-habit reset rules, learning threshold, quiet hours, and Android permission flow remain open.

Continue in planning. Do not implement these ideas or start the architecture audit until the remaining product decisions, screen blueprint, cross-screen contracts, and planning consistency review are complete.

## Implementation reality audit (2026-10-18)

The current repository contains a native Kotlin/Jetpack Compose Android app and SQLite persistence code; the older planning-only status is stale. Source presence is not build/test verification. Read [Implementation Reality Audit](22-IMPLEMENTATION-REALITY-AUDIT.md) before changing architecture or adding features. Gradle build, tests, APK install, and device validation remain unverified by this audit. AI Studio is the active AI-assisted development environment; GitHub `main` remains canonical.
