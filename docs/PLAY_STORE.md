# Play Store preparation

## Build to upload

The current candidate is FocuLY `1.0.2` (`versionCode` 130), package `dev.still.focus`, Android 9+ (`minSdk` 28), targeting API 36. Build the optimized Android App Bundle with the established upload key:

```bash
./gradlew testReleaseUnitTest lintRelease bundleRelease
```

The output is `app/build/outputs/bundle/release/app-release.aab`. Signing is configured with a private `keystore.properties` or `FOCUS_*` environment variables as described in [Development](DEVELOPMENT.md). CI deliberately builds without secrets, so its AAB is **unsigned and cannot be submitted**. Enroll in Play App Signing and upload only a bundle signed with the publisher's upload key. Keep the package name, key identity, and a higher version code for every subsequent update. Test an internal-track delivery from Play before production.

## Public listing draft

- **App name:** FocuLY
- **Short description:** Make room for what matters. Pause apps, sites, Shorts and Reels.
- **Full description:** FocuLY helps you set intentional boundaries with distracting apps, websites, Shorts and Reels. Choose a gentle pause, a 30-second resistant break, or a strict commitment. Set a one-off block, schedule quiet hours, or start a focus profile. Your rules and focus history stay on your device. No account, ads, or analytics. Accessibility protection and Usage Access are optional; focus timers still work without them. Some device-owner controls require separate dedicated-device setup.
- **Category:** Productivity (publisher choice)
- **Privacy policy URL:** `https://focus.venusapp.in/privacy` once the `web/` project is deployed there and the page is publicly reachable.
- **Support contact:** Publisher must supply a monitored support email in Play Console. Do not substitute a fictional address.

Use recent, unmodified screenshots from a release-equivalent build, showing the actual current app UI. Confirm the store icon, screenshots, and feature graphic meet the current Play Console specifications before upload. The checked-in website screenshots are marketing assets, not automatically a store-ready screenshot set.

## Data safety and permission declarations

The Android manifest does not request `INTERNET`; the app stores focus configuration, usage-derived insights, history, optional artwork, and a salted PIN hash on the device. Export is an explicit user action through Android's document picker. Verify the compiled release and any added SDKs before answering the Play Data safety form. The public [privacy policy](../web/public/privacy.html) must match the final shipped build and be linked in the app and listing.

The publisher must complete Play Console declarations for:

1. **Accessibility API:** Explain foreground-app, supported address-field, and feed-control checks plus the user-selected blocking overlay. The app declares `isAccessibilityTool=false` and shows an in-app disclosure with affirmative consent before opening Android's Accessibility settings. Provide a short demonstration video if requested.
2. **Foreground service `specialUse`:** The service is for optional dedicated-device owner policy reconciliation. Explain why it needs foreground execution and provide the requested demonstration. Review whether this specialized feature is appropriate for the general Play listing before submission.
3. **Exact alarm:** Used only for optional precise ending of managed lockdown. Verify the current Play Console declaration path and document the user-facing use case.
4. **Device admin/device owner:** Explain the dedicated-device flow, uninstall resistance, and how an owner can remove management. The publisher should review Play's restrictions on apps that prevent disabling or uninstalling.
5. **App access and testing:** Provide Play reviewers steps for consent and optional protection. Ordinary timers do not need special access; testing blocking requires granting Accessibility (and Usage Access for usage-based rules). Dedicated-device features need a separate provisioned test device.

## Release checks still requiring publisher action

- Deploy `web/` to the intended public domain and verify `/privacy` from an unauthenticated browser, including its link in Settings.
- Add a real support email, store listing graphics, content rating, target audience, ads declaration, Data safety answers, and the permission/foreground-service declarations in Play Console.
- Sign the AAB with the official upload key; check 16 KB page compatibility and Play's device compatibility report for the final uploaded bundle.
- Install through an internal test track and test onboarding, theme contrast, blocking, feed interruption, reminders, data deletion/PIN removal, update behavior, and an offline launch on physical Android 9 and current Android devices. Device-owner controls need a disposable dedicated device.
- If the publisher account is a newly created personal Play account, complete the required closed test before requesting production access.

These are submission steps and review decisions; a successful local build does not imply Play policy approval.

## Official requirements

- [Target API level](https://support.google.com/googleplay/android-developer/answer/11926878)
- [Android App Bundles](https://support.google.com/googleplay/android-developer/answer/9844279)
- [Accessibility API policy](https://support.google.com/googleplay/android-developer/answer/10964491)
- [Foreground service declaration](https://support.google.com/googleplay/android-developer/answer/13392821)
- [User data and privacy policy](https://support.google.com/googleplay/android-developer/answer/18265346)
- [16 KB page sizes](https://developer.android.com/guide/practices/page-sizes)
- [Closed testing for new personal accounts](https://support.google.com/googleplay/android-developer/answer/14151465)
