# Freebuff handoff continuation — currency, precision, welcome screen (2026-11-15)

**Status:** branch work complete; owner acceptance, merge and Stage C/D gates remain **NOT OPEN**.
**Branch:** `fix/freebuff-handoff-ux-verification`, forked from `main` at `1ce65ea1acd508de82d526edceba26860538ceca`.

## 1. Repository baseline

| Item | Value |
| --- | --- |
| Remote `main` HEAD at handover | `1ce65ea1acd508de82d526edceba26860538ceca` |
| Previous AI Studio implementation commit | `386f45a4422eeebdf1658e343c1b5cd00de2bc6a` |
| Local workspace HEAD before the pull | `2c543418850d59257a4ef516b1dabea2de2f6c15` (ancestor of remote `main`; fast-forward, 0 commits ahead) |
| Files changed between the two handoff commits | 13 (10 Kotlin sources/tests, 2 evidence PNGs) |
| Unpushed local work found | none; untracked leftovers from a previously reverted investigation were stashed, not discarded |
| Toolchain actually used | Temurin JDK 21.0.13, Gradle wrapper 9.3.1, Android platform 36 + build-tools 36 |

The environment's system `JAVA_HOME` points at JDK 17, which the project's `sourceCompatibility = VERSION_21` rejects (`error: invalid source release: 21`). Every command below was run with `JAVA_HOME` overridden to the JDK 21 installation for that command only; no system setting was changed.

## 2. What AI Studio had already completed (verified, preserved)

Confirmed present in `1ce65ea` and kept unchanged in intent:

- Fixed-point storage (`unit_code` carrying `@scale2` / `@scale3`) with no database schema migration.
- Scale-aware summaries in `Models.kt`, `HabitCard.kt`, `HabitDetailScreen.kt`, `DayDetailScreen.kt`, `DayseedRepository.kt`.
- Empty-by-default new progress entry in `RecordSessionSheet.kt`, decimal entry, validation and clearer progress summaries.
- Collapsed "Adjust interpretation" controls and target parsing in `CreateEditHabitScreen.kt`.
- Quiet-hours and time helpers in `SettingsSupport.kt`.
- Updated `Phase2ImplementationTest.kt` and `ProgressiveDisclosureAndSmartHabitTest.kt`.

No AI Studio commit was reverted, reset or replaced, and the `main` branch was never force-pushed.

## 3. CI executable-permission correction

- `git ls-tree 1ce65ea gradlew` → `100644`; that is why GitHub Actions run `43715833932` failed with `Permission denied` before Gradle started.
- Corrected with `git update-index --chmod=+x gradlew`; the index now records `100755` with the **same blob** `adff685a0348c64b2b64f0b302f1f82b28eeaaea`, so the wrapper script and its distribution configuration are untouched.

## 4. Currency mapping corrections

`HabitNaturalLanguageParser.CURRENCY_SPECS` previously collapsed KES, UGX and TZS into one `shillings` / `KSh` spec, and `UnitFormatter.CURRENCY_SYMBOLS_BY_UNIT` had no entry for `kes`, `ugx`, `tzs`, `rand` or `shillings` at all.

Now distinct:

| Currency | Canonical code | Symbol | Example aliases |
| --- | --- | --- | --- |
| Kenyan shilling | `kes` | `KSh` | `kes`, `ksh`, `kenyan shillings` |
| Ugandan shilling | `ugx` | `USh` | `ugx`, `ush`, `ugandan shillings` |
| Tanzanian shilling | `tzs` | `TSh` | `tzs`, `tsh`, `tanzanian shillings` |
| South African rand | `rand` | `R` | `rand`, `zar`, `south african rand` |
| Nigerian naira | `naira` | `₦` | unchanged |

A bare "shillings" phrase is genuinely ambiguous, so it now raises `needsUserConfirmation` with an explicit reason instead of silently picking Kenya. `naira`, `dollar`, `euro`, `pound`, `yen`, `rupee` and `cedi` behaviour is unchanged.

## 5. Overflow and silent clamping

- `UnitFormatter.parseTargetInputToStoredUnitsAndUnit` (the Create/Edit Habit save path) now **returns `null`** for a target above the storable range instead of silently shrinking it to a different goal; duration, whole-count and scaled branches are all covered.
- `UnitFormatter.describeTargetInputProblem` supplies the specific message ("That target is larger than Dayseed can store. Enter a value up to 21,474,836.47.") so the editor no longer reports an oversized goal as an invalid sign.
- The natural-language parser cannot reject a phrase outright without discarding the user's measurement, so an oversized phrase is kept but explicitly **flagged** (`needsUserConfirmation = true`) with a reason naming the stored maximum. Nothing is clamped silently.
- `UnitFormatter.maxSupportedWholeValue(scale)` centralises the limit (scale 2 → 21,474,836.47; scale 0 → 2,147,483,647).

## 6. Quick-increment improvements

`target / 4` on a scale-2 integer produced the reported `+0.37 litres` for a 1.5 L target. Both surfaces that offered quick increments (`HabitCard` and `RecordSessionSheet`) now use `UnitFormatter.quickIncrementStoredUnits`, which picks scale-exact "nice" steps:

- 1.5 litres (stored 150) → `+0.25 litres` / `+0.5 litres`.
- 10 pages → `+2 pages` / `+5 pages`.
- ₦1,250.50 (stored 125050) → `+₦200.00` / half-step.
- A target too small for a meaningful step (for example 3 pages) hides the increment chips instead of showing zero.

