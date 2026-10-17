# Dayseed

**Your progress is more than your streak.**

Dayseed is an Android-first, offline-first habit and micro-journal app for people who want to build consistency without being shamed when life interrupts them.

> **Project status: planning and validation.** This repository is the source of truth for product decisions, research, design, architecture, quality requirements, and implementation planning. Do not treat this documentation phase as approval to build the app.

## Product direction

Dayseed should make it easier to do a little, notice progress, reflect briefly, and return after a break. It should treat effort and context as more meaningful than perfect streaks.

Core principles:
- **Progress over perfection:** a tiny action can count; a missed day does not erase past effort.
- **Come back without shame:** no scolding, guilt loops, or assumptions about why someone was away.
- **Private by default:** journal entries and personal habit data stay on-device in the first release.
- **Works offline:** core habits, journaling, history, and comeback guidance must not require a network connection.
- **Useful, not noisy:** insights should be understandable, actionable, and derived locally.
- **Small-screen first:** design for Android phones and realistic low-connectivity use.
- **Trust before monetisation:** no ads or purchases blocking core functionality.

## Documentation map

| Document | Purpose |
|---|---|
| [Project charter](docs/00-PROJECT-CHARTER.md) | Vision, scope, principles, and release gates |
| [Product specification](docs/01-PRODUCT-SPECIFICATION.md) | Requirements and behaviour definitions |
| [UX and design](docs/02-UX-AND-DESIGN.md) | Navigation, screens, design directions, accessibility |
| [Technical architecture](docs/03-TECHNICAL-ARCHITECTURE.md) | Platform and architecture options, proposed stack |
| [Android platform research](docs/13-ANDROID-PLATFORM-RESEARCH.md) | Official Android build route, APK/AAB distinction, package validation gate |
| [Stack comparison](docs/14-STACK-COMPARISON.md) | Expo versus native Kotlin/Compose, scoring, recommended candidate, technical-spike gate |
| [Growth Mark and Comeback research](docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md) | Metric semantics, return examples, and boundary tests |
| [Journal archive research](docs/16-JOURNAL-ARCHIVE-RESEARCH.md) | Timeline/calendar/editorial comparison and usability tasks |
| [Typography, colour, and accessibility](docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md) | Font candidates, semantic palettes, contrast, text scaling, dark theme |
| [Competitor screenshot audit](docs/18-COMPETITOR-SCREENSHOT-AUDIT.md) | Inspected public screenshots and limits of current evidence |
| [Planning gate and Replit/GitHub workflow](docs/19-PLANNING-GATE-AND-REPLIT-GITHUB-WORKFLOW.md) | Native app acceptance, approval gates, quota-exhaustion handoff |
| [Data and privacy](docs/04-DATA-AND-PRIVACY.md) | Local data model, privacy, backup and deletion |
| [Comeback and encouragement](docs/05-COMEBACK-AND-ENCOURAGEMENT.md) | Re-entry states and humane language rules |
| [Monetisation](docs/06-MONETISATION.md) | Ethical monetisation hypotheses and boundaries |
| [Testing and quality](docs/07-TESTING-AND-QUALITY.md) | Test strategy, accessibility, offline and release checks |
| [Implementation roadmap](docs/08-IMPLEMENTATION-ROADMAP.md) | Gated phases and acceptance criteria |
| [Decision log](docs/09-DECISION-LOG.md) | Approved, proposed, and unresolved decisions |
| [Research and competitors](docs/10-RESEARCH-AND-COMPETITORS.md) | Research plan and competitor-review framework |
| [Risks and open questions](docs/11-RISKS-AND-OPEN-QUESTIONS.md) | Uncertainties that must be resolved |
| [Progress and handover](docs/12-PROGRESS-AND-HANDOVER.md) | Current status and instructions for the next session |
| [Design review checklist](docs/DESIGN-REVIEW-CHECKLIST.md) | Review gate before design approval |

## Status vocabulary

- **Approved:** explicitly accepted as a project decision.
- **Proposed:** a recommended direction that still needs review.
- **Open:** unresolved; do not silently decide it during implementation.
- **Deferred:** intentionally outside the current release.
- **Verified:** checked against an actual artefact, test, or source. Do not use this label for assumptions.

The technical stack, final visual direction, reminder semantics, and detailed monetisation placement remain proposals or open questions until explicitly approved and validated.

## Non-goals for the first release

Unless the decision log changes, do not include cloud accounts or sync, AI-generated personal analysis, social feeds or leaderboards, virtual pets or complex gamification, or a large paid content library.

## Planning gate

Implementation must not begin until the product requirements, key user flows, data/privacy approach, architecture, testing plan, and release scope have been reviewed together. Any implementation agent must first read the documentation index, DESIGN.md, decision log, risk register, handover, and planning-gate workflow.

## Repository rules

1. Documentation is the current deliverable; do not create a website or start app implementation without explicit planning-gate approval.
2. DESIGN.md is the single authoritative design-token and component reference.
3. Keep decisions and proposals visibly distinct.
4. Link related documents and update the decision log when a decision changes.
5. Record the date and URL for external research actually consulted. Never invent sources, benchmark numbers, or competitor capabilities.
6. Every roadmap phase needs prerequisites, deliverables, acceptance criteria, tests, risks, and an exit gate.
7. Keep private journal content out of analytics, advertising identifiers, crash reports, and logs.
8. Re-audit cross-document consistency after substantive changes.
