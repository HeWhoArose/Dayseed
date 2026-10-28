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
- **D-010 monetisation:** the owner has a *candidate preference* for restrained banners, user-initiated rewarded extras, and possibly a one-time Supporter Pack. This is **not** approval to monetise initial v1. Current safe recommendation is to keep ads, purchases, and subscriptions out of v1 pending an explicit owner decision and policy/privacy review.
- **D-040 progress bands:** Tiny, Small, Halfway, Nearly there, Goal met, Extra, Huge use the approved target-relative thresholds; classify from the exact actual/target ratio before display rounding. Always show actual amounts and avoid shame/pressure.
- **Measured habits:** duration and quantity preserve non-negative whole-number actuals; explicit zero is distinct from no record. Multiple valid entries per habit/date are permitted and summed; each entry remains inspectable/editable. The dashboard **Complete** shortcut records the usual target for measured habits; **Record** opens detailed entry/correction. Completion-only habits use Complete. A note is not completion.
- **History/date policy (D-187/D-188/D-193/D-194):** saved local-date keys do not move with timezone travel; schedule/target changes preserve historical meaning; rest/non-scheduled/paused days are neutral or excluded as defined; missed status is derived only after a scheduled local day passes without qualifying activity; no manual mark-missed or habit-day Skip action in v1; journal text never changes habit status. Same-day Apply-today replacement is allowed only before qualifying activity exists that day; otherwise preserve today and offer Apply tomorrow.
- **Schedules (D-191/D-195):** only `DAILY`, `SELECTED_WEEKDAYS`, and `FLEXIBLE_WEEKLY_TARGET`; target period is `DAY` or `WEEK` as specified. Weekday mask is fixed and locale-independent: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64. `SELECTED_WEEKDAYS` requires a valid non-empty mask; other schedule kinds do not require one. Flexible-weekly preferred days are optional guidance, never restrictions. No every-N-days schedule unless owner revises the decision.
- **Journal:** multiple saved entries/date; text-only v1; explicit save; edits preserve identity/creation time; optional title and moods; deletion is explicit/confirmed; journal entries remain independent of habit completion and habit deletion.
- **Reminders (D-041–D-046):** daily per-habit reminder defaults to 9:00 a.m. local time as specified. Quiet-hours/shared-cap/collision/deduplication/local timing-learning/partial-tip micro-details are delegated defaults under D-196. **Weekly reflection clock time is still an owner choice**; do not infer it from the daily habit reminder. D-045/D-046 keep the week-start, catch-up, deduplication, and no-backlog behaviour.
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
  - [Growth Mark G1/G2/G3](prototypes/dayseed-growth-mark-variants.svg) — plain monthly context, “Collected moments”, and count with inspectable date marks.
  - [Owner-provided UI reference layout](prototypes/dayseed-owner-reference-layout.webp) — five Today variants/states plus Habit Create/Edit, Habit Detail, History, Journal, Insights, Return, and Settings. Use it as a durable visual north star; exact UI, tokens, and navigation remain unapproved.
- Boards use synthetic data. They are planning artefacts, not approved UI, accessibility validation, or user-test evidence. They still need owner task/accessibility review.
- Requirement matrix covers AC-001 to AC-024. AC-009's deterministic Mood Insights fixture was reviewed; the fixtures and all other acceptance cases are still not executed.
- Earlier Markdown relative-link, heading-fragment, table, and external-URL checks were recorded as passing for the then-current 37 Markdown files. New files/edits require appropriate re-checks before closing planning.
- The weekly reflection clock-time contradiction was corrected: owner choice is open, not delegated.
- The owner batch (doc 31) now contains O-1 through O-11, covering schema, backup/restore compatibility, Today hierarchy, navigation, archive, Growth Mark, type/colour/theme, comeback presentation, weekly reflection time, privacy copy, and v1 monetisation.
- The current handover intentionally does not claim application code has passed build/tests or that any proposed schema/design is implemented.

## 7. Exact next steps — use this order

### Step 1 — Refresh and reconcile current documentation

Fetch current `main`, check the recent commits and relevant decision/doc files. Confirm the three SVG paths exist, keep doc 27/doc 31/doc 12 aligned, and finish a semantic stale-claim pass. In particular, cumulative/history documents must not present old options (decimal numeric entry, one numeric total/day, every-N-days schedules, weekly reflection at a presumed time) as current requirements.

### Step 2 — Present the owner decisions as one review

Use **Section 4, “Owner response sheet,” in doc 31** (O-1 through O-11). The starting recommendations are proposals, not approvals. Ask the owner to reply **Accept / Modify / Defer** to material choices in one grouped review, with a single time field for O-9. It is valid to defer a design or feature while keeping unrelated planning moving. Do not ask them to re-approve decisions already recorded as Approved.

### Step 3 — Apply the owner's actual responses

Record actual decisions in doc 09 and reconcile affected docs 01/03/04/07/11/23/25/27/28/29/30/31/12 as needed. Keep the source choice and status consistent; retain unresolved options as Open or Deferred. The planning gate remains closed while any material decision, privacy wording, schema/conflict policy, or required checklist is not resolved or safely deferred.

### Step 4 — Finish design accessibility review

Review all three SVG boards against the same task list in doc 29 and the accessibility checklist: small/typical widths; large text (including up to 200% where supported); keyboard-open layout; text contrast for real pairs; non-colour states; TalkBack names/order; touch targets; empty/error/destructive states; long labels and entries; corrections; known-date/phrase search; neutral blank dates; and “continue without journaling” on comeback. Do not invent user-test results. Record evidence only if actual users/tasks are tested; otherwise record a heuristic review with its limitations. Update `DESIGN.md` only for owner-approved tokens/components.

### Step 5 — Finish requirement and semantic consistency

Confirm every material requirement has a planned acceptance case and an honest status in doc 28; confirm schema failure/conflict/recovery edges have planned cases in docs 07/30. Check documentation links, heading fragments, table structure, and contradictions after changes. Approve privacy wording only as planning draft unless actual runtime behaviour has been verified later under proper authorisation. Keep no unsupported claims such as “build passed,” “fully offline verified,” “encrypted,” or “backup disabled.”

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
