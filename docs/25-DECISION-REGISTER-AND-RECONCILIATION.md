# Decision Register and Planning Reconciliation

**Status:** Working reconciliation register — planning only; implementation remains not authorised.
**Last reconciled:** 2026-10-27
**Canonical source:** GitHub `main`; the decision log records owner choices and this register makes their current operational status easy to review.

## Purpose and decision rules

This document reduces repeated micro-decisions while protecting owner control over product direction.

- **Approved** — explicitly recorded as approved in the decision log or otherwise clearly accepted by the owner.
- **Delegated default** — an implementation/detail choice delegated to the assistant. Use the documented conservative default unless evidence or an approved decision conflicts; record the rationale and tests.
- **Proposed** — a recommendation that still needs owner approval.
- **Unverified** — repository evidence, research, tests, or cross-document consistency has not yet established the claim.
- **Conflict** — two authoritative documents or decisions currently point in different directions. Do not silently choose between them.

The decision log (`09-DECISION-LOG.md`) remains the detailed record of owner choices. This register is an index and reconciliation aid, not a replacement. Later explicit owner-approved decisions can supersede earlier ones, but the affected documents must be updated together. A file's existence is not proof that its behaviour works.

## Confirmed product direction

| Area | Current status | Operational interpretation |
|---|---|---|
| Product promise | Approved direction | “Your progress is more than your streak.” Preserve progress and make returning ordinary; avoid shame and perfection pressure. |
| Android-first, offline-first | Approved direction for offline core; Android-first target | Core habit tracking, journaling, history, insights, and comeback flows must work without network. The approved stack direction is native Kotlin + Jetpack Compose; build/test/device readiness remains unverified. |
| Personal data and privacy | Approved direction, details open | Initial v1 requires no account or network for core use and includes readable journal export plus user-controlled local backup/restore. Online backup/cross-device sync are premium future capabilities, not initial-v1 scope. Do not claim “never leaves the device” until platform backup, export, SDKs, and data flows are verified. |
| Habit tracking | Approved | Keep numeric, completion-only, and effort-aware modes semantically distinct. Numeric entries are whole numbers; explicit zero differs from no record; multiple legitimate entries can be summed without retry double-counting. |
| Daily vs weekly targets | Approved | Daily goals apply to selected days; flexible weekly goals accumulate across the configured week and do not judge inactive days as daily misses. Week-start is user-selectable. |
| Historical data and date policy (D-187) | Approved baseline; technical verification pending | Preserve local-date keys across timezone changes; handle manual clock uncertainty conservatively; require explicit backdating; use the saved week-start consistently; apply schedule/target changes prospectively; archive habits by default while preserving accessible history; keep permanent deletion separate and explicit. |
| Missed-day status and late activity (D-188) | Approved policy; technical verification pending | Do not offer manual mark-as-missed. Derive missed status after a scheduled day passes without qualifying activity; genuine explicitly backdated activity recalculates that date; journal memos alone never change habit status. Rest/non-scheduled and paused days remain neutral/excluded. |
| Journaling | Approved | Journal writing is independent of habit completion; text-only in v1; multiple entries per date allowed; titles and mood tags are optional. Save/edit/delete and draft behaviour follow the latest decision log. |
| Progress language | Approved | Actual amount is preserved; partial, goal-met, above-target, and Tiny-to-Huge labels are factual progress states, not moral judgements. |
| Comeback experiences | Approved direction with detailed rules | Two-week and one-month triggers have defined precedence and suppression rules. Do not infer why the user was away. See `24-COMEBACK-EXPERIENCE.md`. |
| Weekly reminders | Trigger/count rules approved; weekly reflection time needs separate confirmation | Week-start reflection timing is a proposal until separately confirmed; do not infer 9:00 a.m. from the separate per-habit default. The two-full-days inactivity trigger, three-per-week cap, and stop-on-target rule remain recorded. See `23-WEEKLY-GOAL-REMINDERS.md`. |
| Monetisation (D-010) | Owner preference recorded; v1 release decision open | Preferred candidate to evaluate: restrained secondary-screen banners, optional user-initiated rewarded extras, and a possible one-time Supporter Pack. This is not approval for an ad SDK, purchase flow, or monetisation in initial v1. Keep core features free and usable offline; settle through policy/privacy review and a clear owner choice. |
| AI analysis, social features, leaderboards | Deferred unless explicitly re-approved | Do not add AI processing, accounts, public profiles, or leaderboards implicitly. |
| Visual identity and navigation | Unverified / proposed | Design exploration and mockups are not final approval. `DESIGN.md` should remain the authoritative design-token/component reference. |

