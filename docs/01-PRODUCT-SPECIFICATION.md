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

### H2 — Effort levels
Proposed modes:
- **Tiny:** a deliberately small action.
- **Usual:** the ordinary intended action.
- **Extra:** an optional stretch action.
- **Completion only:** done/not done, without levels.

A Tiny completion counts as completion. Extra must not be framed as morally better. Whether users can customise level definitions is open.

### H3 — Schedule and daily records
Support daily and selected-day schedules at minimum; evaluate weekly targets and flexible frequency before scope lock. A record is associated with a habit and a local calendar date. Prevent accidental duplicate daily records while allowing users to edit an existing record. Define timezone and day-boundary behaviour before implementation.

Possible outcomes to distinguish: completed (with optional effort level), skipped by choice, planned rest, and no record. “Missed” should be a derived display state, not a permanent judgement stored as fact. Final semantics remain open.

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
Resolve before Gate A: schedule model; missed/skipped/rest semantics; date and timezone policy; whether multiple journal entries per day are allowed; edit/delete retention behaviour; export format; reminder behaviour; and whether effort-level names are fixed or customisable.

## Tracking and schedule decision update (2026-10-18)

Owner-approved direction (D-010; detailed semantics remain open):
- Number-based habits allow multiple entries per local calendar date; the day's displayed quantity is the sum of those entries.
- When a user changes a habit's schedule, ask whether the change applies to today or tomorrow.
- Today, History, Insights, and habit editing must represent the individual entries and their daily aggregate consistently, and schedule edits must not silently rewrite existing records.
- This decision does not settle numeric validation, time-zone rules, target snapshots, or all schedule eligibility semantics. See the decision log and docs/21-TODAY-DASHBOARD-CONCEPTS.md.
