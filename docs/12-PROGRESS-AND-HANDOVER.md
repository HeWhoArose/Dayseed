# Progress and Handover

**Last updated:** 2026-10-27  
**Status:** Planning phase; planning gate not approved; feature implementation is not authorised. Existing source files/preview are evidence for the later audit, not proof of a verified build or approval to add features.

## Repository status

Repository: https://github.com/HeWhoArose/Dayseed  
Canonical branch: `main`. GitHub is the source of truth for planning and future implementation, regardless of the active coding environment.

## Current state

- Product direction and core v1 boundaries: most core data semantics are approved and reconciled through D-195; remaining planning work is mainly UX/design approval, a complete schema proposal, reminder/prompt/undo edge cases, traceability and repository consistency.
- UX directions and information architecture: explored, not approved.
- Existing repository source: native Kotlin + Jetpack Compose, `SQLiteOpenHelper`, repository, ViewModel/StateFlow, and screens are present. Treat the project as being in the planning phase for our workflow; do not add features yet. Source/preview presence does not verify build, tests, installation, or device behavior. The later audit must reconcile observed source with the approved plan.
- Growth Mark and return experience: concepts, definitions, sample copy for 3/14/90 days, zero-completion state, and test cases documented in [docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md). Not approved.
- Journal archive: timeline/calendar/editorial comparison and task-based test plan documented in [docs/16-JOURNAL-ARCHIVE-RESEARCH.md](16-JOURNAL-ARCHIVE-RESEARCH.md). Hybrid timeline + calendar is only a proposal.
- Typography, light/dark candidate palettes, contrast checks, font licensing notes, and small-screen tests documented in [docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md).
- DESIGN.md is now specified as the sole authoritative token/component reference; candidate values remain unapproved.
- Competitor screenshot evidence for Loop Habit Tracker and Daylio is recorded in [docs/18-COMPETITOR-SCREENSHOT-AUDIT.md](18-COMPETITOR-SCREENSHOT-AUDIT.md). Public listing screenshots were inspected; current live app flows have not been tested.
- AI Studio/GitHub workflow and native-app feasibility-spike acceptance criteria are in [docs/19-AI-STUDIO-GITHUB-WORKFLOW.md](19-AI-STUDIO-GITHUB-WORKFLOW.md).
- Data/privacy baseline and portable-backup decisions are substantially documented. Android platform Auto Backup/device-transfer policy is resolved by D-186 (exclude sensitive Dayseed data by default); technical verification of the actual rules and supported transfer paths remains open. Other remaining items include final deletion/export wording and implementation-level validation.
- Monetisation remains a hypothesis; no provider/SDK selected.
- Existing app source is present on `main`, but the current workflow remains in planning and feature work is not authorised. Do not claim a clean build or verified implementation without evidence.
- Automated tests/build: none run in this planning continuation. Native Android app source exists on `main`; its build, tests, APK installation, and device behavior remain unverified.


## Planning decisions confirmed — 2026-10-18

The owner has confirmed the following product rules in conversation and they are recorded in [Decision Log](09-DECISION-LOG.md):

- Numeric entries are whole numbers only; an intentional zero is valid and differs from no record. Multiple entries for a habit/date sum to a daily total.
- Weekly targets are flexible, with optional preferred days that do not restrict completion.
- Schedule edits offer Apply today/Apply tomorrow while preserving existing activity records unchanged.
- Historical completion uses the target that applied at the time; comparisons against the current target are separate.
- Rest/non-scheduled days are neutral, paused periods are excluded, and scheduled non-completion is a missed opportunity rather than a judgement.
- Habit measurement mode is selected by the user: duration, quantity, or completion-only. For measured modes, progress labels are derived from actual amounts relative to the usual target, not from self-reported feelings.
- Today cards expose separate quick Complete and detailed Record actions. Quick Complete defaults to the usual target for measured habits; Record supports actual amounts, corrections, and an optional habit/date note.

The current phase remains **planning**. Continue documenting product rules, the eight-screen blueprint, navigation, data contracts, and edge cases. Do not start feature implementation. The existing native app source and preview are evidence for the later implementation-reality audit, not proof that implementation is approved or verified. The progress bands, user-selected week start, multiple journal entries, multiple numeric/duration records, and duplicate/undo direction have since been recorded as decisions. Remaining work is to formalise their edge cases in cross-screen/data contracts and tests; do not describe already-decided rules as undecided.

## Cumulative research inventory and audit — 2026-10-17

- Added [Cumulative Research Inventory and Documentation Audit](20-CUMULATIVE-RESEARCH-INVENTORY-AND-AUDIT.md) to distinguish documented hypotheses from completed evidence and approved decisions.
- Enumerated all 26 Markdown files on `main`; checked relative Markdown file targets and found no missing target files. Heading anchors, external URLs, and claim-level citations still need deeper review.
- Confirmed the competitor research document is a plan, not a completed market study; public listing screenshots are not hands-on competitor tests.
- The audit prioritises remaining day-record edge cases, date/time policy, backup/restore and platform backup verification, reminder delivery edge cases, representative usability validation, and the owner approval gate.
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

Use [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) as the current checklist; it supersedes older next-action lists in this historical handover where they describe resolved decisions as open.

1. Close the written schema proposal: keys, nullability/check constraints, foreign keys/indexes, transaction/rollback, deletion ownership, and migration/backup compatibility.
2. Finalise or safely defer the remaining reminder/notification, retrospective-journal-prompt, partial-tip, and undo/recovery details.
3. Prepare the primary navigation/wireframe and visual-design approval pack, including Growth Mark, archive pattern, typography/colour/theme, and accessibility.
4. Close the gaps in [28-REQUIREMENT-TRACEABILITY.md](28-REQUIREMENT-TRACEABILITY.md) and run the current cross-document claim/link/anchor review.
5. Present the genuine owner choices and safe deferrals together; keep the planning gate closed until the owner explicitly approves it.
6. Only after separate authorisation, run the bounded native technical-validation stage. Do not treat repository source presence as proof of a clean build or verified behaviour.

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

