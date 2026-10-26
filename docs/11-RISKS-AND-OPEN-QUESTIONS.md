# Risks and Open Questions

**Status:** Living register; review at each planning gate. See [Decision Log](09-DECISION-LOG.md) for decision status and supporting research documents.

## 1. Highest-priority risks

| Risk | Impact | Mitigation | Owner/status |
|---|---|---|---|
| Ambiguous missed/rest semantics | Users misunderstand progress; inconsistent summaries | Define outcomes and schedules before implementation; test scenarios | Open |
| Incorrect date/time handling | Records appear on wrong day; comeback messages wrong | Apply approved D-187 local-date/history contract; verify central date service and boundary cases after the planning gate | Policy approved; technical verification pending |
| Accidental loss or disclosure of journal data | High trust and privacy impact | Data-flow review, backup tests, deletion/export tests, no sensitive logs | Open |
| Comeback copy feels shaming or intrusive | Users avoid returning | Test copy with users; dismissal control; no assumed reasons | Open |
| Growth Mark becomes arbitrary gamification | Product promise contradicted; users misread score | Tie every visual change to explainable recorded evidence; compare against plain summary | Open |
| Archive layout makes retrieval difficult | Users cannot revisit older reflections quickly | Test timeline, calendar, and hybrid with known-date tasks | Open |
| Typography or colour fails in real use | Small-screen readability and accessibility failures | Compare device samples; test all actual light/dark contrast pairs and large text | Open |
| Native Kotlin/Compose implementation readiness is unverified | Build or platform issues may require rework | Keep native stack decision (D-009); inspect compatibility and run the authorised build/test spike only after the planning gate | Evidence required |
| AI-assisted workflow drifts from native Android scope | Wrong deliverable or overwritten work | Keep AI Studio instructions explicit about native Kotlin/Compose; GitHub `main` is canonical; no implementation before the planning gate | Controlled; monitor |
| Cross-environment changes overwrite newer work | Lost code or broken history | GitHub is canonical; pull before resuming Replit; reconcile branches rather than blind overwrite | Open |
| Monetisation damages trust or violates policy | User harm, policy/store risk | Consider no ads; privacy review before SDK choice | Open |
| Scope expands before core flows are reliable | Delayed release and complexity | Enforce v1 non-goals and phase exit gates | Open |
| Accessibility is treated as polish | Some users cannot complete core tasks | Include accessibility in design and release gates | Open |
| Local data is unexpectedly included in Android backup/device transfer | Privacy promise becomes misleading | Inspect and test Android backup and transfer behaviour separately from the user-controlled local backup feature | Open |
| Metrics encourage perfectionism | Product contradicts its promise | Explain metrics; never make streak the only success signal | Open |

## Data-model design status

**Exact v1 relational schema: proposed, awaiting owner approval.** A concrete candidate has been added to `03-TECHNICAL-ARCHITECTURE.md`. The key choices needing confirmation are effective-dated habit configuration snapshots; separate numeric/duration entries and single-per-day non-numeric outcomes; distinct date-level and entry-level notes; stable UUIDs and nullable unique idempotency keys; and the state transition when an explicit miss is replaced by later numeric activity. The proposed schedule columns must also be reconciled with approved schedule decisions. Do not implement the candidate or migrate the existing version-1 database until owner approval and a separate technical-validation authorisation.

## 2. Open product questions

1. **Schedule types — mostly resolved:** daily/selected-day schedules and flexible weekly targets are documented in D-024/D-030. Remaining work is to verify the full set of supported schedule combinations and historical edits in the data contract.
2. **Outcome semantics — partially resolved:** explicit zero differs from no record (D-023); rest/non-scheduled days, paused periods, and missed opportunities are defined in D-027. The distinct meaning and user-facing handling of an explicit “skipped” action still needs final specification.
3. **Progress labels:** thresholds and labels are documented in D-040; user-customisable labels remain open and should not be assumed for v1.
4. **Multiple records — resolved:** legitimate multiple numeric entries/duration sessions can occur on one local date and are summed; retries of the same logical action must remain idempotent (D-023, D-031, D-032).
5. **Journal multiplicity — resolved in the planning baseline:** multiple journal entries per date are allowed; confirm create/edit/delete and draft behavior in the detailed data contract.
6. **Date/time policy — baseline approved (D-187):** local date keys are separate from event instants; preserve historical dates across timezone travel; handle clock uncertainty conservatively; allow explicit backdating; and use the saved week-start consistently. Remaining work is technical edge-case verification for clock/timezone changes, reminder/catch-up deduplication, and date boundaries—not another owner policy choice.
7. **Habit lifecycle — baseline approved (D-187):** archive by default to preserve accessible history; permanent deletion is a separate, clearly explained action. Remaining work is to finalise deletion confirmation/retention wording and verify export/backup and derived-summary behaviour; independent journal entries must remain intact.
8. **Backup/export — scope largely approved; technical contract open:** readable journal export and a versioned complete portable backup are required. Replace/Merge, safety backup, integrity checks, password-protection direction, and conflict preservation are recorded in D-091–D-135. Finish exact schema/version compatibility, conflict review/resolution UX, and Android platform-backup policy.
9. **Encouragement — partially resolved:** outcome-aware and comeback behaviors have specific decisions; remaining optional-tip cadence, opt-out, and per-message dismissal rules must be reconciled against those decisions.
10. **Reminders — core rules approved; edge cases open:** onboarding opt-in, per-habit default time, weekly reflection, and inactivity caps are recorded. Finalise quiet hours, global/per-habit controls, Android permission denial, timezone changes, and delivery deduplication.
11. **Monetisation — no v1 model approved:** do not add ads, rewarded extras, purchases, or subscriptions to initial v1 without an explicit owner decision. Future premium cloud scope is gated separately.
12. Which visual direction best balances distinctiveness, legibility, and calm?
13. Which events qualify for Growth Mark, and should the first release use a simple count rather than an illustration?
14. Should comeback prompts appear once per absence episode, as a non-blocking card, or not at all by default?
15. Should journal archive default to timeline, hybrid timeline/calendar, or another pattern after testing?
16. Which font family and dark-theme behaviour are approved after device and accessibility checks?
17. What is the minimum Android version and physical-device test matrix?

## 3. Technical and operational questions

- - **Stack direction resolved (D-009):** continue native Kotlin + Jetpack Compose. Build, test, APK-install, and device readiness remain unverified and must wait for explicit validation authorisation.
- What database and migration library are supported by the selected stack at implementation time?
- How will Android platform backup and device-to-device transfer interact with the privacy promise, separately from the user-controlled local backup file?
- Before any later premium online backup/cross-device sync release, what provider, consent UX, security/threat model, data-flow, deletion/retention, conflict-resolution, recovery, and monetisation design is approved? This is explicitly outside initial v1.
- Is database encryption needed for the threat model, and what are the usability/recovery costs?
- How will exports be versioned and validated?
- What happens when storage is nearly full or writes fail?
- Which coding environment is available at the time of implementation, and how will its changes remain synchronised with GitHub?
- What exact evidence constitutes a verified native Android milestone?

## 4. Decision rules

Resolve questions that affect data integrity, privacy, daily record semantics, or app architecture before implementation. Lower-risk visual details can be explored with prototypes, but should not be silently finalised by an implementation agent. If a question is deferred, record the chosen safe default and the reason in the decision log. Planning completion does not itself authorise implementation; owner approval must be explicit.
