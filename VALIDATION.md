# Validation — v1.1.0, 14 October 2026

Compiled with Android SDK 36. Runtime checks used isolated Android 15 / API 35 Pixel 7 emulators, including a separate fresh device-owner emulator. The user's physical OnePlus Nord CE 2 was not connected.

## Automated checks

- **16 JVM rules tests passed:** overnight boundaries, cross-week conflicts, next schedule start/end, Pomodoro phases, focus totals, restriction/allowlist precedence, domain normalization, daily limits, visit expiry/cooldown, gradual reduction, custom scope, Strict existing-visit rejection, and foreground window selection.
- **6 connected UI/backend tests passed:** three-icon navigation and header actions, website normalization/persistence, explicit timer-only startup, JSON round-trip, Strict early-stop/visit/deletion/rule-edit rejection, expiry, and rejection of unbounded/timer-only Strict.
- **1 installed-app accessibility scenario passed with no skip:** enables protection on an isolated emulator, launches Scroll Test, accepts the Normal popup, waits through Resistant and supplies a reason for a one-minute visit, then verifies Strict ignores that existing visit. Checks notification-shade transitions, home/relaunch recovery, and automatic expiry. The fixture is a separate project, not part of Focus.
- Android lint passed without errors; debug assembly and signed release assembly passed. Release builds use R8 and resource shrinking.
- Signed APK metadata: Focus, version **1.1.0**, version code **110**, minimum API **28**, target API **36**. APK signature verification passed. It requests no network permission.
- The release signing certificate is unchanged from v1.0.0. SHA-256 fingerprint: `BEEE1A11A24A40F99110F85124CB096F30CBC0CAFAA06DBFFF8B4420AFAECA00`.

The installed-app test originally exposed a fallback startup gap when no accessibility window/event identified the foreground package. After moving usage-event recovery before that early return, all seven connected tests passed together. Earlier unsuccessful runs are not counted as passing validation.

## Signed APK and administration checks

- Installed the optimized release APK on the ordinary emulator and completed onboarding/disclosure, app selection, and the real one-minute Strict test. The overlay appeared over Scroll Test with no temporary-access button.
- Activated optional legacy administration through Android's actual activation screen. A developer uninstall attempt returned `DELETE_FAILED_DEVICE_POLICY_MANAGER`. Focus's disable control deactivated the administrator outside Strict.
- On a separate fresh device-owner emulator, enabled uninstall protection and verified Android refused removal.
- Started managed Strict with an eligible whole-app target and precise-ending permission. Android reported the target suspended and clock configuration restricted.
- Disabled accessibility while managed Strict was active: the target remained suspended.
- At expiry, Android reported the target unsuspended and Focus's temporary clock restriction removed, while accessibility remained disabled.
- Focus preserves essential/system access. Managed setup remained isolated from personal devices.

Home, rules, theme, insights, and session screenshots retain the v1.0.0 visual design. The blocking and administrator screenshots show actual v1.1.0 behavior. No fake usage history is presented. Initial v1.0.0 validation also covered light/dark theme and 150% text scaling.

## Reproduce

Use a disposable emulator; the UI tests reset Focus's local state.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew -p test-fixture assembleDebug
adb install test-fixture/build/outputs/apk/debug/ScrollTest-debug.apk
./gradlew connectedDebugAndroidTest
```

Set `ANDROID_SERIAL` if multiple devices are connected. The installed-app scenario deliberately skips physical phones and emulators without Scroll Test. A run with a skip does not verify accessibility blocking. Do not run the connected installer against the separate uninstall-protected device-owner emulator.

## Outstanding coverage

Physical OnePlus/OxygenOS battery behavior, current third-party feed/browser builds, screen-reader interactions, widget launcher differences, older Android versions, extended Doze timing, and managed reboot recovery before first unlock need further device testing. Selective website/feed detection remains experimental. Ordinary-phone Strict cannot prevent Android permission revocation or administrator deactivation. Exact managed expiry assumes Android can access Focus's credential-protected storage; open Focus after unlock if recovery is delayed. Google Play review has not been completed.
