## Context

Step 9 delivers the first player-facing UI. The backend is complete and exposes:

- **REST auth** (`AuthController`, spec `authentication`): `POST /auth/register` → `{id, username}` (201); `POST /auth/login` → `{accessToken, refreshToken}`; `POST /auth/refresh` (body `{refreshToken}`) → `{accessToken}`.
- **JWT model** (`JwtService`, `SecurityConfig`): HS256 symmetric secret, `type` claim distinguishing `access` from `refresh`. Access TTL **900s (15 min)**, refresh TTL **1209600s (14 days)**. Tokens are returned **in the JSON body**, not as cookies. The API is a stateless OAuth2 resource server: `/auth/**`, `/actuator/health`, `/graphiql**` are public; **everything else (including `/graphql`) requires a Bearer access token**. CSRF is disabled; sessions are stateless.
- **No CORS configuration exists** in the backend. A browser calling `/graphql` or `/auth` from a different origin would be blocked by the browser's same-origin policy unless CORS is added.
- **GraphQL** (`schema.graphqls`): the whole read/write model, including `listSaves: [Save!]!` used by the vertical slice.

The four `docs/frontend/*` documents are the constitution: approved stack (Next.js App Router / React / TS / pnpm / Tailwind v4 / shadcn+Radix / TanStack Query / Motion / RHF / Zod / Lucide), token-only styling, editorial/premium restraint, Server-Components-first, accessibility mandatory, reuse-before-create.

This change stands up the foundation and proves it with **login → view saves list**. No other screens.

## Goals / Non-Goals

**Goals:**
- A running `frontend/` Next.js App Router app on the approved stack, type-clean.
- A concrete design-token layer over Tailwind v4 realising all `DESIGN_TOKENS.md` categories, in the `REFERENCES.md` direction, authored via the `impeccable` skill for review.
- A re-skinned shadcn/Radix baseline limited to what the slice needs.
- TanStack Query + a **schema-typed** GraphQL client (codegen from the backend schema).
- Working browser auth (register/login/logout, session persistence, silent refresh, route protection) with authentication credentials never exposed to JavaScript — browser authentication is cookie-based through the BFF.
- One vertical slice — authenticated saves list via `listSaves` — proving auth + data-access + tokens + UI together.

**Non-Goals:**
- Any feature screen beyond login/register and the saves list (onboarding, career hub, play surfaces, management panels).
- Backend changes. The chosen path must work against the backend **as-is**; CORS is not added here.
- Real-time/subscriptions, SSR data-prefetch optimisation, i18n, deployment/Docker (later steps).
- A complete component library — only the primitives the slice requires.

## Decisions

### D1 — Token transport: Backend-For-Frontend (BFF) with httpOnly cookies

**Decision:** The browser never talks to the Spring backend directly. Next.js **Route Handlers** act as a same-origin BFF: `/api/auth/login`, `/api/auth/register`, `/api/auth/logout`, and `/api/graphql`. The BFF calls the backend server-side, receives the body tokens, and stores them in **httpOnly, Secure, SameSite cookies**. Browser requests to `/api/graphql` carry the cookie; the handler reads the access token from the cookie, attaches it as a Bearer header, and forwards to the backend `/graphql`.

**Cookie policy (fixed to avoid divergence):** two cookies, `pg_access` and `pg_refresh`, both `HttpOnly; Secure; SameSite=Lax; Path=/`, with `Max-Age` mirroring the backend TTLs — access **900s**, refresh **1209600s**. When the access cookie has lapsed but the refresh cookie is present, the BFF treats the session as refreshable rather than logged out.

**Why over the alternative (client-held tokens + backend CORS):**
1. **No backend change.** The backend has no CORS config; the BFF is same-origin from the browser, so the missing CORS never bites. Client-held tokens would require adding a `CorsConfigurationSource` to `SecurityConfig` — outside this change's scope and a security surface of its own.
2. **Token security.** httpOnly cookies keep both tokens unreadable from JavaScript, removing the XSS token-theft vector that in-memory/`localStorage` tokens carry. This directly satisfies the `web-authentication` requirement that tokens not be exposed to client JS.
3. **Server-Components-first alignment (`BOUNDARIES.md`).** Auth state and the token live server-side; Server Components can gate on the session without shipping token logic to the client.
4. **Refresh is server-side and invisible.** The 15-min access / 14-day refresh split is handled in the BFF: when a forwarded request returns **HTTP 401** from the backend, the handler calls `/auth/refresh` with the refresh cookie, replaces the access cookie, and retries once — the client never sees tokens or refresh timing.

