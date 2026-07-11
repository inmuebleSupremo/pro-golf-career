## Why

Hall-of-Fame eligibility is a placeholder: flat OR-thresholds (`wins≥15` OR `wins≥8 + top-10s≥40` OR `majors≥3`) evaluated once, automatically, at retirement. It has no sense of prestige, no selectivity, and no ceremony — every qualifying career is "in," instantly, forever. The `HALL_OF_FAME` career goal now surfaces this, so it deserves a real system. Replace it with a two-phase, prestige-driven model inspired by the World Golf Hall of Fame but tuned to this game's majors + event-prestige + tour-tier structure.

## What Changes

- **Phase 1 — baseline eligibility (ballot qualification).** A golfer is *nominable* only if they meet BOTH a **status** gate (competitive age ≥ 45 **OR** retired for ≥ 3 seasons) and a **statistical** gate (≥ 15 professional-tour wins **AND** ≥ 2 majors). Meeting the baseline earns a spot on the ballot — not induction. `HallOfFame.meetsBaseline(credentials)` is a pure function of career primitives; a retiring career still records its baseline eligibility.
- **Phase 2 — biennial election (the prestige mechanic).** Every 2 in-game seasons the World runs an election: it gathers every not-yet-inducted golfer meeting Phase 1 (**active or retired**), scores each by career weight, and inducts only the **single top candidate** that cycle. Everyone else stays on the ballot for the next cycle. The induction is permanent and announced through the media feed.
- **Prestige-weighted HoF score.** `HallOfFame.score(credentials)` weights a career by achievement prestige: **majors ≫ signature (high-importance) events > regular pro wins > development-tier (amateur) wins**, so dominance in the biggest events decides the ballot. Ties break deterministically by golfer id.
- **Richer win tracking.** `CareerStatistics` now classifies each win by prestige and tier (majors, signature wins, development-tier wins — regular pro wins derived), folded at record time from the `(prestige, tier)` already available where results are recorded. Backward-compatible `recordTournament` overloads keep existing call sites intact.
- **Goal + registry.** The `HALL_OF_FAME` career goal is now satisfied by actual **induction** (registry membership), not mere eligibility. The World owns the Hall registry and exposes it (members, inductions) through `WorldService`.

Explicitly out of scope: a voting-percentage/threshold to induct nobody in a weak cycle (the top eligible candidate is always inducted); multi-inductee classes; a public ballot view before election; weighting by earnings or ranking-weeks (career-high ranking / seasons at #1 remain a later refinement, noted in the design); un-induction.

## Capabilities

### Modified Capabilities
- `career-legacy`: Hall-of-Fame status is no longer an automatic retirement evaluation. It becomes a two-phase system — a baseline eligibility gate (status + statistics) that qualifies a golfer for the ballot, followed by a biennial election that inducts the single highest-scored eligible golfer (active or retired) per cycle, by a prestige-weighted career score.

## Impact

- **Codebase**: `CareerStatistics` (+signature/development win classification), `Career.recordTournament` (+tier param, back-compat overloads), `CareerConstants` (baseline + score-weight + cycle tunables), `HallOfFame` (`HallOfFameCredentials`, `meetsBaseline`, `score`; retirement records baseline eligibility) + a pure `HallOfFameInduction` record. World gains the biennial election in `seasonalTransition`, a Hall registry, retirement-season tracking, induction news (`NewsType.HALL_OF_FAME_INDUCTION`), and the goal rewire; `WorldService` exposes the Hall. New `HallOfFameTest` + `WorldHallOfFameTest`; `CareerLegacyTest` / `CareerMajorsTest` updated for the stricter baseline.
- **Determinism**: the election is a pure, deterministic function of recorded stats (score + id tie-break) run on the season cycle — no RNG, reproducible. Retired careers already persist in the `careers` map (only removed from `activeGolfers`), so they remain electable.
- **Boundary/DAG**: `HallOfFame`/`CareerStatistics` stay in `career`, which already depends on `tournament` (`TournamentResult`, `EventPrestige`); adding `Tier` is consistent. The election lives in `world`, which composes every domain. No inversion.
