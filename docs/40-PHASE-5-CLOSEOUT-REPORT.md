# Dayseed Phase 5 — Stage B Integration Closeout Report

**Date:** 2026-11-10
**Repository:** `HeWhoArose/Dayseed`, branch `main`
**Verified baselines:** `e68b74b752e75aaccafa7aa1e684450ee6707f16` and, re-verified after the documentation commits, `56942fb` (both clean-extraction sweeps recorded in `docs/evidence/phase5/e68b74b.md`)
**Scope:** Stage B integration closeout only. No new product features, no UI redesign, no change to habit, streak, reminder, backup or privacy semantics, no production signing.

## 1. Implemented in Phase 5

| Item | Commit | Detail |
| --- | --- | --- |
| Phase 4 correction record corrected | `baba895` | `docs/12`/`docs/28` now state the fixes were committed at `b0fc29d`, with the exact-tree sweep and the honest `p4_33`/`p4_34` coverage limits |
| Reproducible build infrastructure | `9c46edf` | Gradle 9.3.1 wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, properties), `scripts/bootstrap-debug-keystore.sh` + `.ps1`, README prerequisites and debug-signing note |
| Two regression tests closing the F-2/F-3 coverage gaps | `6561588` | `p4_35` (eligible habit stays armed while a stale registration is cleared by a week-start change), `p4_36` (MERGE restore never adopts a foreign scheduler registry and never turns it into a portable conflict) |
| Plan, README and register reconciliation | `678c1c7` | Plan header/§10/new §18; README v3 / 15 tables / build / status; `docs/28` Phase 4 A–D matrix; `docs/07` and `docs/32` status headers; `docs/25` D-232 row and count 226 |
| Consolidated handover | `e68b74b` | Explicit per-case status for all 70 human/device cases; `docs/12` handover table, execution rules and artifact inventory |
| Evidence + report | this commit | `docs/evidence/phase5/e68b74b.md` and this report |

## 2. Regression tests and their negative controls

Both new tests were executed twice: once normally and once with the production line they guard removed.

- `p4_35` — removed the `rescheduleReminderAlarms()` call from `setWeekStartSync` → **FAILED**; restored → **PASSED**.
- `p4_36` — removed the MERGE import filter for `reminder_scheduled_alarm_ids` → **FAILED**; restored → **PASSED**.

`p4_36` also documents a pre-existing MERGE behaviour rather than hiding it: merging an export taken from the same database still raises a conflict on `reminders_enabled` (restore forces reminders off). The test asserts the scheduler key specifically and does not assert overall merge completion.

## 3. Verified by the clean-extraction sweep

- Build from an empty directory through the committed wrapper: **BUILD SUCCESSFUL**.
- Tests: **217/217 across 14 classes**, 0 failures, 0 errors, 0 skipped; `Phase4ImplementationTest` 36/36. The sweep was repeated at `56942fb` with the toolchain rebuilt from scratch and produced identical counts and APK sizes.
- Lint: **0 errors, 58 warnings** (58 vs 57 explained in the evidence log as a remote version-availability suggestion).
- Debug APK 19,126,660 bytes; release APK 12,671,118 bytes, **debug-signed** (`CN=Android Debug`), confirmed with `apksigner`.
- The `debug.keystore` bootstrap is required and works from a clean tree; no `local.properties` is needed when `ANDROID_HOME` is set; neither file is tracked.

## 4. Not run — human/device validation

All **70** cases remain `NOT RUN` and each now carries an explicit per-case status:

| Group | Cases |
| --- | --- |
| `P1-H` | 9 |
| `P2-C` | 9 |
| `P2-H` | 17 |
| `P25-H` | 7 |
| `P3-H` | 9 |
| `P4-H` | 12 |
| `RM-H` | 7 |
| **Total** | **70** |

Not established anywhere in this closeout: real notification delivery and permission denial, reboot/time-zone/Doze behaviour, the SAF document-picker flow, delete-all outcomes on a device, airplane-mode use, TalkBack and 200% text, measured contrast, focus/touch targets, and large-history performance on representative hardware.

## 5. Known limitations and blockers

- **Release signing:** both variants use the gitignored `debug.keystore`; the release APK is an installation artifact only. Production signing material requires an owner decision and was not introduced.
- **PowerShell bootstrap:** authored but **not executed** here (no PowerShell available); it must be run once on Windows before it can be called verified.
- **Debug signature is per machine:** each bootstrap creates a new certificate, so APK signatures are not identical across environments even when content is.
- **Lint warnings:** 57–58 non-error warnings remain; they are optional improvements, not blockers for this closeout.

## 6. Gate status

- **Phase 4 Stage B implementation:** complete (`7bb8d80`, `6ae1ea1`, `b0fc29d`).
- **Phase 5 Stage B integration closeout:** complete for everything that can be established without a device, subject to the limitations in §5.
- **Stage C — independent technical validation:** **NOT OPEN**.
- **Stage D — owner acceptance / release readiness:** **NOT OPEN**.

Nothing in this report constitutes Stage C validation, Stage D acceptance, a production-signed release, or device evidence.
