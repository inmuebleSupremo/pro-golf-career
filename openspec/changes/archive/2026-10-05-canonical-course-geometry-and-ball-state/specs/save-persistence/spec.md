## ADDED Requirements

### Requirement: Deterministic Spatial Continuity

Canonical generated course geometry SHALL be restored by regeneration from the saved world seed, configuration, and generator version rather than by an independently mutable presentation copy. The current save scope SHALL remain unchanged: it does not persist a pending playable event. If a later save format adds an in-progress playable event, it SHALL persist the complete playable `BallState` and any unresolved spatial settlement required to resume at the same legal next-shot origin.

#### Scenario: Restored geometry matches generated geometry

- **WHEN** a world is saved, loaded, and its course geometry is requested
- **THEN** the restored geometry SHALL equal the geometry generated for the original world seed, configuration, and generator version

#### Scenario: Current saves do not claim mid-hole resume

- **WHEN** a save is written while the application has no supported pending-event snapshot
- **THEN** the save contract SHALL not claim to preserve a mid-hole ball position

#### Scenario: Pending events are rejected rather than partially saved

- **WHEN** a caller attempts to snapshot a world with a pending playable event
- **THEN** the snapshot operation SHALL fail rather than silently omit the active `BallState` or settlement

#### Scenario: A future event snapshot is spatially complete

- **WHEN** a future save format persists an in-progress playable event
- **THEN** loading it SHALL restore the exact playable ball position and lie from which the next shot would have begun
