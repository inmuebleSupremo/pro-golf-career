## 1. Playing Conditions model

- [x] 1.1 Create framework-free package `com.progolf.sim.weather` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [x] 1.2 Add `WeatherConstants` (base wind/rain/temperature per classification, seasonal amplitude, evolution volatility, firmness/rain→lie coupling, wind→exposure scaling, forecast error, severity threshold, per-hole bearing spread) as the single tunables surface.
- [x] 1.3 Define an immutable `PlayingConditions` (wind speed + direction, rain, temperature, humidity, ground firmness, green speed, visibility) with a compact constructor validating finiteness and ranges (0–1 normalized fields; wind ≥ 0; direction in [0,360)); include a `calm()` factory and a derived `severity()` read.
- [x] 1.4 Enforce internal consistency: derive coupled fields so rain softens ground, slows greens, and does not improve visibility; firmer ground raises green speed (REQ-229/230/237).
- [x] 1.5 Implement `PlayingConditions.environmentForHole(holeNumber, courseExposure)` → shot `Environment`: scale wind by exposure, decompose against a deterministic per-hole bearing into signed `headWind` + unsigned `crossWind`, and derive `lieQuality` from firmness/rain. No shot math here (REQ-233/239).

## 2. Climate & generation

- [x] 2.1 Implement `ClimateModel.baseConditions(classification, seasonPhase, rng)` — per-`EnvironmentClassification` base tendencies shifted by season phase, internally consistent (REQ-234).
- [x] 2.2 Implement round-to-round evolution: a bounded deterministic random walk from the opening conditions producing one `PlayingConditions` per round (REQ-231/237).
- [x] 2.3 Define `Forecast` as a deterministic bounded perturbation of round 1's actual conditions — approximates but never equals actuals (REQ-238).
- [x] 2.4 Define `TournamentWeather` (per-round conditions + forecast) with `conditionsForRound(roundNo)` (playoff uses the final round), `forecast()`, `severity()`, and a `calm()` factory.
- [x] 2.5 Implement `WeatherSystem` (world-owned; seeded from the master seed) with `generate(seasonId, tournamentId, seasonPhase, classification, rounds)`; isolate the weather stream from shot streams via a dedicated salt in the seed chain (REQ-228/239).
- [x] 2.6 Define an immutable `EnvironmentalRecord` (season, tournament, severity, summary) for significant environmental history (REQ-235).

## 3. Tournament wiring (modified: tournament-play)

- [x] 3.1 Add `Tournament(TournamentDefinition, TournamentWeather)`; make the existing 1-arg constructor delegate with `TournamentWeather.calm()`.
- [x] 3.2 Resolve each round under `weather.conditionsForRound(round).environmentForHole(hole, course.identity().classification().exposure())` in `playCompetitorRound`; use the final round's conditions in `playPlayoffHole`.
- [x] 3.3 Confirm the calm default reproduces prior standalone-tournament scores (no behavioural change without weather).

## 4. World wiring (modified: world-progression)

- [x] 4.1 In `World`, construct one `WeatherSystem(masterSeed)`.
- [x] 4.2 In `resolveEvent`, compute the season phase from the event week, read the course classification, generate the `TournamentWeather`, and pass it to the `Tournament`.
- [x] 4.3 Append significant conditions to a `List<EnvironmentalRecord>` exposed as a read-only environmental-history accessor.

## 5. Verification

- [x] 5.1 Playing-conditions tests: the model exposes all listed conditions; coupled fields cohere (rain → softer/slower/not-clearer); conditions are shared identically for two golfers in the same round (REQ-229/230/232/237).
- [x] 5.2 Environment-mapping tests: strong wind measurably affects resolution and rewards higher `windResistance`; different holes get different head/cross splits; calm maps to the prior calm `Environment` (REQ-233).
- [x] 5.3 Generation tests: same seed → identical conditions; the weather stream does not perturb shot resolution; classification and season phase produce consistently different climates (REQ-228/234/239).
- [x] 5.4 Forecast test: the forecast approximates but does not equal the actual conditions (REQ-238).
- [x] 5.5 Tournament tests: a tournament plays under evolving per-round conditions applied to the whole field; the no-weather constructor reproduces prior scores exactly (REQ-231).
- [x] 5.6 Boundary test: generating and consuming weather changes no Player Attribute, Ranking, Tournament rule, or Career value (REQ-236); `sim.weather` imports no higher domain.
- [x] 5.7 World tests: events are played under generated weather; two worlds with the same seed produce identical weather and play (reproducible); severe conditions are recorded to environmental history (REQ-228/235).
- [x] 5.8 Run `openspec validate add-weather --type change --strict` and resolve findings.
