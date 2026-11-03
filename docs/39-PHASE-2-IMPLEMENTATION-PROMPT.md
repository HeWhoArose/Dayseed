# Gemini Implementation Prompt — Dayseed Phase 2
## Complete Habit Experience, Five-Destination Navigation, and Living Grove

You are implementing **Phase 2 only** of Dayseed's approved initial-v1 programme.

Repository: https://github.com/HeWhoArose/Dayseed  
Canonical branch: `main`  
Current known main SHA when this prompt was prepared: `bf19bc11c37dd9a7929594871266bdff07dfc94e`  
Authorisation: Full Stage B initial-v1 implementation was owner-authorised under D-213. This prompt is the next bounded workstream; it does not authorise Phase 3, Phase 4, Stage C independent validation, Stage D owner acceptance, or release.

## 1. Mandatory start procedure — do this before editing

1. Fetch the latest remote `main`. Do not assume the SHA above is still current.
2. Record the exact HEAD SHA, branch, working-tree state, and whether the workspace is ahead/behind remote.
3. Reconcile the AI Studio workspace with the latest GitHub main. Preserve newer remote commits and any legitimate workspace changes. Never overwrite or force-push.
4. Read, in this order:
   - `AGENTS.md`
   - `README.md`
   - `DESIGN.md`
   - `docs/09-DECISION-LOG.md`
   - `docs/25-DECISION-REGISTER-AND-RECONCILIATION.md`
   - `docs/26-SCREEN-AND-DATA-CONTRACTS.md`
   - `docs/28-REQUIREMENT-TRACEABILITY.md`
   - `docs/32-CONTINUATION-HANDOVER.md`
   - `docs/34-UX-TASK-AND-ACCESSIBILITY-REVIEW-PROTOCOL.md`
   - the Phase 2 section of root `implementation_plan.md`
   - `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`
5. Inspect the actual current navigation, ViewModel, repository, models, screens, components, theme, and tests. Do not trust historical line numbers or assume the source still matches the earlier audit.
6. Run the available automated test suite and debug build as a baseline before feature edits. The known repository configuration uses AGP 9.1.1, Kotlin 2.2.10, compileSdk 36, and Java 21 source compatibility. Use the compatible installed toolchain actually available. The repository previously had no checked-in Gradle wrapper; do not assume `gradlew` exists or claim it ran. Record the exact command and output.
7. **Phase 1 verification warning:** Phase 1 source and test remediation was merged through PR #5. The earlier 57/57 Phase 1 self-check predates the final remediation. The newly added/expanded schema-reconciliation and restore-safety tests still require execution evidence. Run them as part of the baseline. If a Phase 1 regression fails, fix or isolate that defect before building Phase 2 on top of it. Do not relabel a merge as a test pass.
8. If the baseline cannot be run because the environment/toolchain is unavailable, report the precise blocker and stop before feature implementation. Do not invent test evidence.

## 2. Authority and non-negotiable product rules

- Preserve the existing native Android app: Kotlin, Jetpack Compose, SQLiteOpenHelper. Do not scaffold a second app, website, or alternate framework.
- Treat GitHub `main` and the canonical decision log as source of truth.
- Preserve all Phase 1 domain/data behaviour and stored data. Do not rewrite the data layer for convenience.
- Follow D-215: primary navigation is exactly **Today, Habits, Journal, Insights, Settings**. Historical habit calendar belongs inside Insights; Day Detail and Habit Detail are secondary routes. Do not restore the old four-tab arrangement.
- Follow D-216: new habits offer Quantity, Duration, and Simple check-off. Preserve legacy EFFORT_AWARE/EFFORT_LEVELS records losslessly; do not expose legacy modes as new-habit choices. Progress is derived from recorded activity and applicable targets.
- Follow D-217: individual per-habit streaks, elapsed active-run span display with explicit days/weeks unit, neutral explicit zero, and permanent Grove lifecycle states MISSED, PAUSED, ARCHIVED, COMPLETED.
- Follow D-218: the one-off action is labelled **Take a rest day**. It is not Skip, Miss, Complete, or Record; it does not create activity or reduce a weekly target.
- Follow D-194 and the approved U-1 through U-13 decisions. In particular: preserve effective-dated history; honour the current-week target snapshot; do not mix units; keep week-start history stable; do not cross lifecycle closures when recalculating streaks.
- Keep Journal text and personal data local. Do not log or upload private content.
- Keep the UI consistent with the approved direction and `DESIGN.md`. Do not invent new design tokens or silently promote exploratory values to approved tokens.
- No ads, analytics SDK, billing, cloud sync, accounts, online backup, social comparison, XP, or runtime AI analysis. These are outside this phase/initial-v1 scope.

