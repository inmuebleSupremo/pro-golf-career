## MODIFIED Requirements

### Requirement: Deterministic Spatial Continuity

Canonical generated course geometry SHALL be restored by regeneration from the saved world seed, configuration, and explicitly persisted course-generator version rather than by an independently mutable presentation copy. New saves SHALL record that provenance. A supported historical save lacking generator-version provenance SHALL restore through the documented historical V1 generator, never through the latest generator. The current save scope SHALL remain unchanged: it does not persist a pending playable event.

#### Scenario: Version-pinned save restores its original geometry

- **WHEN** a world saved with an explicit supported generator version is loaded
- **THEN** its course pool and canonical geometry SHALL be regenerated with that same version

#### Scenario: Historical save without provenance preserves V1 venues

- **WHEN** a supported pre-provenance save is loaded
- **THEN** it SHALL use the documented V1 compatibility generator rather than the latest generator

#### Scenario: Representative legacy disk fixture remains playable

- **WHEN** a checked-in representative current-v2-envelope save fixture without generator provenance is loaded
- **THEN** it SHALL restore a usable V1-pinned world with the fixture's historical venue identities and canonical geometry

#### Scenario: Unsupported pinned version is rejected

- **WHEN** a save records a course-generator version the application no longer supports
- **THEN** loading SHALL fail clearly before returning a partially regenerated world
