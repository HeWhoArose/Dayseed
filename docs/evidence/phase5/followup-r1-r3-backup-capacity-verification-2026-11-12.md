# Owner follow-up workstreams — R1 reconciliation, R3 async mutation migration, backup-capacity validation, final verification

**Date/time:** 2026-11-12 (afternoon–evening, local UTC+1 / W. Central Africa Standard Time); all
runs executed in one session at a single final HEAD (see §1).
**Repository:** `HeWhoArose/Dayseed`, branch `main`, local working tree at `C:\Dev\Dayseed`.
**Base:** `origin/main` = `ddfef76` (F-01…F-06 remediation closeout commit; CI run
43094155264 success, verified earlier in this session).
**Commits created and pushed in this session (2026-11-12; remote `main` = `2ba23ef`, fetch-verified):**

| Commit | Subject | Scope |
| --- | --- | --- |
| `83037df` | docs(R1-follow-up): reconcile current-status sections with remediation closeout | README, `docs/12`, `docs/32` |
| `a61db75` | feat(R3-follow-up): move remaining ViewModel mutations off the UI thread | `DayseedViewModel.kt`, `CreateEditHabitScreen.kt`, build files, 4 test files + 2 new test files |
| `3f4f45c` | test(R2-follow-up): validate the 8 MiB backup import limit against synthetic long-term datasets | new `BackupCapacityValidationTest.kt` (6 tests) |

**Inspection evidence:** every value below was read back from executed command output or from
`app/build` outputs produced by the recorded runs; harness-local retained logs are listed in §8.

## 1. Final verification at one HEAD

**Command (required form):**

```
gradle.bat --no-daemon -Dorg.gradle.java.home=C:\Users\AHMAD\AppData\Local\Temp\opencode\jdk21\jdk-21.0.13+9 clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:assembleRelease --console=plain
```

(deviation note: `gradlew.bat` was invoked through the installed Gradle 9.3.1 distribution binary
that the wrapper resolves to, with the JDK-21 override, matching the documented R7 method).

**Run history at this HEAD (three attempts, all against the identical tree at `3f4f45c` — no
source file changed between runs):**

1. **Run 1 (no `--no-daemon` flag):** the harness cut the run after a 60-minute ceiling; it was
   interrupted inside `:app:mergeExtDexRelease`. Before the cut the following had completed
   successfully (read back from the retained log and `app/build/test-results` XML): the full
   unit-test suite (**256/256 PASSED, 0 failures/errors, 18 classes**) and `:app:lintDebug`
   (HTML report written), `:app:dexBuilderDebug`. No APK artefacts were produced. No completed
   build to record; the run is retained for the anomaly note below.
2. **Run 2 (`--no-daemon`):** aborted in the harness pipeline layer during
   `:app:compileDebugUnitTestKotlin` (output-capture pipeline stop terminated the Gradle client);
   a harness-capture defect, not a build failure. No evidence recorded from it.
3. **Run 3 (`--no-daemon`, identical task list) — the recorded verification run:**

```
BUILD SUCCESSFUL in 12m 35s
106 actionable tasks: 105 executed, 1 up-to-date
```

Exit code 0. Full log retained (§8). Anomaly note: run 1 exceeded 60 minutes at the same tasks
that run 3 completed in 12m35s; the only flag difference is `--no-daemon` (run 3 forks a
single-use daemon honouring the documented JVM settings). Run 3 is the verification evidence;
run 1 is recorded because its completed test/lint stages corroborate the same results.

## 2. R1 — documentation reconciliation (commit `83037df`)

- Reconciled the "current status" claims in `README.md`, `docs/12-PROGRESS-AND-HANDOVER.md` and
  `docs/32-CONTINUATION-HANDOVER.md` with the true closeout state from the R7 verification
  (`f0e58b2` + `3b8016f`): stale "pending push" wording replaced, test-count statements aligned,
  CI run reference added.
- Link checker (`scripts/check-doc-links.sh`): **`checked 318 relative markdown link targets; 0 broken`**.
  The 316 → 318 delta versus the R7 run is documents gaining links (this session's edits and the
  two new evidence links), not broken or removed targets.

## 3. R3 — ViewModel mutations moved off the UI thread (commit `a61db75`)

### 3.1 Inventory and design

- Full inventory first: all 23 `runBlocking` uses in `DayseedViewModel.kt` and
  `DayseedRepository.kt` catalogued; every repository mutation already has a `suspend` variant;
  43 test call sites mapped.
