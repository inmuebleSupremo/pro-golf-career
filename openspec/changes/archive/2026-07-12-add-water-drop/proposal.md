## Why

V1 resolves every penalty hazard — water and out-of-bounds alike — with a single **stroke-and-distance** rule: the ball replays from the previous spot with one penalty stroke, losing all the distance the shot gained. That is the correct rule for out-of-bounds and lost balls, but it is far too punishing for a **water hazard**, where real golf lets a player take relief near where the ball crossed into the hazard and play forward. Treating water like OOB inflates scores on water holes and hides a meaningful strategic distinction (a water carry is a recoverable gamble; OOB is a disaster).

## What Changes

- Split penalty-hazard recovery by surface: **WATER** now uses a proper **water-drop** — the ball is dropped near where it entered the hazard (its water-entry point) plus a modest setback for near-edge/lateral relief, and the next shot is played from a realistic rough lie, with the existing +1 penalty. **OUT_OF_BOUNDS** keeps the current stroke-and-distance rule (replay from the previous spot, +1).
- The drop advances the ball toward the hole instead of resetting to the previous spot, so a water carry now costs a stroke and some distance rather than a full shot — recoverable, not catastrophic.
- Applied identically across all three replay loops (`RoundResolver.resolveHole`, `PlayableRound`, `PlayableHole`) so AI ↔ playable fidelity is preserved.

## Capabilities

### New Capabilities
<!-- none -->

### Modified Capabilities
- `shot-resolution`: the round loop's hazard-recovery rule gains a requirement distinguishing water (drop near the hazard, play forward) from out-of-bounds (stroke-and-distance). `resolveRound`/playable fidelity is unaffected because the same rule runs in every loop.

## Impact

- `com.progolf.sim.shot.RoundResolver` — the hazard branch of `resolveHole`.
- `com.progolf.sim.play.PlayableRound` and `com.progolf.sim.play.PlayableHole` — their mirrored hazard branches.
- `com.progolf.sim.shot.SimConstants` — new `WATER_DROP_SETBACK` constant (and a defined drop lie).
- Field-wide scoring on water holes eases slightly (a water carry becomes recoverable); `ScoringCalibrationTest` guard must still hold. No API/serialization surface changes.
