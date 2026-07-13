## Context

`add-graphql-read-api` established the GraphQL foundation: a schema-first `.graphqls`, `@Controller` resolvers in `com.progolf.app.api` delegating to `WorldService`, a pure `ApiMapper` projecting `sim.*` reads into DTOs (no engine type in the schema, guarded by `ApiBoundaryTest`), a `Long` scalar, and a `GraphQlErrorResolver` mapping not-found to `NOT_FOUND`. This change adds the write side using that same pattern. The engine already exposes every needed action on `WorldService`; the work is schema, resolvers, input parsing, and error classification — no engine change.

## Goals / Non-Goals

**Goals:**
- Expose the full write surface: player-control decisions, playing/simming a player event, and persistence writes, so a career loop is drivable over GraphQL end-to-end.
- Keep the DTO boundary intact (mutation return types and inputs are app-layer DTOs/records; no `sim.*` type in the schema).
- Parse enum-valued inputs at the edge and classify client-fault failures as `BAD_REQUEST`.
- Reuse the slice-A DTO package, `ApiMapper`, error resolver, and test harness.

**Non-Goals:**
- `selectLoadoutItem` and an owned-equipment/loadout read surface (deferred by decision to a follow-up).
- Subscriptions, auth, and any change to `WorldService` or the engine.
- Optimistic/streaming shot play — `playShot` is a single request/response per shot (the client polls `currentSituation`/`eventLeaderboard` from the read slice between shots).

## Decisions

**D1 — Enum inputs as their names, parsed at the edge.** Club, Strategy, Attribute (development focus), Nationality, Archetype, StaffRole, and GoalType are supplied as `String` enum names and resolved with `Enum.valueOf` in `ApiMapper`. This mirrors slice A (which surfaces enums as names on the read side), keeps the schema decoupled from engine enums, and needs no parallel GraphQL enum definitions. *Alternative:* declare parallel GraphQL enums — rejected as boilerplate that duplicates the engine's enum sets and couples the schema to them; a bad name simply becomes a `BAD_REQUEST` (D4).

**D2 — Mutation return types: outcome-bearing where useful, else the resulting status or a Boolean.** `playShot`/`simShot` return `ShotOutcomeDto` (the resolved shot). `createPlayer` returns the new golfer id. `completeEvent` and `load` return `WorldStatusDto` (the world resumed / the restored session, whose id the client needs to continue). The remaining state-toggling writes (`setResting`, `skipEvent`, `hireStaff`, `save`, …) return `Boolean!` (`true` on success); the client re-queries the affected read model. *Alternative:* every mutation returns a rich affected view (e.g. `skipEvent` → updated schedule) — rejected for this slice as extra coupling; the read model already exposes those views and the client can re-query. The consistent, honest minimum is a Boolean.

**D3 — `ShotDecisionInput` with an optional lateral aim; `CareerGoalInput` with an optional target.** `ShotDecisionInput { club, targetDistance, targetLateral?, strategy }` maps to `ShotDecision` (lateral defaults to 0.0 — the engine's `straight` helper). `CareerGoalInput { type, target? }` maps to `CareerGoal.of(type)` when target is null, else `CareerGoal.of(type, target)`. Boxed types distinguish "unset" from 0.

**D4 — Extend `GraphQlErrorResolver` with a `BAD_REQUEST` mapping.** The write path surfaces client-fault engine exceptions: `IllegalArgumentException` (bad enum name, unaffordable/invalid selection), `IllegalStateException` (no player assigned, no pending event), and `IndexOutOfBoundsException` (offer index out of range). These map to `ErrorType.BAD_REQUEST`; the slice-A `NOT_FOUND` mapping for unknown session/save is kept; anything else falls through to default handling. *Alternative:* validate everything in resolvers before delegating — rejected: it would duplicate the engine's own precondition checks in the app layer; catching and classifying the engine's exceptions is simpler and single-sourced.

**D5 — Group resolvers by concern, one shared mapper.** Player-control, playable-event, and persistence mutations go in separate `@Controller` classes for readability, all delegating to the single `WorldService` and using the single `ApiMapper` for input parsing and outcome projection. This matches the read slice's `WorldQueryController`/`WorldMutationController` split and keeps `ApiBoundaryTest`'s reflection guard simple (extend its resolver list).

## Risks / Trade-offs

- **Enum parsing errors are only caught at execution** → mitigated by D4: a bad name yields a clear `BAD_REQUEST` with the engine's message, not a 500. Documented in the schema descriptions (fields list their accepted enum names' source).
- **Boolean-returning mutations force a client re-query for the new state** → acceptable for V1; the read model already exposes every affected view, and it avoids over-fetching. Can be enriched later without breaking clients (widen the return type via a new field).
- **`playShot` is one round-trip per shot** → fine for a turn-based decision game; a full round is always skippable via `simRound`/`simEvent`, so no long shot-by-shot grind is forced over the wire.
- **Deferring `selectLoadoutItem` leaves one `WorldService` method unwrapped** → intentional: wrapping it without an owned-equipment query would be dead API. Flagged in the proposal; `buyEquipment` auto-equips so the loop is complete.
- **`completeEvent` semantics depend on a pending event existing** → calling it with no pending event throws `IllegalStateException` → `BAD_REQUEST` (D4), which is the correct client signal.
