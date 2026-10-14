# Scroll Test

Separate, offline Android test app for the installed-app protection regression. It is not a module of Focus and is never bundled in the Focus APK.

On an isolated emulator:

```bash
./gradlew -p test-fixture assembleDebug
adb install test-fixture/build/outputs/apk/debug/ScrollTest-debug.apk
./gradlew connectedDebugAndroidTest
```

Use `ANDROID_SERIAL` to select the test emulator when multiple devices are connected. `ProtectionScenarioTest` enables accessibility and Usage Access on the emulator, opens Scroll Test, exercises Normal/Resistant/Strict, then clears its test state and restores the previous accessibility setting. The scenario skips physical phones and emulators without the fixture. The remaining UI tests also reset Focus's local state; run them on test devices only.
