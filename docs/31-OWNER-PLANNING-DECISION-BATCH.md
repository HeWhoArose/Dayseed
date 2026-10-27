# Dayseed Owner Planning Decision Batch

**Review date:** 2026-10-27  
**Status:** Consolidated review aid; it does not record new owner approvals and does not open the planning/implementation gate.  
**Purpose:** Make the remaining product choices reviewable in one pass instead of asking about each tiny detail in separate messages.

**Authority:** The Decision Log remains authoritative. See docs/27-PLANNING-COMPLETENESS-REVIEW.md, docs/28-REQUIREMENT-TRACEABILITY.md, docs/29-UX-DESIGN-APPROVAL-PACK.md, and docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md.

## 1. What is already settled — do not reopen casually

- **Platform and project:** D-009 approves continuing the existing native Kotlin + Jetpack Compose Android project. Source/build/device readiness is not verified.
- **Core scope:** local-first/offline core; no account or network required for core habits, journal, history, insights and comeback. No online account/sync, Dayseed-managed cloud backup, AI processing, ads, purchases, subscription or social leaderboard in initial v1 without a separate approval.
- **Privacy/migration policy:** D-186 excludes sensitive Dayseed data from Android platform Auto Backup/device transfer by default. Portable Dayseed backup/restore is a separate, user-controlled path. Actual OS configuration still requires post-gate verification.
- **Schedule/data semantics:** D-187–D-194 are recorded, including stable local-date keys, effective-dated configuration, derived missed status, no explicit habit-day Skip, separate numeric and non-numeric records, stable UUID/idempotency rules, structured schedules and repeated Apply-today behaviour.
- **Measured progress labels:** keep the approved D-040 target-relative bands; classify using the exact actual/target ratio before rounding a displayed percentage. Docs 01/07/28 now include deterministic planned examples for targets 100, 10 and 1, including unreachable bands for tiny targets; no tests have run.
- **Delegation rule:** D-196 asks the assistant to default routine, reversible details with a documented rationale and checks, and to escalate consequential product, privacy, data-integrity, accessibility, monetisation, and owner-taste choices. This process rule does not open the planning/implementation gate.
- **Monetisation:** D-010's preferred candidate is a restrained hybrid: subtle banners on suitable secondary screens, optional user-initiated rewarded ads for nonessential cosmetics/content, and a possible one-time Supporter Pack. That preference is recorded, but monetisation is **not approved for initial v1**; do not add SDKs, placements or purchase flows by assumption. Present one explicit v1 accept/defer choice with privacy/policy implications.
- **Weekday mask:** D-195 is already **Approved**. Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. It is fixed regardless of locale/week start; selected-weekday masks are non-empty and valid; the other schedule types do not require scheduled weekdays; preferred days for flexible weekly targets are guidance only. Acceptance tests are planned, not executed.
- **Journal core:** multiple entries per local date; explicit Save; edits preserve identity/creation time; saved journal delete requires confirmation; journal activity never changes habit completion; unfinished comeback drafts remain local and separate from saved entries.
- **Weekly-goal rhythm and comeback triggers:** their important trigger/preference/suppression rules are recorded. See docs 23 and 24 for detailed contracts.

## 2. Owner review: accept, modify, or defer these proposals

These are recommendations, **not approved policy**. The owner can accept the proposal, specify a change, or defer the corresponding feature/rule from v1 where safe.

### A. Complete relational schema proposal

**Recommendation:** Review docs 03 and 30 as one proposal and approve only once the two agree on every field, nullability/check, foreign key, index, transaction, backup mapping and recovery case.

Still to reconcile before asking for schema approval:
- All tables and relationships, including effective lifecycle versions, journal drafts, restore-session/conflict records, and notification-cancellation outbox.
- Exact nullability and checks for tracking mode, target period, weekday masks, values, local-date keys, outcomes and idempotency keys.
- Index/uniqueness rules, cross-table mode validation, transaction/rollback map and permanent-deletion ownership.
- Field-level portable-backup mapping for each entity, relationships and excluded device-only fields; format/schema version and integrity/count boundary; idempotency-key and habit/date-note collisions; required restore-conflict parent foreign key/retention and parent-session remapping; merge conflicts where “Keep both” cannot coexist safely; interruption recovery and migration/quarantine behaviour. Docs 03/30 and planned cases in docs 07/28 now draft this contract. Owner review must keep unresolved/unsafe cases visible rather than silently selecting a winner.
- Align schema and planned tests with AC-001, AC-003–AC-008, AC-014, AC-019, AC-020 and AC-022.

