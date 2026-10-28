# Dayseed v1 Monetisation and Ads Evaluation

**Review date:** 2026-10-28  
**Status:** Research/planning only. Ads are a requested full-launch requirement to evaluate, not approved or implemented. No SDK, ad account, placement, revenue assumption, or release configuration is selected by this document.
**Owner decision:** O-11 / D-198 — the owner says a full launch needs at least ads and requests evaluation for initial v1. The existing hybrid concept remains a candidate; this evaluation does not approve the wider package of rewarded ads, purchases, or a Supporter Pack.

## Executive recommendation

Evaluate a **single, clearly labelled, modest banner placement on a secondary screen** as the smallest v1 monetisation candidate—for example, below Insights content, where it does not interrupt reading or push core controls out of reach. Keep the Today habit-recording path, Journal writing/editing, comeback flow, privacy screens, backup/restore, and deletion flows free of ads. Do not start with app-open ads, launch interstitials, forced ad views, misleading native placements, or ads required to unlock core habit/journal functionality. Keep rewarded ads, in-app purchases, subscriptions and the proposed Supporter Pack out of this first evaluation unless separately requested.

This is a recommendation for the next owner decision, **not a decision to ship**. Actual revenue cannot be forecast credibly without a selected ad network, placement/fill design, target markets, usage/impression expectations and current network terms. Do not use an earnings estimate as evidence for the product decision.

## Confirmed policy constraints to design around

1. **Play Store disclosures:** Google Play requires the developer to declare whether the app contains ads. This includes banners, native ads, display ads, and ads delivered through third-party SDKs; a Play Store “Contains ads” label is shown when applicable. See [Prepare your app for review — Privacy policy and Ads](https://support.google.com/googleplay/android-developer/answer/9859455).
2. **Privacy policy and Data safety:** the Play Store listing and the app need a publicly accessible privacy-policy URL. Data safety disclosures must accurately reflect the app's data collection, use and sharing, including practices of third-party SDKs, and remain consistent with the privacy policy. See [Prepare your app for review](https://support.google.com/googleplay/android-developer/answer/9859455) and [Google Play User Data policy](https://support.google.com/googleplay/android-developer/answer/10144311).
3. **Non-disruptive and honest ads:** ads are treated as part of the app and must follow content-rating/restricted-content requirements. Disruptive ads that interfere with normal use, appear unexpectedly, or trick users into taps are prohibited; an ad that interferes with normal use must be easily dismissible without penalty. See [Google Play Ads policy](https://support.google.com/googleplay/android-developer/answer/9857753) and [Mobile Unwanted Software policy](https://support.google.com/googleplay/android-developer/answer/9970222).
4. **Audience rules must be known:** Dayseed's Play Console target-audience declaration has not been confirmed in the planning evidence. If an audience includes children or users of unknown age, Google Play Families policy adds restrictions, including use of eligible self-certified Ads SDKs for the relevant users and restrictions on ad formats/content. Resolve and declare the intended audience before selecting a network or serving ads. See [Google Play Families policies](https://support.google.com/googleplay/android-developer/answer/9893335) and the [Families Self-Certified Ads SDK program](https://support.google.com/googleplay/android-developer/answer/9900633).
5. **Consent and regional rules:** an ad SDK must be configured for the users/regions served. Google's guidance says app publishers using its consent management platform for European-regulation messages need the UMP SDK so the message can display; relevant users in the EEA, UK and Switzerland require the disclosures/consent described by Google's policy. Determine all intended markets and implement the applicable consent/preference flow before serving ads. See [AdMob consent-management guidance](https://support.google.com/admob/answer/7666519) and [how Google's CMP works](https://support.google.com/admob/answer/16918505).

## Dayseed-specific privacy and product safeguards

- Preserve the product's offline-first promise for core habits, journal, history, insights and comeback use. If the network is absent or an ad fails to load, the user must still be able to use core features; do not block saving or retrieving private records.
- Do not send habit names, local activity records, journal text, journal titles, moods, or comeback draft text to an ad network for targeting. Keep ad requests separate from the personal-record data path. This is a product safeguard; it must be confirmed against the eventual SDK/configuration and network behaviour.
- Assume the chosen SDK may process some device, identifier, diagnostics or advertising-related data according to the selected product and configuration until its exact data practices have been inspected. Do not claim Dayseed has no network/data collection merely because its own records are stored locally.
- Update the privacy policy and Play Console Data safety answers to reflect the actual SDK version, data types, purposes, sharing, retention, consent choices and any opt-out/preference controls. Do not copy generic disclosures without verifying the exact library and configuration.
- Label the ad placement clearly. Do not place it beside habit Complete/Record controls, journal Save/Delete controls, or touch targets where an accidental ad tap could be confused with the app's own action. No ad should imitate Dayseed's cards, navigation or system prompts.
- If a banner cannot fit with large text, keyboard-open screens, or small Android layouts, hide it on that screen rather than compromising accessibility. Avoid introducing ad motion or layout shifts in core flows.

## Decision still required before any ad implementation

After the policy and product evaluation is reviewed, the owner must explicitly choose whether to include the proposed secondary-screen banner in initial v1. If approved later, select the SDK and format only after target-audience, privacy/data-flow, regional-consent, device/toolchain compatibility and test requirements are defined. Do not add rewarded ads, purchase flows or subscriptions under the banner-only decision.

Before release, planned acceptance evidence should cover at least: truthful Play Console “Contains ads” declaration; accurate privacy-policy and Data safety disclosures; consent/preferences for affected regions; age/audience-specific SDK/format requirements; no ad gating of core features; no ads when offline without breaking the app; ad load failure/retry; accessibility and accidental-tap testing; relevant SDK data-flow review; and confirmation that no private habit/journal content is sent to the ad provider. These are requirements for future validation, **not executed tests**.

## What remains unverified or outside this evaluation

- Dayseed's target audience and exact launch countries/regions.
- Ad network/SDK, exact compatible version, data collection, personalised/non-personalised settings, consent configuration, and the actual runtime network traffic.
- Current ad account eligibility, payment/tax arrangements, country-specific legal obligations, realistic revenue, and whether ad revenue justifies the UX/privacy cost.
- Actual manifest, dependencies, Play Console declarations, privacy policy hosting, build, tests and physical-device behaviour.

## Official sources

- [Google Play — Ads](https://support.google.com/googleplay/android-developer/answer/9857753)
- [Google Play — Prepare your app for review (including ads and privacy policy declarations)](https://support.google.com/googleplay/android-developer/answer/9859455)
- [Google Play — User Data policy / Data safety](https://support.google.com/googleplay/android-developer/answer/10144311)
- [Google Play — Families policies](https://support.google.com/googleplay/android-developer/answer/9893335)
- [Google Play — Mobile Unwanted Software policy](https://support.google.com/googleplay/android-developer/answer/9970222)
- [AdMob — consent management platform guidance](https://support.google.com/admob/answer/7666519)
- [AdMob — how Google's CMP works](https://support.google.com/admob/answer/16918505)

**Implementation boundary:** This document is only a planning/policy evaluation. It does not open the planning gate or authorise adding an SDK, accessing a live database, running builds/tests, or shipping monetisation.
