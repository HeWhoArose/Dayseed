# Dayseed Screen and Data Contracts

**Status:** Planning contract for reconciliation; not UI approval or implementation authorisation.  
**Canonical sources:** `01-PRODUCT-SPECIFICATION.md`, `03-TECHNICAL-ARCHITECTURE.md`, `04-DATA-AND-PRIVACY.md`, `07-TESTING-AND-QUALITY.md`, `09-DECISION-LOG.md`, and `DESIGN.md`.  
**Rule:** If a conflict is found, record it in `25-DECISION-REGISTER-AND-RECONCILIATION.md`; do not silently change an owner-approved decision.

## 1. Shared data invariants

1. **Local-first:** core reads/writes work offline; a save is reported successful only after local persistence confirms it.
2. **Stable identity:** habits, tracking entries, journal entries, and drafts have stable identifiers. Editing updates the selected entity; it does not create a duplicate.
3. **Separate concepts:** local date keys are separate from event timestamps; habit records are separate from journal entries; saved journal entries are separate from drafts.
4. **Mode-specific records:** quantity/duration values are non-negative whole-number units; completion-only state is explicit; effort-aware state is explicit. Do not encode all modes as numeric 0/1.
5. **Multiple entries:** multiple quantity/duration entries for the same habit and local date are allowed and summed. Each remains individually inspectable/editable/deletable. Retrying the same logical submission counts once; distinct intentional submissions remain distinct.
6. **Missing is not zero:** no record, explicit numeric zero, derived missed status, partial progress, goal met, above-target progress, completion-only completion, and effort-aware selection must not be collapsed into one state. A missed status is derived from schedule and absence of qualifying activity after the scheduled day passes; users cannot manually mark a day missed.
7. **History is not rewritten implicitly (D-187):** target/schedule changes are prospective and past records retain their historical interpretation. Explicit corrections affect only the selected record and dependent calculations. Archive is the default non-destructive habit lifecycle action and preserves accessible history; permanent deletion is a separate, clearly explained action. Habit deletion must not delete independent journal entries or their text. Delegated schema default: queue opaque OS-notification identifiers in the notification-cancellation outbox, then transactionally remove habit-owned tracking/configuration/delivery rows and the habit while setting any optional journal linked-habit reference to NULL. After the database commit, cancel the OS notifications and clear outbox rows only when cancellation succeeds; retry interrupted/failed cancellation on startup. Delivery workers must re-check that the habit exists and is active before posting. This is a planning default, not implementation evidence; see the proposed schema closure plan.
8. **Journal integrity:** multiple saved entries per date are allowed. A saved edit preserves entry identity and creation timestamp. Blank/whitespace-only entries cannot be saved. Unsaved drafts do not appear as saved journal entries and are not automatically shared.
9. **Restore safety:** validate backup integrity and compatibility before mutating live data. A failed/uncertain restore must not appear complete. Replace restore requires a verified safety backup and final confirmation. Restore conflict rules in D-100–D-102 take precedence.
10. **Privacy:** no initial-v1 account, Dayseed-managed cloud backup, cross-device sync, AI processing, or implicit upload of personal text. A user-selected file provider may sync a file under that provider's terms.

## 1A. Proposed persistence model — owner review required

The detailed candidate schema lives in `03-TECHNICAL-ARCHITECTURE.md` and remains **proposed, not approved for implementation**. Two partial schema decisions are approved: effective-dated habit configuration snapshots preserve historical targets, schedules, tracking modes, and other configuration; and numeric/duration entries are stored separately from per-day completion-only/effort-aware outcomes (D-189). Multiple numeric entries per habit/date remain valid and are summed; explicit zero remains a real numeric entry; at most one current non-numeric outcome is allowed per habit/date. The domain layer must validate each record against the effective tracking mode and reject contradictory combinations rather than infer completion from an unrelated record. These partial decisions do not approve the full schema. Stable UUID identity and nullable unique idempotency keys are approved by D-190: edits retain entity identity, retries of the same logical submission reuse the same key, and distinct intentional entries use new keys. Because activity is stored in separate numeric and non-numeric tables, docs 03/30 propose partial unique indexes within each table plus cross-table insert/update guards and a domain-transaction check to enforce non-null key uniqueness across logical activity submissions. This is a proposed schema-enforcement detail pending full schema approval, not verified behaviour or a new owner decision. D-192 confirms both optional per-entry notes and one optional habit/date note; the daily note is the easiest/default path, remains distinct from the general daily journal, and neither note changes habit status or activity totals. D-191 approves structured schedule fields: daily every-day schedule, selected weekdays, or flexible weekly target; optional preferred weekdays guide but do not restrict a flexible weekly target; and target period is explicit as day or week. All weekly calculations use the saved app-level week-start preference.