**Boundary:** This is written-model review only. Do not inspect or modify the live database, build, migrate, or run implementation tests as part of this step.

### B. Habit record Undo

**Proposal in docs 01/03/26/30; traceability AC-023:** one 8-second in-session Undo after a successfully committed single-record create/log/complete/edit. It targets the exact stable ID/version, only reverses a safe inverse, and disappears after timeout/process death. Confirmed journal deletion, permanent habit deletion, delete-all and Replace/Merge restore are not snackbar-undoable. Normal History correction remains available.

- [ ] Accept the 8-second Undo proposal.
- [ ] Modify the window/behaviour.
- [ ] Defer Undo from v1 and explicitly define the alternative protection.

**Related choice:** D-032 still leaves open whether deleting an individual numeric session needs a confirmation dialog. Recommendation: if the 8-second Undo is accepted, use a clear contextual Delete action plus the immediate Undo affordance (no second confirmation for a single selected numeric session); if Undo is deferred, require confirmation. This is a proposal for the owner to accept/modify, not current policy. Keep journal deletion confirmation and permanent-habit deletion confirmation unchanged.

### C. Shared reminders and local timing personalisation

**Proposal in docs 23/05/26/07; traceability AC-011/AC-012:**
- Editable quiet hours defaulting to 21:00–08:00 local.
- Shared cap of three OS notifications per local date and at most one ordinary reminder per habit/date.
- Combine eligible events within 15 minutes where the combined message and tap destination remain clear; prioritise explicit per-habit reminders, weekly-goal-reached acknowledgement, week-start reflection, inactivity check-in, then optional partial tips.
- Re-check opt-in, OS permission, current date/week, habit/goal/activity state and suppression immediately before every send. Stable keys prevent duplicates. Do not backfill stale daily reminders.
- Learn only from local habit-record timestamps; never inspect journal text/moods or send timing data over the network. Proposed threshold: at least seven positive activity timestamps on at least five dates in 14 days, 70% in a two-hour band; candidate time is median plus 30 minutes rounded up to a 15-minute boundary. Never auto-override a manually chosen time. Validate for 28 days, and revert if a stable cluster is no longer supported.
- Optional partial-progress tips: at most three over the following seven days, at least 24 hours apart, maximum one/day; suppress after target-met, dismissal or opt-out. Phone tips require a separate setting.

- [ ] Accept these defaults for planning, including confirmation of the weekly reflection time separately from the per-habit 9:00 a.m. default.
- [ ] Accept with changes (specify which).
- [ ] Defer personalised timing and/or partial-progress tips from initial v1.
- [ ] Explicitly defer the complete reminder enhancement until a later version, retaining only already-approved simple reminders/weekly-goal rules.

These defaults are not approved, the weekly reflection time remains to be separately confirmed, and Android delivery/reboot/permission behaviour remains unverified.

### D. Retrospective journal prompt (D-042)

**Proposal in docs 24/26/07; traceability AC-024:** on the first eligible app opening, offer one non-blocking in-app prompt for the most recent fully completed local day without a saved journal entry. Show at most once for that date; no OS notification, no backlog, no repeat after display/dismiss/“Not now”/leaving, no stacking with a 14-/30-day comeback screen. Reuse an existing draft for that date. Saving remains explicit and never changes habit status.

- [ ] Accept this cadence.
- [ ] Modify it.
- [ ] Defer the retrospective prompt from v1 while retaining ordinary Journal and daily writing prompts.

D-042 approved the optional direction, not this exact cadence.

### E. Visual direction, navigation and Growth Mark

