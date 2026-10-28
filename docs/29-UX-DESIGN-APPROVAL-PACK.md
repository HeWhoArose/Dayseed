# UX and Design Approval Pack

**Review date:** 2026-10-28  
**Status:** Recommendations prepared for owner review; **no visual direction, navigation pattern, token set, or prototype has been approved by this document**. The planning gate remains closed.

**Related documents:** [UX and Design](02-UX-AND-DESIGN.md), [Today Dashboard Concepts](21-TODAY-DASHBOARD-CONCEPTS.md), [Design System](../DESIGN.md), [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md), [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md), [Typography and Accessibility Research](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md), [Design Review Checklist](DESIGN-REVIEW-CHECKLIST.md), [Requirement Traceability](28-REQUIREMENT-TRACEABILITY.md).

## 1. Current status

The product rules are far more settled than the UI choices. Approved baseline requirements include Today’s separate Complete and Record actions; the local journal and its save/edit/delete rules; the History and Insight jobs; 14-day and 30-day comeback triggers; nonjudgmental copy; offline title/body journal search; local backup/restore settings; and the accessibility expectations. Those policies must shape the design rather than being treated as undecided. Three rendered comparison boards are now available in `docs/prototypes/`; they are ready for owner review, but do not constitute accessibility validation or user-test evidence.

Still open are the visual direction (D-008), Growth Mark representation (D-016), comeback presentation/copy (D-017), archive layout (D-018), type and colour tokens (D-019), and exact Journal/History navigation. The current Dashboard Concepts document includes a user-supplied screenshot discussion and a “little of Concept C” direction; use that as the design exploration anchor, not as approval of a finished screen.

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

**Recommended starting option in the rendered comparison:** four bottom-level destinations—**Today**, **History**, **Insights**, and **Settings**—with a very visible, lightweight **Write reflection** action on Today. History should let people switch clearly between habit history and the journal archive, and the Journal archive view should have an obvious “New entry” action. Compare this with the dedicated-Journal alternative before selection.

Why test this first: it keeps the primary daily action, retrieval, summaries and data controls separate without overcrowding the bottom bar. The trade-off is that a journal-first user may not discover History’s journal area quickly enough. Compare it with a dedicated Journal tab variant using the same tasks; do not decide by aesthetics alone.

### Today screen

The habit list remains the practical centre of Today. Each habit should expose the correct action for its tracking mode and schedule. Keep Complete (usual target for numeric/duration, completion for completion-only) separate from Record (actual value, edit/correction, notes). Do not show numeric controls for completion-only habits or imply a journal entry completes a habit.

If a summary is included, use a precise denominator and period. For example, show how many habits are scheduled today and how many have a saved qualifying record; make “not recorded” visually neutral and do not call an active-day total a success percentage. Flexible weekly goals must have their own weekly progress summary.

### Growth Mark

**Recommended comparison:** test a small, plain-language “Collected moments” summary against a plain editorial number/text summary. Define the candidate as distinct local calendar dates in the selected period with at least one qualifying positive habit record (a positive numeric/duration amount or valid completion/effort-aware outcome). An explicit numeric zero remains a real record but is not counted as a positive activity moment. Show the period and let the user inspect the underlying dates/records.

Do not start with a growing plant, arbitrary level, composite score, streak, or time-based decay. Only add a symbolic visual if every change maps to a real, auditable event that users can explain. D-016 remains open until tested/reviewed.

### Journal archive and search

**Recommended first prototype:** recent chronological entries grouped by date, a compact calendar/date-jump control, a readable entry detail/editor, and the approved local title/body search. Keep the search scope to saved title/body content; display recorded mood labels where present and exclude unsaved comeback drafts. Advanced date-range/habit filters are not implied.

A blank date is neutral. Calendar marks mean “an entry exists,” never “this day was good/bad” or “a habit was missed.” Compare this hybrid with timeline-only and calendar-first navigation using identical retrieval/edit/delete tasks.

### Comeback presentation and copy

The active v1 triggers are already approved: the two-week experience after 14 complete local calendar days without opening Dayseed; the broader 30-day experience when either app inactivity or no active habits qualifies; the 30-day experience takes precedence; each is shown at most once for that qualifying episode. Do not treat the older 3-day/90-day copy examples as triggers.

**Recommended presentation to test:** a compact, dismissible, non-blocking welcome that never demands an explanation, never erases progress, and offers a small next step plus a valid continue/not-now path. The one-month choice flow retains its approved create/resume/continue options; optional reflection remains optional and draft-safe. Final copy/layout remains open, not the trigger rules.

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
3. **Growth Mark:** “Collected moments” small labelled metric/inspectable date marks versus a plain text-and-count summary. Do not use abstract plant growth or scoring as a comparison baseline.
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

## 6. Owner decision batch

The owner can approve a recommendation, request a variation, or explicitly defer any of these without blocking core features that do not depend on it. The suggestions below are recommendations, not approvals.

