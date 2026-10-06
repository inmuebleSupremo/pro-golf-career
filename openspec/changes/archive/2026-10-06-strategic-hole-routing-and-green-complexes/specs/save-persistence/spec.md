## MODIFIED Requirements

### Requirement: Deterministic Spatial Continuity

Canonical generated course geometry and version-supported associated semantic plans SHALL be restored by deterministic regeneration from the saved world seed, configuration, and explicitly persisted course-generator version rather than by independently mutable polygon or semantic presentation copies. New saves SHALL record that provenance. A supported historical save lacking generator-version provenance SHALL restore through the documented historical V1 generator, never through the latest generator. The current save scope SHALL remain unchanged: it does not persist a pending playable event.

#### Scenario: Version-pinned V3 save restores its course identity

- **WHEN** a world saved with explicit supported V3 provenance is loaded
- **THEN** its course pool, route/zone/green semantics, progression targets, and canonical geometry SHALL be regenerated with that same version

#### Scenario: Save contains no duplicate terrain authority

- **WHEN** a V3 world is saved
- **THEN** it SHALL persist generator provenance and normal world state without serializing a separately mutable copy of course polygons or semantic terrain
