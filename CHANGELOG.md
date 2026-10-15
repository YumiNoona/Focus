# Changelog

## Unreleased — flow review, 2026-10-15

- Preserve navigation, app-rule drafts, profile identities, and routine days/identities when Android recreates the activity. Keep explicit notification navigation working after restoration.
- Preserve explicitly selected profile artwork across storage round-trips; apply legacy illustration defaults only when artwork was never saved.
- Re-evaluate parked feed overlays using current consent and resistance rules, replacing stale visit buttons and releasing overlays after consent withdrawal.
- Add empty-profile routine recovery and a direct app-selection action for sessions without targets. Keep failed session starts in the editor and return to the correct screen after creating a profile.
- Add keyboard-operated website previews, linked tab panels, Escape/outside-click mobile-menu dismissal, and remove collapsed menu links from keyboard focus.
- Add regression checks for recreation, profile persistence, empty-state recovery, and parked-overlay updates. See [review and validation](docs/FLOW_REVIEW_2026-10-15.md).

## [1.2.0](https://github.com/YumiNoona/Focus/releases/tag/v1.2.0) — 2026-10-14

- Add six distinct transparent cat doodles for reminders, Resistant waiting, Strict boundaries, active focus, Pomodoro breaks, and completion, bringing the illustration set to nine scenes.
- Use mode-specific artwork in native blocking screens, early-ending and unblocking warnings, locked rules, and the custom-message preview. Keep black ink readable on pastel backings in dark mode.
- Add Normal/Resistant/Strict preview choices without changing the saved blocking mode or any enforcement policy.
- Refresh actual emulator screenshots with warnings, completed sessions, a real one-minute Pomodoro work period followed by a break, and dark focus.
- Publish a signed, optimized APK with version code 122 and the existing release certificate.

- Rename the Android app to FocuLY, using the supplied `public/logo.png` for its adaptive launcher icon and in-app wordmark. Preserve the package, administrator component, stored data, and update signing key.
- Add four onboarding screens: three pastel story cards with cat doodles followed by the live permission checklist. Include Next, Back, Skip intro, saved progress, and replay from Settings.
- Add original transparent cat illustrations across Home, active sessions, empty states, insights, protection setup, and native blocking overlays.
- Refresh the theme with warm paper, blush, lime, peach, lilac, and charcoal buttons; retain JetBrains Mono, dark mode, accessible targets, and the three-icon dock.

- Add final permission onboarding and a reusable live checklist with required/optional states.
- Add optional salted-hash Focus admin PIN, 90-second control unlock, failed-attempt cooldown, and Android screen-lock recovery outside Strict.
- Automatically block removing Focus during Strict on managed devices, independent of the persistent uninstall preference.
- Add individual managed app uninstall policies and launch quarantine after later installation, with day/week/manual durations, verified status, expiry cleanup, and a visible owner-only policy monitor.
- Add custom blocking headlines, supporting messages, emoji, presets, and preview; supply administrator support text to Android.
- Add direct Shorts and provider-specific Reels block buttons and compact device-control cards.
- Add PIN security, managed uninstall/later-install regression coverage, and an opt-in emulator gallery workflow.

- Add independent app, website, and feed blocks for today, one day, one week, custom minutes, or until manually unblocked.
- Persist blocks across restarts, keep them active independently of sessions, and reconcile automatic expiry.
- Reject indefinite Strict, preserve Strict integrity, enforce Resistant group-unblocking pauses, and combine overlapping rules by their strongest mode.
- Add Home shortcuts, active-block cards, app icons, duration/mode cards, bulk target selection, and concise confirmation summaries.
- Replace long setup/privacy sections with action cards and expandable details. Preserve drafts and scrolling through permission setup and return navigation.
- Show actual unlock dates and live remaining time in blocking overlays, with shorter copy and corrected minute labels.
- Extend managed Strict suspension and alarm scheduling to independent whole-app groups.
- Add calendar/DST, persistence, UI navigation, and installed-app regression checks.

Website/feed detection remains experimental; ordinary-phone Android settings still control accessibility.

## [1.1.0](https://github.com/YumiNoona/Focus/releases/tag/v1.1.0) — 2026-10-14

- Repair foreground tracking: recheck application windows continuously, ignore keyboards and accessibility overlays, and use usage-event fallback when windows are unavailable.
- Show live protection health, observed app, connection/overlay failures, explicit timer-only status, battery links, and a one-minute real-app test.
- Introduce Normal popup, Resistant 30-second wait with reason and one-minute visit, and finite Strict with no in-app temporary visits or early ending.
- Prevent Strict rule edits, consent withdrawal, data deletion, and existing-visit bypasses; Strict has no Pomodoro breaks or open-ended sessions.
- Preserve explicit empty custom scopes and plan routine end boundaries correctly.
- Add optional administrator uninstall resistance; opt-in device-owner uninstall blocking, app suspension, temporary clock protection, exact expiry, and persisted recovery.
- Add installed-app accessibility regression coverage, Strict backend tests, and dedicated-device/OxygenOS documentation.
- Keep the three-icon dock and existing visual design; reuse the v1.0.0 signing certificate for updates.

Selective websites/feeds remain experimental. Physical OnePlus Nord CE 2 verification remains outstanding; ordinary-phone Strict cannot prevent Android permission revocation.

## [1.0.0](https://github.com/YumiNoona/Focus/releases/tag/v1.0.0) — 2026-10-14

First public Focus release, developed from the earlier Still preview.

### Included

- Three-destination navigation with profile and settings in the header.
- JetBrains Mono typography, spacious layouts, light and dark themes.
- Instant and open-ended focus sessions, custom profiles, and Pomodoro rhythms.
- User-consented app blocking, daily allowances, per-visit limits, and gradual reduction.
- Weekday, weekend, custom-day, and overnight routines with overlap handling.
- Gentle, standard, and strict interruptions, intentional visits, reasons, and cooldowns.
- Local insights, focus history, personal goals, and Android usage measurements.
- Permission health, emergency exceptions, local export and deletion.
- Home-screen widget, Quick Settings tile, and optional notifications.
- Experimental website and short-video feed rules with coverage explanations.
- Gradle wrapper, JVM and connected Android tests, build automation, and documentation.
- Signed, optimized Android APK distributed through GitHub Releases.

### Known limitations

- Browser address-field detection and selective feed blocking depend on third-party app accessibility controls. They are experimental.
- Android settings, uninstalling, and device-specific service restrictions can bypass blocking.
- Reminder delivery can be delayed by Android battery restrictions.
- OEM coverage, current third-party feed versions, and screen-reader interactions need further device testing.
- GitHub distribution does not constitute Google Play approval.
