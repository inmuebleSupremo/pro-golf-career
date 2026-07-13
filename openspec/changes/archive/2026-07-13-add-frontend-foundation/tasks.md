## 1. Project scaffold & tooling

- [x] 1.1 Create the `frontend/` Next.js (App Router) + React + TypeScript project managed with pnpm; confirm the dev server compiles and serves the root route
- [x] 1.2 Configure TypeScript (strict), ESLint, and formatting; add `pnpm` scripts for `dev`, `build`, `lint`, `typecheck`, `format`
- [x] 1.3 Add Tailwind CSS v4 and wire it into the App Router global stylesheet
- [x] 1.4 Add a `.gitignore` for the frontend (node_modules, `.next`, generated GraphQL output) and confirm the package is standalone (not coupled to the Java build)

## 2. Design-token layer (via impeccable)

- [x] 2.1 Author concrete token values with the `impeccable` skill (colour roles, typography, spacing, sizing, radius, borders, shadows, motion, opacity, z-index, breakpoints) in the editorial/premium direction of `docs/frontend/REFERENCES.md`; present for user approval
- [x] 2.2 Define approved tokens as CSS custom properties and expose them through Tailwind v4 `@theme` so utilities resolve to tokens
- [x] 2.3 Set up typography (fonts via Next font) and the base/theme layer on the root layout; verify no raw visual values are needed by base styles
- [x] 2.4 Confirm the impeccable design hook runs clean on the token/base files (no hardcoded-value or contrast findings)

## 3. Component baseline (shadcn/Radix, re-skinned)

- [x] 3.1 Initialise shadcn/ui on Radix and add only the primitives the slice needs (e.g. button, input, label/form field)
- [x] 3.2 Re-skin each primitive to consume the design tokens; remove all default shadcn styling
- [x] 3.3 Verify keyboard reachability and visible focus states on every added primitive

## 4. Shared layout & app structure

- [x] 4.1 Create the root layout carrying the token theme, fonts, and shared page structure
- [x] 4.2 Establish the route groups: public `(auth)/` (login, register) and protected `(app)/` (authenticated shell), plus `app/api/*` for BFF route handlers and a `lib/` for query/GraphQL
- [x] 4.3 Add a shared authenticated page structure/layout used by protected routes (Server Component by default)

## 5. Data-access layer (TanStack Query + typed GraphQL)

- [x] 5.1 Add TanStack Query and a client provider at the app root
- [x] 5.2 Configure `@graphql-codegen` to generate operation and result types from `backend/src/main/resources/graphql/schema.graphqls`; add a `codegen` pnpm script
- [x] 5.3 Author the `listSaves` operation and generate its typed client/hook; confirm a deliberately-wrong field fails type-check
- [x] 5.4 Implement the same-origin `/api/graphql` BFF route handler (the authoritative auth boundary) that attaches the access token server-side and forwards to the backend `/graphql`, driving refresh-on-401 (see 6.3)
- [x] 5.5 Mark authenticated routes and the `listSaves` read as dynamic / `no-store` (per D6) so per-user data never enters a static or shared cache

## 6. Authentication (BFF, httpOnly cookies)

- [x] 6.1 Implement the `/api/auth/login` and `/api/auth/register` BFF route handlers calling the backend REST `/auth/*` endpoints
- [x] 6.2 Store the returned tokens per the fixed cookie policy (D1): `pg_access` and `pg_refresh`, both `HttpOnly; Secure; SameSite=Lax; Path=/`, `Max-Age` mirroring the backend TTLs (access 900s, refresh 1209600s); expose a session-state read for server components
- [x] 6.3 Implement silent refresh triggered **only on HTTP 401** from the backend (not on a GraphQL 200-with-errors): call `/auth/refresh` with the refresh cookie, replace the access cookie, and retry once. Add per-instance single-flight coalescing as an optimisation (not required for correctness — the backend does not rotate refresh tokens)
- [x] 6.4 Implement `/api/auth/logout` to delete both cookies, perform no refresh afterwards, and signal the client to `queryClient.clear()` so TanStack Query stops serving authenticated data
- [x] 6.5 Add route protection as two non-overlapping layers (D5): the Next 16 **proxy** (`proxy.ts`, the renamed middleware) does a cookie-**presence** fast-redirect only (no validation/refresh); the protected `(app)/` server layout + `/api/graphql` handler are the authoritative boundary that validates/refreshes and redirects to `/login` on failure

## 7. Vertical slice screens

- [x] 7.1 Build the login screen (React Hook Form + Zod validation) posting to `/api/auth/login`; handle invalid-credentials and validation errors
- [x] 7.2 Build the register screen posting to `/api/auth/register`; handle username-taken and validation errors
- [x] 7.3 Build the authenticated saves-list screen reading `listSaves` via the typed TanStack Query hook, with loading, error, and empty states
- [x] 7.4 Wire post-login navigation into the authenticated area and a visible logout affordance

## 8. Verification

- [x] 8.1 Run typecheck, lint, and codegen clean
- [x] 8.2 Manually verify end to end against a running backend: register → login → view saves list (with populated and empty cases) → access-token expiry refreshes silently on 401 → session survives reload → logout returns to login and leaves no stale authenticated data (query cache cleared)
- [x] 8.3 Verify tokens are not readable from client JS/web storage (present only as httpOnly cookies)
- [x] 8.4 Confirm the impeccable design hook is clean across the new UI files; verify keyboard navigation, focus states, and reduced-motion behaviour
- [x] 8.5 Run `openspec validate add-frontend-foundation` and resolve any issues
