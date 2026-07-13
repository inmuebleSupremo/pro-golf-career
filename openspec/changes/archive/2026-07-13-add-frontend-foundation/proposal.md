## Why

The game is fully playable through the engine and its GraphQL/REST API, but there is **no player-facing UI** — every decision is exercised through the service/API seam (per `docs/backend/player-experience.md`, item 8 is the one remaining gap). Step 9 begins the frontend. Before building any feature screens, we need a proven foundation: the approved stack wired end to end, a concrete design-token system, and the browser-to-backend data + auth path working through **one thin vertical slice**. Standing this up first de-risks every screen that follows and prevents architectural churn once feature work starts.

## What Changes

- Introduce a new top-level `frontend/` application: **Next.js (App Router) + React + TypeScript + pnpm**, per `docs/frontend/TECHNOLOGY.md`.
- Establish a **concrete design-token layer** (colour roles, typography scale, spacing, sizing, radius, borders, shadows, motion, opacity, z-index, breakpoints) realising the abstract categories in `docs/frontend/DESIGN_TOKENS.md`, in the editorial/premium direction of `docs/frontend/REFERENCES.md`. Concrete values authored via the `impeccable` skill for review. Wired through **Tailwind CSS v4** so all utilities resolve to tokens.
- Add a **re-skinned shadcn/ui baseline on Radix** — the minimum primitives the vertical slice needs (e.g. button, input, form field), customised to the token system; never default shadcn styling.
- Add the **server-state + typed data-access layer**: TanStack Query provider plus a GraphQL client whose types are generated (graphql-codegen) from `backend/src/main/resources/graphql/schema.graphqls`.
- Add **browser authentication** against the backend's REST `/auth` endpoints (register / login / refresh), including session persistence and protected routing. The backend returns JWTs in the response body and has **no CORS configuration**; the token-transport strategy (BFF/httpOnly-cookie vs client-held) is evaluated in `design.md`, which recommends a **Backend-For-Frontend** proxy so the browser stays same-origin.
- **Prove the whole stack with one vertical slice**: an unauthenticated user can log in and, once authenticated, view the list of saved games (`listSaves`). No feature breadth beyond this — onboarding, career hub, and play surfaces are out of scope for this change.
- Add frontend tooling: lint, typecheck, format, and codegen scripts; no CI wiring changes beyond the `frontend/` package.

## Capabilities

### New Capabilities
- `web-foundation`: The frontend application scaffold and design system — Next.js App Router project structure, the concrete design-token layer over Tailwind v4, the re-skinned shadcn/Radix component baseline, shared page-layout conventions, and the accessibility/Server-Components-first defaults every screen inherits.
- `web-authentication`: Browser-side authentication — register/login/logout flows against the backend REST `/auth` endpoints, session establishment and persistence, token transport (via the recommended BFF), refresh handling, and protection of authenticated routes.
- `web-data-access`: The server-state and API-access layer — the TanStack Query setup, the typed GraphQL client generated from the backend schema, the authenticated request path from the browser to `/graphql`, and the saves-list read (`listSaves`) that proves the layer end to end.

### Modified Capabilities
<!-- None. This change introduces the first web-layer capabilities; no backend/engine spec requirements change. -->

## Impact

- **New code**: a `frontend/` pnpm project (Next.js App Router app, token layer, shadcn components, TanStack Query + GraphQL client, auth + BFF route handlers, the login and saves-list screens).
- **New dependencies** (frontend only, all approved by `TECHNOLOGY.md` except build-time codegen): Next.js, React, Tailwind v4, shadcn/ui + Radix, TanStack Query, Lucide, plus `@graphql-codegen/*` (build-time type generation from the schema — justified under the dependency gate as type safety, not a runtime framework).
- **Backend**: no code changes required for the BFF path (the proxy calls the API server-side, sidestepping the missing CORS config). If a non-BFF path is chosen in design, the backend would need a CORS configuration — flagged as a decision, not a committed change.
- **Consumes**: backend REST `/auth` (register/login/refresh) and GraphQL `listSaves`; the GraphQL schema file as the codegen source of truth.
- **Docs**: governed by the four `docs/frontend/*` files; this change realises them without modifying them.
