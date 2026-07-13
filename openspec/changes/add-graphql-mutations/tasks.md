## 1. Inputs, DTOs & mapper

- [x] 1.1 Add input records to `com.progolf.app.api.dto`: `ShotDecisionInput` (club, targetDistance, targetLateral?, strategy) and `CareerGoalInput` (type, target?).
- [x] 1.2 Add `ShotOutcomeDto` (finalSurface, carry, lateral, distanceRemaining, hazardEntered, penaltyStrokes, strokes) — drops the engine's factor breakdown.
- [x] 1.3 Extend `ApiMapper`: `shotDecision(ShotDecisionInput)` (enum parse + lateral default 0), `careerGoal(CareerGoalInput)`/`careerGoals(list)`, `shotOutcome(ShotOutcome)`, and enum-name parse helpers (Club, Strategy, Attribute, Nationality, Archetype, StaffRole, GoalType).

## 2. Schema

- [x] 2.1 Add mutation fields to `schema.graphqls`: player-control (`assignPlayer`, `createPlayer`, `setDevelopmentFocus`, `setResting`, `skipEvent`, `enterEvent`, `setCareerGoals`, `acceptSponsorship`, `hireStaff`, `releaseStaff`, `buyEquipment`).
- [x] 2.2 Add playable-event mutations (`playShot`, `simShot`, `simHole`, `simRound`, `simEvent`, `completeEvent`) and persistence writes (`save`, `load`, `deleteSave`).
- [x] 2.3 Add the `ShotDecisionInput`, `CareerGoalInput` inputs and the `ShotOutcome` type; document accepted enum names in field descriptions.

## 3. Resolvers

- [x] 3.1 `PlayerMutationController` (`@Controller`): `@MutationMapping` for the player-control writes, delegating to `WorldService`; `createPlayer` returns the new golfer id, the rest return `Boolean`.
- [x] 3.2 `PlayEventMutationController`: `playShot`/`simShot` return `ShotOutcomeDto`; `simHole`/`simRound`/`simEvent` return `Boolean`; `completeEvent` returns `WorldStatusDto`.
- [x] 3.3 `PersistenceMutationController`: `save`/`deleteSave` return `Boolean`; `load` returns `WorldStatusDto` for the restored session.

## 4. Error handling

- [x] 4.1 Extend `GraphQlErrorResolver`: map `IllegalArgumentException`, `IllegalStateException`, `IndexOutOfBoundsException` → `ErrorType.BAD_REQUEST`; keep `NOT_FOUND` for unknown session/save; leave others to default.

## 5. Tests

- [x] 5.1 Player-control test: `createPlayer` → id; `setCareerGoals` then query `careerGoals` reflects them; `acceptSponsorship`/`hireStaff`/`buyEquipment` after arranging pending offers via `WorldService` (advance a season).
- [x] 5.2 Playable-event test: drive a session to a pending event (assign/enter a player, advanceWeek), `playShot` and/or `simEvent` return an outcome, `completeEvent` resumes and returns advanced status.
- [x] 5.3 Persistence test: `save` then `load` returns a new session with the saved season/week; `deleteSave` then a read is not-found.
- [x] 5.4 Error tests: off-event `playShot`/`completeEvent` → BAD_REQUEST; out-of-range `acceptSponsorship` → BAD_REQUEST; bad enum name → BAD_REQUEST; unknown save `load` → NOT_FOUND.
- [x] 5.5 Extend `ApiBoundaryTest`'s resolver list with the three new controllers; confirm no `sim.*` return type and `ArchitecturePurityTest` still green.

## 6. Verify

- [x] 6.1 `mvn test` from `backend/` — full suite green.
- [x] 6.2 Manual smoke: boot, create a world + player over GraphQL, save/load, and sim an event to completion.
