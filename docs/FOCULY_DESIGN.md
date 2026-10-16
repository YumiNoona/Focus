# FocuLY visual direction

The current development interface follows the user's newer glassmorphism reference. See [the glass design guide](GLASS_DESIGN.md) for the shared scenic background, translucent controls, updated typography, and screenshots. The pastel direction below documents the earlier v1.2.0 design and the retained mascot system.

FocuLY uses the user's original `public/logo.png` for its launcher icon and in-app wordmark. The raster is copied unchanged into `app/src/main/res/drawable-nodpi/foculy_logo.png`. The Android package, saved data, administrator component, and signing certificate remain unchanged for updates from Focus.

## Four onboarding screens

1. **Less scroll. More life.** Blush card with a cat putting its phone aside.
2. **Find your rhythm.** Soft lime card with a cat reading at a little desk.
3. **Keep your boundary.** Peach card with a cat holding a shield.
4. **Make space for focus.** A focused permission checklist, with consent and the optional timer-only route.

The first three screens use a circular Next action. Four progress dashes sit beside it at the bottom; the fourth screen offers a circular finish action. System Back can revisit earlier screens, but there is no visible Back or Skip intro control. The intro is saveable through activity recreation and can be replayed from Settings → Welcome to FocuLY. Completing a replay does not clear rules or history.

## Visual system

- Warm paper `#FBFAF5`, charcoal `#202520`, blush `#F8DED9`, lime `#F0F7D6`, peach `#FCE6D1`, and lilac `#EAE3F7`.
- Black ink drawings, white cat silhouettes, sparing lilac/peach accents, and true transparent backgrounds.
- JetBrains Mono throughout the current interface; the existing text scale and 48 dp minimum interaction targets.
- Rounded story cards, 20–26 dp padding, concise copy, segmented progress, and a persistent primary action. Long content scrolls rather than clipping on smaller screens or with larger type.
- Doodles also appear in Home, active sessions, app/web/schedule empty states, first-session insights, protection health, the checklist, and native blocking overlays.
- Dark mode retains readable pastel illustration cards with charcoal text, paired with dark surrounding surfaces. Controls still reflect the selected mode and actual permission state.
- Three dock icons remain Home, Rules, Insights; profile and settings remain in the header.

## Illustration assets and prompts

Created with the built-in imagegen tool, using the user's provided pastel onboarding image as a style reference. No account or login flow from that reference was added. The logo was not regenerated or edited.

| Asset in `app/src/main/res/drawable-nodpi/` | Final prompt scene |
| --- | --- |
| `cat_pause.png` | A relaxed white cat gently pushes a smartphone face down with one paw, with a crescent and two small sparkles. |
| `cat_study.png` | The same rounded white cat sits at a little desk reading an open book, beside a pencil and tiny potted leaf. |
| `cat_guard.png` | The same cat hugs a simple chunky shield with a heart-shaped keyhole, with two decorative sparkles. |
| `cat_reminder.png` | A cat raises a gentle paw beside a small warning triangle. Normal interruptions and protection reminders. |
| `cat_patience.png` | A cat waits beside a lilac hourglass. Resistant interruptions and pause dialogs. |
| `cat_boundary.png` | A cat rests its paws on a closed padlock. Strict interruptions and locked rules. |
| `cat_focus.png` | A cat waters a sprouting plant. Attention growing during an active focus session. |
| `cat_break.png` | A cat stretches on a lilac cushion beside a cup of tea. Retained for legacy artwork compatibility. |
| `cat_complete.png` | A cat hugs a peach star with small confetti. Completed sessions only. |
| `cat_choose.png` | A cat taps one of two large choice tiles. Direct app and website selection. |
| `cat_feeds.png` | A cat rolls up a lilac film strip beside a face-down phone. Home and selective feed setup. |
| `cat_routine.png` | A cat marks a blank calendar with a peach pencil. Routine setup and its empty state. |
| `cat_tune.png` | A cat adjusts two chunky sliders. Visible appearance controls. |

Shared prompt: “Transparent raster doodle for FocuLY. Confident slightly wobbly black ink, simple white-filled forms, sparse black solid patches, and tiny motion lines. A charming white cat with triangular ears, dot eyes, whiskers, rounded body, and curled tail. Tiny lilac and peach accent patches. Complete centered illustration, generous transparent margins, square canvas. No text, logo, UI panel, paper background, gradients, 3D rendering, or watermark.”

The files preserve generated alpha and are packaged locally; no runtime image downloads are used.

The complete prompt set is stored in [illustration-prompts.json](illustration-prompts.json).

### Anatomy corrections — 16 October 2026

The selection (`cat_choose.png`), calendar (`cat_routine.png`), and plant-watering (`cat_focus.png`) scenes were edited with built-in imagegen to remove redundant forelegs. A follow-up adds the second visible back leg to the selection and watering cats, giving each four visible paws while preserving their existing foreleg poses. The calendar pose retains one visible folded hind paw and an occluded hind leg. Existing asset names retain all in-app references. Transparent backgrounds, props, palette, and character remain consistent. Exact edit prompts, including the follow-up, are recorded in the JSON above. Screenshots dated 15 October show the preceding artwork.

## Simpler paths — 15 October 2026

Home offers Start focus, Block apps, Block websites, and Shorts & Reels. The dock remains exactly three destinations. An app shortcut opens block setup, automatically presenting the shared selection sheet when there are no selected apps. Websites can be entered directly into block setup. Feed switches are visible together, without a Rules detour.

Focus setup exposes Apps and Websites as summary cards. Each opens its selection sheet immediately; changing a selection switches from the shared saved list to a custom scope automatically. Search, selected counts, clear/select-all, and a persistent Done button stay in the app sheet. Start/Save and block confirmation remain fixed beneath scrolling options. Adding a valid website dismisses its keyboard so duration and resistance choices are reachable immediately.

Protection setup returns to the screen that opened it, preserving the draft. Settings has one protection-setup entry and visible appearance choices. Advanced privacy, device management, and daily app limits remain available. Navigation simplification does not remove Strict acknowledgement, resistance waits, permission consent, or Android limitation disclosures.

The four additional doodles were generated individually with built-in imagegen using `cat_pause.png` as the character reference. Full prompts are recorded in the JSON above; original alpha and the supplied logo are preserved. The four scenes are also available in the profile illustration selector.

The six new scenes use `cat_pause.png` as their character/style reference through built-in imagegen. Original onboarding art and the supplied logo remain unchanged. Art is decorative for accessibility, adds no touch actions, and never replaces the actual mode, expiry, service status, or custom message. Native overlay heights remain 86 dp for Normal and 120 dp for other modes; scrolling retains access to actions and emergency essentials. Pastel badges preserve dark-theme contrast.
