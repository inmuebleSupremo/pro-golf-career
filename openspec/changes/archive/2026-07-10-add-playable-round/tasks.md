## 1. Value types

- [x] 1.1 Create framework-free package `com.progolf.sim.play` (covered by the architecture-purity test).
- [x] 1.2 Add `HoleToPlay` (HoleModel, par, Environment) — the per-hole inputs for a round.
- [x] 1.3 Add `ShotSituation` (hole number, par, shot number, strokes this hole, distance to pin, current lie Surface, reachable ShotZoneProfile) — the decision view.

## 2. PlayableRound

- [x] 2.1 Implement `PlayableRound(attributes, GolferState, List<HoleToPlay>, baseSeedCoordinate, simStrategy)` with per-shot state (current hole, remaining distance, shot number, strokes) mirroring `RoundResolver.resolveHole` (coords via `base.withHole(h).withShot(n)`; hazard = stroke-and-distance; hole out at `HOLED_THRESHOLD`; cap at `MAX_SHOTS_PER_HOLE`).
- [x] 2.2 `situation()` returns the current `ShotSituation`; `playShot(ShotDecision)` resolves one shot via `ShotResolver.resolveShot` and advances state; `isComplete()` / `currentHole()`.
- [x] 2.3 `simShot()` / `simHole()` / `simRound()` auto-play remaining shots via `StrategyPolicy.decide(remaining)`, sharing the one per-shot loop.
- [x] 2.4 Scoring: `totalStrokes()`, per-hole `holeScores()`, `scoreVsPar()`.

## 3. Verification

- [x] 3.1 A round plays to completion (sim) and reports a plausible score and 18 hole scores.
- [x] 3.2 Fidelity: a fully-simmed `PlayableRound` equals the sum of `RoundResolver.resolveHole` over the round for the same attributes, state, holes, environment, coordinate, and strategy.
- [x] 3.3 Interactive: `playShot` with a chosen decision resolves one shot and advances the situation; the situation exposes hole/par/distance/lie/reachable profile.
- [x] 3.4 Determinism: the same sequence of decisions with the same seed yields the same total.
- [x] 3.5 Skippable: `simHole()` completes the current hole; `simRound()` completes the round; mixing hand-played and simmed holes works and stays seed-continuous.
- [x] 3.6 Confirm the full suite and `ArchitecturePurityTest` still pass (`play` is framework-free).
- [x] 3.7 Run `openspec validate add-playable-round --type change --strict` and resolve findings.
