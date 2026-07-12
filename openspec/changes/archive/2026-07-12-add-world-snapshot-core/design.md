## Context

`World` is a mutable coordinator over ~15 owning domains (golfers, careers, finances, health, support, equipment, tours, ranking, statistics, media, Hall of Fame) plus history logs and progression counters. Almost all randomness is **re-derived from the master seed and a coordinate** (per-shot/per-event RNGs are re-seeded on each call), so there are no live RNG streams to serialize — only accumulated state and a few counters. The course pool, `WeatherSystem`, and the sponsorship/staff markets are pure functions of the seed (the markets are stateless). Only `TournamentResult` holds live golfer object references; every other history/registry record is keyed by golfer id or primitives.

## Goals / Non-Goals

**Goals:**
- Capture an autonomous world's full state as an immutable `WorldSnapshot`, and rebuild an identical world from it.
- Prove the round trip with `snapshot → restore → advance N == advance N` over several seasons.
- Keep the running world unchanged — snapshot/restore is additive.

**Non-Goals:**
- Player-control state (designated golfer, pending offers, goals, schedule, paused event) — the next slice. This slice's `snapshot()` rejects a player world.
- Serialization format / files / Jackson — the app-layer `add-persistence` change. This slice produces pure in-memory snapshot records.
- Mid-event capture — rejected by the clean-boundary guard.

## Decisions

**Decision: regenerate the seed-derived parts on restore.** The course pool, `WeatherSystem`, and the stateless markets are not stored in the snapshot; `restore` rebuilds them from `(masterSeed, config)` exactly as `create` does. This shrinks the snapshot to genuinely accumulated state and keeps it small. Everything reproducible from the seed is reproduced, not persisted.

**Decision: explicit per-domain immutable snapshot records; `World` composes them.** Each stateful domain that lacks immutability gains a `Snapshot` record and a way to capture/rebuild it (a `snapshot()` method and a `restore(snapshot)` static factory, or reconstruction by the owner). Domains already immutable (`PhysicalState`, `RankingSnapshot`, and the id/primitive history records) are stored directly. `WorldSnapshot` is a record aggregating them plus the World's own bookkeeping. This matches the locked persistence design and gives the app layer clean, serializable value types.

**Decision: reconstruct the golfer graph by id, in dependency order.** `restore` proceeds: build the world shell (seed, config, calendar at the captured position, regenerated weather/courses/markets) → rebuild every `ProfessionalGolfer` from its snapshot into the registry → rebuild each `Career` bound to its rebuilt player → refill the per-golfer maps (finances, health, support, equipment, loadouts) → rebuild the registries (tours, ranking, statistics, media, Hall of Fame) → rebuild the history that references golfers. Because only `TournamentResult` references golfers, a `TournamentResultSnapshot` captures finishes by golfer id and rebuilds them against the registry; `SeasonArchive` (which contains results) is rebuilt the same way.

**Decision: `snapshot()` guards the clean boundary and, for this slice, autonomy.** It throws if a player event is pending (no half-played event to encode) and, in this slice only, if a player is assigned (player-control capture lands next). The next slice removes the autonomy guard.

**Decision: the determinism test is the spec.** A helper advances two worlds — one straight, one via `snapshot → restore` — by the same N and asserts observable equality (a structured digest over rankings, tour standings, careers/statistics, finances, media, Hall of Fame, calendar). This both proves fidelity and documents exactly what "identical" means. Building the digest also surfaces any un-captured state as a diff, guiding implementation domain by domain.

## Risks / Trade-offs

- **Missed state → silent divergence.** Mitigated by the digest-based determinism test run over several seasons and multiple seeds; any omission shows as a post-advance diff. Implementation is incremental: extend the digest and the snapshot together until the round trip is green.
- **Reconstruction ordering bugs (dangling references).** Mitigated by the fixed dependency order above and by rebuilding golfer-referencing records last, by id, against the completed registry.
- **Regeneration must match exactly.** Courses/weather/markets are pure functions of `(seed, config)`; regenerating them on restore is byte-identical to the original by construction (same code path as `create`). Guarded implicitly by the determinism test.
- **Large surface.** The change touches many domains, but each addition is a mechanical, well-scoped snapshot record; the test gates correctness continuously.