| Decision | Current status | Recommendation to review | What makes it ready to approve |
|---|---|---|---|
| D-008 visual direction | Open; three SVG comparison boards prepared | Prototype Field Journal warmth + Living Editorial hierarchy + Quiet Instrument controls | Review the Today, navigation/archive, and Growth Mark boards at small/typical widths and enlarged text; choose or defer the direction. Exact colour pairs, contrast, and accessibility still need review before final design approval |
| Navigation / Journal placement | Open in UX | Test Today / History / Insights / Settings with Journal accessible from Today and History against a dedicated Journal tab | Task-based find/write/retrieve/edit results; no hidden core flow |
| D-016 Growth Mark | Open | Test “Collected moments” versus plain editorial summary; omit illustrative growth until semantic value is proven | Metric is defined, inspectable and not misleading; representative users understand it |
| D-017 comeback presentation | Trigger policy approved; presentation open | Compact, non-blocking, dismissible welcome using approved 14-/30-day rules | Copy/layout review; proves no mandatory reflection or duplicate episode |
| D-018 Journal archive | Open; hybrid proposed | Chronological list + date jump/calendar + approved local search | Retrieval/edit/delete tasks with large-text/empty-state review |
| D-019 typography/colour | Open | System typography baseline vs a carefully selected editorial option; restrained semantic palette | License/source confirmed; every actual text/interactive pair passes review |
| Theme preference | Open | Follow system by default; consider an optional System/Light/Dark override if usability/accessibility benefits justify the extra setting | A complete light/dark palette and prototype review; no mechanically inverted palette |
| Minimum Android support/device matrix | Product target Android; exact minimum remains unverified | Decide from source/toolchain and intended device needs during the authorised technical-validation stage, not from a guessed number today | Supported API/build evidence and a named test-device matrix; this does not need to delay visual exploration |
| Retrospective journal prompt (D-042) | Optional prompt direction approved; cadence delegated under D-196 | Use the conservative default in docs 24/26/28: on the first eligible app opening of a local calendar day, offer at most one prompt for the most recent fully completed local date only if that date has no saved journal entry; allow “Not now”; suppress repeats after display/dismissal/save; no backlog or OS notification; comeback takes precedence. The editor shows the explicit date and saving remains a normal journal save. | Routine cadence is not a separate approval blocker; the owner may override or defer it in the grouped review. Test app open after 1+ days away, multiple entries/date, dismissal/save suppression, and no effect on habit status |

## 8. Low-fidelity wireframe sketches — planning only

**Rendered planning artefact:** [Today screen comparison — W1 Action-first, W2 Reflective, W3 Compact controls](prototypes/dayseed-today-variants.svg). This SVG compares the same synthetic habit data across three hierarchy variants. It is a planning prototype only: no style/navigation choice is approved, no usability study has been performed, and the sample data are not real user records.

**Rendered navigation/archive planning artefact:** [N1/N2 navigation and A1/A2/A3 archive comparison](prototypes/dayseed-navigation-archive-variants.svg). This board compares four versus five bottom destinations and timeline-first, hybrid, and calendar-first archive layouts using matching synthetic entries. It is also planning-only; owner selection and task/accessibility review remain outstanding.

**Rendered Growth Mark comparison:** [G1 plain monthly context, G2 label + count, and G3 count + inspectable date marks](prototypes/dayseed-growth-mark-variants.svg). These options use the same synthetic October count and Progress destination while varying the presentation. This separates the Growth Mark wording/representation question from the Today-hierarchy comparison; no option is approved, and accessibility review remains outstanding.


The linked SVG boards are rendered comparison prototypes with consistent synthetic data across variants. They are still planning artefacts, not usability findings, approved layout decisions, or application UI. The remaining step is a task-based review with large text, empty/error/destructive states and accessibility checks, followed by owner selection or explicit deferral.

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
│ Collected moments · See all │
│────────────────────────────│
│ Today  History  Insights  ⚙ │
└────────────────────────────┘
```

Purpose: make today's useful habit action first. The denominator must be the count of habits actually scheduled today; the numeric values and outcomes come from saved records, and “0” appears only for explicit zero. Interactions: Record opens the correct mode-specific recorder; the reflection action starts a separate Journal entry; “See all” opens the inspectable growth/history view. Empty state: invite creation or exploration without fake records. Accessibility: every action has a named control and status text beyond colour.

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
│ 6 active dates this month   │
│ [View the dates behind this]│
│────────────────────────────│
│ Today  History  Insights  ⚙ │
└────────────────────────────┘
```

Purpose: test the user's stated interest in a slightly more reflective Today screen without making writing compulsory. The count is a candidate metric only; it must be inspectable and must not count a saved numeric zero as a positive activity moment. Compare whether the reflection invitation crowds out the main habit action, especially with large text and several habits.

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

**Exit rule:** a wireframe is not approved because it looks cleaner. Choose a direction only after the variants use the same content/tasks, major empty/error/destructive states have been checked, accessibility concerns are logged, and the owner approves or explicitly defers each decision. No usability score or observed finding is claimed yet.

## 7. Approval and implementation boundary

This pack is a planning deliverable only. Until the owner approves the planning gate:

- no app UI/code changes;
- no database schema changes or migration;
- no Gradle build/test, APK install, or physical-device validation;
- no visual tokens or layouts may be marked approved merely because a proposal exists.

When the owner settles a consequential design decision, record it in the decision log, update the UX plan and DESIGN.md as applicable, align the dashboard concepts/research docs, update the screen/data contracts and traceability matrix, and re-fetch every changed file for verification.
