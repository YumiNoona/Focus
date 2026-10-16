# Validation — FocuLY v1.2.0, 14 October 2026

## Glass interface — 16 October 2026

- 22 JVM engine tests and all 24 Android checks pass (19 UI/backend, 1 direct Home blocking integration, 2 PIN, 2 native overlay scenarios) on the isolated Android 15 emulator with Scroll Test installed.
- Debug/test APK assembly, Android lint, and optimized release assembly pass. The release build remains local; this design update does not publish a new APK release or change the version/signing identity.
- Cards, onboarding, target sheets, settings, dialogs, primary actions, the dock, native blocking overlays, and the home-screen widget now share the glass direction. The generated backdrop is bundled locally and opaque; no permission or network access was added.
- Native scenarios verify Normal visits, Resistant waiting/reasons, Strict enforcement/expiry, transient windows, independent blocks, and parked-overlay refresh/consent withdrawal with the new renderer.
- The [glass design guide](docs/GLASS_DESIGN.md) records rendering choices, older-Android fallback, the background prompt, and actual emulator captures. Physical OnePlus testing remains outstanding.
- All three opt-in gallery tests pass: full pages/native overlays, simplified flows with keyboard and 130% text size, and real completion/dark-focus milestones. The published development gallery exports 45 actual views plus an overview; raw PNGs remain local.
- The local preview APK verifies with the existing release certificate SHA-256 `BEEE1A11A24A40F99110F85124CB096F30CBC0CAFAA06DBFFF8B4420AFAECA00`.

## Subsequent development review — 15 October 2026

The [simpler-flow iteration](docs/SIMPLER_FLOWS.md) adds four cat doodles, direct Home blocking, shared target sheets, visible duration choices, and persistent primary actions. Updated Android regressions cover direct entry without saved targets, normalized website drafts, picker restoration, profile scopes, and returning from permissions. Its [current screenshot set](docs/screenshots/simple/README.md) is separate from the historical release gallery below.

The current source contains additional Android/web changes after the published v1.2.0 APK. The [flow review](docs/FLOW_REVIEW_2026-10-15.md) records fixes for restoration, saved illustrations, parked-overlay consent/mode changes, setup recovery, and website keyboard navigation, along with validation and reproduction steps. Earlier release checks below describe the tagged release and its historical gallery.

## FocuLY v1.2.0 release

- Six additional imagegen scenes preserve transparent alpha and the original mascot style. Nine local cat assets ship with the app; the supplied logo remains unchanged.
- All 21 JVM tests and 13 Android checks pass (10 UI/backend, 2 PIN, 1 installed-app blocking scenario). The blocking scenario verifies actual Normal visits, Resistant waiting/reasons, Strict restrictions, transient windows, independent blocks, and expiry with the new overlays.
- Android lint, debug/test APK assembly, and optimized signed release assembly pass. APK metadata is `dev.still.focus`, version `1.2.0`, code `122`, Android 9+, label `FocuLY`.
- The APK retains release certificate SHA-256 `BEEE1A11A24A40F99110F85124CB096F30CBC0CAFAA06DBFFF8B4420AFAECA00`, supporting updates from v1.0.0/v1.1.0.
- Warnings retain their buttons and gain scrollable content. Native illustration heights are unchanged, and dark mode uses pastel backings for black ink. Artwork does not change rule enforcement or grant extra access.
- The full opt-in gallery capture passes. A focused milestone recapture also passes with an explicit `BREAK TIME` display assertion after a real one-minute work period; its test clock is resumed before taking the image. The gallery contains 57 screenshots, including complete warning/completion cards and a 130% font-scale warning with both actions visible. Managed comparison images are retained from the preceding validation of unchanged controls.
- Physical OnePlus Nord CE 2 testing remains outstanding. Earlier functional/device-owner validation is recorded below; managed-policy behavior is unchanged by this illustration update.

## FocuLY branding and onboarding

- The supplied logo is packaged byte-for-byte unchanged. Three original transparent cat illustrations were created with the built-in imagegen tool; prompts and asset locations are recorded in [the visual guide](docs/FOCULY_DESIGN.md).
- Four onboarding screens include three pastel story cards and the live permission checklist. Next, Back, Skip intro, and timer-only completion are covered by an Android UI regression.
- 21 JVM tests and 13 Android checks pass (10 UI/backend, 2 PIN, 1 installed-app accessibility). The native scenario verifies the newly illustrated Normal, Resistant, and Strict overlays.
- Android lint, debug build, and optimized signed build pass. Existing package, data keys, administrator identity, and release signing certificate are preserved; version code advances to 121 for updates.
- Physical OnePlus testing and manual screen-reader review remain outside this emulator validation.
- The updated gallery capture passes and contains 50 current screenshots, including all four onboarding screens, long-page sections, native overlays, and refreshed managed-device comparisons. The intro was also reviewed at 130% Android text scaling; its primary action remains visible.

## Latest device-controls iteration

