## Context

Authentication is in place: the JWT filter sets a security principal whose name is the user id (the token subject) on every authenticated request; `/graphql` is gated. `WorldService` is the single engine seam and, by the slice-(a) decision (D6), must stay free of Spring Security — the principal is threaded in as a plain argument, not read from a security dependency inside the seam. Sessions live in an in-memory map; saves live behind the `SaveGameStore` port. This slice scopes both to the owning user.

## Goals / Non-Goals

**Goals:**
- Every session and save belongs to the user who created it; only that user can reach it.
- Cross-user access is denied without leaking existence (not-found).
- `WorldService` receives the owner as a string; `sim.*` and the GraphQL schema are unchanged.
- Existing tests keep working under an authenticated principal; new tests prove isolation.

**Non-Goals:**
- Sharing, transfer, or admin override of another user's resources.
- Roles/authorities beyond the single authenticated user.
- Migrating the pre-existing global saves on disk (dev data; a fresh per-user layout is fine).
- Persisting sessions across restart (sessions remain in-memory; only saves are durable).

## Decisions

**D1 — The principal is read in the resolver and passed as `ownerId`.** A small `AuthenticatedUser` helper returns the current user id from `SecurityContextHolder` (the authentication name = the JWT subject). Each resolver calls it once and passes the id into `WorldService`. `WorldService` gains an `ownerId` first parameter on every public method but no Spring Security import — it stays API-agnostic and reusable. *Alternative:* `WorldService` reads the security context itself — rejected: it would couple the engine seam to Spring Security, breaking the slice-(a) boundary and the seam's reusability. *Alternative:* thread `@AuthenticationPrincipal` into every resolver signature — equivalent but noisier; the helper centralizes the extraction.

**D2 — Sessions carry an owner; `required(ownerId, id)` authorizes.** `WorldSession` gains an `ownerId`. The private `required` lookup returns the session only when its owner matches the caller; otherwise it throws `WorldSessionNotFoundException` (→ NOT_FOUND). `create` stamps the caller as owner. Every session-scoped `WorldService` method (lifecycle, player-control, playable-event, equipment, persistence) takes `ownerId` and goes through `required(ownerId, id)`. *Alternative:* a separate `authorize` step in each method — rejected: folding the ownership check into the existing `required` lookup makes it impossible to forget.

**D3 — Saves are owner-scoped in the store.** `SaveGameStore` methods take `ownerId` (`save`, `load`, `list`, `exists`, `delete`). The filesystem adapter keeps each user's saves in a per-user subdirectory (`<saves-dir>/<ownerId>/<saveId>.json`), so `list(ownerId)` returns only that user's saves and `load`/`delete` for a save the user does not own is `SaveNotFoundException` (→ NOT_FOUND). Autosave writes the reserved slot inside the caller's namespace, so each user has an independent autosave. *Alternative:* one flat directory with an owner field filtered on read — rejected: per-user directories give natural isolation, keep listing cheap, and map cleanly onto a future `WHERE owner = ?` in the Postgres adapter. The user id is a UUID (safe as a directory name).

**D4 — Cross-user access is not-found, not forbidden.** Reaching another user's session or save returns the same not-found result as a nonexistent one, so the API never reveals that another user's resource exists. This is the privacy-preserving default and needs no new error type (reuses the existing NOT_FOUND classification).

**D5 — Existing GraphQL tests run authenticated; new tests prove isolation.** Resolvers now require a principal, so the existing `GraphQlTester` (ExecutionGraphQlService) tests get one — via a security context established for the test (e.g. an authenticated user set on the context) so `AuthenticatedUser` resolves. New tests create resources as user A and assert user B gets not-found on read/advance/load/delete, and that each user's `listSaves` shows only their own. *Note:* if thread-boundary propagation makes the security context invisible to the tester's execution, the ownership tests fall back to `HttpGraphQlTester` against a running server with real bearer tokens (the fully faithful path); this is decided during implementation by what actually propagates.

## Risks / Trade-offs

- **Wide signature churn** (`WorldService`, the store, every resolver gain `ownerId`) → purely mechanical; the compiler enforces completeness, and folding the check into `required`/the store methods means no call site can bypass it. → Mitigation: change the private `required` first so every method is forced through the owner check.
- **Security-context propagation into GraphQL data fetchers** → on the MVC request thread the filter-set context is visible to the synchronous resolver; verified by the HTTP smoke. For the in-process test tester, D5's fallback covers any propagation gap.
- **Pre-existing global saves become unreachable** (they are not under any user's namespace) → acceptable: these are local dev artifacts, and saves are gitignored. No migration needed.
- **A forgotten `ownerId` on some new future method** → the ownership check lives in the shared `required`/store methods, so a new method that uses them is scoped by construction; one that bypasses them would be an obvious review flag.
