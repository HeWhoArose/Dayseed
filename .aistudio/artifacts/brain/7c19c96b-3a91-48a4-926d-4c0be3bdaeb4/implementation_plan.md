# Dayseed — Phase 1 Corrective Completion Plan (Revised)

## Resolving Remaining Phase 1 Defects, Completing v3 Backup/Restore & Merge Safety, Expanding Acceptance Tests, and Reconciling Documentation

---

## 1. Governance & Inspected Baseline Findings

### 1.1 Verified Defects & Additional Inspection Findings

1. **Correction A — `D-040` Progress Classification (`app/src/main/java/com/example/dayseed/data/Models.kt`):**
   - **Defect A1 (150% boundary):** `ProgressBandClassifier.classify` currently evaluates `scaled < t150 -> ProgressBand.EXTRA`, misclassifying exactly 150% as `ProgressBand.HUGE`.
   - Under `D-040`, Extra is **greater than 100% through and including 150%**. Huge is **strictly greater than 150%**.
   - **Fix:** Use `scaled <= t150` for `EXTRA`; values strictly above the threshold belong to `HUGE`.
   - **Defect A2 (Visible labels and fractional-safe short badges):** `ProgressBand` currently uses legacy labels and integer-only badge ranges that can misrepresent fractional ratios. For example, 24.5%, 99.5%, and 150.5% cannot be accurately represented by integer-only ranges.
   - **Requirement A3 (Dual quote preservation):** Every `ProgressBand` must retain both `encouragementQuote` and `motivationQuote` as distinct, non-empty, supportive, non-pressuring strings. This preserves the approved D-216 Option A decision.

2. **Correction B — Complete v3 Portable Backup, `REPLACE`, `MERGE`, and Conflict Resolution (`app/src/main/java/com/example/dayseed/data/DayseedRepository.kt`):**
   - **Defect B1 (`REPLACE` in `importBackupArraysDirect`):**
     - `habit_streak_runs` is exported in v3 backups but is not imported by `importBackupArraysDirect` before `refreshAllSync()`.
     - Recalculation can consequently generate replacement `run_id` and `qualified_at_utc_ms` values instead of preserving valid backup identities and timestamps.
     - Legacy v2 backups without `week_start_history` need a baseline history row derived from the earliest restored relevant local date. Use `1970-01-01` only when the restored dataset contains no relevant dates.
     - The restored `week_start` setting must be preserved.
   - **Defect B2 (`MERGE` in `mergeBackupWithConflictDetection` and `resolveRestoreConflictSync`):**
     - **Habit metadata (`habit`):** Differences in `category` or `lifetime_goal_days` for an existing `habit_id` must not be silently ignored. Record a `"habit"` conflict and support its resolution.
     - **Habit date exceptions (`habit_date_exception`):**
       - Collisions on `(habit_id, local_date)` involving different `exception_kind` or `note` values must be recorded as conflicts.
       - Local recorded activity in `habit_entries` or `habit_day_outcomes` on the same date must also be considered when deciding whether the backup exception can safely be applied.
     - **Strict data-safety rule:** Resolving a `"habit_date_exception"` conflict with `USE_BACKUP` must never automatically delete local `habit_entries` or `habit_day_outcomes`.
       - If local activity exists and explicit destructive authorization has not been provided, reject the operation before commit.
       - The transaction must roll back, preserving the local activity, the unresolved conflict row, and the restore session's `AWAITING_CONFLICT_RESOLUTION` state.
       - Destructive authorization must come only from an explicit user confirmation for that specific conflict. It must never be enabled by default or silently supplied by an automated restore path.
       - A general confirmation to restore or merge a backup does not count as authorization to delete recorded activity.
       - If authorization is absent, preserve the local activity and require the user to choose another resolution.
       - If authorization is explicitly granted, apply the backup exception's actual `exception_kind` and `note`; do not hardcode `REST_DAY` unless the backup record itself specifies `REST_DAY`.
     - **Grove/streak runs (`habit_streak_runs` / `habit_streak_run` conflict type):**
       - Non-conflicting backup runs for existing habits must be imported before recalculation.
       - Validate the habit foreign key, `(habit_id, start_local_date)` identity collisions, and the single-active-run-per-habit invariant.
       - Do not silently discard conflicting run records. Record `"habit_streak_run"` conflicts where a backup run cannot safely be imported, and provide deterministic resolution in `resolveRestoreConflictSync`.
       - Conflict resolution must preserve valid run identities, creation timestamps, and qualification timestamps wherever the selected resolution permits.
       - A run conflict must not be resolved by blindly replacing unrelated or later run history. Check logical identity and active-run constraints before applying the selected resolution.
     - **Week-start history (`week_start_history`):**
       - Merge records by `effective_from_local_date`.
       - Insert non-conflicting records.
       - Record `"week_start_history"` conflicts when the same effective date has different `week_start_iso_day` values.
       - Support conflict resolution without silently overwriting the local history.