## 3. Phase 2 objective

Deliver the complete core habit workflow: create a habit, see it on Today or Habits, record actual activity, inspect and correct individual sessions, manage its lifecycle, and understand its individual streak/Grove history. The UI must be a faithful client of the Phase 1 domain/repository rules.

Implement only the following scope.

### A. Five-destination navigation and route ownership

- Implement the five primary destinations: Today, Habits, Journal, Insights, Settings.
- Preserve existing screen implementations where sound; do not replace working screens wholesale.
- Use a consistent route/state approach for secondary destinations. Navigation Compose may already be declared; inspect actual usage before changing dependencies.
- Implement Habit Detail as a secondary route.
- Preserve the originating destination, selected date, selected habit, and selected record when navigating to detail and back.
- Back must return to the correct originating route/state. Do not hard-code every Back action to Today.
- Keep Settings within its existing hub/panel arrangement.
- Journal and Insights primary destinations must remain navigable even if their deeper Phase 3 features are not yet implemented; do not implement Phase 3 scope early.

### B. Today — compact, action-focused daily view

- Preserve the D-210/D-211 daypart heading/artwork and existing theme behaviour.
- Keep the hierarchy compact: date/greeting, restrained streak/progress indicator, action-focused habit list, a concise Journal entry point, and a compact Collected moments/Growth Mark entry point where the existing contract requires it.
- Keep comeback, undo, and retrospective prompts conditional and non-blocking.
- The default habit card must not become a full management screen. Move editing, history, and detailed session inspection to Habit Detail or focused flows.
- Show today's scheduled habits. Do not hide active habits that are not scheduled today: provide the agreed compact Not scheduled today group/link.
- Flexible weekly habits must show the correct week-to-date total and target, not today's amount compared with a weekly target.
- Keep individual streaks visible without making streak length the sole or dominant success metric.
- Ensure quick actions cannot create duplicate records from accidental repeated taps; preserve legitimate additional sessions.

### C. Dedicated Habits destination and Habit Detail

- Add a dedicated Habits screen showing all habits, not only those scheduled today.
- Provide the approved lifecycle groups/states: Active, Paused, Completed, Archived.
- Include add, search, reorder, and lifecycle management entry points with clear empty states.
- Habit Detail must expose the relevant identity/category, measurement mode/unit, target/period, schedule, current progress, history, individual entries, notes, and lifecycle actions.
- Keep Completed distinct from Pause, Archive, and Permanent Delete.
- Archive and completion preserve the approved historical/Grove records.
- Permanent deletion must follow the existing destructive confirmation and transactional repository contract. Do not make permanent deletion snackbar-undoable unless the repository's approved HABIT_DELETION Undo is explicitly invoked.

### D. Create/Edit Habit — structured full-screen flow

