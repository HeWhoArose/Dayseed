# Dayseed Continuation Handover and Decision Protocol

**Last reconciled:** 2026-10-28  
**Repository / canonical source:** `HeWhoArose/Dayseed`, branch `main`  
**Current stage:** Planning and documentation only. The planning gate is **not approved**.  
**Purpose:** Let the next AI assistant or coding agent continue from the current agreed state without asking the owner to re-explain the product, decision rules, or working constraints.

> **First rule:** Read this file before proposing work. Then refresh the current GitHub `main` state and read the canonical documents listed below. This is a handover, not permission to start implementation.

## 1. Project in one minute

Dayseed is an Android-first, offline-first habit tracker and private micro-journal built around the promise **“Your progress is more than your streak.”** It should help people record what really happened, see their progress honestly, reflect when they want to, and return after interruptions without shame or penalty.

The approved technical direction is to continue the existing **native Kotlin + Jetpack Compose Android app**, with the repository's current SQLite-based persistence and app architecture. Do not create a website, a second app, or a React Native/Expo rewrite.

The repository already contains application source. That does **not** prove it builds or behaves correctly. Build/test/APK/device status and the actual migration path remain unverified. Current work is the completion of planning and documentation. Do not perform a source/runtime audit, inspect the live database, edit app code/schema, run builds/tests, migrate data, install an APK, or perform device validation until the owner explicitly approves the planning gate **and** separately authorises the technical-validation stage.

## 2. Read these sources in this order

Refresh them from the latest `main` branch before relying on this handover; if the live files conflict with this snapshot, reconcile the conflict instead of guessing.

1. **[Decision Log](09-DECISION-LOG.md)** — authoritative record of owner-approved decisions and their status. Do not infer approval from a proposal, a prototype, a previous assistant's recommendation, or a file existing in Git.
2. **[Decision Register and Reconciliation](25-DECISION-REGISTER-AND-RECONCILIATION.md)** — current operational interpretation of decisions and tracked cross-document discrepancies.
3. **[Product Specification](01-PRODUCT-SPECIFICATION.md)** and **[Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md)** — canonical product and interaction requirements.
4. **[Technical Architecture](03-TECHNICAL-ARCHITECTURE.md)** and **[Schema Closure and Migration Plan](30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md)** — proposed data model, integrity rules, backup/restore, transaction boundaries, and migration safety. The schema is **proposed, not owner-approved for implementation**.
5. **[Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md)** and **[Requirement Traceability](28-REQUIREMENT-TRACEABILITY.md)** — readiness, blockers, and planned acceptance coverage. Planned cases are not test results.
6. **[Owner Planning Decision Batch](31-OWNER-PLANNING-DECISION-BATCH.md)** — single grouped sheet for decisions that genuinely require the owner's input.
7. **[UX/Design Approval Pack](29-UX-DESIGN-APPROVAL-PACK.md)**, the [owner-provided multi-screen UI reference](prototypes/dayseed-owner-reference-layout.webp), and the three rendered SVG boards linked there — durable visual direction plus comparison options; none is final UI approval or evidence of usability/accessibility testing.
8. **[Progress and Handover](12-PROGRESS-AND-HANDOVER.md)** — chronological handover notes. Use its latest status and this file; don't treat old dated notes as current where they conflict with the decision log/readiness snapshot.
9. **[AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md)** — stage gates, environment discipline, and current approval record. The gate currently says **Not approved**.
10. **[Data and Privacy](04-DATA-AND-PRIVACY.md)**, **[Testing and Quality](07-TESTING-AND-QUALITY.md)**, **[Risks and Open Questions](11-RISKS-AND-OPEN-QUESTIONS.md)**, and root **[DESIGN.md](../DESIGN.md)** as needed. `DESIGN.md` is the sole token/component authority; recommendations elsewhere do not silently create design tokens.

The Today concepts document (`docs/21-TODAY-DASHBOARD-CONCEPTS.md`) is a cumulative research notebook with dated proposals. Its historical “next step” and “questions still needed” sections are not automatically the current backlog. Follow the current decision log, product/data contracts, decision batch, and latest readiness snapshot.

