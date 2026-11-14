# Dayseed — Comprehensive Bug Investigation, Reproduction & Remediation Report (2026-11-14)

**Repository:** `HeWhoArose/Dayseed`  
**Canonical branch:** `main`  
**Inspected remote `main` baseline SHA:** `159aadc1e5060a2d46eec88c66fb160dacd565b2` (`2026-11-14T09:37:18Z`, `docs: record additional owner-reported Dayseed defects`)  
**Workspace inspection & remediation date:** `2026-11-14` (`America/Los_Angeles` / `UTC`)  
**Work type:** Controlled Stage B bug investigation, reproduction, code remediation, and regression verification  
**Governance status:** Stage A closed (`D-208`). Stage B open (`D-213`). **Stage C (`Independent Technical Validation`) and Stage D (`Final Owner Acceptance / Release Readiness`) remain `NOT OPEN`.** All 75 physical-device human validation cases in [docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md](38-PHASE-1-HUMAN-VALIDATION-REGISTER.md) remain **`NOT RUN`**.

---

## A. Executive Summary

1. **Baseline & Repository Audit:**
   - Verified remote `origin/main` at commit `159aadc1e5060a2d46eec88c66fb160dacd565b2` (`2026-11-14T09:37:18Z`), which added [docs/43-OWNER-REPORTED-DEFECT-INTAKE-2026-11-14.md](43-OWNER-REPORTED-DEFECT-INTAKE-2026-11-14.md) and removed the deleted `docs/42` report (`222014c93d0bf707fa638201207c2e2c8f778d24`). Per owner instruction, the deleted `docs/42` file has **not** been recreated.
   - Audited all historical defect registers, audit reports, screen/data contracts, and implementation reports ([docs/22-IMPLEMENTATION-REALITY-AUDIT.md](22-IMPLEMENTATION-REALITY-AUDIT.md), [docs/26-SCREEN-AND-DATA-CONTRACTS.md](26-SCREEN-AND-DATA-CONTRACTS.md), [docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md](38-PHASE-1-HUMAN-VALIDATION-REGISTER.md), [docs/39-WEEKLY-REMINDER-SEMANTICS-INVESTIGATION.md](39-WEEKLY-REMINDER-SEMANTICS-INVESTIGATION.md), [docs/40-RESTORE-RECOVERY-INVESTIGATION.md](40-RESTORE-RECOVERY-INVESTIGATION.md), [docs/40-PHASE-5-CLOSEOUT-REPORT.md](40-PHASE-5-CLOSEOUT-REPORT.md), [docs/41-SYSTEM-AUDIT-2026-11-11.md](41-SYSTEM-AUDIT-2026-11-11.md), and [docs/43-OWNER-REPORTED-DEFECT-INTAKE-2026-11-14.md](43-OWNER-REPORTED-DEFECT-INTAKE-2026-11-14.md)) directly against current Kotlin source and JUnit/Robolectric tests.

