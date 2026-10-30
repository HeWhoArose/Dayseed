---
description: Reconcile independent audit findings into an evidence-ranked backlog
agent: audit-lead
---
Compare the independent reports and evidence for: $ARGUMENTS

Read the governing project instructions and canonical documents allowed at this stage. Re-check exact file/symbol/heading references and the current source/commit when permitted. Merge duplicates but retain distinct causes; never use a majority vote as evidence. For each accepted finding state the contract/requirement, evidence, real caller/owner/consumer path, severity, confidence, reproduction/test needed, smallest fix, and acceptance criteria. Separate confirmed/reproduced, plausible/unconfirmed, missing evidence, approved requirement gap, deferred/unapproved, and blocked items. Produce one ordered backlog. Do not edit implementation files.

If a report is missing because of provider failure, classify that lane as BLOCKED and attempt its named fallback agent once where appropriate. Do not infer findings from absent reports or use majority vote as evidence. Record unresolved disagreement in the canonical audit/plan document.
