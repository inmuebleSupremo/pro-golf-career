<!-- BEGIN:nextjs-agent-rules -->

# This is NOT the Next.js you know

This version has breaking changes — APIs, conventions, and file structure may all differ from your training data. Read the relevant guide in `node_modules/next/dist/docs/` before writing any code. Heed deprecation notices.
<!-- END:nextjs-agent-rules -->

---

# frontend/AGENTS.md

The player-facing UI. Read the root [`AGENTS.md`](../AGENTS.md) first.

## Stack
- **Next.js 16 (App Router) + React 19 + TypeScript**, package manager **pnpm**.
- **Tailwind CSS v4**, shadcn/ui foundation on **Radix UI** primitives (always restyled — never ship default
  shadcn look).
- **TanStack Query** for server state, **React Hook Form + Zod** for forms, **Motion** for animation,
  **lucide-react** for icons, **date-fns** for dates.
- **graphql** + **graphql-codegen** (client preset) — typed operations generated from the backend schema.

## Architecture
- **BFF pattern.** The browser never calls Spring directly. Route handlers in `src/app/api/` (`/api/graphql`,
  `/api/auth/*`) call the backend server-side over `BACKEND_INTERNAL_URL`, attaching the session from httpOnly
  cookies. `/api/graphql` is the authoritative auth boundary.
- **`src/proxy.ts`** is Next 16's renamed middleware — a UX-only cookie-presence redirect (login gate). It does
  **not** validate tokens; that's the API route's job. Public paths: `/`, `/login`, `/register`, `/scenes`.
- **Codegen**: `pnpm codegen` reads `../backend/src/main/resources/graphql/schema.graphqls` and writes typed
  operations to `src/lib/graphql/generated/` (gitignored). Runs automatically before `dev`/`build`/`typecheck`.
  Consume via the generated `graphql()` function. `Long` scalars are typed as `string` (avoid JS precision loss).

## Layout
- `src/app/` — routes. `(auth)/` = login/register; `(app)/` = the game, keyed by `career/[id]/…`
  (hub, play, calendar, finances, equipment, staff, achievements, career-records, rankings, offseason, …).
- `src/components/` — grouped by area (`app`, `career`, `play`, `manage`, `onboarding`, `offseason`,
  `calendar`, `command`, `ui`, `auth`).
- `src/lib/` — `api`, `auth`, `graphql`, plus per-area logic (`career`, `manage`, `onboarding`, `play`).

## Design system — read before any UI work
[`../docs/frontend/`](../docs/frontend/) is an enforced house style, not suggestions:
- `technology.md` — approved libraries (don't add deps that React/Next/an approved lib already covers).
- `boundaries.md` — non-negotiable rules (reuse components, tokens-only, Server Components first, no glass/neon/
  heavy-gradient/decorative-motion).
- `design-tokens.md` — the token vocabulary; never hardcode colour/spacing/type/shadow/radius.
- `references.md` — visual references.

## Commands
```bash
pnpm install
pnpm dev         # :3000 (codegen runs first) — start the backend first
pnpm typecheck   # tsc --noEmit
pnpm lint
pnpm build
pnpm codegen     # regenerate GraphQL types after a schema change
```
