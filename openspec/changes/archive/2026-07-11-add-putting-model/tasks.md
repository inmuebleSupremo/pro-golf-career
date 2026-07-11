## 1. Thread the ball's lie into resolution

- [x] 1.1 `ShotContext`: add `Surface lie` (last component, non-null); add a backward-compatible 7-arg constructor defaulting `lie` to `Surface.TEE_BOX` so single-shot fixtures/tests are unchanged.
- [x] 1.2 `RoundResolver.resolveHole`: track `lie` (init `TEE_BOX`, update to `outcome.finalSurface()` each shot, matching `PlayableRound`); pass it into the context. Add a lie-aware `buildContext` overload and keep the old signature delegating with `TEE_BOX` (used by the equivalence test for shot 1).
- [x] 1.3 `PlayableRound.resolveOne` and `PlayableHole.resolveOne`: pass their already-tracked `lie` into the `ShotContext`.

## 2. Putting make-probability model

- [x] 2.1 `SimConstants`: add putting tunables (yards→feet, `f50` base/span, sharpness, make cap, fatigue/pressure penalties, leave floor/fraction/proximity-relief/sigma/min/converge).
- [x] 2.2 `ShotResolver.resolveWith`: when `context.lie() == Surface.GREEN`, delegate to `resolvePutt` and return early.
- [x] 2.3 `ShotResolver.resolvePutt`: make roll from a logistic in feet (accuracy-raised `f50`, shaved by fatigue/pressure, capped); on a make, hole out (`distanceRemaining = 0`); on a miss, leave a proximity-scaled distance floored above the holed threshold and capped below the start (converging). Draw the lag gaussian unconditionally so the stream is branch-stable. Emit a complete `ShotOutcome` on `GREEN` with a valid `FactorBreakdown`.

## 3. Recalibrate ball-flight dispersion

- [x] 3.1 `SimConstants`: widen `LATERAL_DISPERSION_FRACTION` (0.048 → 0.094) and `DISTANCE_DISPERSION_FRACTION` (0.038 → 0.058) so GIR/fairways/scoring hit realistic targets now that putting no longer masks them.

## 4. Tests

- [x] 4.1 `PuttingModelTest`: short putts hole out near-certainly (no shot-cap hits); make rate falls with distance and rises with putting skill; putts are wind/lie immune; a missed putt converges; a full round holes out with realistic putts-per-round.
- [x] 4.2 `ShotStatisticsTest`: widen the driver "near target" band (15 → 30 yd, > 0.65) to match the realistic post-recalibration dispersion; keep the extremes-rare and hero-shot assertions.
- [x] 4.3 Full suite green (386 tests): reproducibility, entry-point equivalence, playable-round/event fidelity, and the scoring-calibration guard all hold.

## 5. Verify

- [x] 5.1 Measure a real 5-season World at near-production scale: field-wide ~+3 strokes/round (was ~+90), ~30 putts/round, ~63% GIR — believable golf.
