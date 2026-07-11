## Why

Every full-swing club shared one dispersion multiplier (`1.0`), so the driver and the irons scattered at the same fraction of their shot length. That coupled two stats that should be independent: driving accuracy (fairways hit) and greens in regulation. After the putting-model recalibration they read **~54% fairways vs ~71% GIR** — fairways a touch low, GIR a touch high — with no way to move one without the other. A wedge, a precision club in real golf, was no more accurate per yard than a driver.

## What Changes

- **Per-club dispersion profiles (shot).** `Club` now carries **independent lateral and distance dispersion multipliers** instead of one shared multiplier. `ShotResolver` applies the lateral multiplier to the offline term and the distance multiplier to the long/short term, so each club has its own accuracy character: the driver sprays widest off the tee, irons are looser than the short game, and the **wedge is the precision club** (tightest lateral and distance control). The multipliers are the tuning surface for driving-accuracy and GIR, now decoupled.
- **Calibrated to realistic rates.** Tuned so a generated field reads **~60% fairways and ~67% GIR** in calm conditions (from ~54%/~71%), while the wedge's distance control tightens to a realistic ~6 yards. Field-wide world scoring stays believable (~+3.5/round, ~30 putts).

Explicitly out of scope: per-club **equipment** effects (forgiveness/power are still a bag aggregate — a separate deferred item); wind/lie interacting differently per club; a distinct "shot shape" model. Note a modelling trade-off: because fairways are narrow, hitting a realistic ~60% requires a driver tighter than a real-world driver's raw offline spread — the observable *rate* is realistic even though the underlying driver dispersion is tighter than reality; a wider-driver + wider-fairways pairing is a later course-tuning option.

## Capabilities

### Modified Capabilities
- `shot-resolution`: dispersion is now per-club — each club carries independent lateral and distance dispersion, so driving accuracy and greens-in-regulation are tuned separately and a wedge is more precise than a driver.

## Impact

- **Codebase**: `Club` gains `lateralDispersion()`/`distanceDispersion()` (replacing the single `dispersionMultiplier()`); `ShotResolver` uses them for the two dispersion terms. New `PerClubDispersionTest`. Only `ShotResolver` consumed the old accessor, so the surface is small.
- **Determinism / calibration**: pure calibration — no change to the resolution structure, RNG, or determinism; same-seed worlds stay reproducible. Absolute scores shift slightly (by design) as the dispersion is redistributed; the `ScoringCalibrationTest` guard still holds (field mean ~−2.8 in calm).
- **DAG**: unchanged — all within `sim.shot`.
