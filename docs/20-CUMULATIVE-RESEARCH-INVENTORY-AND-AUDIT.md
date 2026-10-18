# Cumulative Research Inventory and Documentation Audit

**Audit date:** 2026-10-17  
**Repository:** `HeWhoArose/Dayseed`, branch `main`  
**Status:** Initial repository-wide inventory completed; planning gate remains closed. This is a documentation audit, not an approval to build the app.

## Purpose

This document distinguishes research that is recorded, research that is only a proposed method, and work that still needs evidence or owner decisions. A document existing in Git does not mean its recommendations have been validated or approved.

## Audit method and limits

- Enumerated the repository tree on `main` and reviewed the headings, status statements, and open-question language in the 26 Markdown files currently present.
- Checked relative Markdown file links in all 26 files against the repository file inventory. **No missing relative file targets were found.** This check validates target-file existence, not every heading anchor or external URL.
- Compared the documented state across the README, charter, product specification, design references, architecture, decision log, risk register, roadmap, research plan, and handover.
- This pass was a structural and status audit, not a complete line-by-line semantic review of every claim or a fresh independent verification of every external source. That deeper audit remains a planning task.
- Competitor evidence currently includes public Google Play listing/screenshot material. It does not claim hands-on testing of installed competitor apps or user research with Dayseed's target audience.

## Inventory: what exists and how complete it is

| Area | Current documentation/evidence | Assessment | Remaining work |
|---|---|---|---|
| Vision, audience, product principles | `00-PROJECT-CHARTER.md`, `01-PRODUCT-SPECIFICATION.md` | Documented baseline; owner approval is still needed for unresolved semantics | Review requirements, choose launch scope, validate key assumptions with target users |
| Habit effort levels and records | Product spec, technical architecture, decision log | Proposed model is written down; not yet usability-validated | Decide completion-only versus Tiny/Usual/Extra behaviour, customisation, skipped/rest/missed semantics, schedule and editing rules |
| Micro-journal and archive | Product spec, UX, `16-JOURNAL-ARCHIVE-RESEARCH.md` | Alternatives compared; timeline/calendar/editorial hybrid is a prototype recommendation only | Test archive navigation, multiple entries/day, search/filter needs, editing/deletion and empty states |
| Comeback experience | `05-COMEBACK-AND-ENCOURAGEMENT.md`, `15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md` | Copy, candidate inactivity bands and state tests documented as hypotheses | Decide activity signals, boundaries, dismissal/frequency controls and distinguish app inactivity from habit inactivity; test language with users |
| Growth mark / progress summary | `15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md` | Candidate principles and measures documented; no approved metric | Test comprehension and emotional impact; decide whether the feature is a mark, collection of moments, or another transparent summary |
| Navigation and primary flows | `02-UX-AND-DESIGN.md`, `UX.md` | Information architecture and flows are exploratory | Create testable screen/flow prototypes and test with representative users on small Android screens |
| Visual directions and design system | `DESIGN.md`, design checklist, UX docs | Three directions and token/component candidates are recorded; visual direction and values remain unapproved | Choose/test a direction; verify every actual colour pairing, typography, touch targets, dark theme, maximum text scaling, TalkBack and reduced motion |
| Typography and accessibility | `17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md`, `DESIGN.md` | Standards and candidates documented; actual design validation not complete | Measure contrast for real token pairs, verify font licence/distribution/version, test at large text sizes and with accessibility services |
| Competitor landscape | `10-RESEARCH-AND-COMPETITORS.md` | This is a research plan, not a completed market study | Complete a bounded competitor matrix and source log; separate vendor claims from observed evidence |
| Competitor screenshots | `18-COMPETITOR-SCREENSHOT-AUDIT.md` | Public listing screenshots/copy for Loop Habit Tracker and Daylio reviewed | Inspect additional relevant competitors and, if access permits, hands-on core flows; record version, region, date, and observation limits |
| Android platform and build routes | `13-ANDROID-PLATFORM-RESEARCH.md` | Official platform/build documentation reviewed at planning level | Confirm minimum Android/API target and device coverage; recheck current toolchain and policy details before the spike |
| Framework and persistence | `03-TECHNICAL-ARCHITECTURE.md`, `22-IMPLEMENTATION-REALITY-AUDIT.md` | Native Kotlin/Jetpack Compose and SQLiteOpenHelper source exists on main; build/test/device readiness is unverified | Confirm native stack as active baseline, verify build/tests, resolve SQLite migration and owner-approved data-contract gaps |
| Offline-first, dates and local data | Architecture, data/privacy, testing docs | Principles and candidate model documented | Finalise date/time-zone policy, migration strategy, restore/backup implications, conflict-free local record rules and failure recovery |
| Privacy, export and deletion | `04-DATA-AND-PRIVACY.md` | Proposed privacy baseline and checklist documented | Decide device backup/encryption posture, export format, deletion guarantees, crash reporting/analytics policy; review platform behaviour and legal obligations before release |
| Reminders | Android research, product spec, risk register | Permission/platform constraints researched; product behaviour remains open | Define flexible timing, quiet hours, rescheduling, denial handling, reboot handling and expectations around OS-delayed delivery |
| Monetisation | `06-MONETISATION.md`, Android research | Non-coercive candidate model documented; not validated or approved | Confirm whether ads/supporter purchase are in initial release; evaluate SDK/privacy/consent and store-policy impact only after core product validation |
| Testing and release quality | `07-TESTING-AND-QUALITY.md`, roadmap | Test strategy written at a high level; stack-specific tooling pending | Turn requirements into a traceable acceptance matrix, test cases and release gates after stack approval |
| AI Studio/GitHub workflow | `19-AI-STUDIO-GITHUB-WORKFLOW.md` | GitHub-canonical workflow and synchronization guidance documented | Use only after owner opens implementation gate; verify real native Android install, not a web preview |
| Governance and open decisions | Decision log, risks, roadmap, handover | Status vocabulary and approval gate documented | Reconcile each open question to a decision ID, owner, evidence need, safe default, and revisit trigger |
| Research sources and reproducibility | Individual research documents and source sections | Some official and vendor sources recorded; evidence depth varies by topic | Audit source URLs, publication/update dates, primary-source preference, exact support for claims and links; add source log where absent |
| Repository navigation and relative links | 26 Markdown files on `main` | Relative Markdown file-target check passed: no missing targets found | Run heading-anchor checks and a deeper content-level consistency audit; re-run after documentation edits |
| App implementation/build | Native Android source exists on main | Source implementation exists; build/test/APK/device status is **unverified** | Audit and validate existing app; do not scaffold a competing stack. Follow current owner authorization for further implementation |

