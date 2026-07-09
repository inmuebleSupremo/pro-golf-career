## Why

The world runs, tournaments are played, and golfers age — but every shot of every round is resolved under **`Environment.calm()`**. The shot engine already reads headwind, crosswind, and lie (and a `windResistance` skill that mitigates them), yet nothing ever fills those inputs, so wind never blows, greens never firm up, and the windy-day specialist is indistinguishable from the fair-weather one. Weather is the missing environmental layer that makes *where and when* a tournament is played matter: it turns identical fields into different tests week to week, rewards the attributes built to handle rough conditions, and gives the world texture (a brutal links gale, a record-scoring calm morning) without ever touching the rules of the game.

## What Changes

- Add a **Weather System** owned by the World that deterministically generates **Playing Conditions** for competitive play from a seasonal + course-classification climate model — world-managed, available to dependents, persisting across progression (REQ-228/234/239).
- Add the **Playing Conditions** model — wind (speed + direction), rain, temperature, humidity, ground firmness, green speed, visibility — that is **internally consistent** (rain softens ground, softens greens, dims visibility; wind and temperature cohere), **shared identically** by every competitor (no player-specific weather), and **interacts with the Course** while the Course stays authoritative (REQ-229/230/232/237).
- Play **every Tournament under defined conditions that may evolve round to round**, applied consistently to all competitors by round; expose a pre-tournament **Forecast** (a prediction, not a guarantee); and preserve **significant environmental history** (severe or record-setting conditions) (REQ-231/233/235/238).
- **Feed the shot engine** (modifies `tournament-play`): each round resolves under its Playing Conditions mapped to the shared shot `Environment` (wind decomposed into head/cross by hole, firmness/rain into lie) instead of hardcoded calm — so conditions genuinely swing scores and reward `windResistance`. Standalone tournaments default to calm, unchanged.
- **Wire weather into the World** (modifies `world-progression`): the World owns the Weather System, generates each scheduled event's weather (feeding it to the tournament), and records significant environmental history — keeping the world reproducible.

Explicitly out of scope, per REQ-236/239: the Weather domain never modifies **player attributes, rankings, tournament rules, or career progression**, and performs **no shot calculations, tournament scheduling, course generation, or ranking** — it produces conditions; consumers read them. Also deferred: player-facing forecast UI (Presentation), region-level climate geography beyond course classification, and multi-day intra-round weather change (conditions are per round in V1). Weather is deterministic — a pure function of the world seed hierarchy — so a seeded world stays reproducible bit-for-bit.

## Capabilities

### New Capabilities
- `weather-generation`: A world-owned Weather System deterministically generates Playing Conditions from a seasonal + course-classification climate model; weather is world-managed, available to dependent systems, and persists across World progression, without owning any other domain's responsibilities (REQ-228/234/239).
- `playing-conditions`: The Playing Conditions model (wind, rain, temperature, humidity, ground firmness, green speed, visibility) — internally consistent, shared identically across all competitors, interacting with the Course (Course authoritative), never generated solely to increase difficulty, and never modifying attributes/rankings/rules/careers (REQ-229/230/232/236/237).
- `tournament-weather`: Every Tournament is played under defined Playing Conditions that may evolve round to round consistently for all competitors; a pre-tournament Forecast is available as a prediction (not a guarantee); and significant environmental context is preserved as history (REQ-231/233/235/238).

### Modified Capabilities
- `tournament-play`: Each round (and playoff hole) resolves under the tournament's per-round Playing Conditions mapped to the shared shot `Environment`, instead of a hardcoded calm environment. A tournament with no supplied weather defaults to calm, so standalone events are unchanged.
- `world-progression`: The World owns the Weather System, generates each scheduled event's weather and supplies it to the tournament, and preserves significant environmental history — deterministically, keeping the world reproducible.

## Impact

- **Codebase**: New framework-free `com.progolf.sim.weather` package (Playing Conditions, climate model, Weather System, forecast, environmental record) with a single `WeatherConstants` tunables surface. `Tournament` gains a weather-aware constructor and resolves rounds under per-round conditions; `World` owns a `WeatherSystem`, generates each event's weather, and keeps an environmental-history log.
- **Determinism**: Weather is a pure function of the world seed hierarchy (per-tournament stream via `Seeds`/`SplitMix64Rng`, isolated from shot streams), so worlds remain reproducible; conditions shared across the field make results field-fair (REQ-232).
- **DAG**: `weather` depends only on `core` (RNG), `course` (classification/exposure), and `shot` (`Environment` interchange). `tournament` and `world` depend on `weather`; nothing lower imports it — no cycle.
- **Downstream consumers (future changes)**: Economy may weight prestige/prize by conditions; Media/news can surface severe-weather stories from the environmental history; the Presentation layer will show forecasts and live conditions. Health/fitness may later let conditions influence fatigue (out of scope here).
- **Boundary/risk**: Weather is descriptive state, not a rules actor — it never mutates attributes, rankings, rules, or careers; the shot engine reads the derived `Environment` and writes nothing back. The only behavioral change to existing play is that non-calm conditions now shape scores, guarded by the World's full-run reproducibility test and calibration checks.
