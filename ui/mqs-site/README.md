# MQS UI Site (Astro + shadcn + Tailwind)

This project is the web UI source for MQS.

- Source lives in `ui/mqs-site/`.
- Graphene loads `pages/index.html` from MQS assets.
- Build output is copied into `src/client/resources/assets/myqolscripts/pages/`.

## How MQS and Graphene use this

At runtime, MQS opens this classpath URL:

`asset(myqolscripts, "pages/index.html")`

So the generated website must exist under:

`src/client/resources/assets/myqolscripts/pages/`

The publish script handles this copy step after Astro build.

## Commands

From `ui/mqs-site/`:

```sh
npm install
```

```sh
npm run dev
```

Starts local Astro dev server (default: `http://localhost:4321`).

```sh
npm run build:graphene
```

Runs:
1. `astro build` (static output)
2. `scripts/publish-pages.mjs` (copies `dist/` to MQS assets)
3. Rewrites absolute links to relative links so Graphene can resolve CSS, JS, and page navigation correctly.

## Dependencies

- Astro: Static site generator.
- tailwindcss: Utility-first CSS framework.
- shadcn: Component library built on top of Tailwind CSS.

See `package.json` for exact versions.

### Shadcn Usage

Add new components from `ui/mqs-site/`:

```sh
npx shadcn@latest add button card
```

Generated components are placed under `src/components/ui/`.

## Important behavior

- Graphene does not always resolve directory routes like `/markdown-page/` as `index.html`.
- The publish step rewrites links to explicit files (example: `markdown-page/index.html`) to avoid blank pages.
- Re-run `npm run build:graphene` every time you want updated UI inside the mod resources.

