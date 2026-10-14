# Device and app controls

FocuLY has two different levels of Android administration.

| Setup | What FocuLY can enforce |
| --- | --- |
| Regular installation | In-app Strict rules, accessibility blocking, and optional legacy-admin uninstall resistance. Android can still deactivate the administrator or accessibility service. |
| Device owner | OS suspension of whole apps, FocuLY uninstall blocking during Strict, optional persistent uninstall blocking, and individual app uninstall/suspension rules. |

The FocuLY admin PIN guards device-control changes, consent withdrawal, and local data deletion inside FocuLY. It does not protect Android's settings, admin deactivation, app-data reset, or factory reset. Unlocks last 90 seconds in memory. Failed attempts are persisted; five failed attempts cause a one-minute cooldown. PIN hashes use PBKDF2-HMAC-SHA256 with a random 32-byte salt and 120,000 iterations, stored separately from exports and excluded from Android backups. Android screen-lock verification can reset the PIN outside Strict; Strict cannot be recovered away early through this flow.

## Individual apps

Device-owner installs run a visible foreground policy monitor while app policies or Strict rules are active. It keeps a dynamic package receiver alive and reconciles deadlines every two seconds. Android 8+ restricts manifest-only package-added receivers. On Android 11+, FocuLY also requests that Android disable force-stop/data-clear controls for FocuLY while these policies are active. Rules can still be removed inside FocuLY with the appropriate PIN outside Strict; older Android and administrator tooling have additional limitations.

Open Settings → Uninstall & managed protection → Individual app controls. Choose a known app, a selected installed app, or enter its exact package ID.

- **Pause after installation:** suspend the app when present, including after later Play Store or APK installation. The download and installation are not prevented. There can be a short delay between installation and the package-added receiver applying policy.
- **Keep installed:** Android blocks uninstalling that individual package while its rule is active.
- **Duration:** one day, one week, or until removed. Reboot, package events, opening FocuLY, the protection service, and deadline alarms reconcile rules. Without precise alarm permission, Android can delay background expiry; opening FocuLY reconciles immediately.

Rules on regular phones are labelled **prepared, not enforced**. A future-install rule on a managed device shows **waiting for installation**. Installed policies show separate verified uninstall and suspension status. Essential apps and FocuLY's allowlist are excluded from the editor. FocuLY journals policy targets so removal/expiry can release its own policies after process restart.

Android owns the store, installer, suspension, and uninstall dialogs. FocuLY cannot replace every one with its custom overlay. FocuLY supplies custom administrator support text where Android supports it. The full custom overlay is used for accessibility-detected app, website, and feed restrictions.

Selective prevention before installation requires enterprise managed Play app-distribution policy, beyond this standalone app. See [Android's management app policies](https://developers.google.com/android/management/apps) and [DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager). FocuLY does not block system settings or admin deactivation using accessibility.

See [managed-device setup](MANAGED_DEVICE.md) for the separate dedicated-device workflow. Do not reset a personal phone simply to test it.
