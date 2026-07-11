## Why

The player can now hire and pay all five support-team roles, but two of them do **nothing**. `StaffConstants` is explicit — the caddie's strategic support and the sports psychologist's mental support are *"exposed; not yet applied (no shot context)."* Coach (development), fitness coach and physiotherapist (recovery) all work; caddie and psychologist are pure money sinks. Now that hiring is a real player decision (management breadth), a decision with zero payoff is a hollow one — it violates the "meaningful risk/reward" pillar and the "there is a game here" test. This change makes **all five staff roles matter** by wiring the caddie and psychologist into shot resolution, through the same neutral-by-default temporary-state seam equipment already uses.

## What Changes

- **Support enters the shot through the golfer's temporary state**, exactly like equipment. `GolferState` gains two non-negative, [0,1], default-0 inputs — `mentalSupport` and `strategicSupport` — and `PlayerState.setSupport(...)` carries them before play; `Player.toGolferState` surfaces them. Neutral (0) reproduces prior shot behaviour exactly.
- **The shot engine consumes them (spec: shot-resolution):**
  - **Caddie → `strategicSupport`** folds into the mishit-relief term (the same path Course Management already uses at `SimConstants.MISHIT_MANAGEMENT_RELIEF`): better on-course strategy and club/read selection means fewer blow-up shots.
  - **Psychologist → `mentalSupport`** reduces the effect of **fatigue** on the shot: a mentally resilient golfer holds their dispersion (and carry) together when tired. Fatigue is the live in-world condition (pressure is currently always 0 in world play, so composure/pressure is dormant); targeting fatigue makes the psychologist meaningful *today*, and the existing pressure path is left intact for a future pressure model.
- **The World syncs staff effects before play**, alongside the fatigue and equipment sync it already does: `state.setSupport(effects.mentalSupport(), effects.strategicSupport())` from the golfer's `SupportTeam.effects()`.
- Update the `StaffConstants` comments (no longer "not yet applied"); the magnitudes (`PSYCH_MENTAL_PER_QUALITY`, `CADDIE_STRATEGIC_PER_QUALITY` = 0.20) are unchanged.

Explicitly out of scope: activating a pressure model (psychologist targets the live fatigue penalty; the pressure hook stays for later); any change to coach/fitness/physio effects, to how staff are hired/costed, or to permanent attributes (support is temporary shot state only, never persisted into attributes).

## Capabilities

### Modified Capabilities
- `staff-influence`: the caddie's strategic support and the psychologist's mental support are now applied to shot execution (through the golfer's temporary condition, never by modifying results directly), so all five roles have a mechanical effect.
- `shot-resolution`: a shot additionally consumes the golfer's staff-support characteristics — strategic support reduces mishits, mental support reduces the impact of fatigue — neutral by default so standard (no-support) play is unchanged.

## Impact

- **Codebase**: `GolferState` (+2 inputs, backward-compatible constructors), `PlayerState` (transient support + `setSupport`), `Player.toGolferState`, `ShotResolver` (consume the two inputs at the mishit and fatigue terms), `World` (sync staff effects before play), `StaffConstants` (comments). No change to the staff domain's hiring/cost/effect aggregation — `SupportTeam.effects()` already computes both values.
- **Determinism / reproducibility**: support is neutral (0) by default, so all existing shot and tournament tests (which use standard state) reproduce exactly. Two same-seed worlds remain byte-identical. Because AI golfers who employ a caddie or psychologist now play tighter, multi-season *world* outcomes shift from before this change (both same-seed worlds shift identically); any golden multi-season assertions are updated.
- **Boundary**: `World` applies staff influence through the golfer's public temporary state (as it does equipment), and the staff domain never touches results directly — consistent with `staff-influence`.