The head-to-head comparison is in [docs/14-STACK-COMPARISON.md](14-STACK-COMPARISON.md). The five new research and workflow documents are linked above. D-009 (native Kotlin + Jetpack Compose) is now approved. D-008 (visual direction), D-016 (Growth Mark), D-017 (comeback), D-018 (journal archive), D-019 (typography/colour), D-021 (competitor evidence), and D-022 (AI Studio/GitHub workflow) remain open or proposed. No design or implementation approval has been recorded.


## Additional planning decisions confirmed — 2026-10-19

- Users may choose the first day of their week; weekly goals, summaries, and calendar displays must use that choice consistently.
- Multiple duration sessions for the same habit/date are summed into a daily duration total, while individual sessions remain inspectable and editable. Different habits keep separate totals and units.
- Support both an optional habit/date note as the default and optional notes on individual entries. These are separate from the general daily journal.
- Provide a way to undo a recent accidental action, correct/delete entries later, and prevent accidental duplicate quick-completion taps without blocking legitimate repeat sessions.
- Discreet XP/rank near the profile is an idea to research, not an approved feature. Online leaderboards and viewing other people's streaks remain deferred pending explicit approval and privacy/safety/architecture research.

- Daily duration/quantity outcomes distinguish Partial, Goal met, and Above goal while preserving actual amounts. Weekly review distinguishes daily outcomes from flexible weekly-target progress. A short optional, nonjudgmental encouragement sequence after partial progress is approved in direction only; cadence, trigger, copy, and opt-out remain open.

- The owner prefers both in-app encouragement and optional phone notifications. Plan outcome-aware messages for partial, goal-met, above-goal, and still-unrecorded scheduled habits, plus a later-day check-in. Explore learning a user's common completion times over repeated observations, with manual controls, quiet hours, rate limits, opt-out, and privacy safeguards. Learning thresholds, timing defaults, and reminder coordination remain open; this is planning only.

- Reminder setup decision: ask once during onboarding whether reminders are wanted. Yes enables reminders; No creates none. Each habit creation/edit form shows a reminder-time control defaulted to 9:00 a.m. local time, and that habit uses the displayed time unless changed. For habits on system-managed timing, Dayseed may auto-adjust after 14 days of sufficient completion observations, then observe the new time for 28 further days to validate/solidify the adjustment. If the routine changes again, it may auto-adjust again while system-managed timing remains enabled. A manually edited time is protected: later pattern changes may be suggested but not applied automatically unless the user explicitly returns the habit to automatic timing. Option B for follow-up is selected: one optional evening check-in if still unrecorded. Open: evidence thresholds, evidence thresholds, 28-day validation and rollback rules, opt-out and per-habit/global controls, quiet hours, Android permission, and notification limits.

Continue in planning. Do not implement these ideas or start the architecture audit until the remaining product decisions, screen blueprint, cross-screen contracts, and planning consistency review are complete.

## Implementation reality audit (2026-10-18)

The current repository contains a native Kotlin/Jetpack Compose Android app and SQLite persistence code; the older planning-only status is stale. Source presence is not build/test verification. Read [Implementation Reality Audit](22-IMPLEMENTATION-REALITY-AUDIT.md) before changing architecture or adding features. Gradle build, tests, APK install, and device validation remain unverified by this audit. AI Studio is the active AI-assisted development environment; GitHub `main` remains canonical.


## Planning reconciliation — 2026-10-26

- Added `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` to centralise approved decisions, delegated defaults, owner-only decisions, and cross-document conflicts. Commit: `e721ed18ba50fde55169ec7057eb24e0f791a204`.
- Updated `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md` to align with the owner-approved native Kotlin + Jetpack Compose stack. Commit: `fdeb80fbbc96b220dc8ab0cbd79c649789b8c590`.
- Corrected the historical stack comparison's link to `docs/22-IMPLEMENTATION-REALITY-AUDIT.md`. Commit: `1ec1ca523ce8341748783cf8b85dad3181dfc6e1`.
- **Historical note (superseded on 2026-10-27):** an earlier reconciliation recorded 9:00 a.m. as the approved weekly reflection time. That conflated it with the separate per-habit default. The current rule is in D-045 and docs 23/25/27/31: weekly reflection trigger/catch-up rules remain recorded, but the weekly clock time requires separate owner confirmation.
- Re-fetched and verified each changed file after writing.
- No application code was changed; no implementation audit or build/test run was started.
- D-009 is resolved: continue the existing native Kotlin + Jetpack Compose project. R-004 is reconciled: initial v1 includes readable journal export and user-controlled portable local backup/restore, while online backup/cross-device sync remain future premium direction outside v1 and require a separate explicit approval gate.


## D-009 stack decision — 2026-10-26

- Owner decision: continue the existing native Kotlin + Jetpack Compose Android project. The active stack direction is approved; do not scaffold a second app or rewrite to React Native/Expo without a new explicit decision.
- Updated the decision log, technical architecture, AI Studio workflow, and decision register to align with D-009.
- This approval does not authorise feature implementation or an implementation audit. Build/test/APK/device status remains unverified. Continue planning and wait for explicit approval of the planning gate before technical validation.
- R-004 cloud/premium scope conflict is reconciled: initial v1 remains offline/account-free for core use, includes readable journal export and local backup/restore, and excludes online backup/cross-device sync. The future premium direction is recorded but not authorised for implementation.


## Cloud/premium scope reconciliation — 2026-10-26

- Reconciled R-004 across the project charter, product specification, technical architecture, data/privacy, monetisation, testing, roadmap, decision log, workflow, risk register, decision register, and this handover.
- Initial v1: core habit/journal use is offline and account-free; include readable journal export and user-controlled portable local backup/restore. Exclude Dayseed-managed accounts, online backup, and cross-device sync.
- Future direction: online backup/cross-device sync may be premium later, but no service, price, purchase provider, or monetisation model is approved. A later release needs a separate explicit product, privacy/security, consent, data-flow, recovery/conflict, retention/deletion, and monetisation gate.
- Clarified that Dayseed's portable backup is distinct from Android/platform backup. A user-selected Android file provider may itself sync a file online; privacy copy must disclose that possibility without implying Dayseed provides that service.
- Verified after writes: charter `8ee2b1ca9be10e2af4331bd98e6db00c4abff514`; product spec `895612ca99de2eb0f1c8038cb911c219a10bd555`; architecture `f4011b8dbadb2c6f9254d3cc864b42ff8123bce8`; privacy `ac65182f95c04ae4a2db3eae0a4890b2736845be`; monetisation `8212243adbf76cace936fb3da793587bd2e8ec4c`; testing `a79d684b81a67a73de0852a7945a45dc0207fcca`; roadmap `c5483589d06c6ef82597a80208c83d98251b227f`; workflow `26b485aff636389801a71f32409a23af89d4b3dd`; decision log latest backup/privacy clarification `beb7eb06b291696ecf9bc89ae7b00c284317448e`; decision register `bc91e23f4349e060e2e3ce40ab7befedba046d63`; risk register `7c4279caadb8e0714d32ab7950081dd9b158516a`.
- Planning only: no app code changed; no build, tests, APK install, device validation, or implementation audit run.
- Next: continue the remaining planning reconciliation and open UX/data decisions, then perform a planning completeness review. Wait for the owner's explicit approval before technical validation or implementation.


