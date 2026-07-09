## Context

Every round today is resolved with `Environment.calm()` (hardcoded in `Tournament.playCompetitorRound`/`playPlayoffHole`), even though `ShotResolver` already consumes `headWind` (mean carry), `crossWind` (lateral sigma, mitigated by a `windResistance` factor), and `lieQuality` (sigma + mishit probability). This change adds a `sim.weather` domain that generates **Playing Conditions** and maps them to that existing `Environment` interchange, then wires the World and Tournament to use them. Two locked decisions apply: the World is **weekly turn-based** (weather is generated per scheduled event) and **conditions are shared** across the field (no player-specific weather).

Framework-free, Java 21 / Spring Boot 3, deterministic and analytical: weather is a pure function of the world seed hierarchy (`Seeds`/`SplitMix64Rng`), never `java.util.Random`, `StrictMath` for any transcendental math.

## Goals / Non-Goals

**Goals:**
- A world-owned Weather System that deterministically generates Playing Conditions from seasonal phase + course classification.
- An internally consistent Playing Conditions model (wind/rain/temperature/humidity/firmness/green-speed/visibility) shared identically by all competitors.
- Conditions that evolve round to round within a tournament and feed the shared shot `Environment`, so scores move and `windResistance` matters.
- A pre-tournament forecast (prediction, not guarantee) and preserved significant environmental history.
- Wire into the World and Tournament without inverting the DAG or mutating other domains.

**Non-Goals:**
- Player-facing forecast/conditions UI (Presentation layer).
- Intra-round weather change (conditions are constant within a round in V1; they change between rounds).
- Real geographic climate beyond `EnvironmentClassification` + season phase.
- Conditions influencing fatigue/injury (Health domain, deferred) or any attribute/ranking/rule/career change (forbidden, REQ-236).
- Re-tuning the shot model itself (weather fills existing `Environment` inputs; it does not change shot math).

## Decisions

### D1. `sim.weather` is a pure generator; consumers read conditions
`WeatherSystem` (world-owned) generates a `TournamentWeather` from `(worldSeed, seasonId, tournamentId, seasonPhase, classification, rounds)`; `PlayingConditions` is immutable descriptive state. Weather imports only `core` (RNG), `course` (`EnvironmentClassification`), and `shot` (`Environment`). Nothing in `weather` imports `tournament`, `world`, `ranking`, `career`, or `player`. *Why:* REQ-236/239 domain independence — weather produces state, it is not a rules actor. *Alternative rejected:* weather reaching into Tournament/Player to apply effects — inverts the DAG and violates the boundary.

### D2. Playing Conditions map to the shot `Environment`; weather does no shot math
`PlayingConditions.environmentForHole(holeNumber, courseExposure)` returns a shot `Environment`: wind is scaled by `courseExposure` then decomposed against a **per-hole bearing synthesized deterministically from the hole number** into signed `headWind` (into/downwind) and unsigned `crossWind`; `lieQuality` is derived from ground firmness and rain. This is translation into the shot engine's input contract — no dispersion, outcomes, or scoring live here. *Why:* keeps the seam thin and testable and honours REQ-233 (adaptation) while respecting REQ-239 (no shot calculations). *Alternative rejected:* putting the mapping in `Tournament` — pushes wind geometry + firmness knowledge into the tournament and needs the same course exposure anyway. *Alternative rejected:* giving holes a real 2D bearing now — the model is "draw 2D, resolve 1D"; a synthesized per-hole bearing gives realistic head/cross variety without new course geometry, tunable later.

### D3. Conditions are shared and evolve per round, not per player
`TournamentWeather` holds one `PlayingConditions` **per round** (a deterministic sequence generated up front) plus a `Forecast`. `conditionsForRound(roundNo)` returns the shared conditions every competitor in that round experiences; the playoff uses the final round's conditions. Round-to-round evolution is a bounded random walk from the opening conditions (volatility in `WeatherConstants`). *Why:* REQ-231 (evolve, consistently by scheduling), REQ-232 (shared, field-fair), REQ-237 (follows progression, not per-shot gameplay). *Alternative rejected:* re-deriving conditions per shot/hole from RNG — would desync competitors and break shared-environment fairness.

### D4. Climate model keyed by course classification + season phase
`ClimateModel.baseConditions(classification, seasonPhase, rng)` seeds the opening conditions: each `EnvironmentClassification` carries base wind/rain/temperature tendencies (LINKS windy & cool, DESERT hot & dry & calm, PARKLAND mild, COASTAL windy & humid, etc.), and `seasonPhase` (0..1 across the season) shifts temperature and storminess. All magnitudes live in `WeatherConstants`. *Why:* REQ-234 (regions/seasons produce different, internally consistent patterns; climate contributes to course identity). *Alternative rejected:* uniform weather everywhere — erases course/season identity.

