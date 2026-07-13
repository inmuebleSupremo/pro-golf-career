## Why

`add-graphql-read-api` stood up Spring GraphQL over `WorldService` with the entire read model and the world-lifecycle mutations, deferring the write side to keep that first slice reviewable. This change completes the GraphQL layer by adding the remaining **write mutations** — player-control decisions, playing/simming a player event, and persistence writes — on the exact pattern established there (the DTO package, `ApiMapper`, `GraphQlErrorResolver`, and the `GraphQlTester` harness). After this, a client can drive a full career loop over GraphQL end-to-end.

## What Changes

- **Player-control mutations**: `assignPlayer`, `createPlayer`, `setDevelopmentFocus`, `setResting`, `skipEvent`, `enterEvent`, `setCareerGoals`, `acceptSponsorship`, `hireStaff`, `releaseStaff`, `buyEquipment` — each delegating to the matching `WorldService` method.
- **Playable-event mutations**: `playShot` and `simShot` (returning the resolved shot outcome), `simHole`, `simRound`, `simEvent`, and `completeEvent` (which resumes the paused week and returns the new world status).
- **Persistence-write mutations**: `save`, `load` (returns the restored session's status, including its new id), and `deleteSave`.
- **Input types**: `ShotDecisionInput` (club / target / strategy) and `CareerGoalInput` (goal type + optional target); enum-valued arguments (nationality, archetype, development-focus attributes, staff role, club, strategy, goal type) are passed as their enum names and parsed at the edge.
- **New DTO**: `ShotOutcomeDto` (the resolved shot result, minus the engine's internal factor breakdown).
- **Error mapping extended**: `GraphQlErrorResolver` also maps client-fault engine exceptions (`IllegalArgumentException`, `IllegalStateException`, `IndexOutOfBoundsException` — bad enum name, no player assigned, out-of-range offer index) to a `BAD_REQUEST`-classified GraphQL error. The `NOT_FOUND` mapping from the read slice is retained.
- **Deferred (out of scope, by decision)**: `selectLoadoutItem` — switching the loadout to an already-owned item. The engine identifies the item by record equality and no owned-equipment read surface exists yet, so wrapping it now would be an unusable mutation. It moves to a small follow-up that also adds the owned-equipment/loadout query it needs. `buyEquipment` already auto-equips purchases, so the core loop is unaffected.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `graphql-api`: extend the capability with the write-mutation surface (player-control, playable-event, persistence writes), the mutation input types, and the `BAD_REQUEST` error classification. The read model and lifecycle mutations added by `add-graphql-read-api` are unchanged.

## Impact

- **New app-layer code** (`com.progolf.app.api`): a `PlayerMutationController`, a `PlayEventMutationController`, and a `PersistenceMutationController` (or grouped resolvers), input records (`ShotDecisionInput`, `CareerGoalInput`) and `ShotOutcomeDto` in `api.dto`, plus new mapping/parsing methods on `ApiMapper`. `schema.graphqls` gains the mutation fields and input/DTO types.
- **Modified**: `GraphQlErrorResolver` gains the `BAD_REQUEST` mapping.
- **Unchanged**: the simulation core (`sim.*`), `WorldService`'s method surface, the read model, actuator health, and the `ArchitecturePurityTest` boundary.
- **Tests**: `@SpringBootTest` + `GraphQlTester` covering a player-control decision, a played/simmed event through to `completeEvent`, a `save`/`load` round-trip, and the `BAD_REQUEST`/`NOT_FOUND` error paths.
