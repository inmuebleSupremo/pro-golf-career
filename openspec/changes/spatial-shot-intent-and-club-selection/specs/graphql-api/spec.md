## ADDED Requirements

### Requirement: Ball-strike GraphQL contract
The GraphQL API SHALL expose a dedicated ball-strike intent input with a catalogue club ID, absolute aim-point coordinates, and expected opaque shot revision. It SHALL not expose engine-only decision objects as the public contract.

#### Scenario: Intent mutation is validated
- **WHEN** a client submits malformed coordinates, an unknown club, or an out-of-envelope point
- **THEN** the API returns a validation error without resolving a stroke.

### Requirement: Planning guidance projection
The GraphQL API SHALL expose a coherent current-shot planning projection containing canonical ball state, revision, aim envelope, club/reach guidance, and SAFE/PRIMARY/AGGRESSIVE defaults.

#### Scenario: Reach warning is not a server rejection
- **WHEN** guidance reports a selected point beyond approximate club reach
- **THEN** the client can show a warning
- **AND THEN** a point inside the aim envelope remains submittable.

### Requirement: Stale-shot conflict contract
The API SHALL return a distinct retryable stale-shot result when expected revision does not match current round state, without leaking internal mutation details.

#### Scenario: Client refreshes after stale intent
- **WHEN** a stale-shot result is returned
- **THEN** the client can request the latest planning projection before retrying.

## MODIFIED Requirements

### Requirement: Playable-Event Mutations
The API SHALL let the player play or sim their paused event: play the current shot with a ball-strike intent, sim the current shot, sim the rest of the current hole, round, or event, and complete the finished event. Playing or simming a shot SHALL return the resolved shot outcome; completing the event SHALL resume the paused week and return world status. The API SHALL authorize the session and reject stale intents before a stroke is resolved.

#### Scenario: Authorized request uses current revision
- **WHEN** the owner submits a valid intent with the pending revision
- **THEN** the mutation resolves exactly one shot
- **AND THEN** it returns the existing shot outcome representation.

#### Scenario: The event is simmed and completed
- **WHEN** a client sims the remainder of the event and then completes it
- **THEN** the event result SHALL count, the paused week SHALL resume, and returned status SHALL reflect the advanced world.

#### Scenario: Acting off-event is rejected
- **WHEN** a client plays a shot or completes an event with no pending player event
- **THEN** the API SHALL return a client-error-classified GraphQL error.

### Requirement: Mutation Input Types
The API SHALL accept structured inputs for mutations that need them: ball-strike intent (catalogue club, absolute aim point, expected shot revision) and career goal (goal type with optional target). Enum-valued arguments SHALL be supplied as enum names and resolved at the API edge; an unrecognized name SHALL yield a client-error-classified GraphQL error.

#### Scenario: A ball-strike intent input is honored
- **WHEN** a client supplies a valid current ball-strike intent
- **THEN** the engine SHALL resolve the shot using that club and literal aim point.

#### Scenario: An unrecognized enum name is rejected
- **WHEN** a client supplies an enum-valued argument whose name is not valid
- **THEN** the API SHALL return a client-error-classified GraphQL error naming the bad value.