- Replace the tall, overloaded dialog with a structured full-screen form or equivalent approved route.
- Separate the form into identity/category, measurement/units, target/period, schedule, and optional reminder sections.
- New-habit modes: Quantity, Duration, Simple check-off only.
- Show only fields relevant to the chosen mode.
- Explain the distinction between weekly check-off days/week and sessions/week.
- Preserve optional category as metadata, not calculation semantics.
- Support daily, selected weekdays, and flexible weekly schedules according to the current contracts.
- Offer **Take a rest day** only in the eligible context. Do not create a fake completed/skip outcome.
- Apply D-194 at both UI and repository boundaries. UI hiding a button is not a substitute for transaction-level enforcement.
- Preserve existing legacy EFFORT_AWARE/EFFORT_LEVELS habits as readable/editable records without offering those modes for new habits.
- Preserve existing effective-dated configurations and week-snapshot rules.

### E. Record, inspect, correct, delete, and undo

- Provide a focused Record Session flow for actual quantity/duration, optional per-session note, and an explicit local date when backdating is supported by the contract.
- Keep the habit/date note distinct from an individual session note and from Journal.
- Show individual sessions separately from daily/weekly aggregates.
- Wire the existing repository update operation through the relevant screens; users must be able to correct one selected entry without replacing or deleting unrelated sessions.
- Support intentional zero as a real saved record, visually distinct from no record.
- Provide guarded Undo for supported recent actions using the existing token/state guards and documented expiry. Do not create a second Undo mechanism that bypasses repository checks.
- Permanent habit deletion uses the dedicated HABIT_DELETION payload/dispatch and its collision guards. Never promise restoration if the repository rejects an expired, consumed, or colliding token.
- After correction/deletion, refresh dependent progress and streak/Grove views from authoritative source records.

### F. Individual streaks, branches, and Grove

- Show the active per-habit streak/branch and the agreed main longest-active-run indicator using normalized elapsed calendar-day span with an explicit days/weeks unit.
- Provide a clear route from the indicator to individual streak details and Grove entries.
- Each habit's branch identity and visual identity must remain stable across recomputation/restart where the model contract requires it.
- A run-ending event creates or updates the corresponding permanent Grove record with the correct terminal reason.
- Completion preserves the final branch as an achievement. Pause and Archive close runs with their own terminal reasons. A later resumed run is separate.
- Preserve unqualified short-run history without displaying it as a Grove tree.
- Backdated correction may heal a MISSED gap only under the approved rules; it must never bridge PAUSED, ARCHIVED, or COMPLETED closures.
- Animations are event-driven and play once for the actual state transition, not on every Today recomposition.
- Use stable colour plus shape/label cues; do not rely on colour alone.
- Respect reduced-motion settings and avoid expensive animation/rendering on lower-end Android devices. Do not add runtime AI-generated artwork.

## 4. Phase 2 automated tests and verification

Add/extend tests alongside implementation. Keep existing tests and never weaken assertions to obtain a green build.

At minimum cover:

1. Five-tab navigation selection, secondary route entry, correct Back destination, and preservation of selected date/habit/record state.
2. Today scheduled vs unscheduled groups and weekly total presentation sourced from the correct period aggregate.
3. Habits groups and lifecycle transitions: Active, Paused, Completed, Archived; permanent delete remains distinct.
4. Create/edit field visibility and validation for Quantity, Duration, Simple check-off, daily/selected/flexible schedules, days/week, sessions/week, and optional category.
5. D-194 Apply-today protection after normal activity and explicit zero; REST_DAY alone must not lock configuration.
6. Record multiple sessions on one date; display sessions and aggregate separately; correct one session without changing the others.
7. Explicit zero vs no record, and correct persistence after reload.
8. Backdated correction refreshes the affected progress/streak state without crossing lifecycle closures.
9. Guarded Undo success, expiry, single-use, stale-state rejection, and habit-deletion collision/atomicity.
10. Individual streak/Grove display models, terminal reasons, unqualified history, stable identity, and mixed-cadence unit labels.
11. Reduced-motion state selection and animation event idempotence where these can be verified automatically.
12. Regression coverage for all Phase 1 migration, schema, repository, restore, and BigInteger tests.