2. **Four Newly Reported Owner Defects (`ODR-001`–`ODR-004`) Investigated, Reproduced & Remediated:**
   - **`ODR-001` — Ambiguous AM/PM time display and input:**
     - **Root cause:** `CreateEditHabitScreen`, `SettingsPanels.QuietHoursPanel`, `HabitDetailScreen`, and `HabitsScreen` accepted/displayed raw 24-hour `HH:mm` strings (`"04:00"`, `"09:00"`) without an `AM`/`PM` label or 12-hour/24-hour dual preview, and rejected 12-hour input (`"4:00 PM"`). Entering `"04:00"` when intending `4:00 PM` saved `04:00 AM` (`04:00`), which also fell inside default Quiet Hours (`22:00–07:00`).
     - **Fix:** Added `SettingsSupport.parseFlexibleLocalTime`, `formatTime12Hour`, `formatTime24Hour`, `formatUnambiguousTimeDisplay` (e.g., `"4:00 AM (04:00)"` / `"4:00 PM (16:00)"`), `switchTimeAmPm`, `isPmTime`, and `quietHoursUnambiguousSummary`. Updated `CreateEditHabitScreen` with a live resolved-time preview banner (`habit_reminder_resolved_time_preview`), explicit `AM (Morning)` (`habit_reminder_am_chip`) and `PM (Afternoon/Evening)` (`habit_reminder_pm_chip`) `FilterChip`s, `-30m`/`+30m` steppers, quick presets (`8:00 AM`, `12:00 PM`, `4:00 PM`, `8:00 PM`), and flexible 12h/24h parsing while preserving canonical 24-hour `HH:mm` storage in SQLite (`habit_versions.reminder_time_local`). Updated `HabitDetailScreen`, `HabitsScreen`, and `SettingsPanels.QuietHoursPanel` to display unambiguous 12h+24h times.
   - **`ODR-002` — Configured reminder produces no visible/audible notification:**
     - **Root cause:** Four compounding factors:
       1. **Global honesty gate (`D-221` / `P25-H03`):** `reminders_enabled` defaults to `"false"` in SQLite, and `Settings → Gentle Reminders` (`reminders_global_switch`) is intentionally disabled in production builds until physical-device notification verification (`P25-H03`) is executed. However, `CreateEditHabitScreen` allowed enabling per-habit `AT_TIME` reminders without warning the user on the habit form that global reminder delivery is currently gated or if Android 13+ `POST_NOTIFICATIONS` permission is not granted.
       2. **AM/PM Quiet Hours collision:** A time entered as `"04:00"` (intending 4:00 PM) was stored as `04:00 AM`, which falls inside default Quiet Hours (`22:00–07:00`) and is suppressed (`QUIET_HOURS`).
       3. **Missing habit reminder visibility on `HabitDetailScreen` and `HabitsScreen`:** Saved per-habit reminder times were not surfaced on the habit card or detail header.
       4. **Notification metadata gap in `ReminderNotifier.postHabitReminder`:** `postHabitReminder` lacked a `contentIntent` `PendingIntent` to open `MainActivity` when tapped and lacked `NotificationCompat.CATEGORY_REMINDER` (whereas `postWeeklyReminder` had `contentIntent`).
     - **Fix:** Added an explicit Quiet Hours collision warning (`habit_reminder_quiet_hours_warning`) with a one-tap `Switch to <time> PM` button (`habit_reminder_switch_out_of_quiet_hours_btn`) and an honest delivery-status notice (`habit_reminder_status_notice` + Android 13+ `Allow Notifications` button `habit_reminder_request_permission_btn`) inside `CreateEditHabitScreen`; surfaced configured reminder badges on `HabitDetailScreen` (`detail_reminder_badge`) and `HabitsScreen`; and updated `ReminderNotifier.postHabitReminder` and `postWeeklyReminder` with `contentIntent` and `NotificationCompat.CATEGORY_REMINDER` while strictly preserving the `D-221` / `P25-H03` honesty gate.
   - **`ODR-003` — First-run experience on a new phone:**
     - **Root cause:** Tapping `Create Your First Habit` on `TodayScreen` immediately wrote `onboarding_completed = "true"` *before* any habit was saved; if the user backed out of `CreateEditHabitScreen` without saving, the welcome card vanished permanently on an empty database. Furthermore, first-run users on a new phone had no guided checklist for portable backup restore (`allowBackup="false"` under `D-186`), week-start day selection (`Monday` vs `Sunday`), or Android 13+ notification readiness.
     - **Fix:** Removed the premature `onboarding_completed = "true"` write from `onboarding_create_first_habit_btn`; updated `DayseedRepository.saveHabitSync` to persist `onboarding_completed = "true"` inside the SQLite transaction whenever a habit is saved; and added the compact `onboarding_setup_checklist` inside `onboarding_welcome_card` on `TodayScreen` with one-tap `Week: Mon/Sun` toggle (`onboarding_week_start_toggle_btn`), `Alerts: Ready/Off` status (`onboarding_notification_status_text` + `onboarding_request_notifications_btn`), `Create First Habit` (`onboarding_create_first_habit_btn`), `Restore Backup` (`onboarding_restore_backup_btn`), and `Explore First` (`onboarding_dismiss_btn`).
   - **`ODR-004` — Soft keyboard covers active text fields (`P2-H13`):**
     - **Root cause:** Although `AndroidManifest.xml` declared `android:windowSoftInputMode="adjustResize"`, (a) the outer `Scaffold` in `MainActivity.kt` kept the `80.dp` bottom `NavigationBar` visible while the soft keyboard was open, consuming vertical space above the keyboard; (b) `CreateEditHabitScreen`, `RecordSessionSheet`, `JournalEditorDialog`, `HabitDetailScreen`, and `SettingsPanels` lacked `.imePadding()` and focus-driven `BringIntoViewRequester` handling; and (c) multi-line `OutlinedTextField`s did not automatically scroll their container when focused or when the IME finished animating open.
     - **Fix:** Created `Modifier.bringIntoViewOnFocus()` in `KeyboardVisibilitySupport.kt` (combining `BringIntoViewRequester`, `onFocusEvent`, `onSizeChanged`, and `WindowInsets.ime` height-change tracking to keep the active field and its supporting text above the keyboard); hid the outer `80.dp` `NavigationBar` in `MainActivity.kt` whenever `WindowInsets.ime.getBottom(LocalDensity.current) > 0`; and applied `.imePadding()` and `.bringIntoViewOnFocus()` across `CreateEditHabitScreen`, `RecordSessionSheet`, `JournalEditorDialog`, `HabitDetailScreen`, `HabitsScreen`, and `SettingsPanels`.

---

## B. Consolidated Master Defect Inventory

