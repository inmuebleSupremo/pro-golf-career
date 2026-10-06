# course-generation Specification

## MODIFIED Requirements

### Requirement: Deterministic Generation

Course generation SHALL support retained V1, V2, V3, and V4 implementations selected only through an explicit generator version. V4 SHALL deterministically translate the existing course-design and V3 spatial inputs into a semantic hazard plan before compiling canonical hazard regions. V1/V2/V3 output and fixtures SHALL remain unchanged. New worlds SHALL select the configured current version, while restored worlds SHALL select their persisted pin; unsupported versions SHALL fail explicitly.

#### Scenario: Same V4 inputs reproduce hazard intent and terrain

- **WHEN** a V4 course is generated twice from the same seed, classification, design inputs, and version
- **THEN** its hazard features and canonical terrain regions SHALL be identical

#### Scenario: Historical generator output is isolated

- **WHEN** a V1, V2, or V3 fixture is generated after V4 exists
- **THEN** its historical output SHALL remain exact and SHALL not acquire V4 hazard features

## ADDED Requirements

### Requirement: V4 hazards compile into canonical terrain

V4 SHALL compile accepted semantic hazard features into existing canonical `TerrainRegion` polygons. The compiler SHALL use simple deterministic geometry, preserve the existing surface catalogue and canonical precedence contract, and avoid strategic-hazard overlap. Existing rotated ellipses SHALL remain sufficient by default; at most one narrow simple route-aligned polygon primitive MAY be added for a bounded lateral water feature.

#### Scenario: Canonical renderer needs no authored hazard placement

- **WHEN** a V4 hole has a role-based hazard
- **THEN** its gameplay polygon SHALL be present in `CourseGeometry` without a frontend placement generator
