# Dayseed Model Registry

**Registry status:** Initial inventory; no model has been newly evaluated or approved by this document.  
**Last inventory reconciliation:** 2026-10-30  
**Source:** Current model fields in the committed .opencode/agents/*.md files fetched from main for this configuration update.

This registry separates an agent's permanent role from the replaceable model assigned to it. The agent definition and its responsibilities belong to the project; a provider model is an assignment that may become unavailable, change access terms, or be replaced after evaluation.

## 1. Status definitions

| Status | Meaning |
|---|---|
| **EXISTING — NOT EVALUATED OR APPROVED** | The model ID is present in the current agent configuration. This is not an endorsement or proof of current availability, quality, or free access. |
| **CANDIDATE — NOT TESTED** | Discovered for possible evaluation; not approved for project use. |
| **EVALUATED — AWAITING OWNER APPROVAL** | Evaluation evidence is recorded, but the owner has not approved assignment. |
| **APPROVED** | The owner has explicitly approved the model for the stated role and conditions. |
| **DEGRADED / RECHECK REQUIRED** | Previously approved, but availability, quality, pricing, limits, or behaviour needs re-verification. |
| **RETIRED** | No longer approved for new assignments. Preserve the historical record. |

A model ID containing words such as "free" is not sufficient evidence that the endpoint is currently free, unlimited, or available to this account.

## 2. Current agent assignments

All entries below are **EXISTING — NOT EVALUATED OR APPROVED**. They are transcribed from the current agent frontmatter. This inventory does not change any assignment.

| Agent role | Assignment | Exact model ID | Approval status |
|---|---|---|---|
| architecture-auditor | Primary | google/gemini-4-argon | EXISTING — NOT EVALUATED OR APPROVED |
| architecture-auditor-fallback | Fallback | opencode/flint-40b-free | EXISTING — NOT EVALUATED OR APPROVED |
| audit-lead | Primary | opencode/flint-40b-free | EXISTING — NOT EVALUATED OR APPROVED |
| audit-lead-fallback | Fallback | groq/qwen/qwen3.8-27b | EXISTING — NOT EVALUATED OR APPROVED |
| bug-hunter | Primary | groq/qwen/qwen3.8-27b | EXISTING — NOT EVALUATED OR APPROVED |
| bug-hunter-fallback | Fallback | opencode/nemotron-3.5-super-vl-free | EXISTING — NOT EVALUATED OR APPROVED |
| final-reviewer | Primary | opencode/nemotron-3.5-super-vl-free | EXISTING — NOT EVALUATED OR APPROVED |
| final-reviewer-fallback | Fallback | opencode/flint-40b-free | EXISTING — NOT EVALUATED OR APPROVED |
| implementer | Primary | nvidia/poolside/laguna-xs-2.1 | EXISTING — NOT EVALUATED OR APPROVED |
| implementer-fallback | Fallback | groq/qwen/qwen3.8-27b | EXISTING — NOT EVALUATED OR APPROVED |
| project-lead | Primary | google/gemini-3.8-flash | EXISTING — NOT EVALUATED OR APPROVED |
| project-lead-fallback | Fallback | opencode/flint-40b-free | EXISTING — NOT EVALUATED OR APPROVED |
| requirements-auditor | Primary | google/gemini-3.5-flash-lite | EXISTING — NOT EVALUATED OR APPROVED |
| requirements-auditor-fallback | Fallback | opencode/flint-40b-free | EXISTING — NOT EVALUATED OR APPROVED |
| test-auditor | Primary | nvidia/nvidia/nemotron-3.5-super-vl-120b-a12b | EXISTING — NOT EVALUATED OR APPROVED |
| test-auditor-fallback | Fallback | opencode/flint-40b-free | EXISTING — NOT EVALUATED OR APPROVED |
| verification-runner | Primary | opencode/nemotron-3.5-super-vl-free | EXISTING — NOT EVALUATED OR APPROVED |
| verification-runner-fallback | Fallback | groq/qwen/qwen3.8-27b | EXISTING — NOT EVALUATED OR APPROVED |

**Important:** The table records the repository's current assignments, not live provider availability. No live provider account, billing page, quota dashboard, or model endpoint was queried as part of this inventory. Current free-tier status, pricing, context limits, tool support, and availability are therefore **NOT VERIFIED**.

## 3. Required fields for evaluating a candidate

Every candidate evaluation must record:

- Exact provider name and exact model ID (not a display-name guess).
- Date checked and the source used to verify the ID.
- Availability in the owner's actual OpenCode/provider account.
- Pricing and free-tier terms, including request/token quotas, rate limits, and whether payment details or credits are required.
- Context-window limits and practical output limits, with source and date.
- Tool/function calling, structured output, coding, and repository-task support.
- Reliability: timeouts, rate limits, provider errors, and observed availability.
- Independent reasoning quality, instruction following, factuality, and uncertainty handling.
- Performance on the actual role's representative tasks.
- Known weaknesses, safety concerns, and conditions where the model should not be used.
- Evaluation artefacts, test prompts, outputs, and reviewer conclusions.
- Proposed role, fallback rank, approval status, and explicit owner approval reference.

Unknown values must be recorded as **NOT VERIFIED**, not guessed.

## 4. Evaluation method

Evaluate candidates against the role they would actually perform. Do not rank models using marketing claims or a single generic benchmark.

For a fair comparison:

1. Use the same task, repository snapshot, instructions, available tools, and acceptance criteria.
2. Give candidates independent runs; do not show one candidate's answer to another before their initial response.
3. Score factual correctness, evidence quality, instruction compliance, reasoning, useful challenge, tool reliability, output clarity, and cost/availability.
4. Include failure cases: malformed output, unsupported tool calls, timeout, quota exhaustion, unknown model, and context pressure.
5. Keep raw evidence and record the evaluator and date.
6. Separate measured results from subjective observations.
7. Re-test when a provider changes the model, endpoint, quota, or terms materially.

Do not let a model approve itself. Model consensus is not owner approval.

## 5. Manual appointment and replacement procedure

Model replacement is a deliberate owner-controlled configuration change:

1. Detect and describe the current model failure or reason for replacement.
2. Mark the current assignment DEGRADED / RECHECK REQUIRED if appropriate; do not erase its history.
3. Identify candidates whose provider access and cost conditions have been checked.
4. Evaluate candidates using the role-specific method above.
5. Present the strongest candidate, alternatives, evidence, limitations, and fallback implications in beginner-friendly language.
6. Obtain the owner's explicit approval for the exact model ID, role, and any cost/quota conditions.
7. Update the relevant agent frontmatter and this registry together in one reviewable Git change.
8. Verify the exact model ID and configuration after the change; retain the previous assignment in the registry history.

No agent or plugin may silently appoint a replacement model. A fallback assignment already present in an agent file is an existing configured route, not blanket permission to use a paid or unverified endpoint.

## 6. Model whitelist and provider restrictions

No project-level model whitelist or provider restriction is established by this registry. That is a separate design and owner-approval decision. Do not infer that unlisted models have been disabled or hidden in OpenCode.

## 7. Change history

| Date | Change | Approval |
|---|---|---|
| 2026-10-30 | Initial inventory of the 18 existing primary/fallback agent assignments. No model assignments changed; no candidate evaluated or approved. | Inventory only; no model approval granted. |