| Defect ID | Source | Affected Feature / Component | Description | Expected Behaviour | Current Status | Evidence (Code / Tests) | Root Cause | Fix Applied |
|---|---|---|---|---|---|---|---|---|
| **ODR-001** | `docs/43` (`ODR-001`), Owner report (2026-11-14) | `CreateEditHabitScreen`, `SettingsPanels.QuietHoursPanel`, `HabitDetailScreen`, `HabitsScreen`, `SettingsSupport` | Reminder time and Quiet Hours inputs/displays showed bare 24h strings (`04:00`, `09:00`) without AM/PM clarity and rejected 12h input (`4:00 PM`). | Display both 12-hour AM/PM and 24-hour `HH:mm` (`4:00 PM (16:00)`), provide explicit `AM`/`PM` selector chips, steppers, and presets, and accept both 12h and 24h input while storing canonical 24h `HH:mm`. | **FIXED (Source & Automated Tests); Device UI check `NOT RUN`** | `SettingsSupport.kt`, `CreateEditHabitScreen.kt`, `SettingsPanels.kt`, `HabitDetailScreen.kt`, `HabitsScreen.kt`; `OwnerReportedDefectsRemediationTest` (`odr001_*`) | UI bound directly to raw 24h string without 12h/24h formatting or AM/PM controls. | Added `parseFlexibleLocalTime`, `formatUnambiguousTimeDisplay`, `switchTimeAmPm`, `AM`/`PM` chips, `-30m`/`+30m` steppers, and presets. |
| **ODR-002** | `docs/43` (`ODR-002`), `docs/38` (`P25-H03`), Owner report (2026-11-14) | `CreateEditHabitScreen`, `ReminderNotifier`, `ReminderScheduler`, `ReminderAlarmReceiver`, `HabitDetailScreen`, `HabitsScreen` | Setting a habit reminder produced no visible or audible alert on the phone. | Warn when a reminder falls inside Quiet Hours or when global reminders / Android notification permission are off; attach `contentIntent` + `CATEGORY_REMINDER` on habit notifications; preserve `D-221` honesty gate until physical-device verification. | **PARTIAL — Source gaps fixed & verified in Robolectric; Production switch stays gated under `D-221` pending physical-device `P25-H03` (`NOT RUN`)** | `CreateEditHabitScreen.kt`, `ReminderNotifier.kt`, `HabitDetailScreen.kt`, `HabitsScreen.kt`; `OwnerReportedDefectsRemediationTest` (`odr002_*`), `DayseedReminderAlarmLifecycleTest` | (1) `D-221` production honesty gate (`reminders_enabled=false`) not explained on habit form; (2) `"04:00"` AM/PM confusion hit Quiet Hours (`22:00–07:00`); (3) `postHabitReminder` lacked `contentIntent` and `CATEGORY_REMINDER`. | Added Quiet Hours conflict warning + 1-tap PM switch, habit-form reminder status notice + permission request button, detail/list reminder badges, and `contentIntent` + `CATEGORY_REMINDER` in `ReminderNotifier`. |
| **ODR-003** | `docs/43` (`ODR-003`), Owner report (2026-11-14) | `TodayScreen`, `DayseedRepository.saveHabitSync` | First launch on a new phone marked `onboarding_completed="true"` prematurely when opening `CreateEditHabitScreen` (even if cancelled) and lacked new-phone setup controls (backup restore, week start, notification permission). | Keep welcome card until a habit is saved or `Explore First` is tapped; provide compact New-Phone Quick Setup (Week start toggle, Notification status/request, Restore Backup shortcut). | **FIXED (Source & Automated Tests); Device check `NOT RUN`** | `TodayScreen.kt`, `DayseedRepository.kt`; `OwnerReportedDefectsRemediationTest` (`odr003_*`) | `onboarding_create_first_habit_btn` wrote `onboarding_completed="true"` before habit creation succeeded; no quick-setup checklist existed on `TodayScreen`. | Removed premature write on button tap; persisted `onboarding_completed="true"` in `saveHabitSync`; added compact 3-row welcome + quick setup checklist on `TodayScreen`. |
| **ODR-004** | `docs/43` (`ODR-004`), `docs/38` (`P2-H13`), Owner report (2026-11-14) | `MainActivity`, `KeyboardVisibilitySupport`, `CreateEditHabitScreen`, `RecordSessionSheet`, `JournalEditorDialog`, `HabitDetailScreen`, `HabitsScreen`, `SettingsPanels` | Soft keyboard covered active text fields during habit creation, session logging, and note/journal editing. | Active text field and supporting text scroll into view above the keyboard; bottom `NavigationBar` hides while IME is open; primary Save/Cancel actions remain reachable by scrolling. | **FIXED (Source & Automated Tests); Physical-device `P2-H13` `NOT RUN`** | `KeyboardVisibilitySupport.kt`, `MainActivity.kt`, `CreateEditHabitScreen.kt`, `RecordSessionSheet.kt`, `JournalEditorDialog.kt`, `HabitDetailScreen.kt`, `HabitsScreen.kt`, `SettingsPanels.kt`; `OwnerReportedDefectsRemediationTest` (`odr004_*`) | Outer `80.dp` `NavigationBar` stayed visible above the keyboard, and scroll containers/fields lacked `.imePadding()` and focus-driven `BringIntoViewRequester`. | Added `Modifier.bringIntoViewOnFocus()`, hid `NavigationBar` when IME height > 0, and added `.imePadding()` across all text-entry screens/sheets/dialogs. |
| **UI-01** | Responsive UI Inspection (`2026-11-13`) | `HistoryScreen.kt` (`Journal` header) | `Journal` heading broke mid-word across two lines (`Journ` / `al`) on narrow viewports when sharing a row with `Date Jump` and `Write`. | `Journal` title stays on one line (`maxLines = 1`, `softWrap = false`) across `320dp`, `360dp`, and `411dp` viewports without colliding with action buttons. | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | `HistoryScreen.kt:238–317`; `CanonicalUiAndBehaviourReconciliationTest` (`responsiveLayoutsVerifyJournalHeaderInsightsCardsAndTodayFabAcrossViewports`) | Single `Row` gave action buttons priority width over `headlineMedium` title. | Stacked title + reflection count badge above a `FlowRow` of action buttons. |
| **UI-02** | Responsive UI Inspection (`2026-11-13`) | `InsightsScreen.kt` (`OverviewStatBox`) | Metric descriptions in the 2-column Insights grid were truncated with ellipses (`Scheduled daily oppor...`). | Full metric descriptions render without ellipsis truncation and paired cards in each row share equal height (`IntrinsicSize.Max`). | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | `InsightsScreen.kt:381–444, 964–1007`; `CanonicalUiAndBehaviourReconciliationTest` | `maxLines = 1` + `TextOverflow.Ellipsis` on 2-column cards. | Removed 1-line ellipsis cap and applied `IntrinsicSize.Max` + `fillMaxHeight()` to paired row cards. |
| **UI-03** | Responsive UI Inspection (`2026-11-13`) | `TodayScreen.kt` (`add_habit_fab` vs header/cards) | Floating Action Button (`+`) overlapped `Today's Seeds` header / `0 of 0 tended` badge or `EmptyHabitsCard` on compact screens. | `add_habit_fab` never overlaps `Today's Seeds` header, tended count badge, `EmptyHabitsCard`, or first `HabitCard`. | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | `TodayScreen.kt:361–525, 587–615, 793–795`; `CanonicalUiAndBehaviourReconciliationTest` | Missing end-padding clearance on `today_seeds_section_header` and tall onboarding banner pushing `EmptyHabitsCard` into FAB zone. | Added `padding(end = 84.dp)` on `today_seeds_section_header`, `96.dp` bottom spacer, and compact 3-row `onboarding_welcome_card`. |
| **UI-04** | Responsive UI Inspection (`2026-11-13`) | `TodayScreen.kt`, `HabitsScreen.kt`, `HistoryScreen.kt`, `InsightsScreen.kt`, `SettingsScreen.kt` | Excess vertical spacing and padding reduced visible content above the fold across the 5 primary screens. | Balanced vertical rhythm with generous touch targets and clean spacing across `320dp`–`411dp` viewports. | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | Primary screen `LazyColumn` / `Column` paddings and spacings; `CanonicalUiAndBehaviourReconciliationTest` | Oversized vertical paddings and card gaps. | Tightened vertical spacing while preserving Material 3 touch targets. |
| **D233-01** | `docs/09` (`D-233`), Owner report (`2026-11-13`) | `DayseedDatabaseHelper.cleanupLegacyV1DemoDataIfNeeded` | Historical `4c1187a` v1 starter habits (`Gentle Movement`, `Quiet Reading`, `Drink Water`) and starter journal entry persisted on upgraded databases; naive deletion risked deleting user-logged activity. | Remove untouched `4c1187a` starter rows automatically while strictly preserving any demo habit that has user activity or dependent records. | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | `DayseedDatabaseHelper.kt:808–1035`; `CanonicalUiAndBehaviourReconciliationTest` (4 dedicated migration/activity-preservation tests) | Legacy v1 `onCreate` seeded 3 starter habits and 1 starter journal entry (`4c1187a`). | Implemented activity-safe idempotent cleanup checking all 9 dependent tables before deleting any matched starter habit. |
| **D233-02** | `docs/09` (`D-233`), Owner report (`2026-11-13`) | `HabitCard.kt`, `RecordSessionSheet.kt`, `CreateEditHabitScreen.kt` | Manual `🌱 Tiny` / `🌿 Usual` / `🌳 Extra` effort buttons appeared on habit logging surfaces instead of deriving progress bands from recorded evidence. | Future habit logging uses `DURATION`, `QUANTITY`, or `COMPLETION_ONLY` evidence to derive `D-040` bands automatically; historical `EFFORT_AWARE` outcomes remain intact and readable. | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | `HabitCard.kt`, `RecordSessionSheet.kt`, `CreateEditHabitScreen.kt`; `CanonicalUiAndBehaviourReconciliationTest` | Legacy `EFFORT_AWARE` UI controls remained active for new habits. | Removed manual Tiny/Usual/Extra logging buttons, restricted new habits to `DURATION`/`QUANTITY`/`COMPLETION_ONLY`, and added a conversion banner for legacy habits. |
| **D233-03** | `docs/09` (`D-233`), `docs/36` | `MainActivity.kt`, `Theme.kt`, `DaypartArtwork.kt`, `SettingsScreen.kt` | Daypart background layer only rendered on `TodayScreen`, `ThemeModeOption.LIGHT` differed from `SYSTEM` light palette, and reduced motion / cold-launch brand scene were missing. | Shared `DaypartBackgroundLayer` across all 5 tabs and secondary routes; unified `LIGHT` and `SYSTEM`-light palettes; `reduced_motion_enabled` toggle in Settings; once-per-cold-launch brand card. | **FIXED (Verified in `CanonicalUiAndBehaviourReconciliationTest`)** | `MainActivity.kt`, `Theme.kt`, `TodayScreen.kt`, `SettingsScreen.kt`; `CanonicalUiAndBehaviourReconciliationTest` | Per-screen opaque backgrounds and split palette definitions. | Hosted `DaypartBackgroundLayer` in `MainActivity.kt`, unified `LightColorScheme`, added `reduced_motion_enabled`, and added `dayseed_cold_launch_scene`. |
| **F-01..F-07 (Audit 2026-11-11)** | `docs/41-SYSTEM-AUDIT-2026-11-11.md` | `ReminderNotifier`, `DayseedDatabaseHelper`, `DayseedViewModel`, `scripts/check-doc-links.sh`, adaptive icons | API 26–32 `POST_NOTIFICATIONS` unguarded check (`F-01`), unbounded backup import memory (`F-02`), main-thread SQLite/crypto calls (`F-03`), doc-link checker bugs (`F-04`), adaptive icon cleanup (`F-05`), PowerShell bootstrap stderr (`F-06`), `p4_35`/`p4_36` strengthening (`F-07`). | Guard `POST_NOTIFICATIONS` by `SDK_INT >= 33`; enforce 8 MiB streaming backup import cap; move ViewModel mutations off main thread; fix doc-link script. | **FIXED (Verified in `DayseedReminderSemanticsTest`, `DayseedAsyncMutationThreadingTest`, `DayseedBackupCapacityValidationTest`)** | `ReminderNotifier.kt:49`, `DayseedRepository.kt`, `DayseedViewModel.kt`, `scripts/check-doc-links.sh` | Static audit findings `F-01`..`F-07` documented in `docs/41`. | All 7 workstreams (`R1`–`R7` + 2026-11-12 follow-up) implemented and verified across 276 JUnit/Robolectric tests. |
| **W-01..W-04 / R-01..R-05 / D-225..D-232** | `docs/34`, `docs/40`, `docs/09` (`D-225`–`D-232`) | Backup/Restore (`exportPortableBackupSync`, `restorePortableBackupSync`), Reminder Engine & Concurrency (`evaluateAndClaimHabitReminderForPostingSync`) | Historical Phase 1–4 restore integrity, foreign-key validation, stale-alarm cancellation, per-habit alarm evaluation, and concurrent receiver claim-token lease coordination. | Transactional fail-closed restore with safety backup; atomic `BEGIN IMMEDIATE` `POSTING_CLAIM:<uuid>` lease so concurrent receivers never double-post and failed posts never consume the 3/day cap. | **FIXED (Verified across `Phase1ImplementationTest`–`Phase4ImplementationTest`, `DayseedReminderConcurrencyTest`)** | `DayseedRepository.kt`, `ReminderAlarmReceiver.kt`, `ReminderScheduler.kt`; 276/276 passing tests | Historical architectural gaps identified in Stage B self-audits. | Reconciled in `main` through `D-225`–`D-232` and `b0fc29d` / `6561588`. |
| **DEV-H01..H75 (`docs/38`)** | `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`, `docs/26` (`D-221`) | Physical Android device runtime validation (notifications, reboot/Doze, TalkBack, 200% font scale, SAF file picker, real soft keyboard `P2-H13`, `P25-H03`) | 75 human/physical-device validation cases require execution on physical Android hardware. Production `reminders_global_switch` remains gated (`enabled = false`) until `P25-H03` passes on a real device. | Keep all 75 cases honestly marked `NOT RUN` and keep `reminders_global_switch` gated until physical-device validation is performed and approved by the owner. | **OPEN / `NOT RUN` (By Governance Rule)** | `docs/38-PHASE-1-HUMAN-VALIDATION-REGISTER.md`, `SettingsScreen.kt:234–245` | Container environment has no physical Android device or ADB hardware attachment. | Preserved `NOT RUN` status on all 75 cases and preserved the `D-221` honesty gate while adding clear in-app status explanations. |

