# Dayseed — System-Wide Audit, 2026-11-11 (post Phase 5 closeout)

**Date/time:** 2026-11-11, 05:55–06:45 local (WCAST, UTC+1).
**Repository:** workspace of `HeWhoArose/Dayseed`, branch `main`.
**Inspected commit:** `b30efbc7f830b57baae96d32bc64d30a549b8806` (`b30efbc`), the SHA recorded as remote `main` HEAD in the Phase 5 closeout report. Remote head was not re-fetched in this session (no credentials in this shell); every file finding below is specific to `b30efbc` and the uncommitted edits listed in §6.
**Type of audit:** Stage B self-audit. It is **not** Stage C independent validation and does not open Stage C or Stage D.

## 1. What was executed

| Action | Scope | Result |
| --- | --- | --- |
| Full build, test, lint sweep | `clean :app:testDebugUnitTest --rerun-tasks :app:assembleDebug :app:assembleRelease :app:lintDebug` | **BUILD SUCCESSFUL**, 217/217 tests (14 classes, 0 failures/errors/skips), lint 0 errors / 57 warnings, debug APK 19,126,660 B, release APK 12,671,118 B (debug-signed) |
| Recorded as | `docs/evidence/phase5/reverify-windows-2026-11-11.md` | Reproduced the `docs/40` §3 numbers exactly on a second environment |
| Lint triage | All 57 warnings classified by id from `lint-results-debug.xml` | Every one is cosmetic or advisory; no correctness, privacy or data-integrity warning |
| Document consistency scan | `docs/`, `README.md`, `implementation_plan.md` cross-references for HEAD SHAs, test counts, APK sizes, "scheduled to close" / pending language | 3 stale statements found and corrected (§3) |
| Static review of the reminder delivery pipeline | `ReminderScheduler.kt`, `ReminderAlarmReceiver.kt`, `ReminderNotifier.kt`, `ReminderReconcileWorker.kt`, `ReminderSystemEventReceiver.kt` | No defect found (§4) |
| Static review of destructive paths | `DayseedRepository.deleteAllDataSync`, `setSettingSync("week_start")`, `setWeekStartSync` | No defect found (§4) |
| Manifest review | `app/src/main/AndroidManifest.xml` | No defect found (§4) |

## 2. Scope and evidence limits

- **Uninspected:** `DayseedRepository.kt` beyond the settings/reset regions, `DayseedDatabaseHelper.kt` migration internals, `BackupAndExportManager.kt`, all Compose screens, `DayseedViewModel.kt`, the theme/artwork code, and 30 of 40 main-source files. This audit is therefore **not** a whole-codebase line-by-line review; it is a build-plus-targeted review with full automated coverage.
- **Not performed:** device/emulator execution, notification delivery, SAF picker, TalkBack traversal, contrast measurement, 200% text layout, Doze/reboot behaviour, large-history performance. All 70 cases in `docs/38` remain `NOT RUN`.
- Anything below marked "verified" means *executed in this session on the environment recorded in the evidence file*; nothing here is inferred from source presence.

## 3. Findings and dispositions

| ID | Severity | Finding | Disposition |
| --- | --- | --- | --- |
| A-1 | Medium (documentation accuracy) | `docs/28-REQUIREMENT-TRACEABILITY.md` still declared "Current GitHub `main` HEAD: `3ca397f`" and "Latest application-code baseline: `dd6501f`" — superseded by Phases 4 and 5. | **Fixed.** Header now names `b30efbc` as HEAD and `6561588` as the application-code baseline, with the intermediate chain retained. |
| A-2 | Medium (documentation accuracy) | The same file's Phase 4 corrections section said `p4_35`/`p4_36` "are scheduled to close both gaps during the Phase 5 closeout", although both tests were committed at `6561588` and have since passed twice. | **Fixed.** Now states both gaps were closed at `6561588`, with what each test asserts. |
| A-3 | Low (documentation accuracy) | `docs/12` and `implementation_plan.md` named `b0fc29d` as "current HEAD" while four documentation commits exist on top of it. | **Fixed.** Both now name `b30efbc` and list the intervening Phase 5 commits. |
| A-4 | Low (onboarding reliability) | `README.md` required JDK 21 but did not warn that Gradle honours `JAVA_HOME`, so a machine whose `PATH` has JDK 21 but whose `JAVA_HOME` points at JDK 17 fails with `error: invalid source release: 21`. This is exactly what happened during the first sweep attempt here. | **Fixed.** README prerequisite now states the `JAVA_HOME` requirement and the error it produces. |
| A-5 | Info (reproducibility) | A clean worktree cannot build until `scripts/bootstrap-debug-keystore.sh` runs; the failure (`validateSigningDebug`, keystore not found) is correct but cryptic. | **No code change.** Already documented in README and `docs/40` §3; recorded again in the evidence file as an expected first-run step. |

