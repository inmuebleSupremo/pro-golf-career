# backend/AGENTS.md

Spring Boot backend: the pure simulation engine + the app layer (GraphQL API, auth, persistence).
Read the root [`AGENTS.md`](../AGENTS.md) first. Product requirements: [`../docs/backend/explore.md`](../docs/backend/explore.md).

## Stack
- **Java 21 LTS**, **Maven** (no wrapper — use a system `mvn`), **Spring Boot 3.3.5**.
- **Spring for GraphQL** (schema-first) — primary API. `graphql-java-extended-scalars` provides the `Long` scalar.
- **Spring Security + OAuth2 resource server** for JWT (Nimbus encode/decode; no third-party JWT lib), BCrypt.
- Tests: JUnit 5 + Spring Boot Test + Spring GraphQL Test + Spring Security Test.
- No database dependency — persistence is filesystem JSON (see below).

## Package layout (`com.progolf`)
- **`sim.*` — the engine. Framework-free and deterministic. This is the hard rule of the codebase.**
  - No Spring, no annotations, no I/O, no web/JSON types. Given a seed, behaviour is reproducible.
  - ~20 domains: `player`, `shot`, `play`, `tournament`, `tour`, `ranking`, `economy`, `equipment`,
    `staff`, `progression`, `career`, `achievement`, `world`, `course`, `weather`, `health`, `media`,
    `population`, `statistics`, `spatial`, `control`, `core`.
  - The boundary is enforced by `src/test/java/com/progolf/sim/ArchitecturePurityTest.java` — it fails the
    build if `sim.*` imports Spring or `app.*`. Never weaken it.
- **`app.*` — the thin Spring wrapper.** Everything with a framework dependency lives here.
  - `app.world` — `WorldService` / `WorldSession`: **the seam a human (via the API) acts through.** New
    player-facing capabilities are exposed here after being built in `sim.*`.
  - `app.api` — GraphQL resolvers (`*Controller`, `@QueryMapping`/`@MutationMapping`), `ApiMapper` (engine
    types → DTOs), `dto/*`, error + config.
  - `app.auth` — JWT auth (`AuthService`, `JwtService`, `SecurityConfig`, `FilesystemUserStore`).
  - `app.persistence` — save/load (`FilesystemSaveGameStore`, `SaveGame`, `SimSnapshotModule` — the Jackson
    module that snapshots/rehydrates a `WorldSession`).

## GraphQL
- Schema is the source of truth: `src/main/resources/graphql/schema.graphqls` (well-commented; read it before
  changing the API). GraphiQL explorer at `/graphiql` in dev.
- Enum arguments cross the wire as **engine enum names** (e.g. nationality, archetype, Attribute names).
- **After editing the schema, re-run the frontend's `pnpm codegen`** so generated client types stay in sync.
- Flow to add a capability: `sim.<domain>` → expose on `WorldService` → resolver + `schema.graphqls` field →
  DTO/mapper → frontend operation.

## Config & persistence
- `src/main/resources/application.properties` — actuator exposes `health` only (until auth locks it down),
  GraphiQL enabled, JWT TTLs, and the store dirs.
- Users → `./users` (`PROGOLF_USERS_DIR`), saves → `./saves` (`PROGOLF_SAVES_DIR`) — JSON files, gitignored.
- `PROGOLF_JWT_SECRET` — HS256 needs ≥ 32 chars; the checked-in default is dev-only, override in real deploys.

## Commands
```bash
mvn test              # full suite incl. ArchitecturePurityTest + realism/calibration harnesses
mvn spring-boot:run   # API on :8080
mvn package           # runnable jar
```

## When editing the engine
- Keep it pure and immutable-leaning (records where sensible). Determinism from the seed matters — several
  tests are statistical/regression harnesses (scoring realism, attribute-value balance, world player success).
  If a realism harness goes red, that's a signal, not flakiness — investigate.
- Run `mvn test` after engine changes.
