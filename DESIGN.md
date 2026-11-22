# Dayseed Design System

**Authority:** This is the single authoritative reference for design tokens, typography rules, component behaviour, and visual-system decisions.  
**Status:** Research baseline only; candidate tokens and patterns are **not approved for implementation**.  
**Product promise:** **Your progress is more than your streak.**

If another document conflicts with this file about a design token or component rule, update that document or record the conflict; do not create a second token source. Research rationale belongs in the linked research documents. When a candidate is approved, update this file and record the decision in `docs/09-DECISION-LOG.md`.

## Owner planning response — 2026-10-28

The owner accepted platform/system typography and following system light/dark appearance as the baseline. This does **not** approve the candidate colour tokens, exact contrast pairs, custom fonts, spacing/shape tokens, or final components. Keep those items open until measured against the owner-provided reference and accessibility checklist. See [UX/Design Approval Pack](docs/29-UX-DESIGN-APPROVAL-PACK.md) and [the current readiness review](docs/27-PLANNING-COMPLETENESS-REVIEW.md).

## 1. Design principles

- Encouraging and adult; never childish, guilt-inducing, or over-celebratory.
- Warm and distinctive, with editorial clarity and utility-first layouts.
- Progress is shown through understandable evidence, not arbitrary points, ranks, decay, or moral judgement.
- Small Android screens, one-handed use where practical, offline states, and text scaling are first-class.
- Accessibility and privacy are product requirements, not finishing touches.
- Avoid visual changes that cannot be explained by a clear product rule.

## 2. Visual directions under evaluation

- **Field Journal:** warm, tactile, reflective. Risk: decorative styling or weak contrast.
- **Living Editorial:** confident typography and strong hierarchy. Risk: headings compete with daily tasks.
- **Quiet Instrument:** minimal, precise, utility-led. Risk: generic or clinical feel.

**Current hypothesis:** a synthesis of Field Journal warmth and Living Editorial clarity, with Quiet Instrument discipline for controls and data. This is not an approved direction. See [UX and Design](docs/02-UX-AND-DESIGN.md), [Growth Mark and Comeback Research](docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md), [Journal Archive Research](docs/16-JOURNAL-ARCHIVE-RESEARCH.md), and [Typography, Colour, and Accessibility Research](docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md).

**Time-of-day direction (D-210, 2026-10-31):** the owner supplied a five-panel theme sheet (`docs/prototypes/dayseed-theme-time-of-day-reference.jpeg`) defining per-daypart palettes and header artwork, catalogued in [Time-of-Day Theme Specification](docs/36-TIME-OF-DAY-THEME-SPECIFICATION.md). This is a **direction** record, not token approval: the candidate tokens in §3 below remain exploratory, and any exact value still requires measured contrast, scaling and accessibility evidence before it can be promoted into this file.

## 3. Candidate semantic colour tokens

These are candidate values for prototype comparisons, not final approvals. UI code must use semantic token names, never scatter raw hex values through components.

### Light theme

| Token | Candidate | Intended role |
|---|---|---|
| `color.canvas` | `#F7F5EE` | Main background |
| `color.surface` | `#FFFFFF` | Cards, editors, menus |
| `color.text.primary` | `#24352B` | Main text |
| `color.text.secondary` | `#59655C` | Secondary text; verify exact contrast |
| `color.border` | `#D8DED5` | Dividers and boundaries |
| `color.border.control` | `#7F8980` | Boundaries of controls (text fields, outlined buttons, chips). Derived from `color.border` toward `color.text.secondary`; 3.4:1 on the card surface (WCAG 1.4.11). Implemented 2026-11-22 as the Material `outline` role; `color.border` is the `outlineVariant` role and stays for decorative card borders and dividers. |
| `color.brand` | `#286143` | Primary accent / action background |
| `color.onBrand` | `#FFFFFF` | Content on brand background |
| `color.growth.subtle` | `#E4EBDD` | Quiet supporting surface |
| `color.accent.warm` | `#B84A3A` | Rare emphasis, never failure state |
| `color.focus` | `#286143` | Focus indicator candidate |

