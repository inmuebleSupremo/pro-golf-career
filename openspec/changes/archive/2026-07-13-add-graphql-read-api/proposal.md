## Why

`docs/backend/tech_stack.md` makes GraphQL the primary API between frontend and backend, with REST reserved for health/auth. Today the only HTTP surface is a provisional read-only REST endpoint (`GET /api/world/{id}`) that exists solely to prove the engine wrapper end-to-end. The React/Apollo frontend (a later step) needs a real GraphQL schema over the engine. `WorldService` already exposes the complete engine surface; this change begins wrapping it in GraphQL.

The GraphQL surface is large, so it is delivered in two slices. This first slice is a CQRS-shaped split: it stands up the GraphQL foundation, exposes the **entire read model** as queries, and adds the **world lifecycle** mutations (create/advance). The second slice (`add-graphql-mutations`) adds the player-control, playable-event, and persistence-write mutations on top of the pattern established here.

## What Changes

- Add `spring-boot-starter-graphql` as a new backend dependency (Spring GraphQL, version managed by the Boot 3.3.5 parent).
- Schema-first GraphQL: a `.graphqls` schema under `src/main/resources/graphql/`, with GraphiQL enabled (`spring.graphql.graphiql.enabled=true`) for manual exploration.
- `@Controller` resolvers in `com.progolf.app` delegating to the existing `WorldService`. The simulation core (`sim.*`) stays framework-free; `ArchitecturePurityTest` already scopes only `com/progolf/sim`, so app-layer GraphQL is allowed.
- **DTO boundary**: resolvers never return raw `sim.*` records. A small app-layer mapper projects `ProfessionalGolfer`, `LeaderboardEntry`, `PlayerScheduleEntry`, `CareerGoalProgress`, `HallOfFameInduction`, `SponsorshipOffer`, `StaffMember`, `EquipmentItem`, and `ShotSituation` into GraphQL-friendly DTOs.
- **Queries (the whole read model)**: `world(id)` status (season, week, active population, whether a player event is pending); `playerSchedule(id)`; `careerGoals(id)`; `hallOfFame(id)`; `pendingSponsorships(id)`; `pendingStaff(id)`; `pendingEquipment(id)`; the playable-event read side `currentSituation(id)`, `eventLeaderboard(id)`, `playerMadeCut(id)`; and `listSaves`.
- **Mutations (lifecycle only)**: `createWorld(seed)`, `createWorld(seed, config)`, `advanceSeason(id)`, `advanceWeek(id)`. Player/play/persistence-write mutations are deferred to `add-graphql-mutations`.
- Error mapping: a `DataFetcherExceptionResolver` maps `WorldSessionNotFoundException` (and the save-not-found path a read can hit) to GraphQL errors classified `NOT_FOUND` rather than opaque 500s.
- **BREAKING**: remove the provisional REST `WorldController` (`GET /api/world/{id}`). GraphQL now covers status; the tech stack keeps REST only for health/auth. Actuator health is unaffected.

## Capabilities

### New Capabilities
- `graphql-api`: the GraphQL API surface over `WorldService` — schema, resolver behavior, the DTO boundary that keeps `sim.*` types out of the schema, and error classification. This slice populates it with the read model and lifecycle mutations; `add-graphql-mutations` extends the same capability.

### Modified Capabilities
- `world-session`: the provisional read-only REST status endpoint is removed; a session's status is now read via the GraphQL `world(id)` query. (The `WorldService`/`WorldSession` behavior is unchanged — only the HTTP surface that exposes it.)

## Impact

- **New dependency**: `spring-boot-starter-graphql` in `backend/pom.xml`.
- **New app-layer code** (`com.progolf.app.api` / `com.progolf.app.graphql`): resolvers, DTOs, mapper, exception resolver, plus `schema.graphqls` and a GraphiQL/config property.
- **Removed**: `WorldController` and its provisional REST endpoint. `WorldStatus` is superseded by a GraphQL DTO (removed or repurposed).
- **Unchanged**: the simulation core (`sim.*`), `WorldService`'s method surface, actuator health, and the `ArchitecturePurityTest` boundary.
- **Tests**: `@SpringBootTest` + `GraphQlTester` for the query and lifecycle resolvers; richer read states (assigned player, pending offers) are arranged by injecting `WorldService` directly, since the setup mutations arrive in the next slice.