Until the owner approves the model:
- Do not assume a final table layout, foreign-key cascade, idempotency-index implementation, or migration plan.
- Preserve the approved invariants above: multiple numeric/duration entries per habit/date, multiple journal entries per date, an absent record is distinct from an explicit zero; missed status is derived from schedule and qualifying activity records, and journal data is independent from habit deletion.
- Any schema proposal must specify which configuration applies to a historical date, how a retry differs from a distinct intentional entry, how each record is validated against its effective tracking mode, how contradictory numeric and non-numeric states are prevented, and how permanent habit deletion leaves independent journal data untouched.
- D-191 schedule contract: `DAILY` schedules every local calendar day; `SELECTED_WEEKDAYS` requires a valid non-empty weekday mask; `FLEXIBLE_WEEKLY_TARGET` has no required weekdays and may have optional preferred weekdays for guidance only. `target_period` is `DAY` for both `DAILY` and `SELECTED_WEEKDAYS` schedules, and `WEEK` for `FLEXIBLE_WEEKLY_TARGET`. Quantity/duration weekly goals sum compatible units; completion-only/effort-aware weekly goals count qualifying completed dates. All week boundaries use the saved week-start preference, not a hard-coded weekday.
- D-188 resolves missed-day semantics: do not offer a manual mark-as-missed control or store a user-entered miss outcome. Derive a missed status after a scheduled day passes without qualifying activity; permit genuine explicitly backdated activity to recalculate that date. A journal memo alone never changes habit status.
- D-193 resolves habit-day Skip semantics: v1 has no separate “Skip this day” control and does not persist a skipped day-outcome. Derive status from qualifying activity, the applicable schedule, and whether the scheduled day has passed. Rest/non-scheduled days remain neutral, paused periods are excluded, and optional skipping of journal prompts/reflections remains available where separately specified.
- D-194 resolves repeated **Apply today** behavior: the first change creates today's configuration version; a further same-day change replaces it only while no qualifying habit activity has been recorded today. After activity exists, preserve today's configuration and records and offer **Apply tomorrow**. **Cancel** leaves everything unchanged; **Apply tomorrow** creates or updates tomorrow's not-yet-effective version. Past configuration and activity history must not be rewritten.
- D-195 fixes the weekday-mask encoding independently of locale and week start: Monday=1, Tuesday=2, Wednesday=4, Thursday=8, Friday=16, Saturday=32, Sunday=64, stored as a 7-bit integer; combine days with bitwise OR. `SELECTED_WEEKDAYS` requires a valid non-empty mask; `DAILY` and `FLEXIBLE_WEEKLY_TARGET` do not require a scheduled-day mask. A flexible weekly target may optionally use `preferred_weekdays_mask` as guidance only. Reject unknown bits and invalid schedule/mask combinations. Technical validation remains pending; the schema is still proposed.

## 2. Screen contracts

### Today

**Purpose:** Make the next useful habit action obvious with minimal friction.

- Show each active habit, tracking mode, target/unit where relevant, schedule context, and today's state.
- Offer **Complete** as a quick action and **Record** for precise entry/correction where applicable.
- For quantity/duration habits, Complete records the usual target; Record allows actual amount and optional habit/date or entry-specific note as defined by the product spec.
- Completion-only habits never show numeric amount controls. Effort-aware habits use their approved effort choices, not an invented quantity.
- Visibly distinguish no record, explicit zero, partial, target met, and above target. Do not treat a partial amount as a fully met daily target.
- A failed save retains the entered data and communicates that it was not saved; do not show success optimistically.
- A repeated tap/retry of one logical action must not double-count. Legitimate repeated sessions remain available.
- Journal entry point is optional and independent of habit completion.
- **Growth Mark (O-6/D-199, selected planning direction):** Today uses a compact G2-style **Collected moments** summary showing total recorded duration from duration-tracked habits in the current calendar month by default (for example, “3 hr 40 min this month”). Label the reporting period and use a naturally readable duration unit. Tapping the summary opens G3 calendar/date-level detail (in Calendar/Progress or a dedicated detail page), where the user can inspect per-date totals and their underlying duration sessions. Period controls may allow day, configured week and calendar-month views; the configured week-start governs weekly boundaries. This remains a planning contract; layout/format thresholds and interaction accessibility are not yet approved.