## Proposed data model — owner approval pending

| Area | Status | Current proposal / next action |
|---|---|---|
| Exact v1 SQLite schema | Proposed; partial owner decisions recorded; not approved for implementation | Candidate tables and invariants are documented in `03-TECHNICAL-ARCHITECTURE.md`; proposed constraints, transaction boundaries, backup mapping, and migration/rollback requirements are expanded in `30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`. Proposed lifecycle-state coverage, nullable journal-habit links, restore sessions/conflicts, and notification-cancellation-outbox structures remain proposed, not owner-approved. Owner approved effective-dated habit configuration snapshots and separate numeric/duration entries from single-per-day completion-only/effort-aware outcomes (D-189). Domain validation must enforce effective tracking mode and prevent contradictory states. D-190 approves stable UUID identity and nullable idempotency keys unique when non-null; retries reuse the same key and distinct intentional entries use new keys. Because numeric entries and non-numeric outcomes use separate tables, docs 03/30 propose table-local partial unique indexes plus cross-table insert/update guards and a domain-transaction check to enforce the intended global activity-submission key scope; this proposal is not runtime-verified. D-191 approves explicit schedule kind and target period: `DAILY` means every local calendar day with `DAY`; `SELECTED_WEEKDAYS` requires a non-empty weekday mask with `DAY`; `FLEXIBLE_WEEKLY_TARGET` uses `WEEK` with optional preferred weekdays as guidance only. D-192 resolves note cardinality: allow both optional per-entry notes and one optional habit/date note. D-195 approves the locale-independent 7-bit weekday-mask encoding: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64; selected days combine by bitwise OR. `SELECTED_WEEKDAYS` requires a valid non-empty mask, while `DAILY` and `FLEXIBLE_WEEKLY_TARGET` do not require a scheduled-day mask; flexible weekly preferred days remain optional guidance. Technical validation remains gated. D-194 resolves repeated Apply-today behavior: replace today's version only before qualifying activity is recorded today; after activity, offer Apply tomorrow; Cancel makes no change. Permanent-deletion handling has a delegated planning default described in docs 03/30: queue opaque notification IDs, transactionally delete habit-owned database rows while preserving independent journal records and global reminders, then cancel OS notifications outside SQLite's transaction and clear outbox IDs only after success. Retries and active-habit guards prevent stale work. This remains unimplemented/unverified and may be overridden before complete schema approval. D-193 resolves the skipped-state question: v1 has no explicit habit-day Skip action or persisted skipped outcome. D-188 resolves explicit-miss replacement: missed is derived, not user-entered. |
| Transaction/index/cascade design | Proposed; technically unverified | Review constraints and transaction boundaries against the existing version-1 schema after the planning decision; no migration or implementation is authorised. |
| Data-model acceptance tests | Planned, not executed | `07-TESTING-AND-QUALITY.md` records required cases; these are not test results. |

## Delegated defaults

These are conservative defaults for routine details under D-196. Use them rather than asking the owner about each micro-decision. They do not authorise new product scope, collection/sharing of personal data, or irreversible behaviour; the owner may override them before the planning gate.

