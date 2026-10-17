# Dayseed Agent Guardrails

This file is retained only as project guidance for any future agent that may be used. It does not authorise use of Replit, and it does not mean this project should be built in Replit.

## Current instruction
The user explicitly requested that the planning package be stored in Git and said not to use Replit for this work because its AI quota was exhausted and an earlier attempt produced a website rather than an app. Use the Git repository as the source of truth. Do not open or run a Replit project unless the user explicitly changes this instruction.

## Before doing any work
1. Read README.md.
2. Read docs/09-DECISION-LOG.md, docs/11-RISKS-AND-OPEN-QUESTIONS.md, and docs/12-PROGRESS-AND-HANDOVER.md.
3. Read all relevant product, UX, architecture, privacy, and quality documents.
4. Identify whether the requested work is documentation, research, design, or implementation.
5. If implementation has not been explicitly approved through the planning gate, do not write application code.

## Guardrails
- Dayseed is an Android-first app, not a website.
- Preserve offline-first behaviour and local privacy as core requirements.
- Never send journal text or habit records to analytics or advertising services.
- Never invent sources, compatibility, tests, or build results.
- Do not turn proposed stack or design choices into approved decisions without confirmation.
- Any implementation phase must include prerequisites, deliverables, acceptance criteria, tests, risks, and an exit gate.
- For defects: reproduce, identify root cause, implement a targeted fix, run targeted tests, run regression tests, run the full suite, then re-audit.
- Update the decision log, roadmap, and handover whenever material decisions or status change.
- Keep v1 non-goals out of scope unless the product owner approves a change.

## Planning gate
Do not start implementation until product scope, daily record semantics, UX flows, privacy/data handling, technical feasibility, and the test plan have been reviewed and explicitly approved.