---

## C. Detailed Defect Reports (`ODR-001` through `ODR-004`)

### 1. `ODR-001` — Ambiguous AM/PM Time Display and Input

- **Summary:** When viewing or entering a reminder time (`CreateEditHabitScreen`) or Quiet Hours (`SettingsPanels.QuietHoursPanel`), the UI displayed raw 24-hour values such as `04:00` or `09:00` without `AM`/`PM` labels and rejected 12-hour input (`4:00 PM`). A user entering `04:00` while intending `4:00 PM` inadvertently scheduled `4:00 AM` (`04:00`).
- **Exact Code Locations Inspected:**
  - `app/src/main/java/com/example/dayseed/ui/screens/SettingsSupport.kt` (lines 1–195)
  - `app/src/main/java/com/example/dayseed/ui/screens/CreateEditHabitScreen.kt` (lines 299–315, 960–1270, 1410–1495)
  - `app/src/main/java/com/example/dayseed/ui/screens/SettingsPanels.kt` (lines 310–425)
  - `app/src/main/java/com/example/dayseed/ui/screens/HabitDetailScreen.kt` (lines 360–430, 815–835)
  - `app/src/main/java/com/example/dayseed/ui/screens/HabitsScreen.kt` (lines 420–455)
- **Reproduction Steps:**
  1. Open `CreateEditHabitScreen` and enable `Habit Reminder` (`habit_reminder_switch`).
  2. Enter `04:00` into `habit_reminder_time_input`.
  3. Previously, no `4:00 AM (04:00)` vs `4:00 PM (16:00)` distinction was shown, no `AM`/`PM` selector chips existed, and typing `4:00 PM` failed validation (`parseStrictHhMm` returned `null`).
