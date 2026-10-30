# FocuLY — glass interface

FocuLY v1.0.1 (Android build 128) uses a scenic backdrop, frosted blue-grey chrome, fine light edges, and white text. Matching floating bars and focus actions soften the app content underneath while retaining the logo, corrected cat illustrations, three-icon dock, profile/settings placement, and direct blocking flows.

## Shared material

- `GlassBackdrop` draws a bundled misty lake behind the whole Compose interface, with a blue tonal veil. Android 12+ adds a background-only blur; earlier Android versions retain the soft-focus asset and translucent materials.
- `GlassSurface` supplies consistent rounded clipping, a subtle shaded fill, and a diagonal highlight border. Cards, summaries, target/mode choices, illustration badges, onboarding, and fixed editor footers share it.
- Material dialogs use coordinated slate surfaces; selection sheets use translucent slate containers. Inputs, switches, chips, and chart accents share the same blue/white palette.
- The Setup/editor action uses the same frosted material as the bars, with sharp labels, a fine highlight rim, and a restrained press response. It floats over scrolling content, with summary information inside the capsule and sufficient trailing space for the final controls. Home starts directly with block shortcuts and rhythm profiles, without a greeting, Make room card, or separate Start focus action. Other primary actions retain their milky fill. Disabled actions remain visibly different and cannot be clicked.
- The dock is a separate overlay inside the bottom safe area, with a maximum 360 dp width, 72 dp capsule height, low-opacity tint, fine rim, soft shadow, and open space around it. It still has exactly Home, Rules, and Insights. Settings and profile remain in the top header. Scrolling cards pass visibly behind it; the shared page's trailing inset lets its last control scroll clear of the capsule.
- Its selected pill slides over 180 ms using a placement-phase offset rather than recomposing the dock every animation frame. Reduce motion snaps directly; haptic feedback follows its existing preference. Each destination exposes a selected tab role and a 56 dp-high target. Repeated taps preserve position, and usage readings are shared across tabs.
- The 64 dp header shares the dock's glass edges and open margins. Home, Rules, Insights, Settings, Setup, and Block setup scroll behind it, with leading scroll space to place the first control below the header. Setup and Block setup use the dock's lighter tint for a see-through finish. The system status area stays clear of scrolling app text; other editor layouts retain a safe top inset. Profile and Settings keep their root positions; nested screens use Back.
- Settings replaces the System/Light/Dark chip row with a 48 dp sun/moon switch in the header. A selected dark-mode state is exposed to accessibility. The whole screen's palette and backdrop blend over 440 ms without remounting content, losing text input, or moving scroll position; the icon crossfades. Reduce motion snaps instantly, and system animation preferences are respected.
- Home's two blocking shortcuts are illustration-only glass cards with larger centered cats. The entire card is a button, labeled “Block apps” or “Block websites” for screen readers, and opens the corresponding direct setup flow.
- Standard Android sans-serif makes headings and body text lighter; JetBrains Mono remains on timer displays, captions, and small data labels. The established type scale, 48 dp interaction minimums, scrolling content, and persistent editor actions remain.
- Light mode uses the softer landscape tint; Dark mode deepens the blue overlay. The background is static and introduces no continuous animation, including when Reduce motion is enabled.
- Fresh installations start in Light. Existing System preferences resolve once to the current Android light/dark appearance and are stored explicitly; later system appearance changes do not override the manual choice. The Settings credit begins with “Made” and keeps Veil linked to its website.

## Frosted blur and simpler flows

The bars and focus actions use [Haze](https://github.com/chrisbanes/haze/tree/1.6.10) to sample only registered FocuLY drawing layers, with a 24 dp blur, restrained noise, translucent blue tint, and a light rim. Background/scrolling content are registered separately so foreground icons and text remain sharp. The floating Setup action can blur its sibling scrolling form. Automatic input scaling reduces the blur's working resolution.

Real background blur is enabled on Android 12+; Android 9–11 retain the library's frosted scrim fallback rather than forcing its experimental older-device renderer. This is a liquid-glass-inspired treatment, not Apple's private material or a refraction shader. No foreign app/window is sampled, captured, or transmitted. Haze is pinned to a version compatible with the project's existing Compose/Kotlin stack; its Apache license is bundled in the APK assets.

Settings has one Permissions dropdown with direct Android access controls, one Device protection entry for policies/PIN, a Reminders group, and appearance/privacy controls. The welcome replay and informational Essential access row are removed; essential-app exceptions remain enforced. First-time onboarding and recovery setup are retained where needed to finish a pending blocking action.

Page headings use sentence case, including Setup and Settings. Setup names the selected profile once instead of repeating Home's four-preset strip. Home's rhythm art has a 124 dp-high area (formerly 64 dp) within larger cards; titles and durations keep room to grow with text size.

Block setup centers its selection/mode summary and ending on separate wrapping lines, rather than squeezing all metadata into one left-aligned caption. Profile opens with an expanded About you card: name, local profile-photo controls, intention, and goal. Create profile and saved/default profiles follow it. Photos use the system image picker without storage access, are normalized to local PNGs, and appear in the root avatar. Removing or replacing blocking artwork preserves the saved profile photo, and vice versa. Images remain in private, non-backed-up storage; data exports retain references rather than embedding either image.

## Native blocking and widget

Normal, Resistant, and Strict accessibility overlays use the same bundled landscape, blue shading, glass edges, light text, milky return button, and corrected cat artwork. Their rendering uses Android Canvas and drawables, including Android 9–11. The landscape stays behind the scrollable warning content. No other app's pixels are captured or blurred.

The home-screen widget also gains a blue glass-style gradient, light border, and white text. Android owns the system permission screens, notification styling, and launcher widgets' outer presentation.

Only presentation changes: consent, resistance waits, override eligibility, expiry, essential access, administrator policies, saved data, and the package/signing identity keep their existing behavior. No new permission or network access is introduced.

## Background asset

`app/src/main/res/drawable-nodpi/glass_lake.png` is an opaque portrait background generated with built-in imagegen. The user's image supplied the design direction; its app mockups, logos, text, and usage statistics were not copied into the asset. The exact landscape prompt is recorded under `backgrounds` in [illustration-prompts.json](illustration-prompts.json). The image is bundled locally, with no runtime download.

## Review

Current development captures are in the [dock gallery](screenshots/dock/README.md) and [personalization gallery](screenshots/personalization/README.md). Representative [block setup](screenshots/block-setup.jpg) and [native interruption](screenshots/blocking.jpg) captures show the shared glass material. Superseded galleries remain in Git history.

Use an isolated emulator for `AppTest`, `DirectBlockTest`, `AdminPinTest`, and `ProtectionScenarioTest`: they reset local app data. The opt-in `ScreenshotGalleryTest` requires `-e gallery true` and captures onboarding, editors, settings, native warnings, dark mode, keyboard use, and 130% text scaling. Physical OnePlus testing and manual screen-reader review remain separate.
