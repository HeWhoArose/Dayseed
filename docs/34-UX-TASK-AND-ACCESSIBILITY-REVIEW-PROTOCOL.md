# Dayseed UX Task and Accessibility Review Protocol

**Status:** Prepared for future review; not executed.  
**Date prepared:** 2026-10-29  
**Scope:** Planning-only task script and evidence template for the selected W1 Today, N1 navigation, A2 hybrid Journal archive, and modified G2/G3 Growth Mark directions.  
**Gate:** Closed. This document does not authorise application implementation, builds, automated tests, device testing, or a usability study.

## 1. Purpose and rules

Use this protocol when the owner authorises a review with sufficiently complete, comparable prototypes or a runnable build. The owner-provided reference at `docs/prototypes/dayseed-owner-reference-layout.webp` is a mood and hierarchy guardrail, not a pixel-perfect specification. Written decisions and screen/data contracts take precedence over older details in the image.

- Use the same fictional data, task wording, device conditions, and assistance rules across any options being compared.
- Do not teach participants where a control is before recording their first attempt. If they ask for help, record the request and then assist so the session remains comfortable.
- Never use real private journal content. Use synthetic habits, notes, and records.
- Do not ask people to explain personal absence, health, mood, or why they missed a habit. Tasks concern interface comprehension, not personal disclosure.
- Record what was observed, not what the reviewer thinks would have happened. Keep observations, interpretation, and proposed changes in separate fields.
- Do not infer a usability result from a static SVG. If only SVGs are available, perform a reviewer walkthrough and mark interaction/device evidence “not testable from current artefact.”
- Do not mark any accessibility item passed from source text or a static illustration alone.
- The owner may pause, skip any task, or stop the session. No task outcome is worth pressuring a participant.

## 2. Shared synthetic fixture

Keep this fixture identical across compared variants:

- Habits: **Walk** (duration, daily target 20 min); **Read** (quantity, daily target 10 pages); **Stretch** (completion-only).
- Today: Walk has an explicitly saved value of 0 minutes; Read has 10 pages recorded; Stretch has no record. Ensure the visual wording distinguishes recorded zero from not recorded.
- Journal: two saved entries on one date; one older entry with a searchable phrase; one entry with a title; one body-only entry; one long entry; and one unsaved draft that must not appear as a saved archive result.
- Archive dates: include the last few days of one month and first few days of the next, with a blank date and multiple entries on one date.
- Growth Mark: use the existing synthetic fixture whose six date totals sum to **3 hr 40 min**. Include a saved zero-duration session separately from a date with no duration session. Do not add quantity, completion-only, effort-aware, or journal data to the duration total.
- Settings: include labelled destinations for privacy/data explanation, readable export, portable backup/restore, and the distinct deletion actions. Do not imply Android platform Auto Backup is the same as Dayseed portable backup.

If any prototype cannot show a required fixture state, mark it **not represented** rather than improvising a result.

## 3. Task script

Read each task exactly as written. Do not name the expected control or destination.