**Refresh trigger is exactly HTTP 401, nothing else.** In this backend an expired or invalid access token is rejected by the OAuth2 resource-server filter *before* GraphQL executes, so token failures are always a plain 401. A GraphQL `200` carrying `errors` is a domain/resolver error, **not** an auth failure, and must never trigger a refresh. There is no ambiguous "GraphQL auth error" case to guess at here.

**Refresh-token semantics (from the backend):** `/auth/refresh` is stateless — it returns only a new access token and does **not** rotate, store, or revoke the refresh token, which stays valid until its 14-day expiry. Two consequences: (a) concurrent refreshes are harmless (each independently yields a valid access token — see the single-flight risk below), and (b) logout cannot revoke the refresh token server-side; deleting the cookie is the only lever (stated as a limitation, not a defect).

**Trade-off:** every API call takes an extra same-origin hop (browser → Next → Spring). Acceptable: latency is small (co-located), and it buys the security and no-CORS properties. **CSRF posture:** the BFF is cookie-authenticated, so CSRF is a real surface. It is mitigated by `SameSite=Lax` (the cookie is not sent on cross-site POSTs) and by accepting mutations **only through same-origin BFF routes** with no CORS. This is a strong default, not a permanent guarantee; a same-origin Origin/Referer check (or a CSRF token) can be added later if the posture ever needs hardening.

### D2 — GraphQL access: `graphql-request` + `@graphql-codegen`, driven by TanStack Query

**Decision:** Use TanStack Query as the server-state manager (mandated). For transport, use a minimal typed GraphQL fetch (via a small client such as `graphql-request`, or `fetch` in the BFF) with **`@graphql-codegen`** generating operation + result types and typed query hooks from `schema.graphqls`. No Apollo Client / no Apollo cache — TanStack Query owns caching, per the stack.

**Why:** `TECHNOLOGY.md` names TanStack Query, not a GraphQL-specific client/cache. Codegen against the real schema gives compile-time safety and catches schema drift (a `web-data-access` requirement). It is build-time tooling, not a runtime framework, so it clears the dependency gate.

**Alternative considered:** hand-written types — rejected: drifts from the schema, defeats the typed-client requirement. **Apollo Client** — rejected: duplicates TanStack Query's caching and contradicts the stack.

### D3 — Design tokens: authored via `impeccable`, exposed as CSS variables consumed by Tailwind v4

**Decision:** Author concrete token values (palette, type scale, spacing, radius, elevation, motion, etc.) with the `impeccable` skill in the editorial/premium direction, define them as CSS custom properties, and wire them into Tailwind v4's `@theme` so every utility resolves to a token. Components use Tailwind utilities that map to tokens; no raw values.

**Why:** Tailwind v4's CSS-first `@theme` is the idiomatic single source of truth and satisfies "all styling resolves to tokens." `impeccable` produces a coherent, restrained set and its design hook will scan the resulting UI files for regressions (hardcoded values, overused fonts, contrast).

**Open point for review:** the actual palette/type choices are proposed for user approval before lock-in (per the earlier decision, "I propose them via impeccable").

### D4 — Project layout and structure

**Decision:** `frontend/` is an independent pnpm project (own `package.json`, not wired into the Java build). App Router structure with a route group for authenticated routes (e.g. `(app)/`) guarded by a layout/middleware session check, a public `(auth)/` group for login/register, `app/api/*` route handlers for the BFF, a `lib/` for the query client + generated GraphQL, and a `components/ui/` for the re-skinned shadcn baseline. A shared root layout carries the token theme, fonts, and page structure.

**Why:** matches App Router conventions and `BOUNDARIES.md` ("consistent page structure", "reuse existing components"). Keeping it a standalone package avoids coupling the frontend build to Maven/Gradle.