## 3. How decisions are made

The owner has explicitly asked for the next agent to **make safe routine decisions independently**, rather than interrupting them with a long sequence of micro-questions. Follow D-196 and these rules.

### Use exactly these status distinctions

- **Approved** — the owner explicitly approved it, and that approval is recorded in the Decision Log or a clearly traceable owner statement.
- **Delegated default** — the owner delegated a routine, reversible detail. Use the documented conservative default; it is not a new product-scope approval.
- **Proposed** — a recommendation awaiting owner approval.
- **Open** — a real decision, evidence gap, or policy question is unresolved.
- **Deferred** — intentionally excluded from the current scope, with a clear boundary and revisit trigger where appropriate.
- **Rejected / superseded** — considered and not selected, or replaced by a later decision. Preserve the historical reason where it matters.
- **Unverified** — source, build, tests, privacy behaviour, device behaviour, research, or another claim lacks the required evidence.
- **Conflict** — authoritative documents disagree. Do not silently pick the more convenient statement; reconcile and update every affected source.

Never convert **Proposed**, **Delegated default**, **Unverified**, or silence into **Approved**.

### What the agent may decide without asking

For a routine, low-risk, reversible detail, choose the simplest conservative default consistent with approved policy. Record the choice, rationale, relevant acceptance check, and where the default applies. Examples include ordinary spacing, wording polish that does not change policy, or a predictable interaction detail already covered by D-196.

Before selecting a default, check for an existing decision. Reuse the existing decision and encoding; do not create duplicate decision IDs or reopen settled choices. A default cannot add new data collection, online services, permissions, monetisation, irreversible deletion, or other new scope.

### What must be escalated to the owner

Do not settle these by assumption: product scope or philosophy; material privacy/security or data-retention choices; data-integrity, migration, backup/restore, or irrecoverable conflict choices; accessibility trade-offs; monetisation or ads/purchases; irreversible actions; a major product identity/design choice; or a conflict with an approved decision.

The default escalation format is **one grouped owner batch**, not one chat question per detail. Present concise recommendations with their consequences and an **Accept / Modify / Defer** choice. The existing owner response sheet in doc 31 is the current batch. If a safe deferral is possible, recommend it rather than blocking unrelated planning. Do not re-ask routine Undo, reminder-coordination, or retrospective-prompt cadence choices unless the owner wants to override their delegated defaults.

### How to record a decision

1. Confirm the owner's actual response. Do not treat “okay, continue” as approval of every consequential item unless its context unmistakably says so.
2. Update the corresponding entry in `docs/09-DECISION-LOG.md`, retaining the correct decision ID when one already exists. Add a new ID only for a genuinely new decision.
3. Reconcile all affected specifications, data/screen contracts, architecture/schema documents, test plans, UX material, risk register, readiness snapshot, and handover. Keep proposals visibly proposed.
4. Update `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` and `docs/28-REQUIREMENT-TRACEABILITY.md` where applicable.
5. Fetch the edited files again from GitHub and verify the committed contents. Record the commit and the remaining open questions. If documents still conflict, leave the item open and explain the mismatch.

## 4. GitHub working discipline

- GitHub `main` is the canonical shared source. Always refresh the branch/tree and relevant documents before starting a continuation; never assume a past chat summary or local workspace is newer.
- Before editing an existing file, fetch its current content and SHA. Apply the smallest coherent change, commit it, re-fetch it, and confirm the exact content is present. Do not overwrite a newer commit with an older snapshot.
- Keep planning/documentation changes separate from any later application-code work. Use clear commit messages that state the outcome.
- Preserve relative links and existing headings. After document/link changes, re-check relative links and heading fragments; check Markdown table consistency after table edits. Validate SVG/XML source after prototype edits.
- Update `docs/12-PROGRESS-AND-HANDOVER.md` with the current status, verified commit(s), work completed, next action, blockers, and explicitly what was **not** done. Update the current readiness snapshot rather than leaving contradictory “next action” lists.
- Never claim a file was changed, a commit exists, a test passed, a link was checked, a prototype was validated, or a runtime behaviour works unless that exact claim was verified.
- The root repository tree contains a `.env` file. Treat it as potentially sensitive: do not display, quote, copy, or include its contents in logs or documentation. Do not add credentials or journal/private user data to commits. Any suspected secret exposure requires a deliberate, secure owner-aware remediation rather than casually copying the file.