## Planning reconciliation continuation — 2026-10-26

- Normalised the mood-insight decision rows D-083–D-090 in `docs/09-DECISION-LOG.md` to the standard column semantics and removed a duplicate D-090 row. No later decision IDs were renumbered; the broader audit of decision rows after D-090 remains open. Commit: `c9292807c1eac245da95bd14c39d50249073455a`.
- Updated `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` to record that R-003 is partially corrected, with the remaining column-semantics audit explicitly tracked. Commit: `b6afc0768842aead18be70f7c178fbf8422c8b56`.
- Reconciled `docs/11-RISKS-AND-OPEN-QUESTIONS.md` with existing approved decisions so resolved items (schedule direction, multiple entries, backup scope, and native stack) are no longer presented as wholly unanswered. Remaining edge cases stay open. Commit: `ae8161fb733dca6870d6de57ae1da2a4027d29db`.
- Re-fetched and verified each updated document after writing. No app code changed. No build, tests, APK install, device validation, or implementation audit was run.
- Next: finish the broader decision-log row audit, align the handover and roadmap to the reconciled state, then continue the planning completeness review. Do not cross the implementation gate without explicit owner approval.


## Decision-log structure audit completed — 2026-10-26

- Completed the broader decision-log structure pass. Normalised D-106–D-185 to the standard five-column schema, preserving the chosen behaviour in Current position and retaining rationale/acceptance criteria plus decision date in Next action. D-083–D-090 were already normalised in the preceding pass.
- Re-fetched `docs/09-DECISION-LOG.md` after the update and verified every Markdown table row has the expected five fields (six pipe characters including row boundaries), with exactly one D-090 row. Commit: `f96cd3ede7b618ab262efcf64828d02944021d4d`.
- Updated the decision register to mark R-003 corrected and the formatting checklist complete; re-fetched and verified it. Commit: `c4cd44111703e8a548aeb43c96a96703b0c1dc0b`.
- This is a documentation-format correction only: decision IDs were not renumbered, and no product decision was changed. No app code changed; no build, tests, APK installation, device validation, or implementation audit was run.
- Next planning task: reconcile the implementation roadmap and screen/data contracts with the latest approved decisions, then perform the planning completeness review. Owner approval is still required before technical validation or implementation.


## Architecture and quality-plan reconciliation — 2026-10-26

- Updated `docs/03-TECHNICAL-ARCHITECTURE.md` to remove the incorrect one-record-per-habit/date uniqueness rule and the claim that journal-entry multiplicity is undecided. It now reflects approved multiple numeric/duration entries and multiple independent journal entries, while explicitly leaving exact schema, idempotency, date policy, and migration details open. Commit: `a7d9cd86fa461af1809754e8bcd9473380532898`.
- Updated `docs/08-IMPLEMENTATION-ROADMAP.md` so monetisation is not part of initial v1 unless a separate explicit owner decision approves it. Commit: `1c4f0b3762ca4fc2d3e31c406390a9ee8ce985b8`.
- Reordered and expanded the regression checklist in `docs/07-TESTING-AND-QUALITY.md` to cover multi-entry records, independent journal entries, zero/no-record semantics, backup/restore safety, privacy boundaries, and migrations. Commit: `9867c42ad061b8e620832ba2a60514bbd5cd71ab`.
- Added R-008/R-009 to the decision register and split the data-model checklist into completed alignment versus remaining schema/date/migration/platform-backup work. Commit: `5205d47ec20c6518a9258c89bc081c5f811c70b8`.
- Re-fetched and verified every changed document after writing. These are planning-document changes only. No app code, build, tests, APK installation, device validation, or implementation audit was performed.
- Next: reconcile remaining duplicate requirement identifiers and stale product-specification wording, then review screen/data contracts and complete the planning completeness pass. The planning gate remains closed pending explicit owner approval.


## Product requirement identifier cleanup — 2026-10-26

- Re-fetched the product specification and decision log and checked requirement/decision IDs for duplicates. The decision log now has 179 decision rows with no duplicate D-IDs.
- Renamed the second H4 section in `docs/01-PRODUCT-SPECIFICATION.md` to H4b and clarified that retry idempotency and legitimate repeated entries are approved, while the exact undo window/recovery interaction remains open. Re-fetched and verified the product spec; requirement-heading IDs are unique. Commit: `89684eb5e35aef184d711aa80a6313f2b366c7b3`.
- Added R-010 to the decision register and verified it. Commit: `75c5ddb128d4754e4a2c24657af5b8e3a2ce9d87`.
- Documentation only; no app code, build, tests, APK installation, device validation, or implementation audit was performed.


## Date/time and cross-screen contract pass — 2026-10-26

