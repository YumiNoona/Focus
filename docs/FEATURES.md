# FocuLY for Android

An Android focus app built in Kotlin and Jetpack Compose. Version 1.2.0 adds independent day/week/manual blocks and a simpler setup for Android 9 and newer.

## Try it

Build the development APK from source to try version 1.2. The latest published APK remains v1.1.0. On first launch:

1. Choose **Make room**.
2. Read the focus-protection disclosure. If you agree, enable **FocuLY protection** in Android Accessibility settings.
3. Enable Usage Access for daily allowances and usage charts. Notifications are optional.
4. Open **Rules** and choose apps, websites, feeds, and routines.
5. Start a session from **Home**. If protection is off, FocuLY explicitly offers a timer-only session.

Android may require enabling “Install unknown apps” for the app opening this APK. The GitHub release APK is signed with a dedicated release certificate. It is distributed outside Google Play. On some Android versions, sideloaded accessibility access may require allowing restricted settings from FocuLY’s system App Info screen.

## Navigation and visual system

Exactly three bottom-dock icons: Home, Rules, Insights. Each has an accessibility label and selected state. Profile is at the top left, Settings at the top right. Nested screens use a back button.

Warm off-white background, near-black text, muted violet accents, neutral rounded surfaces, and minimal elevation. JetBrains Mono is bundled locally for headings, timers, controls, and compact labels. Body copy uses Android’s sans-serif. Both light and dark themes are provided. Primary actions have subtle optional haptics.

Type sizes are 44, 28, 24, 20, 18, 16, 14, and 12 sp. Spacing follows a 4 dp grid with 24 dp page margins, 24 dp card padding, 24 dp section gaps, 48 dp minimum interaction targets, and primary buttons at least 56 dp high. Content scrolls; primary buttons grow with text instead of clipping. No decorative motion is used in this version, including when reduced motion is enabled.

## Feature coverage

Version 1.2.0 adds independent app, website, and feed groups with today/day/week/manual/custom-minute endings. These work without a session, remain active when a session ends, and appear on Home. Strict requires automatic expiry; Resistant group removal includes a backend-checked pause and reason. See [BLOCKS.md](BLOCKS.md).

Setup now uses compact permission action cards, expandable explanations, app icons, duration/mode cards, and saved drafts when returning from Settings. Existing timers, profiles, routines, budgets, essential access, and the three-icon dock remain available.

| Capability | Implementation |
| --- | --- |
| Instant / open-ended sessions | Start, view time, and recover persisted sessions; Strict requires a finite deadline |
| FocuLY profiles | Four presets, illustrated custom profiles, direct app and website scopes, selectable durations up to one week, and deletion with linked-routine cleanup; empty custom scope stays empty |
| App blocking | User-consented accessibility overlay for selected foreground apps |
| Daily budgets | Android usage events; requires Usage Access and focus protection |
| Per-visit limits | Foreground visit duration while screen is interactive |
| Gradual reduction | Five minutes per day until the chosen target, with pause/resume |
| Website rules | Normalized domains and subdomains; experimental browser address-field detection |
| Feed rules | Opt-in experimental YouTube Shorts, Instagram Reels, Facebook Reels detection |
| Recurring routines | Weekday/weekend/custom days, overnight schedules, overlap warning |
| Conflict resolution | Applicable restrictions combine; strictest applicable mode wins |
| Interruptions | Normal compact popup; Resistant full screen and 30-second wait; Strict full screen with no visit option |
| Intentional visits | Normal configurable visit/reason; Resistant mandatory reason, one-minute visit and minimum ten-minute cooldown; Strict never permits visits |
| Ending sessions | Normal confirmation; Resistant deliberate pause and reason; Strict no early ending even through backend actions |
| Strict integrity | Existing visits ignored; no rule weakening, consent withdrawal, data deletion, timer-only, or open-ended timer |
| Optional administration | Administrator deactivation step before uninstall; actual uninstall blocking/app suspension only with provisioned device owner |
| Essential access | FocuLY, phone, launcher, emergency and system settings are exempt |
| Insights | Daily logged focus, completed sessions, personal goal, selected-app usage, seven-day chart, prior-week comparison when data exists, override count, recent sessions |
| Quick Settings | Starts first favorite profile with protection enabled; otherwise opens FocuLY; active sessions open for review |
| Home-screen widget | FocuLY status and Start/View session action |
| Notifications | Session status, View session / Manage protection actions, optional completion and routine reminders |
| Permission recovery | Live heartbeat, last observed app, failed overlay feedback, battery/app-info links, one-minute selected-app test |
| Profile | Local name, default intention, goal, custom focus profiles |
| Privacy | Local storage, no accounts/ads/analytics/network permission, export, deletion, consent withdrawal |
| Appearance | Top-right Settings light/dark switch, smooth color transitions, haptics, reduced motion, hidden insights |