- **Root Cause:**
  - Storage in SQLite (`habit_versions.reminder_time_local` and `app_settings` `quiet_hours_start` / `quiet_hours_end`) uses canonical 24-hour `HH:mm` (`00:00`–`23:59`), and the UI bound text inputs directly to that raw string without a formatting/parsing layer or explicit `AM`/`PM` period controls.
- **Remediation Implemented:**
  - Added `SettingsSupport.parseFlexibleLocalTime(raw, fallbackPeriodIsPm)` accepting both 24-hour (`"04:00"`, `"16:00"`, `"0:00"`, `"23:59"`) and 12-hour (`"4:00 AM"`, `"4:00 PM"`, `"12:00 am"`, `"12:00 pm"`, `"4:00pm"`) formats while rejecting out-of-range values (`"25:00"`, `"13:00 PM"`, `"00:30 AM"`).
  - Added `SettingsSupport.formatTime12Hour`, `formatTime24Hour`, `formatUnambiguousTimeDisplay` (rendering `"4:00 AM (04:00)"` and `"4:00 PM (16:00)"`), `switchTimeAmPm`, `isPmTime`, and `quietHoursUnambiguousSummary`.
  - Updated `CreateEditHabitScreen.kt` with:
    - Live resolved-time preview banner (`habit_reminder_resolved_time_preview`),
    - Explicit `AM (Morning)` (`habit_reminder_am_chip`) and `PM (Afternoon/Evening)` (`habit_reminder_pm_chip`) `FilterChip`s that convert the current hour between AM (`00:00`–`11:59`) and PM (`12:00`–`23:59`),
    - `-30m` (`habit_reminder_step_minus_30`) and `+30m` (`habit_reminder_step_plus_30`) steppers,
    - Quick preset chips for `8:00 AM (08:00)`, `12:00 PM (12:00)`, `4:00 PM (16:00)`, and `8:00 PM (20:00)`.
  - Updated `SettingsPanels.QuietHoursPanel`, `HabitDetailScreen`, and `HabitsScreen` to display unambiguous 12h+24h labels while preserving canonical `HH:mm` persistence.
- **Automated Verification:**
  - `OwnerReportedDefectsRemediationTest.odr001_boundaryTimesFormatAndParseUnambiguouslyIn12HourAnd24HourModes`: **PASSED** (verifies `00:00`/`12:00 AM`, `00:01`/`12:01 AM`, `04:00`/`4:00 AM`, `12:00`/`12:00 PM`, `16:00`/`4:00 PM`, `23:59`/`11:59 PM`, `AM`↔`PM` switching, and invalid input rejection).
  - `OwnerReportedDefectsRemediationTest.odr001_createEditHabitScreenShowsUnambiguousTimeAndAmPmControlsAndPersistsReload`: **PASSED** (verifies `04:00` displays `4:00 AM (04:00)` + Quiet Hours warning, tapping `PM` chip converts to `16:00`, saving persists `16:00` in SQLite, and reloading displays `4:00 PM (16:00)`).

---

### 2. `ODR-002` — Configured Reminder Produces No Visible or Audible Alert

- **Summary:** After configuring a habit reminder, the owner observed no notification or alert at the expected time.
- **Exact Code Locations Inspected:**
  - `app/src/main/java/com/example/dayseed/ui/screens/CreateEditHabitScreen.kt` (lines 1174–1270)
  - `app/src/main/java/com/example/dayseed/ui/screens/SettingsScreen.kt` (lines 225–255)
  - `app/src/main/java/com/example/dayseed/data/ReminderScheduler.kt` (lines 45–175)
  - `app/src/main/java/com/example/dayseed/data/ReminderAlarmReceiver.kt` (lines 35–165)
  - `app/src/main/java/com/example/dayseed/data/ReminderEngine.kt` (lines 40–185)
  - `app/src/main/java/com/example/dayseed/data/ReminderNotifier.kt` (lines 20–160)
