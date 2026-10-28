# Dayseed — Next Agent Handover

**Last updated:** 2026-10-28  
**Status:** Planning only. The planning gate is **NOT APPROVED**. Feature implementation, live-database inspection/migration, builds, tests, APK installation, device validation, and implementation-reality audit are **not authorised** until the owner explicitly approves the planning gate and separately authorises the relevant next stage.

**Canonical repository:** https://github.com/HeWhoArose/Dayseed  
**Canonical branch:** main  
**Purpose:** Let the next assistant or coding agent resume without making the owner repeat product rules, the decision-making method, current status, or next actions.

---

## 1. Read these first

Read in this order before changing project files:

1. [README](../README.md) — repository status and entry points.
2. [Decision Log](09-DECISION-LOG.md) — authoritative record of approved owner decisions, including D-195 and D-196.
3. [Decision Register and Reconciliation](25-DECISION-REGISTER-AND-RECONCILIATION.md) — current interpretation of approved, delegated, proposed, conflicting, and unverified items.
4. [Planning Completeness Review](27-PLANNING-COMPLETENESS-REVIEW.md) — current readiness checklist and remaining blockers.
5. [Owner Planning Decision Batch](31-OWNER-PLANNING-DECISION-BATCH.md) — the single grouped review sheet; its O-1 to O-11 responses are still open unless the owner has since answered them.
6. [UX and Design Approval Pack](29-UX-DESIGN-APPROVAL-PACK.md) and [Design System](../DESIGN.md) — visual options, comparison tasks, and the sole authoritative design-token/component reference.
7. [Product Specification](01-PRODUCT-SPECIFICATION.md), [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md), [Technical Architecture](03-TECHNICAL-ARCHITECTURE.md), and [Schema Closure and Migration Plan](30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md).
8. This handover, then the [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md).

Use the [Progress and Handover history](12-PROGRESS-AND-HANDOVER.md) for dated context, but do not follow an old “next action” blindly when the current readiness review or decision register supersedes it.

## 2. Non-negotiable operating rules

- **Do not implement yet.** The repository already contains native Kotlin/Jetpack Compose and SQLite application source, but the project workflow remains in planning. Source or preview presence is not proof of a clean build, working features, or owner approval.
- Do not change app source, database schema, migrations, build configuration, or live data; do not run builds/tests/device checks or an implementation audit unless the owner explicitly approves the planning gate and authorises that stage.
- Do not scaffold a website, web dashboard, second app, or a React Native/Expo rewrite. D-009 approves continuing the existing native Kotlin + Jetpack Compose Android project.
- GitHub main is the canonical shared source of truth, including after AI Studio context resets. Refresh the latest remote state before work. Never overwrite newer commits with a stale workspace.
- DESIGN.md is the sole authoritative design-token/component reference. Do not silently approve candidate values there before the design review.
- Do not claim research, user testing, accessibility validation, build success, passing tests, migration safety, installation, or device behaviour unless actual evidence exists and has been inspected.
- Keep journal privacy, offline-first core operation, data integrity, stable historical records, accessibility, and non-judgmental progress language central.
- Keep private journal content, credentials, API keys, signing keys, secrets, and sensitive local machine paths out of Git.

## 3. Decision-making protocol — apply this automatically

The owner explicitly asked not to be interrupted with repeated micro-questions. Use this protocol throughout the project.

### Classify each choice before acting

- **Approved:** A clear owner decision recorded in the current conversation and/or the Decision Log. Follow it. Do not reopen it unless there is a real contradiction or new material evidence.
- **Delegated default (D-196):** A routine, low-risk, reversible detail that the owner has delegated. Apply the conservative documented default, record the rationale where useful, and add acceptance cases. Do not ask the owner about every small choice.
- **Proposed:** A recommendation, mockup, research idea, or schema refinement that has not been accepted. Label it as proposed; never describe it as approved.
- **Open / owner decision:** A material choice affecting product philosophy/scope, privacy/security, data integrity, accessibility, monetisation, irreversible actions, or major product/visual identity. Do not decide silently. Put it in the grouped owner batch.
- **Deferred:** Explicitly out of scope or postponed. Do not sneak it into v1; record any revisit trigger if needed.
- **Unverified:** Code, a document, or a design exists, but its behaviour/evidence has not been checked. Keep it unverified until there is evidence.

### How to resolve ambiguity or contradictions