**Proposal in doc 29:** prototype the user’s prior “a little of Concept C” direction as an action-first Today screen with a modest reflective element, compared with reduced-effort-controls and more-reflective variants. Compare four primary destinations (Today/History/Insights/Settings, Journal accessible from Today and History) with a dedicated Journal tab. Compare timeline-first, hybrid timeline/calendar and calendar-first archive. Test “Collected moments” against a plain labelled summary; no arbitrary XP, score or decorative growth is implied.

- [ ] Review the rendered comparative prototypes when prepared and choose, modify or defer each unsettled choice.
- [ ] Until reviewed, keep D-008 visual direction, D-016 Growth Mark, D-017 comeback presentation, D-018 archive pattern, D-019 typography/colour and Journal/History navigation marked open.
- [ ] Keep platform typography as comparison baseline; do not mark a typeface, colour or light/dark theme as approved before contrast/accessibility review.

Text wireframes in doc 29 exist, but they are not rendered prototypes, user-test evidence or approved UI.

### F. Other safe deferrals and policy text

- **Retrospective prompt and optional partial tips:** can be deferred from v1 explicitly rather than shipping unspecified notification patterns.
- **XP/rank, social/leaderboards, journal photos/audio, AI analysis and online features:** remain outside v1 unless separately approved.
- **Monetisation:** no v1 ads, purchases or subscriptions approved. Keep out by default; no pricing decision is needed to finish core planning.
- **Minimum Android API/device matrix:** do not invent a version from assumption. Capture the actual project/toolchain and supported-device evidence during the separately authorised technical-validation stage.
- **Privacy language:** candidate screen copy is drafted in docs/04 for owner review. Do not make blanket “never leaves this device,” encryption, or fully disabled platform-backup claims until actual manifest/data-extraction rules, export destinations and network/SDK behaviour have been verified. Wording distinguishes local use, portable backup, Android platform backup and user-selected provider syncing.

## 3. What still remains in the planning phase

1. Reconcile docs 03 and 30 and produce the complete written schema for owner review.
2. Get the owner’s accept/modify/defer decisions for B–D above, including the numeric-session deletion confirmation rule; also record the remaining privacy wording and monetisation v1 decision. Use D-196 to delegate routine defaults instead of opening separate micro-questions; record owner choices in docs 09 and reconcile affected documents.
3. Prepare comparable rendered low-fidelity prototypes based on doc 29; conduct task and accessibility review, then record design choices or explicit deferrals.
4. Close doc 28 traceability gaps, especially owner acceptance/deferment of reminder/prompt/Undo proposals, schema failure cases and UX/accessibility review. Doc 07 now includes planned record-type restore-conflict cases for AC-014/AC-022, plus deterministic fixtures for AC-002 progress-band boundaries and AC-009 Mood Insights counts/date-week boundaries. Review for remaining edge cases. All cases remain planned, not executed.
5. Complete the cross-file heading-anchor, external-URL, outdated-claim, duplicate/conflict and supported-evidence review. The current relative Markdown file-target and same-file heading-fragment scan covers all 37 current Markdown documents and found no missing targets/fragments; repeat after further documentation edits.
6. Check user-facing privacy/export/delete wording against the written rules, keeping unverified runtime claims out.
7. Prepare one final planning walkthrough that includes all consequential choices and safe deferrals, then obtain the owner’s explicit Gate A–D approval in docs/19-AI-STUDIO-GITHUB-WORKFLOW.md. Routine details should be delegated under D-196 rather than turned into additional owner questions.

## 4. Separate post-planning technical validation — not currently authorised

Only after explicit owner authorisation: inspect the current native source, Gradle/toolchain and actual SQLite schema; reconcile source; run build/tests with synthetic data; inspect migration and rollback; verify Android platform-backup exclusion, offline persistence, notification delivery, backup/restore/export/delete; and install on a physical device. Record exact evidence and limitations. Feature implementation is a later stage and is not authorised by approval of the schema or stack alone.

**Current truth:** planning has progressed well beyond brainstorming, but the planning gate remains closed. No application code/database change, build, test, migration, APK installation, device validation or implementation audit is claimed by this document.