- Added a provisional shared date/time contract to `docs/03-TECHNICAL-ARCHITECTURE.md`: separate local date keys from event instants, preserve historical date keys across travel/time-zone changes, use the selected week-start consistently, handle backdated records explicitly, avoid fixed-24-hour calendar arithmetic, and deduplicate reminder/comeback occurrences. Trigger-specific approved decisions D-146–D-160 remain authoritative. Commit: `41f026348e9e4b3a0068f6960c86fab96127ff35`.
- Updated `docs/04-DATA-AND-PRIVACY.md` and the decision register to require an explicit owner choice on Android Auto Backup/device-transfer inclusion of sensitive data. This trade-off was deliberately not silently decided: inclusion can ease recovery but may create additional copies; exclusion can reduce unintended copies but may impair device migration. Commits: `3c5a20b8f2cdc381f96a6b1996bf6fec7525fafe`, `ec39a2179364b6a7274f6cca3fa02dfcea247df7`.
- Created `docs/26-SCREEN-AND-DATA-CONTRACTS.md` covering Today, record detail/edit, journal, history/calendar, insights, comeback, reminders, backup/restore, privacy, shared data invariants, failure states, and remaining owner/technical gates. Re-fetched and verified the full file. Commit: `0a2e844095b9f58714872daee1e8d68035bff48f`.
- Added R-011 and R-012 to the reconciliation register for the date/time contract and cross-screen contract; re-fetched and verified. Commit: `5c7f02be65b4f03f72283de22b1e271d2f99d528`.
- No app code changed. No build, automated tests, APK installation, physical-device validation, or implementation audit was run.
- Next: complete cross-document review of reminder/privacy/backup details, verify requirement-to-test traceability and remaining stale wording, then produce the planning completeness review. The implementation gate remains closed.


## Cross-document backup consistency pass — 2026-10-26

- Found and corrected a stale paragraph in `docs/24-COMEBACK-EXPERIENCE.md` that still described portable-backup coverage and restore conflicts as undecided despite approved D-091–D-145 decisions. It now points to the approved backup/restore contract and explicitly keeps Android platform-backup inclusion as a separate owner decision. Re-fetched and verified. Commit: `a2e1b2d215e3fbdc6878e28244883e19a6e9fec0`.
- Added R-013 to `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md` and re-fetched/verified. Commit: `a4502873fb65f429da5bafab5aa2b357c0b2fc56`.
- A follow-up edit to the persistence-test wording in `docs/07-TESTING-AND-QUALITY.md` was not applied because the repository update was rejected; that one line still needs correction to specify identity/idempotency constraints without a blanket unique habit/date constraint. No claim is made that it changed.
- No app code changed. No build, automated tests, APK installation, device validation, or implementation audit was run.

- Updated Phase 3 in `docs/08-IMPLEMENTATION-ROADMAP.md` to include the cross-screen contract and require the Android Auto Backup/device-transfer owner decision to be explicitly resolved or consciously deferred with a safe documented rule. Re-fetched and verified. Commit: `5afee35c5803e0ae1a071e04e7fd6a681fe69b7d`.


## Android platform-backup policy resolved — 2026-10-26

- Owner decision D-186 is recorded in `docs/09-DECISION-LOG.md`: exclude sensitive Dayseed app data from Android platform Auto Backup/device transfer by default in initial v1. Users should use the clearly explained Dayseed portable backup/restore flow for phone migration. This is distinct from the app's opt-in file-based Auto Backup to a user-selected destination.
- Updated and re-fetched `docs/04-DATA-AND-PRIVACY.md`, `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/01-PRODUCT-SPECIFICATION.md`, `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`, `docs/08-IMPLEMENTATION-ROADMAP.md`, and `docs/24-COMEBACK-EXPERIENCE.md` to remove the unresolved owner-choice wording and carry D-186 through privacy, UX, architecture, and planning gates.
- Updated `docs/07-TESTING-AND-QUALITY.md` with a specific acceptance scenario to verify manifest/data-extraction rules and representative supported OS/device-transfer paths separately from Dayseed portable backup. Re-fetched again after an initially stale read; current file contains the updated scenario. These are planned tests, not executed tests.
- Verified changed file contents after writes. Commits: decision log `54867ec87069e7acbb33688a5df28e2af546ab2b`; privacy `5a116f73cf362d1136dbb35e4364da966238fcb2`; architecture `24fce3481bc0cf82f40f18ac31c2c4830a9133a5`; screen/data contract `2fc5aed98ae3f60d45ec057b8524da9e36e79c3f`; product spec `e68a34bea0d2cdb53722cf2e4378f4a9cb830b83`; register `64c0ac0479fb10bae8b4347af6d0fcd708a44b5b`; roadmap `941c035ad6023be86901379bebab2836a55b5d79`; comeback document `501e5551962bfb8472c065525ec158eba133db28`; testing plan `80d5bd8d2d0752d48423bbbbc87be0ae7b1f41e0`.
- No app code changed. No build, automated tests, APK installation, physical-device validation, or implementation audit was run. Technical verification of the actual Android backup configuration remains pending and must wait for the authorised validation gate.

## Date/history policy approved — 2026-10-26

- Owner approved D-187: preserve saved local-date keys across timezone travel; handle manual clock/timezone changes conservatively without silently rewriting records, fabricating elapsed time, or duplicating logical events; apply target/schedule changes prospectively while preserving historical interpretation; archive habits by default and retain accessible history; keep permanent deletion separate and explicit; require explicit date selection for backdated records.
- Recorded D-187 in `docs/09-DECISION-LOG.md` and reconciled `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/04-DATA-AND-PRIVACY.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, `docs/01-PRODUCT-SPECIFICATION.md`, `docs/08-IMPLEMENTATION-ROADMAP.md`, and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`.
- Re-fetched each changed document after writing and verified the decision/policy text and the corrected persistence-test wording.
- Planning only. No app features were implemented; no build, automated tests, APK installation, device validation, or implementation audit was started. Schema/index/transaction/idempotency design, migration safety, and clock/timezone/reminder edge-case evidence remain unverified and must wait for explicit technical-validation authorisation.

## Proposed schema decision — separate activity and daily outcome storage

- Owner approved the partial schema choice to store quantity/duration sessions in `habit_entries` and completion-only/effort-aware selections in `habit_day_outcomes` (D-189). Numeric sessions remain individually correctable and multiple distinct entries per habit/date are allowed; explicit zero is a recorded numeric entry. The domain must validate each write against the effective-dated tracking mode and reject contradictory numeric/non-numeric states.
- Updated the proposed model and affected architecture, screen/data contract, planned acceptance coverage, risk/open-question status, and decision register. The complete schema remains proposed; D-192 resolves note cardinality by approving both optional per-entry notes and one optional habit/date note. D-190 approves stable UUID identity and nullable unique idempotency keys, with key reuse limited to retries of the same logical submission. D-191 subsequently approved the structured schedule fields; stable weekday-mask encoding and technical validation remain gated.
- Each file was fetched from `main` before its update and re-fetched immediately after. Planning only: no implementation, build, tests, APK/device validation, or implementation audit.

