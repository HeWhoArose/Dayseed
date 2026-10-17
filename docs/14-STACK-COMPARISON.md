# Stack Comparison: React Native + Expo vs Native Kotlin + Jetpack Compose

**Research date:** 2026-10-17  
**Status:** Evidence-based comparison and proposed recommendation. No app has been created or built.  
**Decision:** D-009 remains **Open** pending owner approval and the planning gate.

## Executive recommendation

**Preferred candidate for further validation: React Native + Expo + TypeScript + expo-sqlite.** This is a narrow, conditional preference—not a final architecture decision.

Why it currently edges ahead for Dayseed:
- V1 is mainly local forms, lists, history, small summaries, and reflection—not a graphics-heavy or platform-specialised app.
- The user wants Android first but has left iOS as a plausible later target. React Native can share substantial UI and domain logic across Android and iOS.
- Expo has a documented development-build and APK/AAB route, including optional hosted builds, so the team need not depend on a local emulator or Replit to produce every test artifact.
- Expo SQLite documents persistence across app restarts, SQL access, transactions, and an initialization/migration pattern.

**Native Kotlin + Jetpack Compose remains a strong alternative.** It provides the most direct Android API access and pairs naturally with Room, which Android recommends for structured SQLite persistence and which supports compile-time SQL validation and managed migrations. Prefer native Kotlin if tested reminder requirements, platform integration, dependency restrictions, or reliable Android-only delivery outweigh future iOS code reuse.

No source can settle user-specific build friction or prove either app architecture works for Dayseed; only the bounded technical spike can do that.

## 1. Dayseed criteria

Weights reflect project priorities, not industry measurements:
- Android-first v1 fit: 20
- Offline structured data and migrations: 20
- Notification and Android platform control: 15
- Git-based build workflow and low local-tooling burden: 15
- Potential iOS reuse: 10
- Accessibility and testing: 10
- Long-term maintenance/dependency management: 10

Scores run from 1 (poor fit) to 5 (strong fit). These are explicit planning judgements based on the current requirements and official documentation, not benchmark results.

| Criterion | Weight | React Native + Expo | Kotlin + Compose | Rationale |
|---|---:|---:|---:|---|
| Android v1 feature fit | 20 | 4.5 | 4.0 | Both can deliver habit CRUD, journal, summaries, settings, and offline use; Expo's workflow is lighter for an app dominated by standard app UI. |
| Offline SQLite and migrations | 20 | 4.0 | 5.0 | Expo SQLite offers persisted SQLite and explicit SQL/migration patterns; Room adds compile-time query checking and first-party migration/testing support. |
| Android notifications/platform control | 15 | 3.5 | 5.0 | Expo wraps native notification APIs and exposes permission behaviour; Kotlin allows direct control of Android notification channels, alarms, and platform APIs. |
| Git-based build and local-tooling burden | 15 | 4.5 | 3.0 | Expo offers optional hosted EAS builds and APK profiles; native builds can also use CI but require more Android-specific build configuration and maintenance. |
| Potential iOS reuse | 10 | 5.0 | 1.5 | React Native shares much of the UI and TypeScript domain code; a Kotlin/Compose app would generally require a separate iOS client. |
| Accessibility and UI testing | 10 | 4.0 | 4.5 | Both support accessible native experiences. Compose has a well-integrated semantics/testing system; React Native still requires TalkBack and device testing. |
| Long-term maintenance | 10 | 3.5 | 4.5 | Expo SDK/package alignment and native-module constraints add a framework dependency; native Android has more direct control but may require more platform-specific upkeep and a separate iOS implementation. |
| **Weighted total** | **100** | **4.15 / 5** | **4.05 / 5** | Close result; prioritising Android-only platform control would change the outcome. |

The 0.10 point difference is not evidence of a universal winner. Re-score if the owner confirms Dayseed will permanently be Android-only, needs exact alarms as a core promise, or strongly prioritises native integration over a potential iOS release.

## 2. Capability-by-capability comparison

### UI and product iteration
**Expo / React Native:** TypeScript-based components and a common UI/domain layer can support Android now and potential iOS later. React Native officially recommends starting with a framework such as Expo for most new apps, while still allowing framework-less projects when unusual constraints require it.