## Priority order for remaining work

### P0 — Resolve planning blockers

1. Build a decision/evidence matrix from `docs/09-DECISION-LOG.md` and `docs/11-RISKS-AND-OPEN-QUESTIONS.md`; ensure every high-impact unknown has a decision ID, owner, safe default, and revisit trigger.
2. Define the habit day-record state machine: completed (including Tiny), skipped by choice, planned rest, and no record; specify how schedules and historical edits affect derived “missed” states and insights.
3. Define date semantics centrally: local date key, travel/time-zone changes, manual clock changes, daylight-saving behaviour where applicable, and comeback-band calculation.
4. Decide journal cardinality and archive behaviour: one or multiple entries per day, edit/delete, archive navigation, and whether search/filter is v1.
5. Decide reminder contract and privacy/backup/export/deletion expectations without promising exact delivery where Android cannot guarantee it.
6. Confirm Android minimum version/device coverage and whether iOS is a future aspiration or an active architectural constraint.
7. Run a line-by-line cross-document contradiction audit and trace each requirement to an acceptance test.

### P1 — Validate experience before implementation

8. Create low-fidelity, linked prototypes for first launch, Today/habit completion, custom habit creation, journal write/archive/detail/edit/delete, insights, settings/privacy, and each comeback tier.
9. Conduct small, documented usability sessions with people who match the intended audience; record tasks, observed failures, wording confusion, and resulting decisions. Do not present informal feedback as statistically representative.
10. Complete a bounded competitor comparison across direct habit trackers and adjacent journaling/self-care apps. Record version/region/date and distinguish screenshots/listing claims from hands-on observations.
11. Finalise a candidate visual direction, then measure actual contrast pairings and test large text, TalkBack order/labels, touch targets, keyboard layouts, dark theme, empty/error states and motion settings.
12. Test the effort-level labels and growth-mark concept for clarity, shame/pressure risk, and whether users understand that smaller effort counts.

### P2 — Validate technical feasibility after explicit approval

13. Revisit the official-source stack comparison and make D-009 an explicit owner decision.
14. Audit and validate the existing native app using `docs/22-IMPLEMENTATION-REALITY-AUDIT.md` and `docs/19-AI-STUDIO-GITHUB-WORKFLOW.md`; do not create a competing scaffold.
15. Prove install/launch on a physical Android device, durable SQLite writes, migration, offline use, reminder lifecycle/permission denial, and synthetic export/delete. Record actual SDK, dependency versions, device/API, commands, results and failures.
16. Update the roadmap and handover from measured results; do not turn a successful scaffold into permission to implement all features.

## Cross-document consistency findings

- The repository contains native Kotlin/Compose source, but runtime/build correctness remains unverified. Keep visual direction and unresolved product semantics clearly marked until approved.
- The competitor research plan and screenshot audit are distinct artifacts. The latter is partial evidence and does not complete the former.
- The roadmap includes both pre-implementation planning and later implementation phases. The later phases are future work, not current authorisation.
- The research documents contain concrete recommendations, but those recommendations must not silently become product decisions.
- No missing relative Markdown file targets were found in the 26-file inventory. Heading fragments, external links, claim-level source support, and full semantic consistency still require a dedicated follow-up pass.
- The decision log and risk register should be treated as the authoritative unresolved-work ledgers; update them whenever evidence or owner decisions change.

## Planning-gate exit checklist

The planning gate must remain closed until all of the following are evidenced or explicitly deferred with a safe default and revisit trigger:

- [ ] Product scope and core day-record semantics reviewed by the owner.
- [ ] High-impact questions are linked to decision IDs and risk entries.
- [ ] Journal model, date/time policy, reminder contract and privacy/data-loss behaviour are specified.
- [ ] Screen/flow prototypes and a user-validation plan exist; critical flows have been tested or the remaining risk is explicitly accepted.
- [ ] Design direction is selected or explicitly deferred; measurable accessibility checks are ready.
- [ ] Competitor claims have a traceable evidence log and limitations are clear.
- [ ] Stack decision is made by the owner, or the gate explicitly authorises only the bounded spike needed to decide it.
- [ ] Acceptance tests map to product requirements and critical risks.
- [ ] Docs are internally consistent, links and sources are checked, and handover is current.
- [ ] The owner explicitly approves the gate in writing.

**Until runtime evidence is collected:** distinguish existing source from verified build/test/device behaviour; do not create a competing stack or mark features complete without evidence.