3. **Additional Defects Discovered During Source Inspection (`DayseedDatabaseHelper.kt`, `StreakAndGroveEngine.kt`, `DayseedRepository.kt`):**
   - **Defect C1 (Legacy v2 weekly schedule migration under U-11):**
     - The current `migrateV2ToV3FailClosed` normalization derives `target_period` solely from `schedule_kind`. This can overwrite legitimate legacy v2 `DAILY` + `WEEK` and `SELECTED_WEEKDAYS` + `WEEK` configurations.
     - **Fix:** Preserve weekly cadence and weekday eligibility during migration.
       - Map `(DAILY, WEEK)` to `(FLEXIBLE_WEEKLY_TARGET, WEEK)` with no weekday restriction (`preferred_weekdays_mask = NULL`).
       - Map `(SELECTED_WEEKDAYS, mask, WEEK)` to `(FLEXIBLE_WEEKLY_TARGET, WEEK)` while preserving the original weekday eligibility in `preferred_weekdays_mask = mask` and clearing the obsolete `scheduled_weekdays_mask = NULL`.
       - **Verified eligibility equivalence:** In `StreakAndGroveEngine.isScheduledOpportunityOnDate` (`StreakAndGroveEngine.kt:139–150`) and `evaluateWeeklyCadenceRuns` (`StreakAndGroveEngine.kt:481–537`), `FLEXIBLE_WEEKLY_TARGET` with `preferred_weekdays_mask = NULL` evaluates all seven weekdays as eligible opportunities, identical to `DAILY`. `FLEXIBLE_WEEKLY_TARGET` with `preferred_weekdays_mask = mask` evaluates `WeekdayMask.contains(mask, date.dayOfWeek)` for scheduled-date eligibility and U-4 rest-day exception holding while summing weekly progress across the week, identical to `SELECTED_WEEKDAYS` + `WEEK`.
       - `StreakAndGroveEngine` also directly evaluates any in-memory `(DAILY, WEEK)` or `(SELECTED_WEEKDAYS, mask, WEEK)` `HabitVersion` using `StreakUnit.WEEKS`.
       - Reject genuinely corrupt legacy rows (such as `FLEXIBLE_WEEKLY_TARGET` with `target_period = 'DAY'` or out-of-range weekday masks) rather than silently normalizing invalid data into a different valid configuration.
   - **Defect C2 (Weekly check-off `target_value` constraint):**
     - `buildHabitVersionsTableSql` currently requires `target_value IS NULL` for `COMPLETION_ONLY` and `EFFORT_AWARE`, even when `target_period = 'WEEK'`.
     - `saveHabitSync` also sets `normalizedTargetValue = null` for `COMPLETION_ONLY`.
     - **Fix:** Permit `target_value >= 1` for weekly `COMPLETION_ONLY` and `EFFORT_AWARE` configurations, while preserving the appropriate daily constraints (`target_period = 'DAY' AND target_value IS NULL`) and legacy weekly `NULL` compatibility.
     - Preserve the weekly target value in `saveHabitSync`. Do not silently convert a configured weekly target such as "3 days per week" into a target of 1 or `NULL`.
     - Keep the database CHECK constraints consistent with the approved D-216 rules.
   - **Defect C3 (`qualified_at_utc_ms` CHECK safety when a previously qualified run shrinks):**
     - If a run previously had `qualified_at_utc_ms != null` but recalculation reduces it below the qualification threshold, the engine must not retain a qualification timestamp while setting `qualified_for_grove = 0`.
     - **Fix:** Preserve the previous qualification timestamp only when the recalculated run remains qualified. Otherwise set `qualified_at_utc_ms = null`.
     - Preserve the original timestamp when the run remains qualified. Do not replace it with the current time merely because recalculation occurred.
     - Ensure `updated_at_utc_ms = maxOf(created_at_utc_ms, nowUtcMs)`.