Use Compose UI tests if the current project has or can safely add the compatible test infrastructure. Otherwise add the strongest repository/ViewModel/domain tests possible and explicitly identify UI assertions that remain unautomated. Do not describe a test tag as a UI test.

After implementation:
- Run the full JVM/Robolectric suite with rerun enabled.
- Run the debug APK assembly.
- Run lint/static checks available in the project.
- Inspect test XML/report output and record actual counts, failures, skipped tests, and warnings.
- Record exact commit SHA, Gradle/JDK/AGP/Kotlin/SDK versions, commands, duration, and artifact path.
- If a command cannot run, state why and mark it BLOCKED/NOT RUN. Do not invent evidence.

## 5. Human-intervention testing — defer execution, preserve the register

The owner wants all tests requiring human/device intervention consolidated into one session after the planned implementation phases are complete.

- Do not ask the owner to perform device tests during Phase 2.
- Do not claim manual tests passed from emulator screenshots, mock-ups, source inspection, or automated tests.
- Append Phase 2 human/device checks to `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` in a clearly labelled Phase 2 section. Include a unique ID, prerequisites, exact steps, expected result, evidence to capture, and dependency on later phases.
- At minimum register: five-tab navigation and Back behaviour; Today comprehension and unscheduled group discovery; create/edit flows for each mode; multiple-session record/correction/Undo; lifecycle discoverability; individual streak/Grove comprehension; large text/compact-width/reduced-motion checks that require a rendered device.
- Do not execute those human checks now. Phase 3 and Phase 4 must add their own cases to the same register. Run the combined register only after Stage B Phase 5 integration closeout and explicit Stage C authorisation.

## 6. Explicitly out of scope for Phase 2

Do not implement:
- Full Journal archive/search/filter/editor expansion.
- Shared calendar, Day Detail, weekly board, mood markers, Growth Mark detail, or expanded Insights.
- Real Android notification delivery, runtime notification permission flows, reboot/timezone rescheduling, or reminder learning.
- Android SAF file picker, full backup/restore UI, privacy/export/delete-settings expansion beyond the already required habit lifecycle actions.
- Production release signing or Play Store publication.
- Ads, analytics, billing, subscriptions, cloud/account/sync, runtime AI, XP, or social features.

Those belong to later phases or are explicitly deferred.

## 7. Required implementation workflow

Follow the repository's governance and the owner's established review discipline:

**AUDIT → DESIGN → IMPLEMENT → TEST → REGRESSION → RE-AUDIT → DOCUMENT → COMMIT/HANDOVER**

- Audit actual source and current contracts before edits.
- Give a concise technical design for navigation/state ownership and UI-to-repository flows before large changes.
- Implement only Phase 2. Keep changes coherent and reviewable; do not rewrite unrelated screens.
- Run tests after each meaningful slice and the full suite at phase end.
- Perform a read-only re-audit against every Phase 2 acceptance item.
- Update affected docs/09, docs/25, docs/26, docs/28, docs/12, and the human-validation register to reflect only verified reality.
- Preserve historical decisions and evidence. Do not mark Stage C passed or Stage D open.
- Commit the completed Phase 2 work to a reviewable branch/PR. Never force-push or overwrite newer main.

## 8. Required final report

Return a beginner-readable report with:

1. Starting SHA and final SHA.
2. Files changed and what each change does.
3. Navigation/screen flows implemented.
4. Domain/repository contracts reused and any new technical decisions.
5. Database/backup/migration impact (explicitly say none if none).
6. Automated test commands and exact results.
7. Debug build and static-check results.
8. Tests added and requirement IDs they cover.
9. Phase 2 human tests appended to the register, clearly marked NOT RUN.
10. Known defects, blocked items, and deferred scope.
11. PR/commit links and exact next action.

**Do not begin Phase 3 or Phase 4. Do not ask the owner to repeat any settled D- or U-decision. Do not claim the app is release-ready.**