- **End-to-End Trace & Root Causes Identified:**
  1. **Production Honesty Gate (`D-221` / `P25-H03`) Not Surfaced on Habit Form:** Under approved decision `D-221` and `docs/26-SCREEN-AND-DATA-CONTRACTS.md` §7.2, `reminders_enabled` defaults to `"false"` and `reminders_global_switch` in `SettingsScreen.kt` is disabled in production until physical-device notification delivery (`P25-H03`) is verified. However, `CreateEditHabitScreen` allowed turning on `habit_reminder_switch` without informing the user on that screen that global reminder delivery is currently off or that Android 13+ notification permission is not yet granted.
  2. **Quiet Hours Suppression (`22:00–07:00`) Triggered by AM/PM Confusion:** When `"04:00"` was entered intending `4:00 PM`, it saved as `04:00 AM`, which falls inside the default Quiet Hours window (`22:00–07:00`). Even when `reminders_enabled = "true"`, `ReminderEngine` suppresses `04:00` with `suppression_reason = "QUIET_HOURS"`.
  3. **Missing Reminder Badge on `HabitDetailScreen` and `HabitsScreen`:** Users had no visible confirmation on `HabitDetailScreen` or `HabitsScreen` showing what reminder time (`4:00 AM (04:00)` vs `4:00 PM (16:00)`) was saved on a habit.
  4. **`ReminderNotifier.postHabitReminder` Missing `contentIntent` and `CATEGORY_REMINDER`:** Unlike `postWeeklyReminder`, `postHabitReminder` did not attach a `PendingIntent` to launch `MainActivity` when tapped and did not set `NotificationCompat.CATEGORY_REMINDER`.
- **Remediation Implemented:**
  - In `CreateEditHabitScreen.kt`:
    - Added a live Quiet Hours conflict warning card (`habit_reminder_quiet_hours_warning`) whenever the resolved reminder time falls within `quietHoursStart..quietHoursEnd`, including a one-tap button (`habit_reminder_switch_out_of_quiet_hours_btn`) when switching AM→PM moves the time outside Quiet Hours (e.g., `4:00 AM (04:00)` → `4:00 PM (16:00)`).
    - Added an honest reminder delivery status banner (`habit_reminder_status_notice`) explaining whether Android `POST_NOTIFICATIONS` permission is missing (with an inline `Allow Notifications` button `habit_reminder_request_permission_btn` on Android 13+) and whether global `Gentle Reminders` in Settings is currently off (`reminders_enabled == false`), while confirming that the habit's reminder time is saved cleanly.
  - In `HabitDetailScreen.kt` and `HabitsScreen.kt`:
    - Added `detail_reminder_badge` (`"Reminder: 4:00 PM (16:00)"`) in `HabitDetailScreen` Section A and appended the unambiguous reminder time to `habits_meta_<id>` in `HabitsScreen`.
  - In `ReminderNotifier.kt`:
    - Updated `postHabitReminder` to attach a `PendingIntent` launching `MainActivity` (`FLAG_ACTIVITY_NEW_TASK or FLAG_ACTIVITY_CLEAR_TOP`, `FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE`) and set `.setCategory(NotificationCompat.CATEGORY_REMINDER)` on both `postHabitReminder` and `postWeeklyReminder`.
- **Automated Verification:**
  - `OwnerReportedDefectsRemediationTest.odr002_reminderPipelineHonorsGlobalGateQuietHoursAndPostsRealNotificationWhenEnabled`: **PASSED** (verifies 0 alarms armed when `reminders_enabled=false`, 2 alarms armed when `reminders_enabled=true`, `04:00` suppressed with `QUIET_HOURS`, and `16:00` posted as a real notification with `contentIntent != null`, `category == CATEGORY_REMINDER`, and ledger status `DELIVERED`).

---

### 3. `ODR-003` — First-Run Experience on a New Phone

- **Summary:** On a fresh install on a new phone, the first-run flow could lose the welcome card if the user tapped `Create Your First Habit` and cancelled back, and did not surface essential new-phone setup steps (restoring a portable backup, choosing the week-start day, and checking Android notification readiness).
- **Exact Code Locations Inspected:**
  - `app/src/main/java/com/example/dayseed/ui/screens/TodayScreen.kt` (lines 195–240, 360–528)
  - `app/src/main/java/com/example/dayseed/data/DayseedRepository.kt` (lines 1365–1408)
  - `app/src/main/java/com/example/dayseed/data/DayseedDatabaseHelper.kt` (lines 710–735)
- **Reproduction Steps:**
  1. Launch Dayseed on a fresh database (`onboarding_completed = "false"`, 0 habits).
  2. Tap `onboarding_create_first_habit_btn` to open `CreateEditHabitScreen`, then press Back (`Cancel`) without saving a habit.
  3. Previously, `onboarding_create_first_habit_btn` had already written `onboarding_completed = "true"`, so returning to `TodayScreen` hid `onboarding_welcome_card` even though the user still had 0 habits.
- **Root Cause:**
  - `onboarding_create_first_habit_btn` mutated `onboarding_completed = "true"` on navigation intent rather than on habit save completion, and `TodayScreen` lacked a compact new-phone setup checklist.
- **Remediation Implemented:**
  - Removed the premature `onboarding_completed = "true"` write from `onboarding_create_first_habit_btn` in `TodayScreen.kt`.
  - Updated `DayseedRepository.saveHabitSync` to persist `onboarding_completed = "true"` inside the SQLite write transaction whenever a habit is saved.
  - Added a compact 3-row New-Phone Quick Setup inside `onboarding_welcome_card` (`onboarding_setup_checklist`) exposing:
    - Inline `Week: Mon/Sun (tap)` toggle (`onboarding_week_start_toggle_btn`),
    - Live notification permission status (`onboarding_notification_status_text`) and Android 13+ `Allow` action (`onboarding_request_notifications_btn`),
    - `Create First Habit` (`onboarding_create_first_habit_btn`), `Restore Backup` (`onboarding_restore_backup_btn` navigating to `Settings`), and `Explore First` (`onboarding_dismiss_btn`).
  - Kept the card compact (`~82dp` height) so `Today's Seeds` (`today_seeds_section_header`) and `EmptyHabitsCard` (`today_empty_habits_card`) remain visible above `add_habit_fab` even on a `320x680dp` viewport.
