# Dayseed — Original Logo Identity & Brand Asset Specification

**Brand Name:** `Dayseed`  
**Official Product Motto:** `"Your progress is more than your streak."`

---

## 1. Creative Concept & Symbol Architecture

The Dayseed emblem is built around four distinct, harmonious vector forms on a master `240 × 240` coordinate grid (`viewBox="0 0 240 240"`):

1. **Cohesive Opening Seed Body (`#opening-seed-body`):**
   - A dominant, grounded ovate seed (`x: 72..168`, `y: 108..184`) with a whole rounded base and an organic crown cleft (`y: 108..148`) parting at the top to release new growth.
   - Communicates potential, patience, and a personal foundation.
2. **Primary Rising Sprout Stem & Right Leaf (`#sprout-stem-and-primary-leaf`):**
   - A poised central stem rising naturally out of the seed's open crown cleft (`x: 115..125`, `y: 138`) and sweeping smoothly into the larger primary leaf (`x: 115..183`, `y: 34..138`).
   - Expresses steady upward continuity and quiet confidence.
3. **Secondary Young Left Leaf (`#sprout-leaf-left`):**
   - A slightly smaller leaf unfurling at a gentler angle to the upper-left (`x: 60..111`, `y: 61..101`), separated from the stem and seed crown by a clean negative-space channel.
   - Introduces natural organic asymmetry so the emblem feels alive rather than mechanically mirrored.
4. **Restrained Open Horizon / Day-Cycle Cradle Arc (`#horizon-arc`):**
   - A single tapered, open lower crescent (`x: 49..191`, `y: 166..206`) cradling beneath the seed body.
   - Completely open across the top and sides (~125° lower arc with tapered rounded terminals), evoking a day-cycle horizon or ongoing journey while remaining unmistakably distinct from any circular progress ring, loading indicator, or completion meter.

---

## 2. Semantic Colour Direction (`DESIGN.md` & `Color.kt` Authority)

No unapproved global theme colours are introduced. All logo variants use existing canonical Dayseed semantic tokens:

| Element | Primary Colour Version (Light / Warm Paper `#F7F5EE`) | Reversed Version (Dark Surfaces `#121A15` / `#1D2820`) | Monochrome Version (1-Colour Silhouette) |
| :--- | :--- | :--- | :--- |
| **Primary Sprout Stem & Right Leaf** | `#286143` (`LightBrand` — Botanical Green) | `#9CCB9C` (Dark-mode Botanical Green, `DESIGN.md`) | `#24352B` (`LightTextPrimary`) |
| **Secondary Young Left Leaf** | `#558B6E` (`TinySproutColor` — Fresh Sage) | `#88B38C` (`DarkBrand` — Soft Sage) | `#24352B` (`LightTextPrimary`) |
| **Opening Seed Body** | `#24352B` (`LightTextPrimary` — Deep Forest-Earth) | `#F3F2E8` (`DarkTextPrimary` — Warm Ivory) | `#24352B` (`LightTextPrimary`) |
| **Open Horizon Cradle Arc** | `#AC5530` (`EveningBrand` — Warm Terracotta-Earth) | `#D8B984` (`NightGoldAccent` — Warm Gold) | `#24352B` (`LightTextPrimary`) |
| **Wordmark (`Dayseed`)** | `#24352B` (`LightTextPrimary`) | `#F3F2E8` (`DarkTextPrimary`) | `#24352B` (`LightTextPrimary`) |

---

## 3. Five Independent Vector SVG Deliverables (`docs/brand/`)

All five deliverables are standalone, transparent-background SVG files containing pure vector `<path>` geometry with zero embedded raster images, zero filters, and zero external font dependencies:

| Deliverable | File Path | `viewBox` | Description |
| :--- | :--- | :--- | :--- |
| **A. Symbol-only logo** | `docs/brand/dayseed-logo-symbol.svg` | `0 0 240 240` | Primary colour seed, asymmetric two-leaf sprout, and open horizon arc without wordmark. |
| **B. Horizontal logo** | `docs/brand/dayseed-logo-horizontal.svg` | `0 0 700 240` | Independent symbol emblem (`#dayseed-emblem`) followed by the outlined humanist sans-serif vector wordmark `Dayseed` (`#dayseed-wordmark`). |
| **C. Stacked logo** | `docs/brand/dayseed-logo-stacked.svg` | `0 0 480 360` | Independent symbol emblem centered above the outlined vector wordmark `Dayseed`. |
| **D. Monochrome logo** | `docs/brand/dayseed-logo-monochrome.svg` | `0 0 240 240` | Complete symbol emblem rendered in a single solid colour (`#24352B`), using intentional negative-space channels (`>= 6 px` on `240 × 240`) so all four forms remain distinct. |
| **E. Reversed logo** | `docs/brand/dayseed-logo-reversed.svg` | `0 0 700 240` | Horizontal lockup in dark-surface reversed tokens (`#9CCB9C`, `#88B38C`, `#F3F2E8`, `#D8B984`) for `#121A15` / `#1D2820` dark backgrounds. |

