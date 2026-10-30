# UX and Design Approval Pack

**Review date:** 2026-10-30  
**Status:** Owner planning selections recorded through 2026-10-30: W1 Action-first Today, N1 four-destination navigation, A2 hybrid Journal archive, modified G2/G3 Growth Mark roles, D-205 synthesis visual identity, D-207 Soft Botanical palette direction (exact tokens deferred), D-206 comeback copy, platform/system typography and system light/dark baseline, and compact non-blocking comeback presentation. D-201 records Growth Mark duration/period defaults; D-204 records automatic time-of-day and fixed-theme modes with gradual automatic transitions. Exact theme boundaries, palettes/tokens, font selection and accessibility evidence remain open. The planning gate remains closed.

**Related documents:** [UX and Design](02-UX-AND-DESIGN.md), [Today Dashboard Concepts](21-TODAY-DASHBOARD-CONCEPTS.md), [Design System](../DESIGN.md), [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md), [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md), [Typography and Accessibility Research](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md), [Design Review Checklist](DESIGN-REVIEW-CHECKLIST.md), [Requirement Traceability](28-REQUIREMENT-TRACEABILITY.md).

## Owner-provided visual reference — 2026-10-28

[Open the multi-screen Dayseed UI reference layout](prototypes/dayseed-owner-reference-layout.webp). This owner-supplied collage shows five Today states/variants and supporting screens for Habit Create/Edit, Habit Detail, History calendar, Journal Entry, Insights Overview, return after a break, and Settings. Use it as the persistent visual north star for hierarchy and mood while comparing designs and reviewing accessibility. The image communicates a direction—not final approval of its exact controls, information architecture, colours, type, components, or every screen detail. Keep all unsettled choices open in the owner decision batch; DESIGN.md remains the only token/component authority. It does not authorise implementation or change the closed planning gate.

## Time-aware theme behaviour — owner response recorded 2026-10-30 (D-204)

The owner selected **both** appearance modes: Dayseed may follow automatic time-of-day themes, or the user may select one theme to keep throughout the day. A fixed selection takes precedence over automatic switching. The owner also selected **gradual transitions** between automatic daypart themes.

The four-part Morning / Afternoon / Evening / Night concept is a proposed design structure for further exploration, not approval of exact clock boundaries, colour palettes, theme names, transition duration, or final screens. Define boundaries using device-local time; consider reduced-motion settings and avoid transitions that obscure content or interfere with interaction. Each theme must independently meet contrast, text scaling, TalkBack and other accessibility requirements. A user-selected fixed theme should remain fixed until the user changes it; automatic switching should not silently overwrite that preference.

This decision records theme behaviour only. It does not settle D-008 broad visual identity, D-019 exact colour/type tokens, or final UI approval, and it does not open the planning gate or authorise implementation, builds, tests or device validation.

## 1. Current status

The product rules are far more settled than the UI choices. Approved baseline requirements include Today’s separate Complete and Record actions; the local journal and its save/edit/delete rules; the History and Insight jobs; 14-day and 30-day comeback triggers; nonjudgmental copy; offline title/body journal search; local backup/restore settings; and the accessibility expectations. Those policies must shape the design rather than being treated as undecided. Three rendered comparison boards are now available in `docs/prototypes/`; they are ready for owner review, but do not constitute accessibility validation or user-test evidence.

Still open are the broad visual identity (D-008), whether exact colour/type tokens (D-019) are fixed now or explicitly deferred, and final comeback copy/localisation (D-017). W1/N1/A2 and modified G2/G3 are selected planning directions. D-201 settles Growth Mark duration formatting, compatible-unit aggregation and period defaults; actual UI/aggregation and Journal findability/accessibility remain validation obligations, not undecided owner choices. The owner has selected W1 Today, N1 navigation and A2 archive as the starting directions; these are not usability-test results or final approved UI. The current Dashboard Concepts document includes a user-supplied screenshot discussion and a “little of Concept C” direction; use that as the design exploration anchor, not as approval of a finished screen.

## 2. Recommended starting direction

These are the recommended concepts illustrated by the comparison boards, not decisions silently made on the owner’s behalf.

### Visual direction

**Recommend testing a synthesis of Field Journal warmth + Living Editorial hierarchy + Quiet Instrument discipline for controls and data.**

- Keep a warm, quiet neutral canvas and a restrained accent palette as a hypothesis.
- Use strong editorial hierarchy for headings and short reflective moments, while keeping form controls, progress states and settings precise and consistent.
- Avoid generic repeated rounded cards, decorative gradients/glows, excess shadows, ornamental leaves without a defined data meaning, XP/badges, and celebratory motion for routine taps.
- Follow DESIGN.md as the only token/component authority. No candidate colour, font, or component becomes final until it is measured and owner-approved.
- The current colour research notes that at least one candidate muted text colour fails normal-text AA on the candidate canvas; contrast review must change the failing pairing, not merely record it.

### Information architecture and navigation

**Owner-selected starting option (N1):** four bottom-level destinations—**Today**, **History**, **Insights**, and **Settings**—with a very visible, lightweight **Write reflection** action on Today. History should let people switch clearly between habit history and the journal archive, and the Journal archive view should have an obvious “New entry” action. Review journal findability and accessibility before final UI approval; N2 is no longer the default starting direction but could be revisited if task review reveals a material issue.

Why test this first: it keeps the primary daily action, retrieval, summaries and data controls separate without overcrowding the bottom bar. The trade-off is that a journal-first user may not discover History’s journal area quickly enough. Compare it with a dedicated Journal tab variant using the same tasks; do not decide by aesthetics alone.

### Today screen

The habit list remains the practical centre of Today. Each habit should expose the correct action for its tracking mode and schedule. Keep Complete (usual target for numeric/duration, completion for completion-only) separate from Record (actual value, edit/correction, notes). Do not show numeric controls for completion-only habits or imply a journal entry completes a habit.

If a summary is included, use a precise denominator and period. For example, show how many habits are scheduled today and how many have a saved qualifying record; make “not recorded” visually neutral and do not call an active-day total a success percentage. Flexible weekly goals must have their own weekly progress summary.

### Growth Mark

**Owner-selected direction (O-6 clarified in D-199):** use a modified G2 card labelled **“Collected moments”** that shows the total duration recorded by duration-tracked habits, formatted naturally by magnitude (for example, “45 min this week” or “3 hr 20 min this week”). The period must be visible. Consider day, configured week, and calendar-month reporting periods; a month is a reporting period, not a fixed duration unit. G2 remains the compact summary. **G3 becomes the Calendar/date-level or separate detail-page treatment**, where users can inspect per-date totals and underlying duration sessions. G1 remains a simple active-date summary comparison, not the selected Growth Mark card.

Only duration-based habit records contribute to the duration total. Do not add quantity values, completion-only/effort-aware outcomes, journal entries, or incompatible units. Derive the total from saved records and recalculate after corrections/deletions; do not persist a duplicate aggregate without a separate schema decision. Format very large totals with day/week equivalents only when clear and meaningful; do not silently define a month as a fixed duration. Keep no-record, explicit-zero and failed/loading states distinct and nonjudgmental.

