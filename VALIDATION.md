# Validation — 14 October 2026

Tested using an Android 15 / API 35 Pixel 7 emulator at 1080 × 2400, with Android SDK 36 for compilation.

## Automated checks

- 10 JVM rules tests passed: overnight boundaries, cross-week schedule conflicts, next-start calculation, Pomodoro breaks and focus totals, restriction/allowlist precedence, domain normalization, daily limits, override expiry and cooldown, gradual reduction, profile app scope.
- 4 connected Android tests passed: dock and header navigation, website entry and persistence, explicit timer-only warning and session startup, JSON round-trip for rules and schedules.
- Debug APK assembly passed.
- Focus v1.0.0 release assembly passed with R8 optimization and resource shrinking. The APK is 2,552,183 bytes (about 2.6 MB).
- Release APK metadata verified: display name Focus, version 1.0.0, version code 100, minimum API 28, target API 36.
- APK Signature Scheme v2 verification passed with the dedicated 4096-bit RSA release certificate. No network permission is declared.
- Android lint passed with no errors. Warnings remain for pinned dependency versions, English string resources, deliberate application-context storage, and platform attributes on older Android versions.

## Emulator checks

- Completed onboarding and the prominent accessibility disclosure.
- Enabled accessibility protection, Usage Access, and notifications.
- Selected a separately installed test app and started a real focus session.
- Confirmed the accessibility blocking screen appeared over that app.
- Completed the standard-mode waiting period, entered a reason, and confirmed the five-minute intentional visit released the test app.
- Opened Focus from the blocking state and confirmed its active session remained accessible.
- Opened Android system settings while the session was active; access remained available.
- Viewed light and dark themes and text scaling at 150%.
- Captured the app's actual interface. Screenshots use a sample app called Scroll Test. No fake usage history is seeded.
- Installed the optimized, signed v1.0.0 APK and repeated onboarding, permission consent, app selection, focus-session startup, app blocking, return to Focus, insights, and dark-theme checks. Repository screenshots are from that release build.

## Outstanding device coverage

Current third-party feed and browser versions, OEM background behavior, screen-reader interactions, widget launcher differences, Quick Settings behavior on older Android versions, and notification timing under extended Doze have not been fully verified. Website and feed detection remain explicitly experimental in the app. Store review is not completed. The v1.0.0 APK is signed with a dedicated local release key.
