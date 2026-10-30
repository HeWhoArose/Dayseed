# Model Recovery and Manual Replacement Policy

**Status:** Active shared project policy  
**Operational companion:** [Multi-model failure recovery policy](RECOVERY_POLICY.md)  
**Model source of truth:** [Dayseed Model Registry](MODEL_REGISTRY.md)

This document governs model-level decisions. It does not replace the existing retry plugin or the operational recovery steps in RECOVERY_POLICY.md.

## 1. Three different events must not be confused

1. **Request retry:** repeat a failed request to the same configured model when the failure is classified as transient and the retry budget permits it.
2. **Task-level fallback:** after a specialist task fails, the coordinator may use that role's already-configured one-hop fallback under RECOVERY_POLICY.md, after inspecting the checkpoint and partial work.
3. **Model replacement:** change the model assigned to an agent. This requires deliberate owner approval and a reviewable configuration change.

A retry is not a model replacement. A task-level fallback is not permission to rewrite an agent's permanent model assignment.

## 2. Bounded retry rules

Follow RECOVERY_POLICY.md and the installed retry plugin:

- The configured policy allows at most three physical attempts total (initial request plus up to two retries).
- Retry only errors classified as transient.
- Stop immediately for authentication, invalid credentials, permission, billing, exhausted-credit, and explicitly identified hard-quota failures.
- Do not blindly retry malformed requests, unknown models, unsupported endpoints, or context-size errors.
- A tool, shell, or test failure is not automatically a provider failure.
- If the error is unclassified, preserve the underlying OpenCode decision and report uncertainty.

**429 nuance:** a generic HTTP 429 may represent a temporary rate limit or an exhausted quota. The current classifier retries a generic 429 unless the error text matches a hard-stop quota/billing pattern. Therefore, it cannot guarantee detection of every provider's daily quota exhaustion. If the provider's message is ambiguous or the same 429 persists, stop and verify the provider's quota/limit status rather than repeatedly retrying.

Do not increase retry counts or delays to work around a hard quota.

## 3. Failure detection and reporting

When a request or agent fails, report:

- The affected agent and configured model ID.
- The provider error category and status, when available.
- Whether the error is transient, permanent, or unclassified.
- How many attempts actually occurred.
- Whether a fallback was invoked and why.
- Whether any files, tool actions, or partial outputs may have been produced.
- The checkpoint/Git state inspected before resuming.
- What remains blocked or unverified.

Do not claim that a model was automatically detected as removed or unavailable unless an actual provider/OpenCode error or verified account state demonstrates it. This policy does not create a background model-availability monitor.

## 4. No unapproved model substitution

- Never silently change an agent's model because another model is available.
- Never silently switch to a paid endpoint, consume paid credits, or assume a model is free based on its name.
- Never change the Executive Lead's model or fallback assignment without explicit owner approval.
- Never modify agent frontmatter as an automatic recovery action.
- Never treat a model's presence in the registry as approval.
- Never treat a configured fallback as permission to exceed the owner's cost or access constraints.

For a child/specialist failure, follow the existing one-hop role-specific fallback process. If the fallback is unavailable, unsafe, or of unknown cost, mark the task BLOCKED and preserve the checkpoint.

If the primary coordinator fails, preserve state and resume only through an owner-selected available model/session using the canonical handover. Do not pretend that the failed coordinator automatically resumed.

## 5. Manual replacement workflow

Use the procedure in Dayseed Model Registry. In short:

1. Identify the failure and verify its cause.
2. Check candidate model identity, account availability, price, quotas, and required capabilities.
3. Run role-specific comparative evaluation.
4. Explain the evidence, trade-offs, and uncertainties to the owner in beginner-friendly language.
5. Obtain explicit approval for the exact provider/model ID and role.
6. Change the agent assignment and registry together on a reviewable branch.
7. Verify the resulting configuration and preserve the previous assignment in the registry history.

If no candidate passes or cost/access cannot be verified, leave the assignment unchanged and mark the task BLOCKED. Manual selection is preferable to an automatic capability guess.

## 6. Safety and project gates

All recovery and replacement actions remain subordinate to AGENTS.md, OWNER_AUTHORITY.md, and the canonical project decision log.

A fallback model cannot open Dayseed's planning gate, approve product decisions, bypass permissions, or authorise application implementation. Recovery must preserve the original task scope and acceptance criteria.