Do not add a growing plant as a score, arbitrary level, composite score, streak, or time-based decay. Finalise display thresholds, default period, date-detail interactions and accessibility through planning review; no final UI or implementation is approved.

### Journal archive and search

**Owner-selected A2 hybrid direction:** recent chronological entries grouped by date, a compact calendar/date-jump control, a readable entry detail/editor, and the approved local title/body search. Keep the search scope to saved title/body content; display recorded mood labels where present and exclude unsaved comeback drafts. Advanced date-range/habit filters are not implied.

A blank date is neutral. Calendar marks mean “an entry exists,” never “this day was good/bad” or “a habit was missed.” Compare this hybrid with timeline-only and calendar-first navigation using identical retrieval/edit/delete tasks.

### Comeback presentation and copy

The active v1 triggers are already approved: the two-week experience after 14 complete local calendar days without opening Dayseed; the broader 30-day experience when either app inactivity or no active habits qualifies; the 30-day experience takes precedence; each is shown at most once for that qualifying episode. Do not treat the older 3-day/90-day copy examples as triggers.

**Owner-accepted presentation direction:** a compact, dismissible, non-blocking welcome that never demands an explanation, never erases progress, and offers a small next step plus a valid continue/not-now path. The one-month choice flow retains its approved create/resume/continue options; optional reflection remains optional and draft-safe. Final copy/layout remains open, not the trigger rules.

## Owner response reconciliation — 2026-10-28

- **W1 Today:** selected; preserve separate Complete and Record actions for measured habits and keep journaling optional.
- **N1 navigation:** selected; check that Journal is findable from Today and History without ambiguous labels or icon-only routes.
- **A2 archive:** selected; validate date jump, search, edit, delete, empty dates and multiple entries on a date.
- **Growth Mark:** modified G2 “Collected moments” duration-total summary selected; G3 reserved for Calendar/date-level or separate-page details. D-201 records conservative formatting, aggregation and period defaults; actual UI and runtime behaviour remain unverified.
- **Typography/theme baseline:** platform typography and system light/dark appearance accepted; exact colours/fonts/tokens are still open in DESIGN.md.
- **Comeback:** compact, dismissible, non-blocking direction accepted; final copy/localisation/accessibility still needs review.
- **Ads:** D-200 defers ads for initial v1 and retains [the monetisation evaluation](33-MONETISATION-AND-ADS-EVALUATION.md) as pre-launch material only. No SDK, placement or release integration is approved for v1.

These responses do not approve a final UI system, usability/accessibility outcomes, implementation, or the planning gate.
## 3. Primary screens and states to prototype

| Screen / flow | Must show or do | High-value states to include |
|---|---|---|
| First launch | Plain local-storage explanation; single global reminder opt-in; create first habit or continue exploring without an account | Reminder opt-in Yes/No; no network; no sample data mistakenly presented as user records |
| Today | Scheduled habits, correct Complete/Record actions, optional Write reflection, clear separation of daily and weekly target progress | No habits; one/many habits; scheduled/unrecorded; partial; goal met; above target; explicit zero; long habit name; small screen |
| Create/edit habit | Tracking mode, target, DAILY/SELECTED_WEEKDAYS/FLEXIBLE_WEEKLY_TARGET schedule, preferred days as guidance only for flexible weekly, reminder time, Apply today/Apply tomorrow rules | Each mode; invalid mask; same-day change before/after activity; cancel; apply tomorrow; paused/archived habit |
| Record detail / correction | Actual amount or valid non-numeric outcome, existing entries, optional session note and habit/date note, explicit date for backdating | Repeat session; zero; invalid/negative value; retry; edit/delete one entry; write failure |
| Journal editor | Optional prompt, text, optional title and primary/secondary mood, explicit Save, date picker where supported | Unsaved draft; save failure; whitespace-only entry; multiple entries on one date; optional moods omitted |
| History + Journal archive | Habit dates/results/correction entry point; clearly discoverable journal list and date jump; local search; neutral empty dates | Empty archive; many entries per date; no entries on selected date; search empty/results; edit/delete/confirm/cancel |
| Entry detail | Full untruncated body, date and optional title/mood; Edit and permanent Delete actions | Long text; title/no title; confirmation cancel/delete |
| Insights | Transparent weekly/monthly measures; daily schedule results distinct from flexible weekly target; mood distributions from primary moods only | No data; sparse data; mixed moods; week/month boundaries; previous-period comparison hidden until chosen |
| Comeback | Approved trigger precedence, no guilt, no forced reflection, approved Resume/Create/Continue choices where available | 14-day only; 30-day precedence; no active habits; no eligible habit to resume; existing unsaved comeback draft |
| Settings → Reminders | Global opt-in, per-habit reminder settings, displayed time, manual vs system-managed timing clearly labelled, controls for frequency/quiet time when approved | Permission denied; global No; manual time protected; logged/dismissed; reminder scheduled late |
| Settings → Backup & Restore | Separate readable export, manual portable backup, optional Auto Backup, preview, Replace/Merge, conflict review, honest status | Destination unavailable; wrong password; corrupt/incompatible file; safety backup failure; unresolved conflict; provider may sync file |
| Settings → Privacy and data | Distinguish local storage, Android platform backup, portable backup, provider-backed destinations, delete-one/delete-habit/delete-all | Explanatory state before destructive action; failure; confirm/cancel; no unverified encryption promise |

## 4. Comparable design variants and test tasks

Keep the copy, data examples, typography scale and overall palette consistent while varying only the element being tested.

1. **Today hierarchy:** user’s current reference with a light reflective element (baseline); controlled alternative with fewer always-visible effort controls; controlled alternative where reflection has slightly more prominence.
2. **Journal navigation/archive:** four-destination navigation with a Journal area under History; dedicated Journal tab; archive as a calendar-first variant. Use the same data and tasks.
3. **Growth Mark:** validate the modified G2 “Collected moments” duration-total card and G3 date-level calendar/detail view. G1 remains a comparison option for a simple active-date summary. Do not use abstract plant growth or scoring as a comparison baseline.
4. **Typography and theme:** compare Roboto/system baseline against at least one suitable editorial candidate at identical size and copy; design a real light and dark palette before contrast approval.

Use the same task set across variants:
- Log a measured habit using Complete, then correct the amount to what actually happened.
- Find an older journal entry by phrase and by date; edit a typo and cancel a delete confirmation.
- Explain whether a blank calendar date means the user failed.
- Find the difference between today’s scheduled habit target and a flexible weekly target.
- Return after a qualifying absence and continue without writing a reflection.
- Find privacy, export, backup/restore, and delete controls.
- Complete each task with large text and keyboard visible.

Record task completion, wrong taps, backtracking, user explanation of metrics, ability to recover from mistakes, and accessibility problems. These results do not exist yet; do not write estimated usability scores as findings.

## 5. Accessibility and design review requirements

