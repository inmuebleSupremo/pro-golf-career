# course-generation Specification

## Purpose
TBD - created by archiving change add-course-domain. Update Purpose after archive.
## Requirements
### Requirement: Course Composition

A Course SHALL consist of exactly eighteen Holes numbered 1 through 18. The Course's total par SHALL be derived from its holes and never stored independently. Hole ordering SHALL be immutable during tournament play.

#### Scenario: Course has eighteen ordered holes

- **WHEN** a Course is generated
- **THEN** it SHALL contain exactly eighteen Holes numbered 1..18 with no gaps or duplicates

#### Scenario: Par is derived, not stored

- **WHEN** a Course's total par is requested
- **THEN** it SHALL equal the sum of its holes' pars, computed from the hole definitions

### Requirement: Hole Definition

Every Hole SHALL expose the information required for strategic play: hole number, par, length, tee location, green location, active pin position, surface layout, hazard layout, and elevation profile. Every playable location on a hole SHALL belong to exactly one Surface from the existing catalogue.

#### Scenario: Hole exposes required fields

- **WHEN** a generated Hole is inspected
- **THEN** its number, par, length, tee, green, pin, surface layout, hazard layout, and elevation SHALL all be present

#### Scenario: Every location maps to one catalogue surface

- **WHEN** any playable location on a hole is resolved
- **THEN** it SHALL map to exactly one Surface from the defined 13-surface catalogue, and no other surface source SHALL exist

### Requirement: Zone-Band Emission

Course generation SHALL express each hole's surface and hazard layout as the existing `ShotZoneProfile` / `ZoneBand` structures, and SHALL provide a `HoleModel` for each hole that yields the reachable zone profile for any remaining distance. Generated profiles SHALL satisfy the existing partition rules (contiguous, gap-free, every landing resolvable).

#### Scenario: Generated profile is a valid partition

- **WHEN** a hole's zone profile is produced for a shot context
- **THEN** it SHALL be a contiguous, gap-free partition of the reachable range in which every sampled landing resolves to exactly one surface

#### Scenario: Course emits the HoleModel the shot engine consumes

- **WHEN** the shot engine requests a hole's zone profile for a remaining distance
- **THEN** the Course SHALL supply it through the same `HoleModel` contract the resolver already depends on, requiring no change to the shot engine

### Requirement: Deterministic Generation

A Course SHALL be generated deterministically from a seed obtained through the world seed hierarchy. Regenerating a Course from the same seed and generator version SHALL produce an identical Course. Generation SHALL NOT use any ambient randomness.

#### Scenario: Same seed reproduces the same course

- **WHEN** a Course is generated twice from the same seed and generator version
- **THEN** the two Courses SHALL be identical in every generated field

#### Scenario: All randomness routes through the seed hierarchy

- **WHEN** generation requires a random value
- **THEN** it SHALL derive it from the world seed hierarchy, and SHALL NOT read any global or ambient random source

### Requirement: Per-Round Pin Positions

Every Hole SHALL have exactly one active pin position, which SHALL remain fixed for the duration of a completed round. Pin positions MAY differ between rounds of the same tournament, derived deterministically per round. The pin's **depth** (front-to-back placement) SHALL position the green complex off-centre from the pin so that front and back pins play differently: a back pin SHALL leave less green behind it (the over-green trouble closer, so going long is punished), and a front pin SHALL leave less green in front of it (the run-up shorter, so coming up short is punished). A centre pin SHALL leave the green symmetric about it.

#### Scenario: Pin is fixed within a round

- **WHEN** a round is in progress on a hole
- **THEN** that hole SHALL expose exactly one active pin position that does not change until the round completes

#### Scenario: Pins may vary across rounds deterministically

- **WHEN** pin positions are derived for two different rounds of the same tournament
- **THEN** they MAY differ, and each SHALL be reproducible from its round's seed

#### Scenario: A back pin brings the over-green trouble closer

- **WHEN** the green complex is emitted for a back pin versus a front pin
- **THEN** the back pin SHALL have less green behind it (over-green trouble nearer), and the front pin SHALL have less green in front of it (a shorter safe run-up)

#### Scenario: A centre pin is symmetric

- **WHEN** the green complex is emitted for a centre pin
- **THEN** the green SHALL extend equally in front of and behind the pin

### Requirement: Tournament Immutability

Once a Course has been fixed for a tournament, its generated form SHALL NOT change for the duration of that tournament (aside from per-round pin variation defined above).

#### Scenario: Course is stable during a tournament

- **WHEN** a tournament is underway on a Course
- **THEN** the Course's holes, surfaces, hazards, lengths, and elevation SHALL remain unchanged until the tournament completes

### Requirement: Canonical Terrain Generation

Course generation SHALL generate authoritative canonical geometry for every hole from the existing seed hierarchy and generator version. The geometry SHALL include a tee/start position, playable boundary, cup/green frame, and terrain regions required by the current surface catalogue. Existing coarse dimensions and round-specific pin generation SHALL remain available as compatibility/read-model data while canonical geometry is introduced.

#### Scenario: Generated hole terrain is complete

- **WHEN** a course generates a hole
- **THEN** the hole SHALL include canonical geometry sufficient to determine the surface at every finite position and to identify tee and cup positions

#### Scenario: Existing generation remains deterministic

- **WHEN** two courses are generated from the same seed and generator version
- **THEN** their canonical geometry and their existing hole dimensions SHALL be identical

### Requirement: Canonical Geometry Migration Compatibility

The course domain SHALL retain a compatibility path for existing `GeneratedHole` consumers and legacy zone-profile fixtures until generated production play has migrated to canonical terrain. New production terrain SHALL not be added exclusively through boolean hazard flags or `HoleZones` after canonical geometry is available.

#### Scenario: Existing fixture can be adapted

- **WHEN** a test supplies an existing one-dimensional hole fixture during migration
- **THEN** the system SHALL be able to resolve it through the documented legacy adapter without requiring a full canonical course generator

#### Scenario: Production holes do not need presentation synthesis

- **WHEN** a generated production hole exposes a bunker, water, tree, fairway, or green region
- **THEN** that gameplay terrain SHALL exist in its canonical geometry without requiring a frontend generator to invent its location

### Requirement: Migration terrain-exposure fidelity

For the current generated-course population, canonical terrain generation SHALL preserve a meaningful
calibration envelope from the legacy generated `ShotZoneProfile` model while using genuinely two-dimensional,
asymmetric landforms. In particular, fairway, green, fringe, bunker, water/tree recovery terrain, and the
playable boundary SHALL have reachable exposure consistent with the current calibrated shot distribution.
Canonical generation SHALL NOT silently replace legacy hazard/outer-boundary exposure with indefinitely safe
rough or a decorative polygon too small or remote to affect normal miss patterns. This requirement is a
migration compatibility constraint, not a permanent prescribed surface percentage for future course-design work.

#### Scenario: Generated hazards remain reachable

- **WHEN** a deterministic sample of current generated holes includes bunker, water, or tree flags
- **THEN** its canonical polygons SHALL occupy tactically meaningful miss or landing areas within the playable boundary rather than being only decorative or unreachable terrain

#### Scenario: Boundary retains meaningful risk

- **WHEN** a sampled landing travels beyond the generated fairway/cut/rough envelope on a hole without a flanking hazard at that location
- **THEN** canonical surface resolution SHALL eventually reach out of bounds rather than extending safe terrain indefinitely

