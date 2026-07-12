# world-snapshot Specification

## Purpose
An autonomous world can be captured as an immutable snapshot at a clean boundary and rebuilt into an identical world, such that continuing the restored world matches continuing the original. This is the capturability foundation for save/load (the serialization format and files are a separate app-layer concern).

## Requirements
### Requirement: World Capturable As An Immutable Snapshot

The simulation SHALL be able to capture the complete state of an autonomous world as an immutable snapshot, and to rebuild a world from such a snapshot. The snapshot SHALL be a true state capture — sufficient to reconstruct the world exactly — not a log of inputs to be replayed. The seed-derived, purely-generated parts of the world (the course pool, the weather system, and stateless markets) MAY be regenerated from the master seed and configuration on restore rather than stored in the snapshot.

#### Scenario: A snapshot captures the accumulated world state

- **WHEN** a world is captured
- **THEN** the snapshot SHALL include every golfer and career, finances, physical and health state, tour memberships and standings, the world ranking, the statistics archive, the media feed, the Hall-of-Fame registry, season archives and ranking snapshots, the calendar position, and the world's progression counters

#### Scenario: Restore rebuilds the golfer graph consistently

- **WHEN** a world is rebuilt from a snapshot
- **THEN** records that reference golfers (such as tournament results) SHALL be re-linked to the rebuilt golfer instances by identity, so the restored world holds a single consistent object graph

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
