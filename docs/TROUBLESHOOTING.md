# Protection setup and recovery

## OnePlus Nord CE 2

The reported issue was whole-app blocking. This update repairs foreground tracking and checks a live service heartbeat rather than treating a saved Android permission as proof that protection is running. The Nord CE 2 itself has not been connected for physical-device verification. OxygenOS menu names vary by Android version.

1. Install the signed **v1.1.0** APK over v1.0.0. Open FocuLY once after updating.
2. In **Settings → Protection health**, accept the disclosure and enable **FocuLY protection** in Android Accessibility settings. If Android restricts this sideloaded app, open FocuLY's system **App info**, allow restricted settings if offered, then return to Accessibility.
3. Confirm that FocuLY shows the service as **connected**. If it is enabled but disconnected, toggle FocuLY protection off and back on in Android Accessibility settings, then reopen FocuLY.
4. Open FocuLY's **App info / battery settings** link. In OxygenOS battery settings, permit background activity and use unrestricted battery behavior where available. Remove FocuLY from automatic background restrictions. These controls vary with OxygenOS releases.
5. Choose a nonessential app under **Rules → Apps**. Make sure it is enabled and not in **Always allowed**. A session profile must include it, or use **all selected apps**.
6. Use **Settings → Test selected app for 1 minute**. FocuLY starts a finite Strict test and opens the first eligible selected app. That app should show a restriction until the minute ends. Return home or use emergency access while waiting.

Send the Android/OxygenOS version, whether the service says connected, the last observed app shown in Protection health, and the selected app if it still fails. Never include private screen content or an exported history in a public issue.

## Understand when a rule applies

- Whole-app rules block during an applicable session or routine. Merely selecting an app does not start a session. Daily and per-visit limits apply when their allowance is reached.
- **Timer only** explicitly leaves session blocking off. Strict cannot start as timer only.
- Usage Access is required for daily budgets and usage charts. It also supplies a fallback foreground signal when accessibility windows are unavailable.
- FocuLY, the launcher, phone, emergency access, and Android settings are exempt. An allowlisted app is not a test target.
- Normal and Resistant intentional visits temporarily permit the app. Strict ignores existing visits.
- Custom profiles can deliberately contain no app rules. Empty custom scope no longer means “all selected apps.”

## Other protection limits

Website and selective Shorts/Reels rules depend on exposed browser fields and third-party accessibility controls. They remain experimental. Select the whole browser or whole social app when selective detection misses content.

The accessibility service rechecks application windows every 500 ms, ignores keyboard and overlay windows, and can fall back to Android usage events. Background restrictions, unavailable OS events, or accessibility revocation can still interrupt ordinary-phone protection. A stale heartbeat or overlay failure is shown in Settings; active timer-only sessions and interrupted protection are labeled explicitly.

Ordinary Android administration adds a deactivation step before uninstalling. For actual Android-enforced uninstall blocking and whole-app suspension, see [managed devices](MANAGED_DEVICE.md).
