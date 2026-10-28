# AI Continuation Handover and Decision Protocol

**Last updated:** 2026-10-28  
**Canonical repository:** https://github.com/HeWhoArose/Dayseed  
**Canonical branch:** `main`  
**Current phase:** Planning and owner review  
**Planning gate:** CLOSED — not approved  
**Implementation authority:** NOT GRANTED

> This document is the operational handover for the next AI assistant or contributor. Read it before continuing. It supersedes older “next action” lists where they conflict with the current decision log, planning review, or owner decision batch. It does not supersede explicit owner-approved decisions.

## 1. Mission and non-negotiable boundary

Continue Dayseed planning from the current GitHub state without making the owner repeat established context or re-decide routine details. The product is an Android-first, offline-first personal habit tracker and micro-journal. Its guiding promise is **“Your progress is more than your streak.”**

The repository already contains native Kotlin/Jetpack Compose and SQLite application source. For this workflow, feature implementation has **not been authorised**. Existing source is evidence for a later, separately authorised implementation-reality audit; its presence does not prove a clean build, working migration, successful tests, or device readiness.

Until the owner explicitly approves the planning gate and separately authorises the next stage, do **not**:
- edit application source, schema, migrations, live database, build configuration, or app UI;
- run a build, test suite, APK installation, device validation, or implementation-reality audit;
- silently choose a material product/privacy/security/data-integrity policy;
- claim that planned acceptance tests have passed or that runtime behaviour is verified.

Documentation-only planning work, prototype source files, cross-document reconciliation, and link/consistency checks are allowed. If the next requested action would cross the boundary, stop and obtain explicit authorisation.

## 2. Read these sources in this order

1. [Decision Log](09-DECISION-LOG.md) — canonical record of approved owner decisions and their status.
2. [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) — current readiness checklist and remaining planning blockers.
3. [Owner Planning Decision Batch](31-OWNER-PLANNING-DECISION-BATCH.md) — consolidated owner choices. Section 4 contains the response sheet.
4. [Decision Register and Reconciliation](25-DECISION-REGISTER-AND-RECONCILIATION.md) — current operational interpretation and recorded contradictions.
5. [Technical Architecture](03-TECHNICAL-ARCHITECTURE.md) and [Schema Closure and Migration Plan](30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md) — reconciled proposed schema, backup compatibility, conflicts, transactions and rollback.
6. [Requirement Traceability](28-REQUIREMENT-TRACEABILITY.md) and [Testing and Quality](07-TESTING-AND-QUALITY.md) — planned acceptance coverage; not test results.
7. [UX and Design Approval Pack](29-UX-DESIGN-APPROVAL-PACK.md), [Today comparison SVG](prototypes/dayseed-today-variants.svg), [Navigation/archive comparison SVG](prototypes/dayseed-navigation-archive-variants.svg), and [Design Review Checklist](DESIGN-REVIEW-CHECKLIST.md).
8. [Data and Privacy](04-DATA-AND-PRIVACY.md), [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md), [Progress and Handover](12-PROGRESS-AND-HANDOVER.md), and [README](../README.md).
9. Read older research and dated notes only when needed. They may contain superseded hypotheses; do not treat an old “next step” as current authority.

**Authority order when documents disagree:** explicit owner decision in the current Decision Log → current Planning Completeness Review and Decision Register → current owner decision batch → current product/architecture/UX contracts → dated research and historical handover notes. If two authoritative sources still conflict, do not guess: record the contradiction and resolve it in the planning documents.

## 3. How to make decisions — apply D-196

The owner has explicitly asked not to be burdened with repeated micro-questions. Use the following policy throughout planning and later authorised work.

### A. Delegate routine details

Choose a conservative default without asking when the decision is:
- reversible, low-risk and local to the screen/implementation;
- a routine UX or engineering detail that does not alter the product promise or approved semantics;
- already covered by an approved decision or an existing delegated default;
- safe to document, test later, and change without data loss or privacy impact.

Record a delegated default where future work could otherwise drift. Label it **Delegated default under D-196**, include its rationale and planned verification, and keep it subordinate to approved decisions. Do not create duplicate decision-log entries for decisions already settled.

### B. Escalate material choices only

Ask the owner for a decision when it changes or materially affects:
- product scope, user-visible policy or the core product promise;
- personal-data collection, sharing, privacy claims, security or consent;
- data integrity, destructive behaviour, migration, backup/restore or conflict resolution;
- accessibility obligations or a major design/identity/navigation choice;
- monetisation, ads, purchases, accounts, networking or third-party services;
- an irreversible action, a significant trade-off, or a conflict between approved decisions;
- the planning gate or permission to perform implementation/technical validation.