## 5. Current decisions and invariants that must not drift

These are high-value anchors; the full Decision Log and product contracts remain authoritative.

- **D-009:** continue the existing native Kotlin + Jetpack Compose Android project; no second app or stack rewrite.
- **Offline-first:** core habit, journal, history, insights, and comeback use must not require an account or network. Online account/cloud/sync/AI processing is out of initial v1 scope unless separately approved.
- **D-010 / O-11 monetisation:** on 2026-10-28 the owner said at least ads are needed for a full launch and requested an initial-v1 evaluation. See `docs/33-MONETISATION-AND-ADS-EVALUATION.md`. The recommended first candidate is a modest banner on a secondary screen; that proposal is not approval to ship an SDK or placement. Rewarded ads, purchases, subscriptions and Supporter Pack remain unapproved. Do not infer implementation authorisation from the evaluation request.
- **D-040 progress bands:** Tiny, Small, Halfway, Nearly there, Goal met, Extra, Huge use the approved target-relative thresholds; classify from the exact actual/target ratio before display rounding. Always show actual amounts and avoid shame/pressure.
- **Measured habits:** duration and quantity preserve non-negative whole-number actuals; explicit zero is distinct from no record. Multiple valid entries per habit/date are permitted and summed; each entry remains inspectable/editable. The dashboard **Complete** shortcut records the usual target for measured habits; **Record** opens detailed entry/correction. Completion-only habits use Complete. A note is not completion.
- **History/date policy (D-187/D-188/D-193/D-194):** saved local-date keys do not move with timezone travel; schedule/target changes preserve historical meaning; rest/non-scheduled/paused days are neutral or excluded as defined; missed status is derived only after a scheduled local day passes without qualifying activity; no manual mark-missed or habit-day Skip action in v1; journal text never changes habit status. Same-day Apply-today replacement is allowed only before qualifying activity exists that day; otherwise preserve today and offer Apply tomorrow.
- **Schedules (D-191/D-195):** only `DAILY`, `SELECTED_WEEKDAYS`, and `FLEXIBLE_WEEKLY_TARGET`; target period is `DAY` or `WEEK` as specified. Weekday mask is fixed and locale-independent: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. `SELECTED_WEEKDAYS` requires a valid non-empty mask; other schedule kinds do not require one. Flexible-weekly preferred days are optional guidance, never restrictions. No every-N-days schedule unless owner revises the decision.
- **Journal:** multiple saved entries/date; text-only v1; explicit save; edits preserve identity/creation time; optional title and moods; deletion is explicit/confirmed; journal entries remain independent of habit completion and habit deletion.
- **Reminders (D-041–D-046):** daily per-habit reminder defaults to 9:00 a.m. local time as specified. Quiet-hours/shared-cap/collision/deduplication/local timing-learning/partial-tip micro-details are delegated defaults under D-196. **Weekly reflection clock time is owner-selected at 9:00 a.m. device-local on the configured week-start day (O-9, 2026-10-28)**; this is distinct from the per-habit reminder. D-045/D-046 keep the week-start, catch-up, deduplication, and no-backlog behaviour. OS delivery remains unverified.
- **D-186 privacy/backup:** policy is to exclude sensitive data from Android platform Auto Backup/device transfer; Dayseed portable backup is a separate user-controlled file path. Actual manifest/data-extraction/network/export/deletion behaviour is unverified. Never promise “never leaves the device” or encryption without evidence.
- **D-197 migration safety:** delegated fail-closed default. If any legacy row cannot be mapped losslessly, roll back the entire migration and preserve the original DB/schema version; do not silently drop/coerce data or assume a quarantine table.
- **Schema and tests:** the proposed relational schema is reconciled in docs 03/30 but not owner-approved for implementation. Acceptance cases in docs 07/28 are plans, not results.
- **Comeback:** 14 complete local calendar days without an app opening; broader 30-day welcome-back trigger/precedence as recorded in docs 09/24. Do not infer why someone was away, shame them, require reflection, or change habits automatically.