- Test compact and typical Android widths, portrait layout, keyboard-open screens, long entries, empty/error/destructive states, and the largest available text scaling (including up to 200% where supported).
- Check actual text contrast for every semantic pair in light and dark themes; also review non-text contrast and focus separately.
- Colour must never be the sole indicator. Provide talkback-readable names for effort, schedule, progress, zero, unrecorded and derived-missed states.
- Check focus order, control role/name, touch targets, one-handed reachability, reduced motion, text wrapping and no clipped actions.
- Font candidate notes must include source, version, licensing, language coverage and bundle-size consequences before any font is bundled.
- Preserve the approved semantic rules: “not recorded” differs from explicit zero; rest/non-scheduled days are neutral; paused days are excluded; missed is derived only after the scheduled local day passes; no explicit habit-day Skip action exists in v1.

## 5.1 Static heuristic review of the three comparison boards — 2026-10-29

**Review type and limits:** this is a source-content/heuristic review of the three SVG planning boards against the task/accessibility scorecard. It is not an observed user task study, rendered-device inspection, measured contrast audit, TalkBack test, touch-target measurement, or approval of the designs. No task-completion times, wrong-tap rates, usability scores, or accessibility pass claims are inferred.

### Findings requiring correction before the next owner review

1. **Corrected: Today Growth Mark now matches D-199 at the content level.** The follow-up static source review confirms W1, W2 and W3 all show “Collected moments · October,” the same synthetic 3 hr 40 min monthly total, and the scope “Duration-tracked habits only.” The obsolete active-date measure and candidate-metric labels are gone. This verifies text consistency only; it does not prove the aggregation is implemented or accurate.
2. **Today content is now more comparable, but the comparison remains static.** The three variants retain the same visible habit values, October reporting period and 3 hr 40 min duration total; only card placement/wording density follows the hierarchy concept. The SVG does not demonstrate underlying aggregation, zero-versus-no-record edge states, actual G3 navigation or rendering at device text scales. Verify those during the later authorised task/accessibility review.
3. **Journal and navigation comprehension remains a task question, not a finding.** N1/A2 has clear text labels for Today, History, Insights and Settings and shows Journal/search/date-jump inside History. That supports the chosen direction, but the board itself cannot prove users will find Journal, distinguish saved entries from drafts, or locate privacy/export/backup/delete controls. Keep these as explicit owner/task-review checks.
4. **A2 calendar density and date semantics need a closer pass.** The hybrid view combines search, date jump/calendar, and recent entries. Verify the calendar’s month-boundary dates are visually distinguished where needed, selected-date state is explicit without colour alone, multiple entries on one date remain discoverable, and blank dates do not imply failure. No user comprehension result is available yet.
5. **Large text, keyboard, focus and touch-target behaviour are unverified across all boards.** The SVGs are fixed-size mockups and cannot demonstrate 200% text scaling, keyboard-open layout, TalkBack names/order, 48 dp target sizing, one-handed reachability, reduced-motion behaviour, or long-text/empty/error/destructive states. These remain mandatory checks; do not mark them passed from the drawings.
6. **Contrast and typography are not ready for approval.** Candidate colours and font directions remain exploratory. The research notes a candidate muted text pairing that fails normal-text AA; do not carry that pairing forward. Measure exact foreground/background pairs in light and dark themes, and separately inspect non-text boundaries/focus and all interaction states. Compare the system typography baseline with candidates using identical copy and size; do not bundle a custom font without source/version/licence/language coverage and size review.
7. **Comeback emotional safety is directionally supported but not fully reviewed.** The compact, dismissible, non-blocking direction is consistent with the decisions. The actual 3-/14-/30-/90-day edge cases, no-eligible-habit state, unsaved draft reuse, “Not now”/continue path, localisation, and enlarged-text layout still need scenario review.

### Safe deferrals and next review scope

- Keep the selected W1/N1/A2 and modified G2/G3 roles as the current planning directions; do not re-open them solely because evidence is still pending. Revisit a selected direction only if task/accessibility review exposes a material problem.
- Defer final visual identity, exact colour/type/spacing tokens, custom-font selection, Growth Mark display thresholds and alternate-period controls until comparable samples and required accessibility evidence are available.
- The W1/W2 Growth Mark inconsistency has been corrected and the three Today variants now show a consistent synthetic duration summary. Before owner approval, conduct the task review listed above and record observed results separately from static heuristics. The planning gate remains closed.

### Follow-up static consistency check — 2026-10-29

After the SVG correction, the source text was re-fetched and checked: W1/W2/W3 each display the same October reporting period and 3 hr 40 min synthetic duration total, explicitly limited to duration-tracked habits. The terminology was tightened from “duration habits” to “duration-tracked habits” for precision. This was a source-text/coordinate review only, not a rendered-device readability inspection or accessibility/usability test.

## 5.2 Static navigation and archive board review — 2026-10-29

**Review type and limits:** static SVG source-content and coordinate review only. The board cannot prove task completion, touch accuracy, accessibility, rendered readability, or actual navigation. No user study or device test is claimed.

### Findings

1. **N1 creation entry point was underspecified in the drawing.** History showed a Journal switch and recent entries, but no explicit New entry action in that panel. The SVG now shows a labelled “New entry +” control alongside Journal. This is a prototype clarity fix, not approval of final placement or behaviour.
2. **A2/A3 calendar labels overstated the displayed date range.** The grids show a late-October/early-November window, not a complete October month, while their headings previously said “October 2026.” The headings now explicitly identify a partial late-October/early-November view.
3. **A3 selected-date highlight contradicted its own selected-date label.** The highlight was on the cell for 3 November while the detail caption said “Tue 27 Oct.” The highlight is now moved to the 27 October cell to match the shown entry/date.
4. **Edit/delete and safety flows are not depicted.** The board shows retrieval entry points but not the entry detail/editor, saved-versus-unsaved distinction, edit-save/cancel, explicit permanent-delete confirmation/cancellation, or returning to the prior archive position. These remain mandatory task-review scenarios; do not infer they are solved by the current mockups.
5. **Privacy and data portability remain discoverability questions.** Settings is visible in N1/N2 navigation, but the board does not show the Settings screen or demonstrate that privacy, readable export, portable backup/restore, and deletion controls can be found. Keep those in the owner task review.
6. **The synthetic examples are incomplete by design.** A2/A3 show a partial date window, and the visible sample only shows one entry per date. Test month boundaries, multiple entries on one date, empty search results, long text, blank dates, and an entry with no title/only body using identical tasks across variants.
7. **N1 remains the selected starting direction, not a proven winner.** The new entry affordance improves the static flow, but the board cannot establish whether users find Journal within History faster than a dedicated Journal tab. Keep N1 as the current planning direction unless task evidence reveals a material issue; do not treat N2 as owner-selected.

### Safe deferrals and next review scope