---

## 2. Implementation Checklist

### Step 1 — Correction A: D-040 Progress Classification & Labels (`Models.kt`)

- [ ] Change the 150% classification comparison from `scaled < t150` to `scaled <= t150`.
- [ ] Ensure values strictly greater than 150% classify as `HUGE`.
- [ ] Update visible labels and fractional-safe short badges:

  | Band | Visible label | Short badge |
  |---|---|---|
  | ZERO | Zero logged | 0% |
  | TINY_STEP | Tiny | >0–<25% |
  | GETTING_STARTED | Small | 25–<50% |
  | HALFWAY | Halfway | 50–<75% |
  | ALMOST_THERE | Nearly there | 75–<100% |
  | GOAL_MET | Goal met | 100% |
  | EXTRA | Extra | >100–150% |
  | HUGE | Huge | >150% |

- [ ] Ensure the badge wording accurately represents fractional values. Do not replace the ranges with rounded integer ranges that imply incorrect boundaries.
- [ ] Preserve both `encouragementQuote` and `motivationQuote` on every band.
- [ ] Ensure the two quote fields are distinct, non-empty, supportive, and non-pressuring.
- [ ] Do not remove either quote field or combine them into one.
- [ ] Keep the language for HUGE supportive and non-judgmental. Avoid implying that the user must continually outperform previous progress.
- [ ] Preserve exact BigInteger-based ratio comparisons. Do not introduce floating-point calculations that could cause boundary errors.

### Step 2 — Correction B & Schema/Migration Fixes (`DayseedDatabaseHelper.kt`, `StreakAndGroveEngine.kt`, `DayseedRepository.kt`)

**A. `DayseedDatabaseHelper.kt`**

- [ ] Update `buildHabitVersionsTableSql` so weekly `COMPLETION_ONLY` and `EFFORT_AWARE` configurations can retain `target_value >= 1`.
- [ ] Keep daily check-off constraints intact.
- [ ] Preserve legacy weekly `NULL` values where backward compatibility requires them.
- [ ] Include `'habit_streak_run'` in `restore_conflicts.record_type` CHECK constraints in both `createV3Schema` and `migrateV2ToV3FailClosed`.
- [ ] Verify that every schema-creation and migration path produces the same accepted conflict record types. Do not update only one schema path.
- [ ] Existing v3 database compatibility: Preserve the approved D-219 database-version decision. Do not introduce database version 4 unless the owner explicitly approves changing D-219.
  - Inspect existing v3 databases to determine whether their actual SQLite table definitions contain the older CHECK constraints.
  - If they do, implement an idempotent, transactional v3 schema-reconciliation operation that detects the actual existing table definitions and upgrades affected tables without changing the canonical `DATABASE_VERSION = 3`.
  - Rebuild affected SQLite tables where necessary, including `habit_versions` and `restore_conflicts`.
  - Preserve all existing records, primary keys, foreign keys, indexes, and applicable constraints.
  - Run the reconciliation safely on database open, ensuring it does not repeatedly rebuild tables that already have the correct schema.
  - If reconciliation fails, roll back the operation and leave the original database recoverable.
  - Do not assume that changing `createV3Schema` updates existing v3 databases.
  - Add tests for fresh v3 creation, existing-v3 schema reconciliation, repeated/idempotent reconciliation, failed-reconciliation rollback, v2-to-v3 migration, and chained v1-to-v3 migration.
