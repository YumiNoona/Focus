# Focus for Android

An Android focus app built in Kotlin and Jetpack Compose. Version 1.0.0 is the first public release for Android 9 and newer.

## Try it

Install the supplied `Focus-v1.0.0.apk` on an Android phone. On first launch:

1. Choose **Make room**.
2. Read the focus-protection disclosure. If you agree, enable **Focus protection** in Android Accessibility settings.
3. Enable Usage Access for daily allowances and usage charts. Notifications are optional.
4. Open **Rules** and choose apps, websites, feeds, and routines.
5. Start a session from **Home**. If protection is off, Focus explicitly offers a timer-only session.

Android may require enabling “Install unknown apps” for the app opening this APK. The GitHub release APK is signed with a dedicated release certificate. It is distributed outside Google Play. On some Android versions, sideloaded accessibility access may require allowing restricted settings from Focus’s system App Info screen.

## Navigation and visual system

Exactly three bottom-dock icons: Home, Rules, Insights. Each has an accessibility label and selected state. Profile is at the top left, Settings at the top right. Nested screens use a back button.

Warm off-white background, near-black text, muted violet accents, neutral rounded surfaces, and minimal elevation. JetBrains Mono is bundled locally for headings, timers, controls, and compact labels. Body copy uses Android’s sans-serif. Both light and dark themes are provided. Primary actions have subtle optional haptics.

Type sizes are 44, 28, 24, 20, 18, 16, 14, and 12 sp. Spacing follows a 4 dp grid with 24 dp page margins, 24 dp card padding, 24 dp section gaps, 48 dp minimum interaction targets, and primary buttons at least 56 dp high. Content scrolls; primary buttons grow with text instead of clipping. No decorative motion is used in this version, including when reduced motion is enabled.

## Feature coverage

| Capability | Implementation |
| --- | --- |
| Instant / open-ended sessions | Start, view remaining or elapsed time, end, and recover persisted sessions |
| Focus profiles | Four presets, custom profiles, editable intentions, modes, app and website scopes |
| Pomodoro | Custom focus length, break length, and rounds; no final break |
| App blocking | User-consented accessibility overlay for selected foreground apps |
| Daily budgets | Android usage events; requires Usage Access and focus protection |
| Per-visit limits | Foreground visit duration while screen is interactive |
| Gradual reduction | Five minutes per day until the chosen target, with pause/resume |
| Website rules | Normalized domains and subdomains; experimental browser address-field detection |
| Feed rules | Opt-in experimental YouTube Shorts, Instagram Reels, Facebook Reels detection |
| Recurring routines | Weekday/weekend/custom days, overnight schedules, overlap warning |
| Conflict resolution | Applicable restrictions combine; strictest applicable mode wins |
| Interruptions | Gentle pause, standard 10-second wait, strict 30-second wait |
| Intentional visits | Optional reason, configurable duration, cooldown, local history |
| Ending strict sessions | 30-second pause and reason required within Focus |
| Essential access | Focus, phone, launcher, emergency and system settings are exempt |
| Insights | Daily logged focus, completed sessions, personal goal, selected-app usage, seven-day chart, prior-week comparison when data exists, override count, recent sessions |
| Quick Settings | Starts first favorite profile with protection enabled; otherwise opens Focus; active sessions open for review |
| Home-screen widget | Focus status and Start/View session action |
| Notifications | Session status, View session / Manage protection actions, optional completion and routine reminders |
| Permission recovery | Permission health display, service connection status, system-settings links |
| Profile | Local name, default intention, goal, custom focus profiles |
| Privacy | Local storage, no accounts/ads/analytics/network permission, export, deletion, consent withdrawal |
| Appearance | System/light/dark theme, haptics, reduced-motion preference, hidden insights |

### Scope and reliability

The app is useful without an account. A timer by itself does not block anything. Accessibility protection must be enabled and its disclosure accepted. Usage Access is separate.

Website blocking is **not a VPN, DNS filter, or system-wide firewall**. It inspects exposed address fields in Chrome, Edge, Brave, and Firefox during focus. Hidden address bars, private browsing, embedded browsers, browser updates, and unavailable accessibility nodes may bypass it. Domains are limited to ordinary ASCII hostnames in this version. Whole-browser app blocking is the stronger available fallback. No browsing content is saved.

