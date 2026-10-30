<div align="center">

<img src="public/logo.png" alt="FocuLY" width="104" />

# FocuLY

### A little less scroll. A little more you.

An Android app for focus sessions, everyday boundaries, and intentional screen time.

[![Android CI](https://github.com/YumiNoona/Focus/actions/workflows/android.yml/badge.svg?branch=main)](https://github.com/YumiNoona/Focus/actions/workflows/android.yml)
[![Latest release](https://img.shields.io/github/v/release/YumiNoona/Focus?color=7760BA)](https://github.com/YumiNoona/Focus/releases/latest)
[![Android](https://img.shields.io/badge/Android-9%2B-3DDC84?logo=android&logoColor=white)](#installation)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.21-7F52FF?logo=kotlin&logoColor=white)](app/build.gradle.kts)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white)](docs/DESIGN.md)
[![Privacy](https://img.shields.io/badge/Data-on%20device-538565)](#privacy)

**[Download v1.0.1](https://github.com/YumiNoona/Focus/releases/tag/v1.0.1)** · **[What's new](docs/CHANGELOG.md)** · **[Protection setup](docs/TROUBLESHOOTING.md)**

</div>

## Make room for what matters

- Block apps, websites, or feeds for today, a day, a week, custom minutes, or until you unblock them.
- Choose **Normal**, **Resistant**, or **Strict** interruptions. Strict requires an automatic ending and has no in-app temporary visits or early stop.
- Start a focus session, reuse a profile, or schedule weekday, weekend, and overnight routines.
- Set daily app budgets, per-visit limits, essential-app exceptions, and gradual reduction goals.
- Keep saved bans and original deadlines when a target app is uninstalled and reinstalled.
- Personalize blocking screens with cat doodles, your own image/sticker, messages, and emoji.
- Get optional illustrated break reminders for heavily used apps, including apps without a saved rule.
- View locally measured usage and session history, with widgets, a Quick Settings tile, and permission health.

A misty lake backdrop and translucent cards frame a floating, see-through dock. Exactly **three icons — Home, Rules, Insights** — sit at the bottom; Profile stays top left and Settings top right. Selection respects Reduce motion and optional haptics.

## Screenshots

<table>
  <tr><th>Home</th><th>Rules</th><th>Block setup</th></tr>
  <tr>
    <td><img src="docs/screenshots/dock/dock-01-home.jpg" alt="Home with transparent floating dock" width="250" /></td>
    <td><img src="docs/screenshots/dock/dock-03-rules.jpg" alt="Rules with the selected dock destination" width="250" /></td>
    <td><img src="docs/screenshots/block-setup.jpg" alt="Block duration and resistance choices" width="250" /></td>
  </tr>
</table>

[Dock, dark mode, and large text](docs/screenshots/dock/README.md) · [Personalization and onboarding](docs/screenshots/personalization/README.md) · [Break notifications](docs/REINSTALL_AND_REMINDERS.md)

Screenshots are real Android emulator captures. Scroll Test is a separate fixture; usage history is not fabricated. The block setup/overlay reference captures predate the dock refresh and show the shared glass material.

## Installation

1. Download **FocuLY-v1.0.1.apk** from the [latest release](https://github.com/YumiNoona/Focus/releases/latest) and open it on Android 9 or newer.
2. Allow installation from your browser/file manager if Android asks.
3. Follow the four-screen introduction, read the disclosure, and enable **FocuLY protection** in Android Accessibility settings for blocking.
4. Grant **Usage Access** for budgets/insights and notification permission for reminders. Android may require allowing restricted settings for a sideloaded app.
5. Choose a Home blocking card, set its targets/duration/mode, and tap **Block now**. Check **Settings → Protection health** and open a selected app to verify enforcement.

Install over your existing official app to retain saved data. FocuLY keeps package `dev.still.focus` and the official signing certificate. This v1.0.1 uses Android version code **126**, higher than both the restarted v1.0.0 (125) and former v1.4.0 (124). Debug builds use a different certificate. This APK is distributed through GitHub, not Google Play.

## Protection and coverage

Whole-app blocking uses a user-consented accessibility overlay. Phone, emergency access, your launcher, Android settings, and FocuLY stay available. On ordinary phones, Android still allows permission revocation and administrator deactivation/removal. **OS-enforced uninstall blocking requires separately provisioned device-owner management.**

Website and selective Shorts/Reels detection are experimental and depend on third-party accessibility support. Whole-app/browser blocking is the fallback. OEM battery restrictions can interrupt protection or delay reminders. Android usage events measure foreground activity, not attention. See [coverage](docs/FEATURES.md), [OnePlus setup](docs/TROUBLESHOOTING.md), and [validation limits](docs/VALIDATION.md).

## Documentation

| Guide | Contents |
| --- | --- |
| [Features](docs/FEATURES.md) | Coverage, permissions, rule precedence, and measurements |
| [Blocking](docs/BLOCKS.md) | Durations, modes, unblocking, and overlapping rules |
| [Custom artwork](docs/BLOCKING_ARTWORK.md) | Cats, personal stickers, emoji, and private storage |
| [Reinstalls and reminders](docs/REINSTALL_AND_REMINDERS.md) | Original deadlines, removal limits, and gentle notifications |
| [App controls](docs/APP_CONTROLS.md) / [managed devices](docs/MANAGED_DEVICE.md) | PIN, individual app policies, provisioning, and recovery |
| [Design](docs/DESIGN.md) | Glass materials, typography, dock, and artwork provenance |
| [Development](docs/DEVELOPMENT.md) / [validation](docs/VALIDATION.md) | Build, signing, isolated tests, and verified coverage |

## Build from source

Use **JDK 17+** and **Android SDK 36**. Open the repository in Android Studio, or set `ANDROID_HOME` to your SDK installation.

```bash
git clone https://github.com/YumiNoona/Focus.git
cd Focus
./gradlew testDebugUnitTest lintDebug assembleDebug
```

On Windows, use `gradlew.bat`. Debug output is `app/build/outputs/apk/debug/app-debug.apk`. [Release signing and emulator testing](docs/DEVELOPMENT.md) are documented separately. The optional marketing website lives in [web](web/README.md).

## Privacy

FocuLY has no network permission, analytics, advertising, account, or cloud sync. Rules, profile details, session history, and override reasons stay locally stored. Accessibility content is checked in memory without recording or transmission; imported artwork is copied into private storage. Android backup/transfer is disabled.

JSON export contains personal intentions and override reasons. **Delete local data** clears FocuLY's records and artwork outside Strict; Android's usage history remains managed by Android. Histories retain the latest 1,000 entries. No JSON import feature is provided.

## Contributing and notices

[Contributing](.github/CONTRIBUTING.md) · [Security reporting](.github/SECURITY.md) · [Third-party notices](docs/legal/THIRD_PARTY_NOTICES.md) · [JetBrains Mono license](docs/legal/FONT-LICENSE.txt)

The public version series restarted at v1.0.0 on 30 October 2026. Earlier development records remain in [Git history](https://github.com/YumiNoona/Focus/commits/main/) and the [changelog](docs/CHANGELOG.md).