- Retain owner-selected N1/A2 as the current planning directions. Do not reopen them based solely on static-review gaps.
- The source drawing now clarifies the N1 New entry affordance, the partial calendar date window, and the A3 selected date. No app code or design-token authority changed.
- Before owner approval, perform the same find/write/retrieve/edit/delete tasks against comparable variants. Include multiple entries per date, month boundaries, blank dates, search no-results, saved-entry versus draft distinction, cancel/confirm destructive actions, return-to-archive position, large text, keyboard, TalkBack/focus, and findability of privacy/export/backup/restore/delete settings. Record observed outcomes separately from static findings. The planning gate remains closed.

## 5.3 Static Growth Mark and visual-reference alignment review — 2026-10-29

**Review type and limits:** compared the Growth Mark SVG's source content and sample arithmetic with D-199, the Growth Mark research, and the owner-provided reference layout at `docs/prototypes/dayseed-owner-reference-layout.webp`. This is a planning-level visual/content review only. It does not establish rendered-device readability, usability, measured contrast, or accessibility.

### Findings

1. **G2 and G3 are directionally aligned with the owner reference and the later decision.** The reference uses a warm cream canvas, restrained sage/olive accents, editorial headings, rounded restrained surfaces, simple botanical identity, and clear mobile hierarchy. The Growth Mark comparison uses the warm neutral/sage palette, editorial title, rounded panels, and a deliberately restrained presentation. It is a comparison board rather than a finished phone screen, so it does not need to reproduce the full reference layout or add decorative plant artwork at this stage.
2. **The owner-selected metric overrides the older reference's count example.** The reference's Habit Detail example shows “12 collected moments,” but D-199 explicitly modifies G2 to show a readable duration total from compatible saved duration sessions. Do not revert to a count of active dates or collected actions just to match the older image. Botanical decoration is not required; any future plant change must communicate a defined, explainable event rather than become a score.
3. **The G3 sample totals did not match G2.** The original six displayed values summed to 4 hr 40 min, while G2 showed 3 hr 40 min for October. The SVG now changes the 14th from 45m to 25m and the 21st from 1h 10m to 30m. The six values now sum to 220 minutes (3 hr 40 min), matching the synthetic G2 month total.
4. **The G2 destination label was vague.** “Progress →” did not explain what would open. It now reads “See time by date →”, consistent with the selected G3 calendar/date-level detail role.
5. **Visual-system approval is still open.** The board's palette is a directional match, not a measured or approved token set. D-008's broader visual direction, exact colours/type/surfaces, contrast in both themes, large-text behaviour, accessible date labels, zero-versus-no-record presentation, and task-based use remain open. D-201's current-month default, month navigation, compatible aggregation and duration-formatting rules are settled planning defaults; separate day/week selectors and month-equivalent duration displays are deferred for initial v1.

### Safe deferrals and next review scope

- Keep the modified G2 compact duration summary and G3 date/session detail as the selected planning direction; G1 remains an alternative only.
- Use the owner reference now as a hierarchy/mood guardrail, but defer pixel-level matching, final design tokens, and detailed botanical styling until the visual direction and accessibility review. Do not block this content-level review on final colour or font choices.
- Before final owner approval, verify period and unit comprehension; current-calendar-month default; compatible-unit totals; source-session inspection; corrections/deletions; explicit zero versus no records; empty/loading/error states; and large text/TalkBack/focus order. Record task observations separately.
- The arithmetic and wording fixes are static prototype corrections only. No app code, design tokens, implementation, build, test, or device validation was performed; the planning gate remains closed.

## 6. Owner decision batch

The owner can approve a recommendation, request a variation, or explicitly defer any of these without blocking core features that do not depend on it. The table below records current status: owner-selected planning directions are not runtime-validated UI, and open visual details remain separate from already-recorded choices.

| Decision | Current status | Recommendation to review | What makes it ready to approve |
|---|---|---|---|
| D-008 visual direction | Open; three SVG comparison boards prepared | Prototype Field Journal warmth + Living Editorial hierarchy + Quiet Instrument controls | Review the Today, navigation/archive, and Growth Mark boards at small/typical widths and enlarged text; choose or defer the direction. Exact colour pairs, contrast, and accessibility still need review before final design approval |
| Navigation / Journal placement | N1 selected: Today / History / Insights / Settings, with Journal accessible from Today and within History; usability remains unvalidated | Validate find/write/retrieve/edit tasks against the selected N1 direction; a dedicated Journal tab is not the current selection | Task-based evidence, clear Journal entry points, and no hidden core flow |
| D-016 / D-199 / D-201 Growth Mark | Modified G2 duration-summary direction and G3 date/session detail role selected; D-201 records compatible-unit aggregation and period/formatting defaults | Validate accuracy, readable presentation, no-data/explicit-zero/correction states, and the path to G3; separate day/week selectors and month-equivalent duration display are deferred for initial v1 | Totals reconcile to source sessions, period and units are clear, records are inspectable, and later authorised accessibility/task review finds no misleading presentation |
| D-017 comeback presentation | Trigger policy approved; presentation open | Compact, non-blocking, dismissible welcome using approved 14-/30-day rules | Copy/layout review; proves no mandatory reflection or duplicate episode |
| D-018 Journal archive | A2 hybrid timeline plus compact calendar/date jump and local search selected; retrieval/accessibility validation remains open | Validate retrieval, edit, delete, search and date-jump tasks using the selected A2 direction | Retrieval is understandable, blank dates remain neutral, and large-text/empty/error states work in later authorised validation |
| D-019 typography/colour | Open | System typography baseline vs a carefully selected editorial option; restrained semantic palette | License/source confirmed; every actual text/interactive pair passes review |
| Theme preference | Open | Follow system by default; consider an optional System/Light/Dark override if usability/accessibility benefits justify the extra setting | A complete light/dark palette and prototype review; no mechanically inverted palette |
| Minimum Android support/device matrix | Product target Android; exact minimum remains unverified | Decide from source/toolchain and intended device needs during the authorised technical-validation stage, not from a guessed number today | Supported API/build evidence and a named test-device matrix; this does not need to delay visual exploration |
| Retrospective journal prompt (D-042) | Optional prompt direction approved; cadence delegated under D-196 | Use the conservative default in docs 24/26/28: on the first eligible app opening of a local calendar day, offer at most one prompt for the most recent fully completed local date only if that date has no saved journal entry; allow “Not now”; suppress repeats after display/dismissal/save; no backlog or OS notification; comeback takes precedence. The editor shows the explicit date and saving remains a normal journal save. | Routine cadence is not a separate approval blocker; the owner may override or defer it in the grouped review. Test app open after 1+ days away, multiple entries/date, dismissal/save suppression, and no effect on habit status |

## 8. Low-fidelity wireframe sketches — planning only

**Rendered planning artefact:** [Today screen comparison — W1 Action-first, W2 Reflective, W3 Compact controls](prototypes/dayseed-today-variants.svg). This SVG compares the same synthetic habit data across three hierarchy variants. W1 is the selected planning direction; W2/W3 remain comparison alternatives. It is a planning prototype only, not validated application UI or usability evidence, and the sample data are not real user records.

