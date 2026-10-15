# Simpler focus and blocking — 15 October 2026

Development changes after v1.2.0. The published APK is unchanged.

## Fewer detours

| Task | Current path |
| --- | --- |
| Start a focus session | Home → Start focus → duration, resistance, targets → Start |
| Block an app for the first time | Home → Block apps → searchable selection sheet → duration and resistance → Block |
| Block a website | Home → Block websites → add a domain on that screen → Block |
| Pause short videos | Home → Shorts & Reels → switch on the desired feeds → Block |
| Change a profile's targets | Apps or Websites summary → selection sheet → Done |
| Connect protection while preparing a block | Connect protection → Android permissions → Back to block, retaining the draft |
| Change the theme | Settings → visible Appearance card |

Duration choices wrap to show Today, 1 day, 1 week, Until I unblock, and Set minutes. Start, Save, Block, and picker Done actions stay beneath scrolling content. A valid website addition dismisses the keyboard. Home shortcut and profile cards separate artwork from their labels and grow with their content.

Rules continues to hold saved targets, daily/visit limits, always-allowed apps, and routines. Optional device administration and privacy controls remain available. Strict consent, finite deadlines, waiting periods, essential access, and Android's administration limits are unchanged.

## Four additional doodles

Selection, feeds, routines, and appearance now have distinct transparent cat scenes. They are packaged locally and can also be chosen as profile artwork. Built-in imagegen generated each scene with `cat_pause.png` as its character/style reference; full prompts and usage appear in [the visual guide](FOCULY_DESIGN.md) and [prompt record](illustration-prompts.json). The supplied logo and earlier illustrations are preserved.

## Screens

The [updated screenshot set](screenshots/simple/README.md) shows the actual emulator UI, including dark mode and 130% text size with the keyboard open. Scroll Test is a separately installed fixture. The earlier v1.2.0 gallery remains historical.

## Verification

22 JVM engine tests, 19 Compose UI/backend checks, two PIN checks, two native overlay scenarios, and the new direct-block integration check pass on the isolated Android 15 emulator. Debug/test APK assembly and Android lint pass. The focused gallery verifies 19 views, including light/dark surfaces, search with the keyboard open, and 130% font size. All four new PNGs retain a 32-bit alpha channel with transparent corners.

Run functional checks on an isolated emulator; the tests reset FocuLY data. Install the optional Scroll Test fixture before native tests.

```powershell
./gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell am instrument -w -e class dev.still.focus.AppTest,dev.still.focus.DirectBlockTest,dev.still.focus.AdminPinTest,dev.still.focus.ProtectionScenarioTest dev.still.focus.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am instrument -w -e gallery true -e class dev.still.focus.ScreenshotGalleryTest#captureSimplifiedFlows dev.still.focus.test/androidx.test.runner.AndroidJUnitRunner
```

The direct-block regression creates app, website, and feed blocks through Home, verifies their stored targets and periods, and checks that the app block displays a native overlay over Scroll Test. It does not claim coverage of third-party Shorts/Reels or browser UI detection. Physical OnePlus Nord CE 2 verification remains separate.
