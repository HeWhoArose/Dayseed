# Planning UX and Traceability Closure Pass

**Review date:** 2026-10-28  
**Status:** Document-only desk review completed for the selected screen directions and their written contracts. The recommendations below are proposed working defaults, not new owner approvals. This is not a moderated usability study, live-app inspection, measured contrast check, TalkBack run, or executed test. The planning gate remains **not approved**.

**Authority:** Owner decisions remain in [Decision Log](09-DECISION-LOG.md). This pass links the [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md), [UX Approval Pack](29-UX-DESIGN-APPROVAL-PACK.md), [Requirement Traceability Matrix](28-REQUIREMENT-TRACEABILITY.md), [Testing and Quality](07-TESTING-AND-QUALITY.md), and the [Monetisation and Ads Evaluation](33-MONETISATION-AND-ADS-EVALUATION.md). The owner-provided layout reference and SVG boards remain directional prototypes; they are not approved UI.

## 1. Result at a glance

The current written plan covers the main v1 product flows. The clearest remaining planning weakness was that the shared accessibility scorecard did not connect each screen task to a specific observable result and a traceable planned acceptance case. The other notable gaps were (a) Growth Mark formatting/empty/error conventions were described as open but not given one coherent, reviewable default, (b) ad acceptance coverage was not a distinct row in the traceability matrix, and (c) habit/session notes did not have their own clearly identifiable traceability row.

This pass addresses those documentation gaps by:
- mapping Today, navigation, History/Journal, Growth Mark, comeback, and Settings tasks to observable expected outcomes;
- proposing conservative duration formatting, period selection, and no-record/zero/failure states for owner review;
- adding planned acceptance rows AC-026 to AC-028 for ads policy, cross-screen task/accessibility flows, and D-192 habit notes; and
- recording the remaining owner-level choices separately from routine defaults.

No product behaviour has been observed in a running app, no usability result has been invented, and none of the acceptance cases has been executed.

## 2. Desk-review scorecard for selected screen directions

| Flow | Task a person must be able to complete | Required observable result / main risk to prevent | Traceability |
|---|---|---|---|
| **W1 Today** | Log a measured habit, use Complete for the usual target, then use Record to correct the actual value | Complete and Record stay distinct; a correction changes only the selected record and its derived summaries; visible actions do not disappear under large text or an open keyboard | AC-001, AC-002, AC-003, AC-005, AC-018, AC-023, AC-027 |
| **Today: schedule meaning** | Tell a daily/selected-weekday habit apart from a flexible weekly target | The period and denominator are explicit. A flexible weekly target is not represented as a daily failure, and preferred days do not restrict valid completion | AC-004, AC-006, AC-018, AC-027 |
| **Today → Journal** | Start and save a reflection without accidentally completing a habit | Journal remains optional; Save is explicit; a failed/uncertain save keeps the draft and stable save identity; journal-only activity does not change habit status | AC-007, AC-016, AC-019, AC-024, AC-027 |
| **N1 navigation** | Find Journal from Today, then find the archive again from History | Four labelled destinations remain Today, History, Insights, and Settings. Journal has a plain-language entry point on Today and an obvious section within History; not icon-only and no dead-end editor | AC-018, AC-021, AC-027 |
| **A2 History / Journal archive** | Find an entry by date and phrase, distinguish multiple entries on one date, edit a typo, cancel deletion, then confirm deletion | Saved entries remain separate; local search is limited to approved saved title/body fields; cancellation preserves the entry; empty date, no search results, and search failure are distinguishable; archive position is recoverable after opening an entry | AC-007, AC-009, AC-016, AC-018, AC-021, AC-027 |
| **G2 Collected moments** | Understand the total and its period without opening a help screen | The total includes saved, compatible duration sessions only, labels the reporting period, never implies success/failure, and distinguishes zero from no records or a failed calculation | AC-025, AC-027 |
| **G3 Calendar / date detail** | Tap the summary, move to a date/week/month, and inspect the duration sessions behind a date total | Calendar month is a reporting period, not an elapsed-duration unit; selected date/period is clear; each contributing record is inspectable; corrections/deletions recompute totals; empty dates stay neutral | AC-025, AC-027 |
| **Comeback** | Return after a qualifying absence and continue without writing a reflection | The approved trigger precedence is respected; Continue/Not now is visible; no forced explanation or duplicate prompt; existing draft is preserved | AC-010, AC-024, AC-027 |
| **Settings: data and privacy** | Find readable export, portable backup/restore, privacy choices, and destructive deletion controls | Each action explains scope before commit; confirm/cancel is clear; provider-backed files are not presented as guaranteed device-only; failure never reports success or mutates live data partially | AC-014, AC-015, AC-016, AC-019, AC-020, AC-022, AC-027 |
| **Offline and recovery** | Use core flows with no network, then recover from a local save/search/restore failure | Core records remain accessible offline; loading cannot spin forever; failures are not shown as empty success; retry does not duplicate records and preserves user input | AC-017, AC-019, AC-026, AC-027 |

