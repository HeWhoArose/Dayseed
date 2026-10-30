# Owner Authority and Approval Boundaries

**Status:** Active shared project policy  
**Authority:** This document reinforces AGENTS.md and the canonical owner decision log at docs/09-DECISION-LOG.md.

## 1. The owner has final authority

The product owner is the final authority for owner-reserved product, scope, philosophy, privacy, security, data-integrity, monetisation, major design, and approval-gate decisions.

Agents may research, analyse, challenge, recommend, document, and prepare alternatives. They may record and enact an owner approval only when the owner gives a clear, explicit written instruction identifying the approval or action. They may never convert recommendations, model consensus, silence, inferred intent, delegated routine defaults, or existing code into owner approval.

An agent must not:

- Approve a decision on the owner's behalf.
- Mark a proposal as owner-approved without an explicit owner response.
- Open, waive, or bypass a project approval gate without the owner's clear, explicit written instruction.
- Infer gate approval from “continue”, review, silence, documentation completion, or an assistant recommendation.
- Expand the authorised scope because implementation appears straightforward.
- Treat a documentation/configuration change as permission to implement product features.
- Change the canonical decision log to manufacture approval.

## 2. Dayseed's current gate

The Dayseed Stage A planning gate is **APPROVED / OPEN as of 2026-10-30 (D-208)**, based on the owner's explicit written instruction. The approved scope is the documented initial-v1 planning baseline on GitHub `main`, including recorded owner decisions through D-208 and explicit deferrals.

This does **not** authorise Stage B or Stage C:

- **Stage B — bounded technical validation: NOT YET AUTHORISED.** Do not inspect application source/build configuration, edit schemas or migrations, inspect the live database, run builds/tests, install an APK, or perform device/runtime validation until the owner separately authorises Stage B in writing.
- **Stage C — feature implementation: NOT AUTHORISED.** It requires the Stage B exit review and separate explicit owner approval of the implementation plan.
- Rendered-screen, task-comprehension, accessibility, runtime privacy/data-flow, backup/restore/delete, migration and device checks requiring a functioning app remain post-build acceptance/technical-validation obligations. D-208 does not mark them passed or waived.

An agent may record/enact an approval only when the owner gives a clear, explicit written instruction. Record the exact approved scope and date in the canonical decision log and approval record. Never infer approval from “continue”, review, silence, documentation completion, or an assistant recommendation.

## 3. Owner decisions and status labels

Use the canonical status vocabulary and decision log. At minimum, distinguish:

- **ACCEPTED BY OWNER:** explicit owner acceptance is present in the canonical decision record or current explicit instruction.
- **PROPOSED:** recommended but not accepted.
- **DEFERRED:** intentionally postponed.
- **REJECTED:** explicitly declined.
- **UNRESOLVED:** requires further evidence or owner choice.
- **BLOCKED:** cannot proceed under current permissions, evidence, or dependencies.

Only record an owner decision when the owner has clearly made it. Preserve historical proposals as historical; do not rewrite them as current approval.

## 4. Configuration work versus product implementation

The owner may separately authorise changes to agent instructions, project policies, model registries, commands, and other development configuration. That authorisation does not open the Dayseed product-planning gate.

Likewise, a Git commit, pull request, successful documentation check, or configuration reload is not evidence that a product feature is approved, implemented, tested, or ready to release.

## 5. Git and external actions

Follow the owner's explicit scope for branches, commits, pushes, pull requests, and merges. Do not merge a pull request, force-push, delete branches, reset user work, or perform destructive recovery without explicit authorisation.

Protect credentials, private journal data, and other sensitive information. Never copy secrets into policy documents or model registries.

## 6. When instructions conflict

If a request appears to conflict with a canonical owner decision or gate:

1. Stop before the prohibited action.
2. Identify the exact conflict and its source.
3. Explain what can still be done safely.
4. Ask for clarification or explicit authorisation when necessary.

Never resolve an owner-authority conflict by choosing the interpretation that permits more implementation.
