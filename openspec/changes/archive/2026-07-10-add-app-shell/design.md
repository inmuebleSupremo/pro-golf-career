## Context

The backend is a framework-free Maven library: the only dependency is `spring-boot-starter-test`, there is no `@SpringBootApplication`, no web layer, no runtime. The re-alignment decided to pivot from extending the engine to building the application that makes it a game, keeping `sim.*` pure and adding an application layer that wraps it. This change is the first step: a Spring Boot walking skeleton that boots and hosts the engine. The parent POM is already `spring-boot-starter-parent`, so the Boot BOM and plugin are available without version management.

## Goals / Non-Goals

**Goals:**
- A runnable Spring Boot application that boots cleanly.
- A `WorldService` boundary that creates, holds, advances, and reads world sessions using the existing engine.
- A minimal health/status surface proving the wrapper works over HTTP.
- Zero changes to `com.progolf.sim`; the purity test still passes.

**Non-Goals:**
- Persistence/database, the player-control loop, GraphQL, authentication, the frontend (each a later change).
- A rich or stable REST API — the endpoints here are provisional status/health; GraphQL is the primary API later.
- Concurrency/thread-safety hardening of sessions beyond what a single-user skeleton needs.

## Decisions

### D1. The application layer lives in `com.progolf.app`; the engine stays pure
All Spring, web, and future persistence/security code lives under `com.progolf.app`. `com.progolf.sim` gains nothing and imports nothing new. `@SpringBootApplication` sits at `com.progolf.app.Application`, so component scanning covers only the app package and never turns engine classes into beans. *Why:* the re-alignment choice — keep the tested, deterministic engine framework-free; the app wraps it (REQ: tech stack separation of concerns). The `ArchitecturePurityTest` continues to scope `com/progolf/sim` and is unaffected.

### D2. `WorldService` is the single seam to the engine
A `WorldService` (`@Service`) owns a registry of `WorldSession`s (id → `World`) and the only calls into the engine: `create(seed)`, `advanceSeason(id)`, `advanceWeek(id)`, and `status(id)`. Controllers, and later GraphQL resolvers, persistence, and player actions, go through it — never directly to `World`. *Why:* one boundary keeps the engine encapsulated (tech-stack "gameplay domains expose state; the app coordinates") and gives every later layer a single integration point. *Alternative rejected:* controllers holding `World` directly — scatters engine access and leaks the boundary.

### D3. A `WorldSession` handle wraps a running world with its id and seed
`WorldSession` pairs a `World` with its session id and creation seed (and later, save metadata). Sessions are independent and addressable, which is exactly what persistence (a save = a session) and multi-user (a user owns sessions) will build on. In-memory (a `Map`) for this change. *Why:* sets up REQ-271 (multiple independent worlds) without building persistence yet. *Alternative rejected:* a single global world — precludes multiple saves and multi-user from day one.

### D4. Provisional REST for health/status only; GraphQL is primary later
Expose actuator `health` and a read-only `GET /api/world/{id}` returning a `WorldStatus` DTO (season, week, active population). No create/advance over HTTP yet — those are service methods exercised by tests. *Why:* the tech stack makes GraphQL the primary API and limits REST to health/helpers; a read-only status endpoint is enough to prove the wrapper end-to-end without committing to a REST gameplay API. *Alternative rejected:* a full REST world API now — would be thrown away when GraphQL lands.

### D5. Prove it with a context-load + service integration test
A `@SpringBootTest` asserts the context loads, the `WorldService` bean is wired, a session can be created and advanced, and its status reflects the engine. *Why:* the skeleton's value is proving the framework integrates and the seam works; a passing context-load test is the checkpoint. *Alternative rejected:* only a unit test of `WorldService` — would not prove Spring boots.

## Risks / Trade-offs

- **[First framework/runtime dependencies]** → scoped to `com.progolf.app`; the engine and its purity test are untouched; a context-load test guards the wiring.
- **[Provisional REST superseded by GraphQL]** → kept read-only and minimal (health + one status endpoint), explicitly provisional, so replacement cost is near zero.
- **[Session registry is in-memory and single-process]** → acceptable for a skeleton; persistence (next change) and any scaling concerns attach at the `WorldService`/`WorldSession` seam.
- **[Component-scan accidentally touching the engine]** → the app package root prevents it; a test asserts no engine class is a Spring bean if needed.

## Migration Plan

Additive only. Sequencing: (1) add `spring-boot-starter-web`, `spring-boot-starter-actuator`, and the `spring-boot-maven-plugin` to `backend/pom.xml`; (2) `com.progolf.app.Application`; (3) `WorldSession` + `WorldService` wrapping `World`; (4) `WorldStatus` DTO + provisional `WorldController` (health via actuator, `GET /api/world/{id}`); (5) `@SpringBootTest` context-load + service integration test; (6) run the full suite — the app boots, the service wraps the engine, and the 290 engine tests plus the purity test still pass.

## Open Questions

- Default session on boot vs. create-on-demand — create-on-demand via the service in V1; a bootstrapped demo session can be added if useful for manual testing.
- Where world-creation configuration (seed, world size) is supplied — a request/param for now; auth-owned defaults come with the user model later.
- Actuator exposure/security — default health only for now; locked down with the auth change.
