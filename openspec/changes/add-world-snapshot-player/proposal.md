## Why

The core world-snapshot slice captures an autonomous world but deliberately rejects a world with a designated player — `snapshot()` throws when a player is assigned. That leaves the actual save-a-career case (a human controlling a golfer) uncapturable. This slice completes capturability: it snapshots the player-control state and lifts the autonomous-only guard, so a player's career round-trips with the same determinism guarantee as the autonomous world.

## What Changes

- Capture the player-control state that the core slice omits: the `PlayerControl` (designated golfer id, development focus, resting flag, per-event skips, and chosen career goals), the World's pending sponsorship / staff / equipment offers, and the set of already-achieved career goals.
- Lift the autonomous-only restriction in `World.snapshot()`: a world with an assigned player may now be captured (the clean-boundary rule — no pending player event — still holds). `restore` rebuilds the player-control state; an autonomous snapshot restores to `playerControl == null` exactly as before.
- Extend the round-trip determinism test to a player world: with a designated golfer, a non-default development focus, and pending offers/goals, `snapshot → restore → advance N` equals `advance N`.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `world-snapshot`: the snapshot additionally captures the player-control state (designated golfer and its standing decisions, pending offers, achieved goals), so a player-controlled world — not only an autonomous one — can be captured and restored with the same determinism guarantee.

## Impact

- `com.progolf.sim.control.PlayerControl`: a `Snapshot` record + `snapshot()`/`restore()` (golfer id, development focus, resting, skipped events, career goals).
- `com.progolf.sim.world.WorldSnapshot`: new fields for the player-control snapshot (nullable), pending sponsorship/staff/equipment offers, and achieved goals.
- `com.progolf.sim.world.World`: `snapshot()` drops the player-world rejection and captures the player state; `restore()` rebuilds it. All the value types involved (`CareerGoal`, `SponsorshipOffer`, `StaffMember`, `EquipmentItem`, `Attribute`) are already immutable records/enums.
- No behaviour change to a running world; autonomous snapshots are byte-identical to the core slice.
