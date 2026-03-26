# MQS Web UI

This subproject of MQS contains the Graphene-powered web UI for the mod.

It is built with Vite, React, shadcn/ui, and Tailwind CSS v4.

The application is a client-side SPA (Single Page Application) with no server-side rendering.
It is built as static files and loaded by the mod through Graphene/JCEF.

## Commands

- `npm run lint`: Checks for code quality issues.
- `npm run format`: Formats the codebase according to the defined prettier style.
- `npm run build:dev`: Builds the app and copies files in `out/` so graphene can load them.
- `npm run build:prod`: Builds the app and copies files in `src/client/resources/assets/myqolscripts/web/` for production use by graphene.
