# FocuLY — glass interface

The v1.3.0 design follows the user's glassmorphism reference: a calm scenic backdrop, translucent blue-grey panels, fine light edges, white text, and pale primary actions. The existing FocuLY logo, corrected cat illustrations, three-icon dock, profile/settings placement, and direct blocking flows are retained.

## Shared material

- `GlassBackdrop` draws a bundled misty lake behind the whole Compose interface, with a blue tonal veil. Android 12+ adds a background-only blur; earlier Android versions retain the soft-focus asset and translucent materials.
- `GlassSurface` supplies consistent rounded clipping, a subtle shaded fill, and a diagonal highlight border. Cards, summaries, target/mode choices, illustration badges, onboarding, and fixed editor footers share it.
- Material dialogs use coordinated slate surfaces; selection sheets use translucent slate containers. Inputs, switches, chips, and chart accents share the same blue/white palette.
- Primary actions use a milky fill, blue text, rounded corners, and a fine edge. Disabled actions remain visibly different. Selected tabs/modes retain both semantic selection state and a distinct blue fill.
- The dock floats inside the bottom safe area and still has exactly Home, Rules, and Insights. Settings and profile remain in the top header.
- Home's two blocking shortcuts are illustration-only glass cards with larger centered cats. The entire card is a button, labeled “Block apps” or “Block websites” for screen readers, and opens the corresponding direct setup flow.
- Standard Android sans-serif makes headings and body text lighter; JetBrains Mono remains on timer displays, captions, and small data labels. The established type scale, 48 dp interaction minimums, scrolling content, and persistent editor actions remain.
- Light mode uses the softer landscape tint; Dark mode deepens the blue overlay. The background is static and introduces no continuous animation, including when Reduce motion is enabled.

## Native blocking and widget

Normal, Resistant, and Strict accessibility overlays use the same bundled landscape, blue shading, glass edges, light text, milky return button, and corrected cat artwork. Their rendering uses Android Canvas and drawables, including Android 9–11. The landscape stays behind the scrollable warning content. No other app's pixels are captured or blurred.

The home-screen widget also gains a blue glass-style gradient, light border, and white text. Android owns the system permission screens, notification styling, and launcher widgets' outer presentation.

Only presentation changes: consent, resistance waits, override eligibility, expiry, essential access, administrator policies, saved data, and the package/signing identity keep their existing behavior. No new permission or network access is introduced.

## Background asset

`app/src/main/res/drawable-nodpi/glass_lake.png` is an opaque portrait background generated with built-in imagegen. The user's image supplied the design direction; its app mockups, logos, text, and usage statistics were not copied into the asset. The exact landscape prompt is recorded under `backgrounds` in [illustration-prompts.json](illustration-prompts.json). The image is bundled locally, with no runtime download.

## Review

Actual development captures are in the [glass screenshot gallery](screenshots/glass/README.md). Earlier pastel screenshots are retained as historical design records.

Use an isolated emulator for `AppTest`, `DirectBlockTest`, `AdminPinTest`, and `ProtectionScenarioTest`: they reset local app data. The opt-in `ScreenshotGalleryTest` requires `-e gallery true` and captures onboarding, editors, settings, native warnings, dark mode, keyboard use, and 130% text scaling. Physical OnePlus testing and manual screen-reader review remain separate.
