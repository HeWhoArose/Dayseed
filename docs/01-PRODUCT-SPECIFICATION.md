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
Every habit uses one explicit measurement mode selected during creation:
- **Duration:** record time spent in a consistent internal unit (for example, minutes), displayed in a human-friendly format.
- **Quantity:** record a count in the habit's chosen unit (for example, pages, practice questions, or repetitions). Numeric entries are whole numbers only; intentional zero is valid and is distinct from no record.
- **Completion-only:** record whether the task was done, without a quantity or duration.

For duration and quantity habits, the user defines a usual target. Dayseed derives progress labels from the recorded amount relative to that target. Tiny, intermediate levels, Usual, and Extra describe measured progress—not the user's feelings, motivation, character, or moral worth. Exact thresholds, intermediate labels, target-zero handling, and display wording remain open planning decisions.

A quick **Complete** action on Today records the usual target for a duration/quantity habit, or completion for a completion-only habit. A separate **Record** action opens detailed entry, custom amount, correction, and optional note controls. The user must be able to correct a quick completion when the recorded amount differs from what actually happened.

Multiple entries for the same habit on the same local calendar date are supported for numeric modes and sum to a daily total. Duration entries should also sum across sessions. The data model must distinguish an explicit zero entry from no entry. Tiny/partial measured progress is not automatically a completed target; weekly target contribution and completion status must be defined separately.

### H3 — Schedule and daily records
Support flexible weekly targets with optional preferred days. Preferred days guide the routine but do not restrict completion to those days. Define week boundaries and progress calculation explicitly; a flexible weekly target must not label every uncompleted day as a missed opportunity.

Schedule edits offer an effective choice of **Apply today** or **Apply tomorrow**. Existing activity records remain unchanged by schedule edits. Explicit record correction is a separate user action. Historical completion uses the target that applied at the time; current-target comparisons are separate and must not rewrite historical completion.

Distinguish:
- **Rest/non-scheduled day:** neutral, not a failure or missed opportunity.
- **Paused period:** excluded from scheduled opportunities.
- **Scheduled but not completed:** a missed opportunity, never a moral judgement.
- **No record:** unknown/unrecorded activity, not automatically an explicit zero or proof that the user did nothing.

A record is associated with a habit and the local calendar date on which it was intentionally recorded. Prevent accidental duplicate actions while supporting multiple numeric entries per day and transparent corrections. Define timezone/day-boundary behaviour before implementation. Editing a schedule must not silently create or delete historical records; historical schedule eligibility rules must be explicit.

### H4 — History
Show past records and allow correction. Corrections must update summaries consistently. Archiving preserves history. Deleting a habit needs a clear explanation of whether associated records are deleted or retained; recommended default is explicit deletion of the habit and its associated personal data, with confirmation.

### J1 — Micro-journal
Offer low-friction daily reflection with an optional prompt and free text. Journaling is optional and must not be treated as proof that a habit was completed. Users can edit and delete entries. An entry can exist on a day with no habit completions.

### I1 — Insights
Provide simple local summaries such as completed actions, effort-level distribution, active days, and weekly reflection prompts. Explain each metric. Do not infer diagnoses, motivation, personality, or causes from sparse data. Avoid using streaks as the sole or dominant measure.

### C1 — Comeback Flow
When the user returns after inactivity, offer a brief, dismissible, nonjudgmental entry point. Preserve history and provide a simple next action. Distinguish app inactivity from inactivity on an individual habit. Do not assume why the user was away.

### R1 — Reminders
Reminders are optional and require clear consent. Users can change or disable them. Denying permission must not block core features. Exact scheduling, quiet hours, and rescheduling rules remain open.

### O1 — Offline operation
Creating/editing habits, recording outcomes, journaling, viewing history, and displaying Comeback Flow must work without network access. Future network-dependent features must fail gracefully and never block local records.

### P1 — Privacy controls
Explain local storage, export, deletion, and platform backup behaviour. Do not send journal text or personal habit records to analytics or ad providers. No cloud sync in v1 unless explicitly re-approved.

### H4 — Entries, notes, undo, and weekly calendar
Duration sessions and quantity entries are individually recorded and can be reviewed/corrected. Multiple sessions for the same habit and local calendar date sum into that date's total; different habits always remain separate (for example, reading pages, jogging distance, and cups of water are separate goals with their own units and totals). Store a canonical numeric unit internally and display the user's chosen unit consistently.

Support an optional **habit/date note** as the default context note, plus optional **entry-specific notes** for individual sessions. These are distinct from the general daily journal, and neither notes nor journal entries imply habit completion. Users can edit/delete notes according to the documented data-retention policy.

Users can undo a recent accidental action and later correct or delete an individual entry. The UI must communicate what is being changed and update derived totals and insights. Duplicate quick taps should not create unintended duplicate completions, but legitimate repeated sessions/entries must remain possible. Exact undo window and duplicate semantics remain to be specified.

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

The reminder defaults, learning window/confidence, quiet hours, end-of-day definition, notification caps, opt-in UX, and whether all learning stays on-device remain open. Feasibility and privacy must be reviewed before implementation.

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
Resolve before Gate A: detailed schedule/history model; date and timezone policy; journal-entry cardinality; edit/delete retention and export; reminder behaviour and personalised reminder learning policy; exact progress-level thresholds and labels; undo/duplicate semantics; default week start; and whether XP/rank has sufficient value to justify inclusion. Online leaderboards remain deferred pending separate approval.