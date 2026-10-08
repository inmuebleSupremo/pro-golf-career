## ADDED Requirements

### Requirement: Explicit Future Pin-Policy Adoption

A player-controlled world at a clean event boundary SHALL support an explicit, atomic adoption of V5 pin placement for future unstarted scheduled events. The snapshot SHALL retain both the world default for future schedule generation and the per-event policy provenance needed to distinguish migrated future events from completed history. Snapshots lacking this provenance SHALL restore with legacy semantics.

#### Scenario: Adoption changes only future unstarted events

- **WHEN** an eligible existing career explicitly adopts V5 with no pending playable event
- **THEN** its future unstarted scheduled events and future schedule default SHALL become V5
- **AND THEN** completed results, archived schedules, course-generator provenance, career progression, and prior seed-derived history SHALL remain unchanged

#### Scenario: Adoption is unavailable mid-event

- **WHEN** a playable event is pending
- **THEN** V5 adoption SHALL be rejected without changing world state

#### Scenario: Repeated adoption is idempotent

- **WHEN** a career already adopted V5 requests adoption again at a clean boundary
- **THEN** it SHALL make no further state change and consume no random draws
