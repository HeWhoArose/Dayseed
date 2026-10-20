# Comeback Experience

**Status:** Approved direction for inactivity-based in-app welcome-back experiences; exact eligibility and copy remain to be specified.

## Two-week comeback

- Define the 14-day inactivity trigger as no app opening for 14 consecutive days. Any opening of Dayseed resets the timer, including opening only to read the journal, browse history, or check insights. After the 14-day threshold is reached, show the gentle comeback message only when the user next opens Dayseed.
- Do not send a separate notification while the user is away for this two-week comeback trigger.
- Keep the message warm and nonjudgmental. Do not assume why the user was away, imply failure, or require an explanation.
- The message should invite the user to resume an existing goal when useful, without forcing a reset or automatically changing goal records.
- Distinguish app inactivity from lack of recorded habit progress: being away from Dayseed does not prove that a habit was abandoned or that no progress occurred.
- Prevent the same comeback experience from repeatedly appearing after it has been acknowledged or dismissed for the relevant inactivity period.

Example: “We missed you! 🌱 How have things been? Whenever you're ready, we can pick up where you left off.”

## Longer inactivity / no active goals

A separate, broader welcome-back experience is proposed for a user who has not opened the app for about one month, or who returns after having no active goal for about one month. It should invite the user to reflect on what is new and optionally resume an existing habit or create a new one. This is a distinct experience and its eligibility, timing, priority relative to the two-week comeback, and exact copy still need a separate decision.

Example direction: “Welcome back! 🌿 What's new with you? Ready to pick up an old habit or plant a new one?”

## Still to specify

- Exact boundary calculation for 14 consecutive days, including clock/time-zone changes and first-launch tracking.
- How to handle active daily and weekly goals without confusing app inactivity with missed habit days.
- The one-month return and no-active-goal conditions, precedence when both triggers qualify, and repeat suppression.
- Accessibility, localization, privacy-conscious message copy, and acceptance tests.
