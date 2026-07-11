## Why

Shot-level statistics surfaced a latent bug: golfers reached the green (~85–90% GIR) but could not **hole out**, taking ~8 putts per hole and posting absurd absolute scores (**~+90 per round**, +358 per event). The relative standings still worked (everyone was equally broken), so it stayed hidden until putts-per-round put a number on it.

The root cause was that a putt was resolved as a full **ball-flight** shot. The green-shot dispersion floor (~7 in) is large relative to the 1 ft hole, so short putts could never reliably finish within the holed threshold — and, far worse, **wind and lie penalties designed for full shots were applied to putts**: a crosswind doubled putt dispersion and a headwind subtracted absolute yards from a few-yard putt, so in any wind the ball simply orbited the hole until the per-hole shot cap. Fixing this is the highest-value remaining depth-pass item: it turns nonsense absolute scores into believable golf and makes the honestly-derived shot stats (GIR / putts) finally read realistically.

Removing the inflated putting also **exposed** that ball-striking was calibrated too tight (90% GIR, 83% fairways) to compensate for the broken putting — so this change also recentres the shot dispersion against realistic scoring targets, which `SimConstants` always flagged as a pending tuning pass.

## What Changes

- **A dedicated putting model (shot domain).** A shot played from the green (`lie == GREEN`) is resolved by an explicit **make-probability model** instead of the full ball-flight geometry: the ball rolls on the green — **immune to wind and lie** — and either drops or finishes a short, proximity-controlled distance away. Make probability is a logistic in distance (feet), centred on a putting-accuracy-raised 50%-make distance, shaved modestly by fatigue / uncomposed pressure. A miss always leaves a distinct, converging tap-in, so the round holes out in a realistic 1–2 putts from short range with a thin 3-putt tail (no more multi-putt orbiting or shot-cap hits).
- **Thread the ball's lie into resolution.** `ShotContext` gains the starting `lie` (the surface the shot is played from); `RoundResolver`, `PlayableRound`, and `PlayableHole` track and pass it (a backward-compatible constructor defaults it to tee box for single-shot fixtures/tests). Keying the putt model on the lie — not the club — means long first putts across a big green are covered too, which a distance-based club choice would clip to a full shot.
- **Recalibrate ball-flight dispersion.** Widen `LATERAL_DISPERSION_FRACTION` (0.048 → 0.094) and `DISTANCE_DISPERSION_FRACTION` (0.038 → 0.058) so greens-in-regulation (~63%), fairways, and scoring land on realistic targets now that putting no longer masks them. Putts bypass this path, so it does not touch them.

Result (measured): a real 5-season World at near-production scale goes from **~+90 to ~+3 strokes/round** field-wide, with **~30 putts/round** and **~63% GIR** — believable golf.

Explicitly out of scope: per-club dispersion (still a bag aggregate), green-reading / break, sand saves & scrambling as first-class stats, a full water-drop model, and activating the pressure model in-world (pressure is still 0 in world play, so its putting hook is dormant — a separate depth-pass item).

## Capabilities

### Modified Capabilities
- `shot-resolution`: a shot played from the green is resolved by a putting make-probability model — sheltered from wind and lie, holing out near-certainly from tap-in range — rather than by the full ball-flight model, so rounds hole out and produce realistic putts-per-round and absolute scores.

## Impact

- **Codebase**: new `ShotResolver.resolvePutt` + putting constants in `SimConstants`; `ShotContext` gains `lie` (with a back-compat constructor); `RoundResolver` / `PlayableRound` / `PlayableHole` track and pass the lie; ball-flight dispersion fractions retuned. New `PuttingModelTest`; `ShotStatisticsTest` driver band widened to match the realistic dispersion; the `ScoringCalibrationTest` guard now holds with correct putting.
- **Determinism / fidelity**: each shot is still seeded solely at its own coordinate, so the putt path's different RNG draw count never couples across shots; `resolveRound` and the interactive `PlayableRound`/`PlayableHole` compute the lie identically, so a fully-simmed round stays byte-identical to the automatic resolution (fidelity preserved). Reproducibility (same seed → same world) is unchanged. Absolute scores change by design — this is the calibration fix.
- **DAG**: unchanged. `ShotContext` already depends on `spatial` (for `Surface`); no new imports invert the graph.
