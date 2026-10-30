# Validation

## v1.0.1 — frosted chrome and simpler Settings, 30 October 2026

The signed production app uses version `1.0.1`, Android code **128**, and the existing package/certificate. R8 and resource shrinking remain enabled; debugging is disabled. No debug app APK is built or distributed for this iteration.

- All 27 release JVM tests pass; release lint reports 0 errors and 66 warnings. Production assembly succeeds. Haze/font licenses and notices are bundled in APK assets.
- Updated the official production installation on the isolated Android 15 emulator without resetting its saved website or onboarding. The header, dock, and floating focus action render actual in-app background blur while controls remain sharp. Android 9–11 use Haze's scrim fallback; these versions were not exercised for this change.
- Expanded the single Permissions dropdown, checked its ordinary-phone access controls, opened Android Usage access, and returned successfully. Reminders exposes both break and completion/routine preferences. Welcome replay, Essential access, and duplicated setup/health shortcuts are removed from Settings.
- Opened Personal time from the enlarged rhythm cards. Setup shows the chosen profile once; the final Websites target opens and saves its existing selection. Strict's glass action remains disabled until acknowledged. The Normal glass action starts a real session that interrupts Scroll Test with an accessibility overlay; ending the session returns to Home.
- Checked light/dark chrome, reduced motion, retained Settings scroll during theme toggles, and status-area clearance. At 130% text, the last rhythm card opens and the final Websites control scrolls above the floating action and physically opens. Captures include these enlarged-text states.

The updated [production captures](screenshots/dock/README.md) document this iteration. Historical protection/reinstall and managed-device checks below retain their original scope; this visual refresh does not establish new bypass resistance.

## Fresh v1.0.0 — production glass-header refresh, 30 October 2026

Android metadata is `dev.still.focus`, version `1.0.0`, code **127**, Android 9+. This replaces prior releases while retaining the official signing key and stored rules. The final APK is optimized with R8, resource shrinking, and debugging explicitly disabled. No debug app APK is generated or published for this refresh; CI checks release variants and uploads reports only.

- All 27 release-variant JVM tests pass (22 engine, 5 reminder/removal policy). Release lint reports 0 errors and 65 warnings, including existing style/dependency suggestions. Signed release assembly and APK metadata/certificate verification pass.
- Updated an actual official production installation (code 125) to code 127 on the isolated Android 15 emulator. The saved website and onboarding remain. The old System choice, tested with Android dark mode enabled, migrates to explicit Dark. Changing Android to Light and restarting FocuLY preserves that explicit choice.
- The top-right Settings switch toggles both ways; the old System/Light/Dark chips are absent. Settings scroll position stays unchanged across theme switches, including with Reduce motion enabled. Captured palette samples include a real intermediate shade between dark and light; this confirms a rendered transition, not a frame-rate guarantee.
- The capitalized Made footer and Veil website control are present. Header actions and the last profile remain reachable at 130% text size. Home/Rules/Insights/Settings scroll beneath the shared glass chrome; status-bar padding keeps scrolling text out of system icons.
- Accepted the disclosure in the production app and enabled accessibility on the emulator. Saved a one-minute Strict block through the app's real target/duration/mode/acknowledgment flow. Launching Scroll Test produces a real accessibility overlay before and after changing theme in Settings. After expiry the fixture opens without that overlay. Theme changes do not weaken the active block.
- Android UI hierarchy capture temporarily suppresses other accessibility services, so blocking checks use ordinary physical taps, window-state inspection, and real screenshots after reconnection. They do not confuse an automation-induced disconnection with a product bypass. No private app-state injection or debug instrumentation is used for these production UI checks.

Physical OnePlus and current third-party feed/browser review remain outstanding. The ordinary-phone uninstall limits below still apply. Current [header/dock and Settings captures](screenshots/dock/README.md) come from the signed production app; older coverage below is historical rather than a claim that every connected test was rerun for this refresh.

