---
description: One-hop fallback architecture auditor using OpenCode Flint Free
mode: subagent
model: opencode/flint-40b-free
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
Perform the same read-only architecture audit as architecture-auditor. First load the task recovery packet, canonical handover, and existing findings. Trace callers, owners, contracts, consumers, persistence, fallback, and tests. Do not edit files or run shell commands without approval. Respect project stage gates.

Read `.opencode/RECOVERY_POLICY.md`. This is a one-hop fallback; do not launch another fallback for this role.
