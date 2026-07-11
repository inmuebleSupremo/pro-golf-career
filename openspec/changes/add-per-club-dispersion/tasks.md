## 1. Per-club dispersion

- [x] 1.1 `Club`: replace the single `dispersionMultiplier` with independent `lateralDispersion` + `distanceDispersion` per club; add accessors.
- [x] 1.2 `ShotResolver`: apply `club.lateralDispersion()` to the offline term and `club.distanceDispersion()` to the long/short term.

## 2. Calibrate

- [x] 2.1 Tune the per-club values so a calm generated field reads ~60% fairways and ~67% GIR (from ~54%/~71%), the wedge is the precision club, and the driver the widest offline. Verify world scoring stays believable (~+3.5/round, ~30 putts) and `ScoringCalibrationTest` holds.

## 3. Tests

- [x] 3.1 `PerClubDispersionTest`: a wedge is tighter than an iron at the same distance (both lateral and distance); the driver sprays wider off the tee than the wedge; every club has positive dispersion multipliers.
- [x] 3.2 Full suite green (418).
