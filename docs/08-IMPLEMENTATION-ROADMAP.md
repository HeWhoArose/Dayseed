# Implementation Roadmap

**Status:** Proposed sequence. Do not begin implementation until the planning gate is approved.

Each phase must produce evidence and pass its exit gate before the next phase begins.

## Phase 0 — Planning and product validation
**Prerequisites:** none.  
**Deliverables:** reviewed charter, product requirements, open-question register, user research plan, decision log.  
**Acceptance:** core jobs, v1 boundaries, daily record semantics, privacy promise, and success measures are explicit.  
**Tests/review:** document consistency audit; review with representative target users where feasible.  
**Risks:** hidden assumptions become expensive implementation changes.  
**Exit gate:** product owner approves scope and unresolved items are consciously deferred or resolved.

## Phase 1 — UX and design validation
**Prerequisites:** Phase 0 exit.  
**Deliverables:** information architecture, primary-flow wireframes, visual direction, content inventory, accessibility checklist, prototype or equivalent.  
**Acceptance:** users can explain how to log, journal, edit history, return after a break, and manage data.  
**Tests/review:** usability sessions, small-screen review, large text and screen-reader review.  
**Risks:** over-designed daily flow or patronising language.  
**Exit gate:** UX and copy reviewed and approved.

## Phase 2 — Technical and privacy feasibility
**Prerequisites:** Phase 0 scope; Phase 1 may run in parallel where appropriate.  
**Deliverables:** compare React Native + Expo against native Kotlin + Jetpack Compose using [docs/14-STACK-COMPARISON.md](14-STACK-COMPARISON.md); verified toolchain; local database design; date policy; backup/deletion design; notification feasibility findings.  
**Acceptance:** core platform choices supported by evidence from official documentation and a bounded technical spike.  
**Tests/review:** Android release-build experiment, persistence/migration experiment, offline and export/delete checks.  
**Risks:** platform limitations, backup surprises, date errors.  
**Exit gate:** architecture and privacy review approved.

## Phase 3 — Foundation
**Prerequisites:** Phases 0–2 approved.  
**Deliverables:** project skeleton, navigation shell, design tokens, persistence layer, migration framework, clock/date abstraction, automated test baseline.  
**Acceptance:** reproducible build; no feature data depends on network; persistence tests pass.  
**Tests:** clean install, restart persistence, migration tests, static analysis and formatting.  
**Risks:** premature abstraction or untestable date logic.  
**Exit gate:** foundation checklist passes.

## Phase 4 — Habit management and daily records
**Prerequisites:** Phase 3.  
**Deliverables:** create/edit/archive/delete habits, schedules, effort-level logging, history correction.  
**Acceptance:** approved semantics enforced in domain and database; edits update summaries consistently.  
**Tests:** domain, persistence, UI, duplicate action and date-boundary regression tests.  
**Risks:** ambiguous missed/rest semantics.  
**Exit gate:** H1–H4 acceptance scenarios pass.

## Phase 5 — Micro-journal and privacy controls
**Prerequisites:** Phase 3; data design approved.  
**Deliverables:** reflection flow, edit/delete entry, export and delete controls.  
**Acceptance:** journal works offline and independently of habit outcomes; sensitive text never enters logs.  
**Tests:** save/reopen, long text, deletion, export scope, backup behaviour.  
**Risks:** accidental disclosure or data loss.  
**Exit gate:** J1 and privacy acceptance checklist pass.

## Phase 6 — Comeback Flow and local insights
**Prerequisites:** Phases 4–5; thresholds and language approved.  
**Deliverables:** comeback rules, dismissal preferences, local summaries.  
**Acceptance:** history preserved; no assumptions about absence; app and habit inactivity distinguished.  
**Tests:** all threshold boundaries, clock/timezone anomalies, offline operation, summary recomputation.  
**Risks:** guilt-inducing copy or incorrect inactivity calculations.  
**Exit gate:** C1/I1 scenarios pass user review.

## Phase 7 — Reminders and accessibility hardening
**Prerequisites:** core flows stable; reminder behaviour approved.  
**Deliverables:** optional reminders, permission UX, accessibility remediation.  
**Acceptance:** denying notifications does not block app; accessible controls and text scaling verified.  
**Tests:** permission matrix, OS restrictions, TalkBack, contrast, large text, reduced motion.  
**Risks:** unreliable OS scheduling and inaccessible controls.  
**Exit gate:** accessibility and reminder checklist passes.

## Phase 8 — Monetisation evaluation (optional)
**Prerequisites:** privacy review and core app acceptance.  
**Deliverables:** evidence-based decision on no ads, ads, or one-time supporter purchase; provider and policy review if applicable.  
**Acceptance:** core features remain ungated and no sensitive data reaches ad systems.  
**Tests:** offline failure, no intrusive placement, consent and privacy review.  
**Risks:** trust damage, policy violations, SDK bloat.  
**Exit gate:** explicit approval; otherwise document “no monetisation in v1”.

## Phase 9 — Beta and release readiness
**Prerequisites:** prior required phases complete.  
**Deliverables:** release build, install/upgrade instructions, store assets and disclosures, known-issues list, support and recovery guidance.  
**Acceptance:** release checklist complete; no unresolved critical data-loss/privacy defects.  
**Tests:** clean install, upgrade, offline use, backup/deletion, accessibility, representative devices.  
**Risks:** device-specific failures and unclear disclosures.  
**Exit gate:** product owner authorises release.

## Execution discipline
For each defect: reproduce → identify root cause → implement the smallest safe fix → targeted test → regression test → full suite → re-audit affected requirements. Keep the roadmap, decision log, and handover current. Never mark a phase complete based only on code existing.