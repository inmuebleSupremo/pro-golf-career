# course-persistence Specification

## ADDED Requirements

### Requirement: V4 course identity regenerates role-based hazard intent

A persisted V4 course/world SHALL regenerate its semantic hazard features and canonical hazard terrain deterministically from its established seed hierarchy, design inputs, and explicit V4 generator pin. Existing V1/V2/V3 persisted courses SHALL continue to regenerate through their historical pins without fabricated V4 feature metadata.

#### Scenario: Restored V4 course retains intentional hazards

- **WHEN** a V4 world is restored from its save provenance
- **THEN** its hazard features and canonical hazard polygons SHALL match the original generated world
