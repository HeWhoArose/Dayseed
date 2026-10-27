# Planning Completeness Review

**Review date:** 2026-10-27  
**Status:** Planning reconciliation in progress. The planning gate is **not approved**. This document is a readiness snapshot, not authorisation to change application code, run an implementation audit, build, migrate data, or validate on a device.

**Canonical sources:** GitHub main; owner choices in the [Decision Log](09-DECISION-LOG.md); current-state index in the [Decision Register](25-DECISION-REGISTER-AND-RECONCILIATION.md); navigation/data expectations in [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md); technical proposal in [Technical Architecture](03-TECHNICAL-ARCHITECTURE.md); test intentions in [Testing and Quality](07-TESTING-AND-QUALITY.md).

## Executive readout

Dayseed is materially beyond an initial brainstorm. Core habit-record semantics, journal cardinality and save behaviour, comeback rules, portable backup/restore policy, Android platform-backup policy, local-date/history policy, schedule vocabulary, effective-dated edits, note cardinality, stable IDs, retry idempotency, and weekday-mask encoding have been recorded. Decisions D-186–D-195 are reflected in the key planning contracts, including the D-195 locale-independent weekday mask.

The planning gate is **not yet ready to approve**. The remaining work is mainly: (1) close the exact schema as a written proposal and document recovery/deletion transaction rules; (2) resolve or deliberately defer the remaining reminder, prompt, and undo edge cases; (3) review and approve the primary navigation and visual direction; (4) finish requirement-to-test traceability and the repository-wide contradiction/link review; and (5) present a concise owner approval batch.

**Important implementation-status distinction:** the repository already contains native Kotlin/Jetpack Compose source and SQLite persistence according to the existing read-only audit. That is not the same as proven runtime readiness: Gradle build, automated tests, APK installation, physical-device behaviour, and safe migration remain unverified. This planning continuation has changed documentation only; no application code, schema, build, tests, APK, or device state was changed or validated.

## Status by workstream

| Workstream | Current state | What remains before planning-gate approval |
|---|---|---|
| Product scope and core habit semantics | Mostly settled and recorded | Final cross-document pass; make every remaining open item an explicit choice, delegated default, or safe deferral |
| Journal data rules | Most data semantics approved: multiple entries/date, explicit save, identity-preserving edits, confirmed delete, date changes do not move habit data, whitespace-only entries rejected | Finalise archive/navigation choice and remaining retrospective-prompt eligibility/frequency; keep any unapproved search/media out of v1 |
| Date/time and historical meaning | D-187 baseline approved; D-188 and D-193 define derived missed status and no habit-day Skip action; D-194 defines repeated same-day configuration edits | Complete the written edge-case contract and trace tests; actual clock/timezone and migration behaviour require later technical evidence |
| Habit schedule encoding | D-191 schedule fields approved; D-195 fixed Monday-first 7-bit weekday mask approved | Confirm the complete schema’s constraints and acceptance examples; no reopening of the approved mapping without a new owner decision |
| Relational data model | Partial owner decisions are approved; table proposal and closure/migration addendum now exist, but full schema remains proposed | Reconcile doc 30 refinements into the canonical architecture, close remaining constraints/transaction and backup mapping details, decide undo/recovery, and obtain owner review before any database change |
| Reminder and notification lifecycle | Core opt-in, weekly trigger/count rules, default 9 a.m. habit time and 14-day/28-day personalisation direction recorded; delegated safety proposal now written | Owner must accept or explicitly defer quiet hours, shared cap/priority, collision handling, late delivery, local-only learning thresholds/rollback and optional-tip cadence; retrospective journal prompt eligibility/frequency also needs a decision or deferral. OS delivery remains unverified |
| Privacy, export, backup, deletion | Initial v1 is account-free/offline-first for core use; portable backup/restore decisions recorded; D-186 excludes sensitive data from Android platform backup by policy | Final user-facing wording and exact deletion/export/failure states; technical verification of manifest/data-extraction/device-transfer behaviour belongs after authorisation |
| UX and visual system | Screen/data-flow concepts and research exist; current recommendation package is in `29-UX-DESIGN-APPROVAL-PACK.md`; visual/navigation decisions remain unapproved | Use the approval pack to prepare comparable wireframes/prototypes and review navigation, Growth Mark, archive pattern, visual direction, type/colour/theme and accessibility; then record owner decisions or safe deferrals |
| Quality and acceptance | Many cases are already listed in the quality plan, including D-195 mask cases | Initial matrix exists in `28-REQUIREMENT-TRACEABILITY.md`; close missing boundary/failure cases and final test detail. Acceptance cases are planned, not executed |
| Research and documentation | Several research docs and a historical implementation-reality audit exist; competitor audit is public-listing/screenshot evidence, not hands-on testing | Relative Markdown target check across 34 documents: no missing targets. Still check heading anchors, stale claims and semantic contradictions; distinguish historical recommendations from approved choices |
| Native app technical readiness | Native source exists; build/test/APK/device state unverified | Post-gate only: inspect the current checkout/build files, confirm toolchain, build/test, install on device, inspect real schema and migration, and verify notification/backup/export/delete behaviour |
| Governance and handoff | Owner decision log and workflow gate exist | Update the handover/roadmap to current facts, review all open items in one batch, and wait for explicit owner approval. Documentation alone never opens the gate |