- [ ] In `migrateV2ToV3FailClosed`, preserve legacy weekly cadence:
  - `DAILY` + `WEEK` becomes `FLEXIBLE_WEEKLY_TARGET` + `WEEK` (`preferred_weekdays_mask = NULL`).
  - `SELECTED_WEEKDAYS` + `WEEK` becomes `FLEXIBLE_WEEKLY_TARGET` + `WEEK`, preserving weekday eligibility in `preferred_weekdays_mask` and clearing `scheduled_weekdays_mask`.
- [ ] Validate legacy data before migration in `validateV2DataBeforeMigration`. Reject invalid masks, impossible schedule/period combinations (such as `FLEXIBLE_WEEKLY_TARGET` with `target_period = 'DAY'`), and genuinely corrupt rows.
- [ ] Keep migration fail-closed and transactional. If validation or migration fails, the original v2 database must remain recoverable and unchanged.

**B. `StreakAndGroveEngine.kt`**

- [ ] Keep the prior qualification timestamp (`priorQualifiedAtUtcMs`) separately during recalculation.
- [ ] If the recalculated run remains Grove-qualified, preserve the existing `qualified_at_utc_ms`; only assign `nowUtcMs` when the run is newly qualified and has no prior qualification timestamp.
- [ ] If the recalculated run is no longer qualified, set `qualified_at_utc_ms = null`.
- [ ] Preserve `run_id` and `created_at_utc_ms` for existing runs whenever the same logical run survives recalculation.
- [ ] Ensure `updated_at_utc_ms` cannot be earlier than `created_at_utc_ms`.
- [ ] Verify weekly cadence uses `StreakUnit.WEEKS` for the supported weekly configurations.
- [ ] Verify recalculation does not create duplicate active runs or discard valid existing run identities.

**C. `DayseedRepository.kt`**

**`saveHabitSync`**

- [ ] Preserve configured `targetValue` for weekly `COMPLETION_ONLY` and `EFFORT_AWARE` habits.
- [ ] Do not silently replace a weekly target with `NULL` or a default value.

**`importBackupArraysDirect` — REPLACE**

- [ ] Import valid `habit_streak_runs` from `root.optJSONArray("habit_streak_runs")` before calling `refreshAllSync()`.
- [ ] Respect foreign-key dependency order: import or establish the referenced parent habits before importing dependent streak-run records.
- [ ] Validate foreign keys, logical run identities, and active-run uniqueness before inserting backup runs.
- [ ] Ensure recalculation preserves valid backup `run_id`, `created_at_utc_ms`, and `qualified_at_utc_ms`.
- [ ] When `week_start_history` is absent in a legacy v2 backup:
  - Determine the earliest relevant restored local date from the restored data.
  - Insert the baseline history row using that date and the restored `week_start` setting.
  - Use `1970-01-01` only when no relevant restored dates exist.
- [ ] Do not overwrite the restored `week_start` setting while seeding the baseline.
- [ ] Convert legacy v2 weekly schedule representations only when the backup version and legacy schema actually require conversion.
- [ ] Preserve weekly cadence and weekday eligibility during conversion.
- [ ] Keep the entire REPLACE operation transactional. A failed import must not leave a partially restored database.

**`mergeBackupWithConflictDetection` — MERGE**

