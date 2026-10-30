---
description: Save accepted decisions and progress into the existing canonical project documents
agent: project-lead
---
Checkpoint this conversation/project state: $ARGUMENTS

1. Separate owner-accepted decisions from brainstorms, recommendations, rejected ideas, deferrals and unresolved questions.
2. Read the existing decision log, active plan, handover and documentation index before writing. Use their current authority and status vocabulary; do not create a competing decision log or project-state file where one exists.
3. Record accepted material decisions and affected requirement/traceability references, if permitted. Brainstorm ideas remain PROPOSED unless the owner clearly accepted them.
4. Record the current branch/commit, completed work, actual tests/results, CI evidence, blocked items, and one next action. Conversation text alone is not implementation or test evidence.
5. Respect stage gates. Dayseed is planning/documentation only until owner approval plus separate technical-validation authorisation. If no safe write is permitted, return the proposed checkpoint without modifying files.

Also record the recovery point needed by `.opencode/RECOVERY_POLICY.md`: current branch/HEAD and dirty files, active task and stage, completed verified work, partial/uncommitted changes, exact checks and evidence, blockers, and one next action. Update the project's existing authoritative handover/progress ledger; don't create a duplicate status source.
