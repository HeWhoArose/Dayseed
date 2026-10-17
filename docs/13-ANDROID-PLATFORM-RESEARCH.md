# Android Platform and Build-Route Research

**Research date:** 2026-10-17  
**Status:** Official-documentation review; architecture remains a proposal. No app has been created or built as part of this research.

## Executive conclusion

Official Expo documentation supports a viable Android-first React Native/Expo route: use a development build for native-module testing, create an installable APK for direct device testing, and produce an AAB for Google Play distribution. EAS Build can run hosted builds from a Git-based workflow and does not require Replit. Local Android builds are another option but require the native development environment.

This verifies that the route exists; it does **not** prove Dayseed's eventual dependency set, minimum Android version, or release configuration has been tested. React Native + Expo + SQLite remains a candidate until the package compatibility gate is passed.

## 1. Candidate Android build route

1. Keep source, documentation, configuration, and lockfile in GitHub.
2. After the planning gate is approved, create the actual app project from a reviewed stack decision.
3. Use an Expo development build rather than relying only on Expo Go when native dependencies or native configuration are needed.
4. For local device testing, produce an APK. Expo documents that the default Android output is AAB, intended for Play distribution and not directly installable like an APK.
5. Test the APK on a real Android device and an emulator if available.
6. For Play distribution, configure app identity, signing, versioning, release checks, store listing, and privacy disclosures; produce an AAB and submit through the chosen release workflow.
7. Keep build profiles and commands documented and reproducible. Never store private signing keys or secrets in the repository.

Official references:
- Expo EAS Build: https://docs.expo.dev/build/introduction/
- Expo Android development build: https://docs.expo.dev/tutorial/eas/android-development-build/
- Expo APK build configuration: https://docs.expo.dev/build-reference/apk/
- Expo development builds: https://docs.expo.dev/develop/development-builds/introduction/
- Android app signing: https://developer.android.com/studio/publish/app-signing

## 2. Findings by capability

### A. SQLite / offline persistence
The official Expo SQLite documentation describes a SQLite API with database persistence across app restarts and documents transactions and database initialization patterns. This makes it a credible candidate for local-first storage.

Still to verify in a technical spike after approval:
- Current Expo SDK's recommended SQLite version and platform requirements.
- Migration approach and idempotent migration tests.
- Transaction behaviour under concurrent reads/writes.
- Backup and restore behaviour on supported Android versions.
- Export/delete semantics and database file location.
- Handling of locked, corrupt, full, or unavailable storage.
- Whether encryption is required by the agreed threat model.

Reference: https://docs.expo.dev/versions/latest/sdk/sqlite/

### B. Local notifications
The official Expo Notifications docs support scheduled notifications. They also document Android constraints: Android 13+ notification permission is opt-in; exact-time alarms have additional permission and policy considerations on modern Android versions; notification channels are part of Android notification setup. Exact alarms should not be requested unless product requirements truly need them.

Dayseed should prefer flexible local reminders where possible, ask permission in context, and continue working when permission is denied. Scheduling a notification does not guarantee the OS will display it at the exact expected moment.

Reference: https://docs.expo.dev/versions/latest/sdk/notifications/

### C. Development builds and native dependencies
Expo describes development builds as a custom version of Expo Go that can include native libraries and native configuration. Expo documents both local Android builds using Android tooling and hosted builds using EAS. Native dependency or app-configuration changes may require regenerating native projects and rebuilding.

Package selection must happen before implementation and should be checked against the chosen Expo SDK. Do not assume every React Native library works in Expo Go or in a managed configuration without additional native setup.

Reference: https://docs.expo.dev/develop/development-builds/introduction/

### D. APK versus AAB and signing
Expo documents APK output for direct device/emulator installation and AAB as the default format suited to Google Play. Android requires installed APKs to be signed; Play App Signing separates the app-signing key from the upload key in the Play workflow.

Planning implication: define two distinct deliverables: an APK for hands-on testing and an AAB for store release. Decide key ownership and backup procedures before the first production release; never commit keystores, passwords, access tokens, or service credentials.

