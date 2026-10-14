# FocuLY visual direction

FocuLY uses the user's original `public/logo.png` for its launcher icon and in-app wordmark. The raster is copied unchanged into `app/src/main/res/drawable-nodpi/foculy_logo.png`. The Android package, saved data, administrator component, and signing certificate remain unchanged for updates from Focus.

## Four onboarding screens

1. **Less scroll. More life.** Blush card with a cat putting its phone aside.
2. **Find your rhythm.** Soft lime card with a cat reading at a little desk.
3. **Keep your boundary.** Peach card with a cat holding a shield.
4. **Ready when you are.** The live permission checklist, with consent and the optional timer-only route.

Next, Back, system Back, and Skip intro preserve a deliberate flow. Skip leads to the permission checklist. The intro is saveable through activity recreation and can be replayed from Settings → Welcome to FocuLY. Completing a replay does not clear rules or history.

## Visual system

- Warm paper `#FBFAF5`, charcoal `#202520`, blush `#F8DED9`, lime `#F0F7D6`, peach `#FCE6D1`, and lilac `#EAE3F7`.
- Black ink drawings, white cat silhouettes, sparing lilac/peach accents, and true transparent backgrounds.
- JetBrains Mono headings and labels; system body text; the existing text scale and 48 dp minimum interaction targets.
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

Shared prompt: “Transparent raster doodle for FocuLY. Confident slightly wobbly black ink, simple white-filled forms, sparse black solid patches, and tiny motion lines. A charming white cat with triangular ears, dot eyes, whiskers, rounded body, and curled tail. Tiny lilac and peach accent patches. Complete centered illustration, generous transparent margins, square canvas. No text, logo, UI panel, paper background, gradients, 3D rendering, or watermark.”

The files preserve generated alpha and are packaged locally; no runtime image downloads are used.

The complete prompt set is stored in [illustration-prompts.json](illustration-prompts.json).
