# Multi-model failure recovery policy

This is the failure-handling contract for this OpenCode project configuration. It supplements the repository's own rules; it never overrides the project's `AGENTS.md`, decision log, safety boundaries, or current approval gates.

## 1. Failure classes

- **Transient transport/server failure:** the local retry plugin may retry only classified temporary failures. It allows at most three physical attempts total (the initial request plus up to two retries), with bounded delays. OpenCode itself also has a hard retry ceiling.
- **Temporary 429 rate limit:** retry after a short bounded wait only when the error does not look like a daily quota, exhausted credits, billing problem, or provider entitlement restriction.
- **Authentication / invalid key / expired token / permission / billing / hard daily quota:** stop retries immediately. Do not ask the model to retry the same request. Log the non-secret error metadata and use the task-level fallback if the task is safe to continue. A fallback model ID is not proof of free billing: only use endpoints the owner has confirmed are covered by a free quota; otherwise mark the task BLOCKED and ask before selecting a potentially paid route.
- **Bad request, unknown model, unsupported endpoint, context-size issue:** do not blindly retry. Check configuration or follow OpenCode's context-compaction path. Do not mistake context compaction for a general provider fallback.
- **Malformed answer / unsupported tool call:** validate the returned artifact. One task-level retry with a narrower request or an alternate role-specific agent is permitted; repeated malformed results become BLOCKED.
- **Tool, shell, or test failure:** this is not a model-provider failure. Read the actual output, fix the underlying issue if authorised, and rerun the check. Never report a successful test without command/CI evidence.

## 2. Task-level provider failover

The retry plugin does not hot-swap the model in the middle of a tool-writing turn. That is deliberate: a request may have already edited files or executed tools before the failure becomes visible.

When a **child/specialist agent** fails, the coordinator must:
1. Mark that subtask as `FAILED_PROVIDER` or `BLOCKED`, not complete.
2. Read `.opencode/recovery/provider-errors.jsonl` if it exists. This log contains only metadata; no prompts, file contents, API keys, or raw provider error bodies are stored.
3. Inspect the last checkpoint, current Git status, diff, and files affected by that task within the project stage's permitted scope.
4. Launch the named fallback agent for that role exactly once, passing the original objective, input evidence, current task state, and acceptance criteria.
5. Ask it to resume from observed state—not repeat the original task blindly—and review its actual report.
6. If the fallback is unavailable or fails, mark the stage `BLOCKED` and preserve the checkpoint. Do not recurse through unlimited fallback models.

If the **primary coordinator** itself fails, a plugin cannot safely assume it can continue the user's whole task. OpenCode Desktop may surface the failure; switch the session to `project-lead-fallback` (when its free route is available) or start another session and ask it to continue from the canonical project handover. The first step is always to reload the checkpoint and inspect Git state.

## 3. Checkpoint requirements

Before significant work, record the base branch/commit, working-tree state, task scope, current phase, and the canonical plan/handover. After each completed stage, reconcile the **existing authoritative** progress/plan/handover documents with:
- stage status (`NOT_STARTED`, `IN_PROGRESS`, `PASS`, `FAIL`, `BLOCKED`);
- task/finding IDs and exact source evidence;
- changes made and whether they are committed;
- exact test commands and results, or exact CI URL/commit;
- outstanding decisions, blockers, and one next action.

Do not create a duplicate source of truth if the project already has a handover, phase ledger, or decision log. For a new project, use the templates in `new-project-template/`.

Use Git commits/checkpoints only under project policy. Never reset/clean a user's work. Never automatically commit, push, or merge solely to recover from an error. `git status` and `git diff` are the starting point for interrupted edits.

## 4. Integrity rules

- No automatic paid fallback or paid-credit consumption. Prefer the specifically labelled OpenCode Free endpoints and the owner-provided Groq free-key route for fallback. Before enabling any fallback assignment that might be billed, verify its current pricing/quota in the provider UI; if price/access is unknown, stop and ask. Model availability never implies free billing.
- One writer per shared checkout. Auditors/reviewers are read-only. Do not launch a second writer until the first writer is confirmed stopped.
- Checkpoint before fallback after an implementation failure.
- Keep the original acceptance criteria attached to a fallback task.
- Tests are run through the actual shell/CI, not inferred by a model. Record blocked checks honestly.
- Preserve each project's approvals and restrictions across failover. In particular, a fallback cannot open Dayseed's closed planning gate.
- No model agreement vote: evidence decides.

## 5. Context-window exhaustion

Allow OpenCode's context compaction to operate. Before a long task, make project instructions and checkpoint notes concise but complete. If compaction or the resumed request still fails, stop the oversized request and start a new session using the handover and task recovery packet. Never rely exclusively on the chat transcript for accepted decisions or unfinished work.
