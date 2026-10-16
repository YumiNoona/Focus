# Optional managed-device protection

Focus has two separate administration levels. Neither is enabled automatically.

| Setup | What Android enforces | User control |
| --- | --- | --- |
| Personal phone, optional active administrator | Administrator must be deactivated before uninstalling Focus | Deactivate through Android settings, or through Focus outside Strict |
| Separately provisioned device owner | Automatic FocuLY uninstall block during active bans/focus rules/app policies; optional persistent uninstall block; selected whole-app suspension during managed Strict | Device administrator controls provisioning; OS recovery and management remain outside Focus |

Focus requests an empty legacy administrator policy list: no wipe, password, camera, or screen-lock policy. It does not intercept uninstall screens through accessibility.

## Personal-phone uninstall resistance

Open **Settings → Uninstall protection**, enable the optional administrator, and read Android's activation screen. To disable it outside Strict, use the corresponding Focus control or Android's administrator settings. Android may require deactivation before uninstalling. This is an extra deliberate step, not an irreversible lock.

## Device-owner setup

Device owner is a provisioning role for a dedicated device. Focus cannot grant itself that role on an already configured personal phone. Use a fresh isolated emulator or a dedicated device prepared by its administrator. Do not reset your everyday phone for this feature.

For a fresh developer emulator that meets Android's provisioning prerequisites, install Focus and assign the role:

```bash
adb install Focus-v1.1.0.apk
adb shell dpm set-device-owner dev.still.focus/.FocusAdminReceiver
```

Android may reject this command on a configured device or one containing accounts. Follow [Android's dedicated-device provisioning guidance](https://developer.android.com/work/dpc/dedicated-devices) for a managed deployment. A device-owner APK is not generally removable through ordinary developer uninstall commands; keep this setup isolated.

In Focus's **Uninstall protection** screen:

1. Check that the device-owner role is verified.
2. Enable Android uninstall protection if desired. Focus displays the policy actually reported by Android.
3. Enable managed Strict suspension and allow precise-ending alarms.
4. Select at least one supported nonessential whole-app target, enable accessibility, and start a finite Strict session.

## Policies and expiry

- `setUninstallBlocked` automatically protects FocuLY during active bans, focus rules with selected targets, or individual app policies on a device-owner installation. The separate persistent preference also protects it outside active rules; switch that preference off outside Strict when desired.
- `setPackagesSuspended` prevents launching selected eligible apps during managed Strict, including when accessibility is off. Websites and selective feeds still depend on accessibility.
- On Android 11+, FocuLY user controls are disabled while removal protection is active to resist force-stop/data clearing through ordinary system UI.
- While bans/focus rules/app policies are active, FocuLY adds a date/time configuration restriction if its own administrator does not already hold it. It removes only the restriction it added when those rules end; persistent removal protection alone does not keep clock settings locked.
- Focus journals packages before applying policies. Expiry reconciles the journal, unsuspends its targets, and restores its temporary restrictions. Application restart and boot/time broadcasts also reconcile saved deadlines.
- Exact-alarm access is required before managed Strict starts. Ordinary routines use inexact alarms. After reboot, credential-protected storage can defer recovery until first unlock; open Focus if release is delayed. Do not suspend essential applications.

Individual uninstall and future-install launch rules are explained in [App controls](APP_CONTROLS.md). A visible foreground monitor keeps a dynamic installation receiver and deadline checks alive while managed policies are active. Launch quarantine applies after installation; it is not a pre-download store filter. Android owns its restriction dialogs; Focus supplies administrator support text where supported.

The monitor's cat notification reports **Android blocks FocuLY removal** only after the OS confirms that policy. Ordinary administrator installs instead receive an Android deactivation warning and a notification explaining the protection change. A notification, captcha, or in-app PIN cannot veto Android's personal-phone deactivation screen. [Reinstall behavior and reminders](REINSTALL_AND_REMINDERS.md).

This is selected-app management, not a kiosk implementation. Device-owner provisioning, root, recovery, factory reset, and another administrator's policies remain outside Focus's in-app guarantees. A personal OnePlus phone receives accessibility protection and optional administrator friction unless it was separately provisioned as a managed device.

References: [DevicePolicyManager APIs](https://developer.android.com/reference/android/app/admin/DevicePolicyManager), [device administration and uninstall behavior](https://developer.android.com/work/device-admin), and [exact alarms](https://developer.android.com/develop/background-work/services/alarms/schedule).
