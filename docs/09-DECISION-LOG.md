# Decision Log

**Purpose:** Prevent proposals, assumptions, and discarded ideas from being mistaken for approved decisions.

## Status definitions
- **Approved:** explicitly accepted by the product owner.
- **Proposed:** recommended, awaiting approval.
- **Open:** needs a decision or evidence.
- **Deferred:** intentionally outside current scope.
- **Rejected:** considered and deliberately not selected.

## Current decisions and proposals

| ID | Topic | Status | Current position | Next action |
|---|---|---|---|---|
| D-001 | Product promise | Approved direction | “Your progress is more than your streak.” | Preserve across UX and copy |
| D-002 | Android-first | Proposed | Android is the first target; iOS may follow if feasible | Confirm release target and device coverage |
| D-003 | Offline-first | Approved direction | Core habit, journal, history, and comeback features must work offline | Validate architecture and test plan |
| D-004 | Local-only personal data | Proposed baseline | No accounts/cloud sync in v1; review platform backup behaviour | Approve data and backup policy |
| D-005 | Habit effort levels | Proposed | Tiny/Usual/Extra plus completion-only mode; Tiny counts as completion | Test labels and settle customisation |
| D-006 | Daily outcome semantics | Open | Completed, skipped, planned rest, and no record should be distinct | Resolve scheduling and missed-day rules |
| D-007 | Comeback bands | Proposed | 1–2, 3–6, 7–13, 14–29, 30–89, 90+ days | User-test copy and boundary behaviour |
| D-008 | Visual direction | Open | Compare Field Journal, Living Editorial, Quiet Instrument; synthesis is only a hypothesis | Review prototypes and accessibility |
| D-009 | Technical stack | Open | React Native + Expo + TypeScript + SQLite is a candidate, not a commitment | Compare with native Kotlin and verify toolchain |
| D-010 | Monetisation | Open | Subtle secondary-screen ads, optional rewarded extras, optional one-time Supporter Pack are hypotheses | Decide whether monetisation belongs in v1 |
| D-011 | Cloud accounts/sync | Deferred | Excluded from v1 unless explicitly re-approved | Revisit only through a new decision |
| D-012 | AI-generated analysis | Deferred | Excluded from v1 | Revisit only through a privacy/product gate |
| D-013 | Social/leaderboards/pets | Deferred | Excluded from v1 | Do not add implicitly |
| D-014 | Subscription | Not proposed for v1 | No subscription model currently planned | Do not implement without explicit approval |

## Decision change template
For each new consequential decision record:
- ID and date
- Topic and status
- Decision or options
- Rationale and evidence
- Alternatives considered
- Consequences for product, UX, architecture, privacy, tests, and roadmap
- Owner/approval
- Documents that must be updated

## Change rule
A statement in a draft is not approval. Update this log when a decision changes and search for contradictory statements in the rest of the repository.