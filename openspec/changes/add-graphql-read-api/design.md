## Context

The app layer (`com.progolf.app`) wraps the pure simulation engine; `WorldService` is the single seam into it, already exposing the full engine surface (lifecycle, player-control, playable-event, persistence). The only current HTTP surface is a provisional read-only REST endpoint proving the wrapper works. The tech stack mandates Spring GraphQL as the primary API. This change stands up that API and covers the read model + lifecycle; a follow-up slice adds the write mutations. The hard constraint throughout is the architecture boundary: `sim.*` must stay framework-free (guarded by `ArchitecturePurityTest`, scoped to `com/progolf/sim`), so all GraphQL and DTO code lives in `com.progolf.app`, and `sim.*` records must not appear in the schema.

## Goals / Non-Goals

**Goals:**
- A working, schema-first Spring GraphQL API served alongside the existing app, with GraphiQL for manual testing.
- The entire engine **read model** exposed as queries, plus **world lifecycle** mutations (create/advance).
- A clean DTO boundary: no `sim.*` type appears in the schema or in a resolver's return signature.
- A reusable resolver + DTO + mapper + error-handling pattern that the next slice extends mechanically.
- Deterministic, fast resolver tests via `GraphQlTester`.

**Non-Goals:**
- Player-control, playable-event, and persistence-write mutations (deferred to `add-graphql-mutations`).
- GraphQL subscriptions (not in V1).
- Authentication / authorization (a later step; resolvers take a session `id` argument for now).
- Postgres-backed persistence or pagination of large lists (V1 returns full lists).
- Changing any `WorldService` or engine behavior.

## Decisions

**D1 — Schema-first (`.graphqls`) over code-first.** The tech stack names Spring GraphQL, whose idiomatic style is schema-first: a `schema.graphqls` under `src/main/resources/graphql/` with `@Controller` resolvers bound by `@QueryMapping`/`@MutationMapping`/`@SchemaMapping`. *Alternative:* a code-first library (e.g. DGS/Netflix) — rejected as a heavier, non-Spring-native dependency the tech stack does not call for.

**D2 — DTO boundary via a dedicated mapper.** Resolvers return app-layer DTO records (e.g. `WorldStatusDto`, `ScheduleEntryDto`, `LeaderboardRowDto`, `GolferDto`, `ShotSituationDto`, `SaveDto`), produced by a single `ApiMapper` (static pure methods) that projects each `sim.*` read type. This keeps engine records out of the schema (so schema shape is decoupled from engine internals) and confines all mapping to one reviewable place. *Alternative:* register the `sim.*` records directly as GraphQL types — rejected: it leaks engine internals into the schema, couples the wire contract to engine refactors, and violates the stated DTO boundary. *Alternative:* per-resolver inline mapping — rejected: scatters projection logic and duplicates it across the two slices.

**D3 — A single top-level `world(id)` query as the read entry point, siblings for cross-cutting lists.** `world(id)` returns a `WorldStatus` DTO (season/week/activePopulation/hasPendingEvent). The other read surfaces are exposed as sibling top-level queries keyed by session id (`playerSchedule(id)`, `careerGoals(id)`, `hallOfFame(id)`, `pendingSponsorships(id)`, `pendingStaff(id)`, `pendingEquipment(id)`, `currentSituation(id)`, `eventLeaderboard(id)`, `playerMadeCut(id)`), plus `listSaves` (no id). *Alternative:* nest everything as fields under `world` resolved by `@SchemaMapping` — attractive for graph ergonomics, but every field would still need the session id and it complicates null/empty semantics for a session that has no player; flat top-level queries keyed by id are simpler for this slice and the frontend can compose them. Nesting can be revisited later without breaking the DTOs.

**D4 — Config as strong-typed inputs; `createWorld` overloaded via a nullable `config` argument.** `createWorld(seed: Long!, config: WorldConfigInput)` — when `config` is null the resolver calls `WorldService.create(seed)`, otherwise it maps the input to `WorldConfig` and calls `create(seed, config)`. This mirrors the two service overloads without two mutation names. Only the `WorldConfig` fields the API needs are surfaced in `WorldConfigInput`; unset fields fall back to engine defaults.

**D5 — Error classification via `DataFetcherExceptionResolver`.** A `@Component` resolver adapter maps `WorldSessionNotFoundException` (and `SaveNotFoundException`, reachable from `listSaves`/read paths) to a `GraphqlErrorBuilder` error with `ErrorType.NOT_FOUND`, so clients get a typed error instead of an opaque 500. Unmapped exceptions keep Spring's default handling. *Alternative:* throw GraphQL-specific exceptions from resolvers — rejected: it would push GraphQL concerns into `WorldService`, which must stay API-agnostic for auth/frontend reuse.

**D6 — Remove the provisional REST `WorldController`.** GraphQL's `world(id)` supersedes `GET /api/world/{id}`; the tech stack keeps REST only for health/auth, so the endpoint and its `WorldStatus`-returning controller are deleted. `WorldStatus` (the record) is repurposed/renamed as the GraphQL DTO or replaced by `WorldStatusDto`. Actuator health remains the sole REST surface.

**D7 — Tests arrange rich read state through `WorldService` directly.** Because the mutations that create players / pending offers live in the next slice, `@SpringBootTest` resolver tests inject the `WorldService` bean to set up preconditions (assign a player, advance a season to generate offers), then assert via `GraphQlTester`. This keeps the read-model tests meaningful without waiting for the mutation slice.

## Risks / Trade-offs

- **Read state that depends on not-yet-exposed mutations** → mitigated by D7 (arrange via the injected service in tests); production clients simply see empty lists/nulls until the mutation slice lands, which is the honest current capability.
- **DTO drift / duplication across the two slices** → mitigated by D2: one `ApiMapper` and a shared DTO package that the next slice reuses rather than re-deriving.
- **Removing the REST endpoint before the frontend exists** → low risk: it was explicitly provisional and only proved the wrapper; GraphiQL + `GraphQlTester` provide the same manual/automated verification. Reversible (re-add a controller) if ever needed.
- **`ShotSituation`/leaderboard reads return meaningful data only while a player event is pending** → the schema types are nullable and resolvers return null / an empty list off-event; documented in the schema.
- **Adding `spring-boot-starter-graphql` enlarges the app context** → acceptable and mandated by the tech stack; boot time impact is negligible and covered by the existing `@SpringBootTest` startup.