**Kotlin / Compose:** Android's recommended modern toolkit with direct access to Android APIs and Android Studio tooling. This is the most direct way to build a platform-native Android experience and to use platform-specific components with minimal bridging.

**Dayseed implication:** Both are suitable. The current feature list does not justify choosing native solely for UI performance; actual performance should be measured only if a prototype reveals a problem. No web deliverable is part of the product requirement. Any Expo project must be configured and reviewed as an installable Android app; a website is not an acceptable substitute.

### Local persistence and data integrity
**Expo candidate:** expo-sqlite exposes SQLite and documents that the database persists across app restarts. Its docs show parameter binding, transactions, PRAGMA user_version-based migration examples, and a provider initialisation hook. The app still needs to design migrations, uniqueness rules, transaction boundaries, export/deletion, backup behaviour, and recovery tests correctly.

**Native candidate:** Android recommends Room rather than directly using SQLite APIs for structured local data. Room adds compile-time query validation and migration facilities. Android's guidance warns that a missing migration path can cause a failure, and destructive fallback deletes user data; Dayseed should not use destructive migration as a casual recovery strategy.

**Dayseed implication:** Room has a meaningful technical advantage in schema assurance. Expo SQLite remains viable if the repository/data-access layer is explicit and migration tests are mandatory. An ORM is not automatically needed; avoid adding another layer until there is a concrete benefit.

### Reminders and background behaviour
**Expo candidate:** expo-notifications can schedule local notifications. Android still controls delivery. On Android 13+, notification permission is runtime-controlled. Exact-time scheduling involves additional alarm permission/policy considerations on modern Android; Android 14+ denies SCHEDULE_EXACT_ALARM by default for newly installed apps unless allowed. A scheduled request does not guarantee exact display time.

**Native candidate:** Kotlin can use Android notification channels and the appropriate platform API directly. Android recommends WorkManager for persistent, deferrable work—not as a promise of precise clock-time alarms. Exact reminders require the appropriate alarm strategy and permission analysis.

**Dayseed implication:** Do not promise minute-perfect reminders in v1. Default to flexible local reminders, explain OS limitations, respect denied permissions, and keep the app fully usable. If the product owner later requires highly precise alarms, evaluate the exact use case and policy before locking either stack. Test on real devices and multiple Android versions.

### Builds and Git-based workflow
**Expo route:** GitHub remains the source of truth. The project can use local Expo/Android tooling or optional EAS hosted builds. Configure an APK profile for direct device testing. Use an AAB and protected signing/release credentials for Google Play distribution. Expo Go is not sufficient as the only validation environment for native dependencies/configuration; use a development build when required.

**Native Kotlin route:** Keep Gradle/Kotlin source and build scripts in GitHub. Build a debug APK with Gradle/Android Studio for direct testing, run unit and instrumented tests, and create a signed AAB for Play. CI can run builds, but Android SDK/JDK/Gradle setup and cache/version management must be maintained.

**Dayseed implication:** Neither route requires Replit. Expo has the simpler first build path when hosted builds are acceptable; native Kotlin gives direct control but asks for more Android-specific setup. Before choosing, confirm the intended Git-based automation, where builds run, how much cloud dependence is acceptable, and who holds release signing credentials.

### Accessibility and testing
Compose provides a semantics tree used by accessibility services and UI tests. React Native also exposes accessibility APIs, but both stacks need TalkBack testing on real devices, large-font review, keyboard/focus review where relevant, and tests of the actual app build. A framework does not make an app accessible automatically.

### Privacy and security
Neither framework automatically makes the product private. Personal data must be minimised and remain local by design. SQLite/Room does not automatically mean the database is encrypted. Review Android backup/transfer configuration, app-private storage, export/delete semantics, diagnostic logs, and all third-party SDKs. Do not add analytics, ads, billing, cloud sync, or AI APIs to the initial technical spike.

## 3. Provisional package/platform plan

Exact versions are deliberately **not locked** until the stack decision and implementation gate. “Latest” pages can change; choose a compatible SDK family first, then resolve and lock the package set together.

