# hole-spatial-model Specification

## ADDED Requirements

### Requirement: V6 landscape context cannot become a second gameplay geometry

Local effective `CourseGeometry` SHALL remain the sole authority for V6 surface lookup, targets, settlement,
recovery and penalties. Landscape context SHALL be immutable placement/presentation data. A feature crossing playable
space SHALL compile from that same feature into canonical geometry or SHALL be masked outside; it SHALL not invent a
hazard, obstruction, carry or collision.

#### Scenario: Context water has no independent outcome

- **WHEN** context water has no equivalent canonical in-bound region
- **THEN** it SHALL be clipped outside playable terrain and SHALL not alter settlement or target validation
