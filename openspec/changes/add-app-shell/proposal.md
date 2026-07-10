## Why

Eighteen simulation domains are built and tested, but they are a **framework-free library** — there is no running application. The tech stack mandates a Spring Boot backend whose responsibilities are *business logic, simulation engine, GraphQL API, authentication, and database access*; so far only the simulation engine exists, and even that has no host. This change stands up the **application shell**: a real Spring Boot service that boots and wraps the engine behind a service boundary, so every subsequent layer (persistence, player actions, GraphQL, auth) has a running host to attach to. It is deliberately a thin walking skeleton — its job is to prove the framework integrates cleanly and to establish the seam between the pure engine and the application, without changing a line of `sim.*`.

## What Changes

- Add Spring Boot **web** and **actuator** starters (compile scope) and the Spring Boot Maven plugin, turning the module from a test-only library into a runnable application. The simulation core stays framework-free — Spring lives only in a new `com.progolf.app` package.
- Add a `@SpringBootApplication` entry point (`com.progolf.app.Application`) that boots the service. Component scanning is rooted at `com.progolf.app`, so no beans are introduced into `com.progolf.sim`.
- Add a **`WorldService`** that owns the boundary to the engine: it creates and holds **world sessions** (each an independent `World` addressed by id), advances them (season/week), and reads their status. In-memory for now; durable saves are the next change.
- Expose a minimal, provisional status surface: actuator **health**, and a read-only `GET /api/world/{id}` returning a session's season/week/population. These prove the wrapper end-to-end; the real, primary API is GraphQL in a later change.
- Add a `@SpringBootTest` proving the application context loads and the engine is reachable through the service, and confirm the `ArchitecturePurityTest` still passes (it scopes only `com.progolf.sim`).

Explicitly out of scope: persistence/database (next change), the player-control loop, the GraphQL schema, authentication, and the frontend. This change adds **no gameplay behaviour** — it hosts the existing engine. REST endpoints here are provisional status/health only; per the tech stack, GraphQL will be the primary API and can supersede them.

## Capabilities

### New Capabilities
- `world-session`: The application boots as a Spring Boot service and manages running World simulations as sessions — creating and holding independent worlds by id, advancing and reading them through a service boundary, and reporting health/status — while the simulation engine stays framework-free and the application merely wraps it.

## Impact

- **Codebase**: New `com.progolf.app` package (`Application`, `WorldService`, a `WorldSession` handle, a provisional `WorldController`, and the world status DTO). `backend/pom.xml` gains `spring-boot-starter-web`, `spring-boot-starter-actuator`, and the `spring-boot-maven-plugin`. No `com.progolf.sim` files change.
- **Architecture**: establishes the engine/app seam chosen in re-alignment — `sim.*` remains pure and deterministic (its purity test still guards it); `com.progolf.app` is the only place Spring, web, and (later) persistence/security are allowed. The app depends on the engine; the engine never depends on the app.
- **Downstream (future changes)**: `WorldService` becomes the integration point for persistence (load/save a session), player actions (apply a human decision then advance), GraphQL resolvers (read/mutate sessions), and auth (a user owns sessions/saves).
- **Risk**: introduces the first framework and runtime dependencies. Kept low by a thin skeleton: the only new runtime surface is a health check and one read-only status endpoint, both covered by a context-load/integration test; the engine and its 290 tests are untouched.
