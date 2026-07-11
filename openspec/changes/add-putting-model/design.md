## Context

Putts were resolved by the same ball-flight math as full shots. Two coupled defects made rounds unable to hole out: (1) the distance-scaled dispersion has a floor (~0.20 yd) that is large versus the holed threshold (0.35 yd), so short putts could not reliably finish inside it; and (2) the full-shot **wind and lie** terms applied to putts — a crosswind ~doubled putt dispersion and a headwind subtracted absolute yards from a few-yard putt — so in any wind the ball orbited the hole until the shot cap. Measured impact before the fix: breezy conditions produced ~173 putts and +141 per round; a real World averaged ~+90.

## Goals / Non-goals

- **Goals**: putts hole out realistically (~29–32 putts/round, thin 3-putt tail); absolute scores become believable golf (~par, not +90); putting attributes finally expressed; wind/lie no longer wreck putts; determinism and playable-round fidelity preserved.
- **Non-goals**: green-reading/break, per-club dispersion, sand-save/scramble stats, water-drop model, activating the in-world pressure model.

## Decisions

### D1 — Key the putt model on the lie (`lie == GREEN`), not the club
The trigger is *"the ball is on the green,"* carried as a new `ShotContext.lie`. Clubs are chosen by distance, and a big green (18–34 yd deep) yields first putts well beyond the putter's ~8 yd distance band (~12% of putts, up to ~20 yd); keying on the club would clip those long first putts to a wind-exposed full shot. Keying on the lie covers every putt and needs no change to the club-selection policy. **Alternative considered**: raise the putter distance threshold — rejected because distance alone cannot separate "20 yd on the green" (a putt) from "20 yd in the fairway" (a pitch).

### D2 — Explicit make-probability + converging lag, replacing geometry for putts
A putt draws a make roll against `p = 1 / (1 + (feet / f50)^k)`, with `f50` raised by putting accuracy and `p` shaved by fatigue / uncomposed pressure. On a make the ball is holed; on a miss it leaves a proximity-scaled distance that is floored **above** the holed threshold (so a miss is never mistaken for a hole-out) and capped **below** the starting distance (so it always converges). This reproduces the near-certainty of short putts that a fixed-floor geometric model cannot, and guarantees the hole holes out without relying on the shot cap. **Alternative**: keep geometry but make putts wind/lie-immune and shrink the floor — rejected: it tames the catastrophe but leaves ~34 putts and a fat multi-putt tail (the geometry still can't make a 1-footer ~certain).

### D3 — Recalibrate ball-flight dispersion in the same change
Correct putting removed ~6 strokes/round that the tight ball-striking (90% GIR / 83% fairways) had been calibrated to offset, dropping the field to ~-9 and failing the scoring-calibration guard. Widening the two dispersion fractions (lateral 0.048→0.094, distance 0.038→0.058) restores ~63% GIR and a field mean near par. Putts bypass the ball-flight path, so this retune is orthogonal to the putting model. `SimConstants` always flagged these as a pending calibration pass; this is that pass, done together because the calibration guard couples them.

### D4 — Preserve determinism and fidelity
Each shot is seeded solely at its own coordinate (`coordinate.withShot(n)`), so the putt path consuming a different number of RNG draws than the full-shot path never couples across shots. `RoundResolver` (auto), `PlayableRound`, and `PlayableHole` all track the lie identically (init tee box, update to the previous shot's final surface each stroke), so a fully-simmed round stays byte-identical to the automatic resolution — the existing fidelity guardrails still hold. A backward-compatible 7-arg `ShotContext` constructor defaults the lie to tee box, so single-shot fixtures and tests are untouched.

## Risks / Trade-offs

- **Absolute scores change by design.** Reproducibility (same seed → same world) is preserved, but any test pinning absolute scores would need updating; in practice only the driver-dispersion band in `ShotStatisticsTest` needed widening to match the realistic calibration.
- **A "putt" may carry a non-putter club in its (discarded) decision** when the policy clubbed a long green shot as a wedge; the resolver ignores the club on the green, and no outcome stores the club, so this is invisible — but noted.
- **Fairways read a touch low (~54%)** because lateral dispersion is a single fraction shared by driver and irons; per-club dispersion (deferred) would let them diverge. Aggregate scoring is realistic, so this is accepted for V1.
