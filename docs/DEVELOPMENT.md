# Development

## Build

Use JDK 17 or newer and Android SDK 36. The checked-in Gradle wrapper pins the build entry point; Android Studio can create the ignored `local.properties` SDK setting, or use `ANDROID_HOME`.

```bash
./gradlew testReleaseUnitTest lintRelease assembleRelease
```

Use `gradlew.bat` on Windows. Configure signing below for an installable official update. Distribute only the optimized, signed production release; do not upload debug or unsigned APKs.

## Release signing

Copy [config/keystore.properties.example](../config/keystore.properties.example) to `keystore.properties` in the repository root, then set your private keystore path and credentials. Alternatively, supply environment variables:

| Property | Environment variable |
| --- | --- |
| `storeFile` | `FOCUS_STORE_FILE` |
| `storePassword` | `FOCUS_STORE_PASSWORD` |
| `keyAlias` | `FOCUS_KEY_ALIAS` |
| `keyPassword` | `FOCUS_KEY_PASSWORD` |

```bash
./gradlew assembleRelease
```

Release output is `app/build/outputs/apk/release/app-release.apk` when signing is configured. Without configuration, the APK is unsigned. R8 optimization and resource shrinking are enabled. The official key is private and excluded from Git; only the maintainer can sign compatible official updates. A different certificate cannot update an existing installation. Keep a private backup of your key and credentials.

Advance Android `versionCode` for every distributed update even when public numbering changes. The current v1.0.1 uses code 128, higher than the fresh v1.0.0 (127) and earlier builds. Keep package `dev.still.focus`, storage keys, and signing identity stable. Upload signed binaries and checksums to GitHub Releases rather than Git history. Release builds explicitly disable debugging.

## Production UI checks

Install the officially signed release APK with `adb install -r` on a disposable emulator to verify update compatibility, persisted theme/rules, header/dock interactions, font scaling, consent, and a real fixture interruption. Read Android's UI hierarchy and capture actual frames; do not replace measured usage with invented data. This fresh release was checked directly as a production APK, without generating a debug app APK.

## Existing instrumentation suite

Use an isolated Android emulator. Tests replace local FocuLY state and are unsuitable for personal phones.

```bash
./gradlew -p test-fixture assembleDebug
adb install test-fixture/build/outputs/apk/debug/ScrollTest-debug.apk
adb push test-fixture/build/outputs/apk/debug/ScrollTest-debug.apk /data/local/tmp/focus-fixture.apk
./gradlew connectedDebugAndroidTest
```

Set `ANDROID_SERIAL` when more than one device is connected. The fixture is never bundled in FocuLY. Reinstall tests use the APK under `/data/local/tmp`; missing-fixture scenarios skip rather than proving enforcement. Debug test APKs cannot instrument a release-signed installation.

`ManagedLifecycleTest` needs a separate disposable device-owner emulator; see [managed setup](MANAGED_DEVICE.md). Provisioning is a dedicated-device operation and should not be used to reset an everyday phone. Ordinary tests do not verify device-owner enforcement.

Screenshot tests are opt-in. After assembling/installing the matching debug app/test APKs, for example:

```bash
adb shell am instrument -w -e gallery true -e class dev.still.focus.ScreenshotGalleryTest#captureFloatingDock dev.still.focus.test/androidx.test.runner.AndroidJUnitRunner
```

Captures are saved in the app's external files `screen-gallery` directory. Review real output before adding selected compressed images to documentation. Do not publish private images, identifiers, or fabricated usage statistics. [Recorded results and limits](VALIDATION.md).

## Source map

| Location | Purpose |
| --- | --- |
| `app/src/main/java/dev/still/focus/` | Rules, persistence, Compose screens, accessibility enforcement, notifications, managed policies, artwork |
| `app/src/test/` | JVM policy tests |
| `app/src/androidTest/` | Android behavior checks and opt-in captures |
| `test-fixture/` | Disposable local distraction app |
| `docs/` | Current user/developer guides, changelog, validation, legal notices, selected screenshots |
| `config/` | Non-secret configuration examples |
| `public/` | Supplied app logo |
| `web/` | Optional marketing website; see its [README](../web/README.md) |
| `.github/` | Contributor/security policies and Android CI |

Keep release artifacts, logs, exports, emulator disks, and caches out of Git. Locally generated `outputs/` and `local-work/` are ignored; prefer a separate workspace outside the source checkout for large archives. SDK settings and signing credentials are intentionally local and ignored.

[Android CI](../.github/workflows/android.yml) checks release-variant JVM tests, release lint, and optimized release assembly. It uploads reports only, has no private signing secrets, and does not run connected-device checks. Its unsigned verification APK is not published. Final GitHub Releases contain only a verified, officially signed production APK and its checksum. [Contribution expectations](../.github/CONTRIBUTING.md).
