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
7. **History is not rewritten implicitly (D-187):** target/schedule changes are prospective and past records retain their historical interpretation. Explicit corrections affect only the selected record and dependent calculations. Archive is the default non-destructive habit lifecycle action and preserves accessible history; permanent deletion is a separate, clearly explained action. Habit deletion must not delete independent journal entries.
8. **Journal integrity:** multiple saved entries per date are allowed. A saved edit preserves entry identity and creation timestamp. Blank/whitespace-only entries cannot be saved. Unsaved drafts do not appear as saved journal entries and are not automatically shared.
9. **Restore safety:** validate backup integrity and compatibility before mutating live data. A failed/uncertain restore must not appear complete. Replace restore requires a verified safety backup and final confirmation. Restore conflict rules in D-100–D-102 take precedence.
10. **Privacy:** no initial-v1 account, Dayseed-managed cloud backup, cross-device sync, AI processing, or implicit upload of personal text. A user-selected file provider may sync a file under that provider's terms.

## 1A. Proposed persistence model — owner review required

The detailed candidate schema lives in `03-TECHNICAL-ARCHITECTURE.md` and remains **proposed, not approved for implementation**. Two partial schema decisions are approved: effective-dated habit configuration snapshots preserve historical targets, schedules, tracking modes, and other configuration; and numeric/duration entries are stored separately from per-day completion-only/effort-aware outcomes (D-189). Multiple numeric entries per habit/date remain valid and are summed; explicit zero remains a real numeric entry; at most one current non-numeric outcome is allowed per habit/date. The domain layer must validate each record against the effective tracking mode and reject contradictory combinations rather than infer completion from an unrelated record. These partial decisions do not approve the full schema. Stable UUID identity and nullable unique idempotency keys are approved by D-190: edits retain entity identity, retries of the same logical submission reuse the same key, and distinct intentional entries use new keys. D-192 confirms both optional per-entry notes and one optional habit/date note; the daily note is the easiest/default path, remains distinct from the general daily journal, and neither note changes habit status or activity totals. D-191 approves structured schedule fields: daily every-day schedule, selected weekdays, or flexible weekly target; optional preferred weekdays guide but do not restrict a flexible weekly target; and target period is explicit as day or week. All weekly calculations use the saved app-level week-start preference.

Until the owner approves the model:
- Do not assume a final table layout, foreign-key cascade, idempotency-index implementation, or migration plan.
- Preserve the approved invariants above: multiple numeric/duration entries per habit/date, multiple journal entries per date, an absent record is distinct from an explicit zero; missed status is derived from schedule and qualifying activity records, and journal data is independent from habit deletion.
- Any schema proposal must specify which configuration applies to a historical date, how a retry differs from a distinct intentional entry, how each record is validated against its effective tracking mode, how contradictory numeric and non-numeric states are prevented, and how permanent habit deletion leaves independent journal data untouched.
- D-191 schedule contract: `DAILY` schedules every local calendar day; `SELECTED_WEEKDAYS` requires a valid non-empty weekday mask; `FLEXIBLE_WEEKLY_TARGET` has no required weekdays and may have optional preferred weekdays for guidance only. `target_period` is `DAY` for both `DAILY` and `SELECTED_WEEKDAYS` schedules, and `WEEK` for `FLEXIBLE_WEEKLY_TARGET`. Quantity/duration weekly goals sum compatible units; completion-only/effort-aware weekly goals count qualifying completed dates. All week boundaries use the saved week-start preference, not a hard-coded weekday.
- D-188 resolves missed-day semantics: do not offer a manual mark-as-missed control or store a user-entered miss outcome. Derive a missed status after a scheduled day passes without qualifying activity; permit genuine explicitly backdated activity to recalculate that date. A journal memo alone never changes habit status.
- D-193 resolves habit-day Skip semantics: v1 has no separate “Skip this day” control and does not persist a skipped day-outcome. Derive status from qualifying activity, the applicable schedule, and whether the scheduled day has passed. Rest/non-scheduled days remain neutral, paused periods are excluded, and optional skipping of journal prompts/reflections remains available where separately specified.
- D-194 resolves repeated **Apply today** behavior: the first change creates today's configuration version; a further same-day change replaces it only while no qualifying habit activity has been recorded today. After activity exists, preserve today's configuration and records and offer **Apply tomorrow**. **Cancel** leaves everything unchanged; **Apply tomorrow** creates or updates tomorrow's not-yet-effective version. Past configuration and activity history must not be rewritten.

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

