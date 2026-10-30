# Changelog

## [1.0.1](https://github.com/YumiNoona/Focus/releases/tag/v1.0.1) — frosted chrome and simpler Settings, 2026-10-30

- Add real in-app background blur to the dock, header, and floating focus actions using Haze, with a frosted fallback on older Android versions. Preserve sharp controls, highlights, safe areas, reduced motion, and disabled/Strict guards.
- Consolidate permission access into one Settings dropdown; group reminders and device policies/PIN. Remove the welcome replay, Essential access information row, duplicate setup/health cards, and repeated permission shortcuts.
- Capitalize page headings, show the selected profile once in Setup, and remove its redundant preset strip. Enlarge the four Home rhythm illustrations from 64 dp to 124 dp-high areas with roomier cards.
- Float the Setup focus action over scrolling content without a rectangular footer; keep final form controls reachable. Correct singular hour/minute labels.
- Bundle dependency/font licenses in APK assets. Publish an optimized, officially signed, non-debuggable production APK with Android version code 128.

## [1.0.0](https://github.com/YumiNoona/Focus/releases/tag/v1.0.0) — fresh glass-header build, 2026-10-30

- Replace the appearance chip row with a top-right Settings sun/moon switch. Smoothly blend the existing screen's palette and backdrop over 440 ms; preserve forms and scroll state. Reduce motion snaps instantly.
- Share translucent chrome between the header and three-icon dock. Root pages and Settings scroll behind the top bar with leading safe space; fixed editor controls keep their top inset. Profile remains top left and Settings top right on main pages.
- Start new installs in Light and migrate old System choices once to the current Android appearance. Persist explicit Light/Dark choices independently of later system changes.
- Capitalize the Settings footer to “Made with 💜 by Veil,” keeping the existing website link.
- Replace all earlier GitHub releases with this fresh v1.0.0, using Android version code 127 and the existing package/signing identity for in-place updates.
- Explicitly disable release debugging. Check the actual signed production app on an emulator; CI now verifies release variants and publishes reports without debug APK artifacts.

## Historical 1.0.1 — 2026-10-30

- Keep README as the only root Markdown file. Move contributor/security policies into `.github`, supporting documentation into `docs`, legal notices into `docs/legal`, and the signing example into `config`.
- Remove superseded planning notes, obsolete branding, and duplicate historical screenshot galleries. Retain the current dock/personalization galleries and representative block setup, blocking, and reminder captures; earlier material remains available in Git history.
- Consolidate build/signing instructions and validation coverage, shorten the README, and repair documentation links.
- Keep local emulator/release archives outside the source tree and explicitly ignore regenerated local work/output folders.
- Publish Android version code 126 with the existing package/signing certificate. Application behavior is unchanged from the new v1.0.0.

## Historical 1.0.0 (Android code 125) — 2026-10-30

- Restart the public FocuLY release series at v1.0.0 and replace earlier GitHub releases, retaining source history and the official signing key. Internal Android version code 125 allows updates from former builds, including v1.4.0 (124), without deleting saved data.
- Rework the three-icon dock after inspecting Stash: a translucent capsule, fine highlight rim, soft shadow, and clear margins. Content now scrolls behind the dock instead of stopping at a reserved footer area; trailing scroll space keeps final actions reachable.
- Slide the selected tab's glass pill over 180 ms, respect Reduce motion and haptic settings, expose Android tab selection semantics, and keep taps on the current tab from resetting its position.
- Share usage snapshots across tabs instead of making another Android usage query on each navigation tap. Remove the Android navigation contrast scrim on supported versions.
- Retain the complete blocking, personalization, onboarding, managed protection, and break reminder features documented below.

## Earlier development history

The entries below record retired releases. Their downloads were removed on 30 October 2026; this history does not describe separate currently published releases. The original 14 October v1.0.0 is distinct from the new 30 October v1.0.0.

## Historical 1.4.0 — 2026-10-16

### Reinstall protection and break reminders

- Retain app IDs, active bans, original end dates, app rules, profiles, and routines when a target app is removed or reinstalled. Refresh installation status and icons; end old temporary visits without deleting cooldown history. Package updates preserve visits.
- Verify managed app suspension against Android even when cached targets match, repairing a lost policy after reinstall. Automatically protect FocuLY removal, force-stop/data controls, and clock settings while managed bans or focus rules are active; release owned restrictions when they end.
- Add verified managed-removal notification artwork and administrator support text. Personal-phone administration gets a deactivation warning and protection-change notification; Android still controls deactivation/removal on those devices.
- Add optional illustrated usage-break notifications for unconfigured apps too, with 30/45/60/90-minute thresholds, daytime-only delivery, per-app/day deduplication, two-hour global spacing, three-per-day cap, and Quiet today/Take a break actions. No block is created by a reminder.
- Check usage in the connected protection service and through Android's periodic job scheduler when the service is unavailable. Require Usage Access and notification permission; keep the feature independently configurable during Strict.