### Record detail / edit

- Open for a specific habit and date; display the selected date and mode.
- List existing entries individually, with their own values and notes.
- Editing or deleting targets only the selected entry; totals and dependent insights recalculate from remaining valid entries.
- Negative values are rejected for non-negative counters without changing existing records. Above-target values remain valid and are not capped in storage.
- **Delegated Undo default under D-196:** after a successful create/log/complete/edit of one tracking record or habit-date note, show a single 8-second snackbar Undo. It reverses only that exact mutation; a newer mutation replaces the pending Undo, and an edit is reverted only if no later change makes the inverse unsafe. Recompute totals/insights. After timeout/process death, the committed data remains and normal History correction is available.
- Delete of one selected numeric/duration entry uses a clear contextual Delete action plus immediate Undo without a second confirmation dialog. Confirmed journal-entry deletion, permanent habit deletion, delete-all and Replace/Merge restore are not snackbar-undoable; retain explicit confirmation/safety-backup rules and explain irrecoverability. This is the default under D-196 and the owner may override it before Gate A; planned tests are not evidence of implementation.

### Journal editor

- The selected date is visible and can be changed only through an explicit user action.
- A saved entry is created only after explicit Save; whitespace-only text is rejected while preserving the draft.
- Editing updates the existing entry, preserving its ID and creation timestamp.
- Multiple entries on a date remain separate. Habit archive/deletion does not remove journal entries.
- Leaving an unsaved draft does not silently create a saved entry. Drafts restore locally and remain until explicitly saved, deleted, or discarded.
- Deleting a saved entry or permanently discarding a draft requires explicit confirmation.
- Journal operations never mark habits complete or move habit records.

### Optional retrospective journal prompt (D-042; delegated proposal)

- On the first eligible app opening each local date, optionally offer a small in-app-only invitation to journal the most recent fully completed local date if it has no saved entry.
- Show at most once for that target date; display, dismissal, “Not now,” or leaving the prompt suppresses re-prompting for that date. Never send a notification or backlog multiple older dates.
- If a comeback experience takes precedence on the same opening, do not stack the retrospective prompt. A saved entry suppresses the prompt; a draft for the same date is reused rather than duplicated.
- Open the ordinary Journal editor with the target date visible. Explicit save remains required, and the saved entry never changes habit status/progress.
- This frequency/priority is the delegated planning default under D-196; the owner may override or defer it before Gate A. Tests are planned only.

### History / calendar

- Use the saved week-start preference consistently for weekly views.
- Show a derived missed label on a past scheduled date when no qualifying activity was recorded, without requiring a manual action or creating a journal entry. Opening that date may offer a separate optional journal memo; the memo does not alter habit status.
- Historical views show the settings/target that applied at the time; current-target comparisons must be labelled separately.
- Corrections are explicit and local to the selected entity. Do not recalculate unrelated records as a side effect.
- Rest/non-scheduled days are neutral; paused periods are excluded from scheduled opportunities. Before a scheduled day has passed, no record is unrecorded rather than zero. After a scheduled day passes without qualifying activity, missed status is derived; users cannot manually mark a day missed. Genuine backdated activity can recalculate the result, but a journal memo alone cannot change habit status.
- Date/time behaviour follows approved D-187 and D-188 in `03-TECHNICAL-ARCHITECTURE.md`: stable local-date keys separate from event instants, explicit date selection for genuine backdated activity, conservative clock-change handling, saved week-start consistency, no implicit historical rewrite, derived missed status for past scheduled days with no qualifying activity, and no habit-status changes from journal memos.
- **G3 calendar/detail:** show date-level duration totals and allow inspection of the source duration sessions for the selected date/period. A calendar mark indicates only that a qualifying duration record exists, not goal success or a streak. Blank dates are neutral. Corrections/deletions must recalculate the relevant totals. Show records from duration-tracked habits only; do not mix quantity records, completion-only/effort-aware outcomes, or journal entries into this metric.

