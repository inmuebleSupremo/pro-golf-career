## Context

`HallOfFame.evaluate(CareerStatistics)` runs once at retirement with flat OR-thresholds and no selectivity. Two facts make a richer model cheap: (1) where results are recorded (`World.feedConsumers`) both the `EventPrestige` and the tour `Tier` are already in scope, so wins can be classified by prestige and tier at fold time; (2) retired careers persist in the World's `careers` map (only `activeGolfers` drops them), so an election can consider active and retired golfers alike.

## Goals / Non-goals

- **Goals**: a two-phase (eligibility → biennial election) HoF that is selective (one inductee per cycle), prestige-driven (majors ≫ signature > regular > development), deterministic, and reflected in the `HALL_OF_FAME` goal via actual induction.
- **Non-goals**: inducting nobody in a weak cycle, multi-inductee classes, earnings/ranking-weeks weighting, un-induction, a pre-election public ballot view.

## Decisions

### D1 — Classify wins by (prestige, tier) at record time; keep the career pure
`CareerStatistics` gains `signatureWins` and `developmentWins` alongside the existing `majorsWon`; each win is bucketed by priority — **major → signature → development-tier → regular** — so the buckets are disjoint and regular pro wins are derived (`wins − majors − signature − development`). `Career.recordTournament` takes the tour `Tier` (career already depends on `tournament` for `TournamentResult`/`EventPrestige`); backward-compatible overloads default the tier to `STANDARD` so existing call sites and fixtures are unchanged. **Alternative** (store a pre-weighted score) rejected — keeping counts lets the score weights live in `CareerConstants` and be tuned without touching stored state (important for the upcoming persistence work).

### D2 — Baseline uses professional wins; "Pro Tour wins" excludes the development (amateur) tier
Phase-1 statistical gate is `proWins ≥ HOF_MIN_PRO_WINS (15)` where `proWins = totalWins − developmentWins`, AND `majorsWon ≥ HOF_MIN_MAJORS (2)`. The status gate is `age ≥ HOF_MIN_AGE (45)` OR `seasonsSinceRetirement ≥ HOF_RETIRED_SEASONS (3)`. Majors floor is 2 (1 felt too shallow). These are `CareerConstants`, tunable.

### D3 — Credentials record decouples the pure evaluator from where the data lives
`HallOfFame.meetsBaseline(HallOfFameCredentials)` and `score(HallOfFameCredentials)` take a primitive record (majors, signatureWins, totalWins, developmentWins, age, seasonsSinceRetirement, retired). The career supplies the stat/age fields; only the World knows `seasonsSinceRetirement`, so it fills that in at election time. Career-retirement records baseline eligibility with `seasonsSinceRetirement = 0` (age 65 ≥ 45 already satisfies status). Score = `W_MAJOR·majors + W_SIGNATURE·signature + W_REGULAR·regularPro + W_DEVELOPMENT·development` with `W_MAJOR ≫ W_SIGNATURE > W_REGULAR > W_DEVELOPMENT`.

### D4 — The World owns the election; it is deterministic and runs on the season cycle
In `seasonalTransition`, after retirements/replenishment and before the goal check, if `season % HOF_ELECTION_CYCLE_SEASONS == 0` the World: builds credentials for every career not already inducted, filters by `meetsBaseline`, and if any qualify inducts the single highest `score` (ties broken by golfer id for determinism), recording a pure `HallOfFameInduction(golferId, season, score)` in a registry and publishing `NewsFactory.hallOfFameInduction`. No RNG — the election is a pure function of recorded results, so same-seed worlds induct identically. Retirement-season is tracked in a `Map<String,Integer>` populated when a career retires, to derive `seasonsSinceRetirement`.

### D5 — The `HALL_OF_FAME` goal means inducted, not eligible
`World.evaluateGoal` for `HALL_OF_FAME` now returns 1 iff the golfer is in the registry, matching the spec (a marquee lifetime achievement, not mere qualification).

## Risks / Trade-offs

- **World outcomes are unchanged for non-HoF behaviour**: win classification adds fields but does not change scoring/ranking/economy; the election reads state and only publishes news + registry entries. Same-seed reproducibility holds (deterministic, no RNG). Existing HoF unit tests change because the criteria are deliberately stricter (majors now required alongside wins).
- **"Pro Tour wins" interpretation**: excluding development-tier wins from the baseline is a judgement call; centralised in one `proWins` derivation and tunable if it proves too strict/lax.
- **Growing `careers` map**: the election iterates all careers ever created (retired included). This is O(golfers) once per two seasons — negligible — and already how the map behaves; no new unbounded growth is introduced.
- **Deferred richness**: earnings and ranking-weeks (career-high ranking, seasons at #1) are natural future score inputs; the credentials record is the extension point.