## 6. Work completed in the current planning phase

- Decision process D-196 is recorded: delegate conservative, reversible routine details; escalate consequential choices; keep the implementation gate separate.
- Written model reconciliation across docs 03/30 and planned traceability in docs 07/28 covers effective-dated configuration, D-195 schedule/mask constraints, global activity idempotency handling, stable journal-draft save-intent IDs, restore-session/conflict lifecycle, field-level portable-backup mapping, cancellation outbox, and all-or-nothing fail-closed migration.
- Three editable SVG comparison boards are present in GitHub:
  - [Today W1/W2/W3](prototypes/dayseed-today-variants.svg) — action-first, reflective, and compact controls.
  - [Navigation/archive N1/N2/A1/A2/A3](prototypes/dayseed-navigation-archive-variants.svg) — four versus five destinations and timeline/hybrid/calendar-first archive.
  - [Growth Mark G1/G2/G3](prototypes/dayseed-growth-mark-variants.svg) — G1 active-date comparison, owner-selected G2 duration-total “Collected moments” summary, and G3 calendar/date-level details for inspecting duration records.
  - [Owner-provided UI reference layout](prototypes/dayseed-owner-reference-layout.webp) — five Today variants/states plus Habit Create/Edit, Habit Detail, History, Journal, Insights, Return, and Settings. Use it as a durable visual north star; exact UI, tokens, and navigation remain unapproved.
- Boards use synthetic data. They are planning artefacts, not approved UI, accessibility validation, or user-test evidence. A document-only task/edge-state review is now in doc 34; owner disposition of its recommendations and a later real UI/task/accessibility review remain required.
- Requirement matrix now covers AC-001 to AC-028. AC-009's deterministic Mood Insights fixture was reviewed; AC-026/027/028 add conditional ads, screen task/accessibility and habit-note coverage. The document-only review is in doc 34; all acceptance cases remain planned, not executed.
- Earlier Markdown relative-link, heading-fragment, table and external-URL checks were recorded for the then-current 37 Markdown files. The branch was checked across 41 Markdown files for relative targets, same-file heading fragments and pipe-table structure with no issues found; targeted rechecks cover follow-up changes to docs 21/30/12/32. The prior platform-resource external-URL audit remains latest for existing external references; the only new external URL is draft PR #1, verified directly through GitHub.
- The weekly reflection clock-time decision was reconciled: O-9 selected 9:00 a.m. device-local on the configured week-start day; OS notification delivery remains unverified.
- The owner batch (doc 31) records O-1–O-11 and the later O-6 clarification (D-199). Selected directions are W1/N1/A2 and modified Growth Mark G2/G3 roles: G2 shows the readable duration total from duration-tracked habits, defaulting to the current calendar month, while G3 supports date-level duration totals and underlying-session inspection. Formatting/period controls/accessibility remain planning work; weekly reflection is 09:00 local on configured week start; privacy wording is draft-only; the requested ads evaluation is in doc 33.
- The current handover intentionally does not claim application code has passed build/tests or that any proposed schema/design is implemented.

## 7. Exact next steps — use this order

### Step 1 — Refresh and reconcile current documentation

Fetch current `main`, check the recent commits and relevant decision/doc files. Confirm the three SVG paths exist, keep doc 27/doc 31/doc 12 aligned, and finish a semantic stale-claim pass. In particular, cumulative/history documents must not present old options (decimal numeric entry, one numeric total/day, every-N-days schedules, weekly reflection at a presumed time) as current requirements.

### Step 2 — Reconcile the recorded owner decisions

