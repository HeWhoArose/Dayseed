# PowerShell debug-keystore bootstrap — re-execution evidence, 2026-11-11

**Date/time:** 2026-11-11, local (WCAST, UTC+1).
**Repository:** `HeWhoArose/Dayseed`, branch `main`, HEAD `1d882f0`.
**Script under test:** `scripts/bootstrap-debug-keystore.ps1` (current committed version at `269becc`).
**Environment:** Windows 11 (`MINGW64_NT-10.0-26200`), PowerShell 5.1 (`powershell.exe`), `keytool` from Eclipse Temurin JDK 21.0.13+9 on `PATH`.
**Safety:** All three cases ran against a scratch keystore outside the repository (`C:\Users\AHMAD\AppData\Local\Temp\opencode\dayseed-bootstrap-scratch\debug.keystore`). The real repo-local `debug.keystore` (gitignored) was never touched.

## Case 1 — Initial keystore creation

**Setup:** scratch directory with no `debug.keystore` present.

**Command:** `powershell -NoProfile -ExecutionPolicy Bypass -File scripts\bootstrap-debug-keystore.ps1`

**Result:** PASS — exit code 0.

**Output:**
```
bootstrap-debug-keystore: created C:\Users\AHMAD\AppData\Local\Temp\opencode\dayseed-bootstrap-scratch\debug.keystore (alias androiddebugkey, debug-only identity).
bootstrap-debug-keystore: next: .\gradlew.bat :app:assembleDebug
```

**Keystore exists after run:** yes.

## Case 2 — Idempotent second execution

**Setup:** keystore from Case 1 still present.

**Command:** same as Case 1, re-run immediately.

**Result:** PASS — exit code 0, keystore untouched (LastWriteTime unchanged).

**Output:**
```
bootstrap-debug-keystore: C:\Users\AHMAD\AppData\Local\Temp\opencode\dayseed-bootstrap-scratch\debug.keystore already exists - refusing to overwrite.
bootstrap-debug-keystore: existing keystore verified; nothing to do.
```

**Keystore unchanged:** yes.

## Case 3 — Mismatched-keystore handling

**Setup:** `debug.keystore` replaced with a keystore generated under a different storepass (`wrongpass`) and a different alias (`wrongalias`, not `androiddebugkey`).

**Command:** same as Case 1.

**Result:** PASS (failure is the correct behaviour) — exit code 1.

**Output:**
```
bootstrap-debug-keystore: C:\Users\AHMAD\AppData\Local\Temp\opencode\dayseed-bootstrap-scratch\debug.keystore already exists - refusing to overwrite.
Fail : bootstrap-debug-keystore: FAILED: the existing keystore does not contain the expected 'androiddebugkey' entry (keytool exit 1).
(keytool exit 1). Move it aside manually, then retry.
```

**Application data modified:** no — script refused to touch anything and exited before any write.

## Verification of tracking

`git ls-files docs/evidence/phase5/` includes this file:
```
docs/evidence/phase5/e68b74b.md
docs/evidence/phase5/powershell-bootstrap-2026-11-11.md
docs/evidence/phase5/reverify-windows-2026-11-11.md
```

## Relationship to `docs/40` §5 and `docs/41` §7.1

Both documents reference this evidence file as documenting PowerShell bootstrap execution on 2026-11-11. Prior to this re-execution the file was not tracked in git, so those references pointed at absent evidence. With this file now committed:

- `docs/40` §5 ("executed and verified on Windows on 2026-11-11 — creation, idempotency and mismatched-keystore paths all behaved as intended") is **supported** by the three cases above.
- `docs/41` §7.1 (A-7 fix, stderr handling via `Invoke-Keytool`, three paths verified) is **supported**: all three paths (creation, idempotent re-run, mismatched rejection) were exercised and behaved as intended.

PowerShell 7 (`pwsh`) remains unexercised, consistent with the pre-existing limitation recorded in `docs/40` §5 and `docs/41` §5.

## What this does not establish

- No APK assembly was performed here; keystore creation is a prerequisite for `:app:assembleDebug`/`:app:assembleRelease`, covered by separate build evidence.
- No device or emulator was operated.