- Converted every UI-reachable `runBlocking` mutation to
  `viewModelScope.launch { withContext(ioDispatcher) { … } }` calling the existing repository
  suspend variants. `ioDispatcher: CoroutineDispatcher = Dispatchers.IO` is an injectable third
  constructor parameter.
- `logHabit` / `logHabitQuantityOrDuration` resolve their 350 ms rapid-tap idempotency key
  **before** the first suspension point, preserving rapid-tap semantics.
- `saveHabit` split into a fire-and-forget UI wrapper (with a `_isSaveInFlight` guard that drops
  duplicate submissions) + `suspend fun saveHabitAsync(…): HabitVersion?` for return-value needs;
  `CreateEditHabitScreen.kt` call site simplified.
- `acknowledgeDormantRun` also migrated. `refreshAllSync` intentionally remains `runBlocking`
  (test/setup-only, no Compose call site — verified by inventory).

### 3.2 Test infrastructure (deterministic, no sleeps)

- New `MainDispatcherRule` pins `Dispatchers.Main` to an `UnconfinedTestDispatcher`: launch bodies
  run inline until the first real suspension (the repository's internal
  `withContext(Dispatchers.IO)` hop); post-hop tails resume on the IO worker, never on a paused
  main queue — eliminating Main-queue races and deadlocks.
- New `Flow<T>.awaitMatch(predicate)` helper: event-driven completion wait
  (`withTimeout(10_000) { first { predicate } }`), no sleeps, no polling.
- New `DayseedAsyncMutationThreadingTest` (10 tests): non-blocking return, off-main-thread
  execution, duplicate-drop guard, cancellation, journal failure preservation.