## 4. Reviewed and found correct (no change made)

- **Alarm registry lifecycle.** `scheduleAll` recomputes eligibility from repository state, registers inexact `setWindow` alarms for the next *scheduled* day, cancels ids in the persisted registry that are no longer eligible, and rewrites the registry as a whole; weekly-target-met habits are pushed to the next progress-week boundary via `notBefore`. Re-running is idempotent.
- **Fire-time re-validation.** `ReminderAlarmReceiver.processAlarmSync` re-checks action, habit id, persisted reminder time, delivery date, the 6-hour stale grace and the 10-minute early window *before* any ledger write, then applies the global opt-in and `POST_NOTIFICATIONS` gates, then acquires the posting claim inside one `BEGIN IMMEDIATE` transaction. A stale or blocked alarm therefore writes nothing and posts nothing.
- **Honest ledger promotion.** The row is promoted to `DELIVERED` only after `notify` returns without throwing; a failed post is corrected to `SUPPRESSED`/`POST_FAILED` with the caller's claim token, so a claim cannot be finalised by a different invocation.
- **Crash recovery ordering.** `ReminderReconcileWorker` drains the cancel outbox, reconciles stale reservations, runs the weekly pass, then re-registers alarms — a sensible order that cannot resurrect a cancelled notification.
- **Delete-all ordering (F-1).** `deleteAllDataSync` cancels registered alarms **before** the reset transaction removes the registry, enqueues cancellation for every recorded platform notification, then clears all 15 tables and reseeds the onboarding and legacy week-start rows.
- **Week-start change (F-2).** Both `setWeekStartSync` and the legacy-baseline branch of `setSettingSync("week_start")` refresh state and call `rescheduleReminderAlarms()`.
- **Manifest.** `allowBackup="false"` plus both backup rule files; no `INTERNET` permission; `POST_NOTIFICATIONS` and `RECEIVE_BOOT_COMPLETED` only; `MainActivity` is the only exported component; both receivers are `exported="false"`, which still permits protected system broadcasts while blocking third-party senders; the `TIME_SET` action in the filter is the correct manifest name for `Intent.ACTION_TIME_CHANGED`.
- **Lint warnings.** `UseKtx` (19), launcher-icon shape/duplicate/location/monochrome (20), `GradleDependency`/`NewerVersionAvailable`/`AndroidGradlePluginVersion` (12), `InlinedApi` (3), `DefaultLocale` (1), `ObsoleteSdkInt` (1), `UnusedResources` (1) — all advisory; none concern privacy, data integrity, correctness or accessibility regressions.

## 5. Residual risks and limitations (unchanged by this audit)

- **Device behaviour is unverified** — the entire functional surface that depends on Android delivering alarms, showing notifications, opening SAF or rendering large text has never been executed: 70/70 cases `NOT RUN`.
- **Release signing uses the debug identity.** Each bootstrap mints a new certificate (three distinct fingerprints observed across machines), so APK signatures differ per environment. Production signing needs an owner decision.
- **PowerShell bootstrap remains unexecuted** (no PowerShell in the verification environments used so far).
- **Build cost is high** (18m 15s cold on this Windows machine) and the project has no CI workflow, so regressions are only caught when someone runs the sweep.
- **Android's inexact alarms** mean reminder timing is approximate by design; copy must never promise an exact time.
- **Stage C / Stage D remain NOT OPEN.** Nothing in this audit is independent validation or owner acceptance.

## 6. Edits produced by this audit (committed 2026-11-11)

- `docs/28-REQUIREMENT-TRACEABILITY.md` — A-1, A-2.
- `docs/12-PROGRESS-AND-HANDOVER.md` — A-3 plus a pointer to this audit and the evidence file.
- `implementation_plan.md` — A-3.
- `README.md` — A-4.
- `docs/evidence/phase5/reverify-windows-2026-11-11.md` — this session's sweep record.
- This file, plus §7 below covering the remediation pass.

## 7. Remediation pass executed — 2026-11-11

**Environment:** Windows 11 (`MINGW64_NT-10.0-26200`), Temurin JDK 21.0.13+9-LTS, Gradle 9.3.1, branch `main`.
**Command:** `./gradlew --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`
**Result:** **BUILD SUCCESSFUL** — 217/217 tests across 14 classes, 0 failures, 0 errors, 0 skipped; lint **10 warnings, 0 errors** (was 57); debug APK **19,508,532 bytes** (was 19,126,660; +381,872 bytes).

### 7.1 Two real defects found while fixing the reported items

