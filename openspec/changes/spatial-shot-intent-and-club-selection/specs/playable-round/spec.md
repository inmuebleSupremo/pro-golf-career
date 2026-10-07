## ADDED Requirements

### Requirement: Shared human and AI intent seam
Playable rounds SHALL execute both manual and simulated strokes through `ShotIntent`.

#### Scenario: AI strategy emits an intent
- **WHEN** AI policy selects a tactical strategy
- **THEN** it selects a catalogue club and resolves a spatial point into `BallStrikeIntent`
- **AND THEN** the shared resolver executes that intent.

### Requirement: Human strategy is not authoritative shot control
Manual rounds SHALL not accept conservative, balanced, or aggressive strategy as an authoritative shot-control input. Internal AI strategy MAY remain for policy selection.

#### Scenario: Human selected point governs direction
- **WHEN** a human submits a valid aim point
- **THEN** no strategy or `ShotAim` substitution changes that point's intended direction.

### Requirement: Stale shot submissions are safe
Each pending playable shot SHALL expose an opaque revision. Resolution SHALL atomically verify the expected revision before mutating round state and SHALL reject stale or replayed submissions without changing strokes, ball position, or round state.

#### Scenario: Duplicate submission loses the race
- **WHEN** two requests use the same pending-shot revision
- **THEN** at most one resolves a stroke
- **AND THEN** the other returns a retryable stale-shot conflict.

## MODIFIED Requirements

### Requirement: Interactive Shot-by-Shot Round
The system SHALL provide an interactive 18-hole round in which a human plays one shot at a time through the shot engine. For a pending shot it SHALL expose the hole and par, shot number, canonical ball state, current lie, reachable surfaces/hazards, opaque shot revision, aim envelope, available individual clubs, and guidance. It SHALL accept a ball-strike intent rather than a distance/lateral/strategy decision and resolve exactly one shot per current revision.

#### Scenario: Manual shot starts with a planning projection
- **WHEN** a playable human shot is pending
- **THEN** the round exposes ball state, shot revision, aim envelope, available clubs, and guidance
- **AND THEN** the player can submit a ball-strike intent using that projection.

#### Scenario: The round completes with a score
- **WHEN** all eighteen holes have been played
- **THEN** the round SHALL report total strokes, per-hole scores, and score relative to par.
