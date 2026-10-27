# Journal Archive Research: Timeline vs Calendar vs Editorial

**Research date:** 2026-10-17  
**Status:** Recommendation for prototyping; not approved.  

**Current-status note (2026-10-27):** This document's unresolved questions were written before several later owner decisions. The archive layout remains a proposal, but entry/data semantics have since been settled: multiple entries per date (D-170/D-075), optional title (D-073), optional primary/secondary mood tags (D-079–D-086), text-only v1 with no photos (D-072), deliberate entry-date changes (D-169), confirmed permanent deletion with no recycle bin or post-delete undo (D-068–D-070), and offline search over saved entry title/body (D-074, with mood labels where present under D-081). Use this document to compare retrieval layouts, not to reopen those decisions. D-187 governs local-date behaviour.
  
**Purpose:** Help users revisit a personal record quickly without making journaling feel like administrative work.

## 1. User jobs

The archive should help a user:
- find an entry from a known date;
- browse recent reflections;
- notice a past note without being forced to analyse it;
- distinguish days with entries from days without entries;
- edit or delete an entry;
- search only if search is genuinely useful and private;
- understand that no entry does not mean a bad day or missed obligation.

The archive is a retrieval tool, not a performance report.

## 2. Patterns compared

| Pattern | Best at | Weaknesses | Accessibility / small-screen risks | Candidate use |
|---|---|---|---|---|
| **Timeline/list** | Scanning recent entries, reading snippets, chronological browsing | Long scroll; old dates can be tedious to reach | Dense metadata and repeated cards can overwhelm; needs clear headings | Default recent archive |
| **Calendar** | Jumping to a known date and seeing entry distribution across a month | Sparse calendars communicate little; month navigation can be slow | Small date cells and dots can be hard to tap/interpret; must not imply empty dates are failures | Secondary date-jump mode |
| **Editorial pages** | Reading one reflection at a time, highlighting selected memories or weekly summaries | Weak for precise retrieval; can feel curated rather than complete | Large typography can consume screen space and hide navigation | Detail/read mode, not the only archive |
| **Search** | Finding a phrase or keyword in older entries | Typing and query design add friction; private text search raises expectations | Keyboard and empty-result states need careful handling | Optional enhancement after archive basics |
| **Year/month heatmap** | Seeing broad activity distribution | Easily mistaken for mood or productivity score; poor for finding a specific entry | Colour dependence, dense cells, and ambiguous legends | Not the default archive; test only if a real user job supports it |

## 3. Proposed direction to prototype

Prototype a **hybrid archive**:
1. Default to a chronological list grouped by date (recent entries first), with a short preview and optional mood/activity metadata only if Dayseed has explicit product support for those fields.
2. Add a compact calendar/date-jump control as a secondary way to reach a known date.
3. Open an entry in a calm reading/editorial detail view with clear edit and delete actions.
4. Make empty dates neutral. A calendar may mark “entry exists” but must not colour an empty date as failure.
5. Keep journal records independent from habit completion and from the Growth Mark unless a separately approved metric explicitly connects them.
6. Include the approved v1 offline search for saved journal-entry title/body (D-074), displaying recorded primary/secondary mood labels where present (D-081) and excluding unsaved comeback drafts. Treat advanced date-range and habit filters as out of scope unless separately approved. Prototype comparison should focus on archive layout and retrieval flow; basic search is already an approved requirement.

This recommendation is a hypothesis based on task fit, not a measured usability result. Compare the hybrid against a plain timeline and a calendar-first variant with real tasks before locking it.

## 4. Prototype tasks

Use the same tasks across all variants:
- “Find the reflection you wrote on the 12th of last month.”
- “Open the most recent entry and return to the list.”
- “Correct a typo in an older entry.”
- “Delete one entry, and explain what happens.”
- “Tell me whether a blank date means you had a bad day.”
- “Find an entry after scrolling back several months.”
- “Use the archive with large text enabled.”

Measure completion, time-to-find, wrong taps, backtracking, confidence, and interpretation of empty dates. Do not rank layouts on aesthetics alone.

## 5. Entry data and behaviour questions

Current status of data/behaviour questions:
- Entry count per date: resolved — multiple entries per local date are allowed (D-170/D-075).
- Fields: text-only v1; title and primary/secondary mood are optional; photos/attachments are excluded (D-072–D-080).
- Date changes and timezone: a deliberate saved journal-date change affects only the selected entry (D-169); D-187 governs date keys and timezone travel.
- Search: offline title/body search is approved (D-074); mood labels may appear in results (D-081); unsaved comeback drafts are excluded. Advanced date-range/habit filters are not implied.
- Deletion: saved-entry deletion requires confirmation and is permanent; there is no recycle bin or post-deletion undo (D-068–D-070).
- Remaining design/release details: exact export serialization of dates/moods, long-text preview/read mode, empty states, date-jump navigation, and accessibility at large text sizes.
- Archive layout itself remains open: test a hybrid timeline/calendar against timeline-only or calendar-first patterns before choosing.

## 6. Privacy and emotional safety

- Journal text stays on-device under the proposed v1 privacy model.
- Never send journal text to analytics, ad networks, crash reports, or external AI services.
- Do not infer emotional state from words or display diagnostic language.
- Avoid pushy “on this day” prompts unless users opt in; memories can be sensitive.
- If an archive preview truncates text, make truncation obvious and preserve the full entry.
- Screen capture, OS backups, device transfer, export, and deletion policies must match the final privacy statement.

## 7. Exit criteria

Archive direction may be approved only after:
- at least two patterns are prototyped at comparable fidelity;
- known-date retrieval and recent browsing tasks are tested;
- large text and small-screen states are reviewed;
- edit/delete and empty states are specified;
- the selected pattern and evidence are recorded in docs/09-DECISION-LOG.md.

Until then, hybrid timeline + calendar is a proposal, not an approved layout.
