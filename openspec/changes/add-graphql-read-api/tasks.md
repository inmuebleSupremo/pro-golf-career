## 1. Dependency & configuration

- [x] 1.1 Add `spring-boot-starter-graphql` to `backend/pom.xml` (version from the Boot parent).
- [x] 1.2 Enable GraphiQL in `src/main/resources/application.properties` (`spring.graphql.graphiql.enabled=true`).
- [x] 1.3 Create `src/main/resources/graphql/schema.graphqls` skeleton (Query, Mutation, scalars) — content filled as resolvers land.

## 2. DTOs & mapper (the boundary)

- [x] 2.1 Create an `com.progolf.app.api.dto` package with DTO records: `WorldStatusDto`, `ScheduleEntryDto`, `CareerGoalDto`, `HallOfFameDto`, `SponsorshipOfferDto`, `StaffMemberDto`, `EquipmentItemDto`, `GolferDto`, `LeaderboardRowDto`, `ShotSituationDto`, `SaveDto`.
- [x] 2.2 Create `ApiMapper` (static pure methods) projecting each `sim.*` read type into its DTO; map `ProfessionalGolfer` → `GolferDto` (id + name); never expose engine records.
- [x] 2.3 Define a `WorldConfigInput` record for the create-world mutation and map it to `sim.world.WorldConfig` (unset fields → engine defaults).

## 3. Schema

- [x] 3.1 Write the GraphQL types for every DTO in `schema.graphqls`.
- [x] 3.2 Write the `Query` type: `world(id)`, `playerSchedule(id)`, `careerGoals(id)`, `hallOfFame(id)`, `pendingSponsorships(id)`, `pendingStaff(id)`, `pendingEquipment(id)`, `currentSituation(id)`, `eventLeaderboard(id)`, `playerMadeCut(id)`, `listSaves`.
- [x] 3.3 Write the `Mutation` type: `createWorld(seed, config)`, `advanceSeason(id)`, `advanceWeek(id)`; and `WorldConfigInput`.

## 4. Resolvers

- [x] 4.1 `WorldQueryController` (`@Controller`): `@QueryMapping` methods for status + the read model, delegating to `WorldService` and returning DTOs.
- [x] 4.2 `WorldMutationController` (`@Controller`): `@MutationMapping` methods for `createWorld` (nullable config → the two `WorldService.create` overloads), `advanceSeason`, `advanceWeek`, each returning the resulting `WorldStatusDto`.
- [x] 4.3 Off-event safety: `currentSituation`/`eventLeaderboard`/`playerMadeCut` return absent/empty when no player event is pending (guard on `hasPendingEvent`).

## 5. Error handling

- [x] 5.1 `GraphQlErrorResolver` (`DataFetcherExceptionResolver`, `@Component`) mapping `WorldSessionNotFoundException` and `SaveNotFoundException` → `ErrorType.NOT_FOUND`; leave others to default handling.

## 6. Remove provisional REST

- [x] 6.1 Delete `WorldController` (`GET /api/world/{id}`); repurpose/remove the old `WorldStatus` record in favor of the DTO.
- [x] 6.2 Confirm actuator health is unaffected and no other REST gameplay surface remains.

## 7. Tests

- [x] 7.1 `@SpringBootTest` GraphQL test config with `GraphQlTester` (HTTP or `ExecutionGraphQlService`-backed).
- [x] 7.2 Lifecycle tests: `createWorld` (with and without config), `advanceWeek`, `advanceSeason` return correct status; two sessions stay independent.
- [x] 7.3 Read-model tests: arrange state by injecting `WorldService` (assign a player, advance a season for offers), then assert `playerSchedule`/`careerGoals`/`pendingSponsorships`/`hallOfFame`/`listSaves` via `GraphQlTester`.
- [x] 7.4 Off-event safety test: `currentSituation`/`eventLeaderboard` return absent/empty for a fresh session.
- [x] 7.5 Error test: querying an unknown session id yields a NOT_FOUND-classified GraphQL error.
- [x] 7.6 Boundary test: assert the schema/DTOs contain no `sim.*` type (extend an app-layer test or assert package of resolver return types); confirm `ArchitecturePurityTest` still green.

## 8. Verify

- [x] 8.1 `mvn test` from `backend/` — full suite green (existing 476 + new).
- [x] 8.2 Manual smoke: boot the app, open GraphiQL, run `createWorld` then `world(id)`.
