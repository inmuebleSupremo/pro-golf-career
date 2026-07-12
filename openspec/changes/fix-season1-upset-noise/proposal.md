## Why

The media feed flagged an "upset" whenever a winner's pre-event World Ranking was worse than a threshold — with an unranked winner (no position) treated as infinitely low, so *always* an upset. In the opening season the ranking is still empty/forming, so **every** winner reads as unranked and the feed floods with false upsets. An upset should mean a genuine longshot beating the field, not "the ranking has no data yet."

## What Changes

- **Gate upset news on an established ranking (world).** An upset is now reported only once the World Ranking is established — defined as at least one full season having completed (a season-ending ranking snapshot exists). During the opening season no upsets are generated; from the second season on, a low-ranked or unranked winner is a genuine upset and is reported as before.

Measured: the opening season now produces **zero** upset news (was flooded); upsets resume from the second season once the ranking reflects real form.

Explicitly out of scope: the broader looseness of the upset rule in a multi-tier world (a lower-tier event winner is often ranked outside the world top-25 and so reads as an upset even in a mature season) — making upsets rare/marquee (e.g. gating on event prestige or tier) is a separate calibration, noted as a follow-on.

## Capabilities

### Modified Capabilities
- `news-generation`: an upset (a low-ranked winner) is reported only once the ranking is established (at least one completed season), so the opening season — when every winner is still unranked — no longer generates false upsets.

## Impact

- **Codebase**: one guard in `World.feedConsumers` — the upset publish now also requires `!rankingSnapshots.isEmpty()` (a completed season of ranking). New `WorldUpsetNewsTest`.
- **Determinism**: unchanged — the guard reads existing deterministic state; same-seed worlds produce the same (now upset-free-in-season-1) feed. All other news is unaffected.
- **DAG**: unchanged.
