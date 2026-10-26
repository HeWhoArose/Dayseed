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
**Deliverables:** audit the existing Kotlin/Jetpack Compose project only after the planning gate authorises technical validation; verify Gradle toolchain/build/test/device evidence, reconcile data schema with owner-approved product rules, and document privacy/date/local-backup/Android-platform-backup/deletion/notification gaps. Initial v1 excludes accounts, online backup, and cross-device sync; premium online backup/sync is future scope requiring a separate approval gate. The old Expo comparison is historical only.  
**Acceptance:** core platform choices supported by official-documentation evidence and a bounded technical spike.  
**Tests/review:** Android release-build experiment, persistence/migration experiment, offline and export/delete checks.  
**Risks:** platform limitations, backup surprises, date errors.  
**Exit gate:** architecture and privacy review approved.

## Phase 3 — Planning handoff gate

**Prerequisites:** Phases 0–2 reviewed; design and architecture decisions either approved or explicitly deferred with safe defaults.  
**Deliverables:** consistent documentation set including `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, shared date/time policy, implementation prompt, repository workflow, scoped feasibility-spike acceptance criteria, release test plan, and approval record. The Android platform-backup policy is resolved by D-186: exclude sensitive Dayseed data by default. Verify the actual backup/data-extraction configuration and supported device-transfer paths; this is technical validation, not a remaining owner-choice question.  
**Acceptance:** no unresolved high-impact contradiction about data semantics, privacy, date handling, native-app target, backup recovery, or scope. Remaining open questions have named owners and are safe to defer; any owner-level privacy/recovery choice is explicitly recorded rather than silently defaulted.  
**Tests/review:** cross-document link check, contradiction/duplicate/outdated-plan audit, requirement-to-test traceability review, and owner walkthrough.  
**Risks:** implementation agent silently fills gaps or creates a website instead of the mobile app.  
**Exit gate:** explicit owner approval recorded in docs/09-DECISION-LOG.md and docs/19-AI-STUDIO-GITHUB-WORKFLOW.md.

## Phase 4 — Bounded native-app feasibility spike

**Prerequisites:** Phase 3 approval and explicit permission to scaffold the spike.  
**Deliverables:** validated baseline of the existing native Kotlin/Compose app; reproducible build and test evidence; safe SQLite migration plan; offline persistence, reminders, and privacy/export feasibility report. Do not scaffold a second app or rewrite stacks without explicit owner approval.  
**Acceptance:** existing project is verifiably native; Gradle build succeeds; APK installs and launches on a physical Android device; data survives restart; migration, offline, export/delete, and relevant notification tests have recorded outcomes.  
**Tests:** clean Gradle build, unit/instrumentation tests where configured, device install/launch, migration tests, airplane-mode checks, permission denial, schedule/cancel/restart, synthetic export/delete, static checks, full available suite.  
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
**Deliverables:** reflection flow, edit/delete entry, readable journal export, local backup/restore and delete controls, archive pattern selected through research. Android platform backup must be evaluated separately; online backup/cross-device sync remain outside initial v1.  
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

## Phase 10 — Monetisation (not part of initial v1 unless separately approved)

**Prerequisites:** initial v1 core flows are accepted; privacy and store-policy review is complete; the product owner explicitly opens a separate monetisation decision.  
**Deliverables:** evidence-based proposal for whether any future release should use no monetisation, ads, rewarded extras, a one-time supporter purchase, or another model; provider/SDK and policy review if applicable.  
**Acceptance:** this phase does not add monetisation to initial v1 by default. Core habit, journal, history, insights, export, and backup/restore remain ungated. Journal text and personal habit records are never used for ad targeting or sent to ad systems.  
**Tests:** privacy/data-flow review, offline behavior, consent and store-policy checks, accessibility, and confirmation that no core feature is paywalled.  
**Risks:** trust damage, policy violations, SDK bloat, unexpected data collection.  
**Exit gate:** explicit owner approval of a specific monetisation model and its data flows; otherwise initial v1 remains without ads, purchases, or subscriptions.

## Phase 11 — Beta and release readiness

**Prerequisites:** prior required phases complete.  
**Deliverables:** release build, install/upgrade instructions, store assets and disclosures, known-issues list, support and recovery guidance.  
**Acceptance:** release checklist complete; no unresolved critical data-loss/privacy defects.  
**Tests:** clean install, upgrade, offline use, backup/deletion, accessibility, representative devices.  
**Risks:** device-specific failures and unclear disclosures.  
**Exit gate:** product owner authorises release.

## Execution discipline

For each defect: reproduce → identify root cause → implement the smallest safe fix → targeted test → regression test → full suite → re-audit affected requirements. Keep the roadmap, decision log, and handover current. Never mark a phase complete based only on code existing. See [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md) for cross-environment rules.