## Historical v1.0.1 — repository cleanup, 30 October 2026

This release changes documentation/layout and Android version metadata only; protection logic, UI, package `dev.still.focus`, storage keys, and the official signing identity are unchanged from v1.0.0. Android version code 126 supports an in-place update from 125 and former builds.

All 27 JVM tests pass on a fresh run. Debug/release assembly and Android lint pass (0 errors, 64 existing warnings); APK metadata and the official signing certificate verify. All 96 local references across 20 Markdown/HTML files resolve after relocation. The release includes a SHA-256 checksum for its signed APK. Existing Android functional checks below are retained; they are not rerun solely for documentation cleanup.

Official release certificate SHA-256:

```text
BEEE1A11A24A40F99110F85124CB096F30CBC0CAFAA06DBFFF8B4420AFAECA00
```

Each GitHub release supplies the signed APK and `SHA256SUMS.txt`. Private signing credentials remain excluded from Git. Source cleanup removes old galleries and iteration notes from the current checkout; Git history retains them.

## Verified app behavior

| Check | Recorded result and scope |
| --- | --- |
| JVM policy tests | 27 pass: 22 engine and 5 break/removal policy tests |
| Dock refresh, 30 October | 20 checks across `AppTest` and `DirectBlockTest` pass across the main run and focused rechecks: selected-tab state, scroll restoration, drafts/recreation, target saving, and a real fixture interruption |
| Dock capture | Eight real Android 15 captures pass: Home, scrolling behind glass, Rules, Insights, dark/reduced-motion Home, dark scrolling, 130% text, and physical access to the final profile |
| Version reset update | Installed official v1.4.0 (124), completed onboarding, and saved a website. Updating to the restarted v1.0.0 (125) preserves onboarding and that saved rule |
| Reinstalls/reminders, 16 October | 28 ordinary-emulator checks pass, including actual fixture removal/reinstall for manual/day/week/Strict bans, original deadlines, resumed overlays, expiry, notification artwork/actions, deduplication, and Quiet today |
| Managed protection, 16 October | Two separate device-owner checks pass: actual uninstall refusal, suspension repair, post-install quarantine, FocuLY removal protection during a Normal ban, clock/user-control restrictions, verified notification wording, and expiry/release |
| Personal artwork | Private image import/size/alpha bounds, state restoration, export round-trip, built-in choice, removal, invalid input, Strict save rejection, and a native overlay using imported artwork pass |

The dock's initial run exposed a transient detached Compose root during recreation and two physical taps hidden by the overlay. The test wait now tolerates activity handover, and flow checks explicitly scroll/verify targets above the dock before tapping. All three cases pass on recheck. This did not change protection logic.

Notification screenshots use explicit Scroll Test usage input, not invented measured user history. Gallery captures use a separate test app and private bundled artwork; no personal image is published. See the [dock gallery](screenshots/dock/README.md), [personalization gallery](screenshots/personalization/README.md), and [reminder guide](REINSTALL_AND_REMINDERS.md).

## Remaining coverage

Physical OnePlus Nord CE 2, other Android/OEM versions, manual screen-reader operation, three-button navigation for the dock refresh, current production feed/browser releases, widget behavior, and real background reminder timing still need device review. Ordinary Android administrator access cannot enforce OS uninstall blocking. Managed tests do not establish resistance to root, recovery, factory reset, other Android profiles, or privileged tools.

## Reproducing tests

Follow the [development guide](DEVELOPMENT.md). Use disposable emulators: connected tests reset app state, and lifecycle tests uninstall/reinstall only the fixture. Managed tests require a separate device-owner emulator. The opt-in screenshot suite requires `-e gallery true`; it should not run on a personal phone. Skipped fixture/device-owner scenarios do not verify enforcement.

Older per-iteration logs and screenshots remain in Git history. Local release/emulator archives are kept outside the source checkout rather than published as source files.
