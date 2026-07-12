## 1. Constants

- [x] 1.1 Add `WATER_DROP_SETBACK` (~15 yd) to `SimConstants`, documented as the near-edge/lateral relief margin applied to a water-drop.

## 2. Water-drop recovery in the three round loops

- [x] 2.1 In `RoundResolver.resolveHole`, split the `hazardEntered()` branch: `WATER` → dropped `remaining = min(preShotRemaining, outcome.distanceRemaining() + WATER_DROP_SETBACK)` and set the running lie to `Surface.PRIMARY_ROUGH`; any other hazard (OOB) → keep the existing stroke-and-distance reset (`remaining = preShotRemaining`, lie unchanged).
- [x] 2.2 Apply the identical branch in `PlayableRound.resolveOne`.
- [x] 2.3 Apply the identical branch in `PlayableHole.resolveOne`.
- [x] 2.4 Update the class/branch comments in all three loops (they currently say "stroke-and-distance" unconditionally) to describe water-drop vs OOB.

## 3. Tests

- [x] 3.1 Add a unit test proving a WATER outcome advances the ball (dropped `remaining` < `preShotRemaining`, +1 penalty) while an OOB outcome still replays from the previous spot.
- [x] 3.2 Extend or add a fidelity test that a fully-simmed playable round/hole crossing a water hazard equals the automatic `resolveRound` result for the same inputs and seed.
- [x] 3.3 Confirm `ScoringCalibrationTest` and the existing shot/statistics tests still pass; adjust only absolute-bound guards if the water easing moves them, documenting why. (No adjustment needed — the PARKLAND calm calibration is unchanged at field mean −0.40.)

## 4. Verify

- [x] 4.1 Run the full backend test suite green. (446 tests, +4 new `WaterDropTest`.)
- [x] 4.2 Empirically re-measure world scoring over a multi-season run to confirm water holes eased and the field mean stayed within the calibration guard. (Throwaway diagnostic over 480 windy rounds: mean vs par eased +6.44 → +6.18 with the water-drop, water re-finds 200 → 181 — a small welcome easing against the ~+4.9 drift; guard holds.)