### D5. Internal consistency enforced at construction
`PlayingConditions` derives coupled fields so state is always coherent: higher `rain` lowers `groundFirmness` and `greenSpeed` and `visibility`; firmer ground raises `greenSpeed`; wind and low humidity read together. Ranges validated in the compact constructor (finite; 0–1 normalized fields; wind ≥ 0; direction in [0,360)). *Why:* REQ-229/230/237 — conditions describe a believable state and are never assembled arbitrarily. *Alternative rejected:* free/independent fields — allows nonsensical combinations (torrential rain on rock-firm fast greens).

### D6. Forecast is a deterministic perturbation of actual opening conditions
`Forecast` is generated from round 1's actual conditions plus a bounded deterministic error term (from the same weather stream), so it approximates but never equals the actuals — a prediction, not a guarantee. *Why:* REQ-238 (forecasts support preparation; predictions not guarantees). *Alternative rejected:* forecast == actuals — makes it a guarantee, contradicting the spec.

### D7. Tournament gains a weather-aware constructor; calm is the default
Add `Tournament(TournamentDefinition, TournamentWeather)`; the existing `Tournament(TournamentDefinition)` delegates with `TournamentWeather.calm()`. `playCompetitorRound`/`playPlayoffHole` read `weather.conditionsForRound(round).environmentForHole(hole, definition.course().identity().classification().exposure())`. *Why:* least-invasive wiring — no change to `TournamentDefinition`'s arity or to any existing call site or standalone-tournament behavior (REQ-231 applied where weather is supplied; calm otherwise). *Alternative rejected:* a nullable field on `TournamentDefinition` — spreads null handling; changing the record arity breaks every construction site.

### D8. World owns the Weather System and an environmental-history log
`World` constructs one `WeatherSystem(masterSeed)`; in `resolveEvent` it computes `seasonPhase` from the event week, reads the course classification, generates the `TournamentWeather`, passes it to the `Tournament`, and appends any **significant** conditions (severity ≥ threshold, or record scoring difficulty) to a `List<EnvironmentalRecord>` exposed as read-only history. Weather generation is seeded per tournament and isolated from shot streams (a dedicated salt in the coordinate chain), so the world stays reproducible. *Why:* REQ-228 (world-managed, persists), REQ-235 (world preserves significant history), REQ-231 (determined before play). *Alternative rejected:* generating weather inside `Tournament` — the Tournament is a standalone event that should not own climate/season knowledge; the World is the environmental authority.

## Risks / Trade-offs

- **[Weather could leak into a rules actor]** → `weather` never imports `tournament`/`world`/`ranking`/`career`/`player`; it returns descriptive state and a read-only `Environment`; an architecture check and a boundary test assert conditions never change attributes/rankings/rules/careers (REQ-236).
- **[Determinism across the world]** → Weather draws only from `Seeds`/`SplitMix64Rng` off the world seed hierarchy with a per-tournament salt isolated from shot streams; the existing world reproducibility test (two seeds → identical world) covers it end to end.
- **[Scores swing too far / calibration drift]** → Conditions fill the *existing* `Environment` inputs the shot model already bounds; magnitudes isolated in `WeatherConstants`; a calibration guard asserts season scoring stays in a believable band and that calm still reproduces prior behavior; tune later.
- **[Shared-environment fairness]** → `conditionsForRound` returns one shared `PlayingConditions` per round for the whole field; a test asserts two golfers in the same round see identical conditions and control type is irrelevant (REQ-232).
- **[Synthesized hole bearing is not real geometry]** → Accepted for V1 ("draw 2D, resolve 1D"); the bearing is a deterministic function of hole number giving head/cross variety; a real routing bearing can replace it later without changing the contract.

## Migration Plan

Greenfield `sim.weather` + two additive modifications (`Tournament` weather constructor; a weather step in `World.resolveEvent`). Sequencing: (1) `WeatherConstants`, `PlayingConditions` (coupled/validated) + `environmentForHole` + `calm()`; (2) `ClimateModel` (classification + season phase base conditions); (3) round-to-round evolution + `Forecast` + `TournamentWeather` + `WeatherSystem.generate(...)`; (4) `EnvironmentalRecord` + severity; (5) `Tournament` weather-aware constructor + per-round conditions in play/playoff (modifies tournament-play); (6) wire `World` — own `WeatherSystem`, generate per event, feed the tournament, log significant history (modifies world-progression); (7) tests — conditions consistent/shared/course-interacting; generation deterministic & climate-varying; forecast ≈ but ≠ actual; tournament plays under evolving conditions and calm default reproduces prior scores; world stays reproducible and logs severe weather; boundary test (no attribute/ranking/rule/career mutation). Each layer testable before the next.

## Open Questions

- Exact climate magnitudes (base wind/rain/temperature per classification, seasonal amplitude, evolution volatility, forecast error, severity threshold, hole-bearing spread) — placeholder `WeatherConstants`, tuned later against desired scoring bands.
- Units (temperature °F vs °C, wind mph vs m/s) — pick internally consistent numeric units now; Presentation formats later.
- Whether environmental history lives in `World` or is merged into Media/records — kept in `World` now; a later Media/statistics change can surface it.
- Whether conditions should feed round *scheduling* difficulty or tee-time waves — deferred; V1 shares one condition set per round for the whole field.