**Rendered navigation/archive planning artefact:** [N1/N2 navigation and A1/A2/A3 archive comparison](prototypes/dayseed-navigation-archive-variants.svg). This board compares four versus five bottom destinations and timeline-first, hybrid, and calendar-first archive layouts using matching synthetic entries. N1 four-destination navigation and A2 hybrid archive are selected planning directions; task/accessibility validation remains outstanding. The board is not approved application UI.

**Rendered Growth Mark comparison:** [G1 plain active-date summary, G2 “Collected moments” duration total, and G3 calendar/date-level detail](prototypes/dayseed-growth-mark-variants.svg). The board uses synthetic duration examples. The owner selected the modified G2/G3 roles in D-199; it remains a planning artefact, not approved application UI or accessibility/usability evidence. D-201 records the unit-formatting and period defaults; the remaining review is whether the presentation is understandable and accessible and whether detail interactions, corrections, deletion, explicit zero and empty states behave as specified.


The linked SVG boards are rendered comparison prototypes with consistent synthetic data across variants. They are planning artefacts, not usability findings, approved final layout decisions, or application UI. W1/N1/A2 and modified G2/G3 are already selected planning directions; the remaining step is a task-based review with large text, empty/error/destructive states and accessibility checks. The owner can then confirm, modify, or defer a direction based on evidence; initial selection is not pending.

### Today layout variants

**Shared frame:** Android portrait; top app bar with date/context; local data only; no decorative score; daily and weekly targets remain separate; one consistent bottom navigation during each comparison; sample entries must be clearly identified as synthetic.

**W1 — Action-first**

```text
┌────────────────────────────┐
│ Today                [⋯]  │
│ Tue, 27 October             │
│ 2 of 4 scheduled habits     │
│────────────────────────────│
│ TODAY                       │
│ Walk [Complete] [Record] │
│   0 / 20 minutes            │
│ Read [Complete] [Record] │
│   10 / 10 pages · Goal met  │
│ Stretch          [Complete]│
│ Water [Complete] [Record] │
│────────────────────────────│
│ Write a reflection     [＋] │
│ Collected moments · 3h 40m this month → │
│────────────────────────────│
│ Today  History  Insights  ⚙ │
└────────────────────────────┘
```

Purpose: make today's useful habit action first. The denominator must be the count of habits actually scheduled today; numeric values and outcomes come from saved records, and “0” appears only for explicit zero. The compact G2 row reads “Collected moments · 3h 40m this month”; tapping it opens G3 calendar/date-level detail so the user can inspect saved duration sessions. Record opens the correct mode-specific recorder and the reflection action starts a separate Journal entry. Empty state: invite creation or exploration without fake records or fabricated duration. Accessibility: the card and its drill-down have clear TalkBack names and status text beyond colour.

**W2 — Reflective Today (controlled variation)**

```text
┌────────────────────────────┐
│ Today                [⋯]  │
│ Tue, 27 October             │
│ A small note for today  [＋] │
│────────────────────────────│
│ TODAY'S HABITS              │
│ Walk [Complete] [Record] │
│   0 / 20 minutes            │
│ Read [Complete] [Record] │
│   10 / 10 pages · Goal met  │
│ Stretch          [Complete]│
│────────────────────────────│
│ Your collected moments      │
│ 3 hr 40 min recorded this month │
│ [View the dates behind this]│
│────────────────────────────│
│ Today  History  Insights  ⚙ │
└────────────────────────────┘
```

Purpose: test the user's stated interest in a slightly more reflective Today screen without making writing compulsory. The example now uses the owner-selected G2 duration-total direction: only saved duration sessions contribute, and an explicit zero is a saved record but contributes no time. Tapping the duration summary opens G3 for date-level totals and source-session inspection. Compare whether the reflection invitation crowds out the main habit action, especially with large text and several habits.

**W3 — Compact effort controls (controlled variation)**

```text
┌────────────────────────────┐
│ Today                [⋯]  │
│ Tue, 27 October             │
│ TODAY'S HABITS              │
│ Walk [Complete] [Record] │
│   0 / 20 minutes            │
│ Read [Complete] [Record] │
│   10 / 10 pages · Goal met  │
│ Stretch          [Complete]│
│ Measured habits keep both actions│
│────────────────────────────│
│ Write a reflection     [＋] │
│ See history and insights    │
│────────────────────────────│
│ Today  History  Insights  ⚙ │
└────────────────────────────┘
```

Purpose: test lower visual density without removing the approved Complete action (usual target for numeric/duration, completion for completion-only) or the separate detailed Record action for measured habits. Completion-only habits show Complete only. Keep the compact controls visible and test tap-count, mistaken taps, correction success, and how easily users see the saved result. No third Tiny/Usual/Extra action is assumed by this variant.

### Navigation variants

**N1 — Four primary destinations (recommended to prototype first):** Today · History · Insights · Settings. Put “Write reflection” on Today and an explicit Journal section/date-jump within History. Test findability; Journal must not be buried behind ambiguous iconography.

**N2 — Dedicated Journal destination:** Today · Journal · History · Insights · Settings. Compare whether direct Journal access helps journal-first users enough to justify a five-item bottom bar on small screens. Do not shrink labels or remove accessibility names to fit.

For both variants, Settings must expose privacy/export/backup/delete controls under clear text labels. From any journal entry, users should be able to return to the prior archive position.

### Archive variants

- **A1 — Timeline first:** recent saved entries grouped by date; “Choose date” opens a calendar/date jump.
- **A2 — Hybrid (recommended first prototype):** timeline first plus a compact calendar/date jump and local title/body search.
- **A3 — Calendar first:** calendar of dates containing saved entries; selecting a date opens that date's full entry list.

Use identical synthetic entries and tasks for all three. Empty dates remain neutral. Search excludes unresolved comeback drafts; all date/month labels use the stored local-date key. Test retrieval, editing, deletion confirmation/cancellation, multiple entries on one date, long text, large fonts, and empty results.

### Prototype review scorecard

For each variant, record observed results rather than guesses:

| Task | Evidence to record |
|---|---|
| Log a quantity/duration or completion-only habit | completion; seconds/taps if practical; error/correction path |
| Find an older Journal entry by date and phrase | success; wrong taps; backtracking; correct distinction between saved entry and draft |
| Explain a blank date and explicit zero | user's explanation in their own words |
| Distinguish daily targets from a flexible weekly target | correct explanation; any misleading label/denominator |
| Continue after a comeback prompt without reflecting | whether continue/not-now is found without pressure |
| Find privacy, export, backup/restore, and deletion settings | completion and any ambiguity |
| Use the primary tasks with large text and keyboard visible | clipping, lost actions, focus order, scrolling and recovery issues |

**Exit rule:** a wireframe is not approved because it looks cleaner. W1/N1/A2 and modified G2/G3 are the current selected planning directions, but final UI approval requires comparable content/tasks, review of major empty/error/destructive states, logged accessibility concerns, and an explicit owner decision to confirm, modify, or defer based on evidence. No usability score or observed finding is claimed yet.

