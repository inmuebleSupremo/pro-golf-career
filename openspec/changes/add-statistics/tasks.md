## 1. Statistics model

- [x] 1.1 Create framework-free package `com.progolf.sim.statistics` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `StatisticsConstants` (top-10 threshold, any record thresholds) as the single tunables surface.
- [x] 1.3 Define an immutable `EventOutcome` (season, golferId, position, scoreVsPar, madeCut, withdrawn, prize) and an immutable `StatLine` (events, cuts, wins, runnerUps, topTens, bestFinish, totalScoreVsPar, earnings) with `empty()`, `of(EventOutcome)`, `plus(EventOutcome)`, `plus(StatLine)`, and derived reads (`scoringAverage`, `cutMakeRate`).

## 2. Records, champions, comparison

- [x] 2.1 Define `SeasonStatistics` (golferId, season, StatLine) and `Championship` (season, tournamentName, tier, winnerId).
- [x] 2.2 Define `RecordType` (MOST_CAREER_WINS, LOWEST_TOURNAMENT_SCORE, MOST_CONSECUTIVE_CUTS, LONGEST_CAREER) with a direction (lower/higher is better), `RecordHolder` (type, golferId, value, season), and a `RecordBook` with `challenge(type, golferId, value, season)` (sets/keeps the better, appends progression) and `current`/`progression` reads.
- [x] 2.3 Define `CareerComparison` (two career StatLines) with leader helpers (more wins, higher earnings, better scoring average).

## 3. Archive

- [x] 3.1 Implement `StatisticsArchive`: `observeEvent(EventOutcome, tournamentName, tier, isWinner)` accumulating season/career StatLines, registering champions, updating LOWEST_TOURNAMENT_SCORE / MOST_CAREER_WINS / MOST_CONSECUTIVE_CUTS (per-golfer streak state); `observeSeason(golferId, season)` advancing career longevity (distinct seasons) and the LONGEST_CAREER record. No external mutation.
- [x] 3.2 Queries: `seasonStatistics(golferId, season)`, `careerStatistics(golferId)`, `championsOfSeason(season)`, `championshipsOf(golferId)`, `records()`, `recordProgression(type)`, `compareCareers(a, b)`.

## 4. World wiring (modified: world-progression)

- [x] 4.1 In `World`, construct one `StatisticsArchive`; add read-only accessors (`seasonStatisticsOf`, `careerStatisticsOf`, `championsOfSeason`, `records`, `recordProgression`, `compareCareers`).
- [x] 4.2 In `resolveEvent`, feed each finish via `observeEvent` (with tournament name, tier as a string, and whether it is the winner); the champion is registered from the winner.
- [x] 4.3 In `seasonalTransition`, advance the archive's career longevity for each golfer who competed this season.
- [x] 4.4 No randomness anywhere; the archive affects no outcome.

## 5. Verification

- [x] 5.1 StatLine tests: accumulation is correct (events/cuts/wins/top-10s/best finish/score/earnings); `plus` aggregates; derived reads compute; withdrawals are handled.
- [x] 5.2 Records tests: a record emerges from an outcome and references it; a surpassed record makes the new holder current and preserves the previous in progression; the record direction is respected (lowest score, most wins/cuts/longevity).
- [x] 5.3 Archive tests: seasonal statistics are preserved and never overwritten by later seasons; career statistics aggregate and remain after retirement; champions are queryable and permanent; comparison reads without mutating.
- [x] 5.4 Boundary test: the Statistics domain changes no score, ranking, or progression state (REQ-261/262); `sim.statistics` imports only `core`.
- [x] 5.5 World tests: resolving events populates statistics, champions, and records from real outcomes; a retired golfer's career statistics remain; two worlds with the same seed produce identical archives (reproducible).
- [x] 5.6 Run `openspec validate add-statistics --type change --strict` and resolve findings.
