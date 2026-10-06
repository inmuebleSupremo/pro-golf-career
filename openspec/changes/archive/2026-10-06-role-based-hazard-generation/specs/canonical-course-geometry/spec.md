# canonical-course-geometry Specification

## ADDED Requirements

### Requirement: V4 hazard geometry preserves deterministic precedence and recovery safety

V4 hazard compilation SHALL produce finite, counter-clockwise, simple canonical polygons and SHALL reject incompatible same-precedence overlap. Strategic hazard regions SHALL not overlap each other, cover required tee/cup/green cores, or disconnect normal route progression. V4 water features SHALL provide deterministic legal relief for representative intended contacts under the established tee-ward rough-scan rule; layouts that normally require pathological fallback or combine with out-of-bounds to prevent playable progression SHALL be rejected.

#### Scenario: V4 water has a legal deterministic drop

- **WHEN** a representative intended contact enters a generated V4 water feature
- **THEN** the existing relief algorithm SHALL select a legal `PRIMARY_ROUGH` recovery point and the resulting ball state SHALL be the next-shot origin

#### Scenario: Strategic hazards do not create visual ambiguity

- **WHEN** V4 canonical regions are emitted
- **THEN** hazard polygons requiring conflicting visual/gameplay stacking SHALL be rejected or avoided rather than depending on unspecified renderer ordering