The owner responded to O-1–O-11 on 2026-10-28; the follow-up Growth Mark clarification is D-199 in doc 09 and is reconciled in doc 31. Do not ask the owner to repeat these decisions. W1/N1/A2 are selected planning directions, not usability evidence. The modified G2 “Collected moments” duration-total summary and G3 calendar/date-level or separate-page detail are selected planning roles; current-calendar-month is the compact default period. Formatting thresholds, alternative day/week/month period controls, compatible aggregation and accessibility remain open. O-1/O-2 accept the schema/restore package as a planning baseline only. O-9 sets weekly reflection to 09:00 device-local on configured week start. O-10 accepts privacy wording as draft only. O-11 requests an ads evaluation, not permission to ship ads. Continue task/accessibility and semantic review; the planning gate remains closed.

### Step 3 — Apply the owner's actual responses

The owner responses are recorded in doc 09 and doc 31, and the main affected planning documents have been reconciled. Re-fetch the latest files and fix only any remaining contradictions; retain exact colour/type tokens and broader visual identity as Open, and keep the ad-launch decision Proposed pending explicit owner choice after the doc 33 evaluation. O-1/O-2 resolve the written schema/restore-policy review only; implementation, live database inspection and migration remain unapproved. O-10 accepts privacy copy as a draft only. The planning gate stays closed until the remaining required review and readiness checklist are completed, followed by the owner's distinct explicit approval.

### Step 4 — Review the desk-review findings and close owner UX choices

The document-only task/edge-state review is in doc 34 and is mapped to AC-027. Present its expected outcomes to the owner and use the consolidated batch to accept/modify/defer Growth Mark defaults and accept/modify/defer the recommended visual synthesis/final-token decision. The previous written scorecard review does not replace actual user tasks, measured contrast, TalkBack or real UI accessibility checks; those must not be claimed as completed. Update DESIGN.md only for owner-approved tokens/components.

### Step 5 — Finish requirement and semantic consistency

Confirm every material v1 requirement has a planned acceptance case and honest status in doc 28; AC-026–AC-028 now cover conditional ads policy/failures, screen task/accessibility coverage and D-192 notes. Confirm schema failure/conflict/recovery edges have planned cases in docs 07/30. Re-run internal link/heading/table checks and semantic contradiction checks on the edited branch. Keep privacy wording as a planning draft unless runtime behaviour is verified later under proper authorisation. Never claim “build passed,” “fully offline verified,” “encrypted,” or “backup disabled” without evidence.

### Step 6 — Final planning readiness report; do not self-open the gate

Update docs 27, 28, 31, 12 and the workflow approval record only to reflect actual owner choices and verified planning status. Prepare a concise final readiness report listing accepted decisions, explicit deferrals, remaining blockers, and later technical validation scope. Ask the owner for a distinct explicit **planning-gate approval** only when the documented prerequisites are met. Never fill in the owner approval date/name or flip the gate to Approved on the owner's behalf.

### Step 7 — Keep later stages separate

After explicit planning-gate approval, obtain separate authorisation for bounded technical validation. Only then may the next agent inspect the real project/build/schema, run Gradle/tests, plan/test migration, verify platform backup/network/export/deletion/notifications, or install on a device. Feature implementation is a later stage after the feasibility/validation exit gate and the owner's implementation-plan approval.

## 8. Handover completion record

On each meaningful continuation append/update a short entry in `docs/12-PROGRESS-AND-HANDOVER.md` with:
- date and branch/commit SHA;
- planning-only or authorised stage;
- documents/artefacts changed and verification performed;
- tests or builds run (or explicitly “none”);
- decisions accepted/modified/deferred and exact source;
- remaining blocker and one next action.

A handover is successful when a new agent can start with this file, refresh GitHub, understand the decision protocol and current blockers, and continue without asking the owner to restate the project's history. It must not bypass owner authority or the planning gate.

### Latest continuation — owner decisions and ad evaluation (2026-10-28)

