# Design — add-shot-level-stats

## Context

Each resolved shot already carries its `finalSurface`, `strokes`, and `distanceRemaining`; a hole's ordered shots plus its par are enough to derive the standard shot stats. They are currently discarded after each hole (only the hole's total strokes is kept). This change derives them, aggregates them per competitor, and records them — for the whole field including the interactive player.

## Decisions

### D1 — Derive stats from the shot sequence + par (no new shot output)
`HoleStats.of(List<ShotOutcome> shots, int par)`:
- **Fairway hit** — only par ≥ 4 counts (a par 3 has no drive); hit iff the tee shot's `finalSurface` is `FAIRWAY`.
- **Green in regulation** — walk the shots accumulating strokes; the ball has "reached the green" at the first shot whose `finalSurface` is `GREEN` or that holes out; GIR iff the strokes taken to that point ≤ `par − 2`.
- **Putts** — a stroke is a putt iff it is played from the green: count shots whose *starting* lie is `GREEN` (the previous shot's `finalSurface`), so the approach that lands on the green is not a putt and subsequent strokes are.
`ShotStatLine` (fairwaysHit, fairwaysPossible, greensInRegulation, holesPlayed, putts) is the immutable accumulator with `plus(HoleStats)` / `plus(ShotStatLine)` and the rate queries.

### D2 — Aggregate per competitor, capturing the interactive player too
`CompetitorStanding` holds a `ShotStatLine`. The auto path (`playCompetitorRound`) folds each hole's `HoleStats` as it resolves the round. The interactive player's rounds run through `PlayableRound`, which now accumulates its own `ShotStatLine` (from the same per-shot outcomes) and exposes `shotStats()`; `PlayableEvent` submits it to the tournament after each of the player's rounds (`addInteractiveRoundStats`). So every competitor — AI or human, played or simmed — ends with a full `ShotStatLine`, carried on their `Finish`. Because a simmed player's shots are byte-identical to the auto path, their stats match it, preserving playable-event fidelity. (The rare interactive playoff hole is not counted — playoff shots are excluded from season stats, consistent with the score model.)

### D3 — Statistics stays core-only: primitives cross the boundary
The statistics domain depends only on `core` (guarded by a boundary test), so `EventOutcome` and `StatLine` cannot import `sim.shot.ShotStatLine`. They carry the five values as `int` primitives; `StatLine` gains `drivingAccuracy()`, `greensInRegulationRate()`, and `puttsPerRound()` computed from them. The World reads `finish.shotStats()` (a `ShotStatLine`) and passes its five values into `EventOutcome` — exactly how it already passes tier/prestige as strings.

### D4 — Backward-compatible everywhere
`Finish`, `EventOutcome`, and `StatLine` keep their existing constructor shapes as convenience constructors that default the shot stats to zero (and `StatLine.of`/`empty`/`plus` extend to the new fields). Every result-level behaviour, fixture, and reproducibility summary is unchanged; withdrawn/uncounted outcomes contribute zero shot stats.

## Risks

- **Stat-definition edge cases** (hazards adding strokes, chip-ins, holes never reaching a `GREEN` surface) — handled by counting cumulative strokes to the first green-or-holed shot for GIR and by the starting-lie rule for putts; unit tests pin representative holes.
- **Boundary regression** — asserted by the existing statistics boundary test (no `shot` import) plus the primitives-in design.
- **Fidelity** — the playable-event fidelity test is extended to also compare the player's shot stats between auto and simmed-interactive resolution.
