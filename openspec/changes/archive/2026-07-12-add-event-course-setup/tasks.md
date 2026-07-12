## 1. CourseSetup descriptor (sim.course)

- [x] 1.1 Add a `CourseSetup(double pinAggression, double windScale, double widthScale)` record with a `standard()` factory (all 1.0) and finite/positive validation.

## 2. Setup-aware hole production (sim.course)

- [x] 2.1 `GeneratedHole.pinFor(round, CourseSetup)` scales lateral by `widthScale × pinAggression` and depth range by `pinAggression`; `forRound(round, setup)` overload; keep the no-setup overloads delegating to `standard()`.
- [x] 2.2 `HoleZones.profileFor(hole, remaining, pinDepth, widthScale)` scales the green and fairway core half-widths by `widthScale`; keep the existing overload delegating with `widthScale = 1.0`.
- [x] 2.3 `RoundHole` carries the setup's `widthScale` and passes it to `profileFor`; `Course.holeModel(hole, round, setup)` overload, existing `holeModel(hole, round)` delegates to `standard()`.

## 3. Tier/prestige mapping (sim.tournament)

- [x] 3.1 Add `SetupDifficulty.forEvent(Tier, EventPrestige) -> CourseSetup`: one `difficulty` per event class (tier base rising with field strength + prestige bump) mapped monotonically to the three factors. Constants provisional; calibrated in task 6.

## 4. Apply in resolution (sim.tournament, sim.play)

- [x] 4.1 `Tournament` gains a `(definition, weather, CourseSetup)` constructor and a `setup()` accessor; older constructors default to `CourseSetup.standard()`.
- [x] 4.2 `Tournament.playRound` and the playoff-hole path resolve via `holeModel(hole, round, setup)` and scale `exposure` by `setup.windScale()`.
- [x] 4.3 `PlayableEvent` reads `tournament.setup()` and applies it identically in its round and playoff hole production (holeModel + exposure).

## 5. World wiring

- [x] 5.1 In `buildEvent`, compute `CourseSetup setup = SetupDifficulty.forEvent(tier, event.prestige())` and construct the `Tournament` with it (the `PlayableEvent` then reads it from the tournament).

## 6. Calibration

- [x] 6.1 Throwaway diagnostic: per-(tier, prestige) field-scoring average over generated fields with the setup applied. Tune `SetupDifficulty` so each tier's Regular ~−0.5..E, Signature ~+0.5..+1, Major ~+2.5..+4.
- [x] 6.2 Record the tuned targets and remove the diagnostic.

## 7. Tests

- [x] 7.1 `CourseSetup` neutral test: `standard()` reproduces baseline hole geometry/pins; a harder setup narrows widths / tucks pins.
- [x] 7.2 Setup effect test: the same field scores higher under a harder setup than a neutral one (monotonic).
- [x] 7.3 `SetupDifficulty` test: Major difficulty > Signature > Regular for the same tier; weaker tier gets an easier setup than a stronger tier at the same prestige.
- [x] 7.4 Fidelity test: a non-neutral event resolved automatically equals the simmed interactive result (competitor-for-competitor).
- [x] 7.5 Fix up world/tournament absolute-score assertions broken by the calibration; keep direction-based tests intact; confirm raw-course `ScoringCalibrationTest` still passes unchanged.

## 8. Verify

- [x] 8.1 Full backend suite green.
- [x] 8.2 Re-run the per-tier diagnostic (or a short world) to confirm the targets hold end-to-end.