- [ ] Process parent records before dependent records so foreign-key relationships remain valid.
- [ ] Detect differences in existing habit `category` and `lifetime_goal_days`.
- [ ] Record a `"habit"` conflict rather than silently ignoring differences.
- [ ] Import non-conflicting `habit_date_exceptions`.
- [ ] For date-exception conflicts, detect:
  - Different `exception_kind` values for the same `(habit_id, local_date)`.
  - Different `note` values for the same identity.
  - Existing local activity in `habit_entries` or `habit_day_outcomes` on that date.
- [ ] Record `"habit_date_exception"` conflicts using a stable record key `"$hId:$dStr"`.
- [ ] Import non-conflicting `habit_streak_runs` after validating:
  - The referenced habit exists.
  - The logical identity `(habit_id, start_local_date)` does not collide.
  - The single-active-run-per-habit invariant is maintained.
- [ ] Record unresolved `"habit_streak_run"` collisions instead of silently dropping backup data.
- [ ] Ensure run-conflict records contain enough backup and local identity information for deterministic resolution. Do not rely on a record key that cannot uniquely identify the conflicting backup run.
- [ ] Preserve valid backup Grove run IDs and timestamps through recalculation.
- [ ] Merge `week_start_history` by `effective_from_local_date`.
- [ ] Insert non-conflicting history rows.
- [ ] Record `"week_start_history"` conflicts when the same effective date has different `week_start_iso_day` values.
- [ ] Ensure conflicts remain visible and resolvable. Do not mark the restore session complete while unresolved conflicts remain.

**`resolveRestoreConflictSync`**

- [ ] Add resolution handling for `"habit"`, `"habit_date_exception"`, `"habit_streak_run"`, and `"week_start_history"`.
- [ ] Disallow `KEEP_BOTH` for records whose identity or database constraints make keeping both invalid.
- [ ] For `"habit_date_exception"`:
  - Reject `USE_BACKUP` when local recorded activity exists and explicit destructive authorization (`allowDestructiveActivityRemoval == true && authorizedDestructiveConflictId == conflictId`) has not been supplied.
  - Perform the rejection before commit so the transaction rolls back.
  - Preserve the local activity, unresolved conflict row, and `AWAITING_CONFLICT_RESOLUTION` state.
  - Require a specific, explicit user confirmation before any destructive activity removal.
  - Never set destructive authorization automatically, by default, or merely because the user selected a general backup-restore option.
  - Ensure authorization is tied to the exact conflict being resolved (`authorizedDestructiveConflictId == conflictId`) and cannot be reused for another conflict.
  - If explicitly authorized, apply the actual backup exception values (`exception_kind` and `note`) rather than assuming every exception is `REST_DAY`.
- [ ] For `"habit_streak_run"`:
  - Implement deterministic `KEEP_LOCAL` and `USE_BACKUP` behavior based on the actual conflicting run identities.
  - Do not delete or replace unrelated runs.
  - Check active-run uniqueness and foreign-key validity before applying the selected resolution.
  - Preserve valid backup `run_id`, `created_at_utc_ms`, and `qualified_at_utc_ms` when `USE_BACKUP` is selected and the database invariants permit it.
  - If applying the backup run would violate an invariant, reject the operation without partially modifying the database.
- [ ] For `"habit"` and `"week_start_history"`, ensure each resolution updates only the conflicting record and preserves unrelated metadata or history.
- [ ] For all conflict types, ensure the selected resolution is applied atomically.
- [ ] Ensure failed resolution attempts do not partially modify local data or mark conflicts resolved.
- [ ] Ensure restore completion is impossible while unresolved conflicts remain.

### Step 3 — Correction C: Expand Test Coverage (`DayseedAcceptanceTest.kt` and `Phase1ImplementationTest.kt`)

**A. D-040 progress classification**

- [ ] Test `150/100 = EXTRA`.
- [ ] Test `151/100 = HUGE`.
- [ ] Test `3/2 = EXTRA`.
- [ ] Test `4/2 = HUGE`.
- [ ] Test `100/100 = GOAL_MET`.
- [ ] Test small targets and fractional ratios, including values immediately below and above every classification boundary.
- [ ] Test BigInteger overflow safety and exact comparisons.
- [ ] Test all eight visible labels.
- [ ] Test all short badges against fractional values to ensure they do not misrepresent the boundaries.
- [ ] Test that every band has two distinct, non-empty quote fields.

