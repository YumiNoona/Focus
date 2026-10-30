# Frosted header, dock, and focus actions — 30 October 2026

Actual Android 15 emulator captures from the signed production FocuLY v1.0.1 (Android build 128). Top and bottom capsules blur the app layers underneath, with sharp controls, open margins, and scroll space for the first/last actions. The header's stronger blue tint keeps controls readable over pale cards. There are exactly three dock destinations, with Profile and Settings in the root header.

![Frosted Home, floating focus action, and simpler Settings](overview.jpg)

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
| Four rhythm cards with larger cats | [View](rhythm-large.jpg) |
| Selected profile and floating Start focus | [View](focus-setup.jpg) |
| Final Setup control above the action at 130% text | [View](focus-setup-large-scrolled.jpg) |
| Expanded Permissions dropdown | [View](permissions-expanded.jpg) |

Captured directly from the optimized, non-debuggable production app on a disposable emulator. Scroll Test is an installed fixture; captures contain no fabricated usage statistics. JPEGs are resized to 900 px wide, and the overview combines three unchanged captures. The last profile and final Websites selector are physically opened at 130% text size. Settings checks confirm light/dark switching with unchanged scroll position, reduced motion, and status-area clearance. Permission controls now share one dropdown; Welcome replay and Essential access are absent.

The selected dock pill slides over 180 ms. Settings blends the existing screen's palette over 440 ms without remounting its content; Reduce motion makes both transitions instant. This iteration checks a real Normal interruption started by the new glass action and Strict's acknowledgement guard; the earlier Strict/theme/expiry check is recorded in validation. Real blur is enabled on Android 12+ with a frosted scrim fallback on older versions. The app does not capture or blur other apps' pixels. [Design details](../../DESIGN.md) · [Validation](../../VALIDATION.md).