1. Check the current Decision Log and Decision Register first.
2. Prefer the latest explicit owner-approved decision over older brainstorming, screenshots, research, or stale handover text.
3. The product specification and screen/data contracts define approved behaviour; the architecture/schema documents translate that behaviour into a **proposed** technical model.
4. If two authoritative documents conflict, do not guess. Record the conflict, reconcile the affected documents, and verify each changed file after writing.
5. Do not create a duplicate decision ID or re-ask a settled decision. D-195 already approves the fixed weekday-mask mapping; D-196 already delegates routine low-risk details.
6. Bundle consequential questions into one owner review using [O-1–O-11](31-OWNER-PLANNING-DECISION-BATCH.md). Ask only when the owner’s answer materially changes what can safely be planned. Accept partial answers; unanswered items remain open.
7. When proposing a default for a material decision, label it **recommended, not approved**. The owner may Accept, Modify, or Defer.
8. After an owner decision, update the Decision Log and all affected specs/contracts/architecture/traceability/handover together. Re-fetch and verify the GitHub contents after each write.

### Safe delegation examples

Use conservative documented defaults for ordinary copy/layout details, reversible spacing/ordering, low-risk validation messages, and routine acceptance-case expansion. Do **not** use delegation to approve new scope, analytics/ads, cloud services, irreversible deletion policy, a schema migration, new personal-data collection, a major visual identity, or an unapproved release gate.

## 4. Current product baseline to preserve

- Product promise: **“Your progress is more than your streak.”**
- Android-first, offline-first core; no account or network required for core habit tracking, journaling, history, insights, or comeback flows.
- Initial v1 includes readable journal export and user-controlled portable local backup/restore. Dayseed-managed accounts, online backup, and cross-device sync are outside v1. A user-selected file provider may itself sync a file online; privacy copy must distinguish this from a Dayseed cloud service.
- Continue native Kotlin + Jetpack Compose. The existing project source includes SQLite persistence, a repository, ViewModel/StateFlow and screens. Build/test/APK/device status remains unverified.
- Habit modes: duration, quantity, and completion-only. Numeric values are whole numbers; intentional zero is distinct from no record; multiple numeric/duration entries per habit/local date are allowed and summed; individual entries remain inspectable/editable.
- Today cards use two separate actions: **Complete** (usual target for measured habits; completion for completion-only) and **Record** (detailed actual amount/correction flow for measured habits). Do not replace these with a single ambiguous control.
- A habit/date note, per-entry note, and general journal entry are separate. A note never implies completion. Multiple journal entries per date are allowed.
- Preserve history: schedule/target edits are prospective and never silently rewrite saved activity. Historical progress uses the target that applied then; current-target comparison is separate. Rest/non-scheduled days are neutral, paused periods are excluded, and missed status is derived rather than a moral judgement.
- Weekly targets are flexible across the configured week; preferred weekdays guide but do not restrict. The user can choose week start; weekly boundaries follow that saved preference.
- D-195 weekday mask is fixed and locale-independent: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. Valid masks are 1–127; selected-weekday schedules require a valid non-empty mask; DAILY and FLEXIBLE_WEEKLY_TARGET do not require a scheduled-day mask.
- Reminder defaults already recorded: onboarding asks once whether reminders are wanted; per-habit time default is 9:00 a.m. local time; system-managed timing uses 14 days of sufficient observations and then 28 days to validate/solidify; manual times are protected from automatic changes. Routine reminder safety details use D-196 defaults.
- **Do not confuse per-habit reminder time with weekly reflection time.** The weekly reflection clock time is a genuine owner choice and remains open; the 9:00 a.m. per-habit default does not decide it.
- Retrospective journal prompt cadence and guarded Undo/Delete micro-details are delegated defaults under D-196 and may be overridden in the grouped review. They are not separate blockers by default.
- Monetisation is not approved for initial v1. The recommended default is to exclude ads, purchases, and subscriptions from v1; D-010 records a candidate preference, not permission to ship monetisation.
- XP/rank, social/leaderboards, AI processing, journal photo/audio, accounts, and online features remain outside v1 unless separately approved.

## 5. Current planning artefacts and what they mean

Three editable SVG comparison boards are in the repository:

- [Today variants — W1/W2/W3](prototypes/dayseed-today-variants.svg)
- [Navigation and archive variants — N1/N2 and A1/A2/A3](prototypes/dayseed-navigation-archive-variants.svg)
- [Growth Mark variants — G1/G2/G3](prototypes/dayseed-growth-mark-variants.svg)

