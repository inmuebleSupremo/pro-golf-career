## Why

The lateral pin-attacking change made a tucked flag a real risk/reward, but the pin's **depth** (front-to-back) was still inert: the green complex was always centred on the pin, so a back pin and a front pin played identically, and going long was punished the same regardless of where the flag sat. In real golf, pin depth is half the read — a back pin dares you to fire at it over the trouble behind, a front pin punishes a shot that comes up short. This completes the tucked-pin model (the third deferred situational refinement).

## What Changes

- **Pin depth positions the green (course/spatial).** The green complex now shifts off-centre from the pin by the pin's depth offset (already generated per round): a **back pin** leaves less green behind it, so the over-green trouble sits closer and going long is punished; a **front pin** leaves less green in front, so the safe run-up is shorter and coming up short is punished. The green keeps its size; a centre pin stays symmetric (unchanged). `HoleZones` builds the asymmetric green from the round's pin; the shift is a tunable fraction of the pin depth so the effect is meaningful but the pin is never off its own green.

Measured on a mid-iron approach: a back pin roughly halves the "safe long" margin (long misses ~23% vs ~18% for a front pin), and a front pin punishes short misses (~24% vs ~19%), with greens-in-regulation dipping slightly on extreme pins — the natural distance dispersion now meets a pin-dependent penalty, so tighter, more controlled players handle demanding pins better.

Explicitly out of scope: a *decision* to aim safely short of a back pin / long of a front pin (the depth analog of the lateral pin-attack aim) — the geometry makes pin depth matter, but the policy still fires at the pin; the safe-aim decision is a deferred refinement. Also out of scope: green contouring / slope.

## Capabilities

### Modified Capabilities
- `course-generation`: the active pin's depth now positions the green complex off-centre from the pin — a back pin leaves the over-green trouble closer behind, a front pin shortens the run-up in front — so front and back pins play differently.

## Impact

- **Codebase**: `HoleZones.profileFor` gains a pin-depth-offset overload and builds the green with independent front/back extents; `RoundHole` passes the round pin's depth; `CourseGenConstants` gains `PIN_DEPTH_ASYMMETRY` and `PIN_DEPTH_MIN_SIDE`. New `PinDepthTest`.
- **Determinism / calibration**: pure geometry from the already-deterministic per-round pin, so same-seed worlds stay reproducible. Scores shift modestly (extreme pins are a touch harder); the `ScoringCalibrationTest` guard holds. A centre pin reproduces the prior symmetric green exactly (the default overload).
- **DAG**: unchanged — all within `sim.course` / `sim.spatial`.