**Desk-review disposition:** no new core screen or product feature was identified as missing from the existing scope. The contract has been made more testable, but the owner still needs to review the proposed Growth Mark presentation and ads scope. The screen boards still require actual task-based review before final UI approval; accessible behaviour must eventually be verified in the real app, not inferred from a wireframe.

## 3. Growth Mark: recommended working default for owner review

D-199 establishes the metric direction and current-calendar-month default. The following details make that direction testable. They are recommendations under the planning process—not additional owner-approved product decisions. They may be accepted, modified, or deferred as a group.

### G2 summary

1. **Period:** default to the current calendar month, with a period label that stays understandable when the month changes. A full month/year label should be available to assistive technology and date detail. Do not label a calendar month as a duration unit.
2. **Eligible records:** sum only saved duration sessions whose duration values can be converted to a common elapsed-time unit. Keep source values unchanged and sum before rounding. Never include quantity, completion-only, effort-aware, journal, draft, or unsaved records. Do not silently mix an unrecognised or incompatible value into the total; surface a recoverable calculation/data warning and keep the source record inspectable.
3. **Display magnitude:** show minutes below one hour; hours and remaining minutes below 24 hours; days and remaining hours below seven days; weeks and remaining days from seven days upward. Suppress zero-valued trailing units, use at most two meaningful units, and round only the final displayed total at the precision the input model supports. If positive sub-minute values are supported, do not round them to a misleading zero. Never invent a “month” duration equivalent: month lengths vary, while “month” is valid as the reporting period.
4. **Zero, no records, and failure:** no eligible session in the selected period should say that no time is recorded, not display a numeric zero. Saved explicit-zero sessions should remain distinguishable and may correctly produce “0 min recorded” with the relevant session visible in G3. A failed calculation/load should show an error and retry path, never a zero or empty-success state. No duration-tracked habit and no recorded session are different conditions if the contract can reliably tell them apart.
5. **Navigation and detail:** tapping G2 opens G3 and carries the selected period forward. G3 can switch among Day, configured Week, and Calendar Month; the stored week-start preference controls week boundaries. A date selection shows its total and the underlying duration sessions with enough information to identify each source record and correct it through the normal History/record flow.
6. **Accessible description:** the card must expose a plain-language name, full duration, reporting period, and action (for example, “Collected moments. Three hours forty minutes recorded in October 2026. Open duration details.”). Do not rely on colour, an emblem, or a number alone.

**Recommended display examples, illustrative only:** 45 min recorded this month; 3 hr 40 min recorded this month; 1 day 3 hr recorded this month; 1 wk 2 days recorded this month. In the detail view, show the actual selected month/date range. The label should not imply that the duration occurred continuously or that the user met a target.

### Required planned acceptance cases

Use known synthetic records for mixed minute/hour inputs, day/week totals, period boundaries, a month transition, explicit zero, no records, unsupported unit, edits/deletions, and date navigation. Check that quantity, completion-only, effort-aware, journal, and draft data never enter the metric. Check full text at enlarged sizes, sensible focus order, screen-reader output, non-colour meaning, and a recoverable path to source records. These are planned acceptance cases only.

