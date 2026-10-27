# Dayseed Project Charter

**Status:** Planning draft; review required  
**Product promise:** Your progress is more than your streak.

## Vision
Help people build meaningful habits through small actions, reflection, and compassionate recovery after interruptions. Reward honest engagement rather than perfection.

## Problem statement
Many habit tools make consecutive-day streaks the dominant measure of success. That can make a lapse feel like failure and discourage returning. Dayseed should preserve the record of effort, support different levels of capacity, and make restarting feel ordinary. This is a product hypothesis, not a claim that all users experience habit apps the same way; validate it through interviews and usability testing.

## Intended audience
Android users who want to build or maintain personal habits, especially people who struggle with consistency, interruptions, or restarting. The app may support lightweight daily reflection and self-care, but must not present itself as medical treatment.

## Product principles
1. **Effort-aware:** tiny, usual, and extra effort levels can all be meaningful.
2. **Nonjudgmental:** never label a person lazy, broken, or a failure.
3. **History-preserving:** missed days do not erase completed work.
4. **Private by default:** personal records remain local in v1.
5. **Offline-first:** core actions work without connectivity.
6. **Transparent:** explain how summaries are calculated; never invent emotional conclusions.
7. **Accessible:** readable type, sufficient contrast, touch targets, screen-reader labels, and reduced-motion support.
8. **User-controlled:** editing, skipping, pausing, and deleting are understandable.

## Proposed v1 scope
- Create, edit, archive, and delete personal habits.
- Choose a schedule and tracking style.
- Record a daily outcome and effort level.
- Quick micro-journal / daily reflection.
- History and local weekly summaries.
- Comeback Flow after app or habit inactivity.
- Optional local reminders, subject to explicit permission and scheduling decisions.
- Human-readable journal export, complete user-controlled local backup/restore for phone migration, and data deletion. Online backup and cross-device sync are premium future capabilities, not part of the initial v1 release.
- A monetisation model is a candidate for later review only; no ad network, purchase flow, or monetisation feature is approved for initial v1. Any future proposal must never block core functionality and requires explicit product/privacy/policy approval.

## Explicit v1 non-goals
Unless the decision log is deliberately revised: cloud accounts, online backup, and cross-device sync in the initial v1 release; AI-generated analysis; public profiles, feeds, social competition, or leaderboards; virtual pets and elaborate gamification; a large paid content library; and claims to diagnose or treat mental-health conditions. Online backup/sync may be considered later as premium capabilities after a separate privacy, security, consent, data-flow, and monetisation approval gate.

## Success criteria to validate
Do not set arbitrary numerical targets without a baseline. Research should establish whether users understand effort levels; whether returning after a lapse feels supportive; whether logging is quick; whether local storage and deletion are understood; whether offline use is reliable; and whether monetisation harms trust.

## Governance and gates
- **Gate A — Product:** core requirements and unresolved behaviour rules reviewed.
- **Gate B — UX:** flows, wireframes, accessibility, and language reviewed.
- **Gate C — Technical/privacy:** platform feasibility, persistence, migrations, backup, notifications, and deletion reviewed.
- **Gate D — Build readiness:** test plan, release scope, and risk mitigations approved.
- **Gate E — Release:** acceptance criteria pass on representative Android devices.

No implementation is approved merely because a proposal appears in a document.

## Change control
Record consequential decisions in the [decision log](09-DECISION-LOG.md). For changes to scope, update the product specification, affected UX/architecture documents, roadmap, tests, and handover. Re-check cross-document consistency before closing the change.