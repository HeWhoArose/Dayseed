# Risks and Open Questions

**Status:** Living register; review at each planning gate.

## 1. Highest-priority risks

| Risk | Impact | Mitigation | Owner/status |
|---|---|---|---|
| Ambiguous missed/rest semantics | Users misunderstand progress; inconsistent summaries | Define outcomes and schedules before implementation; test scenarios | Open |
| Incorrect date/time handling | Records appear on wrong day; comeback messages wrong | Central date service, local-date policy, boundary tests | Open |
| Accidental loss or disclosure of journal data | High trust and privacy impact | Data-flow review, backup tests, deletion/export tests, no sensitive logs | Open |
| Comeback copy feels shaming or intrusive | Users avoid returning | Test copy with users; dismissal control; no assumed reasons | Open |
| Proposed stack lacks required platform support | Rework or release delay | Compare stack choices; verify official docs and build spike | Open |
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

## 3. Technical questions
- Which stack best meets Android quality, offline persistence, notifications, accessibility, and future iOS needs?
- What database and migration library are supported by the selected stack at the time of implementation?
- How will Android backup and device-to-device transfer interact with the privacy promise?
- Is database encryption needed for the threat model, and what are the usability/recovery costs?
- What are the minimum supported Android version and device test matrix?
- How will exports be versioned and validated?
- What happens when storage is nearly full or writes fail?

## 4. Decision rules
Resolve questions that affect data integrity, privacy, daily record semantics, or app architecture before implementation. Lower-risk visual details can be explored with prototypes, but should not be silently finalised by an implementation agent. If a question is deferred, record the chosen safe default and the reason in the decision log.