Bundle related owner-level choices into **one concise response batch**. Present a recommendation, alternatives, consequences, and safe deferral for each. Use **Accept / Modify / Defer** (or clearly labelled alternatives). Do not re-ask settled questions; do not interpret silence as approval. A deferral must say what remains out of scope or unresolved and when it should be revisited.

### C. Never turn a recommendation into approval

Use status labels precisely:
- **Approved:** explicit owner decision recorded in the Decision Log.
- **Delegated default:** assistant may choose within the D-196 boundaries; owner may override.
- **Proposed:** recommendation awaiting the required owner decision.
- **Deferred:** intentionally excluded or postponed, with a revisit trigger where appropriate.
- **Unverified:** not established by evidence or execution.
- **Historical/superseded:** retain for context, but do not treat as current direction.

A design mockup, research result, file in Git, written schema, test plan, or passing static-document check is not proof of runtime behaviour. Record the source of evidence and what was actually checked.

### D. Keep an auditable decision trail

For every material choice or delegated default:
1. Check the Decision Log and existing docs first.
2. State the problem and identify any existing decision that governs it.
3. Prefer the smallest safe choice that preserves offline operation, privacy, historical data and user agency.
4. Record rationale, alternatives rejected, consequences, and verification/acceptance cases.
5. Update the Decision Log only for a genuine new/changed owner decision or a meaningful delegated policy; do not create duplicate entries.
6. Reconcile every affected specification, architecture, UX, privacy, traceability and handover document in the same planning workstream.
7. Fetch changed files again and verify the committed content before reporting success.

## 4. Product and engineering invariants to preserve

- Android-first, offline-first; the core must work without an account or network.
- Do not add AI processing, social/leaderboards, online sync, ads, purchases, or other scope by implication.
- Keep journal writing independent from habit completion. A note must never mark a habit complete.
- Preserve local-date keys and historical records; schedule/target edits must not silently rewrite past records.
- Numeric/duration entries are whole-number values; explicit zero differs from no record; multiple legitimate entries can be summed without retry double-counting.
- Tracking modes remain distinct: duration, quantity, and completion-only.
- Habit cards have separate **Complete** and **Record** actions. Complete uses the usual target for measured habits and records completion for completion-only; Record opens the detailed actual-value/correction flow. Completion-only cards show Complete without a measured-value Record shortcut.
- Rest/non-scheduled dates are neutral; paused periods are excluded; missed status is derived under the approved policy, not a manual moral judgement.
- Weekly goals are flexible across the configured week; preferred days are guidance, not restrictions. Daily and weekly progress must not be conflated.
- Historical target context is preserved; comparisons to current targets are separate.
- No shame, streak pressure, arbitrary composite score, or invented “growth” metric. Any displayed metric must be defined, inspectable, and truthful.
- Keep stable IDs, idempotency, safe transaction/rollback behaviour, and fail-closed migration. Never add conflicting restore copies together or discard them silently.
- [DESIGN.md](../DESIGN.md) is the single authoritative design-token/component reference. Proposed palette/font choices remain unapproved until review.

Use the Decision Log and current product/data contracts for exact semantics; this summary is a quick guardrail, not a replacement for them.

## 5. Current planning status as of 2026-10-28

### Completed as documentation/planning work
- The written schema proposal in docs 03/30 has been reconciled at the document level. It remains proposed and awaits owner review; no live database inspection or migration was performed.
- D-195 weekday-mask encoding is approved: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. Validation constraints and runtime behaviour are untested.
- D-196 delegates routine, reversible details such as guarded 8-second Undo, single-entry Delete+Undo, shared reminder coordination, local timing-learning safeguards, and retrospective journal-prompt cadence. These remain unimplemented/unverified.
- Two editable SVG design-comparison boards are committed and linked from doc 29: Today hierarchy variants (W1/W2/W3), and navigation/archive variants (N1/N2 and A1/A2/A3). They use synthetic data. They are planning artefacts—not approved UI, accessibility validation, or user-test evidence.
- The weekly reflection clock time is **still an owner choice**. Do not infer it from the separate per-habit 9:00 a.m. default. D-045/D-046 catch-up, deduplication and no-backlog behaviour remain the policy baseline.
- A candidate hybrid journal archive is a recommendation, not an approved choice.
- Initial-v1 monetisation is not approved. The recommended default for the owner review is to exclude ads, purchases and subscriptions from v1.
- The current owner response sheet is in [doc 31, section 4](31-OWNER-PLANNING-DECISION-BATCH.md). Its recommendations are not approvals.

