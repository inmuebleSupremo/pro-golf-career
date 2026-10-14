## ADDED Requirements

### Requirement: New worlds select the organic architecture generator

The system SHALL generate a newly created world's course pool with the explicit V5 generator version while retaining V1 through V4 generation implementations for saved-world compatibility.

#### Scenario: Existing saved world retains its generator provenance

- **WHEN** a world snapshot created with an earlier supported generator version is restored
- **THEN** its course pool SHALL be regenerated with that stored version
- **AND** it SHALL NOT adopt V5 merely because V5 is current for new worlds

### Requirement: Parkland architecture is authoritative geometry

The V5 Parkland generator SHALL derive routing, fairway width variation, greens, hazards and tree regions deterministically from the coordinate and course profile, compiling them into canonical terrain geometry.

#### Scenario: Parkland hole has canonical environmental surfaces

- **WHEN** a V5 Parkland course is generated
- **THEN** its tree and hazard regions SHALL be present in the backend canonical geometry when selected
- **AND** rendering and shot-resolution SHALL consume those same regions
