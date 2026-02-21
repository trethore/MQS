# MQS UI

This Astro project builds the Graphene web UI used by MQS.

## Overview

Here is the structure of the repository:

```text
ui/mqs-ui/
├── package.json                    # Scripts: dev/build/build:graphene:dev/build:graphene:prod.
├── astro.config.mjs                # Astro config (`build.format='file'`, `assetsPrefix='.'`, React + Tailwind Vite plugin).
├── scripts/
│   └── copy-build.js               # Copies dist output to Graphene target folders and rewrites absolute links.
├── public/                         # Static assets copied as-is.
├── src/
│   ├── components/
│   │   ├── astro/                  # Astro-only reusable components.
│   │   └── react/                  # React reusable components.
│   ├── layouts/                    # Shared page shell/layout.
│   ├── pages/                      # Astro pages (build to static HTML files).
│   └── styles/
│       └── global.css              # Global styles and Tailwind imports.
├── dist/                           # Astro build output before Graphene copy step.
└── out/                            # Dev copy target consumed by Graphene in local development.
```

## Project Notes

- The UI is built as static files and copied into Graphene asset folders.
- Runtime target is Graphene WebView (file-based loading), not a traditional web server.
- Keep pages and assets robust in static-file contexts.

## General Coding Conventions

- Keep components small and composable; prefer extracting reusable UI blocks over duplicating markup.
- Preserve existing behavior when refactoring; avoid replacing working components without a clear reason.
- Use TypeScript-friendly patterns in Astro/React files (typed props, explicit interfaces for public component APIs).
- Prefer semantic HTML first, then Tailwind utilities for styling.
- Use relative sizing units (`rem`, `em`, `%`, `vh`, `vw`) for layout and typography.
- Keep spacing, radii, shadows, and font sizes consistent with existing design tokens and utility patterns.
- Avoid inline styles unless values are dynamic and cannot be represented with utilities.
- Keep class lists readable: structure utilities by layout -> spacing -> typography -> visual state.
- Prefer CSS variables for theme-like values that may be reused or adjusted globally.
- Avoid hardcoding absolute root links in page content when a relative/static-safe path is practical.
- Keep files ASCII unless a file already uses non-ASCII content.
- Do not add comments unless a block is non-obvious and benefits from short clarification.

## Routing And Static Output Conventions

- Treat `src/pages` output as static HTML consumed from disk.
- Use predictable page paths and ensure cross-page links work in file mode.
- If introducing custom link patterns, ensure they remain compatible with `scripts/copy-build.js` URL rewriting.

## Dependencies

- Astro
- React
- Tailwind CSS
- shadcn/ui components (when present in the codebase)

See `package.json` for exact versions.

## Testing & Verification

- Do not run `npm run build*` commands yourself; provide commands for the user to run.
- Recommend `npm run build:graphene:dev` after UI changes.
- For release validation, recommend `npm run build:graphene:prod` and manual navigation checks in Graphene.