## Proposed v1 data model — 2026-10-26

- Added a concrete candidate relational schema to `docs/03-TECHNICAL-ARCHITECTURE.md`, explicitly marked proposed and awaiting owner review. Candidate entities cover stable habit identity, effective-dated habit configuration, numeric/duration entries, daily non-numeric outcomes, date-level habit notes, saved journal entries, separate drafts, typed app settings, and reminder-delivery deduplication.
- Updated `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, and `docs/11-RISKS-AND-OPEN-QUESTIONS.md` to reference the proposal, state the key invariants, and list acceptance coverage that has not been executed. The decision register records owner approval as pending.
- The effective-dated configuration snapshot choice remains approved as a partial schema decision. D-189 now also approves separate numeric/duration entries and per-day completion-only/effort-aware outcomes, with mode validation and contradictory-state rejection. The complete schema remains proposed, not approved for implementation. D-192 resolves note cardinality: both optional per-entry notes and one optional habit/date note are supported. D-190 approves stable UUIDs plus nullable unique idempotency keys; D-191 approves the structured schedule fields aligned with the existing schedule decisions. D-188 resolves explicit-miss replacement by removing user-entered miss outcomes and deriving missed status from schedule plus absence of qualifying activity.
- Fetched each changed file before updating and re-fetched after each update; post-write checks confirmed the expected proposed/pending status.
- Planning only: no application code, schema, migration, build, test, APK, or device behaviour was changed or validated. Do not implement or migrate until the owner approves the model and separately authorises technical validation.


## Missed-day and backdated activity policy approved — 2026-10-26

- Owner approved D-188: do not offer a manual “mark as missed” control or store a user-entered miss outcome. A scheduled day becomes derived-missed after it has passed without qualifying activity recorded; rest/non-scheduled days remain neutral and paused periods are excluded.
- Allow genuine habit activity to be explicitly backdated to its actual local date. Recalculate that date's result and dependent summaries from the activity and the configuration effective on that date. A journal memo can explain why activity was not recorded, but a memo alone must never create, remove, or change habit status.
- Updated `docs/09-DECISION-LOG.md`, `docs/01-PRODUCT-SPECIFICATION.md`, `docs/03-TECHNICAL-ARCHITECTURE.md`, `docs/26-SCREEN-AND-DATA-CONTRACTS.md`, `docs/07-TESTING-AND-QUALITY.md`, `docs/11-RISKS-AND-OPEN-QUESTIONS.md`, and `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`. Each file was fetched before its update and re-fetched afterward; expected policy text was checked. One intermediate screen-contract verification exposed stale wording, which was corrected and re-verified.
- The relational schema remains proposed and owner approval is still pending for remaining model choices other than note cardinality, which D-192 resolves. D-191 approved the structured schedule fields; stable weekday-mask encoding and technical validation remain gated. This is planning documentation only: no app code, schema, migration, build, tests, APK, device validation, or implementation audit was run or changed.

- Reconciliation follow-up: marked D-183 as superseded in part by D-188 so its older suggestion of a user-entered explicit miss cannot conflict with the approved derived-missed policy. Updated regression and screen contracts to distinguish no record, explicit zero, and derived missed status, and specified that the calendar/history visibly labels past scheduled days with no qualifying activity. Re-fetched and verified each changed file after its update.


## Proposed schema decision — stable IDs and retry idempotency (D-190)

- Owner approved stable UUID identity for habits, entries, journal entries, and drafts; edits preserve identity.
- A retry of the same logical activity submission reuses the same idempotency key. A distinct intentional entry uses a new key. Non-null keys must be unique; null is permitted where retry protection is not applicable. Do not deduplicate based only on matching values or timestamps.
- Updated the architecture, screen/data contract, planned acceptance coverage, risk/open-question status, and decision register. Schema remains proposed; habit/date note cardinality is resolved by D-192. D-191 approves the structured schedule fields; the stable weekday-mask encoding and technical validation remain gated. Planning only; no code, builds, tests, APK/device validation, or implementation audit.


## Proposed schema decision — structured schedule fields (D-191)

- Owner approved explicit schedule representation: `DAILY`, `SELECTED_WEEKDAYS`, or `FLEXIBLE_WEEKLY_TARGET`; explicit target period (`DAY` or `WEEK`); a selected-weekday mask for selected-day schedules; and optional preferred weekdays for flexible weekly targets as guidance only.
- `DAILY` and `SELECTED_WEEKDAYS` schedules use a day target; `FLEXIBLE_WEEKLY_TARGET` uses a week target. Numeric/duration weekly goals sum compatible units; completion-only/effort-aware weekly goals count qualifying completed dates. The saved app-level week-start preference governs all week calculations. Locale informs the initial week-start default only.
- Updated the proposed architecture, screen/data contracts, planned acceptance coverage, risk/open-question status and decision register. Schema remains proposed; D-192 resolves the date-level note decision, while separate technical validation and stable weekday-mask encoding remain gated. Planning only; no code, builds, tests, APK/device validation or implementation audit.

## Proposed schema consistency review — 2026-10-26

- Reviewed the proposed relational schema against the current screen/data contract, quality plan, risk register, decision register, handover, and decision log.
- Corrected the schedule wording so `DAILY` means every local calendar day, and both `DAILY` and `SELECTED_WEEKDAYS` use `target_period = DAY`; `FLEXIBLE_WEEKLY_TARGET` uses `WEEK`. Clarified that an absent numeric record is distinct from an explicit zero and removed stale wording that implied the recently decided schema choices were still awaiting individual owner decisions.
- Added R-015 to track remaining proposal questions: stable weekday-mask encoding; repeated Apply-today edits when a version effective today already exists; and handling habit-linked reminder-ledger rows and pending OS notifications on permanent deletion. The initial review also noted the explicit skipped-action question, later resolved by D-193.
- Re-fetched each changed document immediately after its update and verified the intended wording. The schema is still proposed, not approved for implementation; the weekday-mask encoding and identified behavioral questions remain unresolved. These are planning-document corrections only.
- No app code or database schema changed. No build, automated tests, APK installation, device validation, or implementation audit was run. Do not begin technical validation or implementation without the owner's separate explicit authorisation.

## Habit-day Skip policy approved — 2026-10-27

- Owner approved D-193: v1 has no explicit “Skip this day” action for a habit and no persisted skipped habit-day outcome.
- A scheduled day without qualifying activity becomes derived-missed only after the scheduled local day passes. Rest/non-scheduled days remain neutral, paused periods are excluded, genuine activity may be explicitly backdated to its actual date, and journal memos do not change habit status. Optional skipping of journal prompts and comeback reflections is unaffected.
- Marked D-006 as superseded in part by D-193 so its older suggestion that skipped habit-days need a separate state no longer conflicts. Reconciled the product specification, proposed schema, screen/data contract, quality plan, risk register, and decision register; test coverage is planned, not executed.
- The complete data schema remains proposed. Following D-195, the weekday-mask encoding policy is approved; technical validation of the mask constraints and reminder-ledger/pending-notification cleanup on permanent habit deletion remain open. No app code, build, tests, APK/device validation, or implementation audit was run. Do not begin technical validation or implementation without separate explicit authorisation.

## Repeated Apply-today behavior approved — 2026-10-27

- Owner approved D-194: the first Apply-today change creates today's configuration version. A further same-day edit replaces today's version only while no qualifying habit activity has been recorded today; after activity exists, preserve today's configuration and activity and offer Apply tomorrow. Cancel changes nothing; Apply tomorrow creates or updates tomorrow's not-yet-effective version.
- Reconciled the decision log, product specification, proposed architecture, screen/data contract, planned quality coverage, risk register, and decision register. The repeated Apply-today owner-policy question is resolved; D-195 now resolves the weekday-mask encoding policy. Technical validation and cleanup of habit-linked reminder-ledger rows/pending OS notifications on permanent deletion remain open.
- The relational schema remains proposed, not approved for implementation. Acceptance cases are planned, not executed. No code, build, automated tests, APK/device validation, or implementation audit was performed; technical validation and implementation remain gated by separate explicit authorisation.

## Weekday-mask encoding approved — 2026-10-27

- Owner approved D-195: encode weekdays with a locale-independent 7-bit integer mask, Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. Combine chosen days by bitwise OR. `SELECTED_WEEKDAYS` requires a valid non-empty mask; `DAILY` and `FLEXIBLE_WEEKLY_TARGET` do not require a scheduled-day mask; flexible weekly preferred weekdays are optional guidance only. Unknown bits and invalid schedule/mask combinations must be rejected.
- Updated the decision log, proposed architecture, screen/data contract, planned acceptance coverage, risk register, and decision register. Re-fetched each updated file and verified the intended content. The encoding policy is resolved; technical validation remains pending.
- The complete schema remains proposed, and acceptance cases remain planned, not executed. Permanent-deletion notification cleanup now has a delegated planning default in docs 03/30/26/07; remaining work is owner review of the complete schema and later authorised verification of transaction, cancellation-failure, startup-retry, and stale-worker guard behaviour. No code, build, tests, APK/device validation, or implementation audit was performed; any technical validation or implementation still requires separate explicit authorisation.


## Current readiness snapshot — 2026-10-27

See [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) for the current workstream matrix and P0/P1 checklist. It supersedes older “next action” lists in this historical handover where they describe already-resolved decisions as open.

- D-195 weekday-mask encoding is approved and reconciled: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. The mapping is locale-independent; valid/non-empty and schedule-kind constraints are planned for validation, not tested.
- The formerly open permanent-habit-deletion notification question now has a delegated planning default: queue opaque notification IDs in a cancellation outbox, delete habit-owned data transactionally, preserve independent journal entries/global reminders, cancel OS notifications after the database commit, clear the outbox only on success, retry after interruption, and guard workers against deleted/inactive habits. This is unimplemented/unverified and may be overridden before schema approval.
- Earlier Expo-first text in the Android platform research and Expo-specific font-packaging wording were corrected to align with D-009 and native Kotlin/Compose.
- Clarified in the product specification that complete schema design is a planning deliverable, while runtime/technical verification (build, real database migration, Android backup rules, device behaviour) waits for separate owner authorisation after the planning gate.
- Created `docs/27-PLANNING-COMPLETENESS-REVIEW.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, `docs/29-UX-DESIGN-APPROVAL-PACK.md`, `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`, and `docs/31-OWNER-PLANNING-DECISION-BATCH.md`. Expanded the architecture proposal with lifecycle refinements, restore-session/conflict concepts, a journal-habit link and notification-cancellation outbox; these remain proposed, not owner-approved.
- Existing repository source is native Kotlin/Compose with SQLite persistence, according to the earlier read-only audit. Build, tests, APK installation, physical-device behaviour and safe migration remain unverified. Do not state that the runtime has passed.
- Documentation updates were fetched before each write and re-fetched for verification. No application code or database was changed; no build, test, migration, APK installation, or device validation was run during this continuation.

