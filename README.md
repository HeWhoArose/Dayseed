# Dayseed

**Your progress is more than your streak.**

Dayseed is an Android-native, offline-first habit and micro-journal application built with Kotlin and Jetpack Compose. It is designed for people who want to build consistency without being shamed when life interrupts them.

## Core Features Implemented

- **Progress Over Perfection:** Support for flexible effort levels (**Tiny**, **Usual**, and **Extra**), plus completion-only habits. A tiny action (e.g., rolling shoulders, reading 1 paragraph) is fully celebrated as showing up.
- **Humane Comeback Loops:** Gentle, dismissible re-entry experiences after time away (e.g. 3, 14, 30+ days) with zero guilt copy, no streak resets, and preserved history.
- **Collected Moments (Growth Mark):** Visualizes genuine user records without arbitrary XP, decay, points, or streaks. Shows active days tended and effort mix (Tiny 🌱, Usual 🌿, Extra 🌳).
- **Daily Micro-Journal:** Low-friction, serene daily reflections with suggested prompts ("What went better than expected today?", "A small moment of calm"), word counter, and full archive search.
- **Historical Editing:** Day-by-day past logs with inline correction. Adjusting past records recalculates metrics in real time without penalty.
- **Private by Default:** 100% on-device SQLite storage. No ads, tracking, or unexpected cloud calls.
- **Backup & Portability:** Complete JSON export and sharing with full data deletion option in Settings.
- **Modern Material 3 Design:** Built strictly according to `DESIGN.md` with warm paper palette, adaptive launcher icon, dynamic dark/light theme support, and accessible 48dp touch targets.

## Technology Stack

- **Platform:** Native Android (minSdk 26, targetSdk 36)
- **Language:** Kotlin 2.2.10
- **UI Toolkit:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Kotlin Coroutines and `StateFlow`
- **Local Persistence:** Android SQLite OpenHelper with full relational schema
- **Build System:** Gradle 9.3.1 with Android Gradle Plugin 9.1.1
