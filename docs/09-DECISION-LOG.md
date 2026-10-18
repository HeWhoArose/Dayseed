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
| D-007 | Comeback bands | Proposed | 1–2, 3–6, 7–13, 14–29, 30–89, 90+ days | Test boundaries, copy, date policy, and dismissal behaviour |
| D-008 | Visual direction | Open | Compare Field Journal, Living Editorial, Quiet Instrument; synthesis remains a hypothesis | Prototype and review with accessibility checks |
| D-009 | Technical stack | Reconcile required | Current `main` contains Kotlin + Jetpack Compose with Android SQLiteOpenHelper, repository, ViewModel/StateFlow, and Compose screens. This is the observed implementation baseline, not proof of successful build/test or final architecture approval. | Audit source/build/tests; owner confirms whether to continue native Kotlin; remove obsolete Expo assumptions from active docs |
| D-010 | Monetisation | Open | Secondary-screen ads, optional rewarded extras, and one-time Supporter Pack are hypotheses | Decide whether monetisation belongs in v1 |
| D-011 | Cloud accounts/sync | Deferred | Excluded from v1 unless explicitly re-approved | Revisit only through a new decision |
| D-012 | AI-generated analysis | Deferred | Excluded from v1 | Revisit only through a privacy/product gate |
| D-013 | Social/leaderboards/pets | Deferred | Excluded from v1 | Do not add implicitly |
| D-014 | Subscription | Not proposed for v1 | No subscription model currently planned | Do not implement without explicit approval |
| D-015 | Android build route | Evidence required | Gradle/Kotlin Android project and source files exist on main; actual build, install, and device-test status still needs evidence. | Run and record the actual Gradle build/test and device installation when authorised |
| D-016 | Growth Mark | Open | Prefer testing a transparent “Collected moments” measure against a plain editorial summary; no arbitrary score or time-based decay | Define qualifying records and test comprehension |
| D-017 | Comeback presentation | Open | Prototype a short, dismissible, non-blocking return card/page; 3-, 14-, 90-day and zero-completion examples documented | Decide trigger, elapsed-day policy, dismissal persistence, and copy |
| D-018 | Journal archive | Open; hybrid proposed | Test recent chronological timeline with secondary calendar date-jump and editorial entry detail | Compare with timeline-only and calendar-first tasks |
| D-019 | Typography and colour | Open | Candidate font families and light/dark semantic palettes documented in DESIGN.md; not approved | Compare real device samples and verify every contrast pair |
| D-020 | Design authority | Proposed governance rule | DESIGN.md is the sole authoritative design-token and component reference; research docs are supporting evidence | Enforce in implementation instructions and audit |
| D-021 | Competitor evidence | Open | Store screenshots give limited visual evidence; no live app interaction audit has been completed | Inspect current app flows on Android before stronger claims |
| D-022 | AI Studio/GitHub workflow | Proposed | AI Studio is the active AI-assisted development environment; GitHub `main` is the canonical shared source of truth. Reconcile from latest remote state before work and commit auditable changes. | Follow `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md` |

## Evidence and research links

- [Stack Comparison](14-STACK-COMPARISON.md)
- [Growth Mark and Comeback Research](15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
- [Journal Archive Research](16-JOURNAL-ARCHIVE-RESEARCH.md)
- [Typography, Colour, and Accessibility Research](17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
- [Competitor Screenshot Audit](18-COMPETITOR-SCREENSHOT-AUDIT.md)
- [AI Studio–GitHub Workflow](19-AI-STUDIO-GITHUB-WORKFLOW.md)
- [Android Platform and Build-Route Research](13-ANDROID-PLATFORM-RESEARCH.md)

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

### Follow-up evidence note (2026-10-17)

Historical note: the stack comparison was researched before the current native Kotlin source appeared in `main`. It must not be used as the active architecture specification. See `docs/22-IMPLEMENTATION-REALITY-AUDIT.md`.