### Next planning actions

1. Reconcile the structural refinements in `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md` against `docs/03-TECHNICAL-ARCHITECTURE.md`, including mask/mode constraints, foreign keys/indexes, transaction/rollback boundaries, portable-backup field mapping, conflict cases, and migration compatibility. Present the full written proposal to the owner; do not inspect/migrate the real database during this planning step.
2. Present the delegated reminder defaults and D-042 retrospective journal-prompt proposal for owner acceptance or explicit deferral; also resolve/defer the eight-second Undo window and whether individual numeric-entry deletion needs confirmation. AC-024 tracks the prompt proposal; these are planning decisions, not technical tests.
3. Turn the text sketches in `docs/29-UX-DESIGN-APPROVAL-PACK.md` into comparable visual/wireframe prototypes, then review navigation, Today hierarchy, Growth Mark, journal archive, typography/colour/theme, empty/error/destructive states and accessibility. Do not build app UI.
4. Close `docs/28-REQUIREMENT-TRACEABILITY.md` gaps (especially consolidated mood-insight fixtures and reminder/Undo/retrospective-prompt cases AC-022–AC-024) and complete heading-anchor, stale-claim, duplicate/conflict and supported-evidence review across current Markdown docs.
5. Walk through `docs/31-OWNER-PLANNING-DECISION-BATCH.md`: record actual accept/modify/defer choices in the decision log and reconcile every affected file. Keep Gate A–D closed until the owner explicitly approves the planning gate.
6. Only after separate explicit authorisation, begin the bounded native technical-validation stage described in the workflow and roadmap.


