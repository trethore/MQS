# MQS Web UI

This subproject of MQS contains the Graphene-powered web UI for the mod.

It is built with Vite, React, shadcn/ui, and Tailwind CSS v4.

The application is a client-side SPA (Single Page Application) with no server-side rendering.
It is built as static files and loaded by the mod through Graphene/JCEF.

## Testing & Verification

- Do not run npm commands yourself; instead provide the exact command for the user to execute.
- Encourage running `npm run build:dev` for development builds and `npm run build` for production builds.
- Use `node --check <file>` to validate syntax.
- Document manual validation steps and remaining risks before completing work.
