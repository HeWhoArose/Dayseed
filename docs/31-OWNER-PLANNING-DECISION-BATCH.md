# Dayseed Owner Planning Decision Batch

**Review date:** 2026-10-28  
**Status:** Consolidated review aid; it does not record new owner approvals and does not open the planning/implementation gate.  
**Purpose:** Make the remaining product choices reviewable in one pass instead of asking about each tiny detail in separate messages.

**Authority:** The Decision Log remains authoritative. See docs/27-PLANNING-COMPLETENESS-REVIEW.md, docs/28-REQUIREMENT-TRACEABILITY.md, docs/29-UX-DESIGN-APPROVAL-PACK.md, and docs/30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md.

## 1. What is already settled — do not reopen casually

- **Platform and project:** D-009 approves continuing the existing native Kotlin + Jetpack Compose Android project. Source/build/device readiness is not verified.
- **Core scope:** local-first/offline core; no account or network required for core habits, journal, history, insights and comeback. No online account/sync, Dayseed-managed cloud backup, AI processing, ads, purchases, subscription or social leaderboard in initial v1 without a separate approval.
- **Privacy/migration policy:** D-186 excludes sensitive Dayseed data from Android platform Auto Backup/device transfer by default. Portable Dayseed backup/restore is a separate, user-controlled path. Actual OS configuration still requires post-gate verification.
- **Schedule/data semantics:** D-187–D-195 are recorded, including stable local-date keys, effective-dated configuration, derived missed status, no explicit habit-day Skip, separate numeric and non-numeric records, stable UUID/idempotency rules, structured schedules, repeated Apply-today behaviour and the fixed weekday-mask encoding. D-197 separately establishes the delegated fail-closed migration fallback.
- **Measured progress labels:** keep the approved D-040 target-relative bands; classify using the exact actual/target ratio before rounding a displayed percentage. Docs 01/07/28 now include deterministic planned examples for targets 100, 10 and 1, including unreachable bands for tiny targets; no tests have run.
- **Delegation rule:** D-196 asks the assistant to default routine, reversible details with a documented rationale and checks, and to escalate consequential product, privacy, data-integrity, accessibility, monetisation, and owner-taste choices. This process rule does not open the planning/implementation gate.
- **Monetisation:** D-010's preferred candidate is a restrained hybrid: subtle banners on suitable secondary screens, optional user-initiated rewarded ads for nonessential cosmetics/content, and a possible one-time Supporter Pack. That preference is recorded, but monetisation is **not approved for initial v1**; do not add SDKs, placements or purchase flows by assumption. Present one explicit v1 accept/defer choice with privacy/policy implications.
- **Weekday mask:** D-195 is already **Approved**. Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. It is fixed regardless of locale/week start; selected-weekday masks are non-empty and valid; the other schedule types do not require scheduled weekdays; preferred days for flexible weekly targets are guidance only. Acceptance tests are planned, not executed.
- **Journal core:** multiple entries per local date; explicit Save; edits preserve identity/creation time; saved journal delete requires confirmation; journal activity never changes habit completion; unfinished comeback drafts remain local and separate from saved entries.
- **Weekly-goal rhythm and comeback triggers:** their important trigger/preference/suppression rules are recorded. See docs 23 and 24 for detailed contracts.

## 2. Owner review: accept, modify, or defer these proposals

The batch separates **owner-level choices** from **delegated defaults**. The schema proposal and material design, privacy and scope choices require owner review. The weekly reflection's local clock time is also an explicit owner choice; it must not be inherited from the per-habit default. Undo, routine reminder coordination and retrospective-prompt cadence use the documented conservative defaults under D-196; they are not separate approval blockers, though the owner may override or safely defer them in this same review.

### A. Complete relational schema proposal

**Recommendation:** The written reconciliation of docs 03 and 30 is complete as a planning draft. They now agree on lifecycle authority, schedule/mask constraints, draft save-intent IDs, table/index proposals, global idempotency handling, restore conflict/session lifecycle, portable-backup mapping, transaction boundaries and fail-closed migration. This is still a proposed schema; no database code or migration is authorised.