| Concern | Expo candidate | Native Kotlin candidate | Decision status |
|---|---|---|---|
| Language/UI | TypeScript + React Native | Kotlin + Jetpack Compose | Expo preferred candidate; not approved |
| Framework/build | Expo SDK + EAS optional, or local Android tooling | Android Gradle Plugin + Gradle + Android SDK | Route documented; actual build untested |
| Local database | expo-sqlite | Room (use current stable official version and KSP/compiler instructions) | Candidates documented |
| Schema migrations | Explicit migration service/table versioning using SQLite | Room schema export and explicit migration paths; test migrations | Must be designed and tested |
| Reminders | expo-notifications for local schedules | NotificationManager plus suitable AlarmManager/WorkManager use by requirement | Keep times flexible until requirements settle |
| State/architecture | Small application/use-case layer and repositories | ViewModels, repositories, UI state, coroutines/flows | Apply same separation principles in either stack |
| Navigation | Decide after UX flows; avoid unnecessary router commitment | Navigation Compose after UX flows | Open until information architecture approved |
| Tests | Domain/unit, SQLite integration, React Native UI, real device | Unit, Room migration/instrumented, Compose UI, real device | Required in both |
| Analytics/ads/billing | None in initial feasibility spike | None in initial feasibility spike | Deferred pending product/privacy decision |

For an approved Expo project, use Expo's SDK-aware installer and compatibility checker instead of combining independently copied “latest” versions:
- npx expo install [package]
- npx expo install --fix when intentionally aligning dependencies to a chosen SDK
- npx expo-doctor

These checks are advisory tooling, not proof that the app behaves correctly. Keep the package lockfile in Git and run the build/test suite against that locked dependency set.

## 4. Recommended architecture shape if Expo wins
- **App shell:** native Android app built with Expo/React Native; target Android first. Do not produce or ship a website as the deliverable.
- **Domain layer:** pure TypeScript rules for schedules, daily outcome semantics, comeback tiers, date-key handling, and local summary calculations. Avoid React hooks and platform APIs in this layer.
- **Application layer:** explicit use cases for saving a habit, recording a completion, saving a journal entry, editing history, and deleting/exporting personal data.
- **Data layer:** repositories over SQLite; parameterised/prepared queries; unique constraints for approved per-day rules; explicit migration history; transaction policy for related writes.
- **Platform adapters:** notification scheduling, file/share/export, app lifecycle, and any platform-specific behaviour.
- **Testing:** deterministic clock/date abstraction, seeded migration fixtures, offline device test plan, and privacy tests.

This is a target architecture proposal, not a source-code implementation.

## 5. Bounded technical spike — required before final stack approval
Once the product/design planning gate is approved, build only a feasibility spike. The spike must not evolve into app screens or feature implementation.

### Required proof
1. Project scaffold is native-app only for this stage and no website is created or deployed.
2. The chosen SDK/toolchain and dependency versions are recorded and locked.
3. Clean dependency install succeeds from the lockfile.
4. A debug/development APK compiles and installs on a physical Android phone.
5. A seeded SQLite database survives force-stop/restart.
6. A schema migration updates a seeded v1 database to v2 without deleting existing records.
7. A local reminder can be scheduled, inspected, cancelled, and tested after restart; permission denial and device restart are covered, without promising exact timing unless explicitly tested and supported.
8. All core feasibility actions work with airplane mode enabled.
9. Export and deletion are exercised with synthetic data only.
10. Dependency check, static checks, and all spike tests pass; commands, logs, device model/Android version, and limitations are recorded. Never commit secrets or private journal content.

### Exit criteria
- All required proof items pass or a blocker is documented with a clear decision.
- The data-loss/privacy risks are acceptable.
- The product owner explicitly approves the stack decision after reviewing evidence.
- Only then can a separate implementation plan authorise building the actual Dayseed features.