### Insights

- Every metric states its period and meaning; counts derive from saved records only.
- Mood distributions count primary moods only where specified; secondary moods remain visible in entry detail/search.
- Sparse/no-data states must be factual and neutral, not fabricated.
- Opening a metric filters the exact selected period. Comparisons use the immediately preceding equivalent period and remain hidden until requested.
- Correcting/deleting a source record recalculates dependent insights without changing unrelated records.
- Do not infer diagnosis, motivation, personality, or why the user was away.

### Comeback experience

- Follow `24-COMEBACK-EXPERIENCE.md` and decisions D-146–D-165 for trigger precedence, date boundaries, episode suppression, and draft preservation.
- App inactivity alone never changes active habits, records, schedules, or progress.
- A comeback reflection is optional and saved only through the normal explicit-save flow.
- If an unresolved comeback draft exists, restore/reuse it; never silently replace it.
- Do not stack the 14-day and 30-day experiences when the approved precedence selects the broader experience.

### Settings → Reminders

- Global reminder opt-in is asked once during setup. If declined, no reminder is scheduled. Notification permission is separate from the in-app preference.
- Habit reminder time defaults to 9:00 a.m. local time. A manually chosen time is user-controlled; automatic adaptation may not override it unless the user explicitly returns to automatic timing.
- Respect quiet hours, per-habit/global controls, approved caps and suppression after relevant progress/dismissal.
- Explain that Android can defer notifications. Do not promise exact delivery time.
- Evidence thresholds, 28-day validation/rollback, quiet-hour default and shared notification cap are delegated planning defaults described below. The owner may override them before Gate A; technical OS scheduling evidence remains pending.

### Shared reminder delivery rules (delegated planning defaults under D-196)

- Default quiet hours are 21:00–08:00 local and are editable in Settings; no OS notification is posted inside the window. Re-check relevance at the next permitted time.
- Use a shared cap of three OS notifications per local date and at most one ordinary reminder per habit/date. Combine eligible events within 15 minutes where one destination can explain them; Today is the default destination for combined habit summaries.
- Priority under the cap: explicit per-habit reminder, weekly-goal-reached acknowledgement, week-start reflection, inactivity check-in, optional partial tip. Defer a weekly reflection to the next suitable permitted opportunity; suppress stale daily reminders after their local date ends.
- Every delivery re-checks global opt-in, OS permission, quiet hours, schedule/active state, saved activity, dismissal/suppression, week/target state, and current habit existence. Stable delivery keys prevent duplicate sends across restart/reschedule.
- Recalculate future local delivery times after time-zone changes without rewriting saved local-date keys; do not use clock jumps to invent elapsed time.
- System-managed daily reminder learning is local-only, never reads journal text/mood, and requires minimum evidence (seven positive activity timestamps on at least five distinct local dates in 14 days, with 70% in a two-hour band). Candidate reminder: median completion time in that band plus 30 minutes rounded up to a 15-minute boundary. Manual times are never auto-overridden. Hold the candidate for 28 days and revert to the previous automatic time if evidence no longer supports the cluster.
- Optional partial-progress tips: proposed maximum three in the following seven days, at least 24 hours apart, maximum one/day, suppressed after the relevant target is met or dismissal/opt-out. Phone tips need a separate tips-notification setting. These are planning defaults, not implementation evidence; the owner may override them before Gate A.

### Settings → Backup & Restore

