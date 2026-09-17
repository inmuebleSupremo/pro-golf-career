# Pro Golf Career — Frontend

The player-facing UI for Pro Golf Career: a Next.js 16 (App Router) app in TypeScript.

For architecture, conventions, and the enforced design system, see **[`AGENTS.md`](AGENTS.md)** and
**[`../docs/frontend/`](../docs/frontend/)**. Top-level project orientation is in the root
[`AGENTS.md`](../AGENTS.md).

## Quick start

```bash
pnpm install
pnpm dev        # http://localhost:3000
```

Start the [backend](../backend) first (default `http://localhost:8080`) so the app has an API. Configure the
backend URL via `BACKEND_INTERNAL_URL` — copy `.env.example` to `.env.local`.

## Scripts

| Command | Purpose |
|---|---|
| `pnpm dev` | Dev server (runs `codegen` first) |
| `pnpm build` / `pnpm start` | Production build / serve |
| `pnpm typecheck` | `tsc --noEmit` (runs `codegen` first) |
| `pnpm lint` | ESLint |
| `pnpm codegen` | Regenerate typed GraphQL operations from the backend schema |
| `pnpm format` / `pnpm format:check` | Prettier |

## How it talks to the backend

The browser never calls Spring directly. Next route handlers under `src/app/api/` act as a BFF, calling the
backend server-side and holding the session in httpOnly cookies. GraphQL types are generated from
`../backend/src/main/resources/graphql/schema.graphqls` — **re-run `pnpm codegen` after any schema change.**