| ID | Read to reviewer/participant | Observe and record | Expected policy-level outcome (not a usability score) |
|---|---|---|---|
| T-01 | “You want to record that you spent zero minutes walking today. Please record it.” | First action, wrong turns, help requests, whether saved zero is visible | A saved zero is distinguishable from no record; no fabricated activity |
| T-02 | “What does the Walk row tell you? What does the Stretch row tell you?” | Explanation in their own words | Explicit zero, unrecorded, rest/non-scheduled and derived-missed concepts are not conflated |
| T-03 | “Write a journal entry about a fictional event and save it.” | Discoverability, save confirmation, whether draft and saved entry are distinguishable | Saved entry is retrievable; no blank/whitespace-only entry is saved |
| T-04 | “Find the saved entry containing the phrase ‘blue umbrella’.” | Search/date route, wrong taps, backtracking, result accuracy | Finds the intended saved entry; unresolved draft is not returned as a saved entry |
| T-05 | “Find both journal entries from Tuesday, 27 October.” | Date navigation and multiple-entry discoverability | Both entries on that date can be found |
| T-06 | “Change one word in the older entry, save it, then open it again.” | Edit path, saved result, recovery from mistakes | Edit is explicit and persisted; unrelated entries remain unchanged |
| T-07 | “Start changing an entry, then leave without saving the change.” | Cancel/back behaviour and resulting content | Unsaved changes are not silently saved; existing saved content remains intact |
| T-08 | “Delete the fictional entry named ‘Rainy afternoon’.” | Confirmation wording, cancel option, post-delete archive position | Permanent deletion is explicit; cancellation preserves the entry; only the selected entry is deleted |
| T-09 | “Search for ‘purple bicycle’, which is not in the journal.” | No-results wording and available recovery path | Neutral no-results state; no fake records or implication of failure |
| T-10 | “Explain what the Collected moments total measures and which period it covers.” | Explanation of metric, units and period | Duration-tracked saved sessions only; current calendar month default is explicit; no count/score implication |
| T-11 | “Open 27 October in Growth Mark and show the records behind that date’s total.” | Route to date detail and source-session visibility | Date-level total corresponds to inspectable duration sessions |
| T-12 | “Correct or remove one fictional duration session. What should happen to the total?” | Whether source record and recalculated total are understandable | Only selected session changes; compatible duration total recalculates; explicit zero is not converted into missing data |
| T-13 | “Find where you would read how Dayseed stores data, export a readable journal, make a portable backup, and restore one.” | Findability, wording and distinction between export, portable backup, and OS backup | All destinations and consequences are discoverable; portable backup is not confused with Android Auto Backup |
| T-14 | “Find the separate controls for deleting a journal entry, deleting a habit and its tracking history, and deleting all app data. Do not confirm any deletion.” | Discoverability and consequence comprehension | Different scopes are clearly distinguished; no destructive action occurs during this task |
| T-15 | “A comeback message appears. Continue into the app without writing a reflection.” | Whether continue/not-now is visible and whether any writing is forced | Comeback does not block access; reflection remains optional; no guilt or forced explanation |
| T-16 | “Show how you would find the next weekly reflection setting or understand when it appears.” | Settings discoverability and comprehension of local week-start timing | 09:00 device-local on the configured week-start day is documented distinctly from per-habit reminders |

## 4. Evidence to capture for each task

Record one row per attempt. Leave fields blank or mark “not run” until evidence exists.

- Session ID (pseudonym; no personal data):
- Date and reviewer:
- Artefact/build identifier and version:
- Variant (W1/N1/A2/G2/G3 or comparator):
- Device model, Android version, display size/density:
- Font/display scaling and theme:
- Task ID:
- Outcome: **Completed unaided / Completed with help / Not completed / Not testable from artefact / Not run**
- Time and taps, only if measured consistently:
- Wrong turns/backtracking:
- Participant explanation in their own words (avoid private personal content):
- Accessibility issue observed:
- Evidence reference (screen recording/photo/note, with consent):
- Reviewer observation (facts only):
- Interpretation (separate from observation):
- Proposed follow-up, owner, severity:
- Retest result and date:

Do not calculate or publish aggregate success rates from a single session. Do not invent timings, participant quotes, or scores.

## 5. Accessibility and layout review matrix

Run against actual rendered screens or an authorised build, not the SVG annotation canvas. Record the exact device, OS, setting, screen, and evidence for each item.

