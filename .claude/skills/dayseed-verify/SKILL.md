---
name: dayseed-verify
description: Run Dayseed's build, unit-test and lint checks and report honest, evidence-backed results (PASSED / FAILED / BLOCKED / NOT RUN). Use before claiming a change works, before a handover, or when the user asks to verify, regression-test or check the current change.
argument-hint: "[what changed / area to verify]"
---

# Dayseed verification

Verify the current change for: $ARGUMENTS

Follows AGENTS.md "Test-result integrity". Never report a result you did not execute and inspect.

## 1. Record the starting point
Run and keep the output of: `git rev-parse HEAD`, `git branch --show-current`, `git status --short`, `date -u`, and `java -version` / `./gradlew --version` once the build is about to run.

## 2. Run checks, narrowest first
1. **Targeted:** the test class(es) closest to the change, e.g. `./gradlew :app:testDebugUnitTest --tests "<fully.qualified.TestClass>"`.
2. **Neighbouring:** tests for adjacent behaviour (repository, migration, backup/restore, reminders as relevant).
3. **Full gate (exactly what CI runs, `.github/workflows/android.yml`):**
   ```
   bash scripts/bootstrap-debug-keystore.sh
   ./gradlew --no-daemon --stacktrace :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
   ```
   Add `:app:assembleRelease` only when the change touches release/signing configuration (it is debug-signed until the owner supplies production material).
4. **Docs touched?** also run `bash scripts/check-doc-links.sh`.

Read the real output. Pull counts from `app/build/test-results/testDebugUnitTest/` and lint from `app/build/reports/lint-results-debug.xml`; do not estimate them.

## 3. Report each check with one status
- **PASSED:** executed, exit 0, output inspected.
- **FAILED:** executed and failed. Quote the failing test/lint rule and message.
- **BLOCKED:** could not run (no SDK, no network, wrong JDK). Give the precise reason. A tooling/dependency failure is BLOCKED, not FAILED and not PASSED.
- **NOT RUN:** not attempted. Say why.

For every PASSED/FAILED line record: exact command, UTC date/time, commit SHA (and note if the tree was dirty), JDK/Gradle versions, exit code, and test/lint counts.

## 4. Hard rules
- Device, emulator, accessibility, usability and runtime-privacy checks stay **NOT RUN** unless real evidence exists. The 75 cases in `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md` are not executed by unit tests.
- Stage B self-checks are not Stage C independent validation. Say so if asked to call it "validated".
- Never weaken, skip or delete a test to get green. Never claim CI passed unless you read the run for this exact SHA.
- Do not print or open secret/signing files; the bootstrap script is the only keystore interaction.