### Dark theme

| Token | Candidate | Intended role |
|---|---|---|
| `color.canvas` | `#121A15` | Main background |
| `color.surface` | `#1D2820` | Raised surface |
| `color.text.primary` | `#F3F2E8` | Main text |
| `color.text.secondary` | `#C1CBC1` | Secondary text candidate |
| `color.border` | `#3C4A3E` | Separators and boundaries |
| `color.border.control` | `#738175` | Same role on dark surfaces; 3.9:1 on the dark card surface. Code values; the dark `color.border` implemented in code is `#334437`, which differs from the candidate above (both remain candidates). |
| `color.brand` | `#9CCB9C` | Accent candidate on dark surfaces |
| `color.onBrand` | `#17251C` | Content on light brand accent |
| `color.growth.subtle` | `#293A2D` | Supporting surface |
| `color.accent.warm` | `#F0A092` | Rare emphasis |
| `color.focus` | `#9CCB9C` | Focus indicator candidate |

**Rules:** Do not assume these pairs all pass contrast. Measure every actual foreground/background pairing, including pressed, disabled, selected, focused, error, and empty states. Border and focus visibility need separate checks from text contrast. Colour alone must never encode status.

## 4. Typography candidates and scale

No family is selected yet. Compare the system Roboto baseline, Inter, Noto Sans, and Atkinson Hyperlegible using identical screen samples. A restrained serif display face may be tested only if it does not harm compact-screen readability. Record exact font source, version, licence, language coverage, and bundle cost before packaging. The upstream Atkinson Hyperlegible repositories are archived; this is a maintenance/distribution consideration, not a claim that the font is unusable.

Candidate scalable sizes (platform units are not interchangeable; validate actual Jetpack Compose rendering):

| Token | Candidate | Usage |
|---|---:|---|
| `type.display` | 32–36 | Rare editorial moment |
| `type.title.large` | 26–28 | Main screen title |
| `type.title.medium` | 20–22 | Section/card title |
| `type.body.large` | 18 | Guidance and journal body |
| `type.body` | 16 | Default content and controls |
| `type.label` | 14 | Secondary labels |
| `type.caption` | 12–13 | Supplementary metadata only |

Never put essential instructions, consent, errors, or primary status only in caption-sized text. Text must wrap and scale; do not force fixed-height containers.

**Implemented type roles (2026-11-22):** `DayseedTypography` now defines every role the UI uses rather than inheriting Material defaults: `titleSmall` 15, `labelMedium` 13 and `labelSmall` 12 (previously implicit 14 / 12 / 11, which put 64 chips and captions below the caption minimum). Bottom-navigation labels are pinned at 12 and their font scale is capped at 1.15 so they stay on one line. These sizes are candidates under the same not-yet-approved status as the table above.

## 5. Spacing, shape, and layout candidates

These values are starting points to test, not locked implementation constants:
- Spacing base: 4 dp; candidate scale 4, 8, 12, 16, 20, 24, 32.
- Page horizontal inset: candidate 16–20 dp on compact phones.
- Card internal spacing: candidate 16 dp.
- Section separation: candidate 24 dp.
- Corner radius: candidate 12–16 dp for cards; smaller consistent radius for controls.
- Minimum interactive target: follow current Android accessibility guidance; aim for 48 dp touch targets for primary interactions.
- Avoid nested cards, excessive pills, and multiple competing accent colours.
- Keep primary actions visible and reachable when text size grows.

## 6. Component contracts

Components should express product semantics and use the tokens above.

