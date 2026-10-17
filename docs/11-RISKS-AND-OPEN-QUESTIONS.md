# Risks and Open Questions

**Status:** Living register; review at each planning gate. See [Decision Log](09-DECISION-LOG.md) for decision status and supporting research documents.

## 1. Highest-priority risks

| Risk | Impact | Mitigation | Owner/status |
|---|---|---|---|
| Ambiguous missed/rest semantics | Users misunderstand progress; inconsistent summaries | Define outcomes and schedules before implementation; test scenarios | Open |
| Incorrect date/time handling | Records appear on wrong day; comeback messages wrong | Central date service, local-date policy, boundary tests | Open |
| Accidental loss or disclosure of journal data | High trust and privacy impact | Data-flow review, backup tests, deletion/export tests, no sensitive logs | Open |
| Comeback copy feels shaming or intrusive | Users avoid returning | Test copy with users; dismissal control; no assumed reasons | Open |
| Growth Mark becomes arbitrary gamification | Product promise contradicted; users misread score | Tie every visual change to explainable recorded evidence; compare against plain summary | Open |
| Archive layout makes retrieval difficult | Users cannot revisit older reflections quickly | Test timeline, calendar, and hybrid with known-date tasks | Open |
| Typography or colour fails in real use | Small-screen readability and accessibility failures | Compare device samples; test all actual light/dark contrast pairs and large text | Open |
| Proposed stack lacks required platform support | Rework or release delay | Compare stack choices; verify official docs and build spike | Open |
| Replit agent creates web app rather than native app | Wrong deliverable, wasted quota, unreliable build assumptions | Explicit native-app prompt; verify APK/device install; use workflow in docs/19 | Open |
| Cross-environment changes overwrite newer work | Lost code or broken history | GitHub is canonical; pull before resuming Replit; reconcile branches rather than blind overwrite | Open |
| Monetisation damages trust or violates policy | User harm, policy/store risk | Consider no ads; privacy review before SDK choice | Open |
| Scope expands before core flows are reliable | Delayed release and complexity | Enforce v1 non-goals and phase exit gates | Open |
| Accessibility is treated as polish | Some users cannot complete core tasks | Include accessibility in design and release gates | Open |
| Local-only data is unexpectedly backed up | Privacy promise becomes misleading | Inspect and test Android backup and transfer behaviour | Open |
| Metrics encourage perfectionism | Product contradicts its promise | Explain metrics; never make streak the only success signal | Open |

## 2. Open product questions

1. Which schedule types are required in v1: daily, selected weekdays, weekly target, flexible frequency?
2. What are the exact meanings and UI treatments of completed, skipped, planned rest, and no record?
3. Can users customise Tiny/Usual/Extra labels and targets?
4. Can a habit have multiple completions per day?
5. Can a day have multiple journal entries?
6. What is the policy for timezone travel, manual clock changes, and backdated records?
7. What does deleting a habit do to its history, and can the user choose?
8. Which export formats and restore behaviours are required?
9. Should encouragement be configurable, and what is its dismissal persistence?
10. Which reminder patterns and quiet hours are supported?
11. Is any monetisation appropriate for v1, or should the first release be ad-free?
12. Which visual direction best balances distinctiveness, legibility, and calm?
13. Which events qualify for Growth Mark, and should the first release use a simple count rather than an illustration?
14. Should comeback prompts appear once per absence episode, as a non-blocking card, or not at all by default?
15. Should journal archive default to timeline, hybrid timeline/calendar, or another pattern after testing?
16. Which font family and dark-theme behaviour are approved after device and accessibility checks?
17. What is the minimum Android version and physical-device test matrix?

## 3. Technical and operational questions

- Which stack best meets Android quality, offline persistence, notifications, accessibility, and future iOS needs?
- What database and migration library are supported by the selected stack at implementation time?
- How will Android backup and device-to-device transfer interact with the privacy promise?
- Is database encryption needed for the threat model, and what are the usability/recovery costs?
- How will exports be versioned and validated?
- What happens when storage is nearly full or writes fail?
- Which coding environment is available at the time of implementation, and how will its changes remain synchronised with GitHub?
- What exact evidence constitutes a verified native Android milestone?

## 4. Decision rules

Resolve questions that affect data integrity, privacy, daily record semantics, or app architecture before implementation. Lower-risk visual details can be explored with prototypes, but should not be silently finalised by an implementation agent. If a question is deferred, record the chosen safe default and the reason in the decision log. Planning completion does not itself authorise implementation; owner approval must be explicit.