## 7. Approval and implementation boundary

This pack is a planning deliverable only. Until the owner approves the planning gate:

- no app UI/code changes;
- no database schema changes or migration;
- no Gradle build/test, APK install, or physical-device validation;
- no visual tokens or layouts may be marked approved merely because a proposal exists.

When the owner settles a consequential design decision, record it in the decision log, update the UX plan and DESIGN.md as applicable, align the dashboard concepts/research docs, update the screen/data contracts and traceability matrix, and re-fetch every changed file for verification.


## 5.4 Cross-board visual-reference alignment review — 2026-10-29

**Reference used:** `docs/prototypes/dayseed-owner-reference-layout.webp`, the owner-provided multi-screen collage. This review compares the reference with the Today, navigation/archive, and Growth Mark SVG planning boards. It is a static source/layout-direction review only; it is not a rendered-device inspection, contrast measurement, accessibility test, or usability study.

### Findings

1. **The boards are moving toward the reference rather than away from it.** The reference's warm paper-like canvas, restrained sage/olive accents, dark readable text, editorial serif screen headings, restrained rounded surfaces, and small seed/botanical identity recur in the Today board's palette and seed mark, and in the navigation/archive and Growth Mark boards' typography, surfaces, and accents. This is sufficient alignment for planning comparisons; exact pixel matching is not needed yet.
2. **Keep the reference as a mood and hierarchy guardrail, not a specification to copy literally.** Its collage includes older screen examples and controls. Later owner decisions and written contracts take precedence where they differ, including D-199's duration-based “Collected moments” summary rather than the reference's older “12 collected moments” count example. The Growth Mark board need not add decorative plant art simply to resemble the reference.
3. **No additional source-level visual correction is justified in this pass.** The three boards already share a restrained palette and editorial direction. Forcing exact colours, fonts, spacing, botanical decoration, or individual control shapes now would prematurely imply that D-008 and the token set are approved. Keep those choices open and use `DESIGN.md` as the only token/component authority.
4. **The reference cannot prove accessibility.** Its small text in a multi-screen collage is not evidence that final phone UI should use the same text sizes. Actual compact-screen layouts, 200% text scaling where supported, contrast in light and dark themes, non-colour status cues, focus order, TalkBack names, touch targets, keyboard-open behaviour, and long/empty/error/destructive states remain unverified.
5. **Important reference screens remain task-review targets, not completed design approvals.** Habit create/edit/detail, journal entry editing, insights, comeback return, and settings/privacy/export/backup/restore/delete need to be checked against their documented requirements when their flows are reviewed. Do not infer that a screen works or is discoverable merely because it appears in the collage.

### Safe next step

- Keep the owner reference visible alongside each prototype review.
- Continue to compare hierarchy, tone, and information density, but defer pixel-level matching and final tokens until the owner reviews comparable samples and the required accessibility evidence is available.
- Retain the current W1/N1/A2 and modified G2/G3 planning directions. Revisit them only if task or accessibility evidence identifies a material problem.
- Record observed task results separately from static findings. The planning gate remains closed; no implementation, build, automated test, device test, or accessibility pass is claimed.


## 5.5 Static small-screen, state-clarity and accessibility-risk review — 2026-10-29

**Scope:** reviewed the three current SVG boards against the owner reference, screen/data contracts, `DESIGN.md`, typography/accessibility requirements, and the design checklist. This is a static source review. The SVG canvases are comparison sheets, not actual device-rendered Compose screens; their font sizes, SVG coordinates and drawn touch areas cannot be treated as Android sp measurements or dp touch-target evidence.

### Findings

1. **Today — zero versus no record is shown in the sample, but state coverage is incomplete.** The selected W1 direction includes an explicit recorded zero (for example, `0 / 20 min · recorded`) and a separate `Not recorded` example, consistent with the product rule. The board does not show all combinations of loading, save failure, validation error, no habits, long names, several habits, or text scaling. Keep the semantic distinction, and do not treat the drawing as evidence that the final UI preserves it accessibly.
2. **Growth Mark — the headline and detail arithmetic align, but G3 remains a concept, not a validated interaction.** The six synthetic date totals sum to 220 minutes (3 hr 40 min), matching G2. The small per-date labels in the SVG are illustrative only. No explicit zero-session versus no-session date, empty month, loading/error state, selected-date accessibility name, source-session editor, or correction/deletion flow is shown. The underlying screen contract requires source sessions to be inspectable and totals to recalculate after corrections/deletions.
3. **Journal archive — retrieval and creation are visible; recovery and empty states are not.** N1 now has a visible New entry action and the boards show search/date navigation, but they do not demonstrate no-results messaging, a body-only entry, multiple entries on one date, saved-versus-draft distinction, edit/save/cancel, permanent-delete confirmation/cancel, or restoration of the prior archive position. These remain open task scenarios, not presumed defects in an unimplemented UI.
4. **Settings/privacy/data portability are not represented at screen level.** The navigation board shows Settings as a destination, but not its contents. A task review must establish whether users can find privacy/data explanations, readable export, portable backup/restore, and distinct deletion controls. The Android platform Auto Backup/device-transfer exclusion must be explained separately from Dayseed's user-controlled backup; no screen or technical enforcement is validated here.
5. **Small-screen and accessibility status remains unknown, not failed or passed.** These wide SVG boards do not prove compact-phone fit, one-handed reach, 48 dp targets, keyboard-open behaviour, large-text wrapping, focus order, TalkBack semantics, reduced-motion behaviour, or contrast in both themes. Some supporting SVG labels are intentionally small because they annotate a comparison board; do not copy those annotation sizes into the actual app UI. Essential screen content and actions must remain readable at the largest supported font setting, with accessible non-colour cues.

### Review disposition

- **No further prototype edits are justified from source inspection alone.** Adding invented empty/error screens or changing tokens without comparable owner review would risk turning unresolved design choices into implied approvals. The existing boards are adequate for comparing high-level direction; they are not adequate evidence to approve detailed interactions or accessibility.
- Keep W1/N1/A2 and modified G2/G3 as the selected planning directions, use the owner reference as a visual guardrail, and record exact tokens/spacing/typography as an unresolved owner planning choice. Do not imply that rendered accessibility or the binary reference image has been inspected.
- Before final design approval, review matching tasks across the comparable options: log a measured value including explicit zero; distinguish zero from no record; find/write/retrieve/edit/delete a journal entry; search with no results; inspect a Growth Mark date and its source sessions; correct/delete a session and verify the expected summary; find privacy/export/backup/restore/delete settings; return from a comeback screen without writing.
- Record actual observations separately. The next authorised review should include compact and typical phone widths, largest supported text size, keyboard open, TalkBack/focus order, touch-target measurements, contrast measurements in light/dark themes, and empty/loading/error/destructive states.
- The design checklist remains unchecked for these validations. No user study, rendered-device inspection, measured contrast audit, TalkBack test, build, app test, or device test is claimed. The planning gate remains closed.