- 21 JVM tests pass. 9 Compose UI/backend checks and 2 PIN security checks pass on the ordinary Android 15 emulator, including direct Shorts setup and message/policy serialization.
- The device-owner Android 15 regression verifies actual `DELETE_FAILED` refusal for a protected fixture, uninstall after releasing policy, launch suspension following a later installation, release at expiry, and automatic Focus uninstall protection during Strict even with its persistent preference off.
- A real package-added test exposed Android's manifest broadcast limits; the owner-only visible foreground monitor and dynamic receiver corrected the later-install case. Managed policy checks pass with accessibility suppressed.
- Android lint and debug/optimized signed assembly pass. The Android screen-lock recovery flow is implemented but requires manual credential-flow testing; no physical OnePlus test is claimed.
- The opt-in `ScreenshotGalleryTest` captures real pages with a separate Scroll Test fixture and no fabricated usage statistics. It is limited to emulators and requires `-e gallery true`.
- Screenshots are visually inspected after capture. The workflow explicitly closes Android's notification panel, foregrounds each page through Android, and reconnects protection before capturing native overlays.
- The final opt-in gallery run passes. **48 screenshots** cover all principal pages, long-page sections, the three overlays, and three real device-owner comparisons, captured after the application-source commit `572e0d2`. The gallery is available in [docs/screenshots/gallery](docs/screenshots/gallery/README.md).
- The installed-app accessibility regression also passes with the customizable overlay: Normal visit, Resistant pause/reason, Strict enforcement, transient windows, independent groups, and expiry. Together these runs verify 34 functional/security checks (21 JVM + 11 UI/PIN + 1 accessibility + 1 managed-device).

## Earlier independent-block validation

- **21 JVM tests passed.** New coverage verifies independent app/web/feed scope, exact expiry, persistent manual blocks, calendar midnight/day/week and daylight-saving boundaries, strongest-mode precedence, Strict visit rejection, managed package selection, and latest matching unlock deadlines.
- **8 Android UI/backend tests passed.** Includes three-icon/header navigation, website persistence, timer-only clarity, JSON round-trip with blocks, Strict rejection/expiry, day/week/manual setup, disabled indefinite Strict, and preservation of drafts through permission settings. Bulk select/clear followed by Settings and Back was explicitly rerun after correcting a nonserializable selection view.
- **1 installed-app accessibility regression passed.** Exercises Normal, Resistant, and Strict against the separately installed Scroll Test app, transient windows, temporary visits, cooldown rules, Resistant group-unblocking delay/reason validation, independent manual blocking without a session, duplicate-group prevention, and independent Strict expiry.
- The full connected run completed 9 tests without a skip. The subsequent bulk-selection check reran all 8 UI/backend tests successfully.
- Android lint, debug assembly, optimized signed APK assembly, and signature verification passed. The development label was compiled and linted after the functional checks.

## Local preview checks

- Installed the signed 1.2.0-dev APK on an isolated Android 15 / API 35 emulator.
- Reviewed actual duration cards, app icons, active-block cards, compact settings, and a persistent primary button. Adjusted duration-card layout and kept the action visible while scrolling and entering minutes.
- Created a Normal app block with no session and no automatic ending; the popup appeared over Scroll Test.
- Restarted the app process and verified the manual block remained present. Reconnected protection afterward and retained the rule.
- On a separate device-owner emulator, updated the development APK with its matching debug certificate, created an independent finite Strict app group, and verified Android suspension remained when accessibility was disabled.
- At expiry, Android reported the target unsuspended and the temporary clock policy resolved to null. The managed emulator was then closed; the ordinary emulator remains available for iteration.
- The local signed preview uses the same release certificate as v1.0.0/v1.1.0: `BEEE1A11A24A40F99110F85124CB096F30CBC0CAFAA06DBFFF8B4420AFAECA00`.

The complete gallery and README examples now use current development captures, including session, theme, insights, and administration pages. No fake usage history is presented. The ordinary emulator currently runs the matching debug build used for screen capture; the separately signed local preview is available in `outputs/Focus-preview.apk`.

## Reproduce

Use an isolated emulator: connected UI tests reset Focus's local data. Do not run the installer against an uninstall-protected device-owner instance.

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew -p test-fixture assembleDebug
adb install test-fixture/build/outputs/apk/debug/ScrollTest-debug.apk
./gradlew connectedDebugAndroidTest
```

Set `ANDROID_SERIAL` if multiple devices are connected. The installed-app scenario skips physical phones and emulators without the fixture. A skipped run does not verify accessibility enforcement. A release-signed app and a debug-signed test APK cannot update each other; use an isolated test installation.

## Coverage limits

The physical OnePlus Nord CE 2 was not connected for this preview. Current third-party browser/feed versions, OEM battery behavior, screen-reader operation, widgets/tiles across older Android versions, extended Doze, and managed reboot recovery before first unlock need further device coverage. Website/feed detection remains experimental. Android settings can revoke ordinary-phone accessibility or deactivate legacy administration. Managed recovery depends on credential-protected storage becoming available after unlock. Google Play review has not been completed.