| ID | Severity | Finding | Disposition |
| --- | --- | --- | --- |
| A-6 | **High (functional)** | `ContextCompat.checkSelfPermission(POST_NOTIFICATIONS)` was called **without an API guard** in `ReminderAlarmReceiver`, `ReminderDebugProbe` and `SettingsPanels`. That permission does not exist below API 33, so the query reports `DENIED`: every reminder would be suppressed on Android 8-12, and Settings would claim the user had denied a permission nobody had to grant. The one `SDK_INT >= 33` check in `SettingsPanels` guarded the *request*, not the *check*. | **Fixed** at all three sites; below API 33 the runtime permission is treated as satisfied and notification-channel state remains the real gate. |
| A-7 | **High (developer tooling)** | `scripts/bootstrap-debug-keystore.ps1` aborted with exit 1 on any machine whose JDK writes to stderr (`JAVA_TOOL_OPTIONS` printing "Picked up ..."). `$ErrorActionPreference = 'Stop'` promotes native stderr to a terminating error, so the script died after `keytool` had already succeeded. | **Fixed** with an `Invoke-Keytool` helper that judges native calls by exit code; three paths verified — see [docs/evidence/phase5/powershell-bootstrap-2026-11-11.md](evidence/phase5/powershell-bootstrap-2026-11-11.md). |

### 7.2 Lint remediation (57 → 10)

- **Converted** the four `toColorInt` sites to the KTX extension.
- **`UseKtx` suppressed with an explicit rationale**, not converted, on the write paths using `beginTransaction()` / `BEGIN IMMEDIATE`: androidx KTX `transaction {}` cannot request an immediate write lock, so converting them would trade a lint warning for weaker concurrency guarantees behind the reminder-posting claim (§4). Four `UseKtx` findings were genuinely fixed, so this is a documented exception, not blanket silencing.
- **Launcher assets rebuilt as adaptive-only** (`minSdk` is 26, so legacy PNG mipmaps were dead weight): removed five density buckets and `anydpi-v26`, added a `mipmap-anydpi` adaptive icon with a `<monochrome>` themed-icon layer, pointed `roundIcon` at the same adaptive icon, moved the artwork to `drawable-nodpi`, and deleted a byte-identical unused duplicate JPEG.
- **`DefaultLocale` fixed:** reminder times now format with `Locale.ROOT`. This is a correctness fix — a locale using non-ASCII digits could otherwise emit a malformed `HH:mm` that the reminder re-check parses back.
- **APK size grew** by +381,872 bytes despite the deleted assets: the themed-icon layer plus two dependency bumps outweighed the removed PNGs. Reported rather than dressed up as a win.
- **Remaining 10:** `GradleDependency` ×7, `NewerVersionAvailable` ×2, `AndroidGradlePluginVersion` ×1 — all version advisories in `gradle/libs.versions.toml`, none a code defect.

### 7.3 Deferred dependency advisories (owner decision)

Available: AGP `9.1.1 → 9.5.1`, Kotlin Compose plugin `2.2.10 → 2.4.21`, Compose BOM `2024.09.00 → 2026.09.00`, lifecycle `2.8.7 → 2.11.0`, activity-compose `1.10.1 → 1.13.0`, navigation-compose `2.8.9 → 2.10.2`, work `2.9.1 → 2.12.0`, core-ktx `1.18.0 → 1.19.1`, Robolectric `4.14.1 → 4.17`.

These are multi-year jumps, not patches: they would change Compose rendering, lifecycle behaviour and the AGP/Kotlin toolchain, none of which can be regression-tested without a device (§5). Only two low-risk bumps were taken (`material-icons-extended 1.7.0 → 1.7.8`, `androidx.test:core 1.6.1 → 1.7.0`). The rest are named deferrals requiring an owner decision and a device pass — not an assertion that the current versions are ideal.

### 7.4 CI

`.github/workflows/android.yml` authored: Ubuntu, Temurin JDK 21, Gradle cache, keystore bootstrap, then `testDebugUnitTest lintDebug assembleDebug`, uploading JUnit XML and lint reports. **Never executed** — the first green run is the evidence. Authored, not verified.

### 7.5 Device validation — BLOCKED

`adb` exists at the SDK path but lists **zero** devices; `C:\Users\AHMAD\.android\avd` does not exist and the SDK has **no emulator binary**. `P25-H03` and every other `docs/38` case remain **`NOT RUN`**, so the reminder activation switches stay disabled. Enabling them would assert a delivery path nobody has observed.

### 7.6 Gates

Unchanged: **Stage C and Stage D remain NOT OPEN.** This pass fixed two real defects and removed noise; it established nothing about behaviour on a real Android device, and §5's residual risks (debug-only release signing, per-machine certificate, no CI evidence yet) still stand.

### 7.7 Behaviour-change note

A-6 changes runtime behaviour on API 26-32: reminders are no longer suppressed there. No automated test covers that branch — Robolectric runs one SDK level — so it is **a reasoned fix awaiting device confirmation**, not a demonstrated one. It is the only change in this pass that could alter what a user experiences.
