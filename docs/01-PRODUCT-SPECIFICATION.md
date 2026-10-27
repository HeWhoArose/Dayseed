# Product Specification

**Status:** Draft for review; not a build approval.

## Core user jobs
- Capture a habit without configuring an elaborate system.
- Do a small version of a habit on a low-capacity day.
- Record what happened without shame or unnecessary friction.
- Reflect briefly on the day.
- Understand progress over a useful time window.
- Return after a break without losing history or being punished.

## Functional requirements

### H1 — Habit management
Users can create, edit, pause/archive, restore, and delete a habit. A habit should have a name, optional description, tracking mode, schedule, optional reminder, and creation/update metadata. Editing a habit must not silently rewrite historical records.

### H2 — Habit measurement and progress levels
Every habit uses one explicit tracking mode selected during creation:
- **Duration:** record time spent in a consistent internal unit (for example, minutes), displayed in a human-friendly format.
- **Quantity:** record a count in the habit's chosen unit (for example, pages, practice questions, or repetitions). Numeric entries are whole numbers only; intentional zero is valid and is distinct from no record.
- **Completion-only:** record whether the task was done, without a quantity or duration.
- **Effort-aware:** record the user-selected qualitative effort outcome for that habit/date. Do not convert effort levels into invented numeric amounts or sum them as quantities. For flexible weekly goals, count qualifying completed dates according to the weekly-goal rules.

For duration and quantity habits, the user defines a usual target. Dayseed derives progress labels from the recorded amount relative to that target: Tiny (>0% and <25%), Small (25% to <50%), Halfway (50% to <75%), Nearly there (75% to <100%), Goal met (exactly 100%), Extra (>100% through 150%), and Huge (>150%). Classify using the exact actual-to-target ratio, not a rounded display percentage; for whole-number values, compare the ratio without floating-point rounding, then round the displayed percentage only after selecting the label. Use the mathematically implied whole-number boundaries and skip intermediate labels that cannot meaningfully apply to a very small target. Always preserve the actual amount. Huge gets distinct, enthusiastic but non-pressuring encouragement; users should never feel obliged to beat their past performance. Labels describe measured progress, not the user's feelings, motivation, character, or moral worth.

A quick **Complete** action on Today records the usual target for a duration/quantity habit, or completion for a completion-only habit. A separate **Record** action opens detailed entry, custom amount, correction, and optional note controls. The user must be able to correct a quick completion when the recorded amount differs from what actually happened.

Multiple entries for the same habit on the same local calendar date are supported for numeric modes and sum to a daily total. Duration entries should also sum across sessions. The data model must distinguish an explicit zero entry from no entry. Tiny/partial measured progress is not automatically a completed target; weekly target contribution and completion status must be defined separately.

### H3 — Schedule and daily records
Support flexible weekly targets with optional preferred days. Preferred days guide the routine but do not restrict completion to those days. Define week boundaries and progress calculation explicitly; a flexible weekly target must not label every uncompleted day as a missed opportunity.

Schedule edits offer an effective choice of **Apply today** or **Apply tomorrow**. The first Apply-today change creates today's configuration version. A further change today replaces that version only if no qualifying habit activity has been recorded today. Once qualifying activity exists, preserve today's configuration and activity history and offer **Apply tomorrow** instead; **Cancel** leaves everything unchanged. **Apply tomorrow** creates or updates tomorrow's not-yet-effective configuration version. Existing activity records remain unchanged by schedule edits. Explicit record correction is a separate user action. Historical completion uses the target that applied at the time; current-target comparisons are separate and must not rewrite historical completion (D-194).

Distinguish:
- **Rest/non-scheduled day:** neutral, not a failure or missed opportunity.
- **Paused period:** excluded from scheduled opportunities.
- **Scheduled but not completed:** after the scheduled local day has passed, derive a missed opportunity when no qualifying activity was recorded; this is a neutral description of missing recorded evidence, never a moral judgement. Do not offer a manual “mark as missed” action or a separate “Skip this day” action in v1 (D-188, D-193).
- **No record:** before the relevant day has passed, activity is simply unrecorded. After the scheduled day passes with no qualifying activity record, the derived status is missed; it is not an explicit zero and does not prove the user did nothing. Users cannot explicitly mark a habit day as skipped; rest/non-scheduled days remain neutral, and paused periods are excluded.

A record is associated with a habit and the local calendar date on which the user says the activity actually occurred. Prevent accidental duplicate actions while supporting multiple numeric entries per day and transparent corrections. Follow the approved D-187 date/history baseline: preserve saved date keys across timezone changes, handle clock uncertainty conservatively, use the saved week-start consistently, and require explicit date selection for backdated records. Allow genuine activity to be recorded against a past date; recalculate that date's result from qualifying activity and the configuration effective on that date. Editing a target or schedule applies prospectively and must not silently create, delete, or reinterpret historical records. A journal memo can explain an unrecorded day but must never change habit status or count as proof of completion.