They use synthetic data and are **planning prototypes only**. They are not approved UI, accessibility results, or user-test evidence. Review them using the same task/accessibility scorecard in doc 29: logging speed, find/write/retrieve/edit/delete tasks, clear daily versus weekly progress, empty/error/destructive states, large text, small widths, TalkBack labels/order, non-colour cues, contrast, scrolling and keyboard overlap.

The main design decisions remain open until the owner selects, modifies, or explicitly defers them:
- Today hierarchy (W1/W2/W3).
- Navigation (N1 four destinations with Journal accessible from Today/History, or N2 dedicated Journal destination).
- Journal archive (A1 timeline-first, A2 hybrid timeline/date jump, or A3 calendar-first).
- Growth Mark representation (G1 plain, G2 “Collected moments”, or G3 date marks); metric must be defined, inspectable, and not misleading.
- Visual direction, typography/colour/theme, and comeback presentation/copy.

The recommended starting points in doc 31 are recommendations only, not owner decisions.

## 6. Schema, backup and migration status

- Docs 03 and 30 have a reconciled **proposed** relational model. It includes effective-dated habit configuration, separated numeric and non-numeric activity, stable journal-draft save identity, global activity idempotency-key handling, restore sessions/conflicts, portable-backup field mapping, notification-cancellation outbox, transaction boundaries and failure recovery.
- D-197 is the delegated fail-closed migration default: if any legacy record cannot be mapped losslessly, roll back the entire migration and leave the original database/schema version intact. No silent dropping/coercion and no assumed quarantine table.
- The portable backup container format version and data schema version are distinct. Recommended compatibility boundary for owner review: accept the current supported format/schema and older versions only when a documented lossless converter exists; reject unsupported versions before live mutation.
- Conflicting numeric activity must never be added together or silently discarded. Same habit/date note collisions must not offer “Keep both” while only one live note is allowed. Journal “keep both” means separate entries with distinct IDs. Unsafe conflicts remain unresolved until a safe explicit choice is possible.
- The complete schema is not owner-approved. Do not inspect, migrate, or alter the live database during this planning stage. Acceptance tests are written plans, not executed results.

## 7. Exact next actions — in order

1. **Refresh GitHub main.** Check the latest commit/tree and any changes since this handover. Do not rely on cached snippets or old conversation state.
2. **Read the current owner response sheet** in doc 31 (O-1–O-11). If the owner has already answered any item in the conversation, record those exact answers; do not ask them again. Otherwise prepare one compact review, not eleven separate conversations.
3. **Finish prototype consistency review.** Confirm the rendered SVGs and text wireframes agree with approved contracts. Correct only clear mismatches (for example, measured habits need both Complete and Record; completion-only habits need Complete). Re-fetch every changed file and verify exact content.
4. **Review schema/restore decisions as a coherent group.** Present docs 03/30 together. Keep the schema proposed until the owner’s choice on compatibility/conflict boundaries is recorded.
5. **Review the three design boards** against the task/accessibility scorecard. Record the owner’s choice or explicit deferral for each material design decision. Do not claim a usability study.
6. **Review privacy/export/delete copy** in doc 04 against the written product rules. Keep claims about encryption, “never leaves the device”, Android Auto Backup exclusion, SDK/network behaviour, and deletion scope explicitly unverified until technical evidence is authorised and collected.
7. **Close requirement traceability** in doc 28 and semantic consistency across current docs. Dated research notebooks can retain old proposals, but clearly mark them as historical/superseded and point to current authority. Fix stale status statements; do not erase useful history.
8. **Run documentation-only checks** after edits: relative links, heading fragments, malformed tables, duplicate/conflicting status claims, and SVG XML/tag integrity. These checks do not authorise app builds/tests.
9. **Prepare one final planning readiness summary.** List what is approved, what is delegated, what is explicitly deferred, the owner choices made, and remaining evidence gaps. Only then ask the owner for explicit planning-gate approval.
10. **Do not edit the workflow approval record to “approved” unless the owner explicitly approves the planning gate.** Gate approval and authorisation for technical validation are not the same as approval to implement features. Follow doc 19's stages and get separate authorisation where required.

## 8. Required format for future handovers

At the end of each substantial work session, update this handover and doc 12 with:
- Date and latest verified commit SHA/branch.
- Exact files changed and why.
- Evidence checks performed and their results.
- Owner decisions recorded versus recommendations still open.
- What was deliberately **not** done (especially builds/tests/schema changes when gated).
- The next single best action, plus any blocker that genuinely requires the owner.

Never write “done” for a task whose output was not re-fetched and verified. Never state that a planned test passed when it has not been run. Keep the handover concise enough to scan, but complete enough that a new agent can continue without re-interviewing the owner.
