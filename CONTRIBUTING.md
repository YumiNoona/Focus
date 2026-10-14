# Contributing to Focus

Open an issue describing the behavior you want to change before a large contribution. For bug reports, include Android version, device model, Focus version, steps to reproduce, and relevant permission status. Remove names, intentions, browsing information, and override reasons from any shared logs or screenshots.

## Development

1. Clone the repository and open it in Android Studio.
2. Install Android SDK 36 and use JDK 17 or newer.
3. Let Android Studio create your local SDK configuration, or set `ANDROID_HOME`.
4. Run `./gradlew testDebugUnitTest lintDebug assembleDebug`.
5. For UI checks, connect a device or emulator and run `./gradlew connectedDebugAndroidTest`.

Use `gradlew.bat` on Windows. Keep changes focused and explain the trigger, resulting behavior, and verification in pull requests.

## Expectations

- Preserve the three-icon dock and accessible profile/settings actions.
- Follow the 4 dp spacing grid, reusable theme, font scaling, and minimum touch sizes.
- Keep permission explanations accurate and blocking status honest.
- Preserve phone, emergency, system-settings, launcher, and Focus access.
- Test changes to schedules, enforcement, usage budgets, and override timing.
- Do not replace measured insights with invented data.
- Do not add analytics, accounts, network transmission, or browsing-content storage without explicit product review.
- Keep signing keys, machine paths, build outputs, and personal exports out of Git.

The CI workflow checks JVM tests, Android lint, and debug assembly. Connected-device coverage is documented separately in `VALIDATION.md`.
