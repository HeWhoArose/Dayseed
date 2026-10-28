# Today Dashboard: Comparative UI Concepts

**Research date:** 2026-10-17  
**Status:** Concept exploration. The owner selected W1/N1/A2 as starting directions and accepted the platform/system appearance baseline; no final layout, navigation implementation, visual token set, or final copy is approved.  
**Purpose:** Continue the Dayseed UI research by comparing different Today-screen priorities before committing to a visual direction or building app UI.

**Current-authority note (2026-10-28):** This is a cumulative design notebook with dated proposals; old “decisions still needed” and “next design iteration” sections are historical snapshots, not a live backlog. For current habit modes, schedule types, date/history policy, and Complete/Record semantics, use `docs/01-PRODUCT-SPECIFICATION.md`, `docs/09-DECISION-LOG.md`, and `docs/26-SCREEN-AND-DATA-CONTRACTS.md`. The current contract in docs 01/09/26 and approved schedule decisions D-191/D-195 supersede earlier tentative ideas about one numeric total per day, optional numeric targets, decimal quantities, arbitrary start dates, and every-N-days schedules. The owner-selected Today direction is W1 Action-first. Under D-199, the compact Growth Mark headline is G2 “Collected moments” (duration total for the current calendar month by default), with G3 date/session detail; the active-date-count concept below is historical only. The selected directions are planning baselines, not final UI approval. Owner disposition of the proposed Growth Mark formatting and final visual/accessibility review remain open. The rendered Today, navigation/archive and Growth Mark comparisons are linked in `docs/29-UX-DESIGN-APPROVAL-PACK.md`.

## Product promise

**Your progress is more than your streak.**

The Today screen should help a person answer three questions quickly:
1. What can I do today?
2. How can I record an effort without friction?
3. Where can I reflect or see my progress without being judged?

The dashboard must not turn into a crowded productivity dashboard. Core habit logging and reflection must work offline. A tiny action can count when the habit supports effort levels. A missed or unrecorded day must not erase prior progress or be described as failure.

## Shared requirements for every concept

- Show today's date and a clear screen title or welcome.
- Make today's habits and their primary logging action immediately discoverable.
- Support Tiny / Usual / Extra where configured, and a completion-only action where appropriate.
- Make daily reflection easy to find but never compulsory.
- Give the Growth Mark a meaningful role only if its measure is transparent and explainable.
- Keep progress summaries subordinate to today's actions.
- Avoid guilt copy, arbitrary XP/points, punitive decay, streak-dominant messaging, and forced comeback gates.
- Keep important controls accessible on small Android screens, with large text, TalkBack, and 48dp target checks.
- Treat all examples below as hypotheses, not approved requirements.

## User-provided reference and direction to expand

On 2026-10-17, the user supplied a dashboard concept screenshot and confirmed that the intended direction combines the existing dashboard exploration with **a little of Concept C (Reflective Today)**. Treat that screenshot as the current discussion anchor, not as an approved final design. The goal is to refine and expand this specific direction—not restart from generic template options.

### What to preserve as hypotheses

- Warm, restrained, paper-like background with subtle tonal variation; no gratuitous gradients or glass effects.
- Strong editorial headline and clear hierarchy.
- Today's habit actions are the practical centre of the screen.
- Tiny / Usual / Extra controls are grouped with the relevant habit and communicate effort plainly.
- A compact, clearly labelled daily progress summary, provided its metric is unambiguous.
- A quiet, optional journal entry point that does not imply journaling is mandatory.
- A restrained growth symbol that has meaning grounded in actual user records.
- Bottom navigation is a candidate, not a locked decision.

### Anti-slop / anti-pattern guardrails

Avoid:
- Template-like dashboards built from many identical rounded cards.
- Decorative gradients, neon glows, glassmorphism, excessive shadows, or ornamental leaves without semantic purpose.
- Generic motivational wallpaper and overfamiliar “you've got this” copy.
- Arbitrary XP, scores, badges, streak pressure, punitive decay, or celebratory animation for every routine action.
- Progress numbers without a precise definition and period.
- Showing effort-level controls where a habit is completion-only or where the choices are not meaningful.
- Excessive empty space that pushes the user's next action down the screen, as well as dense screens that make every feature compete.
- Reflection prompts that feel compulsory, demand emotional disclosure, or imply mood/clinical analysis.
- Overstating completion: “2 of 3 recorded” must clarify whether the denominator means today's scheduled habits, visible habits, or another defined set. “Not recorded” is not automatically “not done.”
- Using colour alone for status or making unselected controls hard to distinguish from disabled controls.

### Specific questions raised by the supplied screenshot

1. **Progress card:** What exactly are the three segments? Are they three habits scheduled today, three habits selected for today, or three possible actions? The count and visual must match the eventual schedule/record semantics.
2. **Effort controls:** Should Tiny / Usual / Extra remain visible on every habit, or appear after selecting a habit? Keep them visible only when they are genuinely useful; test tap count and screen height.
3. **Completed habit:** Show the chosen effort and a clear recorded state without making the whole row look disabled. Allow correction/editing without friction.
4. **Unrecorded habit:** The neutral dot should have an accessible label and must not imply failure. Make the next action obvious.
5. **Reflection card:** Keep it optional and lightweight; explore whether one short invitation is enough instead of multiple lines that compete with habits.
6. **Header/actions:** The screenshot includes menu and compose actions. Their meaning and priority need to be specified; do not add icons merely to fill the header.
7. **Navigation:** “History” versus “Journal” placement remains open; test whether a person can find past habit records and journal entries without confusion.
8. **Small screens:** Validate the actual content height, scroll behaviour, system navigation overlap, keyboard, enlarged text, and touch-target sizes on Android. A concept screenshot is not proof of usability.

### Direction to develop next

Create a refined low-fidelity version of this reference, plus one controlled alternative that reduces visible effort controls and one that gives reflection slightly more prominence. Keep the palette and typography constant across variants so the comparison focuses on hierarchy and interaction. Use realistic examples and annotate each element with its purpose, data source, interaction, empty state, accessibility label, and unresolved decisions. Do not implement app UI or treat generated imagery as a functional prototype before the planning gate is approved.

