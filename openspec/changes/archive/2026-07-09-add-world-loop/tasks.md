## 1. Calendar & config value types

- [x] 1.1 Create framework-free package `com.progolf.sim.world` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `WorldConstants` (weeks/season, events per tier, field size, population size, course-pool size, base year) as the single tunables surface, and a `WorldConfig` record with defaults so tests can build a small world.
- [x] 1.3 Define `WorldCalendar` (season, week, base date) with `dateFor(season, week)` and week/season advance helpers.
- [x] 1.4 Define immutable `ScheduledTournament` (week, tour tier, course index, tournamentId) and a `SeasonArchive` (season, schedule + results) for history.

## 2. Tour deregistration (modified capability)

- [x] 2.1 Add `TourSystem.deregister(golferId)` — removes membership, excludes the golfer from future standings and reviews, preserves movement history.
- [x] 2.2 Test deregistration: a removed golfer has no membership, is absent from standings, and is not moved by a review; history preserved.

## 3. World bootstrap

- [x] 3.1 Implement `World.create(masterSeed, config)` — generate the population, distribute across tiers, create a `Career` per golfer, generate a course pool, register memberships, and generate the season-1 schedule; all sub-seeds via `SeedCoordinate`.
- [x] 3.2 Implement deterministic per-season schedule generation (each tier: N events spread across weeks on rotating courses); fixed for the season.

## 4. Weekly progression

- [x] 4.1 Implement `advanceWeek()` — resolve the current week's scheduled events; on the last week of a season run the seasonal transition, else advance the week.
- [x] 4.2 Implement `resolveEvent(scheduled)` — map tour tier to tournament tier; build the `TournamentDefinition` (course, date from calendar, seed coords); draw the field from the tour's active members (capped); run `Tournament.playToCompletion()`.
- [x] 4.3 Feed each result to `WorldRanking.record(...)`, `TourSystem.recordResult(...)`, and every competitor's `Career.recordTournament(...)`; archive the result. Same shared engine for all — no control-type branch.

## 5. Seasonal transition

- [x] 5.1 Implement `seasonalTransition(date)` — snapshot the ranking; run `TourSystem.reviewSeasonEnd()`; `Career.advanceSeason(date)` for every active golfer, collecting retirees.
- [x] 5.2 For each retiree: `TourSystem.deregister(id)`, mark inactive, and generate a replacement golfer + career + Development membership (`PopulationGenerator.replenish`).
- [x] 5.3 Generate the next season's schedule and advance the season index; archive the completed season.

## 6. Accessors

- [x] 6.1 Expose current season/week, the active population, tour standings, the latest ranking snapshot, and the season archives (read-only, history append-only).

## 7. Verification

- [x] 7.1 Bootstrap test: a created world has a population distributed across tiers, a course pool, careers, and a season-1 schedule.
- [x] 7.2 Week-resolution test: advancing a week with scheduled events produces winners and updates World Ranking, the tour's Season Standings, and competitors' Careers from the results.
- [x] 7.3 Player-independent test: the world advances and produces results/rankings/history with no player action.
- [x] 7.4 Seasonal-transition test: the transition takes a ranking snapshot, applies tour promotion/relegation, advances every Career by one season, replenishes departures, and generates the next schedule.
- [x] 7.5 Reproducibility test: two worlds from the same master seed advanced the same number of weeks have identical schedules, results, rankings, and tour movements.
- [x] 7.6 Continuity test: completed seasons' archives remain retrievable across further progression.
- [x] 7.7 Longevity test: over many seasons golfers retire and the population is replenished so it stays sufficient; retirees leave the standings.
- [x] 7.8 Boundary test: `sim.world` computes no shot outcomes, ranking points, or finances and has no control-type branch (source-level + behavioural checks); it only invokes domain operations.
- [x] 7.9 Run `openspec validate add-world-loop --type change --strict` and resolve findings.
