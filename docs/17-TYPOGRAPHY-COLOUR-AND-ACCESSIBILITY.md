# Typography, Colour, and Accessibility Research

**Research date:** 2026-10-17  
**Status:** Candidate tokens and measurable constraints; visual system not approved.  
**Authority:** `DESIGN.md` is the single authoritative design-token and component reference. This document records research and validation criteria; if a token is approved, update DESIGN.md and link back here.

## 1. Typography candidates

| Candidate | Potential role | Advantages | Risks / validation |
|---|---|---|---|
| **Roboto** (Android/system baseline) | Default UI and body text | Familiar Android rendering; robust small-size behaviour | Less distinctive as a brand voice; confirm actual rendering on supported devices |
| **Inter** | Neutral UI/body alternative | Clear contemporary sans-serif; broad design-system familiarity | Must compare real phone rendering, language coverage, file size, and licensing for the selected font files |
| **Atkinson Hyperlegible** | Accessibility-led body/UI candidate or selective use | Designed to distinguish easily confused letterforms; SIL Open Font License documented in its public repository | May feel more utilitarian than editorial; the upstream GitHub repository is archived as of the research date, so verify a trustworthy distribution source and version before bundling |
| **Noto Sans** | Language coverage / fallback candidate | Broad script coverage and designed for international text | Visual tone may be less distinctive; verify bundled font coverage and licensing for the exact files |
| **Roboto Serif / a restrained serif display face** | Optional editorial headings only | Could give Dayseed a distinctive journal-like voice | Serif headings can use space and become less legible at small sizes; test before adopting |

No typeface is selected by this table. Use the platform default as the initial technical baseline until a side-by-side sample is evaluated. Do not bundle a font solely because it looks attractive in a desktop mockup.

### Type scale to test

These are starting values, not fixed pixel sizes. Use scalable text units and allow content to grow.

| Token | Candidate size | Typical use | Constraints |
|---|---:|---|---|
| display | 32–36 sp | Rare page title / editorial moment | Must wrap and scale; never force fixed-height containers |
| title-large | 26–28 sp | Main screen heading | Test 200% font scaling |
| title-medium | 20–22 sp | Section or card heading | Do not use uppercase as the only hierarchy |
| body-large | 18 sp | Important guidance / journal body | Comfortable reading baseline |
| body | 16 sp | Default content and controls | Avoid dropping below this for core reading text |
| label | 14 sp | Secondary labels and metadata | Check contrast and scaling; not for essential instructions |
| caption | 12–13 sp | Rare supplementary metadata only | Never use for important status, consent, errors, or primary action |

The scale should be adapted to Expo/React Native's actual font-scaling APIs and checked on physical Android devices. Avoid fixed-height text containers and layouts that break when strings wrap.

## 2. Colour approach

Dayseed should not rely on a large palette of emotional status colours. Use a small neutral foundation, one principal growth/brand colour, and a limited semantic palette. A missed date is not a red failure state. A completed action should be understandable from text/iconography as well as colour.

### Exploratory light palette

| Token | Candidate | Intended role |
|---|---|---|
| canvas | `#F7F5EE` | Warm page background |
| surface | `#FFFFFF` | Cards, editors, menus |
| text-primary | `#24352B` | Main text |
| text-secondary | `#59655C` | Secondary text; contrast must be verified before approval |
| border | `#D8DED5` | Dividers and outlines, not text |
| brand | `#286143` | Primary accent / action background |
| brand-on | `#FFFFFF` | Text/icons on brand background |
| soft-growth | `#E4EBDD` | Quiet decorative/supporting surface |
| warm-accent | `#B84A3A` | Rare emphasis, never “failure” |
| focus | `#286143` | Focus indicator candidate; test in every theme |

### Exploratory dark palette

| Token | Candidate | Intended role |
|---|---|---|
| canvas | `#121A15` | Main background |
| surface | `#1D2820` | Raised surface |
| text-primary | `#F3F2E8` | Main text |
| text-secondary | `#C1CBC1` | Secondary text candidate |
| border | `#3C4A3E` | Separators/outlines |
| brand | `#9CCB9C` | Accent on dark surfaces; must be tested for all use cases |
| brand-on | `#17251C` | Text/icons on light brand accent |
| soft-growth | `#293A2D` | Supporting surface |
| warm-accent | `#F0A092` | Rare emphasis |
| focus | `#9CCB9C` | Focus indicator candidate |

These values are exploration tokens only. Never label them approved until all foreground/background pairs have been measured, reviewed in screenshots, and recorded in DESIGN.md. Border and focus requirements differ from text contrast and must be tested separately.

## 3. Contrast requirements and measurement

Use WCAG 2.2 AA as a practical minimum baseline:
- Normal text: at least **4.5:1** against its actual background.
- Large text: at least **3:1** (large is generally at least 18 pt/24 CSS px regular or 14 pt/approximately 18.7 CSS px bold in WCAG terms; map carefully to platform units).
- Meaningful non-text controls, boundaries, and focus indicators need their own contrast/usability checks; do not assume text contrast automatically validates them.
- Aim higher than the minimum for secondary text on low-quality displays or bright outdoor use.
- Do not encode completion/rest/missed states by colour alone.
- Test both themes and all interactive states: default, pressed, disabled, focused, selected, error, confirmation, and empty.