Values stay integral stored units throughout; no floating-point accumulation was introduced.

## 7. First-use / empty Today screen

`FirstUseWelcomeCard` sat in a `LazyColumn` item at the top of an otherwise empty screen. It is now composed inside `FirstUseScene`, a full-height, scroll-friendly scene:

- Identity block at the top (date, time-aware greeting, the single canonical motto).
- A larger reused `DaypartArtwork` illustration claims the middle of the screen.
- The card anchors the bottom with "Your garden starts here.", the supporting line "Choose one small thing you'd like to nurture in your everyday life.", a primary **Plant Your First Habit** action, a quieter **Explore Dayseed**, the compact **Set Up Dayseed** checklist toggle (week start, notifications, backup restore) and "Private by design · Offline · Yours".
- The motto and greeting are not duplicated inside the card.
- Layout uses `heightIn(min = viewportHeight)` inside a vertical scroll, so short screens scroll instead of clipping.
- Progressive disclosure is unchanged: no Daily Reflection, Collected Moments, zero statistics or empty lists before the first habit, and the FAB stays hidden.
- All existing test tags and behaviours are preserved (`onboarding_welcome_card`, `today_empty_habits_card`, `onboarding_create_first_habit_btn`, `onboarding_dismiss_btn`, `onboarding_setup_toggle_btn`, `onboarding_setup_checklist`, `dayseed_cold_launch_scene`).

Discovered while inspecting the rebuilt screenshot: the `+0.37 litres` chips in the handoff screenshot came from `RecordSessionSheet`, not `HabitCard`. Both are corrected.

## 8. Executed verification (this workspace, 2026-11-15)

| Check | Command | Result |
| --- | --- | --- |
| Kotlin compile | `./gradlew --no-daemon :app:compileDebugKotlin` | **BUILD SUCCESSFUL** in 4m 48s |
| Unit + Robolectric tests | `./gradlew --no-daemon :app:testDebugUnitTest` | **294 executed, 291 passed, 3 failed, 0 errors, 0 skipped** |
| Lint | `./gradlew --no-daemon :app:lintDebug` | **0 errors, 21 warnings** |
| Debug APK | `./gradlew --no-daemon :app:assembleDebug` | **SUCCESS**, `app-debug.apk` 19,344,513 bytes |
| Release APK | `./gradlew --no-daemon :app:assembleRelease` | **SUCCESS**, `app-release.apk` 12,118,583 bytes — **debug-signed, installation testing only** |
| Doc links | `bash scripts/check-doc-links.sh` | 341 targets checked, **0 broken** |

New regression coverage: `app/src/test/java/com/example/dayseed/FreebuffHandoffRegressionTest.kt` — 14 JVM tests for currency identity, spelled-out currency aliases, naira preservation, formatter symbols, fractional round-trip, whole-item counts, accumulation, storage-limit rejection, the rejection message, parser overflow flagging, friendly increments, currency increments and hidden increments for tiny targets.

### The three failing tests (all pre-existing)

1. `CanonicalUiAndBehaviourReconciliationTest.runtimeComposeUiLaunchesNavigatesTabsAndVerifiesScreensAndControls` — "Legacy EFFORT_AWARE habit editor must render conversion banner".
2. `ProgressiveDisclosureAndSmartHabitTest.createEditHabitScreen_collapsesInterpretationControlsByDefaultAndRevealsOnAdjustInterpretation` — "Summary must show Quantity measurement type".
3. `ProgressiveDisclosureAndSmartHabitTest.recordSessionSheet_startsEmptyForNewEntryValidatesInputsAndSupportsEditAndDelete` — "Save Progress must be enabled when valid positive amount '2' is entered".

Attribution evidence: with every change on this branch stashed, tests 1 and 2 fail identically on the untouched `1ce65ea` tree, so they were introduced during the AI Studio window (which last recorded 276/276 passing) rather than by this branch (`2 tests completed, 2 failed`). They render `CreateEditHabitScreen` directly and do not touch any modified code path. Test 3 is order-dependent: it passed in an isolated run and fails in full-suite runs; `RecordSessionSheet`'s new-entry contract itself still behaves as documented (see evidence `03_record_session_incremental_progress.png`). These were **not** introduced by this branch and were **not** fixed here; they are recorded as outstanding defects rather than reported as passes.

## 9. Updated evidence

Regenerated by the Robolectric suite (automated evidence, **not** physical-device proof):

- `docs/evidence/01_today_first_use_progressive_disclosure.png` — new first-use scene.
- `docs/evidence/08_fractional_measurement_habit_progress.png` — now `+0.25 litres` / `+0.5 litres`.
- `docs/evidence/02`, `03`, `04`, `05`, `06` — refreshed from the same runs.
- Inspected directly: 01, 03, 04, 08.

## 10. Physical-device validation still outstanding

**NOT RUN.** No device or emulator was available. Still required: real IME/keyboard behaviour, TalkBack and 200% text on the new scene, measured contrast in both themes, notification delivery, and the `P*-H*` register cases. Robolectric screenshots are automated evidence only.

## 11. Remaining product decisions

- Whether the generic word "shillings" should default to a region or always prompt (it currently prompts).
- Whether the empty state should keep "Explore Dayseed" permanently visible or only before onboarding completes.
- Owning the three pre-existing failing tests above.