| ID | Check | Evidence required | Status until performed |
|---|---|---|---|
| A-01 | Compact and typical Android viewport; portrait and relevant orientation changes | Screenshots/recording; clipped or obscured content; scroll recovery | Not tested |
| A-02 | Largest available font scaling (up to 200% where supported) and display size | Screens showing essential text/actions remain available; no overlap/clipping | Not tested |
| A-03 | Light and dark theme text contrast | Measured foreground/background pairs for normal and large text, each interactive state; tool/method and values | Not measured |
| A-04 | Non-text controls, boundaries and focus indicator contrast | Measurements plus state screenshots; separate from text contrast | Not measured |
| A-05 | TalkBack labels, roles, headings, reading/focus order | Recorded screen-reader walkthrough for Today, recorder, archive, editor, Growth Mark, settings and confirmation | Not tested |
| A-06 | Non-colour state cues | Verify selected date, explicit zero, not recorded, error, selected/disabled and success states use text/shape/semantics beyond colour | Not tested |
| A-07 | Touch target dimensions and reachability | Measured dp bounds for primary and destructive controls; one-handed reach notes | Not measured |
| A-08 | Keyboard-open and long journal text | Screens/recording showing focused field, visible save/cancel actions, scrolling and preserved text | Not tested |
| A-09 | Empty, loading, validation, save failure and destructive confirmation | Each state reached or deliberately simulated; recovery path recorded | Not represented/tested |
| A-10 | Reduced motion and animations | Setting and screen behaviour; confirm no essential meaning relies on motion | Not tested |
| A-11 | Date/period semantics | Screen-reader announcement and visible label for selected date, month/year, totals and blank dates | Not tested |
| A-12 | Privacy and data-loss comprehension | Task evidence distinguishing readable export, portable backup/restore, OS transfer backup and each deletion scope | Not tested |

Use WCAG 2.2 AA contrast criteria as the documented measurement baseline where applicable, while also checking Android-native accessibility expectations and platform semantics. A numeric contrast pass alone does not establish readability or usability.

## 6. Issue triage

- **Blocker:** data loss, accidental destructive action, private-data exposure, misleading saved/missing state, inaccessible core task, or inability to recover.
- **High:** core task cannot be completed or understood without help; primary action is hidden/clipped; a key status is ambiguous.
- **Medium:** repeated backtracking, unclear labels/periods, recoverable layout friction, or secondary flow discoverability issue.
- **Low:** polish that does not obstruct comprehension, access, or task completion.

Severity is a review judgement, not a participant score. Record rationale and keep unresolved high-impact issues open.

## 7. Comparison and stop rules

- Compare N1/A2 against N2/A1/A3 only with equivalent tasks and equivalent synthetic data if the owner decides to revisit the selected directions.
- Do not reopen W1/N1/A2 or G2/G3 merely because evidence is pending; reconsider only if a material issue is observed.
- Stop a task if it risks real data loss, asks for personal disclosure, or causes distress. Use synthetic records and non-destructive previews.
- Do not call a screen accessible because it passes one device or one setting. Record coverage and limitations.
- A reviewer walkthrough can find likely issues but cannot substitute for representative user feedback where that is feasible.
- If only SVGs are available, mark device, TalkBack, contrast, and touch-target items “not testable from artefact”; do not mark them passed.

## 8. Exit criteria before visual/design approval

- Critical tasks have documented outcomes and unresolved blockers/high issues have owners and decisions.
- The owner has reviewed the evidence and explicitly approved or deferred the remaining design choices.
- Contrast, scaling, TalkBack, focus, touch targets, keyboard and non-colour status cues have evidence from actual rendered screens/builds.
- Empty, loading, error, draft, correction, delete-confirmation and cancellation paths have been reviewed.
- W1/N1/A2 and G2/G3 remain the current planning choices unless evidence justifies revisiting them.
- `docs/DESIGN-REVIEW-CHECKLIST.md`, `docs/28-REQUIREMENT-TRACEABILITY.md`, the decision log, and `docs/29-UX-DESIGN-APPROVAL-PACK.md` are updated with findings and their limits.
- Design approval and the implementation/planning gate remain separate explicit owner decisions.

## 9. Current status

**Prepared only. No task session, participant study, rendered-device inspection, contrast measurement, TalkBack review, touch-target measurement, build, automated test, or device test has been performed by creating this protocol.** All tasks and accessibility checks remain not run until separately authorised and evidenced. The planning gate remains closed.
