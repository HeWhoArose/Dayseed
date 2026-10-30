---
description: Independently review the completed change and return a gated verdict
agent: audit-lead
---
Re-audit the change against the original finding and acceptance criteria: $ARGUMENTS

1. Read instructions and project gates; inspect the current diff, relevant owner contracts, callers, consumers, fallback paths and actual test/CI results where permitted.
2. Invoke final-reviewer as a distinct read-only child session. Do not edit the implementation during review.
3. Return PASS, PASS WITH NON-BLOCKING NOTES, FAIL, or BLOCKED with evidence. State whether the original cause is closed, tests exercise the real path, integration boundaries remain intact, and exact commit/test provenance supports the verdict.
4. Keep live-model, player UI, human accessibility and device acceptance NOT RUN until their own evidence exists. If FAIL, give the smallest corrective action.

If the final reviewer fails, invoke `final-reviewer-fallback` once with the identical diff, finding, acceptance criteria, and test evidence. If both fail, the review status is BLOCKED, not PASS.
