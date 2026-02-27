# MQS UI

MQS-UI is a subproject of My QOL Scripts (MQS) that provides a web-based user interface for interacting with the mod’s features.  
Once compiled, the source code is served by the Graphene library as an in-game UI.

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
│   ├── bridge/
│   │   ├── core/                   # Graphene bridge access, transport helpers, and shared bridge errors.
│   │   ├── contracts/              # Channel constants, payload types, and parsers per Java bridge.
│   │   └── services/               # Bridge service layer used by hooks/components.
│   ├── components/
│   │   ├── astro/                  # Astro-only reusable components.
│   │   ├── react/                  # React reusable components.
│   │   └── ui/                     # shadcn/ui reusable components.
│   ├── hooks/                      # Feature controllers (state + side effects) consumed by components.
│   ├── layouts/                    # Shared page shell/layout.
│   ├── lib/                        # Generic UI utilities.
│   ├── pages/                      # Astro pages (build to static HTML files).
│   └── styles/
│       └── global.css              # Global styles and Tailwind imports.
├── dist/                           # Astro build output before Graphene copy step.
└── out/                            # Dev copy target consumed by Graphene in local development.
```

## Project Insights

- The UI is built as static files and copied into the Graphene asset folders.
- The runtime target is Graphene WebView (file-based loading), not a traditional web server.
- Ensure pages and assets are robust and fully functional in static-file environments.

## General Coding Conventions

- Keep components small and composable; prefer extracting reusable UI blocks instead of duplicating markup.
- Start with semantic HTML, then use Tailwind utilities for styling.
- Use relative sizing units (`rem`, `em`, `%`, `vh`, `vw`) for layout and typography whenever possible.
- Keep spacing, border radii, shadows, and font sizes consistent with existing design tokens and utility patterns.
- Avoid inline styles unless the values are dynamic and cannot be represented with utilities.
- Keep class lists readable by organizing utilities in this order: layout → spacing → typography → visual state.
- Use CSS variables for theme-related values that may be reused or adjusted globally.
- Avoid hardcoding absolute root links in page content when a relative or static-safe path is sufficient.
- When using shadcn/ui components, do not modify vendor components directly unless absolutely necessary; instead, wrap or
  compose them within your own components to implement project-specific behavior.

## Routing And Static Output Conventions

- Treat `src/pages` output as static HTML consumed from disk.
- Use predictable page paths and ensure cross-page links work in file mode.
- If introducing custom link patterns, ensure they remain compatible with `scripts/copy-build.js` URL rewriting.

## Dependencies

- Astro
- React
- Tailwind CSS
- shadcn/ui components (when present in the codebase)

Refer to `package.json` for the exact versions.

## Testing & Verification

- Do not run `npm run*` commands yourself; provide commands for the user to run such as linting, format or building.
- Recommend `npm run build:graphene:dev` after UI changes.
- For release validation, recommend `npm run build:graphene:prod` and manual navigation checks in Graphene.
