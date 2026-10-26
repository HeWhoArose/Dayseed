# Progress and Handover

**Last updated:** 2026-10-26  
**Status:** Planning phase; planning gate not approved; feature implementation is not authorised. Existing source files/preview are evidence for the later audit, not proof of a verified build or approval to add features.

## Repository status

Repository: https://github.com/HeWhoArose/Dayseed  
Canonical branch: `main`. GitHub is the source of truth for planning and future implementation, regardless of the active coding environment.

## Current state

- Product direction and core v1 boundaries: documented as a planning baseline; some product semantics remain open.
- UX directions and information architecture: explored, not approved.
- Existing repository source: native Kotlin + Jetpack Compose, `SQLiteOpenHelper`, repository, ViewModel/StateFlow, and screens are present. Treat the project as being in the planning phase for our workflow; do not add features yet. Source/preview presence does not verify build, tests, installation, or device behavior. The later audit must reconcile observed source with the approved plan.
- Growth Mark and return experience: concepts, definitions, sample copy for 3/14/90 days, zero-completion state, and test cases documented in [docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md). Not approved.
- Journal archive: timeline/calendar/editorial comparison and task-based test plan documented in [docs/16-JOURNAL-ARCHIVE-RESEARCH.md](16-JOURNAL-ARCHIVE-RESEARCH.md). Hybrid timeline + calendar is only a proposal.
- Typography, light/dark candidate palettes, contrast checks, font licensing notes, and small-screen tests documented in [docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md).
- DESIGN.md is now specified as the sole authoritative token/component reference; candidate values remain unapproved.
- Competitor screenshot evidence for Loop Habit Tracker and Daylio is recorded in [docs/18-COMPETITOR-SCREENSHOT-AUDIT.md](18-COMPETITOR-SCREENSHOT-AUDIT.md). Public listing screenshots were inspected; current live app flows have not been tested.
- AI Studio/GitHub workflow and native-app feasibility-spike acceptance criteria are in [docs/19-AI-STUDIO-GITHUB-WORKFLOW.md](19-AI-STUDIO-GITHUB-WORKFLOW.md).
- Data/privacy baseline and portable-backup decisions are substantially documented. Still open: Android platform-backup inclusion/exclusion policy, central date/time policy, final deletion/export wording, and implementation-level validation.
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

1. Audit all documentation for contradictions, dead links, duplicates, stale claims, and missing acceptance criteria.
2. Reconcile the remaining high-impact edge cases: explicit skipped-state semantics, local date/time policy, habit deletion/history, Android platform backup, and exact export/delete/privacy wording. Treat approved schedule, journal multiplicity, backup scope, and reminder rules as baselines, not fresh open questions.
3. Create comparable low-fidelity screen samples or wireframes for Today, Growth Mark, comeback return, journal archive, and settings/privacy; do not create app code as a substitute.
4. Inspect current live competitor flows on an Android device where possible; until then, keep conclusions limited to publisher screenshots/listing copy.
5. Run typography/contrast and enlarged-text checks on prototype screens; update DESIGN.md only when a token is approved.
6. Complete the planning gate and record explicit owner approval before authorising the bounded native-app feasibility spike.
7. After the spike passes and stack approval is explicit, begin feature implementation in small testable phases.

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
- Clarified that 9:00 a.m. local time on the configured week-start day is approved for weekly reflection; remaining reminder questions concern delivery edge cases, not the chosen time. Commit: `5c381230c6bff8194a6c6948684868bf3d022edc`.
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