- `Phase2ImplementationTest`, `Phase3ImplementationTest`, `DayseedComposeUITest` retrofitted:
  ViewModels constructed with `ioDispatcher = Dispatchers.Unconfined` (test-only; the
  repository's internal `Dispatchers.IO` still exercises real background threading) and one
  event-driven await per mutation site.

### 3.3 Negative control — executed

With a temporary backup copy of `DayseedViewModel.kt` in which the blocking paths were restored
(`runBlocking` + `withContext(EmptyCoroutineContext)` inside `logHabit`; the save in-flight guard
disabled), the threading suite produced **exactly the 3 predicted failures** (10 completed, 3
failed):

| Test | Failure message |
| --- | --- |
| `logHabitReturnsImmediatelyAndRunsLater` | "mutation must return without waiting for the DB write (no completion message yet) expected null, but was:<…Habit h_async_log: Completed>" — i.e. the blocking path completed synchronously on the calling thread |
| `logHabitMutationBodyExecutesOffTheMainThread` | "mutation must dispatch at least once" — i.e. no background dispatch happened |
| `saveHabitWrapperDropsDuplicateSubmissionsWhileFirstIsInFlight` | "duplicate submission must not enqueue a second save expected:<1> but was:<2>" — i.e. the disabled guard let a duplicate through |

The R3 implementation was then restored from the hash-verified backup
(`4715EB62E3BA6D9152D6D84B15EA85AE35297136BB1B74DE7B89371A5892B004`; no
`NEGATIVE CONTROL`/`EmptyCoroutineContext` leftovers), and the suite was re-run: **green again**
(`BUILD SUCCESSFUL in 1m 27s`).

### 3.4 Suite results after R3 + R2-test additions

Aggregated from `app/build/test-results/testDebugUnitTest/*.xml` (run 1 of §1, at HEAD `3f4f45c`):
**18 classes, 256 tests, 0 failures, 0 errors, 0 skipped.**

| Class | Tests | Class | Tests |
| --- | --- | --- | --- |
| BackupCapacityValidationTest (new, §4) | 6 | DayseedReminderSemanticsTest | 16 |
| DayseedAcceptanceTest | 12 | DayseedSafFilenameFeedbackTest | 7 |
| DayseedAppearanceSettingsTest | 5 | DayseedSettingsValidationTest | 11 |
| DayseedAsyncMutationThreadingTest (new, R3) | 10 | DayseedVisualResolutionTest | 15 |
| DayseedComposeUITest | 6 | Phase1ImplementationTest | 29 |
| DayseedReminderAlarmLifecycleTest | 16 | Phase2ImplementationTest | 31 |
| DayseedReminderAndBackupTest | 20 | Phase3ImplementationTest | 14 |
| DayseedReminderConcurrencyTest | 11 | Phase4ImplementationTest | 36 |
| DayseedReminderDebugProbeTest | 5 | **Total** | **256** |

## 4. Backup-capacity validation of the 8 MiB import limit (commit `3f4f45c`)

**Question (R2 work item recorded in the `BackupLimits.MAX_BACKUP_SIZE_BYTES` comment):** is
`8 MiB = 8,388,608 bytes` (enforced by `readBackupFileBounded` on raw file bytes, pre-decryption)
large enough for realistic long-term user datasets, or must adoption be revisited?

**Method (deterministic, production serializer):** four synthetic datasets were inserted directly
into the production SQLite schema via `ContentValues` bulk inserts inside one transaction
(bypassing per-row repository APIs, whose per-insert `refreshAllSync` would make a 73k-row dataset
O(n²)); one `refreshAllSync()` then loaded every StateFlow so `exportPortableBackupSync()` — the
same serializer used for real device exports — weighed exactly what a device export would weigh.
All content deterministic (fixed clock, fixed IDs, ~6/7 days covered, ~300-char journal bodies,
~110-char note bodies). **This bulk-insert bypass is measurement instrumentation only; the
exported payload and the restore path are production code.** Restore round trips used the
production `restorePortableBackupSync(…, "REPLACE")` path on the same repository instance, with
all-table count equality plus first/last entry spot checks as the integrity assertion.

**Measured results (UTF-8 bytes; limit 8,388,608):**

| Dataset | Contents | Rows (entries/outcomes) | Export size | vs limit | Restore |
| --- | --- | --- | --- | --- | --- |
| small | 5 habits × 30 days, 5 journal | 78 entries + 52 outcomes | **57,354 B (≈56 KiB)** | 0.7% | COMPLETED, counts equal |
| typical | 10 habits × 365 days, 100 journal, 110 notes | 1,565 + 1,565 | **1,228,842 B (1.17 MiB)** | 15% | COMPLETED, counts equal |
| typical, password-encrypted | same as typical | same | **1,664,282 B (1.59 MiB)** | 20% | COMPLETED with password, counts equal |
| heavy | 10 habits × ~3 years, 500 journal, 160 notes | 4,695 + 4,695 | **3,733,342 B (3.56 MiB)** | 44% | COMPLETED, counts equal |
| stress | 20 habits × ~10 years, 1,000 journal, 540 notes | 31,290 + 31,290 | **23,131,692 B (22.06 MiB)** | **276% — OVER_LIMIT** | export-measurement only (see below) |

All values are the run 3 `CAPRESULT` lines read back from the JUnit XML of the final run. The
encrypted envelope size is structurally stable (±204 bytes across two runs, from the random
salt/IV and envelope framing — 0.01%); classification is unaffected.

**Limit boundary behaviour (enforcement point pinned):** `readBackupFileBounded` accepts a file of
limit−1 bytes and of exactly limit bytes, and rejects limit+1 bytes with
`BackupRejectedException` ("Backup file size 8388609 bytes exceeds limit of 8388608 bytes").

**Stress dataset restore scope note:** the stress dataset was export-measured only; the restore
code path is identical for every dataset and was exercised end-to-end at the heavy scale
(22.9 s, COMPLETED). A full REPLACE round trip at stress scale is memory-bound in the JVM test
sandbox (the restore path parses and re-normalises the full payload in memory), so it was not
executed; this is an explicit scope limit of the evidence, not a product defect.

**Classification for the capacity report:**
- **Sufficient** for fresh installs (0.7%), one year of typical 10-habit use (15%), and three
  years of heavy 10-habit use (44%) — with end-to-end restore round trips proven at those scales,
  including the password-encrypted envelope (×1.354 size overhead measured).
- **Limited for extreme-scale datasets:** a 20-habit daily user with ~10 years of history exceeds
  the cap by 2.76×. Linear interpolation of the measured points puts the ~8 MiB threshold at
  roughly ~22k day-rows ≈ **~6 years at 10 habits / ~3 years at 20 daily habits**.
- **Failure mode for over-limit users (user-visible):** the limit is enforced at file read — the
  import entry point — so an over-limit backup file is rejected **before** any processing. (The
  restore path itself additionally re-reads its pre-restore safety backup through the same
  bounded read, so a device whose *current* data already exceeds the cap cannot complete a
  restore either.)
- **No limit change was made.** Raising `BackupLimits.MAX_BACKUP_SIZE_BYTES` (or adding
  compression) is a product decision for the owner; this task only produces the measured
  evidence.

**Performance observations (informational, Robolectric/JVM on a 4-core machine — not on-device
measurements):** bulk insert + one `refreshAllSync` took ~16 s at 10k rows and ~167 s at 62k rows;
export itself was 0.1–1.2 s; a heavy-scale restore round trip took ~23 s. These are consistent
with O(n) per-refresh recomputation and are recorded as observations only (any optimisation is a
separate decision).

## 5. Lint (`:app:lintDebug`)

**0 errors, 11 warnings** — lint report title: "Lint Report: 11 warnings". All 11 are
dependency-version advisories in `gradle/libs.versions.toml`; **0 in product code**. Versus the
R7 baseline (0 errors, 10 warnings), the one new warning is
`NewerVersionAvailable` for `org.jetbrains.kotlinx:kotlinx-coroutines-test` (1.10.2 → 1.11.0),
introduced by the R3 test-infrastructure dependency. No action taken (test-only advisory;
upgrading is a routine decision for a future maintenance pass).

## 6. APK artefacts and signing identity

| Artefact | Size (bytes) | Signed by | Verified |
| --- | --- | --- | --- |
| `app/build/outputs/apk/debug/app-debug.apk` | 18,700,618 | debug keystore | — |
| `app/build/outputs/apk/release/app-release.apk` | 12,293,402 | `CN=Android Debug, O=Android, C=US` | `apksigner verify --print-certs` OK |

Release signer certificate SHA-256 (unchanged from the R7 closeout — this machine's bootstrap
keystore): `2458301c2d78968d8acfb53ba848882b8356885e8b376e4e6c1b843c41faf819`. This is a
**debug-signed release artifact** (installable for device validation, not a production release
signature), as recorded in `docs/40` §3.

Debug APK changed 18,684,234 → 18,700,618 (+16,384 bytes), consistent with the R3 ViewModel /
CreateEditHabitScreen source changes; release size is byte-identical to the R7 closeout
(12,293,402) despite the same source changes (R8-minified output; equality is coincidental
determinism, not proof of content identity).

## 7. CI status

The follow-up chain (`83037df`, `a61db75`, `3f4f45c`, plus the evidence/docs commit `2ba23ef`)
was pushed to `origin/main` on 2026-11-12 and fetch-verified (remote `main` = `2ba23ef`).
GitHub Actions run **43286286411** (run number 4) for `2ba23ef` **completed successfully**
(`conclusion=success`, updated 2026-11-12T19:09:37Z) — the second executed run of
`.github/workflows/android.yml` (run 3 = 43094155264 for `ddfef76`, also success).

## 8. Retained evidence artefacts (harness-local)

- `C:\Users\AHMAD\AppData\Local\Temp\opencode\r3-negative-control-run.txt` — threading suite
  against restored blocking paths (10 completed, 3 failed)
- `C:\Users\AHMAD\AppData\Local\Temp\opencode\r3-negative-control-failures.txt` — the 3 failure
  messages
- `C:\Users\AHMAD\AppData\Local\Temp\opencode\r3-restored-verify-run.txt` — post-restore green
  re-run
- `C:\Users\AHMAD\AppData\Local\Temp\opencode\final-verification-2026-11-12.txt` — run 1 log
  (killed at harness timeout; tests/lint completed before the cut)
- `C:\Users\AHMAD\AppData\Local\Temp\opencode\final-verification-2026-11-12-run2.txt` — run 2 log
  (aborted in harness capture layer)
- `C:\Users\AHMAD\AppData\Local\Temp\opencode\final-verification-2026-11-12-run3.txt` — run 3 log
  (the recorded verification run; `BUILD SUCCESSFUL in 12m 35s`, exit 0)
- `app/build/test-results/testDebugUnitTest/*.xml` — per-class JUnit XML from the final run

## 9. What this establishes / does not establish

**Establishes:** R1 documentation now matches the closeout state (0 broken links); all UI-reachable
ViewModel mutations run off the UI thread with deterministic evidence including a negative control;
the 8 MiB backup limit is measured against four realistic long-term datasets with boundary
behaviour pinned and restore round trips proven below the limit; the full required build command
executes at a single HEAD with 256/256 tests passing (pending run 2 completion for the assemble
steps).

**Does not establish:** no device or emulator was operated; all 70 human/device cases in
`docs/38` remain **NOT RUN**; no reminder delivery, SAF picker, accessibility traversal, contrast
measurement or cross-device backup/restore was exercised; the stress-scale restore round trip was
not executed (scope note in §4); no decision was made here to change the 8 MiB limit (that is an
owner decision); this is Stage B self-verification evidence — it does not open Stage C or Stage D.
