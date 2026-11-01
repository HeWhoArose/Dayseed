# Dayseed — Current Findings and Proposed Screen Architecture

**Prepared:** 2026-11-01  
**Repository:** [HeWhoArose/Dayseed](https://github.com/HeWhoArose/Dayseed)  
**Source baseline inspected:** `main` at `334679775643ffb43f384646cf7cced75d401f96`  
**Purpose:** Consolidate the recent competitor/UI review and source-code findings, then record a proposed screen and navigation structure before the next implementation-completion audit.

> **Scope boundary:** This is a findings and product-structure note. It is not the next audit's detailed execution plan, feature implementation authorisation, final visual design approval, or evidence that the app currently passes build, device, notification, accessibility, privacy, or release testing. Recommendations are labelled as proposals where they change an existing decision.

---

## 1. Executive summary

Dayseed already contains substantial domain logic, persistence, tracking, history, journaling, comeback handling, metrics, backup/export work and Compose UI. The principal weakness identified in the source review is not simply a lack of features. It is the gap between:

1. functionality that exists in the data/repository/ViewModel layers;
2. controls and screens that expose that functionality to the user; and
3. the complete, testable journey from beginning an action to reviewing or correcting its result.

The Today screen also carries too many different jobs in one vertical feed. It acts as a dashboard, habit tracker, progress summary, comeback surface and reflection entry point. Individual habit cards contain many controls and record details. Meanwhile, there is no dedicated destination where a user can browse and manage the complete habit collection independently of today's schedule.

The proposed structural direction is:

- Keep **Today** focused on what the user can do today.
- Introduce a dedicated **Habits** destination for the complete habit collection and management.
- Keep **History** responsible for historical habit records and the hybrid Journal Archive.
- Keep **Insights** responsible for summaries and the detailed Growth Mark experience.
- Keep **Settings** as a concise hub with focused sub-pages/panels.
- Use dedicated detail routes for complex work rather than adding a bottom tab for every feature.

A proposed five-destination bottom bar is recorded below. This changes the previously selected N1 four-destination navigation and must be explicitly reconciled by the owner before it is treated as the approved navigation.

## 2. Repository and evidence baseline

The source review was based on the actual Kotlin/Jetpack Compose application, not only planning notes. The inspected project uses four bottom destinations in `MainActivity.kt`:

- Today
- History
- Insights
- Settings

The current screen and component structure reviewed includes:

- `TodayScreen.kt`, `HabitCard.kt`, `GrowthMark.kt`, `ReflectionCard.kt`, `ComebackCard.kt`
- `AddEditHabitDialog.kt`, `HistoryScreen.kt`, `JournalEditorDialog.kt`, `InsightsScreen.kt`
- `SettingsScreen.kt` and the settings panels/support components
- `MainActivity.kt`, `DayseedViewModel.kt`, `DayseedRepository.kt`
- `DayseedDatabaseHelper.kt`, `ReminderEngine.kt`, the application manifest and Gradle configuration
- Existing unit/acceptance tests.

The findings below describe source-level behaviour at the inspected baseline. The next completion audit must refresh the repository head and verify each item again before implementation decisions are made. A method being present or a repository test passing is not, by itself, evidence that a complete Android user journey works.

### Evidence and validation limits

- This note does not claim that a fresh Gradle build, test suite, APK install or device session was run while preparing it.
- The repository records historical forced test runs for the Settings restructuring at commit `0c4933e` (26/26 tests) and the daypart-visual work at commit `137790d` (19/19 tests). Those are historical, commit-specific self-check records, not a new test result for the current baseline.
- The current app source includes the Settings hub/panel restructure recorded under D-212. The old description of Settings as one long, bare wall of controls should not be repeated as the current UI state.
- Competitor-store evidence is a mix of listing screenshots and qualitative review observations. It is not a systematic, representative review sample or an interactive usability study of installed current versions.
- No note in this document should be used to claim that reminders, privacy behaviour, backups, migration, accessibility or release signing have passed on a physical Android device.

---

## 3. What Dayseed already has

The source and existing planning documents show a meaningful base worth preserving.

### 3.1 Habit tracking and scheduling foundations

- Four tracking modes: **Effort-Aware**, **Completion Only**, **Quantity**, and **Duration**.
- Multiple quantity/duration entries per habit per local date, with sums calculated from saved entries.
- Explicit zero can be distinguished from no record.
- Daily and flexible weekly scheduling, plus selected weekdays.
- Partial-progress classification and non-punitive language.
- Preferred weekdays for flexible weekly schedules are guidance, not restrictions.
- Effective-dated habit configuration is used so prospective target/schedule edits need not rewrite historical meaning.
- Habit lifecycle states include Active, Paused, Completed and Archived in the model/schema.

### 3.2 Reflection and personal history

- Daily journal entries and drafts.
- Mood fields and prompts.
- Habit-linked retrospective reflection entry points.
- A Journal Archive view with text search and mood filters.
- Habit/date notes, distinct from the general journal and from per-session entry notes.

### 3.3 Growth, comeback and reporting foundations

- Comeback experience and eligibility logic.
- Growth metrics and duration aggregation.
- Growth Mark G2 summary data and G3 date/session count data foundations.
- Mood Insights and a 30-day rhythm presentation.
- Weekly opportunity/completion calculations in repository logic.

### 3.4 Data and recovery foundations

- Local SQLite persistence and migration code.
- Undo-token infrastructure for supported tracking mutations.
- Portable backup/restore and export code, including encrypted backup handling.
- Restore-conflict and cancellation-outbox concepts in the data layer.
- Settings hub with bottom-sheet panels and a full Backup & Restore sub-screen, as recorded by D-212.

These are substantial foundations. The next audit should distinguish reusable implementation from incomplete or incorrect wiring rather than rebuilding functioning systems unnecessarily.

---

## 4. Confirmed source-level findings

The following were identified by inspecting the application source. “Confirmed” means the source pattern was present at the inspected baseline; it does not mean the complete correction has been implemented or independently runtime-tested.

### 4.1 Backend functionality with no complete UI connection

| Finding | Source evidence | User impact / required outcome |
|---|---|---|
| Individual quantity/duration entry editing is not exposed | `DayseedRepository.updateHabitEntrySync()`, the suspend repository wrapper and `DayseedViewModel.updateHabitEntry()` exist. `HabitCard` entry rows expose Delete but no Edit action; Today and History do not pass an update callback into the card. | Users must delete and re-create an entry to correct its amount or note. Add an edit flow for one existing entry, preserving its identity and guarded undo behaviour. |
| Per-entry notes cannot be entered when logging | `habit_entries.entry_note`, repository save/load/update logic and card display support the field. The custom amount form submits `onAddEntry(amount, null)`; quick-add actions also pass null. | Add an optional session-note field to detailed logging and correction. Keep it distinct from the habit/date note and the daily journal. |
| Completed lifecycle state is not available as a normal habit action | `HabitLifecycleState.COMPLETED` exists in the model and database constraint. The HabitCard menu exposes edit, pause/resume and archive/restore, but no Mark completed action. | Add a deliberate completion lifecycle action and a discoverable Completed collection. Completion must remain distinct from archive and permanent deletion. |
| Several GrowthMetrics fields are not surfaced | Repository computes `currentPeriodCompletions`, `currentPeriodOpportunities`, `totalHabitsCompleted` and `journalEntriesCount`; the current GrowthMark/Insights UI does not display all of them. | Decide which metrics are meaningful, define them precisely and display only metrics that help a user understand progress. |
| Reminder evaluation is not a working Android notification service | `ReminderEngine.evaluateHabitReminder()` and `DayseedRepository.evaluateTodayRemindersSync()` calculate eligibility and write delivery-ledger states. The inspected app source did not show a complete notification-channel/posting and scheduling path, nor a runtime call that drives the daily evaluation as a real reminder service. | Implement and test the complete OS delivery lifecycle. A database row marked DELIVERED must not be confused with a notification actually posted by Android. |

**Metric-definition caution:** `totalHabitsCompleted` is incremented from qualifying outcome and positive entry records. It should not be presented as a count of unique habits completed unless the calculation is changed to count unique habit identities according to a clearly documented period/rule.

### 4.2 Flexible weekly target defect

The database constraint permits weekly targets for all four tracking modes and requires a positive target value for weekly Completion Only and Effort-Aware habits.

The current UI exposes its target input only for Quantity and Duration. Its save logic sets the target value to null for Completion Only and Effort-Aware. Repository normalization also sets those values to null. This conflicts with the database's weekly-target constraint.

**Impact:** Selecting Flexible Week for a Completion Only or Effort-Aware habit can produce a database constraint failure rather than a valid habit.

**Required outcome:**

- When Flexible Week is selected for Completion Only or Effort-Aware, show a clearly labelled weekly completion-count field (for example, “How many days per week?”).
- Validate 1–7.
- Persist the value consistently through the ViewModel, repository, effective-dated version and SQLite constraint.
- Count qualifying completed dates, not quantity values or effort levels converted into invented numbers.

### 4.3 Flexible weekly numeric progress is calculated from today's entries in Today

TodayScreen filters entries to the current local date and supplies that list to HabitCard. HabitCard sums the supplied entries against the habit target.

For a flexible weekly Quantity/Duration habit, this means the card can show today's total as though it were the whole-week total. For example, 20 minutes logged on Monday can appear as 0/60 on Tuesday.

**Required outcome:** Calculate progress from the configured week start through the relevant date, while preserving today's individual sessions as a separate list. The card should state the period explicitly, for example “This week: 20 / 60 min”. Completion-only and effort-aware weekly goals should show qualifying completed days out of the weekly target.

One shared weekly-progress calculation should be used across Today, History and Insights so those screens cannot disagree.

### 4.4 Growth Mark G2/G3 interaction is incomplete

The Today screen currently displays a Collected moments summary, a non-interactive strip of date/session indicators and an information dialog. The indicators do not open date details.

Insights includes a 30-day rhythm visual, but it is not the specified month-navigation and date/session inspection experience.

The product contracts and D-199/D-201 planning direction call for G3 month navigation, date selection, per-date duration totals and inspection of the underlying duration sessions, with correction/deletion paths where appropriate.

**Required outcome:** Provide a dedicated Growth Mark detail route, most naturally opened from the Today summary and reachable from Insights. It should show a calendar month, previous/next month navigation, selectable dates, date totals and the source duration sessions for the selected date.

### 4.5 History navigation is limited to the last 30 dates

The current Daily Log constructs a fixed list from day 0 through day 29. The screen does not provide month navigation or an unrestricted date picker for older history.

**Required outcome:** Add month/year navigation, a calendar/date selector and a Jump to Today action. Older saved records must remain accessible and correctable.

### 4.6 Journal Archive is missing required affordances

The current Journal Archive provides search, mood filters, filtered entries and an empty state. It does not provide the required prominent New entry action or a Clear search/filter action when a search/filter combination produces no results.

**Required outcome:**

- Put a visible New entry action in the Journal Archive itself.
- When filtered results are empty, provide one action that clears the query and mood filter.
- Keep the archive's selected A2 hybrid direction: chronological entries with date jump/calendar support and local title/body search.

### 4.7 Saved journal deletion is immediate

The current JournalEditorDialog invokes the delete callback directly from its trash action. It does not first ask the user to confirm.

The existing contract requires an explicit warning because a saved journal deletion is not covered by the normal tracking snackbar undo.

**Required outcome:** Ask “Delete this entry? This cannot be undone.” Provide Cancel and Delete actions. Cancel must leave the entry intact.

### 4.8 Unscheduled active habits are absent from Today

TodayScreen filters its Active habit collection to habits scheduled for the current date. An active Monday/Wednesday/Friday habit is therefore absent from Tuesday's Today list.

That makes it harder to inspect or manage the habit and prevents the user from easily recording a genuine bonus session on a non-scheduled day.

**Required outcome:** The dedicated Habits destination should always show all active habits. Today may also include a small, collapsible “Not scheduled today” group or a link to Habits. Any bonus logging must remain clearly distinguished from a scheduled opportunity; a rest/non-scheduled day must remain neutral.

### 4.9 Habit ordering and management are limited

The inspected habit list is based on creation ordering. There is no complete, dedicated active-habit management destination or user-facing manual reorder control in the reviewed source.

**Required outcome:** Provide a central habit collection where users can find every habit, change its order, inspect its schedule/status and manage its lifecycle without depending on whether it is scheduled today.

### 4.10 Today and HabitCard carry too many simultaneous jobs

Today currently combines:

- time-aware date/greeting and artwork;
- onboarding, when applicable;
- undo feedback;
- comeback presentation, when applicable;
- Growth Mark summary;
- today's scheduled habit list and progress count;
- mode-specific habit controls and recorded-entry rows;
- retrospective prompt affordances; and
- the daily reflection card.

HabitCard itself supports several actions and states: habit menu, date note editing, tracking controls, quick numeric additions, custom amount, progress bands, entry rows/deletion, outcome state changes and retrospective prompting.

This is a strong feature set, but the composition creates a risk of excessive vertical length and visual density, especially with several habits or large text.

**Required direction:** Progressive disclosure. Show the habit name, today's state, period-appropriate progress and the most useful action first. Put detailed recording, entry correction, notes, history and lifecycle management behind a clear secondary action or expanded state. Do not remove functionality merely to make the card smaller.

### 4.11 Release and validation caveats

The source review also identified release and quality obligations that should not be hidden by a visual redesign:

- The inspected Gradle release signing configuration used the debug signing configuration. This must be corrected and verified before a release candidate is considered ready.
- The repository's tests are mainly unit/Robolectric and acceptance-oriented. The inspected project did not contain a corresponding Android instrumented Compose UI test suite. New screen flows need appropriate UI-level verification.
- The reminder evaluator tests do not demonstrate OS notification delivery.
- An absent widget implementation should be treated as a future feature opportunity, not a broken existing UI promise.

---

## 5. Competitor observations to retain

Competitor comparison is useful for design decisions, but evidence quality must remain visible. Store screenshots are publisher marketing material, not proof of live app behaviour. Review comments are individual experiences, not representative statistics.

| Competitor | Useful pattern / apparent strength | Caution or lesson for Dayseed |
|---|---|---|
| Loop Habit Tracker | Compact habit list/weekly grid; a separate habit detail with progress/history charts; straightforward tracking and custom schedules. | A grid can compare many habits efficiently but can become dense on a small phone. Progress measures need clear definitions and must not make missed days feel punitive. |
| HabitNow | Strong focus on habit/routine management, scheduling, reminders and customisation. | Do not cram every configuration and statistic into the daily card. Keep management and detailed habit information on their own surfaces. Review feedback about reminder behaviour and weekly goals needs direct, current verification before being treated as a broad conclusion. |
| Daylio | Low-friction mood/reflection entry, calendar-based revisiting and visual summaries. | Dayseed's journal should be easy to start and retrieve without forcing users through Today or turning mood into a judgement about habit success. |
| TickTick | Broad combination of tasks, calendar, reminders and habits. | Feature integration can be useful, but Dayseed should not reproduce the full breadth if it makes primary habit actions harder to discover. |

The previous Play Store comparison and review pass suggested recurring user appreciation for quick logging, usable statistics, scheduling flexibility, reminders, widgets, customisation and journal retrieval. Some criticism concerned notification behaviour, schedule/weekly-target limitations, visual contrast and the complexity that can come with feature density. These are qualitative signals only. Do not carry over exact ratings/review counts as stable benchmarks; they vary by storefront and time.

**Existing repository research:**

- [Competitor Screenshot Audit](18-COMPETITOR-SCREENSHOT-AUDIT.md) contains inspectable listing-screen evidence for Loop and Daylio and explicitly states its limitations.
- [Research and Competitors](10-RESEARCH-AND-COMPETITORS.md) remains a research-plan document, not a completed market study.

The design lesson is not “copy the best competitor”. Dayseed should combine rapid habit logging, strong habit management, clear history and reflection while retaining its own nonjudgmental, local-first identity.

---

## 6. Proposed information architecture

### 6.1 Recommended primary navigation

**Proposal: five bottom-level destinations**

1. **Today** — what matters now.
2. **Habits** — the complete habit collection and management.
3. **History** — historical habit records and Journal Archive.
4. **Insights** — weekly/monthly summaries, mood insights and Growth Mark detail.
5. **Settings** — preferences, data, backup/restore and app information.

This proposal responds to a real information-architecture gap: Today only shows habits scheduled for the current date, while users need a stable place to find and manage every habit.

**Decision conflict to preserve:** The owner previously selected N1, a four-destination bottom navigation (Today, History, Insights, Settings), with Journal under History and a Write reflection action on Today. Adding Habits as a fifth bottom destination modifies N1. This note recommends evaluating the five-destination variant, but does not silently supersede N1 or mark the new navigation as approved.

If the owner prefers to preserve N1 exactly, the fallback is a dedicated full-screen Habits route opened from a clearly labelled “Manage habits” action on Today. The Habits page should still exist; the open decision is whether it is a bottom-level tab or a secondary route.

**Journal remains inside History** in this proposal. It should be strongly reachable from Today and from the History screen, but does not need another bottom tab at this stage. This preserves the A2 hybrid archive direction and avoids overcrowding the bottom bar.

### 6.2 Proposed screen map

| Screen / route | Main responsibility | Key content and actions | Entry points |
|---|---|---|---|
| **Today dashboard** | Daily action centre | Compact greeting/date/artwork; scheduled habits; a small daily progress summary; a compact weekly-goal cue; quick reflection action; optional comeback/undo notices. | Default app destination. |
| **Habits hub** *(new)* | Complete habit collection and management | All active habits regardless of today's schedule; groups/filters for Active, Paused, Completed and Archived; search; manual reorder; create habit; open details; lifecycle actions. | Proposed bottom tab or prominent Manage habits route. |
| **Habit detail** *(new)* | One habit's complete context | Name/description, tracking mode, target and schedule, preferred days, reminder status where supported, current weekly/daily progress, historical activity, individual entries/outcomes, notes and lifecycle actions. | Tap a habit from Habits; optional route from a Today card. |
| **Create/edit habit** *(rework)* | Full configuration | Tracking mode; mode-appropriate target; schedule; weekly target count; preferred days; reminder time; effective-date choice; validation and save. | Habits hub, Today empty state, Habit detail. |
| **Record session / correct entry** *(rework)* | Detailed logging and correction | Actual amount; optional per-session note; existing sessions for the selected habit/date; edit/delete a specific entry; explicit date when backdating; guarded undo where supported. | Record action on Today/Habit detail/History. Prefer a focused bottom sheet or dedicated route over an overloaded card. |
| **History — Habit Log** *(rework)* | Historical habit activity | Month navigation, calendar/date selection, Jump to Today, daily results, partial/met/above-target distinctions, entry correction and historical backdating. | History destination. |
| **History — Journal Archive** *(rework)* | Find and revisit reflections | A2 hybrid: recent chronological entries, compact calendar/date jump, local title/body search, mood labels, New entry, edit and confirmed delete. | History destination, Today Write reflection action. |
| **Journal editor** *(rework)* | Create/edit one reflection | Date, title/body, optional prompt, mood fields, linked habit where relevant, saved/draft state, clear save and delete-confirmation behaviour. | Today, History Journal Archive, comeback/retrospective entry point. |
| **Insights overview** *(refine)* | Explain patterns without judgement | Weekly review; daily target results kept distinct from weekly target progress; mood summaries; clearly defined activity metrics; transparent period labels. | Insights destination. |
| **Growth Mark detail (G3)** *(new / incomplete)* | Inspect collected duration | Calendar-month navigation, date totals, selectable days and underlying duration sessions with correction/deletion entry points. | Tap G2 on Today or relevant Growth Mark summary in Insights. |
| **Settings hub** *(existing restructure)* | Find preferences and data controls | Concise state-summary rows and focused panels. Keep Backup & Restore as its own sub-screen and destructive delete-all separated. | Settings destination. |
| **Settings panels/sub-pages** *(existing/review)* | One settings topic at a time | Appearance, week start, quiet hours, journal export, privacy, removed habits, About and Backup & Restore. A full reminders-management surface remains a separately deferred owner decision in D-212. | Settings hub. |
| **Comeback surface** *(existing, refine)* | Gentle re-entry | Compact, dismissible, non-blocking experience with the approved recovery choices. Not a permanent navigation destination. | Triggered from app state. |

### 6.3 Habits hub: proposed content and behaviour

The Habits hub is the most important proposed new screen.

It should solve a different problem from Today:

- **Today answers:** “What can I do today?”
- **Habits answers:** “What habits do I have, and how do I manage them?”

Suggested structure:

1. Page heading **Habits**, with a prominent Add habit action.
2. A compact searchable/filterable collection.
3. **Active** as the default collection. Include both habits scheduled today and active habits not scheduled today.
4. A clearly distinguished “Not scheduled today” state for active habits whose schedule does not include today. It must not look like a missed task.
5. Separate collapsible groups or filters for **Paused**, **Completed** and **Archived**.
6. Manual reordering of active habits, if accepted and supported by a persisted ordering field.
7. Each row/card gives quick access to details and lifecycle management without placing every advanced control permanently on screen.

The Habits hub must not introduce new schedule types by implication. The current approved schedule vocabulary remains Daily, Selected Weekdays and Flexible Weekly Target, with the saved week-start preference respected.

### 6.4 Habit detail: why it deserves its own page

A dedicated Habit detail page is justified because the current HabitCard is being asked to serve simultaneously as a dashboard card, logger, progress report, session list, note editor and management menu.

Habit detail should centralise:

- Habit description and tracking mode.
- Current target, schedule and preferred-day guidance.
- Today's result and current flexible-week progress, with period labels.
- A compact progress/history view.
- Individual entries or outcomes, each openable for correction.
- Habit/date notes and session notes as distinct concepts.
- Edit, pause/resume, mark completed, archive/restore and permanent delete actions with appropriate safeguards.

This is a detail and management surface, not another bottom tab.

### 6.5 Create/Edit and Record: separate complex actions from the dashboard

The current AddEditHabitDialog contains multiple configuration decisions. A dedicated full-screen Create/Edit route is proposed so that fields can be grouped and validated without turning the dialog into a long form.

Detailed numeric logging should be a focused Record flow. The quick Today action remains fast; users only enter the detailed flow when they need an actual amount, session note, entry correction or historical date.

Do not force every daily action to navigate to Habit detail.

### 6.6 History and Journal

History should provide two clearly labelled areas, for example:

- **Habit Log**
- **Journal Archive**

The Habit Log must browse beyond 30 days. The Journal Archive retains the selected A2 hybrid layout: chronological saved entries plus compact calendar/date jump and local search.

Journal remains reachable from Today through Write reflection and from History through New entry. A separate Journal bottom tab is not recommended unless task testing shows the History-based placement is materially undiscoverable.

### 6.7 Insights and Growth Mark

Insights should answer “What does my recorded activity show?” rather than repeat the daily habit list.

Suggested hierarchy:

- Weekly overview: scheduled-day results, partial activity and flexible weekly target completion reported separately.
- Mood insight summary.
- Other metrics only after their definitions are clear.
- Growth Mark/Collected moments summary with a link to G3 detail.

G3 belongs in a dedicated calendar/date-detail route. It should not be compressed into a tiny row of non-interactive dots inside Today.

### 6.8 Settings

The Settings hub has already been restructured under D-212. Preserve that approach: a short list of meaningful rows, current-state summaries and focused panels/sub-pages.

Do not regress to one long screen of all controls. Backup & Restore is a full sub-screen, and destructive delete-all remains separately presented.

Reminder configuration needs special handling: D-212 records that a full reminders Settings surface was deferred pending a separate owner decision. Per-habit reminder controls and actual Android delivery still need to be reconciled with the product contracts and code findings. This note does not silently approve a new global reminders page.

---

## 7. Proposed Today simplification

The goal is not to remove the capabilities already built. It is to change when and where they appear.

### 7.1 Proposed hierarchy

1. **Compact header:** date, greeting and daypart artwork. Keep the current selected daypart identity; reduce decorative height if it competes with the task list.
2. **Today's progress:** one short, precise summary (for example, “2 of 4 scheduled habits recorded”), not a large analytics dashboard.
3. **Today's habits:** the main content, with each card showing only its identity, relevant progress/period and primary Complete or Record action.
4. **Weekly goal cue:** for flexible weekly habits, show this week's accumulated progress rather than reusing today's entries as the denominator.
5. **Secondary details:** session history, per-entry notes, corrections, date notes, effort/outcome changes and management actions appear after the user expands a card or opens Record/Habit detail.
6. **Not scheduled today:** optional small collapsed section or link to Habits. Keep it neutral and secondary to today's scheduled tasks.
7. **Reflection:** an obvious, lightweight Write reflection action. The full editor opens only when requested.
8. **Conditional notices:** undo and comeback surfaces should remain visible when needed but should not permanently occupy the page.

### 7.2 Card principles

- Default card state should be compact and scannable.
- Primary action should depend on the tracking mode.
- Numeric/duration cards must clearly distinguish today's amount from the current week's total.
- Do not display numeric controls for Completion Only or Effort-Aware.
- Do not show every recorded session in full on a long Today list; show a concise summary and provide “View sessions”.
- Entry correction and deletion must be available without forcing the user to delete and recreate an entry.
- Habit management should not be duplicated across multiple oversized cards.
- Use progressive disclosure rather than hiding critical functionality in ambiguous icons.

---

## 8. Design principles to preserve

1. **Action-first, not dashboard-first.** The next useful habit action should be easier to find than secondary analytics.
2. **Progressive disclosure.** Show the essential action first; place detailed controls in the relevant detail/record flow.
3. **Nonjudgmental history.** Rest/non-scheduled days are neutral; paused periods are excluded; missed status is derived, never manually imposed; a journal note never makes a habit complete.
4. **No false aggregation.** Daily progress, weekly targets, duration sessions, mood entries and habit counts are different measures.
5. **Correction is a normal feature.** Users can inspect and correct an individual record. Preserve stable identity and guarded undo.
6. **Offline-first.** Core habits, journaling, history and insights remain local-first and do not require an account or network.
7. **A coherent visual identity.** Preserve the selected synthesis: Field Journal warmth, Living Editorial hierarchy and Quiet Instrument discipline. The selected Soft Botanical palette direction and daypart artwork concept are in the decision history; exact tokens, font choices and accessibility pairings must not be treated as approved merely because provisional values exist.
8. **Accessible at real phone sizes.** Large text, keyboard-open layouts, TalkBack, contrast, touch targets, reduced motion, empty/error states and destructive confirmations require actual verification.

---

## 9. Existing decisions and boundaries that this note must not silently change

| Existing decision / constraint | Treatment in this note |
|---|---|
| N1: four bottom destinations (Today, History, Insights, Settings) | Proposed five-tab navigation adds Habits and therefore requires explicit owner reconciliation. |
| A2: hybrid Journal Archive under History | Preserved. No separate Journal tab recommended at this stage. |
| W1: Action-first Today | Preserved, with a proposed simplification of the current card/feed hierarchy. |
| G2/G3: Collected moments summary plus calendar/date/session detail | Preserved; G3 needs a proper detail route. |
| D-191/D-195: Daily, Selected Weekdays, Flexible Weekly Target | Preserved. Do not reintroduce every-N-days schedules without a new decision. |
| D-194: effective-dated habit changes and Apply today / Apply tomorrow rules | Preserved. Historical records must not be rewritten implicitly. |
| D-186 and the local-first privacy policy | Preserved. No accounts, cloud sync or implicit personal-data uploads are introduced by this screen proposal. |
| D-200: ads deferred for initial v1 | Preserved. No new ad placement is proposed here. |
| D-212: Settings hub with focused panels; reminder settings surface deferred | Preserved. Do not silently add a global reminder panel as an approved requirement. |
| XP/rank and online/social features | Remain outside approved initial-v1 scope. |
| Proposed schema and unverified runtime behaviour | Remain subject to their existing approval and validation boundaries. |

---

## 10. Outstanding product choices raised by this note

These are proposed choices for owner reconciliation, not decisions made by this document.

1. **Habits navigation:** Should Habits become a fifth bottom tab, or remain a dedicated secondary route reachable from Today? Recommendation: evaluate the fifth tab because habit management is a primary user job and all habits need a stable home.
2. **Habit management groups:** Should Completed and Archived be separate filters/sections within Habits, while Settings retains a Removed habits shortcut? Recommendation: Habits is the canonical place for all habit lifecycle states; Settings may link to it rather than duplicate management.
3. **Habit detail entry point:** Recommendation: opening any habit from Habits opens a full detail page; Today keeps its quick action and can link to that same detail page.
4. **Create/Edit presentation:** Recommendation: use a full-screen route instead of an increasingly tall dialog.
5. **Detailed recording presentation:** Recommendation: use a focused bottom sheet/record route, with correction of existing sessions and optional per-session notes.

No additional decision should be invented about new schedule kinds, account/profile features, cloud services, social features, or monetisation while resolving these UI questions.

---

## 11. Reference documents and source files

### Product, design and contract references

- [Project Charter](00-PROJECT-CHARTER.md)
- [Product Specification](01-PRODUCT-SPECIFICATION.md)
- [UX and Design](02-UX-AND-DESIGN.md)
- [Technical Architecture](03-TECHNICAL-ARCHITECTURE.md)
- [Testing and Quality](07-TESTING-AND-QUALITY.md)
- [Decision Log](09-DECISION-LOG.md)
- [Competitor Research Plan](10-RESEARCH-AND-COMPETITORS.md)
- [Competitor Screenshot Audit](18-COMPETITOR-SCREENSHOT-AUDIT.md)
- [Today Dashboard Concepts](21-TODAY-DASHBOARD-CONCEPTS.md)
- [Implementation Reality Audit](22-IMPLEMENTATION-REALITY-AUDIT.md)
- [Weekly Goal and Reminder Contracts](23-WEEKLY-GOAL-REMINDERS.md)
- [Screen and Data Contracts](26-SCREEN-AND-DATA-CONTRACTS.md)
- [Requirement Traceability](28-REQUIREMENT-TRACEABILITY.md)
- [UX Design Approval Pack](29-UX-DESIGN-APPROVAL-PACK.md)
- [Continuation Handover](32-CONTINUATION-HANDOVER.md)
- [Time-of-Day Theme Specification](36-TIME-OF-DAY-THEME-SPECIFICATION.md)

### Main source files

- [MainActivity.kt](../app/src/main/java/com/example/dayseed/MainActivity.kt)
- [TodayScreen.kt](../app/src/main/java/com/example/dayseed/ui/screens/TodayScreen.kt)
- [HabitCard.kt](../app/src/main/java/com/example/dayseed/ui/components/HabitCard.kt)
- [GrowthMark.kt](../app/src/main/java/com/example/dayseed/ui/components/GrowthMark.kt)
- [ReflectionCard.kt](../app/src/main/java/com/example/dayseed/ui/components/ReflectionCard.kt)
- [AddEditHabitDialog.kt](../app/src/main/java/com/example/dayseed/ui/screens/AddEditHabitDialog.kt)
- [HistoryScreen.kt](../app/src/main/java/com/example/dayseed/ui/screens/HistoryScreen.kt)
- [JournalEditorDialog.kt](../app/src/main/java/com/example/dayseed/ui/screens/JournalEditorDialog.kt)
- [InsightsScreen.kt](../app/src/main/java/com/example/dayseed/ui/screens/InsightsScreen.kt)
- [SettingsScreen.kt](../app/src/main/java/com/example/dayseed/ui/screens/SettingsScreen.kt)
- [DayseedViewModel.kt](../app/src/main/java/com/example/dayseed/DayseedViewModel.kt)
- [DayseedRepository.kt](../app/src/main/java/com/example/dayseed/data/DayseedRepository.kt)
- [DayseedDatabaseHelper.kt](../app/src/main/java/com/example/dayseed/data/DayseedDatabaseHelper.kt)
- [ReminderEngine.kt](../app/src/main/java/com/example/dayseed/data/ReminderEngine.kt)

### Existing visual planning boards

- [Today variants](prototypes/dayseed-today-variants.svg)
- [Navigation and archive variants](prototypes/dayseed-navigation-archive-variants.svg)
- [Growth Mark variants](prototypes/dayseed-growth-mark-variants.svg)
- [Owner reference layout](prototypes/dayseed-owner-reference-layout.webp)

---

## 12. Status and intended use

This note consolidates source findings and proposes a clearer screen architecture. It does not replace the Decision Log, Product Specification, Screen and Data Contracts, Requirement Traceability or Continuation Handover.

**No application source, schema, database, tests or runtime behaviour was changed by preparing this note.**

The next requested activity is a refreshed implementation-completion audit using this document as an input. That audit must first refresh the current repository state, trace each finding from data layer through UI and tests, distinguish defects from accepted design decisions, reconcile the proposed Habits destination with N1, and establish what is actually complete. The detailed audit execution plan is intentionally not included here.
