# UX and Design

**Status:** Exploration brief; visual direction not approved.

## Experience goals
The app should feel encouraging without being childish, calm without being bland, and structured without becoming a productivity dashboard. Make the next useful action obvious and keep logging lightweight.

## Candidate visual directions
These are hypotheses to compare, not final brand decisions.

### A. Field Journal
Warm paper-like surfaces, restrained organic motifs, tactile details, and reflective writing emphasis. Risk: decorative styling or low contrast.

### B. Living Editorial
Confident typography, strong hierarchy, expressive headings, clear cards, and a growth motif used sparingly. Risk: editorial styling competes with daily tasks.

### C. Quiet Instrument
Minimal controls, precise data visualisation, restrained colour, and predictable navigation. Risk: feeling clinical or generic.

**Proposed synthesis:** a warm, distinctive, growth-oriented interface with editorial typography and clear utility. Validate on small Android screens and accessibility settings before approval.

## Information architecture proposal
- **Today:** today's habits, fast completion, daily reflection entry point.
- **History:** calendar/list view, past outcomes, corrections.
- **Insights:** understandable weekly/monthly summaries.
- **Settings:** reminders, appearance/accessibility, privacy, export/delete, support.

Usability testing should determine whether journal access belongs on Today, in a dedicated tab, or both.

## Primary flows

### First launch
Explain the product promise → explain local storage plainly → create first habit or explore a sample → choose optional schedule/reminder → arrive at Today. Do not require account creation.

### Daily logging
Open Today → choose a habit → tap Tiny/Usual/Extra or Done → receive brief acknowledgement → return to Today. Minimise modal dialogs and unnecessary confirmation after routine actions.

### Reflection
Open reflection → optional prompt → write or skip → save locally → return to Today. Do not require a minimum word count or streak.

### Comeback
Open app after inactivity → supportive, short message → offer “Choose one small step”, “Review my habits”, or “Just look around” → resume normal use. Dismissal must be respected.

### Editing history
Open History → select date/habit → edit outcome → explain that summaries update → save. Corrections should not feel like cheating.

### Privacy
Settings → Privacy and data → explain local storage and OS backup implications → export or delete → clear confirmation and completion state.

## Content and voice
- Warm, direct, adult, and nonjudgmental.
- No guilt, scolding, fake intimacy, or claims about the user's feelings.
- Never say a user “failed” because they missed a day.
- Avoid over-celebrating routine actions.
- Explain consequences before destructive actions.
- Do not imply that journaling reveals a clinical condition.

## Accessibility and small-screen checklist
- Test large system font sizes and display scaling.
- Meet current Android touch-target guidance.
- Ensure sufficient contrast in every theme.
- Colour must not be the only way to distinguish outcomes.
- Provide TalkBack labels, logical focus order, and meaningful control names.
- Support reduced motion; no essential information only in animation.
- Keep important actions reachable with one hand where practical.
- Check keyboard navigation, text entry, empty states, and error messages.

## Design deliverables before implementation
Approved information architecture and navigation rationale; wireframes for primary flows; design tokens and component inventory; theme and contrast checks; content inventory including comeback variants; reviewed prototype; usability test plan and findings; completed design review checklist.

Do not begin app UI implementation until the planning gate is approved.