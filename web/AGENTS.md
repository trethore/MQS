# MQS Web UI

This subproject of MQS contains the Graphene-powered web UI for the mod.

It is built with Vite, React, shadcn/ui, and Tailwind CSS v4.

The application is a client-side SPA (Single Page Application) with no server-side rendering.
It is built as static files and loaded by the mod through Graphene/JCEF.

## Project Overview & Structure

The `web/` project should stay organized around clear UI, page, and bridge boundaries:

```text
web/
├── src/
│   ├── app/                              # App bootstrap, app shell composition, lightweight routing.
│   ├── pages/                            # Route-level pages.
│   ├── components/
│   │   ├── ui/                           # shadcn/ui primitives and generated UI building blocks only.
│   │   ├── layout/                       # App shell, sidebar, headers, panels, and structural components.
│   │   └── shared/                       # Reusable MQS-specific components shared across features.
│   ├── hooks/                            # Reusable React hooks for state, effects, and UI coordination.
│   ├── bridge/
│   │   ├── contracts/                    # Channel names, request/response types, and event payload types.
│   │   ├── core/                         # Graphene client, transport, serialization, and bridge errors.
│   │   └── services/                     # Domain-oriented bridge APIs used by hooks and pages.
│   ├── lib/                              # Small shared helpers such as className utilities.
│   ├── index.css                         # Tailwind v4 import, theme tokens, and base styles.
│   └── main.tsx
├── public/
├── scripts/
├── components.json                       # shadcn/ui CLI configuration.
├── vite.config.ts
└── package.json
```

## Testing & Verification

- Do not run npm commands yourself; instead provide the exact command for the user to execute.
- Encourage running `npm run build:dev` for development builds and `npm run build` for production builds.
- Use `node --check <file>` to validate syntax.
- Document manual validation steps and remaining risks before completing work.