### Not yet completed / still open
1. Owner review of the full proposed schema, portable-backup format/schema compatibility boundary, and restore-conflict choices where “Keep both” is unsafe.
2. Owner task/accessibility review of both SVG boards, followed by accept/modify/defer choices for Today hierarchy, navigation, archive, Growth Mark, typography/colour/theme and comeback presentation.
3. Owner choice of weekly reflection local clock time.
4. Final review of privacy/export/delete wording. No unverified claim about encryption, Android backup exclusion, or data never leaving the device may be presented as a runtime fact.
5. Owner choice on whether monetisation remains excluded from initial v1 (recommended: exclude).
6. Close remaining traceability and semantic-consistency gaps; rerun document link/heading checks if later edits affect paths or headings.
7. Present the consolidated planning-gate readiness check. Do not change the workflow gate to approved until the owner explicitly approves it.

See [doc 27](27-PLANNING-COMPLETENESS-REVIEW.md) for the live checklist. If this summary and doc 27 drift, reconcile them instead of maintaining two competing backlogs.

## 6. Immediate next steps

1. Read doc 31, section 4, and present its choices to the owner as **one grouped review**. Do not ask the owner to restate the decisions already captured there.
2. Capture the owner's Accept/Modify/Defer responses. For modifications, ask only the minimum clarification needed; batch any remaining consequential questions together.
3. Record actual owner choices in doc 09 and reconcile docs 01–04, 07, 11, 23–31 and any other affected file. Do not mark recommendations approved before receiving the response.
4. Review the SVGs using doc 29 and [Design Review Checklist](DESIGN-REVIEW-CHECKLIST.md). Check the approved interaction contract, representative small-screen and large-text layouts, tap-target size, readable labels, contrast/non-colour cues, empty/error/destructive states, and whether users can find/write/retrieve/edit/delete entries. A static SVG review is not a substitute for device accessibility testing.
5. Finish traceability and semantic-claim reconciliation. Check that the decision log, product spec, schema, privacy wording, UX contracts, handover and roadmap agree. Treat all planned acceptance cases as not executed.
6. Prepare a final gate-readiness summary. Ask for explicit planning-gate approval only after the prerequisites and owner choices have been reconciled.
7. After gate approval, wait for separate explicit authorisation of the next stage. Do not assume gate approval automatically authorises feature implementation, database migration, builds, tests, or device work.

## 7. GitHub / AI Studio working procedure

- GitHub `main` is canonical for plans and approved decisions. The active AI Studio/local working copy may be stale.
- Before every continuation, refresh the current GitHub tree and read the relevant latest files. Never rely only on chat summaries, stale local files, or old commit snippets.
- Before editing a file, fetch its current content and SHA. Apply a minimal, focused change against that exact revision; do not overwrite newer work.
- For each write, fetch the file again from `main` and compare the saved content with the intended content. Report the commit and exact paths only after verification.
- If an update fails, the SHA changes, or a write is ambiguous, stop and fetch the current state before retrying. Do not claim success from a tool response alone.
- Use relative Markdown links for repository docs and verify targets after adding/renaming files.
- Do not store API keys, tokens, passwords, private user data or secrets in the repository. Never inspect or print secret-bearing files unless specifically necessary and explicitly authorised.
- Do not create a parallel source of truth. Update the canonical file and add a short link/reference from the handover rather than copying whole policies into multiple docs.
- Keep commits small and descriptive. Separate documentation-only changes from any future code changes.

## 8. Handover format required at the end of each session

Update [Progress and Handover](12-PROGRESS-AND-HANDOVER.md) with:
- **What changed:** exact files/sections and commit(s).
- **What was verified:** re-fetch, link/heading check, static inspection, or actual execution—state which.
- **What did not happen:** explicitly say when no app code, schema, database, build, tests, APK, device validation, or implementation audit was touched/run.
- **Current blockers:** only genuine unresolved owner choices and planning gaps; do not resurrect settled decisions.
- **Next action:** one concrete next step with a canonical file/link.
- **Gate status:** state whether the planning gate is open/closed and whether any technical stage is authorised.

A good handover lets a new assistant continue without asking the owner to teach it the process again. When in doubt: preserve data, preserve privacy, respect recorded decisions, delegate reversible details, bundle consequential choices, and do not cross the gate without explicit authorisation.
