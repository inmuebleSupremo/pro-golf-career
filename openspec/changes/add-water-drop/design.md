## Context

Every penalty hazard is currently recovered by one rule in three mirrored round loops. `RoundResolver.resolveHole`, `PlayableRound.resolveOne`, and `PlayableHole.resolveOne` each branch on `outcome.hazardEntered()` and, when true, set `remaining = preShotRemaining` — stroke-and-distance — regardless of whether the hazard was `WATER` or `OUT_OF_BOUNDS`. Both surfaces already carry `penaltyStrokes() == 1`, which the resolver folds into `outcome.strokes()`, so the loops add no penalty of their own; they only decide where the next shot is played from.

The shot outcome already computes `distanceRemaining` unconditionally, even for a hazard landing — it is the distance-to-pin from wherever the ball flew (into the water). `Surface.isHazard()` is true for both, but the two are distinct enum constants, so the loops can tell them apart cheaply.

The three loops must stay behaviourally identical: playable-round and playable-event fidelity is guaranteed by "a fully-simmed round equals the automatic resolution," which only holds if the hazard branch matches byte-for-byte across all three.

## Goals / Non-Goals

**Goals:**
- Make a water carry recoverable: drop near where the ball entered the hazard and play forward, +1 stroke, from a realistic lie.
- Keep out-of-bounds on stroke-and-distance (the correct real rule).
- Preserve AI ↔ playable fidelity by applying the identical rule in all three loops.
- Keep the change neutral to every path that never enters a hazard (no drift on hazard-free holes).

**Non-Goals:**
- Modelling the true hazard margin geometry (where exactly the ball crossed the water's edge). We approximate the drop from the shot's own `distanceRemaining`.
- Lateral vs back-on-the-line drop choice, or a player decision at the drop. The drop is automatic and deterministic.
- Changing OOB behaviour, penalty counts, the holed threshold, or the shot cap.

## Decisions

**Decision: branch the hazard recovery on the finishing surface.** In each loop's `hazardEntered()` branch, check `outcome.finalSurface() == Surface.WATER`. Water → drop; anything else hazardous (OOB) → the existing stroke-and-distance reset. This is a minimal, local edit to a branch that already exists in all three loops.

**Decision: water-drop position = `outcome.distanceRemaining() + WATER_DROP_SETBACK`.** The user chose "entry point + setback." The ball's water-landing point (its `distanceRemaining`) is the natural entry approximation; the setback (a new `SimConstants.WATER_DROP_SETBACK`, ~15 yd) pulls the drop modestly back toward the tee to represent near-edge and lateral relief rather than crediting the full carry into the water. Result: a water carry costs +1 stroke and a small distance give-back, far less than the whole-shot loss of stroke-and-distance. Alternatives rejected: no-setback (slightly too generous — credits the full flight into the water); fractional-carry credit (needs pre-shot carry tracking and a tuned fraction for marginal realism gain).

**Decision: post-drop lie = `Surface.PRIMARY_ROUGH`.** A drop near a penalty area is played from grass, not from "water." Today the loops set `lie = outcome.finalSurface()` unconditionally, which after a hazard leaves `lie == WATER` — a quirk that happens to force the next shot conservative (WATER's recoveryDifficulty 1.0 ≥ the caution threshold) but is nonsensical as a played-from lie. For a water-drop we override the lie to `PRIMARY_ROUGH` after the drop so the next shot plays from a believable lie. OOB stroke-and-distance keeps its current lie handling unchanged.

**Decision: guard the drop against going backwards.** `distanceRemaining + setback` could, in a pathological short-hazard case, exceed `preShotRemaining` (i.e. the "drop" would be farther from the hole than where the shot was played). Clamp the dropped `remaining` to at most `preShotRemaining` so a water-drop never leaves the player worse off than stroke-and-distance would.

**Decision: apply in all three loops with identical arithmetic.** The same three lines (compute dropped remaining, clamp, set rough lie) go into `RoundResolver`, `PlayableRound`, and `PlayableHole`. A shared helper is tempting but the loops live in different packages (`sim.shot` vs `sim.play`) and each already inlines its stroke-and-distance branch; a `SimConstants`-driven inline keeps the fidelity obvious and avoids a new cross-package dependency.

## Risks / Trade-offs

- **Scoring drift down on water holes** → world scoring has drifted *up* to ~+4.9/round across the situational changes, so a small easing on water holes is directionally welcome; `ScoringCalibrationTest` guard must still pass and will be re-checked empirically.
- **Fidelity regression if the three branches diverge** → add/extend a fidelity test (simmed playable == auto) that exercises a water hazard, and keep the arithmetic identical and constant-driven.
- **Approximate entry point** → the drop is derived from the ball's water-landing point, not the true margin crossing; acceptable for V1 (documented Non-Goal), and the setback keeps it from over-crediting.
- **Lie override changes next-shot strategy** → moving the post-water lie from WATER to PRIMARY_ROUGH means the next shot is no longer force-conservative and no longer carries the WATER lie penalty; this is intentional (a drop in rough is genuinely easier than the old "play from water" quirk) but is a behavioural change on water holes to verify.