The remaining owner-level review is to confirm the coherent proposal, especially:
- All proposed tables and relationships, including effective lifecycle versions, journal drafts with stable save-intent IDs, restore-session/conflict records, and notification-cancellation outbox.
- Exact nullability and checks for tracking mode, target period, weekday masks, values, local-date keys, outcomes and idempotency keys, including the proposed cross-table guard enforcing D-190's global key scope. D-197's fail-closed fallback applies if a source row cannot be mapped losslessly: rollback all migration work and preserve the original schema/version rather than dropping/coercing it or assuming a quarantine table.
- Verify the restore-session lifecycle: durable `IN_PROGRESS` metadata before the live-data transaction; data, unresolved conflicts and final session state commit atomically; observed rollback becomes `FAILED_SAFE`, while a process interruption is reconciled to `RECOVERY_REQUIRED` only after verifying the live transaction did not commit.
- Review the record-type conflict mapping: a cross-table idempotency collision must become one canonical `ACTIVITY_IDEMPOTENCY_KEY` conflict keyed by the collided idempotency key; do not offer an unsafe “Keep both” action or create separate conflicts by source-table name. The notification-cancellation outbox requires non-null opaque platform tag/ID values and a unique pair; these are proposed schema details, not implementation evidence.
- Index/uniqueness rules, cross-table mode validation, transaction/rollback map and permanent-deletion ownership.
- Proposed restore-conflict lifecycle is now explicit: conflict-row existence means unresolved (no redundant status column); successful resolution deletes that row atomically with applying the selected outcome; the durable parent session remains `COMPLETED_WITH_CONFLICTS` until its last conflict resolves, then becomes `COMPLETED`. Canonical identity is now explicit too: cross-table non-null idempotency collisions use one `ACTIVITY_IDEMPOTENCY_KEY` conflict keyed by the collided key; when the same two payloads also differ under one stable ID, consolidate that overlap into one record-identity conflict and revalidate all constraints on resolution rather than showing duplicate conflict rows.
- Field-level portable-backup mapping for each entity, relationships and excluded device-only fields; format/schema version and integrity/count boundary; idempotency-key and habit/date-note collisions; required restore-conflict parent foreign key/retention and parent-session remapping; and merge conflicts where “Keep both” cannot coexist safely. Docs 03/30 and planned cases in docs 07/28 now draft this contract. For any mapping/conflict that cannot be resolved without loss, the default is to stop/retain the conflict rather than select a winner or fabricate a record. Migration is proposed as all-or-nothing and rolls back if any legacy row cannot be mapped losslessly; no quarantine table is assumed.
- Align schema and planned tests with AC-001, AC-003–AC-008, AC-014, AC-019, AC-020 and AC-022.

**Boundary:** This is written-model review only. Do not inspect or modify the live database, build, migrate, or run implementation tests as part of this step.

### B. Habit record Undo — delegated default under D-196

**Default across docs 01/03/26/28/30; AC-023:** one 8-second in-session Undo after a successfully committed single-record create/log/complete/edit. It targets the exact stable ID and expected version, refuses unsafe inverses after a newer change, and disappears after timeout/process death. For deleting one selected numeric/duration entry, use a clear contextual Delete action plus immediate Undo without a second confirmation dialog. Confirmed journal deletion, permanent habit deletion, delete-all and Replace/Merge restore remain explicitly confirmed/non-undoable.

This is a reversible interaction detail and is **not a required owner checkbox**. The owner may override the default in the consolidated review. AC-023 remains planned, not executed; this default is not evidence of implementation.

### C. Shared reminders and local timing personalisation — defaults under D-196; weekly reflection time needs owner choice