### Prepared task/accessibility review protocol

A reusable, planning-only script and evidence template is now available in [`docs/34-UX-TASK-AND-ACCESSIBILITY-REVIEW-PROTOCOL.md`](34-UX-TASK-AND-ACCESSIBILITY-REVIEW-PROTOCOL.md). It defines a shared synthetic fixture, neutral task wording, observation fields, accessibility evidence matrix, severity triage and exit criteria. The protocol is prepared, not executed; no task outcomes or accessibility passes are claimed. The design checklist remains open and the planning gate remains closed.

## 5.6 Growth Mark semantic reconciliation — 2026-10-29 (D-201)

D-201 records conservative planning defaults to reduce ambiguity without claiming the UI has been implemented or validated:

- Normalize compatible duration units before aggregation; preserve source precision and round only for display.
- Display a useful whole unit plus remainder using minutes, hours, 24-hour days and 7-day weeks; do not display month-equivalent duration totals in initial v1.
- Keep G2's default period at the current calendar month. G3 uses calendar-month navigation and date/session drill-down; independent day/week period selectors are deferred unless the owner reopens scope.
- Keep explicit zero distinct from no record, exclude incompatible habit/journal types, and recalculate totals after source-record correction or deletion.

These defaults are documented in the screen/data contract (doc 26), acceptance criterion AC-025 (doc 28), decision log D-201 (doc 09), and planning readiness report (doc 35). They are planning defaults only. No rendered-device, aggregation, accessibility, correction, or deletion test has been performed. The earlier static-review findings in section 5.5 remain valid for what the SVGs do not demonstrate.

## 5.7 Owner direction decisions — 2026-10-30 (D-205–D-207)

The owner selected the following planning directions for reconciliation:

- **Visual identity (D-205 / D-008):** choose the synthesis — Field Journal warmth and overall identity, Living Editorial typography and hierarchy, and Quiet Instrument discipline for controls and data presentation. The owner reference remains a guardrail, not blanket approval of detailed UI.
- **Palette (D-207 / D-019):** choose Soft Botanical: warm ivory, muted olive and earthy accents. Exact colour values, font selections and token approval are deferred. Any palette values elsewhere in this pack remain exploratory.
- **Two-week comeback copy (D-206 / D-017):** “Good to see you again. Your progress is still here. Take today at your own pace.”
- **One-month comeback copy (D-206 / D-017):** “Welcome back! 🌿 What's new with you? You can pick up an old habit, start something new, or simply continue into Dayseed.”
- No modifications were requested.

These choices settle broad direction and candidate wording only. They do not approve final screens, component details, exact tokens, font packaging, measured contrast, localization, TalkBack, text scaling, reduced-motion behaviour or rendered-device usability. The design review remains incomplete and the planning gate remains closed. No implementation or tests are authorised.

## 5.8 Consolidated static review of Today, navigation and Journal archive — 2026-10-30

**Scope and evidence boundary:** Re-fetched and reviewed the current source text of the three planning SVG boards against the shared synthetic fixture and task/accessibility protocol in doc 34, the selected W1/N1/A2 directions, D-199/D-201 Growth Mark rules, and written Journal/archive requirements. This is a source-level heuristic review only. No rendered Android screen, participant session, actual task completion, contrast measurement, TalkBack walkthrough, touch-target measurement, build, or device test was performed.

### Task 1 — Today board and Growth Mark correction

**Disposition: already corrected in the current board; verified, no further SVG edit required.**

- W1, W2 and W3 all display “Collected moments · October” and the same synthetic **3 hr 40 min** total, with scope explicitly limited to **duration-tracked habits only**.
- W1 and W2 state “3 hr 40 min recorded this month”; W3 includes the same total and scope in its compact line. The reporting period is visible and consistent.
- The board retains the semantic distinction between Walk's explicitly recorded zero (“0 / 20 min · recorded”) and a separate “Not recorded” example. Measured-habit Complete and Record actions remain distinct; journaling is optional and does not change habit progress.
- The 3 hr 40 min total is consistent with the six synthetic date totals in G3 (220 minutes). The Growth Mark board states that quantity/completion records and journal entries do not contribute and that explicit zero remains a record but adds no time.

**Remaining limits:** the SVG does not prove aggregation, correction/deletion recalculation, actual G3 navigation, empty/error states, or readable presentation at device text scales. Do not reinterpret the example as an active-date count.

### Task 2 — Cross-board task/accessibility scorecard review

The three SVGs provide comparable high-level concepts and shared synthetic content, but they do not contain enough detail to score actual task success or accessibility. Protocol T-01–T-16 and A-01–A-12 remain planned checks, not completed tests.

| Area | Static evidence present | Gap / next evidence required | Disposition |
|---|---|---|---|
| Today logging (T-01/T-02) | Recorded zero and “Not recorded” are separately labelled; Complete and Record are separate for measured habits | No save/failure/validation feedback, no-habits state, long-name/many-habit layout, or enlarged-text rendering | Semantics represented; interaction/accessibility unverified |
| Reflection (T-03/T-15) | Write/reflection entry point is visible and optional | No editor, draft/save/cancel, comeback continue-without-writing flow | Entry point only |
| Growth Mark (T-10–T-12) | G2 period and duration scope are stated; G3 shows six date totals matching 220 minutes | No selected-date source-session list, zero-session versus no-session state, empty month, correction/delete flow or accessible date announcement | Metric concept consistent; detail interaction unverified |
| Journal creation/search (T-03/T-04/T-09) | N1 has a labelled Journal route, New entry action and saved-entry search; N2 provides the dedicated-Journal comparator | No body-only result, saved-versus-unsaved draft distinction, empty/no-results state, or long-result behaviour | Entry points represented; retrieval edge cases absent |
| Journal date retrieval (T-05) | A1/A2/A3 compare timeline, hybrid date jump and calendar-first navigation; A2 keeps timeline and date navigation together | Board sample does not visibly demonstrate two entries on one date or the complete cross-month retrieval fixture | Direction comparable; fixture coverage incomplete |
| Journal edit/delete (T-06–T-08) | Review prompt calls out safe edit/delete tasks | No editor, unsaved-change cancellation, permanent-delete confirmation/cancel or return-position flow is drawn | Not represented |
| Settings/privacy (T-13/T-14/T-16) | Settings destination is labelled in primary navigation | Settings content and separate privacy/export/portable backup/restore/deletion destinations are absent | Not represented |
| Accessibility (A-01–A-12) | Boards use text labels and do not rely solely on icons for primary navigation | No device viewport, 200% text, contrast measurements, TalkBack/focus order, dp targets, keyboard-open, reduced-motion, or failure-state evidence | Not testable from SVG |

### Task 3 — Journal archive findability and retrieval review

**N1/A2 remains the selected planning direction; no evidence currently justifies reopening it.** The board gives N1 a visible History destination, a text-labelled Journal switch, a labelled New entry action, date selection and search for saved entries. A2 places compact date navigation alongside a chronological entry list, balancing phrase search, date-based retrieval and browsing. N2/A1/A3 remain comparators, not owner-selected directions.