## Latest planning continuation — 2026-10-27

- Reconfirmed D-195 is already approved on `main`; the seven-bit weekday mask is not a new unresolved owner choice.
- Drafted comparable text wireframes in `docs/29-UX-DESIGN-APPROVAL-PACK.md`: three Today hierarchies, two navigation variants and three journal archive patterns. No rendered prototype, user study or design approval is claimed.
- Documented delegated reminder safety proposals in docs 23/05/26/07 and mapped them in doc 28: editable 21:00–08:00 local quiet hours, a shared three-notifications/day cap, collision/late-delivery revalidation, local-only personalisation evidence/rollback and a capped optional-tip sequence. Owner acceptance or explicit deferral remains pending; OS scheduling is unverified.
- Documented a proposed eight-second, single-mutation Undo in docs 01/03/26/07/30 and mapped it to AC-023. D-032 still requires owner review of the window and whether individual numeric-entry deletion requires confirmation; it is not an approved implementation contract.
- Reconciled traceability IDs: AC-022 remains restore conflict/interruption recovery; AC-023 is the Undo proposal. Removed the duplicate AC-022 row added during reconciliation and re-fetched the matrix to verify the IDs and section boundary.
- The schema closure document 30 exists and contains proposed table/index constraints, transactions, restore sessions/conflicts, notification cancellation outbox, backup mapping and migration design. It must still be reconciled and reviewed with the canonical architecture and owner-approved rules.
- No app code or live database was modified. No build, automated test, migration, APK installation, device validation or implementation audit was run. All acceptance cases remain planned, not executed. The planning gate remains not approved.

- Added a proposed D-042 retrospective journal prompt contract to docs 24/26/07, traced as AC-024: one non-blocking in-app prompt for the most recent fully completed local date without a saved entry; no backlog or OS notification; suppress repeat display for that date; do not stack it with comeback; reuse an existing same-date draft. The direction D-042 is approved, but cadence remains a proposal awaiting owner acceptance/deferral.
- Created `docs/31-OWNER-PLANNING-DECISION-BATCH.md` and linked it from the planning review/register. It consolidates D-195 and other settled policies, the proposed schema review, reminder/retrospective-prompt/Undo proposals, design review topics, safe v1 deferrals, and the separate post-gate validation boundary. The document records no new owner approval.
- Added a deterministic synthetic Mood Insights dataset and expected results for AC-009 in `docs/07-TESTING-AND-QUALITY.md`; linked expected behavior into `docs/28-REQUIREMENT-TRACEABILITY.md` and updated docs 27/31. This strengthens the plan only; no tests were executed.
- Added deterministic progress-band fixtures for AC-002 in docs 01/07/28/27. Classification uses the exact actual-to-target ratio before rounding display percentage, with examples for 100-, 10-, and 1-unit targets, including unreachable bands for tiny targets; fixtures are planned, not executed.
- Tightened the canonical architecture's D-195 schema invariant wording: each schedule kind now explicitly specifies its permitted target period and NULL/non-NULL weekday-mask combinations, rejecting unknown bits rather than trimming them. Re-fetched and verified the architecture, product specification, test plan, traceability matrix, planning review, decision register, schema closure checklist and handover updates. Relevant documentation commits include `0f92e122`, `69db52a5`, `3ff425be`, `4d0c94a7`, `1fd964a0`, `e13a41f6`, and `5b3adbe3`. No application code/database was changed; tests are still planned only.
- Added a proposed record-type restore-conflict matrix to `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`, applying D-094–D-102 to journal entries, tracking records, habit definitions/configuration, preferences, drafts and unsafe backup files. It is a conservative schema proposal, not new owner approval; doc 27/28/31 now identify owner review as remaining. Commit: `b00d18ec`.
- Added planned restore-conflict acceptance cases to `docs/07-TESTING-AND-QUALITY.md` and linked them to AC-014/AC-022 in `docs/28-REQUIREMENT-TRACEABILITY.md`; updated docs 27/31 to show that test planning exists but owner review of the proposed conflict mapping and unsafe “Keep both” cases remains open. Commit: `91a06593`.
- Added candidate privacy-screen and Backup & Restore disclosure copy in `docs/04-DATA-AND-PRIVACY.md`, distinguishing offline/account-free core use, user-controlled backup files, provider-backed syncing, phone migration, and in-app deletion versus copies in saved files. The wording is explicitly draft pending owner review and actual platform/data-flow verification. Commit: `3f2f1105`.
- Updated README.md to link the current planning review, traceability matrix, UX/design pack, schema-closure plan and owner decision batch. A relative-target check of README and docs 27–31 found no missing relative targets in those checked files; the full repository-wide post-edit link/heading audit remains open.


