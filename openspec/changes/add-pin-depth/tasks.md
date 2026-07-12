## 1. Asymmetric green by pin depth

- [x] 1.1 `CourseGenConstants`: `PIN_DEPTH_ASYMMETRY` (0.6) and `PIN_DEPTH_MIN_SIDE` (3.0).
- [x] 1.2 `HoleZones.profileFor`: add a pin-depth-offset overload; split the green depth into independent front/back extents (`greenHalfDepth ± shift`, clamped), so a back pin leaves less green behind and a front pin less in front. Keep the no-offset overload = symmetric (centre pin).
- [x] 1.3 `RoundHole.zoneProfileFor`: pass the round pin's depth offset.

## 2. Tests

- [x] 2.1 `PinDepthTest`: a back pin leaves the over-green trouble closer behind; a front pin shortens the safe run-up in front; a centre pin is symmetric and matches the original green.
- [x] 2.2 Full suite green (436); reproducibility and the calibration guard hold.

## 3. Verify

- [x] 3.1 Measure on a mid-iron approach: back pin ~23% long misses vs ~18% for a front pin (which shows ~24% short misses), GIR dipping ~2 points on extreme pins; world scoring stays believable and the calibration guard passes.