**Defaults documented in docs 09/23/05/26/28; AC-011/AC-012:**
- Ask once during setup whether reminders are wanted; No creates no reminders. Each habit's reminder time defaults to 9:00 a.m. local time; manually chosen times remain protected from automatic adjustment.
- Editable quiet hours default to 21:00–08:00 local; shared cap of three Dayseed OS notifications per local date and at most one ordinary reminder per habit/date.
- Combine eligible events due within 15 minutes when one clear destination preserves their meaning; otherwise stagger within the daily cap. Priority: explicit per-habit reminder, weekly-goal-reached acknowledgement, week-start reflection, inactivity check-in, optional partial-progress tips.
- Re-check global opt-in, Android permission, current local date/week, effective schedule, activity, quiet hours and suppression immediately before sending. Stable keys prevent duplicates. Do not backfill stale daily reminders.
- Timing learning uses only local habit-record timestamps, never journal text/moods or network uploads. For system-managed daily habits, require at least seven positive activity timestamps on at least five local dates in 14 days, with at least 70% in a two-hour band; candidate time is median plus 30 minutes rounded up to a 15-minute boundary. Never auto-override a manual time. Validate for 28 days and revert if the evidence no longer supports the cluster.
- **Owner choice still required:** select the weekly reflection's local clock time for the configured week-start day. Do not infer or inherit the per-habit 9:00 a.m. default. D-045/D-046's trigger, catch-up, deduplication and no-backlog rules remain recorded.
- Optional partial-progress tips default to at most three over seven days, at least 24 hours apart and maximum one/day; suppress after target-met, dismissal or opt-out. Phone tips require a separate opt-in. If tips are unwanted for v1, the safe default is in-app only or omit the automated sequence.

The reminder coordination, local timing-learning and tip-cadence details above are conservative planning defaults, not proof of Android delivery, and do not require separate owner approval by default. The weekly reflection clock time is the exception: choose it as one explicit item in this grouped review, separately from per-habit reminders. Permission, reboot, clock/timezone, scheduling and deduplication behaviour remain post-gate technical-validation work; planned tests are not executed.

### D. Retrospective journal prompt (D-042) — delegated default under D-196

**Default in docs 09/24/26/28; AC-024:** on the first eligible app opening, offer one non-blocking in-app prompt for the most recent fully completed local date only if that date has no saved journal entry. Show at most once for that date; no OS notification, backlog or nagging; suppress repeat after display/dismiss/“Not now”/save; reuse a same-date draft; do not stack with a qualifying 14-/30-day comeback experience. Show the target date clearly, require normal explicit Save, and never change habit status/progress.

This routine cadence is delegated, reversible and not a separate Gate A blocker. The owner may override it or defer the prompt from v1 during the grouped review. AC-024 remains planned, not executed.

### E. Visual direction, navigation and Growth Mark

**Proposal in doc 29:** prototype the user’s prior “a little of Concept C” direction as an action-first Today screen with a modest reflective element, compared with reduced-effort-controls and more-reflective variants. Compare four primary destinations (Today/History/Insights/Settings, Journal accessible from Today and History) with a dedicated Journal tab. Compare timeline-first, hybrid timeline/calendar and calendar-first archive. Test “Collected moments” against a plain labelled summary; no arbitrary XP, score or decorative growth is implied.

- [ ] Review all three rendered comparative SVG boards linked from doc 29 and choose, modify or explicitly defer each unsettled design choice.
- [ ] Until reviewed, keep D-008 visual direction, D-016 Growth Mark, D-017 comeback presentation, D-018 archive pattern, D-019 typography/colour and Journal/History navigation marked open.
- [ ] Keep platform typography as comparison baseline; do not mark a typeface, colour or light/dark theme as approved before contrast/accessibility review.

Three rendered planning boards now exist: `docs/prototypes/dayseed-today-variants.svg` compares W1/W2/W3 Today layouts, and `docs/prototypes/dayseed-navigation-archive-variants.svg` compares N1/N2 navigation plus A1/A2/A3 archive layouts; `docs/prototypes/dayseed-growth-mark-variants.svg` isolates G1/G2/G3 monthly-summary representations using the same data and destination. They use synthetic data and are not user-test evidence or approved UI. Review all variants against the same task/accessibility scorecard before owner selection.