Initial ratio spot-checks (computed from the candidate hex values; not a complete audit):
- `#24352B` on `#F7F5EE`: approximately **11.89:1**.
- White on `#286143`: approximately **7.29:1**.
- `#B84A3A` on `#F7F5EE`: approximately **4.72:1**.
- `#6A756C` on `#F7F5EE`: approximately **4.40:1**, so this muted candidate would fail normal-text AA and must not be used for normal text on that canvas.
- These checks do not validate antialiasing, thin font strokes, opacity overlays, images, gradients, or the final dark palette. Measure the exact rendered pair in the implemented theme.

## 4. Small Android screen and text scaling

Test at minimum:
- a compact Android phone width and a typical modern phone width;
- portrait layout, keyboard open, long journal text, empty states, and validation errors;
- default text size and the largest available Android font-size setting (Android 14+ supports scaling up to 200%);
- display-size enlargement;
- light and dark themes;
- TalkBack navigation, labels, headings, focus order, and control roles;
- reduced-motion settings and disabled animations;
- outdoor/low-brightness conditions where possible.

Design for wrapping and scrolling rather than fixed heights. Primary controls should remain discoverable when text grows. Buttons and hit areas should meet Android guidance; do not shrink controls to preserve a mockup.

## 5. Dark-theme rules

- Dark theme must be designed as a real palette, not a mechanically inverted light theme.
- Avoid pure-black-only assumptions and avoid low-contrast red/green symbols.
- Preserve semantic meaning across themes without relying on hue alone.
- Use semantic tokens (text, canvas, surface, border, accent, focus, success/neutral/warning) instead of raw hex values scattered through screens.
- Respect system theme and make the final user-facing theme preference explicit in the product spec.
- Do not let decorative illustrations lower text contrast or obscure controls.
- Include screenshots for every core flow in both themes in the eventual design review.

## 6. Licensing and font packaging

Before bundling a font, record:
- exact family, version/source URL, downloaded file names and checksum;
- license and required notices;
- script/language coverage;
- app bundle size impact;
- fallback behaviour;
- whether the selected font-rendering approach and native Android Gradle/Compose configuration support the file formats as intended.

Atkinson Hyperlegible's public Google Fonts repository describes the font's legibility goal and SIL Open Font License, but that repository was archived in April 2026. Confirm the current trusted distribution/version before adoption rather than copying files from an old tutorial.

## 7. Exit criteria

- Font samples rendered in the same screen with identical copy and sizes.
- Every required text pair passes contrast checks in light and dark themes.
- Semantic and interactive states are audited, not just brand colours.
- Text at 200% scaling does not clip, overlap, or hide actions.
- Colour-independent indicators and TalkBack descriptions are reviewed.
- Exact font sources and licenses are recorded.
- Owner explicitly approves the token set; approved tokens are maintained only in DESIGN.md.

## 8. Owner palette-direction response — 2026-10-30 (D-207)

The owner selected **B — Soft Botanical** as the palette direction: warm ivory, muted olive and earthy accents. The owner explicitly deferred exact colour values, font selections and token approval.

This is a direction choice, not approval of any specific hex value, font family, contrast pair or production token. Existing light/dark values and font candidates in this document remain exploratory. Continue the contrast audit across semantic text/control states and both themes, test text scaling and TalkBack, and verify font source/version/licensing before presenting an exact token set for approval. Keep DESIGN.md as the sole authority for tokens actually approved later. Do not change it to imply approval from this direction-only response. Stage A planning gate is APPROVED / OPEN under D-208; Stage B implementation remains separately unauthorised.


## Configuration boundary for provisional theme values — 2026-10-30

D-207 selects Soft Botanical as a direction only. Exact colour values, fonts, production tokens, contrast pairings and daypart clock boundaries remain unapproved. Keep candidate semantic token values and local-time daypart start/end boundaries in one central configuration/resource source (with clear provisional status) so they can be changed without editing individual UI components. Do not copy candidate hex values into DESIGN.md as approved tokens, hardcode boundaries across screens, or describe any pairing as WCAG-verified until measured and reviewed. Fixed-theme override, gradual automatic transitions, reduced-motion behaviour and per-theme accessibility still require review.


## 9. Time-of-day theme direction — 2026-10-31 (D-210)

The owner supplied a five-panel theme sheet (`docs/prototypes/dayseed-theme-time-of-day-reference.jpeg`) and instructed that Dayseed's theme direction is defined by the **colours, designs and per-time-of-day pictures**, not by the sample items or wording. The explicit breakdown and code audit are in [Time-of-Day Theme Specification](36-TIME-OF-DAY-THEME-SPECIFICATION.md); D-210 records the decision.

Relevant consequences for this document:

- **Colour:** each daypart now has a named concept — morning "Sage Modern" (soft sage/cream/stone/leaf green), daylight "Field Journal" (parchment/olive/sage/clay), evening "Earth & Clay" (**terracotta/clay primary action** + warm stone/sunset surfaces), fixed light "Minimal Calm" (neutral cream/sage/grey), night "Night Garden" (deep pine, luminous sage, golden moon glow). None of these has an approved hex value; contrast is unmeasured.
- **Imagery:** the reference treats per-daypart header artwork (leaf sprig, botanical branch, sunset hills, crescent moon with foliage) as part of the visual identity, not decoration. The current build has no such artwork; see docs 36 §4.
- **Typography:** the reference shows serif editorial headings. O-7/D-019 still defer the font family to the platform/system baseline, so serif display remains a deferred candidate requiring readability, licensing and bundle-cost review.
- **Accessibility:** the moon-glow evening/night palettes and warm-on-warm morning surfaces are the highest contrast-risk pairs. Measure every foreground/background pair (text, icons, borders, focus rings, selected states) in all five treatments before proposing tokens; reduced-motion must also govern any gradual daypart transition.