Selective feed detection depends on third-party view identifiers or selected accessibility controls. It is **experimental and unverified on current Instagram/Facebook/YouTube production builds**. Messaging remains accessible when selective rules are the only rules enabled. Whole-app blocking takes precedence if also selected. Unsupported feeds such as TikTok use whole-app rules.

This is behavioral blocking, not device administration or kiosk mode. Android’s system settings can disable the service, the app can be uninstalled, and OEM background restrictions can stop protection. Strict mode adds deliberate friction without preventing emergency or system access.

Schedules evaluate local wall-clock time and the current timezone while the accessibility service is connected. Saved session deadlines survive normal process restarts and device reboots, but a user changing the device clock can change timing. Notification alarms are inexact and may be delayed by Doze or battery restrictions. No exact-alarm permission or permanent foreground monitoring service is used.

App-specific daily usage is derived from Android foreground activity events, not a claimed measurement of attention. Split-screen use, OS event availability, and time spent behind a blocking overlay can affect those values. Focus totals record session time excluding Pomodoro breaks; cross-midnight daily totals are apportioned estimates. Ongoing sessions are not included in “logged sessions” until ended. No invented “time saved” metric or sample usage is displayed. Insight comparison is between a rolling seven-day period including the unfinished current day and the preceding seven days, and is shown only when prior usage exists.

History and override records are capped at the latest 1,000 each. Export contains names, intentions, and override reasons, so choose an appropriate destination. Android’s own usage history is not deleted by Focus’s deletion action.

## Screen map

```text
Onboarding → Permission health → Android permission settings
Home → Session setup → Active session → Completion history
Home → Favorite focus profile → Session setup
Rules → Apps → Installed-app picker / App limits / Always allowed
Rules → Websites → Add/remove domain + coverage explanation
Rules → Feeds → Experimental selectors + app fallback
Rules → Schedules → Routine editor → Overlap explanation
Insights → Daily summary / Usage chart / Recent sessions
Profile → Name / Intention / Goal / Focus profile editor
Settings → Permissions / Appearance / Visits / Privacy / Export / Delete
Restricted app → Blocking screen → Return home / Intentional visit / Emergency access
Widget or tile → Session setup or current session
```

## Build and tests

Open this folder in Android Studio, let it discover your Android SDK, and use JDK 17 or newer. The bundled wrapper uses Gradle 8.14.3, Android Gradle Plugin 8.13.1, Kotlin 2.2.21, and a pinned Compose BOM. Install Android SDK 36.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
.\gradlew.bat connectedDebugAndroidTest
.\gradlew.bat lintDebug
```

The generated APK is at `app/build/outputs/apk/debug/app-debug.apk`. Connected tests require an Android device or emulator.

Important files:

- `Engine.kt`: pure rules, schedule, domain, session, and allowance logic.
- `Repository.kt`: persisted state, JSON export, and Android usage-event aggregation.
- `FocusAccessibilityService.kt`: user-consented app/website/feed enforcement and interruption overlay.
- `Integrations.kt`: notification scheduling, reboot receiver, Quick Settings tile, widget.
- `MainActivity.kt`: app screens, forms, navigation, permission explanations.
- `Theme.kt`: colors and typography.
- `EngineTest.kt`: rules tests.
- `AppTest.kt`: connected Android UI and persistence tests.

## Before distribution

Real-device verification is still required for OEM battery behavior, third-party feed/browser coverage, screen-reader operation, widgets, notification timing, and permission recovery across Android versions. Google Play review and any required accessibility declaration have not been submitted. Signing material remains private and is excluded from Git. Store assets and Google Play approval are not included.

Android and Google Play references used for the implementation:

- [Accessibility service documentation](https://developer.android.com/guide/topics/ui/accessibility/service)
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Android app widgets](https://developer.android.com/develop/ui/views/appwidgets/overview)
- [Google Play AccessibilityService requirements](https://support.google.com/googleplay/android-developer/answer/10964491)

JetBrains Mono is distributed under the SIL Open Font License; see [FONT-LICENSE.txt](../FONT-LICENSE.txt).