### F. Other safe deferrals and policy text

- **Retrospective prompt and optional partial tips:** can be deferred from v1 explicitly rather than shipping unspecified notification patterns.
- **XP/rank, social/leaderboards, journal photos/audio, AI analysis and online features:** remain outside v1 unless separately approved.
- **Monetisation:** no v1 ads, purchases or subscriptions approved. Keep out by default; no pricing decision is needed to finish core planning.
- **Minimum Android API/device matrix:** do not invent a version from assumption. Capture the actual project/toolchain and supported-device evidence during the separately authorised technical-validation stage.
- **Privacy language:** candidate screen copy is drafted in docs/04 for owner review. Do not make blanket “never leaves this device,” encryption, or fully disabled platform-backup claims until actual manifest/data-extraction rules, export destinations and network/SDK behaviour have been verified. Wording distinguishes local use, portable backup, Android platform backup and user-selected provider syncing.

## 3. What still remains in the planning phase

1. Written reconciliation of docs 03 and 30 is complete. Present the coherent schema package for owner review of the format/schema compatibility boundary, safe conflict-resolution options and the full proposed model. D-197's all-or-nothing, fail-closed migration fallback is the current delegated safety default. Do not inspect or migrate the real database during this planning review.
2. Review delegated defaults only if you want to override them; no separate Undo/reminder/prompt decisions are required by default. The genuine owner-level choices are the remaining schema/restore-compatibility approval, visual/navigation/accessibility decisions after prototypes, final privacy wording (without unsupported runtime claims), whether to keep all monetisation out of initial v1 (recommended default), and explicit planning-gate approval. Record any overrides or consequential choices in docs 09 and reconcile affected documents.
3. Review the three rendered SVG comparison boards linked from doc 29 against the same core task and accessibility scorecard, then record design choices or explicit deferrals. Do not treat mockups as usability evidence.
4. Close doc 28 traceability gaps, especially schema failure cases, recovery/backup compatibility, privacy and UX/accessibility coverage. AC-011/AC-012/AC-023/AC-024 now use delegated defaults under D-196 unless the owner overrides them. Doc 07 includes planned restore-conflict cases for AC-014/AC-022, deterministic AC-002 progress-band fixtures and AC-009 Mood Insights counts/date-week boundaries. Review any remaining edge cases; all cases remain planned, not executed.
5. The internal Markdown-link, heading-fragment and external-URL audit is complete across all 37 Markdown files, with no missing relative targets/fragments and all current external URLs resolving at review time. Finish the remaining semantic review for stale status claims, duplicate/conflicting statements and unsupported evidence, then rerun link checks only if further documentation edits affect links or headings.
6. Check user-facing privacy/export/delete wording against the written rules, keeping unverified runtime claims out.
7. Prepare one final planning walkthrough that includes all consequential choices and safe deferrals, then obtain the owner’s explicit Gate A–D approval in docs/19-AI-STUDIO-GITHUB-WORKFLOW.md. Routine details should be delegated under D-196 rather than turned into additional owner questions.

## 4. Owner response sheet — choices not yet recorded

This sheet makes the consequential choices answerable in one pass. The recommendations are defaults for review, not approvals. Reply in chat or annotate this document using **Accept**, **Modify**, or **Defer** for each item. “Defer” means keep the relevant feature/decision out of v1 or leave the design unapproved; it must not be silently treated as acceptance.