The following are open validation scenarios, not proven defects:

1. **Findability:** determine whether people understand that Journal is inside History, and whether the Journal switch is sufficiently prominent. Verify that the Today reflection entry point and History/Journal route do not appear to be two different kinds of journal.
2. **Search scope:** verify search covers saved title and body text, including body-only entries, and never returns an unsaved comeback/reflection draft as a saved entry. The current board does not show this distinction or a no-results state.
3. **Multiple entries/date:** the shared fixture requires two saved entries on one date, but the board's visible sample does not demonstrate both. Confirm both remain independently visible and retrievable after selecting that date.
4. **Date boundaries and blank dates:** test the last days of one month and first days of the next; show month/year context, distinguish adjacent-month dates where needed, and keep blank dates neutral. The board labels its sample as a partial late-October/early-November view; preserve that precision.
5. **Edit and destructive recovery:** verify edit/save, cancel/back with unchanged content, permanent-delete scope, explicit confirmation, cancellation, and return to a predictable archive position. None is demonstrated by the static board.
6. **Content/layout states:** test body-only and long entries, empty archive, empty search, keyboard-open search/editor, large text, TalkBack headings/controls, and non-colour selected-date cues.
7. **Settings discoverability:** separately test privacy/data explanation, readable export, portable backup/restore, and distinct entry/habit/delete-all actions. A Settings tab alone does not demonstrate these controls can be found or understood.

### Consolidated disposition and next evidence

- Task 1 is closed as a **source-content correction/verification**: the current Today SVG already matches D-199 and the synthetic G3 total; no SVG mutation was needed.
- Tasks 2 and 3 are completed as **static planning reviews only**. The board/protocol gaps above remain explicit future task and accessibility evidence obligations.
- Keep W1/N1/A2 and modified G2/G3 as selected planning directions. Do not reopen them solely because testing has not yet occurred.
- Preserve doc 34 as a prepared protocol. Do not mark T-01–T-16 or A-01–A-12 passed; participant/device review has not been run or authorised.
- Reconcile findings into doc 28 and the design checklist only after a separately authorised evidence-gathering review. The planning gate remains **CLOSED**; no implementation, build, test, APK or device validation is authorised or claimed.


## 5.9 Growth Mark display review and requirement traceability closeout — 2026-10-30

### Task 4 — Growth Mark display, units, periods and zero states

**Review basis:** D-199/D-201, docs/15 section 9, docs/26 section 4.1, AC-025 in docs/28, the Growth Mark SVG, and T-10–T-12/A-11 in docs/34. This is a cross-document semantic review, not runtime verification.

| Topic | Reconciled rule | Remaining review obligation |
|---|---|---|
| G2 meaning | “Collected moments” is a duration total from saved duration sessions of duration-tracked habits only | Ensure label and supporting text do not suggest action count, active dates, points or score |
| Included/excluded records | Compatible saved duration sessions only; exclude quantity, completion-only/effort-aware outcomes and journal entries | Confirm source filtering and effective-dated habit mode against real records after separate authorisation |
| Aggregation | Normalize compatible units before summing; preserve source precision; round only for display; recompute after corrections/deletions | Verify arithmetic and recomputation; no silent loss or upward rounding |
| Display units | Largest useful whole unit plus smaller-unit remainder: minutes, hours, 24-hour days and 7-day weeks; no month-equivalent duration in v1 | Exact visual thresholds, pluralization/localization, compact layout and TalkBack phrasing remain to be reviewed |
| Period | G2 defaults to current calendar month; G3 navigates calendar months and supports date/session drill-down; no independent day/week selectors in initial v1 unless scope is reopened | Show unambiguous month/year and selected-date context; verify previous/next navigation and matching session list |
| Explicit zero | A saved zero-duration session is a real record, contributes zero and remains distinguishable from no record | Include a zero-only period and a date with no session; never imply zero means missing or failure |
| Empty/error | No qualifying sessions, an empty month, loading, query failure and failed source retrieval are distinct states | These states are not drawn in the current SVG; define and review their presentation later |
| Date detail | G3 exposes inspectable source sessions; blank dates remain neutral | Verify selected-date total equals visible compatible source sessions; edit/delete recalculates G2/G3 |

**Arithmetic cross-check:** the six synthetic date values shown in G3 (35 + 20 + 60 + 25 + 30 + 50 minutes) total 220 minutes, or **3 hr 40 min**, matching the G2 headline. This is an arithmetic check of synthetic illustration data only, not an aggregation test.

**Findings:**
1. D-201 settles aggregation, display-unit family, v1 period defaults and explicit-zero semantics as planning defaults. These should not be listed as undecided owner choices.
2. Main unresolved design details are actual screen hierarchy/wording at narrow widths, unit thresholds and localized labels, zero-only/no-session/empty/error states, source-session inspection, and accessible announcements.
3. Growth Mark SVG shows only illustrative positive date totals. It does not demonstrate explicit-zero versus no-session, zero-only month, no qualifying sessions, failed query, correction/deletion, or selected-date TalkBack output.
4. T-10/T-11/T-12 in doc 34 and AC-025 in doc 28 form the planned task/acceptance chain. All remain not run.

### Task 5 — Requirement-to-screen/test traceability reconciliation

A bounded cross-reference pass now aligns:
- **Growth Mark:** D-199/D-201 → docs/15 and docs/26 section 4.1 → T-10–T-12 → AC-025.
- **Journal lifecycle:** docs/26 Journal/editor contract → AC-007 → T-03/T-06–T-09.
- **Journal navigation/retrieval:** N1/A2 in this pack and navigation/archive board → AC-021 → T-04/T-05/T-09, with limitations recorded in doc 34.
- **Visual/accessibility:** D-205/D-207 → docs/17 and docs/29 → AC-018 and A-01–A-12 in doc 34.
- **Comeback copy/interaction:** D-206 and docs/24 → AC-010 → T-15, with localization/accessibility still open.
- **Privacy/settings discovery:** docs/04 and docs/26 → AC-016/AC-021 → T-13/T-14. The current navigation board shows Settings but not its contents; this is an explicit screen-map evidence gap, not a confirmed navigation defect.

The updated matrix in doc 28 now identifies the relevant decision/source, contract, task/acceptance case and remaining evidence for these chains. This is a bounded planning traceability reconciliation, not an exhaustive audit of every historical repository statement or every implementation requirement.

### Closeout

- Growth Mark semantic rules are internally aligned across the decision, research, screen contract, visual board and acceptance matrix.
- No new owner decision is required for D-201 settled defaults. Any proposed change to the chosen v1 period or unit semantics should be escalated as a scope change rather than silently altered.
- Journal retrieval and Settings/privacy destination details remain explicit future design/task evidence gaps.
- All acceptance cases remain planned/not executed. No user task study, rendered-screen inspection, accessibility measurement, build, test or device validation is claimed. Planning gate remains CLOSED.
