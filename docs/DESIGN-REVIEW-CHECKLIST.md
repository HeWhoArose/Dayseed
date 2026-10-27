# Design Review Checklist

Use this checklist before marking UX/design approved. Do not check items merely because a document mentions them. DESIGN.md is the sole authoritative design-token and component reference.

## Product fit
- [ ] The main daily action is obvious.
- [ ] Tiny effort is treated as a valid completion.
- [ ] The interface does not make streaks the only or dominant success measure.
- [ ] Rest, skipped, and unrecorded days are distinguishable according to approved rules.
- [ ] Journaling is optional and independent of habit completion.
- [ ] Growth Mark represents an explainable measure or has been intentionally omitted.

## Navigation and flows
- [ ] First-launch flow is understandable without account creation.
- [ ] Daily logging is quick and avoids unnecessary dialogs.
- [ ] Journal save/edit/delete flows are clear.
- [ ] Archive pattern has been compared with task-based tests (timeline, calendar, or hybrid).
- [ ] History corrections are possible and summaries update.
- [ ] Comeback Flow can be dismissed and never blocks entry to the app.
- [ ] 3-, 14-, and 90-day return cases and a zero-completion habit have been reviewed.
- [ ] Privacy, export, and deletion controls are findable.

## Visual system
- [ ] A visual direction has been explicitly approved.
- [ ] DESIGN.md contains the authoritative semantic tokens and component contracts.
- [ ] Font family selection is based on comparable device samples and has source/licence notes.
- [ ] Typography, spacing, colour, surfaces, and icon rules are documented.
- [ ] Light/dark theme decision is documented.
- [ ] Actual foreground/background contrast ratios are checked for all semantic and interactive states.
- [ ] Screens are checked on small and larger Android displays.
- [ ] Empty, offline, error, and destructive-confirmation states are designed.

## Accessibility
- [ ] Large font and display scaling tested, including the largest available setting (up to 200% where supported).
- [ ] Contrast checked for all semantic states in light and dark themes.
- [ ] Colour is not the only status indicator.
- [ ] TalkBack labels, roles, headings, and focus order reviewed.
- [ ] Touch targets and one-handed reachability reviewed.
- [ ] Keyboard-open and long-text layouts reviewed.
- [ ] Reduced-motion behaviour considered.

## Content and emotional safety
- [ ] Copy avoids guilt, blame, diagnosis, and assumptions about absence.
- [ ] No message implies previous progress is erased.
- [ ] Users are not pressured to explain why they were away.
- [ ] Dismissal preferences are respected.
- [ ] Empty dates and zero-completion states are neutral.
- [ ] Language has been reviewed with representative users where feasible.

## Evidence and implementation readiness
- [ ] Competitor claims are labelled as observed, vendor-claimed, or unknown.
- [ ] Public store screenshots are not misrepresented as live app testing.
- [ ] Research limitations and outstanding decisions are recorded.
- [ ] Current owner-review pack in docs/29-UX-DESIGN-APPROVAL-PACK.md is reconciled with approved decisions; draft concepts are not represented as final UI.
- [ ] The native-app implementation gate and GitHub/AI Studio handoff rules are understood.

## Approval
- Reviewer:
- Date:
- Outstanding issues:
- Decision: **Approved / Changes required / Deferred**

Do not mark approved until outstanding issues have owners and the decision is recorded in docs/09-DECISION-LOG.md.
