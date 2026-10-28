# Today Dashboard: Comparative UI Concepts

**Research date:** 2026-10-17  
**Status:** Concept exploration; no layout, navigation, visual token, or copy is approved.  
**Purpose:** Continue the Dayseed UI research by comparing different Today-screen priorities before committing to a visual direction or building app UI.

**Current-authority note (2026-10-28):** This is a cumulative design notebook with dated proposals; old “decisions still needed” and “next design iteration” sections are historical snapshots, not a live backlog. For current habit modes, schedule types, date/history policy, and Complete/Record semantics, use `docs/01-PRODUCT-SPECIFICATION.md`, `docs/09-DECISION-LOG.md`, and `docs/26-SCREEN-AND-DATA-CONTRACTS.md`. The current contract in docs 01/09/26 and approved schedule decisions D-191/D-195 supersede earlier tentative ideas about one numeric total per day, optional numeric targets, decimal quantities, arbitrary start dates, and every-N-days schedules. The owner-selected daily summary + one-line monthly context + quiet route to Progress remains the working Today direction; final visual/accessibility approval is still open. The two rendered alternatives are linked in `docs/29-UX-DESIGN-APPROVAL-PACK.md`.

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

The Growth Mark should not be given a large dashboard footprint until its purpose is chosen and understood. Research in [15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md) recommends comparing **Collected Moments** with a plain editorial summary. Prototype both in Concepts A and B.

Every number or visual mark must map to an inspectable, explicitly logged event and show its period. An unrecorded day must not be presented as failure. Journal activity must remain distinct from habit activity unless a future, explicit decision says otherwise.

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

## Suggested next sequence

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

### Exit gate for Screen 1

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

- Keep the daily summary + one-line monthly context + quiet Progress route as the selected design direction.
- The generated board repeats monthly context on multiple states; in a real interface this should remain concise and visually secondary, and should not be repeated if it creates clutter.
- The generated rest-day illustration is more decorative than necessary. Keep the existing editorial reference restrained; illustration is optional and must earn its space.
- The generated mockup includes specific example counts and sample journal content. These are placeholders, not real user data or approved copy.
- The mockup uses a date and device/status-bar details that are illustrative only. Date must be the device's local date; do not hard-code the sample date.
- Avoid relying on colour alone to distinguish recorded states. Combine labels, shape, and accessible state semantics.
- Do not infer that a habit is “completed” from a journal entry.
- Daily count denominator, habit schedule semantics, skipped/missed/rest states, time-zone boundaries, and monthly distinct-date counting remain to be specified before implementation.

### Next design task

Move to **Screen 2: Habit Creation / Edit**, keeping Today as the reference skeleton. The habit editor must make these choices understandable:
- Name and optional description.
- Icon/appearance choice that avoids overwhelming icon catalogues.
- Completion-only versus effort-level logging.
- Tiny / Usual / Extra labels and optional targets.
- Schedule, rest days, and how a habit appears on Today.
- Optional reminders, with permission timing and quiet hours still to be researched.
- Editing, pausing, archiving, and deleting a habit, including effects on history.

Before finalising that screen, resolve the product semantics that affect both creation and Today; do not encode undecided schedule or record rules into the UI as if they were approved.


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

This is a direction to explore, not yet a complete data contract. For number-based habits, the next research must define whether users can log partial quantities, whether a target is optional or required, whether exceeding a target is allowed, unit changes, daily reset/aggregation, and how a record is represented when there is no quantity yet. Do not imply that effort-level records, completion-only records, and numeric records are interchangeable without explicit rules.

### Scheduling: progressive disclosure
The owner wants **selected weekdays plus an optional start date as the default**, with an unobtrusive expandable control for more flexibility.

Default collapsed view:
- Weekday selector.
- Optional start date.
- One short human-readable summary.
- A clearly labelled secondary affordance such as “More schedule options” with an expand/collapse indicator.

Expanded advanced options should be explored to include:
- Weekly target (e.g. choose any 3 days per week).
- Every-N-days interval patterns (e.g. every 2 or 3 days).
- Clear schedule summary and a way to collapse advanced options without losing selections.

### Design constraints
- Advanced scheduling must be discoverable but must not overwhelm first-time habit creation.
- State exactly what a pattern means in plain language and show a summary before saving.
- Avoid silently converting between weekday schedules, weekly targets, and interval schedules.
- Decide how schedule edits affect past dates and eligibility before implementation.
- Handle no weekdays selected, start dates in the past/future, time-zone/local-date boundaries, paused/archived habits, and rest days.
- All schedule controls need accessible labels, non-colour state cues, large-text reflow, and usable touch targets.
- Keep all core scheduling functions available offline.

### Next design iteration
Produce a focused comparative wireframe for:
1. Default weekday picker + optional start date, advanced options collapsed.
2. Expanded view showing weekly-target and every-N-days alternatives.
3. Review/save state with a human-readable schedule summary.
4. Edit-existing-habit state, explicitly distinguishing future schedule changes from historical records.

