# Implementation Roadmap

**Status:** Proposed sequence. Do not begin implementation until the planning gate is explicitly approved by the product owner.

Each phase must produce evidence and pass its exit gate before the next phase begins. Documentation completeness does not itself authorise app implementation.

## Phase 0 — Planning and product validation

**Prerequisites:** none.  
**Deliverables:** reviewed charter, product requirements, open-question register, user research plan, decision log.  
**Acceptance:** core jobs, v1 boundaries, daily record semantics, privacy promise, and success measures are explicit.  
**Tests/review:** document consistency audit; review with representative target users where feasible.  
**Risks:** hidden assumptions become expensive implementation changes.  
**Exit gate:** product owner approves scope and unresolved items are consciously deferred or resolved.

## Phase 1 — UX and design validation

**Prerequisites:** Phase 0 exit.  
**Deliverables:** information architecture, primary-flow wireframes, Growth Mark comparison, comeback copy variants, journal archive comparison, typography/colour samples, accessibility checklist, prototype or equivalent. DESIGN.md is the sole design-token/component reference.  
**Acceptance:** users can explain how to log, journal, edit history, return after a break, interpret progress, and manage data.  
**Tests/review:** task-based prototype tests; small-screen and enlarged-text review; light/dark contrast audit; TalkBack review.  
**Risks:** over-designed daily flow, arbitrary visual rewards, or patronising language.  
**Exit gate:** UX, visual direction, core copy, and accessibility findings reviewed and approved.

## Phase 2 — Technical and privacy feasibility

**Prerequisites:** Phase 0 scope; Phase 1 may run in parallel where appropriate.  
**Deliverables:** compare React Native + Expo against native Kotlin + Jetpack Compose using [docs/14-STACK-COMPARISON.md](14-STACK-COMPARISON.md); verified toolchain; local database design; date policy; backup/deletion design; notification feasibility findings.  
**Acceptance:** core platform choices supported by official-documentation evidence and a bounded technical spike.  
**Tests/review:** Android release-build experiment, persistence/migration experiment, offline and export/delete checks.  
**Risks:** platform limitations, backup surprises, date errors.  
**Exit gate:** architecture and privacy review approved.

## Phase 3 — Planning handoff gate

**Prerequisites:** Phases 0–2 reviewed; design and architecture decisions either approved or explicitly deferred with safe defaults.  
**Deliverables:** consistent documentation set, implementation prompt, repository workflow, scoped feasibility-spike acceptance criteria, release test plan, approval record.  
**Acceptance:** no unresolved high-impact contradiction about data semantics, privacy, date handling, native-app target, or scope. Remaining open questions have owners and are safe to defer.  
**Tests/review:** cross-document link check, contradiction/duplicate/outdated-plan audit, requirement-to-test traceability review, and owner walkthrough.  
**Risks:** implementation agent silently fills gaps or creates a website instead of the mobile app.  
**Exit gate:** explicit owner approval recorded in docs/09-DECISION-LOG.md and docs/19-AI-STUDIO-GITHUB-WORKFLOW.md.

## Phase 4 — Bounded native-app feasibility spike

**Prerequisites:** Phase 3 approval and explicit permission to scaffold the spike.  
**Deliverables:** minimal React Native + Expo native app, locked dependency set, Android build artifact, SQLite persistence/migration evidence, reminder and privacy feasibility report.  
**Acceptance:** project is verifiably native, not a website; APK/dev build installs and launches on a physical Android device; seeded data survives restart; migration, offline, export/delete, and relevant notification tests have recorded outcomes.  
**Tests:** clean dependency install, SDK alignment checks, app build, device install/launch, migration tests, airplane-mode checks, permission denial, schedule/cancel/restart, synthetic export/delete, static checks, full available suite.  
**Risks:** AI-generated project may be web-only, package compatibility may differ, reminder timing may be OS-limited.  
**Exit gate:** evidence and blockers reviewed; owner explicitly approves the stack and feature implementation plan. A failed spike does not automatically authorise an alternative stack.

