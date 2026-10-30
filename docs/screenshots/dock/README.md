# Floating glass header and dock — 30 October 2026

Actual Android 15 emulator captures from the signed production FocuLY v1.0.0 (Android build 127). Top and bottom capsules float over scrolling content, with open margins and scroll space for the first/last actions. The header's stronger blue tint keeps controls readable over pale cards. There are exactly three dock destinations, with Profile and Settings in the root header.

![See-through chrome, Settings theme toggle, and dark mode](overview.jpg)

| Screen | Capture |
| --- | --- |
| Home | [View](dock-01-home.jpg) |
| Scrolling behind the glass | [View](dock-02-scroll-through.jpg) |
| Rules selected | [View](dock-03-rules.jpg) |
| Insights selected | [View](dock-04-insights.jpg) |
| Dark Home, Reduce motion enabled | [View](dock-05-dark-home.jpg) |
| Dark scrolling content | [View](dock-06-dark-scroll.jpg) |
| 130% text size | [View](dock-07-large-text.jpg) |
| Final profile actions scrolled clear of the dock | [View](dock-08-last-actions.jpg) |
| Light Settings / sun toggle | [View](settings-light.jpg) |
| Dark Settings / moon toggle | [View](settings-dark.jpg) |
| Appearance preferences without theme chips | [View](settings-appearance.jpg) |
| Capitalized Made footer and Veil link | [View](settings-footer.jpg) |

Captured directly from the optimized, non-debuggable production app on a disposable emulator. Scroll Test is an installed fixture; captures contain no fabricated usage statistics. JPEGs are resized to 900 px wide, and the overview combines three unchanged captures. The last profile is physically opened at 130% text size. Settings checks confirm persistent light/dark state, unchanged scroll position during switching, removal of the old System/Light/Dark chips, reduced motion, and status-area clearance.

The selected dock pill slides over 180 ms. Settings blends the existing screen's palette over 440 ms without remounting its content; Reduce motion makes both transitions instant. A real Strict interruption is checked before/after changing theme and at expiry. The app does not capture or blur other apps' pixels. [Design details](../../DESIGN.md) · [Validation](../../VALIDATION.md).