The owner-selected direction is to be recorded in the decision log only after the exact semantics are reviewed and confirmed. No app implementation is authorised or started.


## Screen 2 follow-up: number-based records and schedule-change semantics

**Date:** 2026-10-18  
**Status:** Proposed product contract for owner review. These rules are design recommendations, not approved implementation requirements.

### Number-based tracking: recommended first-pass contract

A number-based habit stores a quantity with a user-defined unit (for example, pages, minutes, glasses, repetitions). It may optionally have a target.

Proposed behavior:
- **Unit required:** the unit tells users what the number means. Offer a short editable text unit, with suggestions only as shortcuts.
- **Target optional:** a target gives context but should not be required to record an amount. Distinguish “recorded 4 pages” from “reached target of 10 pages.”
- **Partial values allowed:** allow zero only if zero is meaningful and intentionally recorded; allow decimals where units reasonably need them. Avoid accidental blank or invalid entries.
- **Target exceeded:** accept values above target. Never cap or discard the actual amount just because the target was exceeded.
- **Record one daily aggregate, with corrections:** recommended initial UX is one current quantity per habit per local calendar date, editable later. If the user enters a new amount after recording, ask whether they are replacing the day's total or adding to it only if both interpretations are genuinely needed; avoid ambiguous silent behavior. Recommended default: edits replace the saved daily total, not add to it.
- **No value is not zero:** an unrecorded date remains unrecorded; it must not become zero automatically.
- **Target changes:** new target applies prospectively by default. Historical recorded quantities remain unchanged; historical target snapshots versus current-target comparison remains an open data/reporting choice.
- **Unit changes:** warn that changing units may make old values incomparable. Prefer asking whether the change is only a label correction or a true unit change; do not convert historical data without explicit conversion rules.
- **Today summary:** number-based values should be presented with their unit and optionally target context (e.g. “4 of 10 pages” only if the ratio is meaningful). They should not be reduced to generic completion without explaining the distinction.
- **Effort levels remain categorical:** Tiny / Usual / Extra are not secretly converted to numbers or compared as quantities.

### Scheduling model: separate the pattern from recorded activity

Recommended conceptual separation:
- **Schedule pattern:** selected weekdays, weekly target, or every-N-days interval.
- **Start date:** optional user-selected local calendar date; define the default when omitted.
- **Eligible occurrence:** whether a habit is scheduled for a given local date under the chosen pattern and start/pause/archive rules.
- **Record:** the user's saved action/quantity on a date. Records must remain distinct from eligibility.
- **Rest/no-schedule date:** a date with no eligible occurrence; it should not appear as a missed or incomplete scheduled habit.
- **Eligible but unrecorded date:** not automatically a zero record, failure, or streak break unless a separately approved feature explicitly defines such language.

### Schedule edits: recommended history-preserving rule

1. A schedule edit changes future eligibility from an explicit effective date, defaulting to the next local date after save unless the user deliberately selects a future effective date.
2. Past eligibility and saved records are not silently rewritten when the schedule changes.
3. The user may correct an individual past record, but changing a schedule must not automatically create/delete historical records.
4. If an edit would make a date's historical eligibility unclear, keep the record itself visible in History and avoid retroactive changes to counts until the product defines historical schedule snapshots.
5. Pausing stops future scheduled occurrences from appearing as actionable on Today from the pause effective date; preserve existing records. Resume asks when the habit should become active again.
6. Archiving removes the habit from active Today and reminders but preserves history and derived insights. Restore returns it to active status without deleting records.
7. Deleting is distinct from pausing/archiving and must disclose exactly which records and associated data will be removed. Do not finalise delete behavior until export/backup/privacy policy is decided.

### Important unresolved questions
- Should multiple numeric entries on the same day be supported in v1, or should there be one editable daily total only? Recommendation: one editable daily total initially.
- Should historical insights compare values against the target that existed at the time or the current target? Recommendation: preserve historical target context if the data model can do so simply; otherwise be explicit that charts use the current target.
- When the user edits a schedule, should the change be effective immediately or from tomorrow? Recommendation: from the next local date by default, with an explicit “apply today” option only if a strong use case emerges.
- How do weekly-target schedules define eligibility for each day? A weekly target is an aim over a period, not a predetermined set of weekdays; the UI and metrics must not falsely label individual unchosen days as missed.
- How are daylight-saving/time-zone changes handled for local dates? The date of a record should remain the date on which the user intentionally recorded it, not shift when travelling.
- Should custom units be normalised or left as user-defined strings? Recommendation: user-defined string plus optional suggestions; don't silently convert units.

### Design work to do next
Create focused wireframe states for:
1. Number-based habit setup with unit, optional target, and a clear example.
2. Logging a number on Today, including replace/edit behavior and target-exceeded state.
3. Editing a unit or target with an explanation of what changes for historical data.
4. Editing a schedule with an effective-date summary.
5. Pausing, resuming, archiving, restoring, and deletion confirmation as distinct actions.

Before locking these flows, review the proposed contract with the owner and reflect any confirmed choices in the decision log and product specification. No app implementation is authorised or started.


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