## Three layouts to compare

### Concept A — Action-first Today

**Priority:** Complete or record today's habits with as little friction as possible.

**Proposed order**
1. Compact greeting and date.
2. Today's habit list, with each habit's main action visible.
3. Small secondary reflection entry point.
4. A restrained Growth Mark summary or “see progress” link.
5. Persistent bottom navigation, if testing supports it.

**Strengths**
- Fast to scan and use one-handed.
- The main job is obvious.
- Progress decoration is less likely to push actions below the fold.

**Risks**
- May feel plain or transactional.
- The Growth Mark and Dayseed's reflective identity could become too easy to overlook.
- Many habits may create a long list; test scrolling and prioritisation.

**Best question to test:** Can a new user log one habit and find reflection without help?

### Concept B — Growth-led editorial Today

**Priority:** Make the user's accumulated effort visible, while keeping daily actions close at hand.

**Proposed order**
1. Compact greeting and date.
2. A modest Growth Mark / collected-moments card with a plain-language label.
3. Today's habit list.
4. Daily reflection prompt.
5. A small weekly insight or history link.
6. Persistent bottom navigation, if testing supports it.

**Strengths**
- Makes Dayseed's “progress beyond streaks” promise more distinctive.
- Can create a sense of continuity without a leaderboard or score.
- Connects today's actions to a longer personal story.

**Risks**
- A large visual mark may push useful actions too far down.
- Users may mistake collected dates for a daily-compliance score.
- A growth metaphor can become arbitrary gamification unless each visual state maps to real records.

**Best question to test:** Can users explain what the mark means, what contributes to it, and what a blank date means?

### Concept C — Reflective Today

**Priority:** Balance habit logging with a calm, low-pressure check-in.

**Proposed order**
1. Compact greeting and date.
2. A short, optional “How would you like to show up today?” or neutral reflection entry point (copy must be tested; no mood tracking is implied).
3. Today's habit list with clear logging actions.
4. A small progress summary.
5. A journal/history link.
6. Persistent bottom navigation, if testing supports it.

**Strengths**
- Signals that Dayseed supports reflection as well as task completion.
- May feel welcoming to users rebuilding consistency.
- Makes space for a quick journal without treating it as a productivity metric.

**Risks**
- A reflective prompt may feel intrusive or slow down people who just want to log a habit.
- Could imply mood tracking or emotional assessment that is not in v1.
- The prompt must be skippable and should not dominate the first screen.

**Best question to test:** Do users find the reflection prompt supportive, irrelevant, or pressuring—and can they skip it easily?

## Early comparison matrix

| Criterion | A: Action-first | B: Growth-led | C: Reflective |
|---|---|---|---|
| Speed of habit logging | Expected strongest | Depends on mark size/order | Depends on prompt size/order |
| Distinctive Dayseed identity | Moderate | Expected strongest | Moderate |
| Reflection visibility | Secondary | Secondary | Strongest |
| Risk of visual clutter | Low to moderate | Moderate to high | Moderate |
| Risk of pressure/misinterpretation | Low | Higher if metric is unclear | Higher if prompt feels compulsory |
| Evidence status | Hypothesis | Hypothesis | Hypothesis |

These are qualitative predictions, not measured scores or user-test results. Do not select a winner from this table alone.

## Growth Mark integration decision

The owner clarified the Growth Mark direction in D-199: Today uses a restrained G2-style **Collected moments** card showing the total recorded duration from duration-tracked habits in the current calendar month. The label and period must be explicit, and the amount should use a readable duration format. Tapping it opens G3 calendar/date-level detail, either within Calendar/Progress or on a separate page, so the user can inspect per-date totals and source duration sessions. G1's plain active-date summary remains a comparison alternative, not the selected card. Final display thresholds, period controls, aggregation edge cases and accessibility remain open; this decision does not approve implementation.

Every number must be derived from inspectable saved records and show its period. Only compatible duration-tracked habit sessions contribute to G2; quantity, completion-only/effort-aware outcomes and journal entries do not. An unrecorded date must not be presented as failure. G3 should show actual saved duration records by date, not streak points or decorative progress.

## Shared first-pass content sketch

Example content only; not final copy:

- Header: “Today” plus the local date.
- Habit section: “Your habits” or another tested label.
- Habit row: habit name, optional schedule/context, and a clear logging control.
- Reflection: “Daily reflection” with a short invitation and a clear skip path.
- Progress: a labelled summary such as “Dates with a recorded habit action this week” only if the metric definition is approved.
- Empty state: explain what can be done next without showing a judgemental zero.
- Comeback: a short dismissible message or card, never a blocking gate.

Do not assume a greeting based on time of day is needed; test whether it adds warmth or noise.

## Prototype and usability plan

Create equivalent low-fidelity wireframes for A, B, and C using the same sample habits, same content, same phone viewport, and same visual style. Vary layout hierarchy—not colour, typography, or decorative styling—so tests compare layout rather than branding.

Tasks:
1. Open Today and explain what can be done.
2. Log a Tiny action and explain whether it counts.
3. Find the daily reflection and skip it.
4. Explain the Growth Mark or progress summary.
5. Find yesterday's record and correct it.
6. Find the journal archive.
7. Repeat the primary task with enlarged text.

Record task success, hesitation, wrong taps, time-to-action, misinterpretations, and subjective pressure. Do not claim user research has happened until sessions are conducted. Include people who have struggled to maintain habits as well as confident trackers; do not collect journal content.

## Historical suggested next sequence (2026-10-17; superseded by the current planning review)

This list records an earlier exploration order; do not use it to reopen owner decisions or replace docs 27/31/34 as the current handover. The 3-, 14- and 90-day Return Page examples below were exploratory mockup examples, not the approved v1 triggers. The current comeback trigger/precedence rules are in docs 09/24. The former order was:

