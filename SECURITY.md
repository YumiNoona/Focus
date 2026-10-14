# Security and privacy

Focus processes accessibility controls and supported browser address fields on the device to apply user-selected rules. It does not save screen content or request network access. Stored data includes chosen rules, local profile information, focus sessions, and override reasons. Exports contain that data.

Do not post sensitive exports, signing material, passwords, or screen content in public issues. For a suspected security vulnerability, use [GitHub private vulnerability reporting](https://github.com/YumiNoona/Focus/security/advisories/new) if available. If that repository feature is unavailable, request a private reporting channel without disclosing exploit details publicly.

Version 1.1.x is the current published release line. Version 1.2 is in development. No response-time guarantee is currently offered.

Strict rejects early ending, temporary visits, weakening rules, consent withdrawal, and local deletion inside Focus until its finite deadline. Ordinary phones still permit Android permission revocation and administrator deactivation. Home and emergency access remain available.

Optional legacy administration adds a deactivation step before uninstall. Separately provisioned device-owner management can enforce an uninstall block and suspend selected apps during managed Strict, with exact-alarm expiry and a persisted recovery journal. It requests no wipe/password/camera/lock policy. See [managed-device behavior and recovery limits](docs/MANAGED_DEVICE.md); this is not a claim to resist root, recovery, factory reset, or a device administrator.
