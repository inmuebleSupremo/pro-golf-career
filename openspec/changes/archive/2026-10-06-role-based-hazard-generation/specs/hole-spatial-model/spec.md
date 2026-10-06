# hole-spatial-model Specification

## ADDED Requirements

### Requirement: Hazard semantics complement but do not replace terrain authority

V4 hazard features SHALL be immutable generated planning metadata associated with V4 holes. They MAY support generator diagnostics, future shot-intent reasoning, AI compatibility checks, and tests, but canonical `CourseGeometry` SHALL remain the only authority for surface lookup, playability, penalties, and settlement. Hazard semantics SHALL not become a new surface catalogue, a persisted polygon representation, or a player-facing aim control.

#### Scenario: Future intent can inspect a feature without changing current resolution

- **WHEN** a future caller examines a V4 feature's role, anchor, surface, side, severity, or envelope
- **THEN** current shot resolution SHALL still use only canonical geometry and existing controls
