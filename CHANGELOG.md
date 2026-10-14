# Changelog

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
