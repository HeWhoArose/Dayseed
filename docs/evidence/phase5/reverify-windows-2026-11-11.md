# Phase 5 closeout — independent re-verification on Windows

**Date/time:** 2026-11-11, 06:09–06:31 local (WCAST, UTC+1).
**Repository:** workspace of `HeWhoArose/Dayseed`, branch `main`.
**Inspected/verified commit:** `b30efbc7f830b57baae96d32bc64d30a549b8806` (`b30efbc`) — identical to the commit recorded as remote `main` HEAD in `docs/40-PHASE-5-CLOSEOUT-REPORT.md`. (`git fetch origin main` could not run from this shell because no GitHub credentials are configured there, so the remote head was **not** re-confirmed in this sweep; the local HEAD equals the SHA recorded in the committed evidence.)
**Purpose:** repeat the Phase 5 closeout sweep on a different operating system and a freshly generated debug identity, to test the claim that the recorded results are reproducible rather than machine-specific.

## Environment

| Item | Value |
| --- | --- |
| OS / shell | Windows 11 (`MINGW64_NT-10.0-26200`), Git Bash |
| JDK | Eclipse Temurin `21.0.13+9-LTS` (`C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot`) |
| Gradle | wrapper `9.3.1` (`gradle/wrapper/gradle-wrapper.properties`, `gradle-9.3.1-bin.zip`) |
| Android SDK | `C:\Users\AHMAD\AppData\Local\Android\Sdk`; platform `android-36`; build-tools `36.0.0` (also present: 34.0.0, 28.0.3, unused) |
| Debug keystore | generated in this worktree by `scripts/bootstrap-debug-keystore.sh` |
| Result aggregation | `python3` over `app/build/test-results/testDebugUnitTest/*.xml` and `app/build/reports/lint-results-debug.xml` |

## Command

```
./gradlew clean :app:testDebugUnitTest --rerun-tasks :app:assembleDebug :app:assembleRelease :app:lintDebug --console=plain
```

Exit status **0** — `BUILD SUCCESSFUL in 18m 15s`, 106 tasks (all executed).

## Results

| Check | Result |
| --- | --- |
| Unit/Robolectric tests | **217/217 PASSED**, 14 classes, 0 failures, 0 errors, 0 skipped (`Phase4ImplementationTest` included) |
| Lint (`lintDebug`) | **0 errors, 57 warnings** — `UseKtx` 19, `IconLauncherShape` 10, `GradleDependency` 9, `IconDuplicates` 6, `InlinedApi` 3, `NewerVersionAvailable` 2, `MonochromeLauncherIcon` 2, `IconLocation` 2, `DefaultLocale` 1, `AndroidGradlePluginVersion` 1, `ObsoleteSdkInt` 1, `UnusedResources` 1 |
| `app-debug.apk` | 19,126,660 bytes |
| `app-release.apk` | 12,671,118 bytes |
| Release signer | `CN=Android Debug, O=Android, C=US` — **installable debug-signed artifact, not a production release** |
| Keystore certificate SHA-256 (this machine) | `C4:DC:DA:BA:0E:28:46:FC:A3:92:7A:69:2E:A5:EE:6B:C9:41:A0:87:2E:A0:1F:91:2D:89:25:31:89:8F:24:3F` |

Both APK byte sizes are **identical** to the values recorded at `e68b74b`/`56942fb` in `docs/40` §3, on a different OS and with a different (per-machine) debug certificate — the sizes are reproducible, the signature is not (documented limitation).

## Deviations observed while reproducing

1. **First run failed** at `:app:validateSigningDebug` — `Keystore file '...\debug.keystore' not found for signing config 'debug'`. Expected: the keystore is gitignored and must be bootstrapped. Running `bash scripts/bootstrap-debug-keystore.sh` created it (`alias androiddebugkey`, debug-only identity) and the build proceeded.
2. **Second run failed** at `:app:compileDebugJavaWithJavac` — `error: invalid source release: 21`. Cause: `JAVA_HOME` pointed at `jdk-17.0.20.101-hotspot` while the `java` on `PATH` was Temurin 21; Gradle uses `JAVA_HOME`. Re-running with `JAVA_HOME` set to the JDK 21 installation succeeded. `README.md` now states this explicitly.
3. Build time on this machine was 18m 15s against 2m 40s–5m 23s previously recorded on the Linux container — a cold-cache Windows run, not a regression.

## What this establishes

- The committed wrapper, bootstrap script, tests, lint and both APK builds work from a clean worktree on a second, independent environment.
- The `217/217`, `0 lint errors` and APK-size claims in `docs/40` §3 reproduce exactly.

## What this does **not** establish

- No device or emulator was operated; all 70 cases in `docs/38` remain `NOT RUN`.
- No notification was delivered, no SAF picker opened, no accessibility traversal or contrast measurement performed.
- This is Stage B self-verification evidence, **not** Stage C independent technical validation, and it does not open Stage C or Stage D.
