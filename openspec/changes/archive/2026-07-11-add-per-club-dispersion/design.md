## Context

`ShotResolver` computed `baseLateral` and `baseDistanceDispersion` as `FRACTION × shotDistance × club.dispersionMultiplier() + FLOOR`, with a single `dispersionMultiplier` that was `1.0` for every full-swing club. So the only per-club lever was shared between the offline and distance terms and identical across clubs — driving accuracy and GIR could not move independently, and a wedge was no tighter than a driver.

## Goals / Non-goals

- **Goals**: independent lateral and distance dispersion per club; realistic, separately-tunable fairway-hit and GIR rates; a precision wedge and a wide driver.
- **Non-goals**: per-club equipment forgiveness/power (still a bag aggregate), per-club wind/lie response, shot-shaping.

## Decisions

### D1 — Split the single multiplier into per-club lateral and distance multipliers
`Club` now declares `(lateralDispersion, distanceDispersion)`; `ShotResolver` multiplies the offline term by the former and the long/short term by the latter. Calibrated values: driver `0.87 / 1.15`, fairway wood `0.95 / 1.10`, hybrid `1.00 / 1.05`, iron `1.22 / 1.00`, wedge `0.85 / 0.80`, putter `2.60 / 2.60` (unchanged; putts bypass this path via the make-% model). The iron's looser lateral pulls GIR down to ~67%; the driver's tighter lateral lifts fairways to ~60%; the wedge's tight profile makes it the precision club (~6 yd distance control).

### D2 — Prioritise realistic rates over literal driver spread
Real drivers scatter ~30 yд offline, but the generated fairways are narrow, so a realistic ~60% fairway rate needs the driver tighter than that (~20 yд here). The **observable statistic** (driving accuracy) is what the player sees and what Pillar 3 grades, so it is tuned to be realistic even though the underlying driver dispersion is tighter than a real driver's. A wider-driver + wider-fairways pairing (a course-generation change) is the deferred alternative; it is out of scope for a dispersion-only change.

## Risks / Trade-offs

- **Absolute scores shift slightly** (looser irons add scrambling): world scoring moves from ~+3.3 to ~+3.6/round — still believable, putts unchanged (~30). Reproducibility holds (pure calibration). The `ScoringCalibrationTest` guard still passes.
- **Driver dispersion is tighter than reality** (D2) — a rate-vs-character trade-off, documented; the fix is course-side, not dispersion-side.
- **World-wide GIR/fairways read lower than the calm calibration** (~58%/51% vs ~68%/60%) because the world spans four tiers and real weather; the elite tier reads near the calibrated ideal. This is expected, not a regression.