- **Primary action:** one clear high-priority action per screen/section where possible; label describes the outcome.
- **Secondary action:** visually quieter; must remain discoverable and accessible.
- **Habit row/card:** habit name, schedule/context as needed, one clear logging action, and accessible text for selected effort/outcome. Tiny is a valid completion where the habit supports effort levels.
- **Growth Mark:** displays a labelled, inspectable measure tied to real records. No arbitrary points or decay; see the Growth Mark research.
- **Comeback message:** short, dismissible, non-blocking, no presumed reason for absence, and no history reset.
- **Journal entry preview:** date plus a readable excerpt; make truncation explicit and preserve full text in detail view.
- **Empty state:** explain what is absent and offer a gentle next step; never imply failure.
- **Destructive action:** explain consequences and provide a clear cancel path; deletion must follow approved data policy.
- **Status indicator:** use text/shape/icon as well as colour.
- **Compact habit row and quick-log sheet (owner decision 2026-11-22, D-235):** Today shows one compact row per habit with a single round action (checkbox habits: tap to complete, tap to undo, long-press for a rest day; quantity/duration habits: a round **+** that opens the quick-log sheet with *Complete* or *log what you did*). Touch target at least 48 dp; the row opens Habit Detail.
- **Over-target progress bar (D-235):** past 100 % the bar continues as a second lap drawn over the first in `ProgressExtra` (`#E0A72E` light, `#EBBE55` dark) with a thin surface-colour gap; rust stays reserved for the accent. The spoken description states the percentage over target.
- **Progress band chip (owner decision 2026-11-22, D-234):** show the band label only (for example `Halfway`); the exact percentage is always shown beside it. The strict D-040 range (`50–<75%`, kept unchanged in `ProgressBand.shortBadge`) is spoken in plain words in the accessibility description (`ProgressBand.spokenDescription()`), never as symbols.
- **Headings and roles:** screen titles and card titles are marked as headings; clickable rows expose a button role; expandable cards announce `Expanded`/`Collapsed`; the undo banner is a polite live region. Shared building blocks (`DayseedPrimaryButton`, `DayseedSecondaryButton`, `DayseedStatusPill`, `DayseedExpandableCard`, `AdaptiveActionRow`) should be preferred over hand-built equivalents.
- **Large text and narrow screens:** action pairs stack vertically at font scale above 1.15 or container width under 270 dp (`AdaptiveActionRow`); header rows that hold a title plus a chip or button wrap (`FlowRow`) instead of squeezing the title.
- **Dates in copy:** never show ISO `YYYY-MM-DD` to people; use `friendlyDate` (`Sat, Nov 14`).
- **Charts:** label metric, time range, and meaning; expose equivalent information to screen readers.

Do not build a component library for its own sake. Add components only when repeated use and consistent behaviour justify them.

## 7. Accessibility and themes

- Use WCAG 2.2 AA as a minimum practical contrast baseline: normal text 4.5:1; large text 3:1. Check exact rendered combinations.
- Review non-text contrast, focus visibility, and touch-target usability separately.
- Test default and maximum available Android font scaling (including 200% where supported), display scaling, keyboard-open layouts, TalkBack labels/order, reduced motion, and empty/error states.
- Design dark theme intentionally rather than mechanically inverting light theme.
- Support system appearance; whether to expose a manual theme choice remains an open product decision.
- Never rely on colour alone, especially for completed, rest, skipped, or unrecorded states.

## 8. Research and decision links

- [UX and Design](docs/02-UX-AND-DESIGN.md)
- [Design Review Checklist](docs/DESIGN-REVIEW-CHECKLIST.md)
- [Growth Mark and Comeback Research](docs/15-GROWTH-MARK-AND-COMEBACK-RESEARCH.md)
- [Journal Archive Research](docs/16-JOURNAL-ARCHIVE-RESEARCH.md)
- [Typography, Colour, and Accessibility Research](docs/17-TYPOGRAPHY-COLOUR-AND-ACCESSIBILITY.md)
- [Competitor Screenshot Audit](docs/18-COMPETITOR-SCREENSHOT-AUDIT.md)
- [Decision Log](docs/09-DECISION-LOG.md)

## 9. Approval status

- Visual direction: **Open**
- Typeface: **Open**
- Candidate colour palette: **Open**
- Light/dark theme behaviour: **Open**
- Growth Mark representation: **Open**
- Journal archive layout: **Open**
- Component contracts: **Proposed baseline**
- Owner approval/date: not yet recorded

A design token becomes approved only when the owner explicitly approves it and the decision log is updated. Until then, the candidate values may guide comparative prototypes, but must not be represented as final brand decisions.
