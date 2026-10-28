# Monetisation

**Current owner position (O-11, 2026-10-28):** the owner states that at least ads are needed for a full launch and requested evaluation for initial v1. See [Monetisation and Ads Evaluation](33-MONETISATION-AND-ADS-EVALUATION.md). This request is not approval to add an ad SDK or ship a placement; the banner-first option remains proposed pending an explicit follow-up choice.

**Status:** Hypothesis only; no ad network or purchase system is approved.

## 1. Principles
Monetisation must not undermine the core promise or exploit a user's lapse, journal content, or emotional state. Habit tracking, journaling, history, and Comeback Flow must remain usable without payment. Never target ads based on journal text or inferred wellbeing.

## 2. Candidate model
A restrained hybrid model to evaluate:
- Small, subtle banner placement on suitable secondary screens only if it does not disrupt reading, logging, accessibility, or privacy.
- Optional rewarded ads for cosmetic extras or clearly nonessential content, only after the user deliberately chooses to watch.
- Optional one-time Supporter Pack for themes or appearance customisation.

This is not an approved release decision. Validate platform policies, actual SDK behaviour, and whether advertising fits the product's privacy promise.

## 3. Never gate
Do not put core habit creation, daily completion, journaling, personal history, editing, data export/deletion, or comeback support behind ads or payment. Never require watching an ad to repair a streak, recover history, or dismiss a guilt-based prompt.

## 4. Ads and privacy
- No journal text, habit names, or private records in ad requests.
- No ads in the middle of writing or saving a journal entry.
- No blocking interstitial at launch or after a comeback prompt.
- Ads must fail gracefully offline and must not block core UI.
- Provide clear disclosures and comply with applicable store policies and local laws.
- Review consent requirements, child-directed implications, SDK permissions, identifiers, and data handling before selecting a provider.

## 5. Purchases
A one-time purchase should be optional, clearly priced, and restorable where the platform requires it. Purchases must not create a false sense that personal progress is at risk. Subscription billing is not proposed for v1.

## 6. Validation
Before approval, compare user trust, implementation cost, expected maintenance, store policy requirements, privacy impact, and accessibility. Do not invent revenue projections or conversion rates without evidence. A no-ads release is **not the current owner preference**: the owner said at least ads are needed for full launch. Do not silently ship an ad-free v1; reconsider that scope only if the owner explicitly revises this requirement.

## 7. Decision gate
Online backup and cross-device sync are intended as premium future capabilities, but they are not in the initial v1 release and do not imply an approved subscription, price, purchase provider, or monetisation model. No monetisation SDK, ad placement, cloud service, or purchase implementation begins until the product owner approves the model and the relevant privacy/security review is complete.