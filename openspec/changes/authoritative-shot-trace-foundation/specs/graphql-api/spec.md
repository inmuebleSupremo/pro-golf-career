## ADDED Requirements

### Requirement: Shot trace projection
The GraphQL API SHALL expose an optional `ShotOutcome.trace` for observable canonical shots. The projection SHALL include factual club identity, origin, intended aim point, contact with surface, optional non-flight transition, and final legal point. It SHALL reuse established point/contact representations where practical and SHALL retain `ShotSettlement`.

#### Scenario: Observable ball strike returns trace and settlement
- **WHEN** an authorized client resolves a current canonical ball-strike intent
- **THEN** its outcome SHALL expose the trace that arose from that exact intent and resolution
- **AND THEN** trace final point SHALL agree with `settlement.ball.position`.

#### Scenario: Historical or summary result may have no trace
- **WHEN** an outcome was resolved without trace materialization or predates trace support
- **THEN** its trace field MAY be null
- **AND THEN** existing outcome and settlement consumers SHALL remain valid.

### Requirement: Transition projection is explicitly non-flight
The API SHALL project recovery/replay as an optional trace transition with kind, source contact point, and legal destination. It SHALL not encode this transition as a flight path.

#### Scenario: Out-of-bounds is distinct from endpoint travel
- **WHEN** a shot contacts out of bounds and settlement replays from a legal point
- **THEN** the API SHALL expose the out-of-bounds contact and replay transition
- **AND THEN** the client can distinguish it from ordinary travel to final point.

## MODIFIED Requirements

### Requirement: Playable-Event Mutations
The API SHALL let the player play or sim their paused event: play the current shot with a ball-strike intent, sim the current shot, sim the rest of the current hole, round, or event, and complete the finished event. Playing or visibly simming one canonical shot SHALL return its resolved outcome and materialized trace; completing the event SHALL resume the paused week and return world status. Playing SHALL authorize the session and reject stale intents before a stroke is resolved.

#### Scenario: Stale intent contains no trace
- **WHEN** a client submits a stale ball-strike revision
- **THEN** the typed stale result SHALL have no outcome and no trace
- **AND THEN** no spatial or scoring state changes.
