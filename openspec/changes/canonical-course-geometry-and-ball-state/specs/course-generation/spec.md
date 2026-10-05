## ADDED Requirements

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