- **Automated Verification:**
  - `OwnerReportedDefectsRemediationTest.odr003_firstRunOnboardingChecklistPreservesStateOnCancelAndCompletesOnHabitSave`: **PASSED**.
  - `CanonicalUiAndBehaviourReconciliationTest.responsiveLayoutsVerifyJournalHeaderInsightsCardsAndTodayFabAcrossViewports`: **PASSED** across `320dp`, `360dp`, and `411dp`.

---

### 4. `ODR-004` — Soft Keyboard Obscures Active Text Fields (`P2-H13`)

- **Summary:** When typing in `CreateEditHabitScreen`, `RecordSessionSheet`, `JournalEditorDialog`, `HabitDetailScreen`, `HabitsScreen`, or `SettingsPanels`, the soft keyboard covered the active text field so the user could not see the text being typed without dismissing the keyboard.
- **Exact Code Locations Inspected:**
  - `app/src/main/AndroidManifest.xml` (`android:windowSoftInputMode="adjustResize"`)
  - `app/src/main/java/com/example/dayseed/MainActivity.kt` (lines 110–215)
  - `app/src/main/java/com/example/dayseed/ui/screens/KeyboardVisibilitySupport.kt` (new file)
  - `app/src/main/java/com/example/dayseed/ui/screens/CreateEditHabitScreen.kt`
  - `app/src/main/java/com/example/dayseed/ui/screens/RecordSessionSheet.kt`
  - `app/src/main/java/com/example/dayseed/ui/screens/JournalEditorDialog.kt`
  - `app/src/main/java/com/example/dayseed/ui/screens/HabitDetailScreen.kt`
  - `app/src/main/java/com/example/dayseed/ui/screens/HabitsScreen.kt`
  - `app/src/main/java/com/example/dayseed/ui/screens/SettingsPanels.kt`
- **Root Cause:**
  1. `MainActivity.kt` rendered the `80.dp` bottom `NavigationBar` unconditionally inside the outer `Scaffold`, stacking the `80.dp` navigation bar on top of the open soft keyboard and shrinking the visible form viewport.
  2. Scrollable form containers (`CreateEditHabitScreen`, `RecordSessionSheet`, `JournalEditorDialog`, `HabitDetailScreen`, `SettingsPanels`) lacked `.imePadding()`.
  3. `OutlinedTextField` composables lacked a focus-triggered `BringIntoViewRequester` that re-requests visibility both when focus arrives and after the Android IME finishes its resize animation (`WindowInsets.ime` height change).
- **Remediation Implemented:**
  - Created `Modifier.bringIntoViewOnFocus(extraBottomClearance: Dp = 28.dp)` in `KeyboardVisibilitySupport.kt`, which attaches a `BringIntoViewRequester`, listens to `onFocusEvent`, `onSizeChanged`, and `WindowInsets.ime.getBottom(density)`, and calls `bringIntoView` immediately plus after the IME resize settles (`140ms` and `300ms`) with `28.dp` bottom clearance for supporting/error text.
  - Updated `MainActivity.kt` to hide the outer `NavigationBar` whenever `WindowInsets.ime.getBottom(LocalDensity.current) > 0`.
  - Applied `.imePadding()` and `.bringIntoViewOnFocus()` to all text inputs across `CreateEditHabitScreen`, `RecordSessionSheet`, `JournalEditorDialog`, `HabitDetailScreen`, `HabitsScreen`, and `SettingsPanels`.
- **Automated Verification:**
  - `OwnerReportedDefectsRemediationTest.odr004_textFieldsRemainScrollableAndVisibleInsideKeyboardConstrainedViewport`: **PASSED**.

---

## D. Code & Documentation Changes Made