- Schema reconciliation continuation: reconciled docs 03 and 30 on durable restore-session lifecycle, atomic Replace/Merge completion, interrupted-restore recovery states, index notation, notification-cancellation outbox uniqueness and proposed global idempotency-key enforcement across `habit_entries` and `habit_day_outcomes`. Added planned failure/recovery cases to docs 07/28 and updated the planning readiness checklist in doc 27. This narrows the schema gaps but does not mean the complete schema has owner approval or that any tests were run.
- Decision-log rendering fix: re-fetched `docs/09-DECISION-LOG.md`, removed blank-line breaks between adjacent D-001–D-190 rows and added a proper five-column table header for D-191–D-196. A fresh check found 190 decision rows, all with five columns, rendered as two continuous table blocks; no decision content or IDs were changed.
- Traceability/semantic review follow-up: corrected AC-009's synthetic October month fixture from Calm 2 to Calm 3 (J1, J3, J5); J5 was a primary Calm entry within October. Consolidated the duplicate Mood Insights comparison-control section in `docs/24-COMEBACK-EXPERIENCE.md`. Both are planning-document corrections; no test was run and no application code changed.
- Effective-date consistency fix: docs 03/30 previously described habit versions as immutable once effective, which did not precisely express D-194's same-day edit exception. Both now allow replacing today's version only before qualifying activity is recorded on that date; after activity, the flow offers Apply tomorrow and preserves the current-day/past history. Added the exact behavior to the planned test cases in doc 07. No test was run.
- Conflict/outbox design refinement: docs 03/30 now require a single canonical `ACTIVITY_IDEMPOTENCY_KEY` conflict for cross-table collisions, using the collided key as `record_key`, and require non-null opaque platform tag/notification-ID pairs in the cancellation outbox. Planned tests and traceability now cover these rules; they remain proposed schema requirements, not runtime evidence.
- Restore-conflict identity refinement: docs 03/30 now distinguish a canonical cross-table `ACTIVITY_IDEMPOTENCY_KEY` conflict from a same-stable-ID record conflict and require overlapping constraints for the same two payloads to be consolidated into one review item. Planned cases were added to docs 07/28 and the owner review batch doc 31. These are proposed schema/recovery rules only; no tests were executed.
- Documentation link/reference validation: parsed internal Markdown links across all 37 Markdown files and found no missing relative targets or heading fragments (there are no cross-file fragment links in the current set). All current external URLs in docs 13/18 and the repository links in the handover/workflow were opened successfully, including Google Play listing/screenshot assets, Android signing, AdMob, Play Billing, Android contrast guidance and WCAG; Expo-specific references remain explicitly historical. Updated doc 27 to mark the link/external-URL pass complete. The broader stale-claim/semantic contradiction pass remains open.
## Planning continuation — delegated defaults and current remaining work — 2026-10-27

- Re-fetched `main` and confirmed D-195 is already **Approved** in `docs/09-DECISION-LOG.md`; no duplicate decision was added. The fixed mask is documented in the data contracts and proposed schema (Monday=1 through Sunday=64; valid non-empty masks 1–127; selected-day mask required only for `SELECTED_WEEKDAYS`; flexible-weekly preferred days are optional guidance).
- Added D-196 as an approved planning-process rule: delegate conservative, reversible micro-decisions; reserve owner review for product scope/philosophy, privacy/security, data integrity, accessibility, monetisation, irreversible choices, conflicting policy, and owner-taste/identity. Reconciled this rule into the decision register, planning completeness review, owner decision batch, project charter and AI Studio workflow. It does not authorise application implementation or technical validation.
- Reminder provenance check: older entries in docs 09/23 previously treated 9:00 a.m. as the weekly reflection time. The catch-up/deduplication rule remains approved, but the weekly reflection clock time is now marked for separate confirmation; it must not be inferred from the approved per-habit 9:00 a.m. default. See D-045, docs 23, 25, 27 and 31.
- D-010 now records the owner's preferred *candidate* monetisation approach (subtle banners on suitable secondary screens, optional user-initiated rewarded extras, and a possible one-time Supporter Pack). This is not approval to ship monetisation in initial v1; no SDK, ad placement, purchase provider or monetisation implementation is authorised by that preference.
- **Planning-only status:** the weekday-mask schema rule is documented; no application code/schema/migration was changed, and no build, test, APK install, device check or implementation audit was performed in this continuation. The planning gate remains not approved.

### Consolidated planning work still required

1. Reconcile `docs/03-TECHNICAL-ARCHITECTURE.md` with `docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`, review all proposed tables/relationships/constraints/indices/transactions, versioned portable-backup mapping, restore-conflict safe options, deletion/outbox behaviour and migration/rollback acceptance. Present one coherent written schema to the owner; do not inspect or migrate the live database at this stage.
2. Use `docs/31-OWNER-PLANNING-DECISION-BATCH.md` for a single grouped review: (a) proposed bounded Undo and single numeric-entry deletion confirmation; (b) shared reminder safety/personalisation defaults; (c) retrospective journal-prompt cadence; (d) rendered UX prototypes and design choices/deferrals; (e) final privacy/export/delete wording; (f) whether to include the preferred monetisation candidate in v1 or defer it; and (g) explicit planning-gate approval. D-196 means routine details are delegated, not additional owner questions.
3. Complete rendered low-fidelity comparison prototypes and usability/accessibility review; record either owner decisions or deliberate v1 deferrals for navigation, Today hierarchy, Growth Mark, journal archive, typography/colour/theme, and comeback presentation.
4. Close the remaining requirement-to-test traceability gaps and semantic stale-claim/duplicate/conflict/supported-evidence review. The internal relative-link, heading-fragment and external-URL checks are complete across all 37 Markdown files with no broken relative targets/fragments; repeat them if later changes alter links or headings. All acceptance cases remain planned, not executed.
5. Keep post-gate native build/test/device/schema/migration/backup/notification validation separate. It requires explicit authorisation after the planning gate; feature implementation is a later, separate stage.

The canonical readiness snapshot is `docs/27-PLANNING-COMPLETENESS-REVIEW.md`; the decision log remains authoritative. Older historical handover entries should not be treated as current when they conflict with that snapshot.


## Portable backup schema reconciliation — 2026-10-27

- Added and verified a field-level portable-backup mapping in doc 30 and aligned docs 03/07/28/27/31: entity fields and relationships, preservation of stable IDs/local-date keys/idempotency, distinct format/schema-version compatibility rules, the minimum portable context for unresolved restore conflicts, fresh local restore-session ID remapping, constrained “Keep both” and idempotency/note-conflict handling, and exclusions for device-only notification/cancellation state, provider grants, caches and secrets. Removed the superseded category-only list to avoid two competing mapping descriptions. Acceptance coverage is planned only; owner review of the proposed compatibility boundary and unsafe conflict choices remains open.
- This is a documentation-only refinement. No app code or live database was changed; no build, test, migration, APK install, device validation or implementation audit was performed.
- A further consistency pass reconciled restore-conflict lifecycle: conflict rows exist only while unresolved (no separate status column); the parent restore session uses an explicit proposed state and remains `COMPLETED_WITH_CONFLICTS` until its last child conflict is resolved, then transitions to `COMPLETED` atomically. Updated docs 03/30/07/28 and recorded the discrepancy as R-016 in doc 25. These are proposed schema rules with planned tests, not an implementation finding.