**B. Migration tests**

- [ ] Add a chained v1 → v2 → v3 migration test.
- [ ] Verify survival of original v1 habits, records, journals, and settings.
- [ ] Verify readable legacy tracking modes.
- [ ] Verify all expected v3 tables and indexes against the actual schema definition.
- [ ] Verify week-start baseline and foreign-key integrity.
- [ ] Test existing v3 databases containing the old `habit_versions` CHECK constraint.
- [ ] Test existing v3 databases containing the old `restore_conflicts.record_type` CHECK constraint.
- [ ] Verify the reconciliation preserves all existing records, indexes, primary keys and foreign keys.
- [ ] Verify running reconciliation multiple times does not damage or unnecessarily rebuild an already-correct schema.
- [ ] Verify failed reconciliation rolls back completely.
- [ ] Verify `DATABASE_VERSION` remains consistent with D-219.
- [ ] Add fail-closed v2 → v3 rollback tests for:
  - Negative `value_units`.
  - `value_units > Int.MAX_VALUE`.
  - Unsupported `outcome_kind`.
  - Invalid calendar dates.
  - Invalid schedule/period combinations and weekday masks.
- [ ] Verify that a failed migration leaves the original v2 database intact and recoverable.
- [ ] Verify the migration does not partially update schema version, table definitions, or migrated records when it fails.

**C. Weekly-target tests (U-11)**

- [ ] Test legacy/v2 `DAILY` + `WEEK` migration.
- [ ] Test legacy/v2 `SELECTED_WEEKDAYS` + `WEEK` migration.
- [ ] Verify the migrated records retain their intended weekly cadence and weekday eligibility.
- [ ] Verify weekly cadence evaluation uses `StreakUnit.WEEKS`.
- [ ] Test v3 `habit_versions` CHECK constraints against invalid schedule/period combinations.
- [ ] Test weekly `COMPLETION_ONLY` and `EFFORT_AWARE` targets with values greater than 1.
- [ ] Test that daily configurations still obey their original constraints.

**D. Streak lifecycle and historical reconstruction**

- [ ] Test `PAUSED`, `ARCHIVED`, and `COMPLETED` lifecycle transitions.
- [ ] Verify active runs close with the corresponding `terminal_reason`.
- [ ] Verify `broken_on_local_date == null` for lifecycle closures where required.
- [ ] Test backdated entry recalculation under D-188.
- [ ] Verify a previously MISSED break can be healed when the rules allow it.
- [ ] Verify surviving logical runs retain their original `run_id` and `qualified_at_utc_ms`.
- [ ] Verify recalculation never bridges across a PAUSED range under U-1 and U-5.
- [ ] Test a previously qualified run shrinking below the qualification threshold. Verify its qualification flag and timestamp remain consistent with the database CHECK constraint.
- [ ] Verify recalculation does not create duplicate active runs or alter unrelated historical runs.

**E. Complete v3 backup REPLACE and MERGE tests**

- [ ] Test v3 REPLACE round-trip preservation of:
  - Habit `category`.
  - `lifetime_goal_days`.
  - `habit_date_exceptions`.
  - `habit_streak_runs`, including `run_id` and `qualified_at_utc_ms`.
  - `week_start_history`.
- [ ] Test legacy v2 backup baseline seeding when week-start history is absent.
- [ ] Verify the baseline uses the earliest relevant restored date and preserves the restored week-start setting.
- [ ] Test MERGE non-conflicting imports.
- [ ] Test Grove run identity and timestamp preservation.
- [ ] Test conflict creation and resolution for:
  - `habit`.
  - `habit_date_exception`.
  - `week_start_history`.
  - `habit_streak_run` collisions.
