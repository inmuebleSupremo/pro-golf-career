# course-persistence Specification

## ADDED Requirements

### Requirement: V6 regenerates shared geography from retained provenance

V6 courses/worlds SHALL regenerate their landscape plan, placements and local canonical terrain from explicit V6
version, existing seed hierarchy and design inputs. Saves SHALL not duplicate polygons, context or candidate pools;
V1--V5 pins SHALL not be rewritten or interpreted as V6.

#### Scenario: Restored V6 geography is exact

- **WHEN** a V6 world is restored
- **THEN** its shared features, placements and canonical geometry SHALL match its original generation
