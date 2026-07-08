## Context

The shot-resolution core (archived change `add-shot-resolution-core`) is live: `com.progolf.sim.spatial` owns `ZoneBand` / `ShotZoneProfile` / `Surface`, `com.progolf.sim.shot` owns the `HoleModel` interface that `RoundResolver` consumes, and `com.progolf.sim.core` owns the deterministic seed hierarchy (`SeedCoordinate`, `Seeds`, `RngFactory`). Today nothing *produces* holes — tests hand-assemble profiles.

This change designs the Course domain (REQ-068–085): procedurally generate persistent, reproducible 18-hole courses that emit exactly those zone-band structures and implement `HoleModel`. The Course owns *environment* only; tournaments (a later change) consume courses. Stack is Java 21 / Spring Boot 3; the domain stays framework-free like the existing `sim.*` packages.

## Goals / Non-Goals

**Goals:**
- Turn a seed into a complete, valid 18-hole Course: pars, lengths, tee/green/pin, elevation, and surface/hazard layout as `ShotZoneProfile`s.
- Emit the exact `HoleModel` contract the shot engine already depends on — zero changes to `sim.shot`.
- Reproducibility by construction: same seed + generator version ⇒ identical Course (REQ-082, REQ-265/299).
- Keep identity, difficulty, and mastery as clean, separately-owned concepts with no tournament state and no attribute effects (REQ-083/085).

**Non-Goals:**
- Tournament flow, field selection, scoring — the consumer, not this domain.
- Weather generation — the Course only declares an environment *classification* others read.
- Persistence framework wiring (JPA/schema) — the domain exposes serialisable state; the Persistence change stores it.
- Realistic 2D visual geometry — generation targets the 1D-resolve contract (REQ-084).
- Final realism tuning of hole shapes and difficulty weighting — structure now, calibrate later.

## Decisions

### D1. `HoleModel` is generated per hole; a Course is a list of generated holes
`GeneratedHole` implements `shot.HoleModel` and precomputes its band layout; `Course` holds 18 of them plus identity/difficulty. *Why:* the shot engine already speaks `HoleModel`, so the Course is a producer with no adapter. *Alternative rejected:* a new richer hole interface — would force a change to `sim.shot` and break the "no modified capabilities" boundary.

### D2. Generation is a pure function of a `SeedCoordinate` course scope
Reuse the existing hierarchy: a course seed derives from `(worldSeed, seasonId, …, courseId)`; each hole derives a child seed; per-round pins derive from `(courseSeed, holeNo, roundNo)`. All generation randomness comes from `RngFactory.forCoordinate(...)`. *Why:* guarantees reproducibility and lets any hole/pin be regenerated in isolation (REQ-082). *Alternative rejected:* generate-then-store-only — loses the "reconstruct from seed" property the persistence spec wants and bloats saves.

### D3. Per-round pin is a derived overlay, not stored mutation
The hole's geometry is fixed; the active pin for round *r* is `pinFor(round)` derived deterministically and applied as an overlay when building that round's `ShotZoneProfile`/`HoleModel`. *Why:* satisfies "pin fixed within a round, may vary across rounds" (REQ-076) without mutating the immutable hole. *Alternative rejected:* storing a pin list on the hole — works, but the derived overlay keeps the hole immutable and shrinks state.

### D4. Zone-band synthesis: parameterised corridor model
Each hole is generated as a sequence of longitudinal segments (tee → landing zones → green complex); each segment emits a `ZoneBand` with a central playable surface flanked by progressively penal surfaces plus placed hazards, honoring the existing partition rules (contiguous, gap-free, resolvable). Hole archetypes (par 3/4/5, dogleg vs straight) parameterise segment counts and hazard density. *Why:* directly yields valid `ShotZoneProfile`s and keeps generation in the same 1D-resolve space the engine uses. *Alternative rejected:* 2D polygon generation then rasterise to bands — much heavier and unnecessary for the resolve model (REQ-084).

### D5. Difficulty and mastery are computed/owned outside the hole geometry
`CourseDifficulty` is a pure read-only function over the generated Course (length, hazard density, green complexity, exposure-from-classification). `CourseMastery` is a separate `(playerId, courseId) → value` record, never held by the Course. *Why:* enforces REQ-078/081/083 ownership boundaries — difficulty can't touch attributes, mastery can't leak between courses or into the Course entity. *Alternative rejected:* caching mastery on the Course — violates ownership and complicates multi-player worlds.

### D6. Environment classification is a data tag, weather stays elsewhere
`EnvironmentClassification` (enum: Links/Parkland/Desert/Mountain/Coastal/Woodland) is stored on identity and influences generation parameters (e.g., Links ⇒ more exposure, firmer defaults) but the Course emits no weather. *Why:* REQ-080/085 — the Course is read by the Weather domain, not the reverse.

## Risks / Trade-offs

- **[Generated holes could violate the shot engine's partition invariants]** → Generation runs the existing `ShotZoneProfile`/`ZoneBand` validators; add generator tests asserting every hole of many seeded courses produces valid, resolvable profiles.
- **[Generator version drift silently changes historical courses]** → Stamp a `generatorVersion` alongside the seed; reproducibility is defined as same seed *and* version; treat version bumps as explicit and never retroactive (REQ-082, persistence spec).
- **[Depends on the deferred shot-dispersion follow-up (task_a0db3f20)]** → Generation is independent of that fix; holes are valid regardless. But playtest "does a competent golfer make par?" only becomes meaningful after distance-scaled dispersion lands — note it, don't block on it.
- **[Difficulty/identity scope creep toward tournament data]** → Requirements forbid competitive fields; a persistence round-trip test asserts no leaderboard/prize/ranking data is present.
- **[Realism of generated layouts unproven]** → Keep all generation tunables in one constants surface (mirroring `SimConstants`); assert structural validity now, defer aesthetic calibration.

## Migration Plan

Greenfield addition — no rollback surface. Sequencing: (1) core value types (`Course`, `Hole`/`GeneratedHole`, `PinPosition`, `EnvironmentClassification`, identity); (2) seed-driven generation of a single hole emitting a valid `ShotZoneProfile` + `HoleModel`; (3) full 18-hole course assembly with derived par; (4) per-round pin overlay; (5) difficulty profile; (6) mastery relationship; (7) reproducibility + validity + boundary test suites. Each layer is testable before the next.

## Open Questions

- Hole archetype set and distribution (how many par 3s/4s/5s, dogleg frequency) — pick sensible defaults now, tune during calibration.
- Exact difficulty formula and weightings — deferred to calibration against target scoring once dispersion scaling (task_a0db3f20) lands.
- Course naming scheme (procedural names/regions) — cosmetic; a simple deterministic generator suffices for V1.
- How `courseId` is allocated and where `generatorVersion` is persisted — finalised with the Persistence change; this domain only needs them as inputs to reproducibility.
- Mastery growth curve and its effect magnitude — the relationship is defined here; its gameplay influence is a progression/shot-input concern for a later change.
