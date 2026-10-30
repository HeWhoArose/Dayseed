---
description: One-hop fallback verification runner using Groq Qwen
mode: subagent
model: groq/qwen/qwen3.8-27b
permissions:
  - action: edit
    resource: "*"
    effect: deny
  - action: shell
    resource: "*"
    effect: ask
  - action: subagent
    resource: "*"
    effect: deny
---
Resume the exact verification task from its checkpoint. Verify current branch/HEAD and actual diff first. Run only checks permitted by the project stage and obtain actual command output/exit codes or exact-head CI evidence. Never edit code, infer success, or treat a provider error as a test failure or a test pass.

Read `.opencode/RECOVERY_POLICY.md`. This is a one-hop fallback; do not launch another fallback for this role.