### H4 — History
Show past records and allow correction. Corrections must update summaries consistently. The calendar/history view should visibly label a scheduled day as missed when the day has passed without qualifying activity, without requiring a manual miss/Skip action or creating a journal entry (D-188, D-193). A user may open that date to add an optional journal memo explaining what happened; the memo does not change the habit result. Genuine activity can be explicitly backdated to that date and then recalculates the result. Archiving preserves history. Deleting a habit needs a clear explanation of whether associated records are deleted or retained; recommended default is explicit deletion of the habit and its associated personal data, with confirmation.

### J1 — Micro-journal
Offer low-friction daily reflection with an optional prompt and free text. Journaling is optional and must not be treated as proof that a habit was completed or as a way to change a missed-day result. A memo may explain that activity happened but was not recorded; only an actual habit activity record, including a user-entered backdated record, affects the derived result. Users can edit and delete entries. An entry can exist on a day with no habit completions.

### I1 — Insights
Provide simple local summaries such as completed actions, effort-level distribution, active days, and weekly reflection prompts. Explain each metric. Do not infer diagnoses, motivation, personality, or causes from sparse data. Avoid using streaks as the sole or dominant measure.

### C1 — Comeback Flow
When the user returns after inactivity, offer a brief, dismissible, nonjudgmental entry point. Preserve history and provide a simple next action. Distinguish app inactivity from inactivity on an individual habit. Do not assume why the user was away.

### R1 — Reminders
Reminders are optional and require clear consent. Users can change or disable them. Denying permission must not block core features. The core choices in D-041 and weekly-goal rules are recorded; proposed safety defaults for quiet hours (21:00–08:00), a shared three-notifications/day cap, collision handling, late-delivery suppression and local-only timing learning are documented in `05-COMEBACK-AND-ENCOURAGEMENT.md`, `23-WEEKLY-GOAL-REMINDERS.md`, `26-SCREEN-AND-DATA-CONTRACTS.md`, and `28-REQUIREMENT-TRACEABILITY.md`. These are recommendations, not owner-approved settings. The owner must accept or explicitly defer them before Gate A; Android delivery behaviour is post-gate verification.

### O1 — Offline operation
Creating/editing habits, recording outcomes, journaling, viewing history, displaying Comeback Flow, readable journal export, and local backup/restore must be designed around offline-first operation. Core use requires no account or network. Online backup and cross-device sync are premium future capabilities and are not included in the initial v1 release; any later release requires a separate privacy/security, consent, data-flow, deletion/retention, conflict-resolution, and monetisation gate. Network absence must never block local records.

### P1 — Privacy controls
Explain local storage, readable journal export, user-controlled portable backup/restore, deletion, and Android platform-backup behaviour. Under approved decision D-186, sensitive Dayseed app data is excluded from Android platform Auto Backup/device transfer by default; users should create a Dayseed portable backup before changing phones. This is separate from Dayseed's opt-in file-based Auto Backup to a user-selected destination. Do not send journal text or personal habit records to analytics or ad providers. Initial v1 has no accounts, online backup, or cross-device sync. Treat premium online backup/sync as future scope requiring separate explicit approval and privacy/security review.

### H4b — Entries, notes, undo, and weekly calendar
Duration sessions and quantity entries are individually recorded and can be reviewed/corrected. Multiple sessions for the same habit and local calendar date sum into that date's total; different habits always remain separate (for example, reading pages, jogging distance, and cups of water are separate goals with their own units and totals). Store a canonical numeric unit internally and display the user's chosen unit consistently.

Support an optional **habit/date note** as the default context note, plus optional **entry-specific notes** for individual sessions. These are distinct from the general daily journal, and neither notes nor journal entries imply habit completion. Users can edit/delete notes according to the documented data-retention policy.

Users can undo a recent accidental tracking action and later correct or delete an individual entry. **Delegated planning default under D-196:** after a successful create/log/complete/edit of one tracking record or habit-date note, show one 8-second Undo action. Undo reverses only that exact mutation, using stable entity identity and a guarded transaction; it must not delete unrelated entries or override a later edit. A new mutation replaces the previous pending Undo. After timeout or process death, saved data remains; no durable undo is promised, and supported records can still be corrected/deleted through history. Confirmed journal-entry deletion, permanent habit deletion, delete-all, and Replace/Merge restore are not snackbar-undoable; explain consequences and use the approved confirmation/safety-backup flows. These are proposed defaults, not owner-approved final UX. Duplicate quick taps must not create unintended duplicate completions, while legitimate repeated sessions remain possible.

Users can choose their preferred first day of the week (for example Saturday, Sunday, or Monday). Weekly target progress, weekly summaries, and calendar displays must use this setting consistently. The initial default should follow the device/locale convention where available, with a user override.

### H5 — XP, rank, and social comparison (exploration only)
A discreet optional rank/XP concept may be researched as a secondary profile detail, with restrained rank-up feedback. No XP formula, progression system, leaderboard, online identity, or social feature is approved for v1. Any future XP must avoid shaming missed days, privileging streak perfection, or making the product feel game-first. Online leaderboards require a separate explicit product decision and privacy/safety review; they are not part of the current local-first v1 scope.