| Ref | Owner choice | Recommended starting point | Response |
|---|---|---|---|
| O-1 | Proposed relational schema | Accept the reconciled written model as the planning baseline, subject to the explicit compatibility/conflict rules below. This does not approve a live schema change or migration. | **Open** — Accept / Modify / Defer |
| O-2 | Portable backup compatibility and restore conflicts | Accept only the current format/schema, plus older versions with a documented **lossless** converter; reject unsupported versions before any live-data mutation. Keep journal entries as separate IDs if choosing “both”; choose one live habit/date note; never add conflicting numeric activity together; leave unsafe collisions unresolved and never offer “Keep both” where constraints cannot support it. | **Open** — Accept / Modify / Defer |
| O-3 | Today layout | W1 Action-first is the recommended starting point: keep Complete and Record distinct for measured habits; leave writing optional. W2 and W3 remain alternatives. A selection is provisional until task/accessibility review. | **Open** — W1 / W2 / W3 / Modify / Defer |
| O-4 | Primary navigation | N1 four destinations (Today / History / Insights / Settings), with an obvious Journal entry point on Today and Journal archive access from History, is the recommended starting point. N2 adds a dedicated Journal destination. | **Open** — N1 / N2 / Modify / Defer |
| O-5 | Journal archive | A2 hybrid timeline + compact date jump/calendar is the recommended starting point; compare the same find-by-date, search, edit and delete tasks against A1 timeline-first and A3 calendar-first. | **Open** — A1 / A2 / A3 / Modify / Defer |
| O-6 | Growth Mark | Review the isolated G1/G2/G3 comparison in `docs/prototypes/dayseed-growth-mark-variants.svg`. Prefer a plain, inspectable summary (“Collected moments”, plain count, or count plus inspectable date marks); no plant-growth animation, arbitrary score, streak or XP. A displayed metric must have an explicit definition and open the underlying dates/records. | **Open** — G1 Plain / G2 Collected moments / G3 Date marks / Defer metric |
| O-7 | Type, colour and theme | Use platform/system typography as the baseline and follow system light/dark appearance. Treat the warm-neutral restrained palette as a hypothesis only; do not approve exact colour pairs or custom fonts until contrast, text scaling and licensing are reviewed. | **Open** — Accept baseline / Modify / Defer |
| O-8 | Comeback presentation | Keep the approved 14-/30-day trigger and precedence; prefer a compact, dismissible, non-blocking presentation with no forced explanation or reflection. Final copy/layout remains open for review. | **Open** — Accept recommendation / Modify / Defer presentation |
| O-9 | Weekly reflection time | This is a true owner choice, not a delegated micro-detail. Recommended candidate: 9:00 a.m. device-local time on the configured week-start day, after quiet-hours and shared-cap checks. Do not infer this from the separate per-habit reminder default. | **Open** — 9:00 a.m. / Other time: ____ / Defer weekly reflection notification from v1 |
| O-10 | Privacy/export/delete copy | Accept the candidate wording in doc 04 as a **draft baseline only**, with the explicit caveat that it is not release-approved until actual SDK, manifest/data-extraction, network, export and deletion behaviour is verified. Do not promise encryption or “never leaves the device” without evidence. | **Open** — Accept as draft / Modify / Defer copy review |
| O-11 | Initial-v1 monetisation | Recommended: exclude ads, purchases and subscriptions from initial v1. D-010 records a candidate preference, not approval to ship it. Revisit only through a separate explicit policy/privacy and product decision. | **Open** — Exclude from v1 / Request separate evaluation for v1 |

**Gate status:** this response sheet does **not** ask for planning-gate approval yet. First record these owner choices, reconcile affected documents, finish the remaining traceability/semantic review and accessibility checklist, then present a final readiness check. No response here authorises app implementation, live database inspection/migration, builds, tests or device validation.

## 5. Separate post-planning technical validation — not currently authorised

Only after explicit owner authorisation: inspect the current native source, Gradle/toolchain and actual SQLite schema; reconcile source; run build/tests with synthetic data; inspect migration and rollback; verify Android platform-backup exclusion, offline persistence, notification delivery, backup/restore/export/delete; and install on a physical device. Record exact evidence and limitations. Feature implementation is a later stage and is not authorised by approval of the schema or stack alone.

**Current truth:** planning has progressed well beyond brainstorming, but the planning gate remains closed. No application code/database change, build, test, migration, APK installation, device validation or implementation audit is claimed by this document.