---

## 4. Android Launcher Icon & In-App Integration

1. **Android Adaptive Launcher Icon (§9):**
   - `app/src/main/res/drawable/ic_launcher_background.xml`: Canonical warm Field Journal paper canvas (`#F7F5EE`).
   - `app/src/main/res/drawable/ic_launcher_foreground.xml`: Pure `<vector>` (`108dp × 108dp`, `viewportWidth="108"`, `viewportHeight="108"`) with the master Dayseed symbol positioned strictly within the `66dp` safe zone (`x: 30.27..77.72`, `y: 26.05..81.95`, centered at `(54, 54)`).
   - `app/src/main/res/drawable/ic_launcher_monochrome.xml`: Pure single-colour `<vector>` silhouette for Android 13+ themed icons.
   - `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` & `ic_launcher_round.xml` (and `mipmap-anydpi/`): Adaptive icon definitions binding `@drawable/ic_launcher_background`, `@drawable/ic_launcher_foreground`, and `@drawable/ic_launcher_monochrome`.
   - `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png` & `ic_launcher_round.png`: Rasterized square and circular-masked fallback PNGs (`48px`, `72px`, `96px`, `144px`, `192px`) generated directly from the master vector paths.
2. **Standalone VectorDrawables & Compose Brand Components (§10):**
   - `app/src/main/res/drawable/ic_dayseed_logo_symbol.xml` & `ic_dayseed_logo_monochrome.xml`
   - `app/src/main/java/com/example/dayseed/ui/components/DayseedBrandLogo.kt`:
     - `DayseedLogoSymbol`: Reusable symbol composable supporting `AUTO`, `PRIMARY`, `REVERSED`, and `MONOCHROME` modes.
     - `DayseedHorizontalLockup`: Reusable horizontal lockup (`DayseedLogoSymbol` + `Dayseed` wordmark).
     - `DayseedOpeningBrandLockup`: Stacked opening/onboarding/about lockup displaying the emblem, the wordmark **`Dayseed`**, and the official motto **`Your progress is more than your streak.`** in smaller supporting typography below.

---

## 5. Quality-Control Review (§12)

| # | Context | Verification Result |
| :--- | :--- | :--- |
| 1 | **Full size on light background (`#F7F5EE`)** | High contrast (`#24352B` seed/wordmark, `#286143` primary leaf, `#558B6E` secondary leaf, `#AC5530` horizon arc against `#F7F5EE`). Warm, calm, and legible. |
| 2 | **Full size on dark background (`#121A15` / `#1D2820`)** | Reversed palette (`#F3F2E8` seed/wordmark, `#9CCB9C` primary leaf, `#88B38C` secondary leaf, `#D8B984` horizon arc) provides crisp contrast without harsh glare. |
| 3 | **Single-colour monochrome** | `dayseed-logo-monochrome.svg` and `ic_launcher_monochrome.xml` maintain complete separation of the horizon arc, seed cleft, stem, and both leaves via negative space alone. |
| 4 | **Android adaptive icon preview** | Foreground bounds (`30.27..77.72`, `26.05..81.95`) sit comfortably inside the `21..87` (`66dp`) safe zone with balanced visual weight around `(54, 54)`. |
| 5 | **Circle crop** | Verified via `ic_launcher_round.png` across all densities; generous margin around the upper leaf tip and lower horizon arc prevents clipping. |
| 6 | **Rounded-square crop** | Optical center aligns with the seed crown and lower stem; corners remain unobstructed. |
| 7 | **Small size (`48 × 48 px`)** | Tested in `mipmap-mdpi/ic_launcher.png` (`48 × 48 px`); seed cleft, two asymmetric leaves, and lower horizon curve remain immediately distinct. |
| 8 | **Very small size (`32 × 32 px`)** | Verified at `32 × 32 px`; no thin hairlines, veins, or fragile root details collapse or blur. |
| 9 | **Horizontal header lockup** | `dayseed-logo-horizontal.svg` (`700 × 240`) pairs the emblem with the outlined `Dayseed` wordmark along a balanced optical baseline. |
| 10 | **Splash / opening screen with motto** | `DayseedOpeningBrandLockup` renders the emblem, `Dayseed` wordmark, and `"Your progress is more than your streak."` with calm hierarchy and no second tagline. |
