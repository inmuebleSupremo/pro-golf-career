## 1. Owner-scoped save store

- [x] 1.1 `SaveGameStore`: add `ownerId` (first param) to `save`, `load`, `list`, `exists`, `delete`.
- [x] 1.2 `FilesystemSaveGameStore`: store each owner's saves under `<dir>/<ownerId>/`; `fileFor(ownerId, saveId)`, per-owner `createDirectories`, atomic temp file in the owner dir; `list(ownerId)` scans only that dir; load/delete/exists scoped to the owner.
- [x] 1.3 Update `FilesystemSaveGameStoreTest` for the owner-scoped signatures; add a per-owner isolation test (same save id under two owners loads distinct worlds; one owner can't load/delete the other's).

## 2. Owned sessions & WorldService

- [x] 2.1 `WorldSession`: add `ownerId`.
- [x] 2.2 `WorldService`: `required(ownerId, id)` returns the session only if owned, else `WorldSessionNotFoundException`; `create`/`load` stamp the owner.
- [x] 2.3 Thread `ownerId` (first param) through every public `WorldService` method (lifecycle, player-control, playable-event, equipment, persistence); route each through `required(ownerId, id)`; save ops call the owner-scoped store; `autosave` writes the owner's reserved slot.

## 3. Principal in resolvers

- [x] 3.1 `AuthenticatedUser` helper (`com.progolf.app.auth`): current user id from the security context (authentication name = JWT subject); throws if unauthenticated.
- [x] 3.2 All five resolver controllers: obtain `ownerId` from `AuthenticatedUser` and pass it into `WorldService`. The owner is never a schema argument.

## 4. Tests

- [x] 4.1 Existing GraphQL resolver tests (`WorldGraphQlApiTest`/`MutationsTest`/`EquipmentTest`): run under an authenticated principal so resolvers receive an owner (establish a security context for the tests). Confirm they pass unchanged in behavior.
- [x] 4.2 New ownership test: user A creates a session/save; user B gets not-found on read/advance/load/delete; each user's `listSaves` shows only their own. (Use two principals, or `HttpGraphQlTester` + real tokens if context propagation requires it.)
- [x] 4.3 Update `WorldPersistenceTest` / `WorldPlayerServiceTest` / `WorldApplicationTest` for the new `WorldService` signatures (pass a test owner id).
- [x] 4.4 `ArchitecturePurityTest` green; `WorldService` has no Spring Security import; `ApiBoundaryTest` still green.

## 5. Verify

- [x] 5.1 `mvn test` from `backend/` — full suite green.
- [x] 5.2 Manual smoke: two users over HTTP — each creates a world + save, lists saves (sees only own), and gets not-found addressing the other's session/save.
