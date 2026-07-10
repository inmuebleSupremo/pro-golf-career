## 1. Build & application entry point

- [x] 1.1 Add `spring-boot-starter-web` and `spring-boot-starter-actuator` (compile scope) and the `spring-boot-maven-plugin` to `backend/pom.xml`; keep `sim.*` free of any new dependency.
- [x] 1.2 Add `com.progolf.app.Application` (`@SpringBootApplication`, `main`); component scanning rooted at `com.progolf.app` so no `com.progolf.sim` class becomes a bean.

## 2. Engine boundary

- [x] 2.1 Add `WorldSession` (session id, seed, the running `World`).
- [x] 2.2 Add `WorldService` (`@Service`) owning an in-memory registry of sessions: `create(seed)`, `advanceSeason(id)`, `advanceWeek(id)`, `status(id)` — the only calls into the engine.

## 3. Status surface

- [x] 3.1 Add a `WorldStatus` DTO (session id, season, week, active population).
- [x] 3.2 Add a provisional `WorldController` exposing read-only `GET /api/world/{id}` returning `WorldStatus`; rely on actuator for health. No gameplay mutation over HTTP.

## 4. Verification

- [x] 4.1 `@SpringBootTest` asserting the context loads and `WorldService` is wired.
- [x] 4.2 Service integration test: create a session, advance it, and assert its status reflects the engine; two sessions are independent.
- [x] 4.3 Confirm the full suite still passes — the 290 engine tests and `ArchitecturePurityTest` are unaffected (the app package is outside its scope).
- [x] 4.4 Run `openspec validate add-app-shell --type change --strict` and resolve findings.
