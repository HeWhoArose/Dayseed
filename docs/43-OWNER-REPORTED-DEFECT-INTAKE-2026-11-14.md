# Dayseed — Owner-Reported Defect Intake (2026-11-14)

**Repository:** `HeWhoArose/Dayseed`  
**Branch:** `main`  
**Status:** Investigated, reproduced, and remediated in source/automated tests — see [docs/44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md). Physical-device validation cases remain `NOT RUN`.  
**Governance:** Controlled Stage B investigation/remediation only. Stage C and Stage D remain closed. No physical-device test is marked passed.

## Purpose

Record additional defects described by the owner so they are not lost while the existing repository bug reports and acceptance cases are reconciled. Full root-cause traces, code fixes, and regression test evidence (`OwnerReportedDefectsRemediationTest`) are recorded in [docs/44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md).

## Newly reported findings

### ODR-001 — Reminder time display does not communicate AM/PM clearly

- **Owner observation:** When choosing or viewing a reminder time, the UI appears to show a bare hour such as `4`, without making clear whether it means 4 a.m. or 4 p.m.
- **Expected behaviour:** The selected time must be unambiguous. Use a clear locale-appropriate 12-hour display with AM/PM or an explicitly labelled 24-hour display. Do not silently change the stored time semantics.
- **Related existing contract:** `docs/26-SCREEN-AND-DATA-CONTRACTS.md` §7.2/§7.5; reminder-time storage and validation are described as strict 24-hour `HH:mm` in `docs/26` and `docs/38` P2-C08.
- **Investigation required:** Trace the complete Settings and per-habit time entry/display path, including formatting, parsing, persistence, locale and device 12/24-hour preference. Determine whether the report concerns the reminder-time field, a native picker, or another timer UI.
- **Status:** **FIXED in source & automated tests (`OwnerReportedDefectsRemediationTest.odr001_*`); physical-device UI check `NOT RUN`.** See [docs/44](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md) §C.1.

### ODR-002 — Configured reminder produces no audible/visible notification

- **Owner observation:** After setting an alarm/reminder, there is no ring, notification or other observable alert.
- **Expected behaviour:** If reminders are offered as active, an eligible configured reminder should produce the promised user-visible notification, subject to Android notification permission, app notification state, channel state, quiet hours, schedule eligibility and the documented inexact delivery window.
- **Related existing cases:** `docs/38` P25-H03 (end-to-end delivery), P25-H04 (reboot/time-change/Doze), P25-H05 (permission-denied honesty), RM-H01 onward (delivery lifecycle and recovery).
- **Important existing design constraint:** `docs/26` §7.2 and §7.2/§7.3 amendments describe an honesty gate: production reminder activation switches remain disabled until physical-device delivery verification. The reminder pipeline has extensive automated coverage, but this does not establish that a real phone delivered a notification.
- **Investigation required:** Confirm exactly which UI control the owner used; inspect whether the activation control was disabled, whether global opt-in and Android permission were granted, whether app/channel notifications were enabled, whether an alarm was registered, and whether the receiver posted or suppressed it. Capture the ledger and Android notification state without assuming that a scheduled alarm equals a delivered notification.
- **Status:** **PARTIAL — Source gaps fixed (Quiet Hours collision warning + 1-tap PM switch, habit-form reminder readiness notice + Android 13+ permission request button, detail/list reminder badges, and `ReminderNotifier.postHabitReminder` `contentIntent` + `CATEGORY_REMINDER`) & verified in Robolectric (`OwnerReportedDefectsRemediationTest.odr002_*`); production `reminders_global_switch` remains gated under `D-221` pending physical-device `P25-H03` (`NOT RUN`).** See [docs/44](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md) §C.2.

### ODR-003 — First-run experience on a new phone does not complete required setup clearly

- **Owner observation:** The app does not seem to guide or initialise a new-phone first launch through everything it needs to do. The precise missing step has not yet been identified.
- **Expected behaviour:** A clean installation should initialise safely and guide the user through required first-run choices and any permission-dependent setup. Core habit and journal use must remain available when optional notification permission is denied.
- **Related existing contract:** `docs/26-SCREEN-AND-DATA-CONTRACTS.md` §7.2 and §7.2 expanded states describe first-launch global reminder opt-in and separate Android notification permission; `docs/38` P4-H01 and P25-H05 cover permission allow/deny behaviour.
- **Investigation required:** Reproduce from a genuinely clean install on a disposable test device/profile (never clear or uninstall the owner's live app data). Trace database creation, onboarding state, default settings, first-run welcome, reminder opt-in, Android 13+ notification permission request, denied-permission recovery, and subsequent relaunch. Ask the owner for the exact missing setup step only if source/device investigation cannot narrow it down.
- **Status:** **FIXED in source & automated tests (`OwnerReportedDefectsRemediationTest.odr003_*`); physical-device check `NOT RUN`.** See [docs/44](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md) §C.3.

### ODR-004 — Soft keyboard obscures the field being edited

- **Owner observation:** During habit creation or other text entry, the keyboard covers the active writing field. The owner must dismiss the keyboard to inspect what was typed, then reopen it and continue typing without being able to see the text.
- **Expected behaviour:** The focused input and text being entered remain visible above the soft keyboard; the screen scrolls or resizes as needed, and primary actions remain reachable without dismissing the keyboard.
- **Existing report/case:** This is already represented by `docs/38` P2-H13, covering Create/Edit Habit and Record Session, with an expectation that focused fields and Save/Cancel remain reachable above the keyboard. The screen/data contract also states the keyboard-open layout requirement.
- **Investigation required:** Test every text-entry surface, not only Create/Edit Habit: habit name/description/target, session notes, journal editor, search, settings time fields and any dialogs/sheets. Check actual window soft-input mode, edge-to-edge insets, nested scroll containers, `imePadding()`, focus scrolling, modal sheet behaviour and device-specific keyboard height. Verify that the cursor and typed text remain visible while typing.
- **Status:** **FIXED in source & automated tests (`OwnerReportedDefectsRemediationTest.odr004_*`); physical-device `P2-H13` remains `NOT RUN`.** See [docs/44](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md) §C.4.

## Existing repository context — do not misclassify

- `docs/38` P2-H13 already records the keyboard-open layout acceptance case.
- `docs/38` P25-H03/P25-H04/P25-H05 and RM-H cases already record real-device notification delivery and recovery checks.
- `docs/41-SYSTEM-AUDIT-2026-11-11.md` states that reminder delivery was statically reviewed but not executed on a device; it explicitly records the device cases as NOT RUN.
- `docs/26-SCREEN-AND-DATA-CONTRACTS.md` specifies strict 24-hour `HH:mm` reminder input, while the owner is reporting an ambiguous displayed hour. These may be different layers of the same flow; inspect before changing formats or storage.
- Do not claim ODR-001 through ODR-004 are confirmed source defects until reproduced and traced. Do not mark any existing physical-device case passed based on automated tests or this intake.

## Investigation & remediation record

Full investigation, reproduction, root-cause analysis, code remediation, and verification results (`276/276` JUnit/Robolectric tests across 20 classes, `0` lint errors, debug/release APK builds) are documented in [docs/44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md](44-COMPREHENSIVE-BUG-INVESTIGATION-AND-REMEDIATION-2026-11-14.md).