## 6. Questions that could change the recommendation
1. Is iOS a real medium-term goal, or is Dayseed Android-only indefinitely?
2. Are reminders allowed to be approximate under Android battery/permission rules?
3. Can a hosted build provider such as EAS be used, or must the build be entirely reproducible through local tools/GitHub Actions?
4. Is TypeScript or Kotlin more maintainable for the expected contributors?
5. What minimum Android version and test-device matrix are required?
6. Does the privacy threat model require database encryption at rest, and what recovery/export trade-offs are acceptable?

Unless the answers materially change, continue planning with Expo as the preferred candidate and native Kotlin as the fallback. Do not treat this as final approval.

## 7. Sources consulted
All sources below are official vendor/platform documentation and were checked on **2026-10-17**. They establish documented capabilities and constraints, not independent performance benchmarks.

### React Native and Expo
- React Native — Get Started: https://reactnative.dev/docs/environment-setup
- React Native — Platform-specific code: https://reactnative.dev/docs/platform-specific-code.html
- Expo — Development builds: https://docs.expo.dev/develop/development-builds/introduction/
- Expo — Android development build: https://docs.expo.dev/tutorial/eas/android-development-build/
- Expo — Android APK build: https://docs.expo.dev/build-reference/apk/
- Expo — SDK reference and platform support: https://docs.expo.dev/versions/latest/
- Expo — SQLite: https://docs.expo.dev/versions/latest/sdk/sqlite/
- Expo — Notifications: https://docs.expo.dev/versions/latest/sdk/notifications/
- Expo — Development tools / Expo Doctor: https://docs.expo.dev/develop/tools/
- Expo — Upgrade SDK / dependency alignment: https://docs.expo.dev/workflow/upgrading-expo-sdk-walkthrough/

### Native Android
- Android — Jetpack Compose UI toolkit: https://developer.android.com/develop/ui
- Android — Architecture recommendations: https://developer.android.com/topic/architecture/recommendations
- Android — Room local database: https://developer.android.com/training/data-storage/room
- Android — Room migrations: https://developer.android.com/training/data-storage/room/migrating-db-versions.html
- Android — Compose accessibility: https://developer.android.com/develop/ui/compose/accessibility/semantics
- Android — Compose UI testing: https://developer.android.com/develop/ui/compose/testing
- Android — Task scheduling / WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Android — Build command line / APKs: https://developer.android.com/build/building-cmdline
- Android — Android Studio installation and hardware requirements: https://developer.android.com/studio/install

## 8. Evidence and limitations
This comparison is based on official documentation and project requirements. The scores are subjective planning estimates. No code, build, package compatibility run, device test, battery test, migration test, notification timing test, accessibility audit, or app-size benchmark has been run for Dayseed. The recommendation must be revisited if the technical spike fails or new requirements appear.

## 9. Follow-up verification: versioning and dependency discipline

A follow-up check of the current official documentation reinforces two important implementation cautions:

- Expo's versioned SQLite documentation provides a version-specific recommended package range and directs installation through `npx expo install expo-sqlite`. The package must be selected against the chosen Expo SDK; this comparison intentionally does not pin a package number before the SDK decision.
- Android's current Room documentation now describes Room 3.0 under the `androidx.room3` group, Kotlin/KSP code generation, and coroutine-first APIs. It is a major-version boundary from Room 2.x, not a version to copy blindly from older tutorials. If native Kotlin is chosen, compare the currently supported Room 2.x and 3.x migration paths and select one deliberately.

This matters because package compatibility is a set of aligned versions, not a list of individually “latest” libraries. Before the spike, record the selected SDK, language/toolchain, package coordinates, plugin versions, and official compatibility references in one matrix; then install, lock, compile, and test the exact set.

Additional official sources checked:
- Expo SQLite, versioned SDK documentation: https://docs.expo.dev/versions/latest/sdk/sqlite/
- Android Room overview and current dependency coordinates: https://developer.android.com/training/data-storage/room
- Android Room 2.x-to-3.0 migration guidance: https://developer.android.com/training/data-storage/room/migration-2-to-3
- React Native framework guidance: https://reactnative.dev/blog/2024/06/25/use-a-framework-to-build-react-native-apps

These checks do not change the conditional recommendation or close D-009. No package installation, build, or device test was run.
