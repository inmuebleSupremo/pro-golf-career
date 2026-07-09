## Context

Nine domains expose capabilities but nothing drives them: `Career.advanceSeason`, `TourSystem.reviewSeasonEnd`, and ranking snapshots sit idle, and AI events never happen on their own. This change adds `sim.world`: the top-level coordinator that owns a calendar, advances week by week, resolves scheduled events automatically through the shared Tournament engine, feeds results into ranking/tours/careers, and runs a seasonal transition firing every seam. It is a pure composition over existing public APIs — the largest integration surface in the project.

Framework-free, Java 21 / Spring Boot 3, deterministic from a world master seed. Progression/aging of attributes and Postgres persistence stay deferred.

## Goals / Non-Goals

**Goals:**
- One `World` that bootstraps a population, courses, tours, careers, and a season-1 schedule from a master seed.
- Weekly `advanceWeek()` that auto-resolves scheduled events and feeds results to World Ranking, Tour Season Standings, and each Career.
- A seasonal transition: ranking snapshot → tour promotion/relegation → career advance/retire → population replenishment → next schedule.
- Player-independent, reproducible-from-seed, identical-rules progression; permanent history.
- A pure coordinator — no shot/ranking/finance/UI logic here.

**Non-Goals:**
- Attribute progression/aging (Careers age; attributes don't evolve — later Progression domain).
- Postgres persistence (World exposes serialisable state; later Persistence domain).
- A UI / the human's interactive per-shot loop (all competitors resolved headlessly).
- Calendar realism (a simple deterministic schedule suffices for V1).

## Decisions

### D1. `World` is a pure coordinator holding the composed subsystems
It owns: the master seed, a `WorldCalendar` (season + week + base date), a course pool, a `TourSystem`, a `WorldRanking`, `Map<golferId, ProfessionalGolfer>`, `Map<golferId, Career>`, the current season schedule, and archives. Every progression step invokes a domain's public operation. *Why:* REQ-101/112 — the World coordinates and preserves continuity without reimplementing domain work. *Alternative rejected:* folding domain logic into the World — violates the boundary and single ownership.

### D2. Deterministic bootstrap from the master seed
`World.create(masterSeed, config)` generates the initial population (`PopulationGenerator`), distributes golfers across tiers, creates a `Career` per golfer (start age = the golfer's age, already 16–22), generates a course pool (`CourseGenerator`), registers memberships in the `TourSystem`, and generates the season-1 schedule. All sub-seeds derive from the master seed via `SeedCoordinate`. *Why:* REQ-299 reproducibility; a `WorldConfig` (population size, weeks/season, events, field size) lets tests build a small fast world. 

### D3. Season schedule is a deterministic list of allocated events
A `ScheduledTournament(week, TourTier, courseIndex, tournamentId)` list is generated per season from a season seed: for each tier, spread N events across the season's weeks on rotating courses. Fixed for the season, archived on completion. *Why:* REQ-101/106; every event has exactly one tour and one course.

### D4. `advanceWeek()` resolves that week's events; the last week triggers the transition
`advanceWeek()` finds scheduled events for the current week and resolves each; if the week is the last of the season it runs the seasonal transition and rolls to season+1 week 1, else week+1. *Why:* REQ-105 weekly turns, REQ-102/108 the world runs itself.

### D5. Event resolution composes the tournament engine and feeds all consumers
`resolveEvent`: map `TourTier`→`tournament.Tier`; build a `TournamentDefinition` (course, tier, date from calendar, seed coords); draw the field from the tour's active members (excluding retired), capped at field size; run `Tournament.playToCompletion()`; then feed the `TournamentResult` to `WorldRanking.record(...)`, `TourSystem.recordResult(...)`, and each competitor's `Career.recordTournament(...)`; archive the result. *Why:* REQ-109/104/110 — one shared engine, one result updating every consumer.

### D6. Seasonal transition fires every seam in a fixed order
`seasonalTransition(date)`: (1) snapshot `WorldRanking.rankingAsOf(date)`; (2) `TourSystem.reviewSeasonEnd()`; (3) `Career.advanceSeason(date)` for every active golfer, collecting those who retire; (4) for each retiree, `TourSystem.deregister(id)` and generate a replacement golfer + career + membership (`PopulationGenerator.replenish`); (5) generate next season's schedule; (6) advance the season index. *Why:* REQ-106; ordering matters — golfers retiring this season still participated in this season's review before leaving.

### D7. `TourSystem.deregister` is the one additive extension (modified capability)
A living world retires golfers; without removal they would pollute future standings/reviews (0-point retirees getting "relegated"). `TourSystem.deregister(golferId)` removes membership and excludes them from standings/reviews while preserving movement history. *Why:* required by REQ-125/107; kept additive and specified as a `tour-membership` delta.

### D8. Fields drawn from active tour members only
The World tracks active (non-retired) golfers; a field is the tour tier's members intersected with active golfers, ordered deterministically, capped at field size. Retired golfers never appear. *Why:* correctness of the living world; avoids scheduling the departed.

### D9. Dates come from the calendar, feeding ranking and career
`WorldCalendar.dateFor(season, week)` maps each turn to a `LocalDate` (base year + season/week offset). These flow into `ranking.record(date)` and `career.recordTournament(date)` so decayed rankings and career history are correctly dated. *Why:* the downstream domains are date-driven; the calendar is the single time source.

## Risks / Trade-offs

- **[Largest integration surface — a bug implicates many domains]** → Keep the World a thin coordinator invoking only public operations; test a full season and a multi-season run end-to-end (reproducibility + seam firing) and assert no domain is reimplemented.
- **[Performance of many events × full tournaments]** → A season is a few thousand hole resolutions per event; the shot core is allocation-light. A `WorldConfig` keeps test worlds small; the retirement test uses a tiny world advanced many seasons.
- **[Retirement/replenishment correctness]** → Deregister on retirement + replenish at a fresh population index; assert the active population stays sufficient across many seasons and retirees leave standings.
- **[Schedule/field constants uncalibrated]** → Isolate in `WorldConstants`/`WorldConfig`; assert structural correctness (events resolve, consumers update, transition fires), not magnitudes.
- **[Career start age vs. world time]** → Career Age is the gameplay clock advanced by the transition, independent of the golfer's DOB; the two are not conflated.

## Migration Plan

Greenfield addition (+ one additive `TourSystem.deregister`). Sequencing: (1) `WorldConstants` + `WorldConfig` + `WorldCalendar` + `ScheduledTournament` + `SeasonArchive`; (2) `TourSystem.deregister` and its delta spec; (3) `World.create` bootstrap (population/tiers/careers/courses/memberships/season-1 schedule); (4) schedule generation; (5) `advanceWeek` + `resolveEvent` composing the tournament engine and feeding ranking/tour/career; (6) `seasonalTransition` firing all seams + replenishment + next schedule; (7) accessors (current season/week, ranking snapshot, tour standings, archives); (8) test suites — bootstrap, week resolution feeds consumers, transition fires every seam, reproducibility from seed, historical continuity, multi-season retirement/replenishment, coordination boundary (no shot/points/finance/UI in `sim.world`). Each layer testable before the next.

## Open Questions

- Weeks per season, events per tier, field size, population size — placeholder `WorldConstants` defaults; a `WorldConfig` lets tests and later tuning vary them.
- Course-pool size and rotation — a small pool reused across events for V1; richer venue variety later.
- How the human player selects into events vs. headless resolution — resolved headlessly here; the UI/presentation change adds player choice on top.
- Whether replenishment reuses retired golfers' tiers or always enters at Development — enter replacements at the lowest tier (a clean development pathway); revisit with the economy/objectives.
