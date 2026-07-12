## Why

A career exists only in memory: close the process and the whole world is gone. To let a player save and resume, the simulation must be able to capture its complete state as an immutable snapshot and rebuild an identical world from it — a **true state snapshot**, not a replay. This change delivers that capturability for the core (autonomous) simulation: the golfer/career/ranking/tour/finance graph and all accumulated history. Player-control state (designated golfer, pending offers, goals, schedule) is the second slice; the app-layer save file (Jackson + filesystem) is a later change. Getting the round-trip provably correct here is the foundation both build on.

## What Changes

- Add an immutable `WorldSnapshot` (and per-domain snapshot records) capturing the full state of an autonomous world: every golfer (identity, attributes, live state, career status, attribute-change log), every career (age, statistics, milestones, history, seasons, runtime state, Hall-of-Fame eligibility), finances, physical/health state, support teams, equipment inventories and loadouts, tour memberships and standings, the ranking ledger, the statistics archive, the media feed, Hall-of-Fame registry, season archives, ranking snapshots, environmental/health history, the calendar position, and the world's counters (`nextTournamentId`, `replenishCounter`, `previousNumberOne`, announced-prospects, current schedule and season results).
- Add `World.snapshot()` — guarded to a clean boundary (rejects if a player event is pending) and, in this slice, to autonomous worlds (rejects if a player is assigned; the next slice lifts that).
- Add `World.restore(masterSeed, config, WorldSnapshot)` — rebuilds an identical world, **regenerating** the seed-derived parts (course pool, weather system, stateless markets) rather than serializing them, and reconstructing the golfer graph by id (only `TournamentResult` carries live golfer references, re-linked against the rebuilt registry).
- Prove it with the determinism guarantee: **`snapshot → restore → advance N` equals `advance N`** for the same world, across several seasons.

## Capabilities

### New Capabilities
- `world-snapshot`: an autonomous world can be captured as an immutable snapshot at a clean boundary and rebuilt into an identical world, such that continuing the restored world matches continuing the original.

### Modified Capabilities
<!-- none -->

## Impact

- `com.progolf.sim.world`: new `WorldSnapshot` + `World.snapshot()` / `World.restore(...)`; the seed-derived state is regenerated on restore.
- Snapshot records added to the stateful domains that lack them: `player` (golfer/player/state), `career`, `economy` (financial account), `tour`, `ranking`, `statistics`, `media`, `equipment`, `support`, plus value snapshots for `TournamentResult`/`SeasonArchive` reconstruction. Health `PhysicalState`, `RankingSnapshot`, and the id/primitive history records are already immutable and captured directly.
- No behaviour change to a running world; snapshot/restore is a new, additive surface. No new dependencies (Jackson arrives with the app-layer persistence change).
