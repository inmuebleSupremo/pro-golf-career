## Why

`add-auth-accounts` authenticates every API request, but sessions and saves are still global: any logged-in user can address any session id or save id, list everyone's saves, and load or delete them. This slice completes Step 8 by making sessions and saves **owned per user** — you can only see, load, and act on your own — which is the point of having accounts.

## What Changes

- **Owned sessions**: a `WorldSession` gains an owner (the authenticated user id). `WorldService` authorizes every session operation against the caller's user id; a session that belongs to someone else is reported as not-found (its existence is not revealed).
- **Owned saves**: the `SaveGameStore` becomes owner-scoped — save, load, list, delete, and exists take the owner. The filesystem adapter stores each user's saves under their own namespace, so `listSaves` returns only the caller's saves and cross-user load/delete is not-found. **Autosave** is now per-user (the reserved slot lives in the caller's namespace).
- **Principal threading**: GraphQL resolvers read the authenticated user id from the security context (via a small helper) and pass it into `WorldService` as an explicit `ownerId` argument. `WorldService` stays free of Spring Security — it only receives a user-id string — preserving the engine seam.
- **No engine change**: `sim.*` is untouched; ownership lives entirely in the app layer.

## Capabilities

### New Capabilities
- `resource-ownership`: sessions and saves belong to the authenticated user who created them; the API scopes every operation to that user and denies (as not-found) any attempt to reach another user's session or save.

### Modified Capabilities
- `save-persistence`: the store and its operations become owner-scoped (each user's saves are isolated; autosave is per-user), while keeping the versioning, checksum, multiple-saves, and autosave guarantees.
- `world-session`: sessions are owned; a session is retrievable and mutable only by its owner.

## Impact

- **Modified app-layer code**: `WorldSession` (owner field); `WorldService` (every public method takes an `ownerId`; `required` authorizes ownership; save ops and autosave scope to owner); `SaveGameStore` + `FilesystemSaveGameStore` (owner-scoped methods, per-user directories); all five GraphQL resolver controllers (extract the principal, pass `ownerId`); a new `AuthenticatedUser` helper in `com.progolf.app.auth`.
- **Unchanged**: the simulation core (`sim.*`), the GraphQL schema (no new fields — ownership is enforced underneath), the `/auth` endpoints, and the `ArchitecturePurityTest` boundary.
- **Behavioral note**: cross-user access to a session or save resolves to **not-found** (privacy-preserving — it does not distinguish "not yours" from "does not exist").
- **Tests**: existing GraphQL resolver tests run under an authenticated principal so resolvers receive an owner; new tests prove two users' sessions and saves are isolated (a user cannot read, advance, load, or delete another user's resources); `SaveGameStore`/persistence tests updated for the owner-scoped signatures.