References:
- https://docs.expo.dev/build-reference/apk/
- https://developer.android.com/studio/publish/app-signing

### E. Monetisation packages
Google's official Mobile Ads documentation lists SDK prerequisites, including Android SDK requirements and AdMob app registration. Google's Play Billing docs describe product setup, purchase flows, purchase acknowledgement, and server verification considerations.

These are reasons to postpone package selection until the monetisation decision is approved. An ad or billing library can add native configuration, consent obligations, SDK updates, policy checks, and potentially backend work. Do not add either SDK just because a monetisation hypothesis exists. V1 can ship without monetisation if that is the approved choice.

References:
- https://developers.google.com/admob/android/quick-start
- https://developer.android.com/google/play/billing/integrate
- https://developer.android.com/google/play/billing/getting-ready

## 3. Package selection gate

Before choosing a package, record:
1. Exact package name and official repository/documentation URL.
2. Current release/version and date checked.
3. Compatibility with the selected Expo SDK, React Native version, and Android minimum/target SDK.
4. Whether it requires custom native code, config plugins, prebuild changes, or a development build.
5. Maintainer activity, issue history, release cadence, and licence.
6. Permissions and manifest changes.
7. Offline behaviour, data collection, telemetry, and privacy implications.
8. Transitive dependencies and known conflicts.
9. Test coverage and an isolated proof of compatibility.
10. Replacement/migration risk and whether the dependency is truly necessary.

Use Expo's documented installation guidance for compatible packages, then validate the exact resolved versions and run dependency compatibility checks after the project is approved. Do not select versions from stale blog posts.

## 4. Required compatibility matrix

| Capability | Candidate | Evidence status | Required verification |
|---|---|---|---|
| App framework | React Native + Expo | Supported route documented | Decide against native Kotlin; verify current SDK/toolchain |
| Local database | expo-sqlite | Official Expo API documented | Migrations, transactions, restart, storage failure behaviour |
| Local reminders | expo-notifications | Official Expo API documented | Permission denial, reboot, OS restrictions, schedule/cancel, timezone changes |
| Native development | Expo development build | Official route documented | Build profile and native module integration |
| Direct device test | APK | Official route documented | Install/update on target devices |
| Google Play release | AAB + Play signing | Official route documented | App identity, signing custody, Play policy and release track |
| Ads | AdMob SDK or alternative | Platform docs exist; product choice open | Privacy, consent, native compatibility, policy, offline fallback |
| One-time purchase | Play Billing via compatible wrapper/native bridge | Google billing flow documented | Wrapper compatibility, purchase lifecycle, restore/refund cases, policy; backend needs |
| Analytics/crash reports | None selected | Not approved | Prefer no third-party analytics initially; privacy review if proposed |

## 5. Technical spike acceptance criteria

After explicit approval of the planning gate, run a small isolated spike before committing to the final architecture. It must demonstrate:
- A development build compiles for Android.
- An APK installs and launches on a real device.
- SQLite data survives force-stop and restart.
- A schema migration upgrades a seeded database without losing records.
- A scheduled local notification can be created and cancelled; permission denial is handled.
- Core actions work with airplane mode enabled.
- Export and deletion behave according to the approved policy.
- The dependency tree and native permissions are recorded.
- Build commands, tool versions, device/OS details, and logs are captured without secrets or private journal content.

The spike is a bounded feasibility experiment, not the start of the product build. Do not expand it into app screens or features.

## 6. Decision outcome

**Verified:** Expo documents a real Android route using development builds, APKs for direct testing, and AABs for Play distribution. Expo documents SQLite persistence and local notification APIs, with platform constraints.

**Not verified:** Dayseed itself builds; a particular package combination compiles; notification timing is exact across devices; SDK versions are final; monetisation SDKs are suitable; or the Play release is ready.

**Recommendation:** retain React Native + Expo + TypeScript + SQLite as a **proposed candidate**, not an approved decision. Complete product/design planning, approve the technical spike, then validate exact package versions and native configuration before implementation.