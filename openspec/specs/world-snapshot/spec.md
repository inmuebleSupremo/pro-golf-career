# world-snapshot Specification

## Purpose
An autonomous world can be captured as an immutable snapshot at a clean boundary and rebuilt into an identical world, such that continuing the restored world matches continuing the original. This is the capturability foundation for save/load (the serialization format and files are a separate app-layer concern).
## Requirements
### Requirement: World Capturable As An Immutable Snapshot

The simulation SHALL be able to capture the complete state of a world — autonomous or player-controlled — as an immutable snapshot, and to rebuild a world from such a snapshot. The snapshot SHALL be a true state capture — sufficient to reconstruct the world exactly — not a log of inputs to be replayed. When a golfer is designated to the player, the snapshot SHALL additionally capture the player-control state: the designated golfer and its standing decisions (development focus, resting, per-event skips, chosen career goals), the world's pending sponsorship / staff / equipment offers, and the set of already-achieved career goals. The seed-derived, purely-generated parts of the world (the course pool, the weather system, and stateless markets) MAY be regenerated from the master seed and configuration on restore rather than stored in the snapshot.

#### Scenario: A snapshot captures the accumulated world state

- **WHEN** a world is captured
- **THEN** the snapshot SHALL include every golfer and career, finances, physical and health state, tour memberships and standings, the world ranking, the statistics archive, the media feed, the Hall-of-Fame registry, season archives and ranking snapshots, the calendar position, and the world's progression counters

#### Scenario: A player-controlled world captures the control state

- **WHEN** a world with a designated player golfer is captured
- **THEN** the snapshot SHALL include the designated golfer id, the player's development focus, resting and per-event skip choices, chosen career goals, pending sponsorship/staff/equipment offers, and achieved goals; and restoring it SHALL reproduce the same player control

#### Scenario: Restore rebuilds the golfer graph consistently

- **WHEN** a world is rebuilt from a snapshot
- **THEN** records that reference golfers (such as tournament results) SHALL be re-linked to the rebuilt golfer instances by identity, so the restored world holds a single consistent object graph

#### Scenario: An autonomous world restores without player control

- **WHEN** a world with no designated player is captured and restored
- **THEN** the restored world SHALL have no player control, identical to the pre-existing autonomous behaviour

### Requirement: Snapshot Is Taken At A Clean Boundary

Capturing a world SHALL be permitted only at a clean boundary — when no player event is pending (no interactive event is mid-play). Capturing SHALL be rejected while a player event is paused, so a snapshot never has to encode a half-played event.

#### Scenario: Capturing mid-event is rejected

- **WHEN** a capture is requested while a player event is pending
- **THEN** the capture SHALL be rejected rather than producing a partial snapshot

### Requirement: Restore Preserves Determinism

Continuing a restored world SHALL be indistinguishable from continuing the original. For the same world, snapshotting then restoring then advancing any number of steps SHALL produce a world identical to advancing the original by the same number of steps — same rankings, standings, careers, finances, statistics, and history.

#### Scenario: Restore-then-advance equals advance

- **WHEN** a world is snapshotted, rebuilt from that snapshot, and both the original and the rebuilt world are advanced by the same number of steps
- **THEN** the two worlds SHALL be observably identical afterward

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
