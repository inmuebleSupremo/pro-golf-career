# save-persistence Specification

## ADDED Requirements

### Requirement: V6 adoption preserves historical saves

Save persistence SHALL retain V6 provenance without migrating historical course pins, pools, events or results.
Saving a loaded V1--V5 world after V6 exists SHALL preserve its original generator version and regeneration meaning.

#### Scenario: V5 save remains V5

- **WHEN** a V5 save is loaded and saved after V6 support exists
- **THEN** it SHALL retain its V5 generator provenance