| Topic | Delegated default | Guardrail / verification |
|---|---|---|
| Input validation | Reject invalid values with a clear inline explanation; preserve existing saved data. | Test empty, whitespace, boundary, invalid, and repeated submissions. |
| Persistence | Treat a save as successful only after local persistence confirms success; keep drafts/data intact when a save fails or outcome is uncertain. | Test restart, process death, failed writes, and retry idempotency. |
| Duplicate actions | Make retries of the same logical action idempotent; preserve distinct intentional entries. | Never deduplicate legitimate separate numeric entries merely because values/timestamps look similar. |
| Dates and time | Use the user's current local calendar for habit-day/week labels; store instants separately from date keys; centralise calculations. | Travel, timezone changes, clock changes, week boundaries, and daylight-saving tests where relevant. Do not invent precision if device time is unreliable. |
| Reminder delivery | Persist delivery keys, deduplicate after restarts/rescheduling, respect opt-in, OS permission, quiet hours, and shared limits; never promise exact Android delivery time. | If notification permission is denied, core app functionality remains usable. Do not send notifications when global opt-in is No. |
| Missed scheduled work | On next suitable opportunity, recover only the latest still-relevant item; do not flood users with stale backlogs. | Follow specific approved catch-up decisions such as D-045/D-046; do not generalise in ways that override them. |
| Empty/loading/error states | Provide calm, actionable, accessible states; distinguish no data from failure and explicit zero. | Never fabricate sample user records or report a failed action as successful. |
| Accessibility | Text labels alongside colour, TalkBack names, logical focus, scalable text, adequate contrast and touch targets, reduced motion. | Verify every screen/theme and small-screen layout. |
| Testing | Add focused unit tests for pure rules, persistence tests for data changes, and acceptance scenarios for user-visible flows. | A test passing does not prove physical-device behaviour; report exact evidence and limitations. |
| Copy | Warm, direct, adult, nonjudgmental, concise; no guilt, fake intimacy, diagnosis, or assumed reasons. | Consequential wording and major brand voice changes remain owner-reviewable. |
| Technical organisation | Prefer the smallest maintainable change consistent with the verified architecture; keep domain rules testable and avoid unnecessary dependencies. | Do not switch frameworks, add cloud services, or introduce SDKs without the required gate. |
| Backup/restore safety | Validate a backup fully before touching live data; create and verify a safety backup before Replace/Merge; stop if safety backup fails; preserve source backup files. | These behaviours are owner-approved; exact format/merge semantics remain governed by D-091 onward. |

D-196 records the planning process: default routine, reversible details conservatively and explicitly; escalate material product, privacy, safety, data-integrity, accessibility, monetisation, and owner-taste decisions. The decision log remains the authority, and this rule does not authorise implementation.

## Decisions requiring owner approval

These are not micro-decisions and must not be silently delegated:

1. **Build readiness (D-015):** D-009 is resolved: the owner approved continuing the existing native Kotlin + Jetpack Compose project. The observed code is not a verified build. Keep the active plan native; build, tests, APK installation, and device behaviour remain unverified and must wait for explicit planning-gate/technical-validation authorisation.
2. **Privacy and network scope (D-004, D-011, D-091):** reconciled. Initial v1 includes readable journal export and local backup/restore, with no accounts, online backup, or cross-device sync. Premium online backup/sync is future scope requiring a separate explicit privacy/security, consent, data-flow, recovery/conflict, retention/deletion, and monetisation gate.
3. **Monetisation (D-010, D-014, D-091):** ads/rewarded extras/supporter pack and a subscription model are not approved for initial v1. Premium online backup/sync is a future product direction only; no pricing, purchase provider, or monetisation mechanism is approved.
4. **Visual identity and navigation (D-008, D-016–D-021):** final visual direction, Growth Mark definition, archive navigation, typography/colour and competitor findings remain to be validated. Do not treat concepts as approved UI.
5. **XP/rank and social comparison (D-033):** private rank/XP is exploratory; online leaderboards remain deferred and require separate explicit approval.
6. **Data and privacy claims:** encryption-at-rest, verification that the D-186 Android platform-backup exclusion is effective, analytics/crash reporting, and exact deletion/export wording must match inspected configuration and evidence.
7. **Release scope and planning gate:** implementation cannot begin until the owner explicitly approves the planning gate in `19-AI-STUDIO-GITHUB-WORKFLOW.md`.

## Reconciliation findings