- [ ] Verify local records remain intact when a conflict is unresolved or resolution is rejected.
- [ ] Verify destructive REST_DAY conflict resolution is rejected unless the user explicitly authorizes the specific destructive operation.
- [ ] Verify authorization for one conflict cannot authorize deletion for another conflict.
- [ ] Verify failed conflict resolution leaves the conflict unresolved and the restore session awaiting resolution.
- [ ] Verify a restore session cannot complete with unresolved conflicts.
- [ ] Verify foreign-key integrity and active-run uniqueness after both REPLACE and MERGE.
- [ ] Verify failed REPLACE or MERGE operations roll back without leaving partially imported data.
- [ ] Verify conflict resolution does not overwrite unrelated local records.

### Step 4 — Corrections D & E: Full Verification and Documentation Reconciliation

- [ ] Run `compile_applet`.
- [ ] Run `gradle :app:testDebugUnitTest --rerun-tasks`.
- [ ] Inspect the XML test reports under `app/build/test-results/testDebugUnitTest/`.
- [ ] Record the exact test count and test outcomes from the generated reports.
- [ ] Investigate every failure. Do not treat a successful build as proof that all acceptance requirements are satisfied.
- [ ] Verify the exact commit SHA that was tested. If any code changes occur after the test run, rerun the required verification against the new final commit.
- [ ] Reconcile root `/implementation_plan.md`, `/docs/28-REQUIREMENT-TRACEABILITY.md`, and `/docs/12-PROGRESS-AND-HANDOVER.md`.
- [ ] Record the corrected defects, implementation changes, changed files, exact tested commit, actual test results, and outstanding issues.
- [ ] Do not claim emulator or physical-device validation unless it was actually performed.

---

## 3. Implementation Rules and Completion Criteria

1. **Continue from the current implementation.** This is a corrective completion task, not a new project-planning exercise. Do not redesign unrelated features or reopen already-approved decisions.
2. **Preserve approved requirements.** Do not silently change D-040, D-216, U-11, streak lifecycle semantics, or existing owner-approved decisions to make implementation easier.
3. **Inspect before changing.** Check the current implementation and database schema before editing. The line numbers in this plan are navigation references, not permission to assume the code is unchanged.
4. **Protect user data.** Backup and restore operations must remain transactional. Never silently discard local activity, backup records, run identities, or historical settings.
5. **Do not weaken constraints to make tests pass.** Correct the underlying migration, schema, repository, or engine behavior.
6. **Test behavior, not merely implementation details.** Tests must verify observable outcomes, data integrity, migration recovery, conflict resolution, and identity preservation.
7. **Do not invent completion evidence.** Report only tests that were actually executed. Inspect generated test reports and identify the exact commit they validate.
8. **Keep the work within Phase 1.** Do not begin Phase 2 or introduce unrelated features.
9. **Preserve the existing validation gates.** Technical implementation and test completion do not automatically constitute independent Stage C validation or final Stage D acceptance. Keep those boundaries visible in the handover.

## 4. Required Final Report

When implementation and verification are complete, provide:

- **Defects addressed:** Each original defect and its actual correction.
- **Files changed:** Exact paths and a brief explanation of each change.
- **Data safety:** How REPLACE, MERGE, conflict resolution, migration rollback, and user-data preservation were verified.
- **Tests:** Exact test count, pass/fail results, and the inspected XML report evidence.
- **Build:** Actual `compile_applet` result.
- **Commit:** Exact final commit SHA that was tested.
- **Repository status:** Branch, commit, PR status, and whether changes have been merged into `main`.
- **Outstanding issues:** Anything not implemented, not verified, or blocked.
- **Phase 1 status:** Explicitly state `PASSED` or `NOT PASSED`, based on the actual evidence.

Do not mark Phase 1 as passed if required corrections or tests remain incomplete. Do not represent Stage C independent validation or Stage D final acceptance as completed unless those separate gates have actually been performed and authorized.