1. Agree on the user jobs and compare A/B/C wireframes.
2. Research the Growth Mark separately and decide whether it deserves a prominent Today card.
3. Prototype the Return Page states (including 3-, 14-, and 90-day examples) without making them blocking.
4. Prototype the journal archive (timeline versus hybrid timeline/calendar).
5. Revisit navigation and screen hierarchy with these concepts together.
6. Run accessibility checks on the chosen candidate, then record explicit decisions in [09-DECISION-LOG.md](09-DECISION-LOG.md).
7. Do not implement app UI until the planning gate is explicitly approved.

## Generated visual reference

A concept-board image was generated during the 2026-10-17 discussion to make the general warm/editorial direction easier to discuss. It depicts five example screens (Today, Growth Mark, Return Page, Journal Archive, and Insights). It is an illustrative reference only: generated imagery may contain inaccurate text, inconsistent details, and invented interface decisions. It is not a functional prototype, user-tested design, or approved specification. The three layout alternatives above still need comparable wireframes.

## Related documents

- [UX and Design](02-UX-AND-DESIGN.md)
- [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
- [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md)
- [Typography, Colour, and Accessibility](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
- [Design Review Checklist](DESIGN-REVIEW-CHECKLIST.md)
- [Decision Log](09-DECISION-LOG.md)


## Emerging preference: today first, broader growth second

**Status:** Owner's stated preference, to guide the next prototype round; not final UI approval.

The user prefers the Today dashboard to primarily record today's activity, with a small amount of broader progress context. Recommended hierarchy:

1. **Primary:** today's habits and clear record actions.
2. **Daily summary:** a compact, clearly labelled summary of today's recorded habits, such as “2 of 3 habits recorded,” only when the denominator is defined from the habits that are actually scheduled/active today.
3. **Secondary broader context:** one restrained sentence or small link, such as “You've recorded a habit on 8 days this month,” provided that this is computed from valid records and its period is explicit.
4. **Details on demand:** richer history, trends, and Growth Mark exploration belong in History/Progress rather than taking over Today.

### Important semantic guardrails

- “2 of 3” must mean three eligible habits for the current day, not three actions, effort levels, or an arbitrary target. Define handling for rest days, paused habits, schedules, and skipped habits before implementation.
- Prefer neutral wording like “recorded” over “completed” if Tiny effort is a valid logged action and the app is reporting records rather than judging outcomes.
- Never imply that an unrecorded habit is proof the person failed or did nothing.
- A broader monthly count should count distinct local calendar dates with at least one qualifying recorded habit action, if that metric is selected. Label it precisely; do not combine it with journal entries or turn it into a score.
- The broader glimpse must remain visually secondary and must not displace habit logging below the fold on small Android screens.
- Keep the panel useful offline and accessible without relying on colour alone.

### Next prototype comparison

Build low-fidelity variants with identical content:
- **Variant 1:** daily summary only.
- **Variant 2:** daily summary plus one-line monthly context.
- **Variant 3:** daily summary plus a small tap-through progress affordance, with the monthly context shown on Progress.

Compare scan speed, comprehension of the denominator, perceived pressure, and whether users notice the broader context without mistaking it for a score. These are test candidates, not implementation requirements.


## Decision recorded: daily summary + one-line monthly context

**Status:** Selected direction for the next prototype round by the project owner on 2026-10-17. This is a research/design decision, not authorization to implement the app or a claim that the UI is final.

Use the recommended combination:
- **Today remains primary.** Today's habit list and logging actions are the main focus.
- **Daily summary:** show a compact count such as “2 of 3 habits recorded” only after the denominator rules are defined for schedules, planned rest, paused habits, and skipped/missed records.
- **Broader context:** a single restrained sentence such as “You've recorded habits on 8 days this month.” This is a candidate copy/metric, not yet approved wording or final data semantics.
- **Deeper progress:** provide a quiet route to the Progress screen rather than a chart or decorative timeline on Today.
- **No tiny timeline on the initial Today concept.** It can be researched later if user evidence suggests it adds value.

The monthly measure should be explicitly labelled and computed from real qualifying habit records, using local calendar dates. It must not include journal entries, infer action from missing records, or treat a day without a record as failure. The broader context should remain visually secondary and must not push the habit actions down the screen on small Android devices.

### Prototype variants to prepare next

Prepare two equivalent low-fidelity wireframes with the same content and viewport:
1. Daily summary + one-line monthly context + quiet Progress link.
2. Daily summary + one-line monthly context, with no explicit Progress link in the card (Progress remains accessible through navigation).

Compare whether the link is discoverable, whether the monthly sentence is understood, and whether the card draws too much attention. Keep the screenshot's restrained editorial direction and apply the anti-AI-slop checklist. Do not finalise colour, typography, or the full design system based on this decision alone.


## Visual exploration: selected daily-first direction

A visual concept was generated on 2026-10-17 to explore the selected direction. It places the daily habit summary first, keeps the habit list as the dominant working area, includes the optional “A note to yourself” entry, and adds a quiet monthly sentence with a route to Progress.

**This generated image is a discussion artifact only**, not a production-ready UI specification. It may contain invented or inconsistent details and must not be treated as validated product behaviour.

Review notes for the next iteration:
- Preserve the user's earlier screenshot as the stronger reference for restrained, practical styling; avoid letting large botanical artwork dominate usable space.
- Keep the monthly context below or after the primary habit actions where possible. Test whether it should appear above reflection or below it, especially on short screens.
- Avoid duplicated settings access in the header and bottom navigation unless a clear need is established.
- Verify that the daily count denominator is well-defined and that “recorded” matches the underlying event semantics.
- Ensure effort controls remain usable at narrow widths and with large text; do not compress Tiny / Usual / Extra into undersized pills.
- Do not use generated text, icons, or imagery as final assets without review.
- Use the concept to discuss hierarchy only. Final typography, palette, iconography, spacing, states, and navigation remain open for comparative wireframing and accessibility review.


## Follow-up exploration: placement, density, and key states

**Research date:** 2026-10-17  
**Status:** Visual exploration only; not approved for implementation.

A generated design board explored five Today states/layouts plus secondary screens. It is useful for discussing hierarchy, but generated small text, icons, and detailed UI may be inaccurate. Do not treat it as a functional prototype or a source of approved requirements.

### Placement variants compared conceptually

**A — Primary / recommended baseline**
- Daily summary at the top of the content.
- One-line monthly context below the daily summary.
- Habit list follows immediately.
- Optional reflection after the habits.
- Bottom navigation remains consistent.

This most clearly communicates “today first, broader context second,” but the monthly note and reflection together may push lower habit rows below the fold.

**B — Reflection higher**
- Reflection appears before the daily summary and habit list.
- This makes journaling more prominent but weakens the action-first hierarchy and may create friction for people who only want to log a habit.

**C — Compact controls**
- Attempts to shrink effort selectors into icon-like controls.
- Do not adopt this without usability evidence: Tiny / Usual / Extra must remain understandable without requiring users to memorise icons. At small widths, clear text labels may be safer than compact symbolic controls.

**D — Completed day**
- Shows a neutral, completed state when all eligible habits have records.
- Avoid celebratory overload or implying that a perfect day is more valuable than a partial day.
- Ensure users can still edit/correct records and access reflection.

**E — Empty day**
- Offers a gentle first action and neutral progress display.
- Avoid “0” as a judgement or messages that assume failure. Confirm how planned rest, new habits, paused habits, and unscheduled days are represented before defining exact copy.

### Recommended next wireframe baseline

Use A as the baseline for the next round, with these refinements:
1. Keep the daily summary first, using a denominator based on eligible habits for that local date.
2. Keep the monthly context to one concise sentence with a quiet, optional route to Progress. Avoid adding a separate decorative plant illustration if the text and link already communicate the idea.
3. Keep the habit list as the dominant working area.
4. Keep “A note to yourself” after the habit list and make it clearly optional.
5. Test whether the monthly sentence sits directly beneath the daily summary or after the reflection. Default hypothesis: beneath the daily summary if it is short enough not to push key actions down; compare on a small Android viewport.
6. Retain text labels for effort options; never rely on icon-only Tiny / Usual / Extra controls.
7. Keep header and bottom navigation consistent; do not duplicate settings shortcuts without a demonstrated need.

### Required stress-test scenarios

Create wireframes for the same viewport and content across these states:
- Typical day with a mix of recorded and unrecorded habits.
- All eligible habits recorded.
- No habits recorded yet.
- Planned rest day / no habits scheduled.
- Several habits (long list requiring scrolling).
- Long habit names and optional descriptions.
- Large system text / display scaling.
- A habit with completion-only logging rather than effort levels.
- A past record that can be edited or corrected.

Review each for:
- Whether the primary action is obvious.
- Whether “2 of 3” has a clear denominator.
- Whether monthly activity is distinguishable from a streak and from journal activity.
- Whether the screen remains useful without colour or illustration.
- Tap target size, wrapping, clipping, scrolling, and bottom-navigation overlap.
- Whether any copy implies blame, a broken streak, or moral judgement.

### Current conclusion

The owner selected the daily summary + one-line monthly context + quiet route to Progress. The first visual baseline should be layout A, not the reflection-first or icon-compressed alternatives. This is a directional choice for research, not final design approval. No app implementation is authorised or started by this work.


## Owner direction: screens 1–8, using the recommended Today baseline

**Recorded:** 2026-10-17  
**Status:** Approved as the structure for the next design exploration round. Not final visual approval and not permission to implement app code.

The owner likes the generated design board's numbered screens 1–8 and wants those screens developed using the recommended baseline as the skeleton.

### Eight-screen design set

Use the numbered set from the concept board as the working scope:
1. **Today** — recommended daily-first baseline: daily summary, one-line monthly context with quiet Progress link, habits as the main working area, optional reflection after habits, consistent bottom navigation.
2. **Habit Creation / Edit** — create and configure a habit with clear, beginner-friendly choices.
3. **Habit Detail** — view a habit's records and relevant details without turning it into a scorecard.
4. **History (Calendar)** — inspect dated habit records and navigate to a past day.
5. **Journal Entry** — write, view, edit, and delete a personal note with clear save state.
6. **Insights (Overview)** — transparent, understandable summaries derived from actual records; no arbitrary composite score.
7. **Return Page (After a Break)** — non-judgmental, dismissible welcome back experience that preserves history and offers a next step.
8. **Settings** — essential controls for reminders, appearance/accessibility, privacy, and data export, subject to unresolved policy decisions.

The Today screen is the skeleton and sets the hierarchy, typography direction, spacing discipline, navigation, and tone. The other screens should feel like parts of the same product, not unrelated templates. However, their exact visual treatments and information architecture remain open for design exploration.

### Today baseline rules for all related screens

- Today is the daily action centre.
- The daily count comes before broader context.
- The monthly line stays short and visually secondary, with a quiet route to Progress.
- Habit actions remain readable and usable at small widths; Tiny / Usual / Extra stay text-labelled.
- Reflection is optional and comes after the main habit actions.
- Navigation remains consistent across screens; avoid duplicate controls without a clear need.
- Preserve the existing screenshot's restrained editorial feel and anti-AI-slop guardrails.
- Test ordinary, all-recorded, empty, rest/no-schedule, long-list, long-label, and enlarged-text states.
- Keep claims, metrics, and screen behaviours clearly marked as approved, proposed, or unresolved.

### Work sequence

1. Refine screen 1 across its key states and small-screen constraints.
2. Design screen 2 (habit creation/edit), since the quality of Today depends on how habits and effort options are configured.
3. Design screens 3–5 (habit detail, history, journal) and verify record editing/cardinality/navigation relationships.
4. Design screens 6–8 (insights, return, settings), ensuring metrics, comeback copy, and privacy/export behaviours agree with the product specification.
5. Review the full eight-screen set for consistency, contradictions, accessibility, and anti-slop issues.
6. Update related docs and the decision log after the design choices are reviewed.
7. Do not implement app source code until the explicit planning gate is approved.


## Next refinement: Today state matrix

**Status:** Required design exploration before treating the Today layout as stable. The generated home-screen image is a reference for hierarchy only; its text, dates, spacing, icons, and metrics are not implementation specifications.

### Shared layout invariants

Across all states, preserve this order unless small-screen testing shows a clear usability problem:
1. Header and local date.
2. Compact daily summary.
3. One-line monthly context with a quiet Progress destination.
4. Today's habit list and readable action controls.
5. Optional “A note to yourself” reflection entry.
6. Bottom navigation: Today, History, Insights, Settings.

The habit list is the primary working area. Monthly context must remain subordinate. Reflection is optional. The header must not duplicate navigation controls without a clear purpose.

### State matrix

| State | What should change | What must not happen |
|---|---|---|
| Typical day | Show count of eligible habits with a qualifying record; unrecorded habits retain clear effort controls | Do not imply an unrecorded habit means failure |
| All eligible habits recorded | Use neutral confirmation and retain access to edit/correct records | No fireworks, XP, superiority language, or locked controls |
| Nothing recorded yet | Invite one manageable action; make clear the user can start anywhere | No shaming zero, red warning, or broken-streak message |
| Rest / no habits scheduled | Explain neutrally that nothing is scheduled today; keep history and reflection available | Do not render “0 of 0” as failure or manufacture a target |
| Long habit list | Keep actions consistent and allow natural scrolling; preserve access to reflection and navigation | Do not shrink effort controls to fit everything above the fold |
| Long names / descriptions | Wrap text, allow rows to grow, retain visible action labels | No clipping, overlapping labels, or icon-only controls |
| Enlarged text / display scaling | Allow vertical growth and scrolling; keep touch targets comfortable | No fixed-height cards that truncate content or cover bottom navigation |
| Completion-only habit | Use a clear completion action instead of Tiny / Usual / Extra | Do not force effort levels on habits that do not use them |
| Past record edit | Make the selected date and existing value clear before changing it | Do not silently overwrite records or confuse past and today |

### Metric semantics that remain open

Before any count is treated as a requirement, decide:
- Which habits are eligible for the daily denominator: active, scheduled for the local date, not paused, and not on planned rest.
- Whether an explicit skip is a record for the daily summary or shown separately. Do not conflate skip with successful effort.
- How schedule changes and timezone changes affect historical eligibility.
- Whether monthly “days with a recorded habit action” counts distinct local calendar dates with at least one qualifying effort record, and how edits/deletions affect it.
- Whether journaling contributes only to journal activity (recommended) rather than habit progress.

### Small-screen review checklist

Use at least one narrow Android viewport and one enlarged-text condition. Check:
- No horizontal scrolling in the normal layout.
- Effort labels remain fully readable and easy to tap.
- Long habit names wrap without pushing controls off-screen.
- The first habit action is discoverable without unnecessary scrolling.
- Bottom navigation does not cover the final habit or reflection content.
- The monthly sentence is visually secondary.
- The screen still makes sense without colour, plant illustration, or progress fill.
- All-recorded and rest-day states remain calm and non-punitive.

### Historical exit gate for Screen 1 (2026-10-17; superseded by the current readiness review)

Do not move to Screen 2 as if Today were final until:
1. The state matrix is represented in wireframes or annotated concepts.
2. Daily and monthly metric definitions have either been decided or clearly marked open.
3. The design has been checked against the anti-AI-slop and accessibility checklists.
4. The owner has reviewed the variants and approved the direction.

This is a design/research gate only; it does not authorize app implementation.


## State review round: Today and the connected eight-screen system

**Date:** 2026-10-17  
**Status:** Generated visual exploration; hierarchy and state coverage review only. Not a final UI specification.

A generated board now shows five Today states and seven connected screens. The numbering in the generated image expands beyond the eight-screen scope by treating individual supporting screens as separate numbered views. For canonical planning, retain the agreed eight screen groups: Today; Habit Creation/Edit; Habit Detail; History; Journal Entry; Insights; Return Page; Settings. The generated board is illustrative and may contain inconsistent or invented details.

### Today state review

1. **Ordinary day:** A mix of recorded and unrecorded habits. Primary task remains easy to identify.
2. **All eligible habits recorded:** Neutral acknowledgement; do not imply the user must achieve a perfect day to have value.
3. **Nothing recorded yet:** Offer a manageable first step. Do not assume failure or use a punitive zero.
4. **Rest / nothing scheduled:** Explicitly distinguish from an eligible day with no records. Avoid counting it as incomplete.
5. **Many habits:** Keep controls legible and scrollable; bottom navigation must not cover content.
6. **Long names and descriptions:** Wrap or truncate deliberately with access to the full content; avoid overlapping effort buttons.
7. **Large text / display scaling:** Reflow controls and cards; do not rely on fixed-height containers.
8. **Completion-only habit:** Provide a simple record action without presenting irrelevant Tiny / Usual / Extra levels.
9. **Past record correction:** Make it clear that historical records can be edited and that edits update derived summaries.
10. **Offline state:** Logging and journaling remain available locally. Any connected-only monetisation must not interfere with core use.

### Findings and guardrails

- **Historical G1-era proposal (superseded by D-199):** the daily summary plus monthly active-date context was explored as a compact direction. The selected Growth Mark headline is now G2 “Collected moments” (duration total) with G3 date/session details; do not treat active-date count as the selected headline metric.
- The generated board repeats monthly context on multiple states; in a real interface this should remain concise and visually secondary, and should not be repeated if it creates clutter.
- The generated rest-day illustration is more decorative than necessary. Keep the existing editorial reference restrained; illustration is optional and must earn its space.
- The generated mockup includes specific example counts and sample journal content. These are placeholders, not real user data or approved copy.
- The mockup uses a date and device/status-bar details that are illustrative only. Date must be the device's local date; do not hard-code the sample date.
- Avoid relying on colour alone to distinguish recorded states. Combine labels, shape, and accessible state semantics.
- Do not infer that a habit is “completed” from a journal entry.
- **Superseded open-question list:** schedule kinds, missed/rest/paused status, timezone date-key behaviour and effective-date edits have since been recorded in D-187/D-188/D-191/D-193/D-194/D-195 and docs 01/26. Monthly distinct-date count is not the selected Growth Mark metric; D-199 selects a duration total and G3 date detail. Any still-relevant exact summary denominator must be specified in the current product/screen contract rather than reopened as an unspecified bundle.

### Historical next design task (pre-decision)

The following checklist predates the owner decisions and is retained only as a set of potential states to verify against the current product contract. It is not a live backlog of undecided semantics. The current sequence and unresolved choices are tracked in docs 27/31/34. The habit editor must reflect these current rules:
- Name and optional description.
- Icon/appearance choice that avoids overwhelming icon catalogues.
- Completion-only versus effort-level logging.
- Tiny / Usual / Extra choices and progress labels must follow the current mode/target rules. Duration/Quantity habits have a usual target; do not present those targets as optional.
- The approved schedule modes, neutral non-scheduled/paused states, and how a habit appears on Today; do not add every-N-days or a habit-day Skip action.
- Optional per-habit reminders use the per-habit default; the separate weekly reflection uses O-9's 09:00 device-local time on the configured week-start day. Both follow the shared quiet-hours/cap/collision defaults in D-196. Permission timing and OS delivery need later platform validation.
- Editing, pausing, archiving, and deleting a habit, including effects on history.

Historical instruction superseded: the main schedule, target, record-mode, multiple-entry and effective-date semantics are now specified in docs 01/09/26 and decisions D-187/D-189–D-195. Use those sources when reviewing the editor; escalate only a consequential gap that is genuinely not covered by the current contracts.


## Screen 2 research: Habit Creation / Edit

**Date:** 2026-10-18  
**Status:** Generated concept board and proposed UX structure; not approved as final UI or product semantics.

A visual exploration board was generated with six creation steps and variations for editing, tracking type, schedules, reminders, rest days, and archived habits. It is a discussion artifact only: small generated text may be inaccurate, and no behavior should be implemented solely because it appears in the image.

### Proposed creation flow

Keep the first-time flow approachable and avoid forcing every user through a long advanced form. Explore a progressive flow with these sections:

1. **Choose a starting point (optional):** select a simple suggested template or start custom. Templates should be shortcuts, not categories users must understand. Allow skipping this step.
2. **Basic details:** required habit name; optional description; optional icon/accent. Avoid large icon catalogues, decorative customisation, or requiring a colour.
3. **Tracking method:** choose one clearly explained method:
   - **Effort levels:** Tiny / Usual / Extra; a Tiny action is a valid record for that day.
   - **Completion-only:** a single “Record” or “Done” action; do not show irrelevant effort controls.
   - **Number-based:** optional candidate for later scope; if retained, specify units, targets, reset period, and what constitutes a record before exposing it in v1. This is not yet approved for v1.
4. **Schedule:** choose days/pattern with plain-language summary and a way to edit later. Define local-date behavior, start date, and how schedule changes affect historical eligibility before implementation.
5. **Optional reminders:** off by default unless a deliberate onboarding decision says otherwise; explain that reminders are optional and require OS permission when notifications are requested. Avoid requesting permission before the user chooses to enable reminders.
6. **Review / save:** concise summary, clear create action, and easy back navigation. Don't require a separate review screen if testing shows it adds unnecessary friction.

### Edit, pause, archive, delete

- Editing should not silently rewrite historical records. If changing tracking method or schedule changes how old records are interpreted, explain the consequences and define the policy.
- **Pause** should mean temporarily stop showing/scheduling future instances without deleting history. Exact eligibility semantics are open.
- **Archive** should remove a habit from the active Today list while preserving historical records and access in History/Insights. Restoring should be possible.
- **Delete permanently** is a destructive action and needs explicit confirmation and clear disclosure of which associated records and journal data (if any) are deleted. Retention and export policy must be settled first.
- Never conflate a rest day, a skipped occurrence, a missed/unrecorded occurrence, pausing, and archiving.

### Anti-slop and usability requirements

- Match the chosen Dayseed editorial skeleton: warm restrained surface, strong readable hierarchy, deliberate spacing, small purposeful accents.
- Avoid unnecessary gradients, glassmorphism, glowing effects, decorative illustrations that displace controls, excessive pills, arbitrary gamification, and generic motivational copy.
- Do not make icon or colour selection mandatory.
- Use labels and examples to explain tracking methods; never rely on icons alone.
- Keep core setup usable offline.
- Check small Android screens, keyboard overlap, large text, TalkBack order/labels, tap target size, contrast, and scrolling.
- Every screen needs loading/error/save feedback where applicable, but do not invent cloud-sync states for an offline-first design.
- Generated counts, names, dates, sample content, and option lists in the image are placeholders.

### Decisions still needed before finalising this flow

1. Which tracking methods belong in v1: effort levels and completion-only are the current core candidates; number-based tracking remains unapproved.
2. Whether schedule patterns beyond selected weekdays are required for v1.
3. How a schedule change affects prior dates and existing records.
4. How skipped/missed/rest occurrences are represented.
5. Whether pause and archive are both needed in v1, and how each affects Today, reminders, and insights.
6. Reminder frequency, quiet hours, permission timing, and notification copy.
7. Delete/export/backup semantics and whether journal entries can ever be linked to a habit.
8. Whether icon/accent customisation is valuable enough to include initially.

### Next step

Review the flow for unnecessary complexity, then create a focused first-pass wireframe for the **tracking-method choice** and **schedule picker**. These two sections have the largest impact on the Today dashboard and the meaning of progress metrics. Record decisions in the decision log only after the owner confirms them. No app implementation is authorised or started.


## Owner preference: tracking methods and progressive scheduling

**Recorded:** 2026-10-18  
**Status:** Owner-selected direction for the next design iteration. Product semantics still need specification; no implementation is authorised.

### Tracking methods
The owner prefers the tracking range to extend from effort levels through number-based tracking. Design exploration should support:
- **Effort levels:** Tiny / Usual / Extra.
- **Completion-only:** a simple recorded/done action.
- **Number-based:** record a quantity with a user-defined unit/target where applicable.

This paragraph records an earlier exploration, not an open current requirement. The current contract is in [Product Specification](01-PRODUCT-SPECIFICATION.md), [Decision Log](09-DECISION-LOG.md), and [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md): Duration and Quantity habits have a usual target; Quantity actuals are non-negative whole numbers; multiple valid numeric/duration sessions per habit/local date are allowed and summed; explicit zero differs from no record; and Completion-only/Effort-aware modes remain distinct. Do not use this notebook’s historic questions as current owner blockers.

### Scheduling: historical progressive-disclosure proposal (superseded)

**Historical proposal from 2026-10-18; current authority is D-191/D-195 in [Decision Log](09-DECISION-LOG.md) and the schedule contract in [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md).** This section originally explored a selected-weekday-first interface; it is not the current schedule specification. Current v1 schedule kinds are `DAILY`, `SELECTED_WEEKDAYS`, and `FLEXIBLE_WEEKLY_TARGET` only. A progressive-disclosure design may still be explored if it presents those modes accurately, but **every-N-days is not a v1 option**.

Default collapsed view:
- Weekday selector.
- Optional start date.
- One short human-readable summary.
- A clearly labelled secondary affordance such as “More schedule options” with an expand/collapse indicator.

Historical exploration listed these ideas; apply the current authority above instead:
- `DAILY` schedule.
- `SELECTED_WEEKDAYS` schedule, with its required valid weekday selection.
- `FLEXIBLE_WEEKLY_TARGET` (for example, achieve a chosen number of days in the configured week); preferred days are optional guidance, not restrictions.
- A clear schedule summary and a way to collapse advanced options without losing selections.

Do not offer an every-N-days interval in v1.

### Design constraints
- Advanced scheduling must be discoverable but must not overwhelm first-time habit creation.
- State exactly what a pattern means in plain language and show a summary before saving.
- Avoid silently converting between weekday schedules, weekly targets, and interval schedules.
- Decide how schedule edits affect past dates and eligibility before implementation.
- Handle no weekdays selected, start dates in the past/future, time-zone/local-date boundaries, paused/archived habits, and rest days.
- All schedule controls need accessible labels, non-colour state cues, large-text reflow, and usable touch targets.
- Keep all core scheduling functions available offline.

### Historical next-design-iteration note

The old proposed wireframe checklist below is retained as historical context, not as a current requirement. Any new comparison should use the current D-191/D-195 schedule vocabulary; do not add the superseded every-N-days option. Effective-date behaviour must follow D-194 in the [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md). The schedule vocabulary has already been recorded; do not re-open it as an unmade decision. No app implementation is authorised or started.


## Screen 2 follow-up: historical number-based and schedule exploration

**Date of original note:** 2026-10-18. This is retained as design history, not as the current owner decision batch. The current authority is [Product Specification](01-PRODUCT-SPECIFICATION.md), [Decision Log](09-DECISION-LOG.md), and [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md). In particular, the current v1 contract requires a usual target for Duration/Quantity, allows multiple entries per habit/date, restricts numeric actuals to non-negative whole numbers, uses only D-191/D-195 schedule kinds, and follows D-194 for same-day configuration edits. Where this dated exploration conflicts with those sources, the current sources win.

### Number-based tracking: historical first-pass notes

A Quantity habit stores a count in a user-defined unit (for example, pages, questions, or repetitions) and has a usual target under the current v1 contract. Duration is tracked as elapsed time in the compatible internal unit defined by the screen/data contract.

Proposed behavior:
- **Unit required:** the unit tells users what the number means. Offer a short editable text unit, with suggestions only as shortcuts.
- **Usual target:** the user defines a usual target for Duration/Quantity. Distinguish an actual saved amount from the target; progress labels derive from the actual-to-target ratio and never replace or cap the recorded amount.
- **Whole-number actuals:** actual quantity/duration values are non-negative whole numbers under the current v1 contract. Zero may be deliberately recorded when meaningful; decimal actuals are not a v1 input option. Blank/invalid input must not create a record.
- **Target exceeded:** accept values above target. Never cap or discard the actual amount just because the target was exceeded.
- **Multiple sessions per date:** allow multiple valid quantity/duration entries for one habit and local calendar date. Sum them for derived totals while retaining each entry as separately inspectable and editable. Correcting or deleting one entry targets only that entry; do not replace the day's entire total or silently add a retry as a second entry.
- **No value is not zero:** an unrecorded date remains unrecorded; it must not become zero automatically.
- **Target changes:** the effective-dated target that applied at the time governs historical completion/progress. Current-target comparisons, when shown, must be labelled separately and never rewrite historical completion (D-194/H2).
- **Unit changes:** never silently reinterpret historical values when a Quantity unit changes. Any distinction between a label correction and a true unit change must follow the approved screen/data contract; do not invent conversion factors.
- **Today summary:** show the actual with its unit and useful target context; do not reduce a measured value to generic completion or treat progress as moral worth.
- **Effort levels remain categorical:** Tiny / Usual / Extra are not secretly converted to numbers or compared as quantities.

### Scheduling model: separate the pattern from recorded activity

Recommended conceptual separation:
- **Schedule pattern:** `DAILY`, `SELECTED_WEEKDAYS`, or `FLEXIBLE_WEEKLY_TARGET` only. Every-N-days is excluded from v1 by the current approved schedule vocabulary.
- **Configuration effective date:** schedule changes use D-194's Apply today/Apply tomorrow versioning rules. Do not introduce a separate arbitrary user-selected start-date feature based on this historical notebook; creation and effective-date semantics must follow the current product/screen contracts.
- **Eligible occurrence:** whether a habit is scheduled for a given local date under the chosen pattern and start/pause/archive rules.
- **Record:** the user's saved action/quantity on a date. Records must remain distinct from eligibility.
- **Rest/no-schedule date:** a date with no eligible occurrence; it should not appear as a missed or incomplete scheduled habit.
- **Eligible but unrecorded date:** not automatically a zero record, failure, or streak break unless a separately approved feature explicitly defines such language.

### Schedule edits: recommended history-preserving rule

1. Schedule edits are effective-dated and must follow D-194: Apply-today replacement is allowed only before qualifying activity exists for today; after qualifying activity, preserve today's configuration and offer Apply tomorrow.
2. Past eligibility and saved records are not silently rewritten when the schedule changes.
3. The user may correct an individual past record, but changing a schedule must not automatically create/delete historical records.
4. If an edit would make a date's historical eligibility unclear, keep the record itself visible in History and avoid retroactive changes to counts until the product defines historical schedule snapshots.
5. Pausing stops future scheduled occurrences from appearing as actionable on Today from the pause effective date; preserve existing records. Resume asks when the habit should become active again.
6. Archiving removes the habit from active Today and reminders but preserves history and derived insights. Restore returns it to active status without deleting records.
7. Deleting is distinct from pausing/archiving and must disclose exactly which records and associated data will be removed. Do not finalise delete behavior until export/backup/privacy policy is decided.

### Former open questions — current resolution
- **Multiple entries:** resolved in H2/D-189: multiple valid numeric/duration entries for one habit/date are permitted and summed; each entry remains separately inspectable/editable. No single-daily-total-only rule.
- **Target/history semantics:** resolved in H2/D-194: Duration/Quantity have a usual target; the target effective on the activity date governs historical completion; current-target comparison is distinct.
- **Schedule effective date:** resolved in D-194: Apply-today replacement only before qualifying activity exists today; otherwise preserve today's configuration and offer Apply tomorrow. Cancel makes no change.
- **Weekly target meaning:** resolved in D-191/D-195: `FLEXIBLE_WEEKLY_TARGET` is a period goal rather than predetermined required weekdays; preferred days are optional guidance.
- **Local-date behaviour:** D-187 preserves saved local-date keys across timezone changes; actual platform/device behaviour still needs later authorised technical validation.
- **Custom Quantity units:** retain the user-defined unit; do not silently convert incompatible or changed units. Exact copy/interaction details are delegated design details unless they change scope or data meaning.

### Potential states for detailed UI review

The list below originated as a pre-contract wireframe checklist. Use the current selected screen directions and the task/accessibility scorecard in [Planning UX and Traceability Closure](34-PLANNING-UX-AND-TRACEABILITY-CLOSURE.md); revisit these states if the later real-UI review identifies a material gap. It is not a separate owner-decision backlog.

Potential states:
1. Quantity/Duration habit setup with its required usual target, compatible unit, and a clear example; Completion-only and Effort-aware modes must remain distinct.
2. Logging a number on Today, including replace/edit behavior and target-exceeded state.
3. Editing a unit or target with an explanation of what changes for historical data.
4. Editing a schedule with an effective-date summary.
5. Pausing, resuming, archiving, restoring, and deletion confirmation as distinct actions.

Use docs/27 and docs/31 for current open owner choices and gate status. Record any future consequential change in the decision log before changing the contracts. No app implementation is authorised or started.


## Owner-approved interaction direction — quick Complete and detailed Record (2026-10-18)

The owner approved a two-level interaction model. This is a product/UX requirement for planning; it is not evidence that the current preview implements it.

### Habit card actions

- Show the habit name, its measurement mode/usual target where applicable, and a clear state for today's records.
- **Complete** is the quick path. For duration and quantity habits, it records the configured usual target by default. For completion-only habits, it records completion.
- **Record** opens the detailed entry flow for that habit/date. The user can enter or correct the actual amount, use target-derived quick choices, choose a custom amount, review same-day entries, and optionally add a short note about that habit/date.
- Quick completion must not require navigating to the full detail page. Avoid accidental duplicate completions and provide a clear correction path.
- A note is optional and must not itself mark a habit complete. A habit/date note is distinct from the broader daily journal. Provide both: an optional habit/date note as the default, plus optional notes on individual entries. These remain distinct from the general daily journal.
- For duration habits, quick choices may be in minutes/hours; for quantity habits, choices use the configured unit. Custom entries must preserve the approved numeric rules.
- The interface distinguishes no record from explicit zero, partial activity, usual target reached, and above-target activity. Do not treat an absent record as zero.
- Progress labels are calculated from the actual amount versus the user's usual target. They describe measured progress, not self-reported emotion or moral worth. Exact thresholds and intermediate labels remain open.
- Multiple numeric entries on the same local date sum into a daily total. Duration sessions also sum to a daily duration total; each session remains individually inspectable and correctable.

### Interaction states to design

1. Today card with no record.
2. Quick Complete confirmation/feedback and its usual-target default.
3. Existing record, with Edit/Correct and additional Record options.
4. Detailed duration entry (quick values plus custom value).
5. Detailed quantity entry (whole-number values, unit, intentional zero).
6. Completion-only recording.
7. Optional habit/date note and note editing.
8. Multiple entries and derived daily total.
9. Error, offline persistence, undo/correction, and duplicate-action handling.
10. Partial target, usual target reached, above-target amount, rest day, paused habit, and missed opportunity.

These interaction states should be designed and reviewed before any feature implementation. Mockups are not working app functionality.


### Weekly calendar and restrained rank

- Weekly summaries and calendar groupings follow the user's chosen week-start day. The initial setting should use the device/locale convention when available and remain user-configurable.
- Undo a recent accidental quick action and support later correction/deletion of an individual entry. Avoid duplicate quick-tap records while allowing multiple legitimate sessions.
- XP/rank is exploratory only, not approved for v1. If later approved, keep it discreet near the profile and celebrate rank changes without pressure. Online leaderboards and other users' streaks are deferred pending a separate privacy, safety, identity, and networking decision.
