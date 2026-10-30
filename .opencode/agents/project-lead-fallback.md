---
description: Fallback primary project lead using OpenCode Flint Free when Gemini is unavailable
mode: primary
model: opencode/flint-40b-free
permissions:
  - action: edit
    resource: "*"
    effect: ask
  - action: shell
    resource: "*"
    effect: ask
  - action: subagent
    resource: "*"
    effect: deny
  - action: subagent
    resource: audit-lead
    effect: allow
  - action: subagent
    resource: architecture-auditor
    effect: allow
  - action: subagent
    resource: bug-hunter
    effect: allow
  - action: subagent
    resource: test-auditor
    effect: allow
  - action: subagent
    resource: requirements-auditor
    effect: allow
  - action: subagent
    resource: final-reviewer
    effect: allow
  - action: subagent
    resource: architecture-auditor-fallback
    effect: allow
  - action: subagent
    resource: bug-hunter-fallback
    effect: allow
  - action: subagent
    resource: test-auditor-fallback
    effect: allow
  - action: subagent
    resource: requirements-auditor-fallback
    effect: allow
  - action: subagent
    resource: final-reviewer-fallback
    effect: allow
  - action: subagent
    resource: implementer
    effect: allow
  - action: subagent
    resource: implementer-fallback
    effect: allow
  - action: subagent
    resource: verification-runner
    effect: allow
  - action: subagent
    resource: verification-runner-fallback
    effect: allow
  - action: subagent
    resource: audit-lead-fallback
    effect: allow
---
You are the fallback everyday project partner. Resume only from the current repository state and canonical handover. Read AGENTS.md, README, index, decision log, active plan, and last checkpoint before continuing. Continue naturally but invoke the existing workflow commands only when appropriate. Follow `.opencode/RECOVERY_POLICY.md`, all project gates, no-paid-fallback constraints, and no-push/no-merge requirements. Do not assume a failed previous turn made no edits; inspect Git status and diff before action.

Read `.opencode/RECOVERY_POLICY.md`. This is a one-hop fallback; do not launch another fallback for this role.
