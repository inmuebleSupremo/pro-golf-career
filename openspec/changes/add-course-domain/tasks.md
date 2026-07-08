## 1. Domain value types

- [ ] 1.1 Create framework-free package `com.progolf.sim.course` (consistent with existing `sim.*`; covered by the architecture-purity test).
- [ ] 1.2 Define `EnvironmentClassification` enum (Links, Parkland, Desert, Mountain, Coastal, Woodland).
- [ ] 1.3 Define `CourseIdentity` (id, name, region, classification, style) and a `PinPosition` type.
- [ ] 1.4 Define `Hole` contract and a `GeneratedHole` record that implements `com.progolf.sim.shot.HoleModel` and carries number, par, length, tee, green, surface/hazard bands, elevation.
- [ ] 1.5 Define `Course` (18 holes + identity) with derived total par; reject any course not having exactly 18 ordered holes.

## 2. Seed-driven generation

- [ ] 2.1 Add `CourseGenConstants` — the single tunables surface for generation (segment counts, hazard density, archetype mix), mirroring `SimConstants`.
- [ ] 2.2 Implement course/hole/pin `SeedCoordinate` derivation using the existing hierarchy (`Seeds`/`RngFactory`); no ambient randomness.
- [ ] 2.3 Implement the corridor/segment zone-band synthesiser: generate a hole's `ZoneBand`s (central playable surface + progressively penal flanks + placed hazards) honoring existing partition rules.
- [ ] 2.4 Implement hole archetypes (par 3/4/5, straight/dogleg) parameterising segment counts, length, and hazard density.
- [ ] 2.5 Implement `GeneratedHole.zoneProfileFor(remainingDistance)` returning a valid, resolvable `ShotZoneProfile` for any remaining distance.

## 3. Course assembly & pins

- [ ] 3.1 Implement `CourseGenerator.generate(seedCoordinate, classification)` producing a complete 18-hole `Course` with derived par.
- [ ] 3.2 Implement per-round pin derivation (`pinFor(round)`) as a deterministic overlay; hole geometry stays immutable.
- [ ] 3.3 Ensure a Course fixed for a tournament is immutable apart from per-round pin variation.
- [ ] 3.4 Stamp `generatorVersion` alongside the seed so reproducibility is defined as (seed + version).

## 4. Identity, difficulty & mastery

- [ ] 4.1 Implement deterministic course identity generation (name/region/style) from the seed; classification drives generation parameters (exposure/firmness defaults) but emits no weather.
- [ ] 4.2 Implement `CourseDifficulty` as a pure read-only function of length, surfaces, hazards, green complexity, and exposure; never touches attributes.
- [ ] 4.3 Implement `CourseMastery` as a separate `(playerId, courseId) -> value` relationship, owned outside `Course`, with per-player isolation and cross-season persistence semantics.

## 5. Verification

- [ ] 5.1 Reproducibility tests: same (seed, version) regenerates an identical Course; regenerating a single hole/pin in isolation matches.
- [ ] 5.2 Validity tests: across many seeded courses, every hole's `zoneProfileFor(...)` passes the existing `ShotZoneProfile`/`ZoneBand` partition validation and every landing resolves to exactly one surface.
- [ ] 5.3 Composition tests: exactly 18 numbered holes; derived total par equals sum of hole pars.
- [ ] 5.4 Pin tests: pin fixed within a round; pins may differ across rounds and are each reproducible.
- [ ] 5.5 Boundary tests: Course/identity/persisted form contains no tournament data (leaderboard/prize/ranking/competitor/score); difficulty never mutates attributes; mastery never leaks between courses.
- [ ] 5.6 Integration smoke test: resolve a hole end-to-end via the existing `RoundResolver` against a generated `HoleModel` (no change to `sim.shot`).
- [ ] 5.7 Run `openspec validate add-course-domain --type change --strict` and resolve findings.
