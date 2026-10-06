# save-persistence Specification

## ADDED Requirements

### Requirement: V4 persistence stores provenance rather than hazard copies

Save persistence SHALL retain the world course-generator version pin needed to reproduce V4 hazard planning. It SHALL NOT store full canonical hazard polygons or duplicate mutable hazard-plan payloads merely because V4 introduces semantic feature metadata. Missing legacy provenance SHALL retain its documented V1 fallback and unsupported pins SHALL fail explicitly.

#### Scenario: V4 save does not need polygon serialization

- **WHEN** a V4 save is written and restored
- **THEN** its role-based hazard terrain SHALL be regenerated from deterministic provenance rather than a saved polygon copy