### Record detail / edit

- Open for a specific habit and date; display the selected date and mode.
- List existing entries individually, with their own values and notes.
- Editing or deleting targets only the selected entry; totals and dependent insights recalculate from remaining valid entries.
- Negative values are rejected for non-negative counters without changing existing records. Above-target values remain valid and are not capped in storage.
- Provide recent undo for accidental actions when the final undo window/interaction is specified; always support later explicit correction/deletion.
- The exact undo duration and recovery UI remain open.

### Journal editor

- The selected date is visible and can be changed only through an explicit user action.
- A saved entry is created only after explicit Save; whitespace-only text is rejected while preserving the draft.
- Editing updates the existing entry, preserving its ID and creation timestamp.
- Multiple entries on a date remain separate. Habit archive/deletion does not remove journal entries.
- Leaving an unsaved draft does not silently create a saved entry. Drafts restore locally and remain until explicitly saved, deleted, or discarded.
- Deleting a saved entry or permanently discarding a draft requires explicit confirmation.
- Journal operations never mark habits complete or move habit records.

### History / calendar

- Use the saved week-start preference consistently for weekly views.
- Show a derived missed label on a past scheduled date when no qualifying activity was recorded, without requiring a manual action or creating a journal entry. Opening that date may offer a separate optional journal memo; the memo does not alter habit status.
- Historical views show the settings/target that applied at the time; current-target comparisons must be labelled separately.
- Corrections are explicit and local to the selected entity. Do not recalculate unrelated records as a side effect.
- Rest/non-scheduled days are neutral; paused periods are excluded from scheduled opportunities. Before a scheduled day has passed, no record is unrecorded rather than zero. After a scheduled day passes without qualifying activity, missed status is derived; users cannot manually mark a day missed. Genuine backdated activity can recalculate the result, but a journal memo alone cannot change habit status.
- Date/time behaviour follows approved D-187 and D-188 in `03-TECHNICAL-ARCHITECTURE.md`: stable local-date keys separate from event instants, explicit date selection for genuine backdated activity, conservative clock-change handling, saved week-start consistency, no implicit historical rewrite, derived missed status for past scheduled days with no qualifying activity, and no habit-status changes from journal memos.

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
- Reminder learning/auto-adjustment confidence, validation, rollback, and full quiet-hour controls remain to be finalised before implementation.

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

## 4. Open items that must not be silently defaulted

1. Android Auto Backup/device-transfer exclusion under D-186 — **decision resolved; technical verification remains required** to confirm the manifest/data-extraction configuration excludes private records on supported Android versions and transfer paths.
2. Exact schema, indexes, transaction boundaries, stable ID/idempotency strategy, and migration from the current version-1 SQLite schema.
3. Exact undo window and user-facing recovery interaction.
4. Final delete-all wording and any retention details required for exported/OS backup copies.
5. Reminder-learning confidence, 28-day validation/rollback behaviour, quiet hours, and global/per-habit controls.
6. Exact whole-number rounding at progress-band boundaries and minimum-target display exceptions.
7. Final navigation, visual direction, Growth Mark, and journal archive layout after research/prototype review.
8. Build, automated test, APK install, physical-device, accessibility, and privacy/data-flow evidence — none is asserted by this planning contract.

## 5. Traceability

- Product behaviour: `01-PRODUCT-SPECIFICATION.md`
- UX and proposed flows: `02-UX-AND-DESIGN.md`
- Architecture/date contract: `03-TECHNICAL-ARCHITECTURE.md`
- Privacy and backup: `04-DATA-AND-PRIVACY.md`
- Regression plan: `07-TESTING-AND-QUALITY.md`
- Owner choices: `09-DECISION-LOG.md`
- Reconciliation and open gates: `25-DECISION-REGISTER-AND-RECONCILIATION.md`
- Design tokens/components: `DESIGN.md`
