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

Every Hole SHALL have exactly one active pin position, which SHALL remain fixed for the duration of a completed round. Pin positions MAY differ between rounds of the same tournament, derived deterministically per round.

#### Scenario: Pin is fixed within a round

- **WHEN** a round is in progress on a hole
- **THEN** that hole SHALL expose exactly one active pin position that does not change until the round completes

#### Scenario: Pins may vary across rounds deterministically

- **WHEN** pin positions are derived for two different rounds of the same tournament
- **THEN** they MAY differ, and each SHALL be reproducible from its round's seed

### Requirement: Tournament Immutability

Once a Course has been fixed for a tournament, its generated form SHALL NOT change for the duration of that tournament (aside from per-round pin variation defined above).

#### Scenario: Course is stable during a tournament

- **WHEN** a tournament is underway on a Course
- **THEN** the Course's holes, surfaces, hazards, lengths, and elevation SHALL remain unchanged until the tournament completes