| ID | Finding | Status | Required action |
|---|---|---|---|
| R-001 | Active workflow and architecture instructions previously conflicted with the native project by preserving an assumed Expo path. | Corrected; D-009 approved | D-009 now explicitly approves continuing native Kotlin + Jetpack Compose. Architecture/workflow docs now align; build and test status remain unverified. |
| R-002 | `14-STACK-COMPARISON.md` previously linked to the wrong implementation-reality-audit filename. | Corrected | Link now points to `22-IMPLEMENTATION-REALITY-AUDIT.md`; historical stack research remains available. |
| R-003 | `09-DECISION-LOG.md` had inconsistent column semantics in D-083–D-090 and D-106–D-185, plus a duplicate D-090 identifier. | Corrected in documentation | Normalised D-083–D-090 and D-106–D-185 to the standard columns, preserving decisions, rationale/acceptance criteria, and dates; removed the duplicate D-090 without renumbering later IDs. A later structure pass removed blank-line breaks between adjacent decision rows and added a proper header/separator for the D-191–D-196 decision block, so all 190 decision rows now render within two valid five-column Markdown tables. Re-fetch verification found no malformed decision-row widths. |
| R-004 | Earlier product/privacy documents excluded cloud sync from v1 while D-091 described premium online backup and cross-device sync. | Reconciled in planning docs | Initial v1 includes readable journal export and local backup/restore only; online backup/sync are premium future capabilities and remain outside initial v1. Reopen only through a separate explicit approval gate. |
| R-005 | Weekly reflection time was treated as approved despite lacking a separate confirmation from the per-habit reminder default. | Reconciled; weekly time confirmation pending | Keep the approved weekly reflection trigger, catch-up, and deduplication rules. Treat the weekly delivery time as proposed until the owner separately confirms it; do not infer 9:00 a.m. from the distinct per-habit reminder default. Updated docs 09/23/27/31 and added this item to the consolidated owner review. |
| R-006 | Repository files and past notes mention source and schema, but there is no current build/test/device evidence in this document. | Unverified | Do not claim the app builds, installs, or passes tests until authorised verification produces evidence. |
| R-007 | Android platform backup, Dayseed portable local backup, and provider-backed file syncing could be mistaken for one another. | Owner decision resolved; technical verification pending | D-186 approves excluding sensitive Dayseed app data from Android platform Auto Backup/device transfer by default. Keep this distinct from Dayseed's user-controlled portable backup and opt-in file-based Auto Backup. The system file picker may offer cloud-backed providers; disclose that a selected provider may upload/sync a file. Verify the manifest/data-extraction rules and supported OS/device-transfer paths before claiming exclusion is effective. |
| R-008 | The architecture proposal incorrectly imposed one record per habit/date and left multiple journal entries per date open, conflicting with approved decisions. | Corrected in documentation | Updated `03-TECHNICAL-ARCHITECTURE.md` to model multiple stable-ID numeric/duration entries per date, explicit zero/no-record distinctions, and multiple independent journal entries. Exact schema, idempotency implementation, date policy, and migration strategy remain open. |
| R-009 | The testing strategy had out-of-order regression numbering and did not explicitly enumerate several approved multi-entry and backup edge cases. | Corrected in documentation | Reordered and expanded the regression list in `07-TESTING-AND-QUALITY.md`; this is a plan only, not evidence that tests have run or passed. |
| R-010 | `01-PRODUCT-SPECIFICATION.md` reused the H4 requirement identifier for two different sections and described duplicate-entry semantics as wholly undecided. | Corrected in documentation | Renamed the second section H4b and clarified that retry idempotency and legitimate repeated entries are approved, while the exact undo window/recovery interaction remains open. No requirement content was discarded. |
| R-011 | Date/time policy was scattered across architecture, comeback, weekly reminders, and product requirements, leaving cross-screen interpretation vulnerable to drift. | Baseline drafted; review required | Added a shared provisional date/time contract to `03-TECHNICAL-ARCHITECTURE.md`, preserving specific approved comeback thresholds and keeping local date keys separate from instants. Cross-document review and boundary acceptance tests remain required. |
| R-012 | Cross-screen data behaviours and failure states were distributed across several documents without one navigable contract. | Contract drafted; not approved for implementation | Added `26-SCREEN-AND-DATA-CONTRACTS.md` covering Today, record editing, journal, history, insights, comeback, reminders, backup/restore, privacy, state handling, and unresolved gates. It consolidates requirements; it is not proof of implementation or test results. |
| R-013 | The journal/archive research document still said portable backup coverage and restore conflict/replacement behaviour were undecided, despite approved D-091–D-145 decisions. | Corrected in documentation | Updated `24-COMEBACK-EXPERIENCE.md` to reference the approved portable-backup and restore contract and distinguish it from the still-open Android platform-backup inclusion decision. Re-fetch verification confirms the stale claim is gone. |
| R-014 | Date/time and habit lifecycle rules were documented as open even though a shared provisional contract and history decisions existed. | Owner policy approved by D-187; technical verification pending | Promote the shared date/history rules to an approved v1 planning baseline. Update architecture, privacy, screen/data contracts, product specification, test plan, roadmap, and handover. Keep schema, migration, reminder/time-boundary testing, and deletion implementation unverified until the authorised technical-validation gate. |
| R-015 | Schema cross-read found schedule/target-period wording drift, incomplete interrupted-restore lifecycle rules, malformed index notation, and idempotency-key uniqueness across separate activity tables not enforced by table-local indexes alone. | Partially reconciled in docs 03/30/07/28; full schema review still pending | Align `DAILY`/`SELECTED_WEEKDAYS` target-period language and use “local calendar day” consistently. D-191/D-194/D-195 resolve schedule fields, repeated Apply-today behavior, and mask encoding respectively; D-193 resolves no explicit habit-day Skip action/outcome. Docs 03/30 now propose (a) a durable `IN_PROGRESS` restore-session row before the live transaction, (b) atomic commit of restored data, conflict rows and final session state, (c) `FAILED_SAFE` after an observed rollback and `RECOVERY_REQUIRED` after verifying a process-interrupted transaction did not commit, (d) a cross-table idempotency-key guard plus domain validation, and (e) explicit indexes/outbox uniqueness. Planned recovery and collision cases were added to docs 07/28. These remain proposed schema details, not verified behaviour; no implementation or technical validation is authorised. |
| R-016 | Portable backup was category-only, while field/relationship mapping was implicit and unresolved conflicts referenced a restore-session parent not represented in the portable mapping. | Draft reconciled; owner review pending | Added a field-level map in `30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md` and aligned `03-TECHNICAL-ARCHITECTURE.md`, `07-TESTING-AND-QUALITY.md`, `28-REQUIREMENT-TRACEABILITY.md`, `27-PLANNING-COMPLETENESS-REVIEW.md`, and `31-OWNER-PLANNING-DECISION-BATCH.md`. The proposal specifies stable IDs/local-date keys/idempotency, format/schema integrity/count validation, unresolved-conflict snapshots plus fresh local parent-session remapping, explicit restore-session state transitions, and conflict rows that exist only while unresolved (no redundant conflict status column). It also defines constrained “Keep both” handling and exclusion of device-specific notification/outbox state, provider grants, caches and secrets. Acceptance coverage is planned only. Owner review of the compatibility boundary and unsafe conflict choices remains; no live database/code/tests are authorised. |

