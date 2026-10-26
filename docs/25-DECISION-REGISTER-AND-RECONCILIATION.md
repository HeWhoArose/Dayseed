# Decision Register and Planning Reconciliation

**Status:** Working reconciliation register — planning only; implementation remains not authorised.
**Last reconciled:** 2026-10-26
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
| Personal data and privacy | Approved direction, details open | Local-first by default. Do not claim “never leaves the device” until platform backup, export, SDKs, and data flows are verified. |
| Habit tracking | Approved | Keep numeric, completion-only, and effort-aware modes semantically distinct. Numeric entries are whole numbers; explicit zero differs from no record; multiple legitimate entries can be summed without retry double-counting. |
| Daily vs weekly targets | Approved | Daily goals apply to selected days; flexible weekly goals accumulate across the configured week and do not judge inactive days as daily misses. Week-start is user-selectable. |
| Historical data | Approved | Schedule/target changes are prospective; preserve past records and explicit corrections. Never silently rewrite history. |
| Journaling | Approved | Journal writing is independent of habit completion; text-only in v1; multiple entries per date allowed; titles and mood tags are optional. Save/edit/delete and draft behaviour follow the latest decision log. |
| Progress language | Approved | Actual amount is preserved; partial, goal-met, above-target, and Tiny-to-Huge labels are factual progress states, not moral judgements. |
| Comeback experiences | Approved direction with detailed rules | Two-week and one-month triggers have defined precedence and suppression rules. Do not infer why the user was away. See `24-COMEBACK-EXPERIENCE.md`. |
| Weekly reminders | Approved direction | 9:00 a.m. local time on the configured week-start day for weekly reflection; two full days without progress before inactivity check-in; at most three inactivity reminders per week; stop when goal is met. See `23-WEEKLY-GOAL-REMINDERS.md`. |
| AI analysis, social features, leaderboards | Deferred unless explicitly re-approved | Do not add AI processing, accounts, public profiles, or leaderboards implicitly. |
| Visual identity and navigation | Unverified / proposed | Design exploration and mockups are not final approval. `DESIGN.md` should remain the authoritative design-token/component reference. |

## Delegated defaults

These are conservative defaults for routine details. They do not authorise new product scope, collection/sharing of personal data, or irreversible behaviour.

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

## Decisions requiring owner approval

These are not micro-decisions and must not be silently delegated:

1. **Build readiness (D-015):** D-009 is resolved: the owner approved continuing the existing native Kotlin + Jetpack Compose project. The observed code is not a verified build. Keep the active plan native; build, tests, APK installation, and device behaviour remain unverified and must wait for explicit planning-gate/technical-validation authorisation.
2. **Privacy and network scope (D-004, D-011, D-091):** local-first v1/cloud sync deferred language conflicts with the newer approved direction that online backup and cross-device sync may be premium. Resolve the exact v1 boundary and consent/data-flow requirements before implementation.
3. **Monetisation (D-010, D-014, D-091):** ads/rewarded extras/supporter pack are not approved; subscription is not proposed for v1. Premium online backup/sync direction needs a consistent product and monetisation decision.
4. **Visual identity and navigation (D-008, D-016–D-021):** final visual direction, Growth Mark definition, archive navigation, typography/colour and competitor findings remain to be validated. Do not treat concepts as approved UI.
5. **XP/rank and social comparison (D-033):** private rank/XP is exploratory; online leaderboards remain deferred and require separate explicit approval.
6. **Data and privacy claims:** encryption-at-rest, Android backup inclusion/exclusion for saved records, analytics/crash reporting, and exact deletion/export wording must match inspected configuration and evidence.
7. **Release scope and planning gate:** implementation cannot begin until the owner explicitly approves the planning gate in `19-AI-STUDIO-GITHUB-WORKFLOW.md`.

## Reconciliation findings

