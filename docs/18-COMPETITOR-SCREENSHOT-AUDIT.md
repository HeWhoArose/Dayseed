# Competitor Screenshot and Flow Audit

**Research date:** 2026-10-17  
**Status:** Initial public-listing evidence reviewed; full interaction-flow audit remains incomplete.  
**Evidence rule:** Google Play screenshots are publisher-provided promotional screenshots. They are useful for inspecting visible screen structure, but they do not prove the current installed app behaves identically. No app was installed or interactively tested in this research pass.

## 1. What was inspected

### Loop Habit Tracker

- Google Play listing: https://play.google.com/store/apps/details?hl=en_US&id=org.isoron.uhabits
- Screenshot inspected: publisher screenshot linked from listing (screen titled “Habits”).
- Screenshot URL: https://play-lh.googleusercontent.com/KrxN9bOtspk63xhtNJx2uyaHZ3GOYK_IJ-VRIhMlh2ChFCQuBxXLWoRJd48uFRU_Rk97Nz3gHiI0eteLn2KDFQ%3Dw526-h296
- Second screenshot inspected: habit detail with score graph and history.
- Screenshot URL: https://play-lh.googleusercontent.com/2nZZDFddPS4-wHVgPvAfeYik3vMTsrV-Mgys3g1A6s-KLCcUWsf1PlYLJfKXGyxUnUHkSKX8sESQkrEx6mQN%3Dw526-h296

**Visible evidence:** The “Habits” screenshot uses a compact table-like weekly grid with rows for habits and day columns; different habit rows have distinct accent colours. The detail screenshot shows an overview/score, a line chart, and a history chart. The listing describes habit scores, flexible schedules, reminders, offline use, and data export. Its current public copy says the habit-strength score increases with repetitions and weakens with missed days; this is an explicit vendor description of the scoring model, not an independent evaluation. The listing also describes the app as ad-free and open source. These are listing claims, not independent tests.

**Design implications for Dayseed:** A compact grid can compare many habits efficiently but may be dense on small screens. Habit detail graphs can provide history but should explain the measure and avoid making streaks the only success signal. A recent user review on the listing mentions poor red visibility in dark theme; treat this as one user's report, not proof of a universal defect. It reinforces the need to test semantic colours in both themes.

### Daylio Journal - Mood Tracker

- Google Play listing: https://play.google.com/store/apps/details?hl=en-US&id=net.daylio
- Screenshots inspected: a promotional journaling/calendar overview and a mood-selection screen.
- Screenshot URLs:
  - https://play-lh.googleusercontent.com/GLa2gMHsOmi3aK7IxYEI_nfUa9gbjYG_iqSq8LNOzz0h6ySkw_tpzKaP0pz1FR6JVQ9gGSqim-5ItqrzGXH6%3Dw526-h296
  - https://play-lh.googleusercontent.com/YBmpZ-Tc2orgU8knXP9vA3k7DhrVSpIV8T5nJTD6zvy-Ca0xPGZE__pDbfgpn6ziB0YguK7oTPorUxqMJndfnQ%3Dw526-h296

**Visible evidence:** One publisher graphic presents a monthly calendar filled with mood/activity symbols and advertises several journaling features; another screenshot shows a simple mood selection step. The current public listing describes one-tap mood tracking, text/photo/audio entries, charts, weekly/monthly/yearly summaries, a “Year in Pixels” view, reminders, optional backup/export, and lists ads plus in-app purchases. These are vendor claims and listing metadata; availability and exact presentation may vary by region/version.

**Design implications for Dayseed:** A calendar can make dated entries discoverable, but dense colourful symbols need a clear legend and must not imply that blank dates are bad. A single-step reflection/mood selection can be quick, but Dayseed must not add mood tracking by implication: its current scope is a micro-journal, and any mood field needs a separate product decision.

## 2. Comparison: what we can and cannot conclude

| Question | Evidence now | Safe conclusion | Still needed |
|---|---|---|---|
| How does Loop show multiple habits? | Public listing screenshot | A weekly grid with habit rows and day columns is shown in promotional material | Install current version; check scrolling, tap targets, editing and accessibility |
| How does Loop show progress? | Public listing screenshot and listing copy | A detail screen shows score and history graphs; public copy says missed days weaken the score | Verify metric definition, date behaviour, dark theme and real interaction |
| How does Daylio start a reflection? | Public listing screenshot | A mood-choice screen is shown | Install current version; test skip, text entry, save/edit and privacy controls |
| How does Daylio support revisiting entries? | Promotional calendar image and listing text | A calendar-style archive is advertised | Verify current calendar navigation, list/detail flow, search, empty dates and accessibility |
| Are these layouts best for Dayseed? | No user tests | Not established | Test comparable prototypes against Dayseed tasks |

## 3. Audit method for the next pass

When a physical Android device and current app access are available:
1. Record app name, package ID, version, install date, Android version, region, and whether screenshots are from live use or the store listing.
2. Inspect first launch, Today/home, habit creation, daily logging, archive/history, entry detail/edit/delete, comeback after inactivity if testable, settings, privacy/export, reminders, light/dark themes, and enlarged text.
3. Capture only non-sensitive screens. Do not use a personal journal entry or account data in screenshots.
4. Record each observation separately from interpretation and recommendation.
5. Do not bypass paywalls or infer paid features from promotional material.
6. Note regional/version differences and update date.
7. Use the same tasks on Dayseed prototypes so comparisons are task-based rather than visual imitation.
8. Do not copy competitors' branding, illustrations, exact copy, or distinctive assets.

## 4. Additional competitors to inspect later

Candidates for a future pass include HabitNow and a privacy-focused journal app, but do not add detailed layout claims until an official listing screenshot or live screen has actually been inspected. Prioritise comparisons that answer a Dayseed decision: quick daily logging, recovery after a break, journal retrieval, offline privacy, and accessible light/dark themes.

## 5. Sources

- Loop Habit Tracker Google Play listing, accessed 2026-10-17: https://play.google.com/store/apps/details?hl=en_US&id=org.isoron.uhabits
- Daylio Journal - Mood Tracker Google Play listing, accessed 2026-10-17: https://play.google.com/store/apps/details?hl=en-US&id=net.daylio
- Android contrast/accessibility guidance: https://support.google.com/accessibility/android/answer/7158390?hl=en
- W3C WCAG 2.2: https://www.w3.org/TR/wcag/

This is an initial screenshot audit, not a complete hands-on competitor usability study.