## Documentation authority and update order

1. `09-DECISION-LOG.md` — authoritative owner decisions and their statuses.
2. `01-PRODUCT-SPECIFICATION.md` and `00-PROJECT-CHARTER.md` — product scope and promises.
3. Feature specifications, including `23-WEEKLY-GOAL-REMINDERS.md` and `24-COMEBACK-EXPERIENCE.md` — detailed behaviour.
4. `02-UX-AND-DESIGN.md`, research docs, and root `DESIGN.md` — experience evidence and design authority.
5. `03-TECHNICAL-ARCHITECTURE.md`, `04-DATA-AND-PRIVACY.md`, `07-TESTING-AND-QUALITY.md`, and `08-IMPLEMENTATION-ROADMAP.md` — technical/privacy/test/sequence implications.
6. `19-AI-STUDIO-GITHUB-WORKFLOW.md` — workflow and explicit implementation gate.
7. `12-PROGRESS-AND-HANDOVER.md` — factual status, verified commits, tests and next task.
8. `27-PLANNING-COMPLETENESS-REVIEW.md` — current readiness snapshot and planning/post-gate checklist; it does not supersede the decision log.
9. `28-REQUIREMENT-TRACEABILITY.md` — initial requirement-to-screen/data-rule/test mapping; its gaps still need closure and its cases are not test results.
10. `29-UX-DESIGN-APPROVAL-PACK.md` — proposed design/navigation recommendations and owner-review topics; prototypes and owner approval remain outstanding.
11. `30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md` — proposed constraints, transaction, restore, backup mapping and migration requirements; not implementation approval.
12. `31-OWNER-PLANNING-DECISION-BATCH.md` — consolidated settled policies, proposed defaults, owner accept/modify/defer items, and gate boundary; review aid only, not an approval or implementation gate by itself.
When a decision changes, update the decision log and every affected document in one traceable reconciliation batch. Fetch each current file immediately before writing it, preserve unrelated content, then re-fetch and verify the result.

## Planning gate checklist

### Completed in planning documentation