- Owner responses O-1–O-11 were recorded in docs 09/31; D-198 captures that batch and D-199 captures the follow-up Growth Mark clarification. Do not reopen settled choices without a material reason.
- O-1/O-2 accept the written schema and backup/restore/conflict rules as planning baselines only; no database implementation/migration is authorised. W1/N1/A2 are selected planning directions. O-6 now selects modified G2 “Collected moments” total duration with G3 calendar/date-level detail; the current month is G2’s default period. Exact thresholds, unit formatting, alternate periods and accessibility review remain open. Platform typography/system appearance and compact non-blocking comeback direction are accepted baselines; exact tokens remain open.
- O-9 sets the weekly reflection time to 09:00 a.m. device-local on the configured week-start day. O-10 accepts privacy wording only as a draft, not a verified runtime claim.
- O-11 requests evaluation of ads for the intended full launch. [Monetisation and Ads Evaluation](33-MONETISATION-AND-ADS-EVALUATION.md) recommends evaluating a secondary-screen banner first and documents official Play/AdMob policy checks. No SDK, placement, rewarded feature or purchase is approved.
- Planning gate remains **not approved**. Next: finish task/accessibility and semantic consistency review for W1/N1/A2 and G2/G3, review the ads evaluation and obtain the explicit include/defer decision, reconcile remaining stale claims/traceability, and prepare the readiness report. Technical validation and implementation remain separate authorised stages.


### Latest continuation — UX and traceability desk review (2026-10-28)

- Added docs/34-PLANNING-UX-AND-TRACEABILITY-CLOSURE.md: document-only review of W1/N1/A2/G2/G3 and Settings task flows, observable outcomes, edge/failure/offline states, and proposed Growth Mark formatting/period defaults. This is not live-app review, user testing, measured contrast, TalkBack validation, or acceptance execution.
- Added traceability cases AC-026 (conditional ads policy/failure), AC-027 (selected-screen task/accessibility flows), and AC-028 (D-192 habit/session notes); expanded docs/07 with future acceptance coverage and aligned docs/15/26/28/29/27/25/31 accordingly. Corrected AC-014's stale statement that the written backup/restore compatibility policy still awaited owner review: O-2 accepted it as a planning baseline, but implementation remains gated.
- Follow-up semantic sweep marked older dashboard notes (decimal actuals, optional numeric targets, one aggregate/day, arbitrary start dates and every-N-days) as superseded by current D-191/D-194/D-195 and docs 01/26, and clarified doc 30 that O-1/O-2 accepted the written schema/backup rules as planning baselines only—not implementation approval. The complete schema remains proposed; no database/schema change is authorised without the planning gate, separate technical-validation authorisation, and explicit schema/implementation approval.
- Further semantic reconciliation updated the current-authority paragraph and the old suggested sequence in doc 21, fixed the stale weekly-reflection-time row in doc 25, and aligned active reminder/Growth Mark/archive questions in doc 11. AC-025 in docs 07/28 now explicitly expects the six static-board session values to total 280 minutes / 4 hr 40 min. These are planning/documentation corrections only; the broad semantic contradiction sweep remains open.
- Static heuristic review of the three SVG boards is complete as a document-only task. It identified and corrected the Growth Mark synthetic total mismatch (the six date values total 4 hr 40 min); Today board labels now say `0 min recorded · target 20 min` and count records rather than implying completed goals; A2/A3 boards now show a visible saved-entry/neutral-blank-date legend. Doc 34 records remaining G3 month/year/session-detail and accessibility risks. The revised SVGs and exact 280-minute expected sum are reflected in the planned AC-025 checks; this is not user testing or real assistive-technology evidence.
- Branch: planning/ux-traceability-closure-2026-10-28. [Draft PR #1](https://github.com/HeWhoArose/Dayseed/pull/1) is open against main and is documentation-only. Initial doc-creation commit: acbbd58de8479b139aa6f56cc4733db88680b940. Current-branch links/fragments/table structure were checked across all 41 Markdown documents with no issues found; the final semantic status/claim sweep remains open. Next: finish that sweep and obtain owner dispositions; do not merge or open the planning gate before owner review.
- Owner choices still pending: include or defer the single banner candidate; accept/modify/defer Growth Mark formatting defaults; accept/modify/defer the recommended visual synthesis or explicitly defer final tokens. No new decision ID or owner approval was created. Planning gate remains not approved; no app code/schema/database/build/test/migration/device validation was performed.
