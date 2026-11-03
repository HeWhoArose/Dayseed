# Phase 1 Human-Intervention Test Register

**Project:** Dayseed  
**Phase:** 1 — Domain, data-integrity and historical-record foundation  
**Status:** Prepared for later execution; **NOT RUN**  
**Execution window:** After the planned Stage B implementation phases are complete, as part of one consolidated human/device validation session.  
**Authority:** Phase 1 requirements in the canonical root \`implementation_plan.md\`, Phase 1 Implementation Plan Revision 7, and owner-approved decisions D-216 through D-219 and U-1 through U-13.

## 1. Purpose and execution rule

This register captures Phase 1 checks that cannot be adequately established by unit tests, repository tests, migration fixtures, or static source inspection alone. They require a real Android installation, interaction with the finished UI, observation of Android lifecycle behaviour, or owner review.

**Do not run these checks immediately after Phase 1.** Add them to the consolidated human-validation session and run them after the planned Stage B phases are integrated. Some cases depend on screens or system workflows delivered in later phases; those dependencies are stated below.

This deferral applies only to human/device checks. Gemini must still run automated unit, repository, migration, backup/restore, and regression tests after each implementation phase. Automated tests added in the Phase 1 remediation must also be executed before Phase 1 is treated as technically verified.

This document is a test plan, not evidence that any test has passed. Stage C independent validation remains a separate gate requiring its own explicit authorisation after Stage B implementation is complete.

## 2. Safety and evidence rules

- Use synthetic test habits and synthetic journal/record data. Do not use the owner's real personal history for destructive migration, restore, deletion, or conflict tests.
- Use a disposable test installation/device profile. Before any destructive scenario, create a recoverable test backup.
- Record the exact Git commit SHA and APK/build identifier under test.
- Record device model, Android version, app version, date/time, steps, expected result, actual result, and evidence.
- Use only these outcomes: **PASS**, **FAIL**, **BLOCKED**, or **NOT RUN**. A blocked test is not a pass.
- Capture screenshots or screen recordings for visible states. Capture logs only where useful; never include Journal text, habit notes, backup passwords, tokens, or other private data.
- If a scenario risks losing data, stop and restore the disposable fixture. Do not improvise a destructive recovery.
- A screenshot of a design mock-up is not evidence of actual device behaviour.

## 3. Phase 1 human-intervention cases

### P1-H01 — Upgrade an existing v2 installation to v3 without losing history

**Why human intervention is needed:** A real app upgrade exercises Android installation and app-private database continuity in addition to the automated migration fixture.

**Dependency:** A reproducible v2 test installation or a documented way to seed a synthetic v2 database on the test device.

**Procedure:**
1. Install the v2 test build and create synthetic habits covering Quantity, Duration, and Simple check-off/legacy-compatible records.
2. Add multiple sessions on one date, an explicit zero record, a completed outcome, date notes, and at least two effective-dated habit configurations.
3. Record the visible totals and history for later comparison.
4. Install the v3 build as an upgrade over the same package without clearing app data.
5. Open Dayseed and inspect the same habits and historical dates.

**Expected:** The upgrade completes without a reset or silent loss. Habit IDs, historical entries/outcomes, notes, effective-dated configuration, and visible totals remain consistent. Any migration block is explicit and leaves the original data recoverable.

**Evidence:** Before/after screenshots, device/app versions, APK identifiers, commit SHA, and migration outcome.

### P1-H02 — Data persistence after force-stop and process death

**Why human intervention is needed:** This verifies the installed Android app and its process lifecycle, not merely an in-memory repository test.

**Procedure:**
1. Create synthetic habits and save several sessions, including an explicit zero and a note.
2. Navigate away and return; verify the saved state.
3. Force-stop Dayseed from Android App Info or the device's app controls.
4. Relaunch Dayseed.
5. Where practical, repeat after Android reclaims the app process or after a device restart.

**Expected:** Committed records, effective configuration, streak/Grove history, and settings reload from persistent storage. No duplicate sessions appear and no saved record silently disappears.

**Evidence:** Before/after screenshots and exact device/build details.

### P1-H03 — Same-day configuration protection after real activity

**Procedure:**
1. Create a scheduled habit and record a qualifying entry.
2. Attempt to change an evaluation-affecting target or schedule with an Apply-today action.
3. Repeat with an explicit zero record.
4. On a separate test habit, create a REST_DAY exception without recording activity, then attempt the same configuration edit.

**Expected:** Existing activity, including explicit zero, protects today's historical evaluation and routes the change to the approved later effective date. A REST_DAY exception alone does not count as qualifying activity and does not trigger the activity lock.

**Evidence:** Screenshots of the offered action, saved configuration, and selected-date result.

### P1-H04 — Weekly target semantics and week-start history

**Procedure:**
1. Create a weekly Quantity or Duration habit and record multiple sessions across several dates.
2. Create a weekly Simple check-off habit and complete it on distinct dates.
3. Create a weekly session-count habit and record multiple sessions on one date.
4. Inspect the displayed totals and qualifying progress.
5. Change the configured week-start day in a disposable profile.
6. Inspect the completed historical period, the current transition period, and the first period using the new boundary.
7. Change a weekly target mid-week; separately attempt a unit/measurement-mode change.

**Expected:** Quantity/Duration uses summed compatible units; days/week counts distinct qualifying dates; sessions/week counts sessions. A completed historical week is not repartitioned. The transition interval follows the approved neutral rule. The current week's target/metric snapshot is not retroactively changed; unit/metric-mode changes take effect only at the approved weekly boundary.

**Evidence:** Screenshots showing dates, week labels, totals, and effective target values.

### P1-H05 — One-off REST_DAY behaviour

**Procedure:**
1. On an eligible scheduled date, choose **Take a rest day** for one habit.
2. Inspect Today, History/Calendar, streak display, and weekly target progress.
3. Confirm no activity entry or completed outcome was created.
4. Attempt REST_DAY on an unscheduled date and on a past/future date.
5. Log real activity, including an explicit zero in a separate run, on a date that currently has REST_DAY.
6. Add a date note without logging activity.

**Expected:** REST_DAY is accepted only under U-1 eligibility rules; it is neutral, holds the relevant streak, does not create activity, does not inflate active-day/check-in totals, and does not reduce a weekly target. Real activity transactionally clears the exception; a note alone does not.

**Evidence:** Screenshots of the date state and totals before/after each action.

### P1-H06 — Individual streak independence and Grove lifecycle

**Procedure:**
1. Create at least two habits with different cadences.
2. Record full progress, positive partial progress, explicit zero, a missed scheduled opportunity, and a REST_DAY in separate controlled scenarios.
3. Pause one habit, resume it, and later qualify a new run.
4. Archive one habit and mark another Completed.
5. Inspect the active streak indicator and the Grove/history view.
6. Reopen the app and inspect the same records.

**Expected:** Each habit has an independent run. Partial progress and rest follow the approved hold rules; explicit zero is neutral; a pause closes the run with PAUSED and a later qualifying run starts separately. Archive and completion retain distinct terminal histories. Mixed-cadence comparison displays the agreed elapsed span with an explicit unit. Grove qualification and unqualified short-run history remain distinct.

**Dependency:** Habit Detail/Grove UI from Phase 2.

**Evidence:** Screenshots and a date-by-date record of the scenario.

### P1-H07 — Backdated correction does not cross lifecycle boundaries

**Procedure:**
1. Build a synthetic history with a missed gap and a separate PAUSED, ARCHIVED, or COMPLETED closure.
2. Use the finished record-edit flow to add or correct a backdated entry around the missed gap.
3. Inspect the affected run identities and Grove history.
4. Attempt a correction that would appear to bridge a lifecycle closure.

**Expected:** Approved MISSED-gap healing/recalculation behaves deterministically. A backdated record never reopens or bridges a PAUSED, ARCHIVED, or COMPLETED boundary. Unrelated habits and run histories remain unchanged.

**Dependency:** Phase 2 record correction and Habit Detail/Grove UI.

**Evidence:** Before/after screenshots and the exact synthetic timeline.

### P1-H08 — Portable backup compatibility and full v3 round-trip

**Procedure:**
1. Export a v3 portable backup from a disposable installation containing the Phase 1 data families.
2. If a supported v2 backup fixture is available, restore it into the v3 app using the supported restore flow.
3. Restore the v3 backup into a clean disposable installation.
4. Exercise supported Replace/Merge conflict choices with synthetic collisions.
5. Verify the restored habits, effective versions, entries, outcomes, notes, REST_DAY exceptions, streak runs, and week-start history.

**Expected:** Supported v2 JSON maps explicitly into v3. A v3 round-trip preserves IDs and supported data. Unsupported/corrupt input fails closed; no partial restore is presented as successful. Conflict resolution does not silently delete unrelated activity.

**Dependency:** Phase 4 Android file-picker/backup UI. The data-mapping and rollback assertions remain automated-test responsibilities too.

**Evidence:** Sanitised backup metadata, before/after screenshots, restore outcome, and commit/build details. Never share backup passwords or private payload contents.

### P1-H09 — Habit deletion Undo

**Procedure:**
1. Create a synthetic habit with versions, entries, date notes, REST_DAY/streak history, and any supported linked records.
2. Permanently delete it through the UI.
3. Use the offered Undo within the documented eight-second window.
4. Repeat after expiry, after consuming the token, and with a deliberately conflicting/reused identity in a disposable fixture where the UI supports it.

**Expected:** Successful Undo restores the complete supported habit payload atomically. Expired, consumed, or colliding Undo is rejected without partial restoration or overwriting newer data.

**Dependency:** Phase 2 lifecycle/detail UI.

**Evidence:** Before/after counts and screenshots. Collision cases that cannot be safely triggered through the UI remain automated tests.

## 4. Tests that should remain automated

Do not replace these with manual checks:

- Exact BigInteger arithmetic, boundary ratios, very large totals, and display formatting.
- Every SQLite CHECK, FK, index, trigger, migration row-count/primary-key comparison, rollback, and integrity assertion.
- Malformed metadata and invalid/out-of-range legacy values.
- Deterministic streak-engine permutations, tie-breaks, run identity, and all U-1 through U-13 edge cases.
- Backup schema/format-version combinations, payload validation, atomic rollback, conflict authorization, and deletion collision guards.
- Race conditions, transaction boundaries, and idempotency.

Human review supplements these tests; it does not replace them.

## 5. Consolidated execution gate

Run P1-H01 through P1-H09 together with the human/device checks added by later phases only after:
1. All planned Stage B implementation phases are complete and committed.
2. The full automated suite and debug build have current, inspectable evidence.
3. The exact candidate commit and test APK are frozen for validation.
4. Stage C independent validation has been explicitly authorised.

At that point, execute one coordinated session, record every case as PASS/FAIL/BLOCKED/NOT RUN, triage failures, and repeat affected cases against the corrected candidate. Do not describe Stage C as passed while any required case is failed, blocked, or not run without an explicitly accepted disposition.