## Decisions and defaults already recorded

The following are considered current planning baselines, not fresh open questions:

- **D-186:** exclude sensitive Dayseed data from Android platform Auto Backup/device transfer by default; portable Dayseed backup remains a distinct user-controlled path.
- **D-187:** preserve saved local-date keys across timezone travel, separate date keys from instants, use explicit dates for backdating, apply settings prospectively, archive by default and make permanent deletion separate.
- **D-188 / D-193:** derive missed status only after a scheduled local day passes without qualifying activity; no user-entered miss and no explicit habit-day Skip action/state in v1; journal memos cannot affect habit status.
- **D-189–D-192:** separate numeric/duration activity from non-numeric outcomes; stable UUIDs and retry idempotency; structured schedule fields; both entry-level and habit/date notes.
- **D-194:** a repeated Apply-today edit may replace today’s version only before qualifying activity exists; afterward preserve today and offer Apply tomorrow.
- **D-195:** Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. Use a fixed 7-bit mask; SELECTED_WEEKDAYS must have a valid non-empty mask; DAILY and FLEXIBLE_WEEKLY_TARGET do not require a scheduled-day mask; flexible-weekly preferred days are optional guidance only. Reject unknown bits and invalid combinations.

For permanent habit deletion, the architecture and schema addendum propose a delegated default: queue opaque notification IDs in a cancellation outbox; transactionally remove habit-owned DB data and delivery rows while preserving independent journal entries (nulling any optional habit link) and global reminders; cancel OS notifications after the DB commit; clear outbox IDs only after successful cancellation and retry after interruption. Delivery workers check that the habit still exists and is active. This is a planning default and may be overridden before full schema approval; it has not been implemented or tested.

## Remaining planning checklist

### P0 — Complete before asking the owner to approve the planning gate

