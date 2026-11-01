# FocuLY web

Marketing site for the FocuLY Android app.

## Local development

```bash
npm install
npm run dev
```

## Production build

```bash
npm run build
npm run preview
```

## Deploy to Vercel

Import this repository in Vercel and set **Root Directory** to `web`. Vercel will use `vercel.json`, run `npm run build`, and publish `dist`.

Point `focus.venusapp.in` at this deployment before submitting FocuLY to Play. The app's Settings link and the store listing use `https://focus.venusapp.in/privacy`; verify that public URL returns the policy page after deployment.

Alternatively, from this directory with the Vercel CLI installed:

```bash
vercel
```