### H6 — Partial progress and weekly reflection
For duration and quantity habits, preserve the actual amount and distinguish at least three outcomes against the applicable target: **Partial** (some activity, below target), **Goal met** (target reached), and **Above goal** (target exceeded). These are factual progress states, not judgements about the user's effort or worth. No record is not the same as a recorded zero; an intentional zero is shown distinctly from no record. Partial activity is meaningful progress but must not be silently counted as a fully met daily target.

A weekly review distinguishes daily target-met days, partial days, and above-target days, while separately summarising totals and flexible weekly-target progress. A weekly target may be met across several days even if some individual days are partial; the UI must not conflate daily goal completion with weekly target achievement.

After partial progress, Dayseed may offer a short, time-limited sequence of optional motivational tips over the following week. Content should explain how small steps can support habit formation, acknowledge the recorded progress accurately, and offer practical next steps without guilt, pressure, or assumptions about the user's feelings or circumstances. Avoid intrusive repetition; allow dismissal/opt-out. Exact cadence, trigger, thresholds, and copy remain open.

### H7 — Personalised reminders and outcome-aware encouragement
Plan for complementary in-app messages and optional Android notifications. Messages should reflect the actual habit/day state: Partial, Goal met, Above goal, or a scheduled opportunity that remains unrecorded. Keep these distinct from neutral rest/non-scheduled days, paused periods, the inactivity-based Comeback Flow, and the short encouragement sequence after partial progress.

Explore local pattern-based timing. If a user repeatedly logs a habit around a particular time, Dayseed may suggest a reminder when the habit remains unrecorded after that usual window, followed by an optional later-day check-in. Treat learned timing as a tentative pattern, not certainty. Provide a manual schedule as an alternative and let the user edit or disable learned reminders.

Reminders must be non-shaming, deduplicated, rate-limited, and suppressed when the relevant activity is recorded or the user has dismissed the prompt. Respect Android notification permission, quiet hours, per-habit settings, and user-defined frequency. In-app messages remain available when phone notifications are disabled. Use only the minimum local activity metadata required; do not infer absence reasons or mine journal text for behavioural predictions.

Ask once during first-run setup whether reminders are wanted. Yes enables reminders; No creates none. Do not repeat this global question for each habit. Each habit creation/edit screen displays a reminder-time control defaulted to 9:00 a.m. in the user's local time; the reminder for that habit uses the selected time, which may be changed while creating or editing the habit. For a habit that remains on system-managed timing, Dayseed may automatically adjust the reminder after 14 days of sufficient completion observations, then observe the new time for a further 28 days to validate and solidify the adjustment. If the user's routine later changes, Dayseed may automatically adjust again while system-managed timing remains enabled. If the user manually changes a habit's reminder time, treat it as user-controlled: later routine changes can trigger a suggestion, never an automatic change unless the user explicitly returns the habit to automatic timing. Offer one optional evening check-in if a scheduled habit remains unrecorded; respect settings, quiet hours, daily limits, and suppression after logging/dismissal. Android notification permission is separate from in-app preferences. The local-only learning proposal, confidence/rollback policy, quiet hours, shared notification limit, and optional-tip cadence remain subject to owner acceptance/deferment before Gate A. Actual permission and scheduling behaviour requires authorised post-gate technical validation.

## Quality requirements
- Successful writes survive app restart.
- Date handling is consistent across timezone changes and daylight-saving changes where relevant.
- Accessible controls, dynamic text sizing, screen-reader labels, and contrast.
- Clear empty, loading, error, and offline states.
- No loss of records during app upgrades or schema migrations.
- Performance and storage targets are measured on representative low- and mid-range Android devices.

## Acceptance scenarios
1. Create a habit offline, close the app, reopen it, and verify persistence.
2. Record Tiny effort and verify it counts as completion without increasing an “effort quality” score.
3. Edit yesterday’s record and verify all affected summaries update.
4. Archive a habit and verify past history remains accessible.
5. Write a journal entry with no completed habits and verify it remains valid.
6. Return after a long absence and verify there is no shame language or history reset.
7. Deny notification permission and verify all core features remain usable.
8. Delete personal data and verify it is absent from app views and the database according to the documented deletion policy.

## Open requirements
Before the owner approves planning Gate A, finish the **written design** for the exact schema (entities, keys, constraints/indexes, transactions/rollback, deletion/cascade and migration plan), and resolve or explicitly defer remaining product/UX/privacy/recovery choices with a safe default and revisit trigger. Do not conduct runtime/technical verification as part of this planning step: inspection of the actual D-186 Android backup rules, Gradle build/tests, APK installation, device behaviour, and real SQLite migration belongs after separate owner authorisation of the post-planning technical-validation gate. The D-187 date/history policy and archive-by-default habit lifecycle are approved; their technical edge cases remain to be verified later. XP/rank remains unapproved for v1 and must not block core scope unless separately reconsidered. Premium online backup/sync remains outside initial v1 and requires a separate approval gate before any future implementation. Online leaderboards remain deferred pending separate approval.