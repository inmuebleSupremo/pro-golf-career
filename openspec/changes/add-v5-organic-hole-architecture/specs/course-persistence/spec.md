# course-persistence Specification

## ADDED Requirements

### Requirement: V5 architecture provenance regenerates without rewriting history

A persisted V5 course/world SHALL regenerate its architecture profile, selected architecture plans, quality
signature and canonical terrain from its established seed hierarchy, design inputs and explicit V5 generator pin.
Existing V1--V4 courses SHALL continue to regenerate through their historical pins without fabricated V5 metadata.
V5 introduction SHALL not require serializing duplicate terrain polygons or destructively migrating a career.

#### Scenario: Restored V5 course preserves its selected identity

- **WHEN** a V5 world is restored from save provenance
- **THEN** its generated architecture profile, ordered selected plans and canonical geometry SHALL match the original
  generated world

#### Scenario: Earlier career does not adopt V5

- **WHEN** a save created with a V1, V2, V3 or V4 course pin is loaded after V5 exists
- **THEN** its course pool and historical events SHALL retain their recorded generator behavior and geometry