### D5 — Route protection: two layers with distinct, non-overlapping jobs

**Decision:** Split protection into two layers so neither duplicates the other's logic:

- **Proxy = UX optimisation only.** (In Next.js 16 the former *Middleware* is renamed **Proxy** — a root `proxy.ts` exporting a `proxy()` function; behaviour is unchanged, and the Next 16 docs explicitly say it "should not be used as a full session management or authorization solution", only "optimistic checks" — exactly this role.) It checks *presence* of the session cookies and fast-redirects an obviously-unauthenticated request away from `(app)/` (and an authenticated one away from `(auth)/`). It does **not** decode/validate JWTs, call the backend, or refresh. It is not a security boundary and is never trusted for authorisation.
- **Protected server layout / route handlers = the authoritative security boundary.** The `(app)/` server layout (and the `/api/graphql` handler) perform the real work: read the cookies server-side, drive refresh-on-401 through the BFF, and treat a request as authenticated only when a valid access token is (or can be) obtained. If validation/refresh fails here, the session is ended and the user is sent to `/login`.

**Why:** Next middleware genuinely cannot safely validate or refresh a token, so making it merely a fast cookie-presence redirect — with the server layer as the single authoritative gate — removes the awkward "middleware sees expired token but can't refresh" split and avoids implementing the same check twice.

**Logout** is a BFF route (`/api/auth/logout`) that (1) deletes both cookies, (2) does not attempt any refresh afterwards, and (3) signals the client to `queryClient.clear()` so TanStack Query stops serving authenticated data before the next reload. Note the backend cannot revoke the refresh token (see D1); cookie deletion is the only server-visible action.

### D6 — Authenticated routes are dynamic (no caching of per-user data)

**Decision:** Routes and fetches that read authenticated, per-user data are marked dynamic / `no-store`, so a user's data (e.g. their saves list) never enters a static or shared cache.

**Why:** App Router caches aggressively by default; without opting out, per-user responses could be statically cached or shared across users. `no-store` on the authenticated data path is the safe default for this app.

## Risks / Trade-offs

- **Extra network hop from the BFF** → Mitigation: co-located deployment; the hop is same-origin and cheap; only API traffic is proxied.
- **Refresh-race under concurrent requests** (multiple in-flight calls hit a just-expired access token) → Mitigation: single-flight refresh coalesces concurrent refreshes *within one Node instance*. This is a **per-instance optimisation, not a global guarantee** — across multiple instances (serverless/horizontal scaling) refreshes are not deduplicated. That is safe here because the backend does not rotate refresh tokens (D1): redundant concurrent refreshes each return a valid access token with no race, so single-flight only trims wasted calls, it is not required for correctness.
- **Logout cannot revoke the refresh token server-side** (no revocation store in the backend) → Mitigation: none available at this layer; logout deletes the cookies and the token simply expires at 14 days. Documented as a known limitation; server-side revocation would be a backend change out of scope here.
- **Codegen coupling to a local schema file** → Mitigation: codegen reads `backend/src/main/resources/graphql/schema.graphqls` directly (monorepo); a script re-runs it, and CI/type-check fails on drift. If the file moves, update one path.
- **Token/secret handling in the BFF** → Mitigation: backend base URL and any server-only config live in server env vars, never `NEXT_PUBLIC_*`; tokens exist only in httpOnly cookies and server memory.
- **Scope creep into feature screens** → Mitigation: specs cap the slice at login/register + saves list; anything else is a separate change.
- **Design-token bikeshedding** → Mitigation: propose the token set once via `impeccable` for a single approval, then lock; the design hook guards regressions thereafter.

## Migration Plan

Additive and greenfield — no existing frontend to migrate. Deployment/Docker is a later step; this change is validated locally: `pnpm install`, run codegen, start the dev server, register/log in against a running backend, and view the saves list. Rollback is trivial (the `frontend/` directory is self-contained and touches no backend code).

## Open Questions

- **Concrete token values** (palette, typeface, scale) — to be proposed via `impeccable` and approved before the token layer is locked.
- **Backend base URL / env configuration** for the BFF in local vs future deployed environments — settled at implementation with server-only env vars; deployment specifics deferred to the Docker step.