## Phase 5 — Foundation

**Prerequisites:** feasibility spike passed or its blocker consciously accepted; architecture approved.  
**Deliverables:** project skeleton, navigation shell, design tokens from DESIGN.md, persistence layer, migration framework, clock/date abstraction, automated test baseline.  
**Acceptance:** reproducible build; no feature data depends on network; persistence tests pass.  
**Tests:** clean install, restart persistence, migration tests, static analysis and formatting.  
**Risks:** premature abstraction or untestable date logic.  
**Exit gate:** foundation checklist passes.

## Phase 6 — Habit management and daily records

**Prerequisites:** Phase 5.  
**Deliverables:** create/edit/archive/delete habits, schedules, effort-level logging, history correction.  
**Acceptance:** approved semantics enforced in domain and database; edits update summaries consistently.  
**Tests:** domain, persistence, UI, duplicate action and date-boundary regression tests.  
**Risks:** ambiguous missed/rest semantics.  
**Exit gate:** H1–H4 acceptance scenarios pass.

## Phase 7 — Micro-journal and privacy controls

**Prerequisites:** Phase 5; data design approved.  
**Deliverables:** reflection flow, edit/delete entry, export and delete controls, archive pattern selected through research.  
**Acceptance:** journal works offline and independently of habit outcomes; sensitive text never enters logs.  
**Tests:** save/reopen, long text, deletion, export scope, backup behaviour, known-date archive retrieval.  
**Risks:** accidental disclosure or data loss.  
**Exit gate:** J1 and privacy acceptance checklist pass.

## Phase 8 — Comeback Flow and local insights

**Prerequisites:** Phases 6–7; thresholds and language approved.  
**Deliverables:** comeback rules, dismissal preferences, transparent local summaries and approved Growth Mark if retained.  
**Acceptance:** history preserved; no assumptions about absence; app and habit inactivity distinguished; every metric is explainable.  
**Tests:** all threshold boundaries, zero-completion cases, clock/timezone anomalies, offline operation, summary recomputation.  
**Risks:** guilt-inducing copy, arbitrary reward semantics, or incorrect inactivity calculations.  
**Exit gate:** C1/I1 scenarios pass owner and user review.

## Phase 9 — Reminders and accessibility hardening

**Prerequisites:** core flows stable; reminder behaviour approved.  
**Deliverables:** optional reminders, permission UX, accessibility remediation.  
**Acceptance:** denying notifications does not block app; accessible controls and text scaling verified.  
**Tests:** permission matrix, OS restrictions, TalkBack, contrast, large text, reduced motion.  
**Risks:** unreliable OS scheduling and inaccessible controls.  
**Exit gate:** accessibility and reminder checklist passes.

## Phase 10 — Monetisation evaluation (optional)

**Prerequisites:** privacy review and core app acceptance.  
**Deliverables:** evidence-based decision on no ads, ads, or one-time supporter purchase; provider and policy review if applicable.  
**Acceptance:** core features remain ungated and no sensitive data reaches ad systems.  
**Tests:** offline failure, no intrusive placement, consent and privacy review.  
**Risks:** trust damage, policy violations, SDK bloat.  
**Exit gate:** explicit approval; otherwise document “no monetisation in v1”.

## Phase 11 — Beta and release readiness

**Prerequisites:** prior required phases complete.  
**Deliverables:** release build, install/upgrade instructions, store assets and disclosures, known-issues list, support and recovery guidance.  
**Acceptance:** release checklist complete; no unresolved critical data-loss/privacy defects.  
**Tests:** clean install, upgrade, offline use, backup/deletion, accessibility, representative devices.  
**Risks:** device-specific failures and unclear disclosures.  
**Exit gate:** product owner authorises release.

## Execution discipline

For each defect: reproduce → identify root cause → implement the smallest safe fix → targeted test → regression test → full suite → re-audit affected requirements. Keep the roadmap, decision log, and handover current. Never mark a phase complete based only on code existing. See [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md) for cross-environment rules.