### Scope and reliability

The app is useful without an account. A timer by itself does not block anything. Accessibility protection must be enabled and its disclosure accepted. Usage Access is separate.

Website blocking is **not a VPN, DNS filter, or system-wide firewall**. It inspects exposed address fields in Chrome, Edge, Brave, and Firefox during focus. Hidden address bars, private browsing, embedded browsers, browser updates, and unavailable accessibility nodes may bypass it. Domains are limited to ordinary ASCII hostnames in this version. Whole-browser app blocking is the stronger available fallback. No browsing content is saved.

Selective feed detection depends on third-party view identifiers or selected accessibility controls. It is **experimental and unverified on current Instagram/Facebook/YouTube production builds**. Messaging remains accessible when selective rules are the only rules enabled. Whole-app blocking takes precedence if also selected. Unsupported feeds such as TikTok use whole-app rules.

On an ordinary phone, Android settings can disable accessibility and deactivate the optional administrator. Strict removes in-app escape paths but retains home, emergency, and system access. Separately provisioned device-owner management adds opt-in OS uninstall blocking and selected whole-app suspension. See [managed setup and recovery](MANAGED_DEVICE.md). FocuLY does not implement kiosk mode.

Schedules evaluate local time and timezone; both start and end boundaries are planned. Saved deadlines survive normal process restarts and reboots, but a user changing the clock can change ordinary-phone timing. Ordinary reminders use inexact alarms. Managed Strict requires precise-ending permission, temporarily restricts clock configuration, journals suspended targets, and releases them at expiry independently of accessibility. Reboot recovery can wait until first unlock. Device-owner protection uses a visible foreground monitor while managed policies are active; ordinary-phone blocking relies on the connected accessibility service.

App-specific daily usage is derived from Android foreground activity events, not a claimed measurement of attention. Split-screen use, OS event availability, and time spent behind a blocking overlay can affect those values. FocuLY totals record elapsed session time; cross-midnight daily totals are apportioned estimates. Ongoing sessions are not included in “logged sessions” until ended. No invented “time saved” metric or sample usage is displayed. Insight comparison is between a rolling seven-day period including the unfinished current day and the preceding seven days, and is shown only when prior usage exists.

History and override records are capped at the latest 1,000 each. Export contains names, intentions, and override reasons. Local deletion is unavailable during Strict. Android's own usage history is not deleted by FocuLY.

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
Profile → Name / Intention / Goal / FocuLY profile editor
Settings → Permissions / Appearance / Visits / Privacy / Export / Delete
Settings → Uninstall & managed protection → Android administration / Device-owner controls
Restricted app → Blocking screen → Return home / Intentional visit / Emergency access
Widget or tile → Session setup or current session
```

## Build and tests

The [development guide](DEVELOPMENT.md) covers the SDK/JDK setup, signed production release builds, source map, and isolated emulator checks. The [validation record](VALIDATION.md) separates verified behavior from remaining device coverage.

## Before distribution

Real-device verification is still required for OEM battery behavior, third-party feed/browser coverage, screen-reader operation, widgets, notification timing, and permission recovery across Android versions. Google Play review and any required accessibility declaration have not been submitted. Signing material remains private and is excluded from Git. Store assets and Google Play approval are not included.

Android and Google Play references used for the implementation:

- [Accessibility service documentation](https://developer.android.com/guide/topics/ui/accessibility/service)
- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [Android app widgets](https://developer.android.com/develop/ui/views/appwidgets/overview)
- [Google Play AccessibilityService requirements](https://support.google.com/googleplay/android-developer/answer/10964491)

JetBrains Mono is distributed under the SIL Open Font License; see [FONT-LICENSE.txt](legal/FONT-LICENSE.txt).