- Manual backup is available regardless of Auto Backup. The user chooses a destination through the Android system file picker.
- Show a pre-creation summary of included categories. v1 backup is complete; no category-exclusion controls.
- Manual backups may be password-protected after two matching entries; warn that a forgotten password may make the file unrecoverable. Auto Backup is not password-protected in v1.
- Verify the produced file's integrity/readability before showing success. Never treat a partial file as a valid backup.
- Backup history shows only files Dayseed can reliably identify; do not imply it scans all device storage.
- Restore preview shows backup date/version and category counts. Validate integrity/compatibility before any mutation.
- Replace requires a verified safety backup and final confirmation. Merge/Replace conflict decisions D-091–D-105 govern; source backup remains untouched.
- Auto Backup is opt-in, uses the chosen destination, follows the approved intervals, creates new files, and never automatically prunes old backups. Android may defer scheduled work.
- If the chosen destination is unavailable/revoked, stop and ask the user to choose again; never silently redirect.
- **Approved policy D-186:** exclude sensitive Dayseed app data from Android platform Auto Backup/device transfer by default. Explain before migration that Android's own transfer may not carry private Dayseed records and direct users to create a complete Dayseed portable backup. Keep platform backup separate from the app's own opt-in file-based Auto Backup; verify actual OS rules before claiming exclusion is effective.

### Settings → Privacy and data

- Explain local storage, readable export, portable backup, Android platform backup, and user-selected provider behaviour as distinct paths.
- Deleting a journal entry, deleting a habit with its associated tracking history, and deleting all app data are separate actions with clear consequences and confirmation.
- Habit deletion must preserve independent journal entries and their text. Existing exported/provider/OS backup copies may require separate deletion.
- No analytics/crash SDK or advertising SDK is implicitly approved; inspect actual dependencies/data flows before making release privacy claims.

## 3. Cross-screen state and failure handling

All affected screens must implement the following state distinctions:

- **Loading:** no success claim until the local operation finishes.
- **Empty:** explain what is absent and offer a gentle next step.
- **Validation error:** identify the invalid field; preserve other input and saved data.
- **Save failed/uncertain:** retain entered text/values; allow safe retry using the same logical submission identity.
- **Restore failed:** live data unchanged or safely rolled back; never display partial restore as complete.
- **Permission denied:** core local features remain usable; explain how to re-enable only when relevant.
- **Unavailable destination:** no silent fallback location.
- **Corrupt/incompatible backup:** stop before live-data mutation and explain the recovery path.
- **Clock/time uncertainty:** avoid duplicate reminders/comeback displays and avoid asserting an unsupported exact elapsed duration.

## 4. Unresolved items and delegated defaults that must stay explicit

1. Android Auto Backup/device-transfer exclusion under D-186 — **decision resolved; technical verification remains required** to confirm the manifest/data-extraction configuration excludes private records on supported Android versions and transfer paths.
2. Complete proposed schema approval: reconcile the table/constraint/index/transaction, restore-conflict, journal-link and cancellation-outbox refinements in `03-TECHNICAL-ARCHITECTURE.md` with `30-SCHEMA-CLOSURE-AND-MIGRATION-PLAN.md`; owner approval and post-gate migration verification are still outstanding.
3. Delegated Undo default: 8-second in-session single-mutation Undo and no second confirmation for deleting one selected numeric/duration entry. The owner may override this before Gate A; it is not a hard blocker requiring a separate decision.
4. Final delete-all wording and any retention details required for exported/OS backup copies.
5. Delegated reminder and retrospective-prompt defaults, including per-habit manual-time protection; these are reversible defaults under D-196 and may be overridden as part of the grouped review rather than answered separately.
6. Exact whole-number rounding at progress-band boundaries and minimum-target display exceptions.
7. Final colour/type tokens and broad visual direction, Growth Mark unit-format thresholds and period controls/accessibility, and journal archive layout/task review. G2/G3 roles are owner-selected planning directions under D-199; these do not approve final UI or implementation.
8. Build, automated test, APK install, physical-device, accessibility, and privacy/data-flow evidence — none is asserted by this planning contract.

## 5. Traceability

- Product behaviour: `01-PRODUCT-SPECIFICATION.md`
- UX and proposed flows: `02-UX-AND-DESIGN.md`
- Architecture/date contract: `03-TECHNICAL-ARCHITECTURE.md`
- Privacy and backup: `04-DATA-AND-PRIVACY.md`
- Regression plan: `07-TESTING-AND-QUALITY.md`
- Owner choices: `09-DECISION-LOG.md`
- Reconciliation and open gates: `25-DECISION-REGISTER-AND-RECONCILIATION.md`
- Requirement-to-test mapping: `28-REQUIREMENT-TRACEABILITY.md`
- Design tokens/components: `DESIGN.md`