### Personal blocking screens

- Replace onboarding's separate dash row with a four-step progress ring around the circular Next/Done button. Float it over the shared backdrop without a footer panel.
- Float the app picker's Done action over the list and retain space to reach the last app.
- Add ten original cat illustrations for breathing, moon naps, tea, reading, headphones, clouds, flowers, a kite, a pillow, and a lantern. Choose artwork independently of resistance mode, match each mode automatically, or hide it.
- Add personal image/sticker selection through Android's file picker, private local copies, transparent image support, bounded resizing, and shared preview/native-overlay rendering. Animated images use a still frame.
- Offer 24 larger emoji/symbol suggestions and keep the custom blocking screen's Save action visible. Artwork choices persist with existing messages; Strict still prevents customization changes.

## Historical 1.3.0 — 2026-10-16

### Glass interface

- Make Home's app and website shortcuts illustration-only, with larger centered cats and accessible button labels.
- Restyle the complete Android interface with a bundled misty lake backdrop, translucent blue glass panels, highlight borders, pale buttons, and coordinated inputs, pickers, dialogs, and charts.
- Float the existing three-icon dock above the background; retain direct blocking, fixed primary actions, and corrected cat doodles.
- Use lighter sans-serif headings/body copy while retaining JetBrains Mono timers and small labels. Provide light and deeper dark glass themes.
- Carry the design into onboarding, native Normal/Resistant/Strict overlays, and the home-screen widget without changing protection behavior or adding permissions.

### Illustration corrections

- Add a second visible back leg to the selection and plant-watering cats, preserving both existing forepaws and their props.
- Remove extra forelegs/paws from the selection, calendar, and plant-watering cat illustrations. Preserve their props, character, palette, and transparent backgrounds.

### Simpler flows and fixes

- Simplify Home shortcuts: choose and block apps directly, add websites within block setup, or open all Shorts/Reels controls together. Remove the intermediate saved-profile detour from blocking.
- Share searchable app and website selection sheets across focus/profile setup, show target summaries directly, and keep Start/Save/Block actions visible while scrolling.
- Return from permission setup to the original draft, consolidate protection setup, expose appearance controls, and dismiss the keyboard after adding a valid website.
- Add four transparent cat doodles for choosing targets, feeds, routines, and appearance; include them in profile artwork choices.
- Preserve navigation, app-rule drafts, profile identities, and routine days/identities when Android recreates the activity. Keep explicit notification navigation working after restoration.
- Preserve explicitly selected profile artwork across storage round-trips; apply legacy illustration defaults only when artwork was never saved.
- Re-evaluate parked feed overlays using current consent and resistance rules, replacing stale visit buttons and releasing overlays after consent withdrawal.
- Add empty-profile routine recovery and a direct app-selection action for sessions without targets. Keep failed session starts in the editor and return to the correct screen after creating a profile.
- Add keyboard-operated website previews, linked tab panels, Escape/outside-click mobile-menu dismissal, and remove collapsed menu links from keyboard focus.
- Add regression checks for recreation, profile persistence, empty-state recovery, and parked-overlay updates. See the [validation summary](VALIDATION.md).

## Historical 1.2.0 — 2026-10-14

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

## Historical 1.1.0 — 2026-10-14

- Repair foreground tracking: recheck application windows continuously, ignore keyboards and accessibility overlays, and use usage-event fallback when windows are unavailable.
- Show live protection health, observed app, connection/overlay failures, explicit timer-only status, battery links, and a one-minute real-app test.
- Introduce Normal popup, Resistant 30-second wait with reason and one-minute visit, and finite Strict with no in-app temporary visits or early ending.
- Prevent Strict rule edits, consent withdrawal, data deletion, and existing-visit bypasses; Strict has no Pomodoro breaks or open-ended sessions.
- Preserve explicit empty custom scopes and plan routine end boundaries correctly.
- Add optional administrator uninstall resistance; opt-in device-owner uninstall blocking, app suspension, temporary clock protection, exact expiry, and persisted recovery.
- Add installed-app accessibility regression coverage, Strict backend tests, and dedicated-device/OxygenOS documentation.
- Keep the three-icon dock and existing visual design; reuse the v1.0.0 signing certificate for updates.

Selective websites/feeds remain experimental. Physical OnePlus Nord CE 2 verification remains outstanding; ordinary-phone Strict cannot prevent Android permission revocation.

## Historical 1.0.0 — 2026-10-14

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
