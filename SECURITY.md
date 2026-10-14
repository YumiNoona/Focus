# Security and privacy

FocuLY processes accessibility controls and supported browser address fields on the device to apply user-selected rules. It does not save screen content or request network access. Stored data includes chosen rules, local profile information, focus sessions, and override reasons. Exports contain that data.

Do not post sensitive exports, signing material, passwords, or screen content in public issues. For a suspected security vulnerability, use [GitHub private vulnerability reporting](https://github.com/YumiNoona/Focus/security/advisories/new) if available. If that repository feature is unavailable, request a private reporting channel without disclosing exploit details publicly.

Version 1.1.x is the current published release line. Version 1.2 is in development. No response-time guarantee is currently offered.

Strict rejects early ending, temporary visits, weakening rules, consent withdrawal, and local deletion inside FocuLY until its finite deadline. Ordinary phones still permit Android permission revocation and administrator deactivation. Home and emergency access remain available.

Optional legacy administration adds a deactivation step before uninstall. Separately provisioned device-owner management can enforce an uninstall block and suspend selected apps during managed Strict, with exact-alarm expiry and a persisted recovery journal. It requests no wipe/password/camera/lock policy. See [managed-device behavior and recovery limits](docs/MANAGED_DEVICE.md); this is not a claim to resist root, recovery, factory reset, or a device administrator.

The optional FocuLY PIN protects in-app device controls, consent withdrawal, and data deletion. It cannot secure Android's settings or prevent administrator deactivation on ordinary phones. A random salted PBKDF2-HMAC-SHA256 hash is stored separately from exports; backup is disabled. Five failed attempts cause a persisted one-minute cooldown. Android screen-lock recovery is disallowed during Strict. See [app controls and PIN behavior](docs/APP_CONTROLS.md).

Individual managed launch rules suspend an app after installation; they do not stop its download or installation beforehand. A visible foreground policy monitor is used only on device-owner installations with active policies. Android owns its suspension/uninstall dialogs. Prepared rules on personal phones remain inactive. Third-party feed and browser detection is experimental.