- [x] Resolve R-001 / D-009: the active stack is the existing native Kotlin + Jetpack Compose project; earlier Expo recommendations are historical only.
- [x] Resolve R-004: initial v1 remains account-free/offline-first for core use, with readable journal export and user-controlled portable backup/restore; online sync is future scope behind a separate gate.
- [x] Align core tracking and journal semantics with the architecture and planned acceptance cases: multiple legitimate numeric entries, explicit zero versus no record, independent journal entries, stable IDs/idempotency, effective-dated schedules, no explicit habit-day Skip, and derived missed status.
- [x] Record and reconcile D-186–D-196, including Android platform-backup policy, date/history baseline, structured schedule fields and D-195 weekday-mask encoding. D-196 sets the delegated-default decision process; it does not approve implementation.
- [x] Add cross-screen data contracts and planned tests; document a delegated default for permanent-habit-deletion notification cleanup using an opaque cancellation outbox.
- [x] Correct the known broken stack-comparison link and normalise decision-log rows; remove duplicate D-090 without renumbering later decisions.
- [x] Add the current [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) so completed decisions, open planning work, and post-gate validation are separated.

### Still required before the owner can approve the planning gate

- [x] Expand the written schema/transaction/migration proposal in docs 03 and 30, including proposed restore-conflict and notification-cancellation-outbox structures.
- [ ] Reconcile remaining proposed-model gaps, resolve undo/recovery policy, and present the complete schema to the owner. Do not change or migrate the database in this step.
- [x] Document delegated reminder safety defaults across docs 23/05/26/07: quiet hours, shared daily cap, collision/late-delivery suppression, local-only timing-learning thresholds/rollback, and optional-tip cadence.
- [ ] Have the owner accept or explicitly defer the shared reminder defaults and the D-042 retrospective journal-prompt proposal in docs 24/26/07 (AC-024); no-backlog, once-per-date and comeback-priority rules remain proposed.
- [x] Draft the bounded Undo/recovery proposal in docs 01/03/26/30 and trace it as AC-023: single-mutation, eight-second Undo; guarded inverse; destructive deletion and Replace/Merge are not snackbar-undoable.
- [ ] Owner must accept or explicitly defer the Undo proposal before Gate A; no implementation/test has run.
- [x] Prepare the UX/design recommendation pack in `29-UX-DESIGN-APPROVAL-PACK.md`.
- [x] Draft comparable text wireframes for Today hierarchy, navigation and archive alternatives in doc 29; rendered visual prototypes, task review and owner approval remain outstanding.
- [ ] Review the new text wireframes and prepare rendered comparable navigation/screen-flow prototypes; then record owner choices for Growth Mark, archive, visual direction, typography/colour/theme, and accessibility.
- [x] Create the initial requirement-to-screen/data-rule/test matrix in [28-REQUIREMENT-TRACEABILITY.md](28-REQUIREMENT-TRACEABILITY.md).
- [ ] Close the remaining traceability gaps listed in doc 28; its acceptance cases are plans, not test results.
- [x] Re-ran the relative Markdown file-target check across all 37 current Markdown documents; no missing target files found.
- [x] Checked same-file Markdown heading fragments across all 37 documents; none were missing under the reviewed slug rules.
- [ ] Cross-file heading fragments, external URLs, stale claims and semantic contradictions remain for the final content-level pass.
- [ ] Reconcile stale statements, heading anchors, and evidence claims across current documents and handover; keep dated research snapshots labelled historical.
- [x] Consolidate settled decisions and owner accept/modify/defer items in `31-OWNER-PLANNING-DECISION-BATCH.md`.
- [ ] Walk through the batch, update the decision log and every affected document, then record explicit owner approval of the planning gate in the workflow approval record only if the owner chooses to approve it.

### Post-gate technical validation — not a planning task and not currently authorised

- [ ] Inspect current Gradle/toolchain/source and the actual version-1 SQLite schema; run build/test validation only after separate owner authorisation.
- [ ] Verify migration, offline persistence, notification lifecycle, export/delete/restore, and the D-186 Android backup/data-extraction exclusion using synthetic data.
- [ ] Install and verify the native app on a physical Android device; record exact versions, commands, results and limitations.

**Boundary:** This register records planning state only. It does not authorise code changes, an implementation audit, technical validation, migration, or a release-readiness claim.