| File | Summary of Changes |
|---|---|
| `app/src/main/java/com/example/dayseed/ui/screens/SettingsSupport.kt` | Added `parseFlexibleLocalTime`, `formatTime12Hour`, `formatTime24Hour`, `formatUnambiguousTimeDisplay`, `switchTimeAmPm`, `isPmTime`, and `quietHoursUnambiguousSummary` (`ODR-001`). |
| `app/src/main/java/com/example/dayseed/ui/screens/KeyboardVisibilitySupport.kt` | New reusable `Modifier.bringIntoViewOnFocus()` helper keeping focused text fields and supporting text visible above the soft keyboard (`ODR-004`). |
| `app/src/main/java/com/example/dayseed/MainActivity.kt` | Hid the outer `80.dp` bottom `NavigationBar` while `WindowInsets.ime.getBottom(LocalDensity.current) > 0` (`ODR-004`). |
| `app/src/main/java/com/example/dayseed/data/ReminderNotifier.kt` | Added `contentIntent` (`PendingIntent` opening `MainActivity`) to `postHabitReminder` and `NotificationCompat.CATEGORY_REMINDER` to both `postHabitReminder` and `postWeeklyReminder` (`ODR-002`). |
| `app/src/main/java/com/example/dayseed/data/DayseedRepository.kt` | Updated `saveHabitSync` to persist `onboarding_completed = "true"` inside the SQLite transaction whenever a habit is saved (`ODR-003`). |
| `app/src/main/java/com/example/dayseed/ui/screens/CreateEditHabitScreen.kt` | Added unambiguous 12h+24h reminder time preview, `AM`/`PM` chips, `-30m`/`+30m` steppers, quick presets, Quiet Hours conflict warning + 1-tap PM switch, honest reminder delivery readiness notice + Android 13+ permission button, `.imePadding()`, and `.bringIntoViewOnFocus()` (`ODR-001`, `ODR-002`, `ODR-004`). |
| `app/src/main/java/com/example/dayseed/ui/screens/TodayScreen.kt` | Removed premature `onboarding_completed = "true"` write on `Create First Habit` tap, and added compact 3-row New-Phone Quick Setup inside `onboarding_welcome_card` (`ODR-003`, `UI-03`). |
| `app/src/main/java/com/example/dayseed/ui/screens/HabitDetailScreen.kt` | Added `detail_reminder_badge` with unambiguous 12h+24h reminder time, unambiguous session timestamp formatting, `.imePadding()`, and `.bringIntoViewOnFocus()` (`ODR-001`, `ODR-002`, `ODR-004`). |
| `app/src/main/java/com/example/dayseed/ui/screens/HabitsScreen.kt` | Appended unambiguous reminder time to `habits_meta_<id>` and added `.imePadding()` + `.bringIntoViewOnFocus()` to search input (`ODR-001`, `ODR-002`, `ODR-004`). |
| `app/src/main/java/com/example/dayseed/ui/screens/RecordSessionSheet.kt` | Added `.imePadding()` and `.bringIntoViewOnFocus()` to date, quantity/duration, and session note inputs (`ODR-004`). |
| `app/src/main/java/com/example/dayseed/ui/screens/JournalEditorDialog.kt` | Added `.imePadding()` and `.bringIntoViewOnFocus()` to date, title, body, and new-tag inputs (`ODR-004`). |
| `app/src/main/java/com/example/dayseed/ui/screens/SettingsPanels.kt` | Updated `QuietHoursPanel` with unambiguous 12h+24h time summaries and flexible 12h/24h parsing, and added `.imePadding()` + `.bringIntoViewOnFocus()` to all Settings text inputs (`ODR-001`, `ODR-004`). |
| `app/src/test/java/com/example/dayseed/OwnerReportedDefectsRemediationTest.kt` | Added 5 JUnit/Robolectric regression tests covering `ODR-001`, `ODR-002`, `ODR-003`, and `ODR-004`. |
| `docs/43-OWNER-REPORTED-DEFECT-INTAKE-2026-11-14.md` | Updated status cross-references to link to this investigation and remediation report while keeping physical-device validation cases `NOT RUN`. |
| `docs/12-PROGRESS-AND-HANDOVER.md` & `docs/28-REQUIREMENT-TRACEABILITY.md` | Recorded the 2026-11-14 bug investigation, remediation, and 276/276 test verification results. |

---

## E. Executed Verification Matrix (`2026-11-14`)

| Verification Check | Command / Tool | Status | Inspected Evidence |
|---|---|---|---|
| **Applet Compilation** | `compile_applet` | **PASSED** | `Build succeeded - the applet is compiled.` |
| **Owner-Reported Defects Regression Suite (`ODR-001`–`ODR-004`)** | `gradle :app:testDebugUnitTest --tests com.example.dayseed.OwnerReportedDefectsRemediationTest` | **PASSED (5/5)** | `TEST-com.example.dayseed.OwnerReportedDefectsRemediationTest.xml`: 5 tests, 0 failures, 0 errors. |
| **Canonical UI & Responsive Viewport Suite (`320dp`, `360dp`, `411dp`)** | `gradle :app:testDebugUnitTest --tests com.example.dayseed.CanonicalUiAndBehaviourReconciliationTest` | **PASSED (15/15)** | `TEST-com.example.dayseed.CanonicalUiAndBehaviourReconciliationTest.xml`: 15 tests, 0 failures, 0 errors. |
| **Full Unit & Robolectric Suite** | `gradle :app:testDebugUnitTest` | **PASSED (276/276 across 20 classes)** | 276 tests completed, 0 failures, 0 errors, 0 skipped. |
| **Android Lint** | `gradle :app:lintDebug` | **PASSED (`0 errors`)** | `app/build/reports/lint-results-debug.html`: `0 errors`. |
| **Debug & Release APK Builds** | `gradle :app:assembleDebug :app:assembleRelease` | **PASSED** | `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release.apk` built cleanly. |
| **Markdown Link Integrity** | `bash scripts/check-doc-links.sh` | **PASSED (`0 broken`)** | Verified all relative markdown links and confirmed `docs/42-RESPONSIVE-UI-LAYOUT-CORRECTIONS-REPORT.md` is absent. |
| **Physical Android Device Validation (`docs/38`, 75 cases incl. `P2-H13`, `P25-H03`)** | Physical Android hardware | **`NOT RUN`** | Requires physical Android hardware execution. Never claimed as passed from automated or container runs. |

---

## F. Remaining Risks, Honest Limitations & Open Owner Questions

1. **Physical-Device Notification Verification Gate (`D-221` / `P25-H03`):**
   - Under `D-221` and `docs/26-SCREEN-AND-DATA-CONTRACTS.md` §7.2, `Settings → Gentle Reminders` (`reminders_global_switch`) remains intentionally disabled (`enabled = false`) in production builds until physical-device notification delivery (`P25-H03` and `P25-H04`) is verified on a real phone.
   - **Owner Question (Optional):** If you want to test live end-to-end OS alarm notifications interactively on your physical phone right now (outside the debug probe), would you like a follow-up change that unlocks `reminders_global_switch` in `SettingsScreen.kt` for your device trial, or should it remain gated until formal `P25-H03` device validation?
2. **Physical-Device Soft-Keyboard Verification (`P2-H13` / `ODR-004`):**
   - While `.imePadding()`, focus-driven `BringIntoViewRequester` (`Modifier.bringIntoViewOnFocus()`), and hiding the outer `80.dp` `NavigationBar` when the IME opens are now implemented and verified in Robolectric, `docs/38` case `P2-H13` remains `NOT RUN` until confirmed on a physical phone with an OEM input method (e.g., Gboard / Samsung Keyboard).
3. **Stage C & Stage D Governance Boundaries:**
   - Stage C (`Independent Technical Validation`) and Stage D (`Final Owner Acceptance / Release Readiness`) remain **`NOT OPEN`**.