- [x] **Schema closure draft prepared:** docs 30 expands the proposed constraints, transactions/rollback, deletion and notification cleanup, restore-conflict records, backup mapping, and migration acceptance plan; docs 03 now references and incorporates the first structural refinements. This remains proposed, not owner-approved or implemented.
- [ ] **Schema proposal final review:** reconcile all doc 30 refinements and verify every table/constraint/index/transaction in docs 03; decide the undo/recovery contract; present the complete proposal for owner approval. Do not inspect or migrate the real database in this planning step.
- [x] **Reminder safety defaults documented:** docs 23/05/26/07 now define proposed 21:00–08:00 quiet hours, a shared three-notifications/day cap, collision handling, late-delivery checks, local-only personalisation evidence/rollback, and partial-tip frequency/suppression. These are delegated proposals, not owner-approved policy or tested OS behaviour.
- [ ] **Reminder/prompt owner review or deferral:** accept or explicitly defer the delegated reminder defaults, and resolve/defer retrospective journal prompt eligibility/frequency before Gate A.
- [ ] **Undo and recovery:** specify which latest actions offer immediate undo, what ordinary edit/delete can do later, and what is irrecoverable after permanent deletion. No value-based deduplication or silent history rewrite.
- [x] **UX/design approval pack prepared:** `29-UX-DESIGN-APPROVAL-PACK.md` consolidates a recommended navigation baseline, Today hierarchy, Growth Mark/archive options, comeback presentation, screen/state inventory, and comparable prototype tasks. This is a planning package, not owner approval.
- [x] **Text low-fidelity wireframe variants drafted:** doc 29 now includes three comparable Today hierarchies, two navigation options, three archive patterns and a task-based scorecard. These are planning sketches only; rendered prototype review and owner approval remain outstanding.
- [ ] **UX prototype/review:** turn the text wireframe sketches into comparable visual prototypes; review core retrieval/logging tasks, empty/error/destructive states and accessibility. Text sketches alone are not usability evidence.
- [ ] **Design decision pack:** owner-reviewed Growth Mark measure/representation, journal archive pattern, typography/colour candidates, system/light/dark theme policy, and accessibility review. Any feature not approved can be explicitly deferred rather than silently assumed.
- [ ] **Privacy language:** align user-facing claims with the approved no-account/offline core, portable backup versus provider-backed file syncing, D-186 OS-backup exclusion policy, deletion/export coverage, and unverified encryption/implementation facts.
- [x] **Initial traceability pass:** created [28-REQUIREMENT-TRACEABILITY.md](28-REQUIREMENT-TRACEABILITY.md), mapping core requirements to screen/data contracts and planned acceptance cases.
- [ ] **Traceability completion:** close the gaps listed in doc 28, especially mood-insight fixtures, final reminder policy, schema/transaction cases, and the UX/accessibility review.
- [x] **Relative-link target pass:** checked relative Markdown file targets across all 36 current Markdown documents; no missing targets were found. This does not validate heading fragments or external URLs.
- [ ] **Semantic/anchor reconciliation:** audit heading fragments, stale claims, duplicate/conflicting statements, and evidence claims across README and the planning set. Keep dated research as historical evidence rather than treating it as the current register.
- [ ] **Owner walkthrough:** show resolved choices, proposed defaults, and the small set of genuine owner-level decisions in one batch. Update the decision log where a new consequential owner choice is made.
- [ ] **Gate record:** only after the owner explicitly approves, update the approval record in docs/19-AI-STUDIO-GITHUB-WORKFLOW.md. Until then, the gate remains closed.

### P1 — Only after a separate explicit technical-validation authorisation

- [ ] Inspect the latest repository tree and actual Gradle/toolchain configuration; reconcile source against the approved requirements.
- [ ] Run reproducible Gradle build and available checks/tests; record exact commands, versions, failures and commit.
- [ ] Inspect the existing SQLite schema and placeholder upgrade path; verify transaction/index/foreign-key constraints and write a migration strategy before any data migration.
- [ ] Verify D-186 Android backup/data-extraction rules on supported OS/device-transfer paths.
- [ ] Test local persistence, restart/process death, airplane mode, notification permission denial, scheduling/cancel/reboot behaviour, export, deletion, backup/restore and migration using synthetic data.
- [ ] Install and verify on a physical Android device; record Android version, device, artifact and limitations.
- [ ] Re-audit planned requirements against observed source and test evidence. Do not treat source presence or a passing build as proof every feature is complete.

## Current next action

Continue the documentation reconciliation and close the remaining P0 items without modifying application code. The UX/design approval pack exists in doc 29 and the schema closure/migration plan in doc 30; next, finish the schema and traceability reconciliation and prepare visual/comparable prototypes based on doc 29, not UI implementation. An initial requirement-to-test matrix exists in doc 28; close its listed gaps, then finish heading-anchor and semantic consistency review. After that, present the owner approval batch. No technical validation or feature implementation should start until the owner separately approves the planning gate and authorises the relevant next stage.