| ID | Finding | Status | Required action |
|---|---|---|---|
| R-001 | Active workflow and architecture instructions previously conflicted with the native project by preserving an assumed Expo path. | Corrected; D-009 approved | D-009 now explicitly approves continuing native Kotlin + Jetpack Compose. Architecture/workflow docs now align; build and test status remain unverified. |
| R-002 | `14-STACK-COMPARISON.md` previously linked to the wrong implementation-reality-audit filename. | Corrected | Link now points to `22-IMPLEMENTATION-REALITY-AUDIT.md`; historical stack research remains available. |
| R-003 | `09-DECISION-LOG.md` has inconsistent table shapes around D-083 onward and a duplicate D-090 identifier. | Documentation defect | Preserve decision meaning; later tidy the formatting and duplicate ID with explicit traceability rather than silently renumbering references. |
| R-004 | Earlier product/privacy documents describe cloud sync as excluded from v1, while D-091 approves a direction toward premium online backup and cross-device sync. | Conflict / owner decision | Clarify whether this is a future direction only or an approved v1 scope change; update charter, spec, privacy, monetisation, roadmap and tests together after owner confirmation. |
| R-005 | The weekly reminder spec approved 9:00 a.m. local week-start delivery, while its remaining section ambiguously said the timing remained to be specified. | Corrected | The spec now states that 9:00 a.m. local time is approved; timezone/clock-change, deduplication, combination, and test edge cases remain open. |
| R-006 | Repository files and past notes mention source and schema, but there is no current build/test/device evidence in this document. | Unverified | Do not claim the app builds, installs, or passes tests until authorised verification produces evidence. |
| R-007 | Current privacy docs say no third-party analytics is the default proposal and warn that Android backup behaviour is unverified; later backup decisions define a detailed local backup/restore system. | Partially reconciled | Keep local file-based backup distinct from Android platform backup and online sync; verify each data path separately. |

## Documentation authority and update order

1. `09-DECISION-LOG.md` — authoritative owner decisions and their statuses.
2. `01-PRODUCT-SPECIFICATION.md` and `00-PROJECT-CHARTER.md` — product scope and promises.
3. Feature specifications, including `23-WEEKLY-GOAL-REMINDERS.md` and `24-COMEBACK-EXPERIENCE.md` — detailed behaviour.
4. `02-UX-AND-DESIGN.md`, research docs, and root `DESIGN.md` — experience evidence and design authority.
5. `03-TECHNICAL-ARCHITECTURE.md`, `04-DATA-AND-PRIVACY.md`, `07-TESTING-AND-QUALITY.md`, and `08-IMPLEMENTATION-ROADMAP.md` — technical/privacy/test/sequence implications.
6. `19-AI-STUDIO-GITHUB-WORKFLOW.md` — workflow and explicit implementation gate.
7. `12-PROGRESS-AND-HANDOVER.md` — factual status, verified commits, tests and next task.

When a decision changes, update the decision log and every affected document in one traceable reconciliation batch. Fetch each current file immediately before writing it, preserve unrelated content, then re-fetch and verify the result.

## Planning gate checklist

- [x] Resolve R-001 / D-009 stack decision: continue native Kotlin + Jetpack Compose; remove contradictory Expo instructions from active workflow and architecture guidance.
- [ ] Resolve R-004 cloud/premium scope conflict.
- [ ] Reconcile product specification, privacy, monetisation, and roadmap with approved decisions.
- [ ] Finish UX/navigation, design tokens, archive, Growth Mark, and research evidence.
- [ ] Define the data/date model, migration safety, local backup/restore, and Android platform-backup policy.
- [ ] Complete notification and journaling edge-case acceptance tests.
- [ ] Correct broken links and decision-log formatting/identifier anomalies with traceability.
- [ ] Reconcile the handover and roadmap against the actual latest repository state.
- [ ] Owner explicitly approves Gates A–D before implementation begins.

**Boundary:** This register records planning state only. It does not authorise code changes, an implementation audit, or a release-readiness claim.