## 4. Cross-screen state contract

| State | Required behaviour across affected screens |
|---|---|
| Normal saved data | Display values derived from committed records and clear local-date/reporting-period labels |
| No record | Neutral explanatory text and a relevant next step; never imply failure and never substitute zero for absence |
| Explicit zero | Preserve and label the saved zero as a real record; allow its source record to be inspected |
| Loading | Keep context; do not announce success or show a fabricated placeholder record; loading must terminate into data or an error |
| Save failure / uncertain save | Preserve user input/draft; offer safe retry with the same logical submission ID; do not duplicate a saved record |
| Calculation/search failure | Distinguish failure from an empty result; provide retry; do not report a wrong zero/empty archive |
| Offline | Habit, Journal, History, Insights and comeback core functions remain usable. An optional ad/network failure cannot block or impair them |
| Destructive action | State the exact scope and consequences before confirming; Cancel leaves data untouched; success appears only after the committed action finishes |
| Accessibility settings | Text scales and wraps; names/roles/focus are meaningful; colour is not the only cue; control/action remains available on compact screens and with the keyboard open |

## 5. Traceability changes and coverage result

The matrix in [28-REQUIREMENT-TRACEABILITY.md](28-REQUIREMENT-TRACEABILITY.md) now adds:
- **AC-026 — ads policy and failure behaviour:** only applies if ads are explicitly included in v1. Covers audience/region, consent, SDK data-flow review, truthful store/privacy disclosures, no private habit/journal targeting, no core-flow gating, offline/load failure, and accessibility.
- **AC-027 — task-flow and accessibility coverage:** maps the selected W1/N1/A2/G2/G3 flows to observable completion, edge states, recovery and planned accessibility checks.
- **AC-028 — habit/session notes (D-192):** preserves the difference between a per-session note and a habit/date note, confirms neither changes progress/status, and covers edit/delete plus export/restore.

The wider matrix provides coverage for habit configuration/lifecycle (AC-001/004/005/008/020), measured activity/outcomes (AC-002/003/006/023), Journal/moods (AC-007/009/016/019/024), dates/history (AC-006/013/025/027), reminders/reflection (AC-010/011/012/024), offline/recovery (AC-017/019), backup/privacy (AC-014/015/016/019/020/022), accessibility and task comprehension (AC-018/021/027), and ads only if approved (AC-026). Coverage means a planned case exists; it does not mean implementation or testing is complete.

## 6. Owner choices still pending

This review intentionally does not convert a recommendation into an owner decision. The consolidated batch in [31-OWNER-PLANNING-DECISION-BATCH.md](31-OWNER-PLANNING-DECISION-BATCH.md) should request only the consequential choices that remain:

1. **Initial-v1 ads: include the banner candidate or defer all ads.** The owner said that a full launch needs at least ads and requested evaluation; that statement did not approve an SDK or placement. The exact include/defer decision is still required.
2. **Growth Mark working defaults: accept, modify, or defer.** The proposed thresholds and states above are designed to be explicit and testable. Accessibility and usability evidence still needs a later real UI review.
3. **Visual identity: accept the recommended synthesis as the exploration baseline or explicitly defer final style/tokens.** Keep the already accepted system typography and system light/dark baseline; DESIGN.md remains the single token/component authority.

For ads, even a product-scope “include banner candidate” decision does not authorise SDK integration. Target audience and launch regions, current SDK terms, consent, privacy/data flows, compatibility, and release disclosures must be resolved at the appropriate later approval and validation gates.

## 7. Boundaries and next step

No new decision ID is created in this pass. Do not alter docs/09-DECISION-LOG.md to imply approval of any recommendation. Once the owner responds, reconcile the actual response across the decision log, UX pack, screen/data contracts, monetisation evaluation, readiness review, traceability, and handover. Then present the final planning-readiness report and ask for a distinct explicit gate decision.

No application code, schema/database, migration, build, automated test, APK, device state, runtime data flow, or accessibility/usability result was changed, inspected, run, or claimed here. Technical validation and feature implementation remain separately gated.
