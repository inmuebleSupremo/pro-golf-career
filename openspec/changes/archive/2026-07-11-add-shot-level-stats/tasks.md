## 1. Shot domain: derive and accumulate

- [x] 1.1 Add `com.progolf.sim.shot.HoleStats` (fairwayEligible, fairwayHit, greenInRegulation, putts) with `of(List<ShotOutcome> shots, int par)` — fairway = par≥4 tee shot on FAIRWAY; GIR = on green/holed within par−2 strokes; putts = strokes started from GREEN.
- [x] 1.2 Add `com.progolf.sim.shot.ShotStatLine` (fairwaysHit, fairwaysPossible, greensInRegulation, holesPlayed, putts) — `empty()`, `plus(HoleStats)`, `plus(ShotStatLine)`, and `drivingAccuracy()` / `greensInRegulationRate()` / `puttsPerRound()`.

## 2. Tournament: aggregate per competitor + interactive submit

- [x] 2.1 `CompetitorStanding`: hold a `ShotStatLine`; `addHole(HoleStats)` and `addRound(ShotStatLine)`; expose it.
- [x] 2.2 `Tournament.playCompetitorRound`: for the auto path, fold `HoleStats.of(out.shots(), par)` (par from `definition.course()`) per hole into the standing.
- [x] 2.3 `Tournament.addInteractiveRoundStats(ShotStatLine)`: add to the interactive competitor's standing.
- [x] 2.4 `TournamentResult.Finish`: carry the competitor's `ShotStatLine`; keep a convenience constructor (no stats → `ShotStatLine.empty()`); `complete()` puts each standing's stats on its finish.

## 3. Playable path: player's own stats

- [x] 3.1 `PlayableRound`: accumulate a `ShotStatLine` (per hole, from the shots) and expose `shotStats()`.
- [x] 3.2 `PlayableEvent`: after each player round completes, submit `currentRound.shotStats()` via `Tournament.addInteractiveRoundStats`.

## 4. Statistics (core-only) + World

- [x] 4.1 `EventOutcome`: add `fairwaysHit, fairwaysPossible, greensInRegulation, holesPlayed, putts` (int); convenience constructor defaulting them to 0. Withdrawn/uncounted → 0.
- [x] 4.2 `StatLine`: add the five int fields; extend `empty()`/`of()`/`plus()`; add `drivingAccuracy()` / `greensInRegulationRate()` / `puttsPerRound()`.
- [x] 4.3 `World.feedConsumers`: build `EventOutcome` with the finish's shot-stat primitives (`finish.shotStats()`), passed into `observeEvent`.

## 5. Verification

- [x] 5.1 `HoleStats`: representative holes — a fairway/GIR/2-putt par; a missed fairway; a par 3 (no fairway eligibility); a 3-putt; a hazard hole (extra strokes, no GIR).
- [x] 5.2 Aggregation: a resolved event's finishes carry non-trivial shot stats; season/career `StatLine` folds them and the rate queries are sensible (0–1 for accuracy/GIR, putts/round ~ order of 30).
- [x] 5.3 Player: the interactive player (played and simmed) ends with shot stats in their finish; a simmed player's shot stats equal the auto path's (fidelity extended).
- [x] 5.4 Boundary/reproducibility: statistics still imports only `core` (boundary test); same-seed worlds byte-identical; result-level fields and existing tests unchanged.
- [x] 5.5 Full suite + `ArchitecturePurityTest` pass.
- [x] 5.6 `openspec validate add-shot-level-stats --type change --strict`.
