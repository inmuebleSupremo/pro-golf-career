# save-persistence Specification

## ADDED Requirements

### Requirement: V5 generator adoption does not destructively migrate saves

Save persistence SHALL preserve the world/course generator provenance needed to regenerate V5 architecture while
continuing to load supported earlier saves. Adoption of V5 for newly created worlds SHALL not rewrite an existing
save's generator pin, course pool, historical events or completed results, and SHALL not require persisting
canonical polygons or transient candidate pools.

#### Scenario: Pre-V5 save remains stable

- **WHEN** a supported pre-V5 save is loaded and later saved after V5 support is introduced
- **THEN** its existing course generator provenance and historical course meanings SHALL remain unchanged
