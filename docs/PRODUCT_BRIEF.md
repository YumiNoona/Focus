# Focus: smoother setup and stronger app boundaries

Build on the existing Android Focus app, preserving its three-icon dock (Home, Rules, Insights), profile at the top left, and settings at the top right. Keep JetBrains Mono headings, generous open space, rounded cards, a quiet violet accent, meaningful icons, 48 dp minimum touch targets, and the existing typography scale. Put the primary action next to the relevant app or feed; avoid repeating the same menu on several screens.

## This iteration

- End onboarding with a live permission checklist. Ask for accessibility consent before opening Android settings. Include usage access, optional administrator activation, optional Focus PIN, notifications, and background activity. Show what is connected, what is optional, and a timer-only path when protection is unavailable. Reuse this checklist in settings.
- Explain Strict before activation. It has no early exit inside Focus. On ordinary phones Android still permits disabling accessibility and deactivating administrators. Legacy administrator activation adds uninstall friction. Device-owner management is required for OS uninstall blocking. Never promise a password that locks Android's administrator settings.
- On managed devices, automatically block uninstalling Focus while any Strict rule is active. Offer persistent self-uninstall protection outside Strict separately. Keep selected whole-app suspension, clock protection, deadline reconciliation, and emergency/system access.
- Add a 6–12 digit Focus admin PIN for sensitive in-app device controls. Store a salted derived hash separately from rule exports. Require the existing PIN before replacement/removal; rate-limit failed attempts; expire an unlock after 90 seconds. Permit recovery through Android's screen-lock credential only outside Strict. Do not change Android's password or request wipe powers.
- Add a customizable blocking screen with a headline, optional supporting message, emoji, presets, and live preview. Preserve app identity, mode, real unlock time, and mode-specific actions. Keep message changes locked during Strict.
- Give Shorts and each Reels provider a direct block button leading to the same duration/mode editor as apps and websites. Clearly label selective detection experimental and offer whole-app fallback.
- Add individual app-management rules using exact package IDs, including apps not yet installed. Let users pause launches after installation, keep selected apps installed, or combine both. Offer one day, one week, or until the rule is removed. Show prepared, waiting, and verified states distinctly.
- Enforce individual policies only through Android device-owner APIs. Reapply after package addition/replacement and release policies when rules expire or are removed. Exempt essential and always-allowed apps. Do not intercept or hide Android's uninstall/admin screens using accessibility.
- Be precise about installation coverage: quarantine happens after installation, not before download. Android may show its own suspension/admin dialog, with Focus support text where supported. A selective pre-install store filter requires enterprise managed Play configuration and is not part of this standalone personal app.
- Test PIN protection, state persistence, the real overlay, managed uninstall refusal, later-install suspension, and expiry/release. Capture all principal pages and important long-page sections on an emulator after committing the code. Commit and push source changes; do not publish a release until requested.

## Suggested next iterations

1. A guided OnePlus protection diagnostic with a real app launch, connection checks, and battery-setting instructions tailored to the Android/OxygenOS version.
2. Optional per-rule blocking messages using the same message editor, with a global default.
3. Accessibility options: larger type preview, higher-contrast themes, and TalkBack review across the onboarding and overlay flows.
4. Enterprise provisioning with managed Play policies if preventing selected apps before installation is a firm requirement. Treat this as a separate managed-device product flow.
5. Optional accountability PIN handoff with a deliberate recovery policy, designed around the device owner's authorization.

These suggestions are future work, not claims of features included in this iteration.
