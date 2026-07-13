## Context

The core slice (`add-world-snapshot-core`) captures an autonomous world; `World.snapshot()` throws when `playerControl != null`. The remaining state is small and entirely id/primitive/record: `PlayerControl` (golfer id, `List<Attribute>` development focus, resting flag, `Set<Long>` skipped events, `List<CareerGoal>` goals — all in `sim.control`), plus the World's `playerPendingOffers` (`List<SponsorshipOffer>`), `playerPendingStaff` (`List<StaffMember>`), `playerPendingEquipment` (`List<EquipmentItem>`), and `achievedGoals` (`Set<CareerGoal>`). The designated golfer itself is already captured by the golfer registry (human or simulation control both handled).

## Goals / Non-Goals

**Goals:**
- Capture the player-control state and lift the autonomous-only guard, so a player's career round-trips.
- Keep autonomous snapshots byte-identical to the core slice (the new fields are empty/null for an autonomous world).

**Non-Goals:**
- Mid-event capture — the clean-boundary rule (`snapshot()` still rejects a pending player event) is unchanged.
- No new player behaviour; this is pure capturability.

## Decisions

**Decision: a `PlayerControl.Snapshot` record + `snapshot()`/`restore()` in `sim.control`.** It captures golfer id, development focus, resting, skipped events, and career goals. `restore(snapshot)` rebuilds a `PlayerControl` and re-applies its setters (the class already exposes `setDevelopmentFocus`/`setResting`/`skipEvent`/`setCareerGoals`), so no new mutation surface is needed beyond exposing the skipped-event ids.

**Decision: extend `WorldSnapshot` with nullable player fields.** New components: `PlayerControl.Snapshot playerControl` (null for an autonomous world), `List<SponsorshipOffer> playerPendingOffers`, `List<StaffMember> playerPendingStaff`, `List<EquipmentItem> playerPendingEquipment`, `Set<CareerGoal> achievedGoals`. All are immutable records/enums, so `WorldSnapshot` remains a self-comparing determinism digest.

**Decision: `World.snapshot()` drops only the player-world rejection; the pending-event guard stays.** It captures the player fields null-safely (control is null when autonomous). `World.restore()` rebuilds `playerControl` (if present) and refills the pending lists and achieved goals. An autonomous snapshot leaves them null/empty exactly as a freshly-created world.

## Risks / Trade-offs

- **Determinism must cover control-driven divergence** → the round-trip test sets a non-default development focus and pending offers/goals so the captured control demonstrably affects advancement; a missing field shows as a post-advance snapshot diff. The player world is advanced with `advanceSeason` (which sims the player's events unattended) so both worlds follow the same path.
- **Ordering of the skipped-event set / focus list** → captured as ordered collections (`LinkedHashSet`/`List`) and restored in order, matching the live representation.
