## Why

The archive records only *result-level* statistics — events, cuts, wins, top-10s, scoring, earnings. The **shot-level** stats that make a golf world feel real and give a career texture — **driving accuracy** (fairways hit), **greens in regulation**, **putts per round** — were deferred because they "need tournament/shot changes." Every shot is already resolved with a surface and a stroke count, so the data exists; it is simply thrown away after each hole. This change captures it: for the whole field including the player, the sim now derives per-hole shot stats from the resolved shots, aggregates them per competitor, and records them into season and career statistics with queryable rates. It deepens the living world (Pillar 3/4) and gives the player a real picture of their own game — and, since it adds persistent statistics state, it is cheaper to build now, before the persistence work captures that state.

## What Changes

- **Derive shot stats from resolved shots (shot domain).** Add `HoleStats.of(shots, par)` computing, for one hole: fairway hit (a par-4/5 tee shot at rest on the fairway), green in regulation (the ball on the green within `par − 2` strokes), and putts (strokes taken from the green). Add `ShotStatLine`, an immutable accumulator (fairways hit / fairways possible / greens in regulation / holes played / putts) with `drivingAccuracy()`, `greensInRegulationRate()`, and `puttsPerRound()`.
- **Aggregate per competitor (tournament).** `CompetitorStanding` accumulates a `ShotStatLine`; the auto path computes each hole's stats as it resolves the round; the **interactive player submits their round's stats** (from `PlayableRound`, which now emits them) so the protagonist's own driving/GIR/putts are captured — including when they sim. Each `TournamentResult.Finish` carries the competitor's `ShotStatLine`.
- **Record into the archive (statistics, core-only).** `EventOutcome` and `StatLine` gain five primitive shot-stat fields (kept as ints so the statistics domain stays core-only — the World feeds primitives from the finish's `ShotStatLine`), folded across seasons and career; `StatLine` gains `drivingAccuracy()`, `greensInRegulationRate()`, and `puttsPerRound()`.
- Backward-compatible constructors throughout (`Finish`, `EventOutcome`, `StatLine`) default the shot stats to zero, so every result-level behaviour and existing fixture is unchanged.

Explicitly out of scope: sand saves, scrambling, proximity-to-hole, strokes-gained, or per-club stats (a first, standard set only); exposing the stats through GraphQL/UI (later); the interactive **playoff** hole's stats (rare; playoff shots are not counted).

## Capabilities

### Modified Capabilities
- `competitive-statistics`: the maintained statistics additionally include shot-level performance — driving accuracy, greens in regulation, and putts per round — accumulated per golfer across seasons and career.
- `tournament-completion`: each competitor's finishing record additionally carries their shot-level statistics for the event, so the World can record them.

## Impact

- **Codebase**: new `sim.shot.HoleStats` + `ShotStatLine`; modified `CompetitorStanding`/`Tournament` (accumulate + interactive submit), `TournamentResult.Finish` (+shot stats), `EventOutcome`/`StatLine` (+5 ints + queries), `PlayableRound` (emit stats) + `PlayableEvent` (submit them), `World.feedConsumers` (feed the primitives). No change to shot mathematics or scoring.
- **Determinism / boundary**: stats are a pure function of the already-deterministic shots, so nothing about resolution changes and same-seed worlds stay byte-identical (result-level fields untouched; shot stats added). Statistics stays core-only (primitives in, no `shot` import). A simmed player's shot stats equal the auto path's (same shots), preserving playable-event fidelity.
- **DAG**: `tournament`/`play` already depend on `shot`; `world` feeds `statistics` primitives as today. No inversion.
